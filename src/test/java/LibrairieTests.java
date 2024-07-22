import org.example.Main;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

class LibrairieTests {

    @Test
    void getVersion() {
        Assertions.assertEquals("1.0.2", Main.getVersion("C:\\Users\\a926932\\.m2\\repository_sae\\com\\worldline\\edoc\\edoc-reporting-core\\1.0.2"));
    }

    @Test
    void getArtifactId() {
        Assertions.assertEquals("stos-core", Main.getArtifactId("stos-core-2.2.6.jar", "2.2.6"));
        Assertions.assertEquals("stos-ws", Main.getArtifactId("stos-ws-2.2.6.pom", "2.2.6"));
        Assertions.assertEquals("spring-boot-starter-edoc-test", Main.getArtifactId("spring-boot-starter-edoc-test-2.6.1.jar", "2.6.1"));
    }


    @Test
    void filterUnwantedPomFiles() {
        Assertions.assertEquals(Optional.of("stos-ws-2.2.6.pom"), Main.filterUnwantedPomFiles(List.of("stos-ws-2.2.6-test-sources.pom", "stos-ws-2.2.6-sources.pom", "stos-ws-2.2.6.pom", "edoc-reporting-starter-2.2.6.pom.sha1"), "2.2.6"));
    }

    @Test
    void filterUnwantedJarFiles() {
        Assertions.assertEquals(Optional.of("stos-ws-2.2.6.jar"), Main.filterUnwantedJarFiles(List.of("stos-ws-2.2.6-test-sources.jar", "stos-ws-2.2.6-sources.jar", "stos-ws-2.2.6.jar", "edoc-reporting-starter-2.2.6.jar.sha1"), "2.2.6"));
    }


    @Test
    void getGroupId() {
        Assertions.assertEquals("com.worldline.edoc", Main.getGroupId("C:/Users/a926932/.m2/repository_sae", "C:/Users/a926932/.m2/repository_sae/com/worldline/edoc/edoc-reporting-core/1.0.2"));
        Assertions.assertEquals("com.worldline.edoc.yolo", Main.getGroupId("C:/Users/a926932/.m2/repository_sae", "C:/Users/a926932/.m2/repository_sae/com/worldline/edoc/yolo/edoc-reporting-core/1.0.2"));
        Assertions.assertEquals("commons-codec", Main.getGroupId("C:/Users/a926932/.m2/repository_sae", "C:/Users/a926932/.m2/repository_sae/commons-codec/commons-codec/1.2"));
    }
}
