package rosmop.parser.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import rosmop.codegen.GeneratorUtil;

/**
 * A logic property in the specification with handlers.
 * @author A. Cody Schuffelen
 */
public class Property {

	private final String name;
	private final String syntax;
	private final List<PropertyHandler> handlers;

	private final String specName;

	private List<Event> publishKeywordEvents = null;

	/**
	 * Construct the Property out of its component elements.
	 * @param name The logic name of the property (e.g. ere, fsm).
	 * @param syntax The code describing the property.
	 * @param handlers Handlers used to respond to states in the property.
	 */
	public Property(final String name, final String syntax, final List<PropertyHandler> handlers, 
			final String specName) {
		this.name = name;
		this.syntax = syntax;
		this.handlers = Collections.unmodifiableList(new ArrayList<PropertyHandler>(handlers));
		this.specName = specName;

		createEventOutOfPublish();
	}

	private void createEventOutOfPublish(){

		for (PropertyHandler propertyHandler : handlers) {
			String preproc = propertyHandler.getAction(), publish, message, serialize, 
					accum = "", topic, msgType;
			int i1 = 0, count = 1, i2, i3;

			while(i1 < preproc.length()){
				if(publishKeywordEvents == null)
					publishKeywordEvents = new ArrayList<Event>();

				String st = preproc.substring(i1);
				//			System.out.println(st);
				if(st.indexOf("PUBLISH") != -1){ 
					accum += st.substring(0, st.indexOf("PUBLISH"));
					i1 += st.indexOf("PUBLISH");

					//the whole PUBLISH statement (whole line)
					publish = preproc.substring(i1, preproc.indexOf(";", i1)+1);
					//				System.out.println("=================="+publish);

					i2 = publish.indexOf(",");
					//topic name
					//				topic = publish.substring(publish.indexOf("(\"")+2, i2);
					topic = publish.substring(publish.indexOf("(")+1, i2);
					topic = topic.trim();
					topic = topic.replaceAll("\"", "");
					//				System.out.println(topic);

					i3 = publish.lastIndexOf(",")+1;
					//message variable
					message = publish.substring(i3, publish.lastIndexOf(")"));
					message = message.trim();
					//				System.out.println("***"+message);

					//message type
					msgType = publish.substring(i2+1, i3-1);
					msgType = msgType.trim();
					msgType = msgType.replaceAll("\"", "");
					//				System.out.println(msgType);

					Event pubevent = new Event(new ArrayList<String>(), "publish"+message+count, 
							new ArrayList<String>(), "()", topic, msgType.replace("::", "/"), 
							new HashMap<String, String>(), "{}", specName);
					publishKeywordEvents.add(pubevent);

					serialize = "ros::SerializedMessage serializedMsg" + count 
							+ " = ros::serialization::serializeMessage(" + message + ");\n" 
							+ GeneratorUtil.SERVERMANAGER_PTR_NAME + "->publish(\"" + topic 
							+ "\", serializedMsg" + count +");";

					accum += serialize;

					i1 += publish.length();
					count++;
				} else break;
			}

			propertyHandler.setAction(accum + preproc.substring(i1));
//			System.out.println(propertyHandler.getAction());
		}
	}

	/**
	 * The logic name of the property.
	 * @return The name of the logic repository plugin used in the property.
	 */
	public String getName() {
		return name;
	}

	/**
	 * The expression describing the property.
	 * @return The property logic formula.
	 */
	public String getSyntax() {
		return syntax;
	}

	/**
	 * An unmodifiable list of the handlers for the different states of the property.
	 * @return A list of state handlers.
	 */
	public List<PropertyHandler> getHandlers() {
		return handlers;
	}

	public List<Event> getPublishKeywordEvents() {
		return publishKeywordEvents;
	}

	public String getSpecName() {
		return specName;
	}
}
