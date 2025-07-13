package com.github.mikkoi.projects_maven_plugin;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GreetMojoTest
{
    @Test
    public void testSomethingWhichDoesNotNeedTheMojoAndProbablyShouldBeExtractedIntoANewClassOfItsOwn()
    {
        GreetMojo mojo = new GreetMojo();
        String formatString = mojo.getFormatString();
        assertThat(formatString).isEqualTo("%s, %s!");
        assertThat(formatString).isNotEqualTo("!%s, %s!");
//        assertEquals("%s, %s!", formatString);
//        assertNotEquals("!%s, %s!", formatString);
//            assertThat(result).isSuccessful()
//                    .out().info().contains("Welcome, Foobar!");
    }
}