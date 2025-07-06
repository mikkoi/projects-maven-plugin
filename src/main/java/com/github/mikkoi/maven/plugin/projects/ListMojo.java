package com.github.mikkoi.maven.plugin.projects;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * list -Dincludes -Dexcludes -Dorder=random|alphabetic (traversal order) -Dformat="%groupId:%artifactId:%type"
 * tree -Dincludes -Dexcludes -Dindent=4
 * add-dependencies -Dincludes -Dexcludes
 */
@Mojo(name = "list")
public class ListMojo extends AbstractMojo {

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession mavenSession;

    /**
     * Skip execution
     */
    @Parameter(property = "projects" + ".list" + ".skip", defaultValue = "false", alias = "skip")
    private boolean skip;

    /**
     * Force output to STDOUT instead of using [INFO] logging level.
     */
    @Parameter(property = "projects" + ".list" + ".forceStdout", defaultValue = "false", alias = "forceStdoutTwo")
    private boolean forceStdout;

    /**
     * Run only in the project which is at execution root, i.e. the dir in which `mvn` is executed.
     */
    @Parameter(property = "projects" + ".list" + ".runOnlyAtExecutionRoot", defaultValue = "true")
    private boolean runOnlyAtExecutionRoot;

    /**
     * Include by project [groupId:]artifactId.
     * Default value: all projects included.
     */
    @Parameter(property = "projects" + ".list" + ".includes")
    private List<String> includes;

    /**
     * Exclude by project [groupId:]artifactId.
     * Default value: No projects excluded.
     * If includes list contains any items, they are evaluated first.
     * Then excludes are excluded from them.
     */
    @Parameter(property = "projects" + ".list" + ".excludes")
    private List<String> excludes;

    /**
     * Sorting order: maven | alphabetic, default: maven
     */
    @Parameter(property = "projects" + ".list" + ".sortOrder", defaultValue = "maven")
    private String sortOrder;

    /**
     * Format for printing.
     */
    @Parameter(property = "projects" + ".list" + ".printFormat", defaultValue = "{groupId}:{artifactId}")
    private String printFormat;

    /**
     * Create a comparator which compares Maven project groupId and artifactId
     * for alphabetical listing.
     *
     * @param sortOrder "random" / "alphabetic"
     * @return the comparator
     */
    static Comparator<MavenProject> getMavenProjectComparator(String sortOrder) {
        Comparator<MavenProject> comparator;
        if (sortOrder.equals("maven")) {
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
        if ((text == null) || (repl == null) || (with == null) || (repl.isEmpty())) {
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

    private void validateParameters() {
//        excludes.stream().forEach( a -> {
//            if(a == null) {
//                throw MojoExecutionException(a, "Failure in parameter", "Failure in parameter excludes. String is null");
    }

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

        getLog().debug(includes.toString());
        getLog().debug(excludes.toString());

        validateParameters();

        if (runOnlyAtExecutionRoot && !isLastProjectInReactor(this.mavenSession)) {
            return;
        }

        final List<MavenProject> projects = mavenSession.getProjects();
        final List<String> outRows = this.list(projects);
        outRows.forEach(row -> {
            if(this.forceStdout) {
                System.out.println(row);
            } else {
                getLog().info(row);
            }
        });
    }

    public String formatProject(MavenProject mavenProject) {
        String s = this.printFormat;
        s = replace(s, "{groupId}", mavenProject.getGroupId());
        s = replace(s, "{artifactId}", mavenProject.getArtifactId());
        s = replace(s, "{name}", mavenProject.getName());
        s = replace(s, "{description}", mavenProject.getDescription());
        s = replace(s, "{version}", mavenProject.getVersion());
        s = replace(s, "{absPath}", mavenProject.getBasedir().getAbsolutePath());
        s = replace(s, "{packaging}", mavenProject.getPackaging());
        return s;
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

    /**
     * @param projects List<MavenProject>
     * @return String ready for writing out.
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
     * Is this project the last project in the reactor?
     *
     * @return true if last project (including only project when only one project)
     */
    private boolean isLastProjectInReactor(MavenSession mavenSession) {
        List<MavenProject> sortedProjects = mavenSession.getProjectDependencyGraph().getSortedProjects();
        MavenProject lastProject = sortedProjects.isEmpty() ? mavenSession.getCurrentProject() : sortedProjects.get(sortedProjects.size() - 1);
        // : sortedProjects.getLast();
        if (getLog().isDebugEnabled()) {
            getLog().debug("Current project: '" + mavenSession.getCurrentProject().getName() + "', Last project to execute based on dependency graph: '" + lastProject.getName() + "'");
        }
        return mavenSession.getCurrentProject().equals(lastProject);
    }

}
