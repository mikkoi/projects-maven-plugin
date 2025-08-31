package com.github.mikkoi.projects_maven_plugin;

import org.apache.maven.project.MavenProject;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class MojoUtilities {

    /**
     * Create a comparator which compares Maven project groupId and artifactId
     * for alphabetical listing.
     *
     * @param sortOrder "random" / "alphabetic"
     * @return the comparator
     */
    static Comparator<MavenProject> getMavenProjectComparator(String sortOrder) {
        Comparator<MavenProject> comparator;
        if ("maven".equals(sortOrder)) {
            comparator = (o1, o2) -> 0;
        } else {
            comparator = (o1, o2) -> {
                String o1GroupId = o1.getGroupId();
                String o2groupId = o2.getGroupId();
                String o1artifactId = o1.getArtifactId();
                String o2artifactId = o2.getArtifactId();
                String o1Packaging = o1.getPackaging();
                String o2Packaging = o2.getPackaging();
                int r1 = o1GroupId.compareTo(o2groupId);
                if (r1 != 0) {
                    return r1;
                }
                int r2 = o1artifactId.compareTo(o2artifactId);
                if (r2 != 0) {
                    return r2;
                }
                return o1Packaging.compareTo(o2Packaging);
            };
        }
        return comparator;
    }

    /**
     * Convert string for matching.
     *
     * @param s String
     * @return converted string
     */
    public static String convertStringForMatching(String s) {
        String t = s.replace(".", "\\.");
        t = t.replace("*", ".*");
        if (!t.contains(":")) {
            t = ".*:" + t + ":.*";
        }
        if (Arrays.stream(t.split(":")).count() < 3) {
            t = t + ":.*";
        }
        return t;
    }

    public static boolean isIncluded(List<String> includes, List<String> excludes, MavenProject mavenProject) {
        String projectId = String.format("%s:%s:%s", mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getPackaging());

        // Match from the end of the id, artifactId alone is enough.
        Predicate<String> predicateForProjectId = s -> projectId.matches(convertStringForMatching(s));
        return includes.stream().anyMatch(predicateForProjectId) && excludes.stream().noneMatch(predicateForProjectId);
    }

}
