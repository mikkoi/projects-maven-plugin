package com.github.mikkoi.projects_maven_plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * List all projects in the build.
 */
@Mojo(name = "list", defaultPhase = LifecyclePhase.NONE, aggregator = true)
public class ListMojo extends BaseMojo {

    private List<String> includes;
    /**
     * Include by project [groupId:]artifactId.
     * Default value: all projects included.
     * If includes list contains any items, they are evaluated first.
     * Then excludes are excluded from them.
     * @param includes the includes
     */
    @Parameter(property = "projects" + ".list" + ".includes", alias = "includes")
    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    private List<String> excludes;
    /**
     * Exclude by project [groupId:]artifactId.
     * Default value: No projects excluded.
     * @param excludes the excludes
     */
    @Parameter(property = "projects" + ".list" + ".excludes", alias = "excludes")
    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    /**
     * Sorting order: maven | alphabetic, default: maven
     */
    @Parameter(property = "projects" + ".list" + ".sortOrder", defaultValue = "maven")
    String sortOrder;

    private String printFormat;
    /**
     * Format for printing.
     * Default value: {groupId}:{artifactId}:{packaging}
     * @param printFormat the print format
     */
    @Parameter(property = "projects" + ".list" + ".printFormat", defaultValue = "{groupId}:{artifactId}:{packaging}")
    public void setPrintFormat(String printFormat) {
        this.printFormat = printFormat;
    }

    /**
     * Validate parameters provided via properties
     * either on the command line or using configuration element in pom.
     */
    private void validateAndPrepareParameters() throws MojoExecutionException {
        getLog().debug("includes=" + includes);
        getLog().debug("excludes=" + excludes);
        getLog().debug("sortOrder=" + sortOrder);

        for (String a : includes) {
            if (a == null) {
                throw new MojoExecutionException(includes, "Failure in parameter", "Failure in parameter 'includes'. String is null");
            }
        }
        if (includes.isEmpty()) {
            includes.add("*");
        }

        for (String a : excludes) {
            if (a == null) {
                throw new MojoExecutionException(excludes, "Failure in parameter", "Failure in parameter 'excludes'. String is null");
            }
        }

    }

    /**
     * Convert list of projects to a list of strings.
     *
     * @param projects   List of MavenProject objects.
     * @return String    List of strings ready for writing out.
     */
    public List<String> list(List<MavenProject> projects) {
        getLog().debug("Begin of projects:");
        Comparator<MavenProject> comparator = MojoUtilities.getMavenProjectComparator(this.sortOrder);
        List<String> rows = new ArrayList<>();
        projects.stream().filter(this::isIncluded).sorted(comparator).forEach(mavenProject -> rows.add(formatProject(mavenProject)));
        getLog().debug(":End of projects");
        return rows;
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

    /**
     * Format a single project to a string.
     * Replaces {groupId}, {artifactId}, {name}, {description}, {version}, {absPath}, {packaging}.
     *
     * @param mavenProject MavenProject
     * @return String formatted project
     */
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

    /**
     * Decide if the project is included or excluded.
     *
     * @param mavenProject MavenProject
     * @return true if included, false if excluded
     */
    public boolean isIncluded(MavenProject mavenProject) {
        boolean r = MojoUtilities.isIncluded(this.includes, this.excludes, mavenProject);
        getLog().debug(String.format("isIncluded(%s:%s:%s:%s): %b", mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion(), mavenProject.getPackaging(), r));
        return r;
    }

    /**
     * The main entry point for mojo.
     */
    @Override
    public void execute() throws MojoExecutionException {
        MavenProject currentProject = this.mavenSession.getCurrentProject();
        getLog().debug(String.format("Current Project: %s:%s", currentProject.getGroupId(), currentProject.getArtifactId()));
        MavenProject topLevelProject = this.mavenSession.getTopLevelProject();
        getLog().debug(String.format("Top Level Project: %s:%s", topLevelProject.getGroupId(), topLevelProject.getArtifactId()));

        if (this.skip) {
            getLog().info("Skip execution ...");
            return;
        }

        validateAndPrepareParameters();

        final List<MavenProject> projects = mavenSession.getProjects();

        printOut(this.list(projects));
    }

}
