package com.github.mikkoi.projects_maven_plugin;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ListMojo.
 */
class ListMojoTest {

    @Test
    void testToString() {
        ListMojo mojo = new ListMojo();
        assertThat(mojo.toString()).isEqualTo("ListMojo{includes=null, excludes=null, sortOrder='null', printFormat='null'}");

        mojo.setSortOrder("alphabetic");
        mojo.setPrintFormat("{name} - {version}");
        mojo.setIncludes(new ArrayList<>());
        mojo.setExcludes(new ArrayList<>());
        assertThat(mojo.toString()).isEqualTo("ListMojo{includes=[], excludes=[], sortOrder='alphabetic', printFormat='{name} - {version}'}");
    }

    @Test
    void testIsIncluded() {
        MavenProject project = new MavenProject();

        ListMojo mojo = new ListMojo();
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        mojo.setIncludes(includes);
        mojo.setExcludes(excludes);

        // This is what CreateBomMojo.validateParameters would do:
        includes.add("*");
        mojo.setIncludes(includes);
        project.setGroupId("com.github.mikkoi");
        project.setArtifactId("test-artifact");
        project.setPackaging("jar");
        assertThat(mojo.isIncluded(project)).isTrue();

    }

    @Test
    void testFormatProject() {
        MavenProject project = new MavenProject();
        project.setGroupId("this.group.id-has-dashes");
        project.setArtifactId("this-artifact-id");
        project.setName("This Artifact Name");
        project.setDescription("This Artifact Description\n has two rows.");
        project.setVersion("1.2.3-DEBUG");
        project.setPackaging("ear");
        project.setFile(new File("/this/base/dir/pom.xml"));

        ListMojo mojo = new ListMojo();

        mojo.setPrintFormat("{groupId}:{artifactId}:{name}");
        assertThat(mojo.formatProject(project)).isEqualTo("this.group.id-has-dashes:this-artifact-id:This Artifact Name");

        mojo.setPrintFormat("{artifactId}:{packaging}:{version}:{description}:{absPath}");
        assertThat(mojo.formatProject(project)).isEqualTo("this-artifact-id:ear:1.2.3-DEBUG:This Artifact Description\n has two rows.:/this/base/dir");
    }
}
