package com.github.mikkoi.projects_maven_plugin;

import com.soebes.itf.jupiter.extension.MavenCLIOptions;
import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenOption;
import com.soebes.itf.jupiter.extension.MavenProject;
import com.soebes.itf.jupiter.extension.MavenRepository;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.extension.SystemProperty;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

@MavenJupiterExtension
public class CreateBomMojoIT {

    @Nested
    @MavenProject      // Use same Maven project for all tests in this set.
    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:create-bom")
    @MavenOption(MavenCLIOptions.BATCH_MODE)
    @MavenOption(MavenCLIOptions.QUIET)
    @MavenOption(MavenCLIOptions.VERBOSE)
    @MavenRepository   // We can share the local repository because this plugin does not use it.
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class set_001 {

        // Create bom with all the projects in the build (set-001)
        @MavenTest
        @Order(1)
        void the_first_test_case(MavenExecutionResult result) {
            // Log
            assertThat(result).isSuccessful();
            Path bomPath = Paths.get(result.getMavenProjectResult().getTargetProjectDirectory().toString(), "target", "bom", "pom.xml");
            assertThat(Files.exists(bomPath)).isTrue();
            final ModelReader modelReader = new DefaultModelReader();
            Model model;
            try {
                model = modelReader.read(bomPath.toFile(), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertThat(model.getDependencyManagement().getDependencies().size()).isEqualTo(6);
            final Dependency rootProject = new Dependency();
            rootProject.setType("pom");
            rootProject.setGroupId("com.github.mikkoi");
            rootProject.setArtifactId("projects-maven-plugin-create-bom-set-001");
            rootProject.setVersion("0.0.1-SNAPSHOT");
            assertThat(model.getDependencyManagement().getDependencies().get(0).getGroupId().equals(rootProject.getGroupId())).isTrue();
            assertThat(model.getDependencyManagement().getDependencies().get(0).getArtifactId().equals(rootProject.getArtifactId())).isTrue();
            assertThat(model.getDependencyManagement().getDependencies().get(0).getVersion().equals(rootProject.getVersion())).isTrue();
            assertThat(model.getDependencyManagement().getDependencies().get(0).getType().equals(rootProject.getType())).isTrue();
        }

        @MavenTest
        @SystemProperty(value = "projects.createBom.path", content = "target/bom-set-002/pom.xml")
        @SystemProperty(value = "projects.createBom.groupId", content = "this.test.project")
        @SystemProperty(value = "projects.createBom.artifactId", content = "bom")
        @SystemProperty(value = "projects.createBom.version", content = "123")
        @SystemProperty(value = "projects.createBom.excludes", content = "com.github.mikkoi:projects-maven-plugin-create-bom-set-001")
        @Order(2)
        void the_second_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful();
            Path bomPath = Paths.get(result.getMavenProjectResult().getTargetProjectDirectory().toString(), "target", "bom-set-002", "pom.xml");
            assertThat(Files.exists(bomPath)).isTrue();
            final ModelReader modelReader = new DefaultModelReader();
            Model model;
            try {
                model = modelReader.read(bomPath.toFile(), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertThat(model.getDependencyManagement().getDependencies().size()).isEqualTo(5);
            assertThat(model.getGroupId()).isEqualTo("this.test.project");
            assertThat(model.getArtifactId()).isEqualTo("bom");
            assertThat(model.getVersion()).isEqualTo("123");
        }

        @MavenTest
        @SystemProperty(value = "projects.createBom.path", content = "target/bom-set-003/pom.xml")
        @SystemProperty(value = "projects.createBom.groupId", content = "this.test.project")
        @SystemProperty(value = "projects.createBom.artifactId", content = "bom")
        @SystemProperty(value = "projects.createBom.version", content = "TODAY")
        @SystemProperty(value = "projects.createBom.includes", content = "com.github.mikkoi:other*")
        @SystemProperty(value = "projects.createBom.excludes", content = "com.github.mikkoi:other-second-*")
        @SystemProperty(value = "projects.createBom.sortOrder", content = "alphabetical")
        @Order(3)
        void the_third_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful();
            Path bomPath = Paths.get(result.getMavenProjectResult().getTargetProjectDirectory().toString(), "target", "bom-set-003", "pom.xml");
            assertThat(Files.exists(bomPath)).isTrue();
            final ModelReader modelReader = new DefaultModelReader();
            Model model;
            try {
                model = modelReader.read(bomPath.toFile(), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertThat(model.getDependencyManagement().getDependencies().size()).isEqualTo(2);
            assertThat(model.getGroupId()).isEqualTo("this.test.project");
            assertThat(model.getArtifactId()).isEqualTo("bom");
            assertThat(model.getVersion()).isEqualTo("TODAY");
            assertThat(model.getDependencyManagement().getDependencies().get(1).getGroupId()).isEqualTo("com.github.mikkoi");
            assertThat(model.getDependencyManagement().getDependencies().get(1).getArtifactId()).isEqualTo("other-second");
            assertThat(model.getDependencyManagement().getDependencies().get(1).getVersion()).isEqualTo("0.0.1-SNAPSHOT");
            assertThat(model.getDependencyManagement().getDependencies().get(1).getType()).isEqualTo("pom");
        }

        @MavenTest
        @SystemProperty(value = "projects.createBom.path", content = "target/bom-set-003/pom.xml")
        @SystemProperty(value = "projects.createBom.groupId", content = "this.test.project")
        @SystemProperty(value = "projects.createBom.artifactId", content = "bom")
        @SystemProperty(value = "projects.createBom.version", content = "TODAY")
        @SystemProperty(value = "projects.createBom.includes", content = "com.github.mikkoi:other*")
        @SystemProperty(value = "projects.createBom.excludes", content = "other,other-second")
        @SystemProperty(value = "projects.createBom.sortOrder", content = "alphabetical")
        @Order(4)
        void the_fourth_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful();
            Path bomPath = Paths.get(result.getMavenProjectResult().getTargetProjectDirectory().toString(), "target", "bom-set-003", "pom.xml");
            assertThat(Files.exists(bomPath)).isTrue();
            final ModelReader modelReader = new DefaultModelReader();
            Model model;
            try {
                model = modelReader.read(bomPath.toFile(), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertThat(model.getDependencyManagement().getDependencies().size()).isEqualTo(3);
            assertThat(model.getGroupId()).isEqualTo("this.test.project");
            assertThat(model.getArtifactId()).isEqualTo("bom");
            assertThat(model.getVersion()).isEqualTo("TODAY");
            assertThat(model.getDependencyManagement().getDependencies().get(2).getGroupId()).isEqualTo("com.github.mikkoi");
            assertThat(model.getDependencyManagement().getDependencies().get(2).getArtifactId()).isEqualTo("other-second-third");
            assertThat(model.getDependencyManagement().getDependencies().get(2).getVersion()).isEqualTo("0.0.1-SNAPSHOT");
            assertThat(model.getDependencyManagement().getDependencies().get(2).getType()).isEqualTo("pom");
        }

    }
}
