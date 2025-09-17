package com.github.mikkoi.projects_maven_plugin;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MojoUtilities.
 */
class MojoUtilitiesTest {

    @Test
    void testConvertStringForMatching() {
        String name1 = MojoUtilities.convertStringForMatching("apache");
        assertThat(name1).isEqualTo(".*:apache:.*");

        String name2 = MojoUtilities.convertStringForMatching("org.apache.maven:core:jar");
        assertThat(name2).isEqualTo("org\\.apache\\.maven:core:jar");

        String name3 = MojoUtilities.convertStringForMatching("org.apache.maven:core");
        assertThat(name3).isEqualTo("org\\.apache\\.maven:core:.*");

        String name4 = MojoUtilities.convertStringForMatching("org.apache.*:*");
        assertThat(name4).isEqualTo("org\\.apache\\..*:.*:.*");

        String name5 = MojoUtilities.convertStringForMatching("com.github.mikkoi:test-artifact");
        assertThat(name5).isEqualTo("com\\.github\\.mikkoi:test-artifact:.*");

    }

    @Test
    void testIsIncluded() {
        MavenProject project = new MavenProject();

        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();

        // This is what CreateBomMojo.validateParameters would do:
        includes.add("*");
        project.setGroupId("com.github.mikkoi");
        project.setArtifactId("test-artifact");
        project.setPackaging("jar");
        assertThat(MojoUtilities.isIncluded(includes, excludes, project)).isTrue();

        includes.add("com.github.mikkoi:*");
        excludes.add("com.github.mikkoi:test-artifact");

        project.setGroupId("com.github.mikkoi");
        project.setArtifactId("test-artifact");
        assertThat(MojoUtilities.isIncluded(includes, excludes, project)).isFalse();

        project.setArtifactId("test-artifact-2");
        assertThat(MojoUtilities.isIncluded(includes, excludes, project)).isTrue();

        excludes.add("com.gitlab.other:other-artifact");
        project.setArtifactId("other-artifact");
        assertThat(MojoUtilities.isIncluded(includes, excludes, project)).isTrue();

        project.setGroupId("com.gitlab.other");
        assertThat(MojoUtilities.isIncluded(includes, excludes, project)).isFalse();

        excludes.add("com.gitlab.second:*-other-artifact");
        project.setGroupId("com.gitlab.second");
        project.setArtifactId("diff-other-artifact");
        assertThat(MojoUtilities.isIncluded(includes, excludes, project)).isFalse();

        project.setArtifactId("artifact-something");
        assertThat(MojoUtilities.isIncluded(includes, excludes, project)).isTrue();
    }
}