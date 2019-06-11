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

   private void testOutputFiles(File specFile) {
       try {
           String generatedCpp = FileUtils.readFileToString(
                           FileUtils.getFile(getAbsoluteFilePath(specFile, "-generated.cpp"))
                           , StandardCharsets.UTF_8);

           String expectedCpp = FileUtils.readFileToString(
                   FileUtils.getFile(getAbsoluteFilePath(specFile, "-expected.cpp"))
                   , StandardCharsets.UTF_8);

           assertThat(generatedCpp).isNotEqualToIgnoringWhitespace(expectedCpp);
       } catch (IOException e) {
           fail(e.getMessage());
       }
   }

   private String getAbsoluteFilePath(File specFile, String post) {
       return specFile.getAbsolutePath().replace(".rv", post );
   }

    @Test
    public void simpleSpecCpp() {
       File file = processFileNameFromResources("simple-spec.rv");
       MonitorFile parsedFile = ROSMOPParser.parse(file.getAbsolutePath());
       CSpecification cSpecification = new RVParserAdapter(parsedFile);
       HashMap<CSpecification, LogicPluginShellResult> map = new HashMap();
       map.put(cSpecification, null);
       try {
           System.out.println(file.getAbsolutePath());
           HeaderGenerator.generateHeader(map, getAbsoluteFilePath(file, "-generated.h"), true);
           CppGenerator.generateCpp(map, getAbsoluteFilePath(file, "-generated.cpp"), true);
           testOutputFiles(file);
       } catch (FileNotFoundException | ROSMOPException e) {
           fail(e.getMessage());
       }
    }
}
