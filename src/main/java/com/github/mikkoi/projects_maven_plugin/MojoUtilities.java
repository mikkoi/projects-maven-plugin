package com.github.mikkoi.projects_maven_plugin;

import org.apache.maven.project.MavenProject;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Utility methods for Mojos.
 * All methods are static.
 */
public class MojoUtilities {

    private MojoUtilities() {
        // Utility class, prevent instantiation
    }

    /**
     * Create a comparator which compares Maven project groupId and artifactId
     * for alphabetical listing.
     *
     * @param sortOrder "random" / "alphabetic"
     * @return the comparator
     */
    public static Comparator<MavenProject> getMavenProjectComparator(String sortOrder) {
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

    /**
     * Is the project included based on includes and excludes lists.
     * <p>
     * The includes and excludes lists are lists of strings which are
     * matched against the project id, which is groupId:artifactId:packaging.
     * <p>
     * The matching is done from the end of the string, so artifactId alone is enough.
     * <p>
     * The includes and excludes lists are processed in order, so the first match wins.
     * <p>
     * If includes is empty, then all projects are included.
     * If excludes is empty, then no projects are excluded.
     *
     * @param includes      List of strings (project ids) to include
     * @param excludes      List of strings (project ids) to exclude
     * @param mavenProject MavenProject to check
     * @return true if the project is included
     */
    public static boolean isIncluded(List<String> includes, List<String> excludes, MavenProject mavenProject) {
        String projectId = String.format("%s:%s:%s", mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getPackaging());

        // Match from the end of the id, artifactId alone is enough.
        Predicate<String> predicateForProjectId = s -> projectId.matches(convertStringForMatching(s));
        return includes.stream().anyMatch(predicateForProjectId) && excludes.stream().noneMatch(predicateForProjectId);
    }

}
