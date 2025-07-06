package com.github.mikkoi.maven.plugin.projects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ListMojoTest {

    @Test
    public void testConvertStringForMatching() {
        String name1 = ListMojo.convertStringForMatching("apache");
        assertThat(name1).isEqualTo(".*:apache");
        String name2 = ListMojo.convertStringForMatching("org.apache.maven:core");
        assertThat(name2).isEqualTo("org\\.apache\\.maven:core");
        String name3 = ListMojo.convertStringForMatching("org.apache.*:*");
        assertThat(name3).isEqualTo("org\\.apache\\..*:.*");
    }

}
