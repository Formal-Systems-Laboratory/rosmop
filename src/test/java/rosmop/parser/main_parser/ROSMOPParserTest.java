package rosmop.parser.main_parser;

import org.junit.BeforeClass;
import org.junit.Test;
import rosmop.parser.ast.MonitorFile;
import rosmop.parser.ast.Property;
import rosmop.parser.ast.ROSEvent;
import rosmop.parser.ast.Specification;
import rosmop.util.ResourceUtils;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

public class ROSMOPParserTest {

    static ResourceUtils resourceUtils;

    @BeforeClass
    public static void setup() {
        resourceUtils = new ResourceUtils();
    }

    @Test
    public void simpleSpecParserTest() {
        try{
            File specFile = resourceUtils.processFileNameFromResources("simple-spec.rv");
            MonitorFile monitorFile = ROSMOPParser.parse(specFile.getAbsolutePath());

            assertThat(monitorFile.getSpecifications().size()).isEqualTo(1);

            Specification spec = monitorFile.getSpecifications().get(0);
            assertThat(spec.getEvents().size()).isEqualTo(1);
            assertThat(spec.getProperties()).isEmpty();

            ROSEvent event = spec.getEvents().get(0);
            assertThat(event.getTopic()).isEqualTo("/test");
            assertThat(event.getMsgType()).isEqualTo("std_msgs/Float64");

        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void simpleDlSpecParserTest() {
        try{
            File specFile = resourceUtils.processFileNameFromResources("simple-dl-spec.rv");
            MonitorFile monitorFile = ROSMOPParser.parse(specFile.getAbsolutePath());

            assertThat(monitorFile.getSpecifications().size()).isEqualTo(1);

            Specification spec = monitorFile.getSpecifications().get(0);
            assertThat(spec.getProperties()).isNotEmpty();
            Property property = spec.getProperties().get(0);

            assertThat(property.getName()).isEqualTo("dL");
            assertThat(property.getHandlers()).isNotEmpty();
            assertThat(property.getHandlers().get(0).getAction()).isNotBlank();

        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

}
