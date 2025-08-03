package com.github.mikkoi.projects_maven_plugin;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * List all projects in the build.
 */
@Mojo(name = "list")
public class ListMojo extends BaseMojo {

    /**
     * Exclude by project [groupId:]artifactId.
     * Default value: No projects excluded.
     * If includes list contains any items, they are evaluated first.
     * Then excludes are excluded from them.
     */
    @Parameter(property = "projects" + ".list" + ".excludes")
    List<String> excludes;

    /**
     * Include by project [groupId:]artifactId.
     * Default value: all projects included.
     */
    @Parameter(property = "projects" + ".list" + ".includes")
    private List<String> includes;

    /**
     * Sorting order: maven | alphabetic, default: maven
     */
    @Parameter(property = "projects" + ".list" + ".sortOrder", defaultValue = "maven")
    String sortOrder;

    /**
     * Format for printing.
     */
    @Parameter(property = "projects" + ".list" + ".printFormat", defaultValue = "{groupId}:{artifactId}")
    private String printFormat;

    /**
     * The main entry point for mojo.
     */
    @Override
    public void execute() {
        if (this.skip) {
            return;
        }
        MavenProject currentProject = this.mavenSession.getCurrentProject();
        getLog().debug(String.format("Current Project: %s:%s", currentProject.getGroupId(), currentProject.getArtifactId()));
        MavenProject topLevelProject = this.mavenSession.getTopLevelProject();
        getLog().debug(String.format("Top Level Project: %s:%s", topLevelProject.getGroupId(), topLevelProject.getArtifactId()));

        validateParameters();

        if (runOnlyAtExecutionRoot && !isLastProjectInReactor(this.mavenSession)) {
            return;
        }

        final List<MavenProject> projects = mavenSession.getProjects();

        printOut(this.list(projects));
    }

    /**
     * Validate parameters provided via properties
     * either on the command line or using configuration element in pom.
     */
    private void validateParameters() {
        getLog().debug("includes=" + includes.toString());
        getLog().debug("excludes=" + excludes.toString());

//        excludes.stream().forEach( a -> {
//            if(a == null) {
//                throw MojoExecutionException(a, "Failure in parameter", "Failure in parameter excludes. String is null");
    }

    /**
     * Convert list of projects to a list of strings.
     *
     * @param projects   List<MavenProject>
     * @return String    List<String>, List of strings ready for writing out.
     */
    public List<String> list(List<MavenProject> projects) {
        getLog().debug("Begin of projects:");
        Comparator<MavenProject> comparator = getMavenProjectComparator(this.sortOrder);
        List<String> rows = new ArrayList<>();
        projects.stream().filter(this::isIncluded).sorted(comparator).forEach(mavenProject -> {
            rows.add(formatProject(mavenProject));
        });
        getLog().debug(":End of projects");
        return rows;
    }

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
            comparator = new Comparator<MavenProject>() {
                @Override
                public int compare(MavenProject o1, MavenProject o2) {
                    return 0;
                }
            };
        } else {
            comparator = (o1, o2) -> {
                String o1GroupId = o1.getGroupId();
                String o2groupId = o2.getGroupId();
                String o1artifactId = o1.getArtifactId();
                String o2artifactId = o2.getArtifactId();
                int r = o1GroupId.compareTo(o2groupId);
                if (r != 0) {
                    return r;
                }
                return o1artifactId.compareTo(o2artifactId);
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
    static String convertStringForMatching(String s) {
        String t = s.replace(".", "\\.");
        t = t.replace("*", ".*");
        if (!t.contains(":")) {
            t = ".*:" + t;
        }
        return t;
    }

    /**
     * <p>Replace a String with another String inside a larger String,
     * for the first <code>max</code> values of the search String.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * <p>Copied and modified from org.apache.maven.shared.utils.StringUtils.</p>
     *
     * @param text text to search and replace in
     * @param repl String to search for
     * @param with String to replace with
     * @return the text with any replacements processed
     */
    static String replace(String text, String repl, String with) {
        if ((text == null) || (repl == null) || (with == null) || repl.isEmpty()) {
            return text;
        }

        StringBuilder buf = new StringBuilder(text.length());
        int start = 0, end;
        while ((end = text.indexOf(repl, start)) != -1) {
            buf.append(text, start, end).append(with);
            start = end + repl.length();
        }
        buf.append(text, start, text.length());
        return buf.toString();
    }

    public String formatProject(MavenProject mavenProject) {
        String s = this.printFormat;
        return replace(
                replace(
                        replace(
                                replace(
                                        replace(
                                                replace(
                                                        replace(s, "{groupId}", mavenProject.getGroupId()),
                                                        "{artifactId}", mavenProject.getArtifactId()
                                                ), "{name}", mavenProject.getName()
                                        ), "{description}", mavenProject.getDescription()
                                ), "{version}", mavenProject.getVersion()
                        ), "{absPath}", mavenProject.getBasedir().getAbsolutePath()
                ), "{packaging}", mavenProject.getPackaging()
        );
    }

    public boolean isIncluded(MavenProject mavenProject) {
        String projectGroupId = mavenProject.getGroupId();
        String projectArtifactId = mavenProject.getArtifactId();
        String projectId = projectGroupId + ":" + projectArtifactId;

        // Match from the end of the id, artifactId alone is enough.
        Predicate<String> predicateForProjectId = s -> projectId.matches(convertStringForMatching(s));
        if (this.includes.isEmpty() && this.excludes.isEmpty()) {
            return true;
        } else if (this.includes.isEmpty()) {  // && ! this.excludes.isEmpty()
            return this.excludes.stream().noneMatch(predicateForProjectId);
        } else if (this.excludes.isEmpty()) {  // && !this.includes.isEmpty()
            return true;
        } else {
            return true;
        }
    }

}
