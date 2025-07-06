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

import java.io.File;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

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
//    @MavenRepository   // We can share the local repository because this plugin does not use it.
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class set_001 {

        @MavenTest
        @Order(1)
        void the_first_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful()
                    .out().info().contains("com.github.mikkoi:projects-maven-plugin-list-set-001");
        }
    }

//    /*
//        Using command line properties values.
//     */
//    @Nested
//    @MavenProject      // Use same Maven project for all tests in this set.
//    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:list")
//    @MavenOption(MavenCLIOptions.BATCH_MODE)
//    @MavenRepository   // We can share the local repository because this plugin does not use it.
//    @TestMethodOrder( MethodOrderer.OrderAnnotation.class )
//    class set_002 {
//
//        @SystemProperty(value="projecthelper.sayhi.greeting", content="Welcome")
//        @SystemProperty(value="projecthelper.sayhi.target", content="Foobar")
//        @MavenTest
//        @Order(1)
//        void the_first_test_case(MavenExecutionResult result) {
//            assertThat(result).isSuccessful()
//                    .out().info().contains("Welcome, Foobar!");
//        }
//    }
//
//    /*
//        Using configuration values.
//     */
//    @Nested
//    @MavenProject      // Use same Maven project for all tests in this set.

    /// /    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:sayhi")
//    @MavenGoal("package")
//    @MavenOption(MavenCLIOptions.BATCH_MODE)
//    @MavenRepository   // We can share the local repository because this plugin does not use it.
//    @TestMethodOrder( MethodOrderer.OrderAnnotation.class )
//    class set_003 {
//
//        @MavenTest
//        @Order(1)
//        void the_first_test_case(MavenExecutionResult result) {
//            Predicate<String> predicate =s -> s.matches("^The Greeting, The Target!$");
//            Predicate<? super List<? extends String>> predicateList = s -> s.stream().anyMatch(predicate);
//            assertThat(result).isSuccessful().out().info().matches(predicateList);
//        }
//    }

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
    class set_004 {

        @MavenTest
        @Order(1)
        void the_first_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful().out().info().contains("com.github.mikkoi:projects-maven-plugin-list-set-004").contains("com.github.mikkoi:other").contains("com.github.mikkoi:second").contains("com.github.mikkoi:fourth").contains("com.github.mikkoi:third");
        }

        // -Dprojects.list.runOnlyAtExecutionRoot=true -Dprojects.list.skip=false  -Dprojects.list.excludes="db,draw" -Dprojects.list.printFormat="{artifactId}"
        @MavenTest
        @SystemProperty(value = "projects.list.printFormat", content = "{artifactId}")
        @SystemProperty(value = "projects.list.excludes", content = "second,third")
        @Order(2)
        void the_second_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful().out().info().contains("projects-maven-plugin-list-set-004").contains("other").contains("fourth");
        }

        @MavenTest
        @SystemProperty(value = "projects.list.skip", content = "true")
        @Order(3)
        void the_third_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful().out().info().doesNotContain("projects-maven-plugin-list-set-004").doesNotContain("other").doesNotContain("second").doesNotContain("fourth").doesNotContain("third");
        }

        @MavenTest
        @SystemProperty(value = "projects.list.sortOrder", content = "alphabetic")
        @SystemProperty(value = "projects.list.excludes", content = "projects-maven-plugin-list-set-004")
        @Order(4)
        void the_fourth_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful().out().plain().asString().contains(
                    "[INFO] com.github.mikkoi:fourth, [INFO] com.github.mikkoi:other, [INFO] com.github.mikkoi:second, [INFO] com.github.mikkoi:third,"
            );
        }

        @MavenTest
        @SystemProperty(value = "forceStdout", content = "true")
        @Order(5)
        void the_fifth_test_case(MavenExecutionResult result) {
            assertThat(result).isSuccessful().out().plain().asString().doesNotContain(
                    "com.github.mikkoi:projects-maven-plugin-list-set-004, com.github.mikkoi:fourth, com.github.mikkoi:other, com.github.mikkoi:second, com.github.mikkoi:third,"
            );
        }

    }
}
