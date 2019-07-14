package rosmop.codegen;

import com.runtimeverification.rvmonitor.c.rvc.CSpecification;
import com.runtimeverification.rvmonitor.logicpluginshells.LogicPluginShellResult;
import org.junit.*;
import org.apache.commons.io.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import rosmop.ROSMOPException;
import rosmop.RVParserAdapter;
import rosmop.parser.ast.MonitorFile;
import rosmop.parser.main_parser.ROSMOPParser;
import rosmop.util.ResourceUtils;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;


public class CppGeneratorTest {
    static ResourceUtils resourceUtils;

    @BeforeClass
    public static void setup() {
        resourceUtils = new ResourceUtils();
    }


   private void testOutputFiles(File specFile, boolean monitorAsNode) {
       String generatedCpp, expectedCpp;
       try {
           if(monitorAsNode) {
               generatedCpp = FileUtils.readFileToString(
                       FileUtils.getFile(getCppPathForSpec(specFile, "-isolated-generated.cpp"))
                       , StandardCharsets.UTF_8);

               expectedCpp = FileUtils.readFileToString(
                       FileUtils.getFile(getCppPathForSpec(specFile, "-isolated-expected.cpp"))
                       , StandardCharsets.UTF_8);
           } else {
               generatedCpp = FileUtils.readFileToString(
                       FileUtils.getFile(getCppPathForSpec(specFile, "-complete-generated.cpp"))
                       , StandardCharsets.UTF_8);

               expectedCpp = FileUtils.readFileToString(
                       FileUtils.getFile(getCppPathForSpec(specFile, "-complete-expected.cpp"))
                       , StandardCharsets.UTF_8);
           }
           generatedCpp = generatedCpp.replaceAll("[\\h]*\\R", System.lineSeparator());
           assertThat(generatedCpp).isEqualTo(expectedCpp);
       } catch (IOException e) {
           fail(e.getMessage());
       }
   }

   private String getCppPathForSpec(File specFile, String post) {
       String basePath = FilenameUtils.concat(specFile.getParent(), "cpp");
       return FilenameUtils.concat(basePath, specFile.getName().replace(".rv", post));
   }

   private void simpleTestRunWithParams(String specFileName, boolean monitorAsNode) {
       try {
       File file = resourceUtils.processFileNameFromResources(specFileName);
       MonitorFile parsedFile = ROSMOPParser.parse(file.getAbsolutePath());
       CSpecification cSpecification = new RVParserAdapter(parsedFile);
       HashMap<CSpecification, LogicPluginShellResult> map = new HashMap();
       map.put(cSpecification, null);
       if(monitorAsNode) {
           HeaderGenerator.generateHeader(map, getCppPathForSpec(file, "-isolated-generated.h"), true);
           CppGenerator.generateCpp(map, getCppPathForSpec(file, "-isolated-generated.cpp"), true);
       } else {
           HeaderGenerator.generateHeader(map, getCppPathForSpec(file, "-complete-generated.h"),false);
           CppGenerator.generateCpp(map, getCppPathForSpec(file, "-complete-generated.cpp"), false);
       }
       testOutputFiles(file, monitorAsNode);
       } catch (IOException | ROSMOPException e) {
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

    @Test @Ignore("Ignoring Dl Generation Test (WIP)")
    public void simpleDlSpecIsolatedNode() { simpleTestRunWithParams("simple-dl-spec.rv", true);}
}
