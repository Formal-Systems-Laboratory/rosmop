package rosmop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.runtimeverification.rvmonitor.c.rvc.CSpecification;
import com.runtimeverification.rvmonitor.logicpluginshells.LogicPluginShell;
import com.runtimeverification.rvmonitor.logicpluginshells.LogicPluginShellResult;
import com.runtimeverification.rvmonitor.logicpluginshells.cfg.CCFG;
import com.runtimeverification.rvmonitor.logicpluginshells.fsm.CFSM;
import com.runtimeverification.rvmonitor.logicpluginshells.tfsm.CTFSM;
import com.runtimeverification.rvmonitor.logicrepository.LogicException;
import com.runtimeverification.rvmonitor.logicrepository.LogicRepositoryData;
import com.runtimeverification.rvmonitor.logicrepository.parser.logicrepositorysyntax.LogicRepositoryType;
import com.runtimeverification.rvmonitor.logicrepository.parser.logicrepositorysyntax.PropertyType;
import com.runtimeverification.rvmonitor.logicrepository.plugins.LogicPluginFactory;
import com.runtimeverification.rvmonitor.util.RVMException;

import rosmop.codegen.CppGenerator;
import rosmop.codegen.HeaderGenerator;
import rosmop.parser.ast.Event;
import rosmop.parser.ast.MonitorFile;
import rosmop.parser.ast.Specification;
import rosmop.parser.ast.Variable;
import rosmop.parser.main_parser.ROSMOPParser;
import rosmop.util.Tool;

/**
 * @author Cansu Erdogan
 * 
 * Entry point when calling ROSMOP.jar
 *
 */
public class Main {

	static String logicPluginDirPath, pathToOutputNoExt;

	/**
	 *	Possible parameters:
	 *	1- only one .rv file
	 *	2- a list of .rv files
	 *	3- a directory of .rv files -- not recursive, there shouldn't be any other files in the 
	 *	directory
	 * @param args One or list of .rv file(s)
	 */
	public static void main(String[] args) {
		try {

			if(args.length == 0){
				throw new ROSMOPException("Make sure you provide at least "
						+ "one .rv file or a folder of .rv files.");
			}

			logicPluginDirPath = readLogicPluginDir();

			String pathToFile = "";
			File fileToGetPath;
			MonitorFile readyToProcess;
			/*
			1- "." means current directory (all .rv files in the directory)
			2- if it ends with "/" or "\" process the given directory (all .rv files in the 
				directory)
			3- one .rv file
			 */
			if (args.length == 1) {
				if(args[0].equalsIgnoreCase(".")){
					fileToGetPath = new File(System.getProperty("user.dir"));
					pathToFile = fileToGetPath.getAbsolutePath();
					//					/home/cans-u/workspace/rosmop
					//					System.out.println(pathToFile);
					pathToOutputNoExt = pathToFile + File.separator + "rvmonitor";
					//					System.out.println(pathToOutputNoExt);
					processDirOfFiles(pathToFile);
				} else if(args[0].endsWith(File.separator)){
					fileToGetPath = new File(args[0]);
					pathToFile = fileToGetPath.getAbsolutePath();
					//					/home/cans-u/Desktop
					//					System.out.println(pathToFile);
					pathToOutputNoExt = pathToFile + File.separator + "rvmonitor";
					//					System.out.println(pathToOutputNoExt);
					processDirOfFiles(pathToFile);
				} else {
					if (!checkArguments(args)) {
						throw new ROSMOPException("Unrecognized file type! The ROSMOP "
								+ "specification file should have .rv as the extension.");
					}
					fileToGetPath = new File(args[0]);
					pathToFile = fileToGetPath.getAbsolutePath();
					//					/home/cans-u/Desktop/deneme.rv
					//					System.out.println(pathToFile);
					pathToOutputNoExt = pathToFile.substring(0, 
							pathToFile.lastIndexOf(File.separator)+1) + "rvmonitor";
					//					System.out.println(pathToOutputNoExt);
					readyToProcess = ROSMOPParser.parse(pathToFile);
					List<MonitorFile> readyMonitor = new ArrayList<MonitorFile>();
					readyMonitor.add(readyToProcess);
					process(readyMonitor);
				}
			}
			/*
			 * multiple .rv files 
			 */
			else {
				// output file is going to be written in the first file's dir
				fileToGetPath = new File(args[0]);
				pathToFile = fileToGetPath.getAbsolutePath();
				//					/home/cans-u/Desktop/deneme.rv
				//				System.out.println(pathToFile);
				pathToOutputNoExt = pathToFile.substring(0, 
						pathToFile.lastIndexOf(File.separator)+1) + "rvmonitor";
				//				System.out.println(pathToOutputNoExt);
				processMultipleFiles(args);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Wraps the parsed monitor files as CSpecifications to send them to logic repository
	 * (unless raw monitor) and then output the .h and .cpp files 
	 * @param readyMonitors A list of MonitorFiles which are parsed specifications
	 */
	private static void process(List<MonitorFile> readyMonitors){
		HashMap<CSpecification, LogicRepositoryData> rvcParser = 
				new HashMap<CSpecification, LogicRepositoryData>();

		try {
			for (MonitorFile mf : readyMonitors) {
				CSpecification cspec = (CSpecification) new RVParserAdapter(mf);
				//raw monitor
				if(cspec.getFormalism() != null){
					LogicRepositoryData cmgDataOut = 
							sendToLogicRepository(cspec, logicPluginDirPath);
					rvcParser.put(cspec, cmgDataOut);
				} else
					rvcParser.put(cspec, null);
			}

			outputCode(rvcParser, pathToOutputNoExt); 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** 
	 * Takes a list of .rv files and sends them to the parser
	 * Once all the specification files are parsed, makes sure there are no duplicate
	 * event names or field declarations, and if so sends them to {@link Main#process(List)}
	 * @param args Takes a list of .rv files
	 */
	private static void processMultipleFiles(String[] args) {
		Set<String> events = new HashSet<String>();
		Set<String> declarations = new HashSet<String>();
		List<MonitorFile> readyMonitors = new ArrayList<MonitorFile>();
		try {
			if (!checkArguments(args)) {
				throw new ROSMOPException("Unrecognized file type! The ROSMOP specification "
						+ "file should have .rv as the extension.");
			}
			for (String arg : args) {
				MonitorFile f = ROSMOPParser.parse(arg);

				/* In case of multiple .rv files as input, all of the specifications 
				 * are gathered and checked for duplicate event names and field declarations;
				 * they should have unique names.*/
				for (Specification spec : f.getSpecifications()) {
					for (Event event : spec.getEvents()) {
						if (!events.add(event.getName())) 
							throw new ROSMOPException("Duplicate event names");
					}

					for (Variable var : spec.getSpecDeclarations()) {
						if(!declarations.add(var.getDeclaredName())) 
							throw new ROSMOPException("Duplicate field declarations");
					}
				}

				readyMonitors.add(f);
			}

			process(readyMonitors);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Called when the input is a directory (ends with a file separator)
	 * After handling directory structure, calls {@link Main#processMultipleFiles(String[])}
	 * 
	 * @param arg Name of the input directory
	 * @throws ROSMOPException
	 */
	private static void processDirOfFiles(String arg) throws ROSMOPException {
		try {
			File folder = new File(arg);
			File[] listOfFiles = folder.listFiles();

			//get rid of hidden files/folders & directories in the folder
			//(won't be processed)
			ArrayList<File> onlyFiles = new ArrayList<File>(Arrays.asList(listOfFiles));
			for (int i = 0; i < onlyFiles.size(); i++) {
				if(onlyFiles.get(i).isDirectory() || onlyFiles.get(i).getName().startsWith(".")){
					onlyFiles.remove(i);
				}
			}

			File noDirs[] = new File[onlyFiles.size()];
			noDirs = onlyFiles.toArray(noDirs);
			String fileNames[] = new String[noDirs.length];
			for (int i = 0; i < fileNames.length; i++) {
				fileNames[i] = noDirs[i].getAbsolutePath();
			}

			processMultipleFiles(fileNames);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Makes sure the provided input(s) is/are valid
	 * @param args .rv file names
	 * @return True if all provided file names end with .rv, false otherwise
	 */
	private static boolean checkArguments(String[] args) {
		if(args.length == 0) return false;
		for (String arg : args) {
			if(!Tool.isSpecFile(arg)){
				return false;
			}
		}
		return true;
	}

	/**
	 * Makes sure the LOGICPLUGINPATH environment variable is set.
	 * @return The location of the logic plugins
	 */
	static public String readLogicPluginDir() {
		String logicPluginDirPath = System.getenv("LOGICPLUGINPATH");
		if (logicPluginDirPath == null || logicPluginDirPath.length() == 0) {
			try {
				throw new LogicException(
						"Unrecoverable error: please set LOGICPLUGINPATH variable to refer to "
						+ "the plugins directory");
			} catch (LogicException e) {
				e.printStackTrace();
			}
		}

		return logicPluginDirPath;
	}

	/**
	 * Sends the specification to the logic repository. 
	 * The appropriate logic repository plugins are run on the code.
	 * @param rvcParser Wrapped AST classes of parsed specification file 
	 * @param logicPluginDirPath The location at which to find the logic repository plugins
	 * @return The output of the logic plugins
	 */
	static public LogicRepositoryData sendToLogicRepository(CSpecification rvcParser, 
			String logicPluginDirPath) throws LogicException {
		LogicRepositoryType cmgXMLIn = new LogicRepositoryType();
		PropertyType logicProperty = new PropertyType();

		// Get Logic Name and Client Name
		String logicName = rvcParser.getFormalism();
		if (logicName == null || logicName.length() == 0) {
			throw new LogicException("no logic names");
		}

		cmgXMLIn.setSpecName(rvcParser.getSpecName());

		logicProperty.setFormula(rvcParser.getFormula());
		logicProperty.setLogic(logicName);

		cmgXMLIn.setClient("CMonGen");
		StringBuilder events = new StringBuilder();
		for(String event : rvcParser.getEvents().keySet()){
			events.append(event);
			events.append(" ");
		}
		cmgXMLIn.setEvents(events.toString().trim());

		StringBuilder categories = new StringBuilder();
		for(String category : rvcParser.getHandlers().keySet()){
			categories.append(category);
			categories.append(" ");
		}
		cmgXMLIn.setCategories(categories.toString().trim());

		PropertyType prop = new PropertyType();
		prop.setLogic(rvcParser.getFormalism());
		prop.setFormula(rvcParser.getFormula());

		cmgXMLIn.setProperty(prop);

		LogicRepositoryData cmgDataIn = new LogicRepositoryData(cmgXMLIn);

		// Find a logic plugin and apply it
		ByteArrayOutputStream logicPluginResultStream 
		= LogicPluginFactory.process(logicPluginDirPath, logicName, cmgDataIn);

		// Error check
		if (logicPluginResultStream == null || logicPluginResultStream.size() == 0) {
			throw new LogicException("Unknown Error from Logic Plugins");
		}
		return new LogicRepositoryData(logicPluginResultStream);
	}

	/**
	 * Generates .h and .cpp files from the final monitor specification objects.
	 * @param rvcParser Map of wrapped specifications and their logic plugin results
	 * @param outputPath The location to output the generated monitoring code
	 * @throws LogicException
	 * @throws FileNotFoundException
	 * @throws RVMException
	 */
	static private void outputCode(HashMap<CSpecification, LogicRepositoryData> rvcParser, 
			String outputPath) throws LogicException, FileNotFoundException, RVMException {
		HashMap<CSpecification, LogicPluginShellResult> toWrite = 
				new HashMap<CSpecification, LogicPluginShellResult>();
		
		for (CSpecification cspec : rvcParser.keySet()) {
			if(rvcParser.get(cspec) != null){
				LogicRepositoryType logicOutputXML = rvcParser.get(cspec).getXML();
				LogicPluginShellResult sr = evaluateLogicPluginShell(logicOutputXML, cspec, false);
				toWrite.put(cspec, sr);
			} else {
				toWrite.put(cspec, null);
			}
		}

		try {
			HeaderGenerator.generateHeader(toWrite, outputPath+".h");
			CppGenerator.generateCpp(toWrite, outputPath+".cpp");
		} catch (ROSMOPException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Evaluates the appropriate logic plugin shell on the logic formalism.
	 * @param logicOutputXML The result of the logic repository plugins
	 * @param rvcParser The extracted information from the monitor specification
	 * @return The result of applying the appropriate logic plugin shell to the parameters
	 * @throws LogicException Something went wrong in applying the logic plugin shell
	 * @throws RVMException 
	 */
	private static LogicPluginShellResult evaluateLogicPluginShell(
			LogicRepositoryType logicOutputXML, CSpecification rvcParser, boolean parametric)
					throws LogicException, RVMException {
		//TODO: make this reflective instead of using a switch over type
		String logic = logicOutputXML.getProperty().getLogic().toLowerCase();
		LogicPluginShell shell;

		if("fsm".equals(logic)) {
			shell = new CFSM((com.runtimeverification.rvmonitor.c.rvc.CSpecification) rvcParser, 
					parametric);
		}
		else if("tfsm".equals(logic)) {
			shell = new CTFSM((com.runtimeverification.rvmonitor.c.rvc.CSpecification) rvcParser, 
					parametric);
		}
		else if("cfg".equals(logic)) {
			shell = new CCFG((com.runtimeverification.rvmonitor.c.rvc.CSpecification) rvcParser, 
					parametric);
		}
		else {
			throw new LogicException("Only finite logics and CFG are currently supported");
		}

		return shell.process(logicOutputXML, logicOutputXML.getEvents());
	}
}
