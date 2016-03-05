package rosmop.util;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Cansu Erdogan
 *
 * Calls "rosmsg show <message>" by creating an external process
 * Gets the fields of a ROS message so they can be pattern-matched with provided patterns in events
 * 
 */
public class MessageParser {

//	public static void main(String[] args) {
//		parseMessage(args[0]);
//	}

	public static Map<String, String> parseMessage(String msgName) {
		Map<String, String> msgMapping = new HashMap<String, String>();
		try {
			Process tr = Runtime.getRuntime().exec(new String[] { "rosmsg", "show", msgName });
			BufferedReader rd = new BufferedReader(new InputStreamReader(tr.getInputStream()));
			String message = rd.readLine();
			while (message != null) {
				// get first level fields
				if (!message.startsWith("  ") && Pattern.matches(".*[a-zA-Z]+.*", message)) {
					Map.Entry<String, String> entry = parseSingleMessage(message);
					if (!entry.getKey().contains("=")) {
						// ignore constant fields
						msgMapping.put(entry.getKey(), entry.getValue());
					}
				}
				message = rd.readLine();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return msgMapping;
	}
	
	public static Map.Entry<String, String> parseSingleMessage(String msg){
		String[] results = msg.split(" ");
		Map.Entry<String, String> entry =
			    new AbstractMap.SimpleEntry<String, String>(results[1], results[0]);
		return entry;
	}
}
