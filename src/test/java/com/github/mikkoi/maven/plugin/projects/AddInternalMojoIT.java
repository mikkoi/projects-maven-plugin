package com.github.mikkoi.maven.plugin.projects;

import com.soebes.itf.jupiter.extension.MavenCLIOptions;
import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenOption;
import com.soebes.itf.jupiter.extension.MavenProject;
import com.soebes.itf.jupiter.extension.MavenRepository;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.extension.SystemProperty;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

@MavenJupiterExtension
public class AddInternalMojoIT {

    /*
        Using default values.
     */
    @Nested
    @MavenProject      // Use same Maven project for all tests in this set.
//    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:add-internal")
    @MavenGoal("install")
    @MavenOption(MavenCLIOptions.BATCH_MODE)
    @MavenOption(MavenCLIOptions.QUIET)
    @MavenOption(MavenCLIOptions.VERBOSE)
    @MavenRepository   // We can share the local repository because this plugin does not use it.
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class set_001 {

        @MavenTest
        @Order(1)
        @SystemProperty(value = "projects.errorIfUnknownProject", content = "true")
        @SystemProperty(value = "projects.errorIsWarning", content = "true")
        void the_first_test_case(MavenExecutionResult result) {
            String g = System.getProperty("project.groupId");
            String v = System.getProperty("project.version");

            assertThat(result).isSuccessful();
            assertThat(result).out().info().contains(String.format("Project %s:projects-maven-plugin-add-internal-set-001-aggregate:%s has dependency %s:fourth:%s", g, v, g, v));
            assertThat(result).out().info().contains(String.format("Project %s:projects-maven-plugin-add-internal-set-001-aggregate:%s has dependency %s:other:%s", g, v, g, v));
            assertThat(result).out().info().contains(String.format("Project %s:projects-maven-plugin-add-internal-set-001-aggregate:%s has dependency %s:second:%s", g, v, g, v));
            //assertThat(result).out().warn().contains("");
        }
    }

}
