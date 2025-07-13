package com.github.mikkoi.projects_maven_plugin;

import com.soebes.itf.jupiter.extension.MavenJupiterExtension;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

@MavenJupiterExtension
public class GreetMojoIT {

//    /*
//        Using default values.
//     */
//    @Nested
//    @MavenProject      // Use same Maven project for all tests in this set.
//    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:greet")
//    @MavenOption(MavenCLIOptions.BATCH_MODE)
//    @MavenRepository   // We can share the local repository because this plugin does not use it.
//    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//    class set_001 {
//
//        @MavenTest
//        @Order(1)
//        void the_first_test_case(MavenExecutionResult result) {
//            assertThat(result).isSuccessful().out().info().contains("Hello, World!");
//        }
//    }
//
//    /*
//        Using command line properties values.
//     */
//    @Nested
//    @MavenProject      // Use same Maven project for all tests in this set.
//    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:greet")
//    @MavenOption(MavenCLIOptions.BATCH_MODE)
//    @MavenRepository   // We can share the local repository because this plugin does not use it.
//    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//    class set_002 {
//
//        @SystemProperty(value = "projecthelper.greet.greeting", content = "Welcome")
//        @SystemProperty(value = "projecthelper.greet.target", content = "Foobar")
//        @MavenTest
//        @Order(1)
//        void the_first_test_case(MavenExecutionResult result) {
//            assertThat(result).isSuccessful().out().info().contains("Welcome, Foobar!");
//        }
//    }

//    /*
//        Using configuration values.
//     */
//    @Nested
//    @MavenProject      // Use same Maven project for all tests in this set.
////    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:greet")
//    @MavenGoal("package")
//    @MavenOption(MavenCLIOptions.BATCH_MODE)
//    @MavenRepository   // We can share the local repository because this plugin does not use it.
//    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//    class set_003 {
//
//        @MavenTest
//        @Order(1)
//        void the_first_test_case(MavenExecutionResult result) {
//            Predicate<String> predicate = s -> s.matches("^The Greeting, The Target!$");
//            Predicate<? super List<? extends String>> predicateList = s -> s.stream().anyMatch(predicate);
//            assertThat(result).isSuccessful().out().info().matches(predicateList);
//        }
//    }

}
