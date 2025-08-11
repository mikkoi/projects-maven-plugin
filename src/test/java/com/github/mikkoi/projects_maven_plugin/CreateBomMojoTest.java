package com.github.mikkoi.projects_maven_plugin;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreateBomMojoTest {

    @TempDir
    static Path tempDir;

    @Test
    public void testWritePOMFile() {
        Model model = new Model();
        final Path path = Paths.get(tempDir.toString(), UUID.randomUUID().toString(), "pom.xml");
        try {
            Files.createDirectories(path.getParent());
            CreateBomMojo.writePOM(path, model);
            assertThat(Files.exists(path)).isTrue();
            Files.delete(path);
        } catch (IOException e) {
            assertThat(false).isTrue().describedAs("Test fails");
        }
    }

//    @Test
//    public void testCreateSimpleBom() {
//        Model model = new Model();
//        MavenProject mavenProject = new MavenProject(model);
//        org.codehaus.plexus.PlexusContainer container = PlexusContainer();
//        MavenExecutionRequest request = MavenExecutionRequest();
//        MavenExecutionResult result = MavenExecutionResult();
//        MavenSession mavenSession = new MavenSession(container, request, result);
//    }

}
