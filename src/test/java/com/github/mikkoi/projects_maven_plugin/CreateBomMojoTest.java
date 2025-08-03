package com.github.mikkoi.projects_maven_plugin;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class CreateBomMojoTest {
    @Test
    public void testWritePOMFile() {
        Model model = new Model();
        final Path path = Paths.get("tmp-pom.xml");
        try {
            CreateBomMojo.writePOM(path, model);
            assertThat(Files.exists(path)).isTrue();
            Files.delete(path);
        } catch (IOException e) {
            assertThat(false).isTrue().describedAs("Test fails");
        }
    }
}
