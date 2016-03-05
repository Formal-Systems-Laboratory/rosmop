package rosmop.util;

import java.io.File;
import java.io.FileWriter;

import rosmop.ROSMOPException;

/**
 * @author Cansu Erdogan
 *
 * ROSMOP utility class
 * 
 */
public class Tool {
	
	/**
	 * Checks whether the given file name ends with .rv (proper specification file extension)
	 * @param path Name of an input file
	 * @return True if file name ends with .rv, false otherwise
	 */
	public static boolean isSpecFile(String path){
		return path.endsWith(".rv");
	}

	/**
	 * Extracts file name from file path
	 * @param path File path
	 * @return File name
	 */
	public static String getFileName(String path){
		int i = path.lastIndexOf(File.separator);
		return path.substring(i+1, path.length());        
	}

	/**
	 * Outputs the generated code content in a file in given location 
	 * @param content .cpp or .h monitor file content
	 * @param location Output location of rvmonitor.h/.cpp files
	 * @throws ROSMOPException
	 */
	public static void writeFile(String content, String location) throws ROSMOPException {
		if (content == null || content.length() == 0)
			return;

		try {
			FileWriter f = new FileWriter(location);
			f.write(content);
			f.close();
		} catch (Exception e) {
			throw new ROSMOPException(e.getMessage());
		}

		System.out.println(Tool.getFileName(location) + " is generated");
	}
}