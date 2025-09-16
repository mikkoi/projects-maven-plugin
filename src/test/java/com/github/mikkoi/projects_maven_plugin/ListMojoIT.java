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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

/**
 * Integration tests for ListMojo.
 */
@MavenJupiterExtension
public class ListMojoIT {

    /*
        Using default values.
     */
    @Nested
    @MavenProject      // Use same Maven project for all tests in this set.
    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:list")
    @MavenOption(MavenCLIOptions.BATCH_MODE)
    @MavenOption(MavenCLIOptions.QUIET)
    @MavenOption(MavenCLIOptions.VERBOSE)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class set_001 {

        @MavenTest
        @Order(1)
        void the_first_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful().out().info()
                    .contains("com.github.mikkoi:projects-maven-plugin-list-set-001:pom")
            ;
        }
    }

    /*
        Using configuration values.
     */
    @Nested
    @MavenProject      // Use same Maven project for all tests in this set.
    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:list")
    @MavenOption(MavenCLIOptions.BATCH_MODE)
    @MavenOption(MavenCLIOptions.QUIET)
    @MavenOption(MavenCLIOptions.VERBOSE)
    @MavenRepository   // We can share the local repository because this plugin does not use it.
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class set_002 {

        @MavenTest
        @Order(1)
        void the_first_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful().out().info()
                    .contains("com.github.mikkoi:projects-maven-plugin-list-set-002:pom")
                    .contains("com.github.mikkoi:other:pom")
                    .contains("com.github.mikkoi:second:pom")
                    .contains("com.github.mikkoi:fourth:pom")
                    .contains("com.github.mikkoi:third:pom")
            ;
        }

        // -Dprojects.list.runOnlyAtExecutionRoot=true -Dprojects.list.skip=false  -Dprojects.list.excludes="db,draw" -Dprojects.list.printFormat="{artifactId}"
        @MavenTest
        @SystemProperty(value = "projects.list.printFormat", content = "{artifactId}")
        @SystemProperty(value = "projects.list.excludes", content = "second,third")
        @Order(2)
        void the_second_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful().out().info()
                    .contains("projects-maven-plugin-list-set-002")
                    .contains("other")
                    .contains("fourth")
            ;
        }

        @MavenTest
        @SystemProperty(value = "projects.skip", content = "true")
        @Order(3)
        void the_third_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful().out().info()
//                    .contains("Skip execution ...")
                    .doesNotContain("projects-maven-plugin-list-set-002")
                    .doesNotContain("other")
                    .doesNotContain("second")
                    .doesNotContain("fourth")
                    .doesNotContain("third")
            ;
        }

        @MavenTest
        @SystemProperty(value = "projects.list.sortOrder", content = "alphabetic")
        @SystemProperty(value = "projects.list.excludes", content = "projects-maven-plugin-list-set-002")
        @Order(4)
        void the_fourth_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful().out().plain()
                    .asString().contains(String.join(
                            ", ",
                            "[INFO] com.github.mikkoi:fourth:pom",
                            "[INFO] com.github.mikkoi:other:pom",
                            "[INFO] com.github.mikkoi:second:pom",
                            "[INFO] com.github.mikkoi:third:pom,"
                    )
            );
        }

        @MavenTest
        @SystemProperty(value = "projects.forceStdout", content = "true")
        @Order(5)
        void the_fifth_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful().out().info()
                    .doesNotContain("com.github.mikkoi:projects-maven-plugin-list-set-002")
                    .doesNotContain("com.github.mikkoi:other")
                    .doesNotContain("com.github.mikkoi:second")
                    .doesNotContain("com.github.mikkoi:third")
                    .doesNotContain("com.github.mikkoi:fourth");
            assertThat(result).isSuccessful().out().plain()
                    .contains("com.github.mikkoi:projects-maven-plugin-list-set-002:pom")
                    .contains("com.github.mikkoi:other:pom")
                    .contains("com.github.mikkoi:second:pom")
                    .contains("com.github.mikkoi:third:pom")
                    .contains("com.github.mikkoi:fourth:pom");
        }

    }
}
