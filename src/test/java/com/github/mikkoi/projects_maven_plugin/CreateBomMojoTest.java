package com.github.mikkoi.projects_maven_plugin;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

    @Test
    public void testIsIncluded() {
        MavenProject project = new MavenProject();

        CreateBomMojo mojo = new CreateBomMojo();
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        mojo.setIncludes(includes);
        mojo.setExcludes(excludes);

        // This is what CreateBomMojo.validateParameters would do:
        includes.add("*");
        project.setGroupId("com.github.mikkoi");
        project.setArtifactId("test-artifact");
        assertThat(mojo.isIncluded(project)).isTrue();

        includes.add("com.github.mikkoi:*");
        excludes.add("com.github.mikkoi:test-artifact");

        project.setGroupId("com.github.mikkoi");
        project.setArtifactId("test-artifact");
        assertThat(mojo.isIncluded(project)).isFalse();

        project.setArtifactId("test-artifact-2");
        assertThat(mojo.isIncluded(project)).isTrue();

        excludes.add("com.gitlab.other:other-artifact");
        project.setArtifactId("other-artifact");
        assertThat(mojo.isIncluded(project)).isTrue();

        project.setGroupId("com.gitlab.other");
        assertThat(mojo.isIncluded(project)).isFalse();

        excludes.add("com.gitlab.second:*-other-artifact");
        project.setGroupId("com.gitlab.second");
        project.setArtifactId("diff-other-artifact");
        assertThat(mojo.isIncluded(project)).isFalse();

        project.setArtifactId("artifact-something");
        assertThat(mojo.isIncluded(project)).isTrue();
    }
}
