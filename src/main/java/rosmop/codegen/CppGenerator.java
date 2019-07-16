package rosmop.codegen;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rosmop.ROSMOPException;
import rosmop.RVParserAdapter;
import rosmop.parser.ast.ROSEvent;
import rosmop.parser.ast.Variable;
import rosmop.util.MessageParser;
import rosmop.util.Tool;

import com.runtimeverification.rvmonitor.c.rvc.CSpecification;
import com.runtimeverification.rvmonitor.logicpluginshells.LogicPluginShellResult;

/**
 * @author Cansu Erdogan
 *
 * Generates rvmonitor.cpp file from the wrapped ASTs of input specification files
 *
 */
public class CppGenerator {

	protected static SourcePrinter printer = new SourcePrinter();

	public static void reset() {
		printer = new SourcePrinter();
	}

	public static void generateCpp(HashMap<CSpecification, LogicPluginShellResult> toWrite,
								   String outputPath, boolean monitorAsNode)
	    throws FileNotFoundException, ROSMOPException, java.io.IOException
	{
		printer.printLn("#include \"rvmonitor.h\"");

		if(!monitorAsNode) {

			printer.printLn();
			printer.printLn("using namespace std;");
			printer.printLn("namespace rv");
			printer.printLn("{");
			printer.indent();

			// Print declared variables
			printer.printLn("// Declarations of shared variables");
			for (CSpecification rvcParser : toWrite.keySet()) {
				printer.printLn(rvcParser.getDeclarations());

				printer.printLn();

				if (toWrite.get(rvcParser) != null) {
					printer.printLn(
							(String) toWrite.get(rvcParser).properties.get("state declaration"));
					printer.printLn(
							(String) toWrite.get(rvcParser).properties.get("categories"));
					printer.printLn(
							(String) toWrite.get(rvcParser).properties.get("monitoring body"));
					printer.printLn();
				}
			}

			printMonitorNamespace(toWrite);

			// print init()
			if (HeaderGenerator.hasInit) {
				printer.printLn("void " + GeneratorUtil.MONITOR_CLASS_NAME + "::init(){\n");
				printer.indent();

				for (CSpecification rvcParser : toWrite.keySet()) {
					if (!((RVParserAdapter) rvcParser).getInit().isEmpty()) {
						printer.printLn(((RVParserAdapter) rvcParser).getInit());
					}
				}

				printer.unindent();
				printer.printLn("}");
				printer.printLn();
			}

			// print reset()
			for (CSpecification rvcParser : toWrite.keySet()) {
				if (toWrite.get(rvcParser) != null) {
					printer.printLn(((String) toWrite.get(rvcParser).properties.get("reset"))
							.replace("void\n__RVC_", "void "
									+ GeneratorUtil.MONITOR_CLASS_NAME + "::__RVC_"));
					printer.printLn();
				}
			}

			// print constructor
			printConstructor(toWrite);

			// print monitor callback functions
			printMonitorCallbacks(toWrite, false);

			printer.printLn();
			printer.unindent();
			printer.printLn("}");
			printer.printLn();
		} else {
		    toWrite.values().forEach(x -> {
			if(x != null) {
					printer.print(x.properties.getOrDefault("state setters", "").toString());
					printer.printLn("");

					printer.print(x.properties.getOrDefault("monitoring body", "").toString());
					printer.printLn("");
				}
		    });
			printMonitorCallbacks(toWrite, true);
			printMain(toWrite);
		}

		Tool.writeFile(printer.getSource(), outputPath);
	}

	private static void printMain(HashMap<CSpecification, LogicPluginShellResult> toWrite) {
	   printer.printLn("int main(int argc, char ** argv)");
	   printer.printLn("{");
	   printer.indent();

	   printer.printLn("ros::init(argc, argv, \"rvmonitor\");");

       if(HeaderGenerator.hasInit) {
           printer.printLn("// Manually Written Initialization Code");
           printer.printLn("init()");
       }

		for (CSpecification rvcParser : toWrite.keySet()) {
			for (ROSEvent event : ((RVParserAdapter) rvcParser).getEventsList()) {
                String handleName = (event.getTopic() + "Handle")
						.replace("/", "")
						.replace("_", "");

                String subscriberName = (event.getTopic() + "Subscriber")
						.replace("/", "")
						.replace("_", "");

                printer.printLn("ros::NodeHandle " + handleName + ";");
                printer.printLn("ros::Subscriber " + subscriberName + " = ");
                printer.indent();
                String topicName = event.getTopic().replace("/", "");
                printer.print(handleName + ".subscribe(" + "\"" + topicName + "\"" + " , 1000, ");
				if (HeaderGenerator.addedTopics.get(event.getTopic()).size() > 1)
					printer.printLn("mergedMonitorCallback_" + topicName + ");");
				else
					printer.printLn("monitorCallback_" + event.getName() +");");
				printer.printLn("");
				printer.unindent();
			}
		}
		printer.printLn("ros::spin();");
		printer.unindent();
		printer.printLn("return 0;");
		printer.printLn("}");
	}

	private static void printMonitorNamespace(
			HashMap<CSpecification, LogicPluginShellResult> toWrite) {
		printer.printLn("namespace monitor");
		printer.printLn("{");
		printer.indent();

		printer.printLn("std::set<std::string> " + GeneratorUtil.MONITOR_TOPICS_VAR + ";");
		printer.printLn("std::set<std::string> " + GeneratorUtil.MONITOR_TOPICS_ALL + ";");
		printer.printLn("std::set<std::string> " + GeneratorUtil.MONITOR_TOPICS_ENB + ";");
		printer.printLn("std::map<std::string,std::string> "
				+ GeneratorUtil.MONITOR_TOPICS_AND_TYPES + ";");

		printer.printLn();

		printMonitorInsertion(toWrite);

		printAdvertisingOptions(toWrite);

		printer.unindent();
		printer.printLn("}");
		printer.printLn();
	}

	private static void printMonitorInsertion(
			HashMap<CSpecification, LogicPluginShellResult> toWrite) {
		printer.printLn("void initMonitorTopics()");
		printer.printLn("{");
		printer.indent();

		for (String topic : HeaderGenerator.addedTopics.keySet()) {
			printer.printLn(GeneratorUtil.MONITOR_TOPICS_VAR + ".insert(\"" + topic + "\");");
			printer.printLn(GeneratorUtil.MONITOR_TOPICS_AND_TYPES + "[\"" + topic + "\"] = \""
					+ HeaderGenerator.addedTopics.get(topic).get(0).getMsgType() + "\";");
		}
		printer.printLn();
		for (CSpecification rvcParser : toWrite.keySet()) {
			printer.printLn(GeneratorUtil.MONITOR_TOPICS_ALL + ".insert(\""
					+ rvcParser.getSpecName() + "\");");
		}
		printer.printLn();
		printer.unindent();
		printer.printLn("}");
		printer.printLn();
	}

	private static void printAdvertisingOptions(
			HashMap<CSpecification, LogicPluginShellResult> toWrite) {
		printer.printLn("void initAdvertiseOptions(std::string topic, ros::AdvertiseOptions"
			+ " &" + GeneratorUtil.ADVERTISE_OPTIONS + ")");
		printer.printLn("{");
		printer.indent();

		//ops_pub: publisher registration

		//if(topic=="/landshark_control/base_velocity")
		//  ops_pub.init<geometry_msgs::TwistStamped>(topic,1000);
		boolean isFirst = true;

		for (String topic : HeaderGenerator.addedTopics.keySet()) {
			if (!isFirst) {
				printer.print("else ");
			} else {
				isFirst = false;
			}

			printer.printLn("if (topic == \"" + topic + "\") {");

			printer.indent();
			printer.printLn(GeneratorUtil.ADVERTISE_OPTIONS + ".init<"
					+ HeaderGenerator.addedTopics.get(topic).get(0).classifyMsgType()
					+ ">(topic, 1000);");
			printer.unindent();
			printer.printLn("}");
		}

		printer.unindent();
		printer.printLn("}");
		printer.printLn();
	}

	/**
	 * Generate code for constructor
	 * @param toWrite
	 */
	private static void printConstructor(HashMap<CSpecification, LogicPluginShellResult> toWrite){
		printer.printLn(GeneratorUtil.MONITOR_CLASS_NAME + "::" + GeneratorUtil.MONITOR_CLASS_NAME
				+ "(string topic, ros::SubscribeOptions &"
				+ GeneratorUtil.SUBSCRIBE_OPTIONS + ")");
		printer.printLn("{");
		printer.indent();

		//	     topic_name = topic;
		//	     server_manager = rv::ServerManager::instance(
		printer.printLn(GeneratorUtil.TOPIC_PTR_NAME + " = topic;");
		printer.printLn(GeneratorUtil.SERVERMANAGER_PTR_NAME
				+ " = rv::ServerManager::instance();");

		if(HeaderGenerator.hasInit){
			printer.printLn();
			printer.printLn("init();");
		}

		printer.printLn();

		//ops_sub: subscriber registration

		// if(topic=="/landshark_control/base_velocity")
		// ops_sub.init<geometry_msgs::TwistStamped>(topic,1000,boost::bind(
		// &RVt::monitorCallbackLandsharkBaseVelocityReverse, this, _1));
		boolean isFirst = true;

		for(String topic : HeaderGenerator.addedTopics.keySet()){
			if (!isFirst) printer.print("else ");
			else isFirst = false;

			printer.printLn("if (topic == \"" + topic + "\") {");
			printer.indent();

			if(HeaderGenerator.addedTopics.get(topic).size() == 1){
				printer.printLn(GeneratorUtil.SUBSCRIBE_OPTIONS + ".init<" +
						HeaderGenerator.addedTopics.get(topic).get(0).classifyMsgType()
						+ ">(topic, 1000, boost::bind(&" + GeneratorUtil.MONITOR_CLASS_NAME +
						"::monitorCallback_"
						+ HeaderGenerator.addedTopics.get(topic).get(0).getName()
						+ ", this, _1));");
			}else{
				String callback = "::mergedMonitorCallback_" + topic.replace("/", "");

				printer.printLn(GeneratorUtil.SUBSCRIBE_OPTIONS + ".init<" +
						HeaderGenerator.addedTopics.get(topic).get(0).classifyMsgType()
						+ ">(topic, 1000, boost::bind(&" + GeneratorUtil.MONITOR_CLASS_NAME +
						callback + ", this, _1));");
			}

			printer.unindent();
			printer.printLn("}");
		}

		printer.unindent();
		printer.printLn("}");
		printer.printLn();
	}

	private static void printMonitorCallbacks(
			HashMap<CSpecification, LogicPluginShellResult> toWrite, boolean monitorAsNode)
	    throws java.io.IOException
    {
		for (CSpecification rvcParser : toWrite.keySet()) {
			for (ROSEvent event : ((RVParserAdapter) rvcParser).getEventsList()) {
				if(HeaderGenerator.addedTopics.get(event.getTopic()).size() > 1){

				    if(!monitorAsNode)
                        printer.printLn("void " + GeneratorUtil.MONITOR_CLASS_NAME
                                + "::mergedMonitorCallback_" + event.getTopic().replace("/", "")
                                + "(const " + event.classifyMsgType()
                                + "::ConstPtr& " + GeneratorUtil.MONITORED_MSG_NAME + ")");
				    else
						printer.printLn("void " + "mergedMonitorCallback_" + event.getTopic().replace("/", "")
								+ "(const " + event.classifyMsgType()
								+ "::ConstPtr& " + GeneratorUtil.MONITORED_MSG_NAME + ")");
					printer.printLn("{");
					printer.printLn();
					printer.indent();

					printParametersBindingAll(event.getTopic());
					printer.printLn();

					for(ROSEvent mergeevents : HeaderGenerator.addedTopics.get(event.getTopic())){
						if(toWrite.get(rvcParser) == null ||
								!rvcParser.getEvents().keySet().contains(mergeevents.getName())){
							printActionCode(mergeevents, monitorAsNode);
						}else{
							printRVMGeneratedFunction(mergeevents, monitorAsNode);
						}
						printer.printLn();
					}
					HeaderGenerator.addedTopics.get(event.getTopic()).clear();

					if(!monitorAsNode) {
						publishAndSerializeMsg(event);
					}

					printer.unindent();
					printer.printLn();
					printer.printLn("}");
					printer.printLn();

				}else if(HeaderGenerator.addedTopics.get(event.getTopic()).size() > 0){

				    if(!monitorAsNode)
                        printer.printLn("void " + GeneratorUtil.MONITOR_CLASS_NAME
                                + "::monitorCallback_" + event.getName()
                                + "(const " + event.classifyMsgType()
                                + "::ConstPtr& " + GeneratorUtil.MONITORED_MSG_NAME + ")");
				    else
						printer.printLn("void " + "monitorCallback_" + event.getName()
								+ "(const " + event.classifyMsgType()
								+ "::ConstPtr& " + GeneratorUtil.MONITORED_MSG_NAME + ")");

					printer.printLn("{");
					printer.printLn();
					printer.indent();

					printParametersBinding(event);
					printer.printLn();

					if(toWrite.get(rvcParser) == null ||
							!rvcParser.getEvents().keySet().contains(event.getName())){
						printActionCode(event, monitorAsNode);
					}else{
						printRVMGeneratedFunction(event, monitorAsNode);
					}
					printer.printLn();
					HeaderGenerator.addedTopics.get(event.getTopic()).remove(event);

					if(!monitorAsNode)
						publishAndSerializeMsg(event);

					printer.unindent();
					printer.printLn();
					printer.printLn("}");
					printer.printLn();
				}
			}

			if(toWrite.get(rvcParser) != null){
				String str = (String) toWrite.get(rvcParser).properties.get("event functions");
				printer.printLn(str.replace("void\n__RVC_", "void "
						+ GeneratorUtil.MONITOR_CLASS_NAME + "::__RVC_"));
				printer.printLn();
			}
		}
	}

    private static void printParametersBindingAll(String topic)
        throws java.io.IOException
	{
		printer.printLn(HeaderGenerator.addedTopics.get(topic).get(0).classifyMsgType()
				+ " " + GeneratorUtil.MONITOR_COPY_MSG_NAME + ";");

		Map<String, String> msgMapping = new HashMap<String, String>();
		Set<String> noDoubleParam = new HashSet<String>(); //declaredName

		for(ROSEvent event : HeaderGenerator.addedTopics.get(topic)){
			msgMapping.putAll(MessageParser.parseMessage(event.getMsgType()));

			if(event.getParameters() != null){
				for (Variable var : event.getParameters()) {
					noDoubleParam.add(var.getDeclaredName());
				}
			}
		}

		for (String msgName : msgMapping.keySet()) {
			printer.printLn(GeneratorUtil.MONITOR_COPY_MSG_NAME + "." + msgName + " = "
					+ GeneratorUtil.MONITORED_MSG_NAME + "->" + msgName + ";");
		}

		printer.printLn();
		printer.printLn();

		for(ROSEvent event : HeaderGenerator.addedTopics.get(topic)){
			if(event.getParameters() != null){
				for (Variable parameter : event.getParameters()) {
					if(noDoubleParam.contains(parameter.getDeclaredName())){
						if(parameter.getType().endsWith("[]")){
							printer.print("vector<" + parameter.getType().replace("[]", "")
									+ ">" + "& " + parameter.getDeclaredName() + " = ");
						} else
							printer.print(parameter.getType() + "& "
									+ parameter.getDeclaredName() + " = ");
						printer.print(GeneratorUtil.MONITOR_COPY_MSG_NAME + "."
									+ event.getPattern().get(parameter.getDeclaredName()) + ";");
						printer.printLn();

						noDoubleParam.remove(parameter.getDeclaredName());
					} else{
						System.err.println("Duplicate parameter-pattern matching!: \t"
								+ parameter.getDeclaredName()
								+ "\n(Beware: Same named parameters are removed.)");
					}
				}
			}
		}
	}

	private static void printParametersBinding(ROSEvent event)
	    throws java.io.IOException
	{
		// geometry_msgs::TwistStamped rv_msg;
		// rv_msg.header = msg->header;
		// rv_msg.twist = msg->twist;

		// float& H = rv_msg.header;
		// double& A = rv_msg.twist.angular;
		// ...

		printer.printLn(event.classifyMsgType() + " " + GeneratorUtil.MONITOR_COPY_MSG_NAME + ";");

		Map<String, String> msgMapping = MessageParser.parseMessage(event.getMsgType());
		for (String msgName : msgMapping.keySet()) {
			printer.printLn(GeneratorUtil.MONITOR_COPY_MSG_NAME + "." + msgName + " = "
					+ GeneratorUtil.MONITORED_MSG_NAME + "->" + msgName + ";");
		}

		printer.printLn();
		printer.printLn();

		if(event.getParameters() != null){
			for (Variable parameter : event.getParameters()) {
				if(parameter.getType().endsWith("[]")){
					printer.print("vector<" + parameter.getType().replace("[]", "")
							+ ">" + "& " + parameter.getDeclaredName() + " = ");
				} else
					printer.print(parameter.getType() + "& "
							+ parameter.getDeclaredName() + " = ");
				printer.print(GeneratorUtil.MONITOR_COPY_MSG_NAME + "."
							+ event.getPattern().get(parameter.getDeclaredName()) + ";");
				printer.printLn();
			}
		}
	}

	private static void printMonitorEnabledCheck(ROSEvent event) {
		printer.printLn("if(monitor::" + GeneratorUtil.MONITOR_TOPICS_ENB
				+ ".find(\"" + event.getSpecName() + "\") != monitor::"
				+ GeneratorUtil.MONITOR_TOPICS_ENB + ".end())");
	}

	private static void printActionCode(ROSEvent event, boolean monitorAsNode) {
		if(!monitorAsNode) {
			printMonitorEnabledCheck(event);
		}

		if(monitorAsNode) {
			printer.indent();
			printer.print(event.getAction());
			printer.unindent();
		} else {
			printer.print(event.getAction());
		}
		printer.printLn();
		//		printer.unindent();
		//		printer.printLn("}");
		printer.printLn();
	}


	private static void printRVMGeneratedFunction(ROSEvent mergeevents, boolean monitorAsNode) {
		// __RVC_safeTrigger_checkPoint(std::string monitored_name, double monitored_position)
		if(!monitorAsNode) {
		    printMonitorEnabledCheck(mergeevents);
			printer.printLn("{");
			printer.printLn();
			printer.indent();
		}

		printer.print("__RVC_" + mergeevents.getSpecName() + "_" + mergeevents.getName() + "(");
		if(mergeevents.getParameters() != null && mergeevents.getParameters().size() != 0){
			int c = mergeevents.getParameters().size()-1;
			for (Variable var : mergeevents.getParameters()) {
				printer.print(var.getDeclaredName() + ((c-- > 0) ? ", " : ""));
			}
		}
		printer.printLn(");");

		if(!monitorAsNode) {
			printer.unindent();
			printer.printLn();
			printer.printLn("}");
		}
		printer.printLn();
	}

	private static void publishAndSerializeMsg(ROSEvent event) {
		// ros::SerializedMessage serializedMsg = ros::serialization::serializeMessage(msgName);
		// publishPtr->publish(serializedMsg);
		String serializedMessage =
				"ros::SerializedMessage serializedMsg = ros::serialization::serializeMessage("
						+ GeneratorUtil.MONITOR_COPY_MSG_NAME + ");";

                printMonitorEnabledCheck(event);
                printer.printLn("{");
                printer.indent();

		printer.print(serializedMessage);
		printer.printLn();
		serializedMessage = GeneratorUtil.SERVERMANAGER_PTR_NAME
				+ "->publish(" + GeneratorUtil.TOPIC_PTR_NAME + ", serializedMsg);";
		printer.print(serializedMessage);
		printer.unindent();
		printer.printLn();
		printer.print("}");
	}

}
