package rosmop.parser.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import rosmop.codegen.GeneratorUtil;
import com.runtimeverification.rvmonitor.core.ast.Event;

/**
 * An event monitored in the output program.
 */
public class ROSEvent extends Event {

  /*
    private final List<String> modifiers;
    private final String name;
    private final List<String> definitionModifiers;
    private final String definition;
    private final String pointcut;
    private final String action;
   */

    private final String topic, msgType;
    private HashMap<String, String> pattern;

    private final String specName;
    private List<Variable> parameters = null;
    private List<ROSEvent> publishKeywordEvents = null;

    /**
     * Construct an Event out of its component parts.
     * @param modifiers Strings that change the meaning of the event.
     * @param name The name of the event to monitor.
     * @param definition The descrption of what the event is on, e.g. its parameters.
     * @param action The action to take on encountering the event.
     */
    public ROSEvent(final List<String> modifiers, final String name, 
            final List<String> definitionModifiers, final String definition, final String topic, 
            final String msgType, final HashMap<String, String> pattern, final String action, 
            final String specName) {
        super(modifiers, name, definitionModifiers, preprocessDef(definition), "",
              preprocessAction(action));
        
        this.topic = topic;
        this.msgType = msgType;
        this.pattern = pattern;
        this.specName = specName;

        parameterize(preprocessDef(definition));
        initPublishKeywordEvents(replaceMESSAGE(action));
    }

    private static String preprocessDef(String definition) {
        // get rid of multiline comments!!
        return definition.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)","");
    }

    private static String preprocessAction(String action) {
        String modifiedAction = replaceMESSAGE(action);
        String result = createEventOutOfPublish(modifiedAction);
        return result;
    }

    private void parameterize(String definition) {
        String tmp = definition.substring(1, definition.length()-1);
        String[] vars = tmp.trim().split(",");
        if(!tmp.isEmpty()){
            this.parameters = new ArrayList<Variable>();
            for (String string : vars) {
                //                System.out.println(string.trim());
                if(string.trim().startsWith("//")) continue;
                this.parameters.add(new Variable(string.trim()));
            }
        }
    }

    private static String replaceMESSAGE(String action){
        String preproc = action, accum = "", message = null;
        int i1 = 0;
        String result = action;

        while(i1 < preproc.length()){
            String st = preproc.substring(i1);
            if(st.indexOf("MESSAGE") != -1){ 
                accum += st.substring(0, st.indexOf("MESSAGE"));
                i1 += st.indexOf("MESSAGE");

                //the MESSAGE keyword
                message = preproc.substring(i1, preproc.indexOf(";", i1)+1);
                //                    System.out.println(message);

                accum += GeneratorUtil.MONITOR_COPY_MSG_NAME + ";";
                //                    System.out.println(accum);
                i1 += message.length();
            } else break;
        }

        if(!accum.equals("")) {
            result = accum + preproc.substring(i1);
        }

        return result;
    }

    private static String createEventOutOfPublish(String action){
        String preproc = action, publish, message, serialize, accum = "", topic, msgType;
        int i1 = 0, count = 1, i2, i3;

        while(i1 < preproc.length()){

            String st = preproc.substring(i1);
            //            System.out.println(st);
            if(st.indexOf("PUBLISH") != -1){ 
                accum += st.substring(0, st.indexOf("PUBLISH"));
                i1 += st.indexOf("PUBLISH");

                //the whole PUBLISH statement (whole line)
                publish = preproc.substring(i1, preproc.indexOf(";", i1)+1);
                //                System.out.println("=================="+publish);

                i2 = publish.indexOf(",");
                //topic name
                //                topic = publish.substring(publish.indexOf("(\"")+2, i2);
                topic = publish.substring(publish.indexOf("(")+1, i2);
                topic = topic.trim();
                topic = topic.replaceAll("\"", "");
                //                System.out.println(topic);

                i3 = publish.lastIndexOf(",")+1;
                //message variable
                message = publish.substring(i3, publish.lastIndexOf(")"));
                message = message.trim();
//                                System.out.println("***"+message);

                //message type
                msgType = publish.substring(i2+1, i3-1);
                msgType = msgType.trim();
                msgType = msgType.replaceAll("\"", "");
                //                System.out.println(msgType);

                serialize = "ros::SerializedMessage serializedMsg" + count 
                        + " = ros::serialization::serializeMessage(" + message + ");\n" 
                        + GeneratorUtil.SERVERMANAGER_PTR_NAME + "->publish(\"" + topic 
                        + "\", serializedMsg" + count +");";

                accum += serialize;

                i1 += publish.length();
                count++;
            } else break;
        }

        String result = accum + preproc.substring(i1);
        return result;
        //        System.out.println(this.content);
    }

    private void initPublishKeywordEvents(String action) {
        String preproc = action, publish, message, serialize, accum = "", topic, msgType;
        int i1 = 0, count = 1, i2, i3;

        this.publishKeywordEvents = new ArrayList<ROSEvent>();

        while(i1 < preproc.length()){

            String st = preproc.substring(i1);
            //            System.out.println(st);
            if(st.indexOf("PUBLISH") != -1){ 
                accum += st.substring(0, st.indexOf("PUBLISH"));
                i1 += st.indexOf("PUBLISH");

                //the whole PUBLISH statement (whole line)
                publish = preproc.substring(i1, preproc.indexOf(";", i1)+1);
                //                System.out.println("=================="+publish);

                i2 = publish.indexOf(",");
                //topic name
                //                topic = publish.substring(publish.indexOf("(\"")+2, i2);
                topic = publish.substring(publish.indexOf("(")+1, i2);
                topic = topic.trim();
                topic = topic.replaceAll("\"", "");
                //                System.out.println(topic);

                i3 = publish.lastIndexOf(",")+1;
                //message variable
                message = publish.substring(i3, publish.lastIndexOf(")"));
                message = message.trim();
//                                System.out.println("***"+message);

                //message type
                msgType = publish.substring(i2+1, i3-1);
                msgType = msgType.trim();
                msgType = msgType.replaceAll("\"", "");
                //                System.out.println(msgType);

                ROSEvent pubevent = new ROSEvent(new ArrayList<String>(), "publish"+message+count, 
                        new ArrayList<String>(), "()", topic, msgType.replace("::", "/"), 
                        new HashMap<String, String>(), "{}", specName);
                this.publishKeywordEvents.add(pubevent);

                serialize = "ros::SerializedMessage serializedMsg" + count 
                        + " = ros::serialization::serializeMessage(" + message + ");\n" 
                        + GeneratorUtil.SERVERMANAGER_PTR_NAME + "->publish(\"" + topic 
                        + "\", serializedMsg" + count +");";

                accum += serialize;

                i1 += publish.length();
                count++;
            } else break;
        }
    }

    public String classifyMsgType() {
        return msgType.replace("/", "::");
    }

    public String getTopic() {
        return topic;
    }

    public String getMsgType() {
        return msgType;
    }

    public HashMap<String, String> getPattern() {
        return pattern;
    }

    public String getSpecName() {
        return specName;
    }

    public List<Variable> getParameters() {
        return parameters;
    }

    public List<ROSEvent> getPublishKeywordEvents() {
        return publishKeywordEvents;
    }
}
