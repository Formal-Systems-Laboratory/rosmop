package rosmop.codegen;

import com.runtimeverification.rvmonitor.c.rvc.CSpecification;
import com.runtimeverification.rvmonitor.logicpluginshells.LogicPluginShellResult;
import org.junit.*;
import org.apache.commons.io.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import rosmop.ROSMOPException;
import rosmop.RVParserAdapter;
import rosmop.parser.ast.MonitorFile;
import rosmop.parser.main_parser.ROSMOPParser;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;


public class CppGeneratorTest {


   private File processFileNameFromResources(String specFileName) {
       ClassLoader classLoader = getClass().getClassLoader();
       File file = new File(classLoader.getResource(specFileName).getFile());
       if(file == null) {
           fail("File " + specFileName + " absent or invoker doesn't have adequate privileges to get the resource");
       }
       return file;
   }
   private void testOutputFiles(File specFile, boolean monitorAsNode) {
       String generatedCpp, expectedCpp;
       try {
           if(monitorAsNode) {
               generatedCpp = FileUtils.readFileToString(
                       FileUtils.getFile(getAbsoluteFilePath(specFile, "-isolated-generated.cpp"))
                       , StandardCharsets.UTF_8);

               expectedCpp = FileUtils.readFileToString(
                       FileUtils.getFile(getAbsoluteFilePath(specFile, "-isolated-expected.cpp"))
                       , StandardCharsets.UTF_8);
           } else {
               generatedCpp = FileUtils.readFileToString(
                       FileUtils.getFile(getAbsoluteFilePath(specFile, "-complete-generated.cpp"))
                       , StandardCharsets.UTF_8);

               expectedCpp = FileUtils.readFileToString(
                       FileUtils.getFile(getAbsoluteFilePath(specFile, "-complete-expected.cpp"))
                       , StandardCharsets.UTF_8);
           }
           assertThat(generatedCpp).isEqualToIgnoringWhitespace(expectedCpp);
       } catch (IOException e) {
           fail(e.getMessage());
       }
   }

   private String getAbsoluteFilePath(File specFile, String post) {
       return specFile.getAbsolutePath().replace(".rv", post );
   }

   private void simpleTestRunWithParams(String specFileName, boolean monitorAsNode) {
       File file = processFileNameFromResources(specFileName);
       MonitorFile parsedFile = ROSMOPParser.parse(file.getAbsolutePath());
       CSpecification cSpecification = new RVParserAdapter(parsedFile);
       HashMap<CSpecification, LogicPluginShellResult> map = new HashMap();
       map.put(cSpecification, null);
       try {
           if(monitorAsNode) {
               HeaderGenerator.generateHeader(map, getAbsoluteFilePath(file, "-isolated-generated.h"), true);
               CppGenerator.generateCpp(map, getAbsoluteFilePath(file, "-isolated-generated.cpp"), true);
           } else {
               HeaderGenerator.generateHeader(map, getAbsoluteFilePath(file, "-complete-generated.h"),false);
               CppGenerator.generateCpp(map, getAbsoluteFilePath(file, "-complete-generated.cpp"), false);
           }
           testOutputFiles(file, monitorAsNode);
       } catch (FileNotFoundException | ROSMOPException e) {
           fail(e.getMessage());
       }

   }
   @Before
   public void resetGenerators(){
       HeaderGenerator.reset();
       CppGenerator.reset();
   }

    @Test
    public void simpleSpecIsolatedNode() {
      simpleTestRunWithParams("simple-spec.rv", true);
    }

    @Test
    public void simpleSpecCppRVMaster() {
       simpleTestRunWithParams("simple-spec.rv", false);
    }
}
