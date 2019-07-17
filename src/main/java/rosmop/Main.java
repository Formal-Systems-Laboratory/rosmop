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

import org.apache.commons.cli.*;

import com.runtimeverification.rvmonitor.c.rvc.CSpecification;
import com.runtimeverification.rvmonitor.logicpluginshells.LogicPluginShell;
import com.runtimeverification.rvmonitor.logicpluginshells.LogicPluginShellResult;
import com.runtimeverification.rvmonitor.logicpluginshells.cfg.CCFG;
import com.runtimeverification.rvmonitor.logicpluginshells.dl.CDL;
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
import rosmop.parser.ast.ROSEvent;
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

    static String logicPluginDirPath;

    private static boolean monitorAsRosNode = false;

    /**
     *  Possible parameters:
     *  1- only one .rv file
     *  2- a list of .rv files
     *  3- a directory of .rv files -- not recursive, there shouldn't be any other files in the
     *  directory
     * @param args One or list of .rv file(s)
     */
    public static void main(String[] argv)
        throws ROSMOPException, LogicException, RVMException, java.io.IOException
    {
        Options options = new Options();
        options.addOption("n", "monitor-as-node", false, "Generated monitor can be run as a stand-alone ROS node.");
        options.addOption(Option.builder("o")
                                .withLongOpt("output-prefix")
                                .withDescription("Path-prefix of generated output files. Full path without extension.")
                                .hasArg().withArgName("path-prefix")
                                .create()
                         );
        CommandLineParser parser = new DefaultParser();
        CommandLine line = null; 
        try { line = parser.parse(options, argv); }
        catch (ParseException e) {
            System.err.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("rosmop", options);
            System.exit(1);
        }
        monitorAsRosNode = line.hasOption("monitor-as-node");
        String outputPrefix = null;
        if (line.hasOption("output-prefix")) {
            outputPrefix = line.getOptionValue("output-prefix");
        }

        List<File> rvFiles = expandDirectories(line.getArgList());
        checkArguments(rvFiles);
        List<MonitorFile> parsed = parseRVFiles(rvFiles);
        logicPluginDirPath = readLogicPluginDir();
        if (outputPrefix == null) {
            outputPrefix = rvFiles.get(0).getParentFile().getAbsolutePath() + "/rvmonitor";
        } 
        process(parsed, outputPrefix);
    }

    // Replace each directory in the input args with the list of files it contains
    private static List<File> expandDirectories(List<String> inputFiles)
        throws java.io.IOException
    {
        List<File> rvFiles = new ArrayList<File>();
        for (String p : inputFiles) {
            File f = new File(p);
            if (f.isDirectory()) {
                rvFiles.addAll(Arrays.asList(f.listFiles()));
            }
            else {
                rvFiles.add(f);
            }
        }
        return rvFiles;
    }

    /**
     * Wraps the parsed monitor files as CSpecifications to send them to logic repository
     * (unless raw monitor) and then output the .h and .cpp files
     * @param readyMonitors A list of MonitorFiles which are parsed specifications
     */
    private static void process(List<MonitorFile> readyMonitors, String pathToOutputNoExt)
        throws java.io.IOException, LogicException, RVMException
    {
        HashMap<CSpecification, LogicRepositoryData> rvcParser =
                new HashMap<CSpecification, LogicRepositoryData>();
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
    }

    /**
     * Takes a list of .rv files and sends them to the parser
     * Once all the specification files are parsed, makes sure there are no duplicate
     * event names or field declarations, and if so sends them to {@link Main#process(List)}
     * @param args Takes a list of .rv files
     */
    private static List<MonitorFile> parseRVFiles(List<File> args)
        throws ROSMOPException
    {
        Set<String> events = new HashSet<String>();
        Set<String> declarations = new HashSet<String>();
        List<MonitorFile> readyMonitors = new ArrayList<MonitorFile>();

        for (File arg : args) {
            MonitorFile f = ROSMOPParser.parse(arg.toString());

            /* In case of multiple .rv files as input, all of the specifications
             * are gathered and checked for duplicate event names and field declarations;
             * they should have unique names.*/
            for (Specification spec : f.getSpecifications()) {
                for (ROSEvent event : spec.getEvents()) {
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
        return readyMonitors;
    }

    /**
     * Makes sure the provided input(s) is/are valid
     * @param args .rv file names
     * @return True if all provided file names end with .rv, false otherwise
     */
    private static void checkArguments(List<File> args)
        throws ROSMOPException
    {
        if (args.isEmpty()) {
            throw new ROSMOPException("Make sure you provide at least "
                    + "one .rv file or a folder of .rv files.");
        }
        for (File arg : args) {
            if(!Tool.isSpecFile(arg.toString())){
                throw new ROSMOPException(
                              "Unrecognized file type for '" + arg.toString() + "'. "
                            + "The ROSMOP specification file "
                            + "should have .rv as the extension."
                            );
            }
        }
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
            String outputPath)
        throws LogicException, FileNotFoundException, RVMException, java.io.IOException
    {
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
            HeaderGenerator.generateHeader(toWrite, outputPath+".h", monitorAsRosNode);
            CppGenerator.generateCpp(toWrite, outputPath+".cpp", monitorAsRosNode);
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
        else if("dl".equals(logic)) {
            shell = new CDL((com.runtimeverification.rvmonitor.c.rvc.CSpecification) rvcParser,
                    parametric);
        }
        else {
            throw new LogicException("Only finite logics and CFG are currently supported");
        }

        return shell.process(logicOutputXML, logicOutputXML.getEvents());
    }
}
