package com.github.mikkoi.projects_maven_plugin;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Add one or more dependencies into the project.
 * These must be artifacts from the same root project.
 * This goal can be used to, for example, dynamically add all subprojects
 * as dependencies for
 * <a href="https://www.eclemma.org/jacoco/trunk/doc/report-aggregate-mojo.html">Jacoco report-aggregate goal</a>.
 */
@Mojo(name = "add-internal")
public class AddInternalMojo extends BaseMojo {

    /**
     * Skip execution
     */
    @Parameter(property = "projects" + ".addInternal" + ".skip", defaultValue = "false", alias = "skip")
    boolean thisMojoSkip;

    /**
     * Exclude by project [groupId:]artifactId.
     * Default value: No projects excluded.
     * If includes list contains any items, they are evaluated first.
     * Then excludes are excluded from them.
     */
    @Parameter(property = "projects" + ".addInternal" + ".excludes")
    List<String> excludes;

    /**
     * Include by project [groupId:]artifactId.
     * Default value: all projects included.
     */
    @Parameter(property = "projects" + ".addInternal" + ".includes")
    private List<String> includes;

    /**
     * Error if unknown project in includes/excludes.
     * If a wildcard (*) is used in the name, this parameter has no effect.
     */
    @Parameter(property = "projects" + ".errorIfUnknownProject", defaultValue = "true")
    private boolean errorIfUnknownProject;

    /**
     * Sorting order: maven | alphabetic, default: maven
     */
    @Parameter(property = "projects" + ".addInternal" + ".sortOrder", defaultValue = "maven")
    String sortOrder;

    /**
     * Format for printing.
     */
    @Parameter(property = "projects" + ".addInternal" + ".printFormat", defaultValue = "{groupId}:{artifactId}")
    private String printFormat;

    /**
     * The main entry point for mojo.
     */
    @Override
    public void execute() {
        if (this.skip || this.thisMojoSkip) {
            return;
        }
        validateParameters();

//        if (runOnlyAtExecutionRoot && !isLastProjectInReactor(this.mavenSession)) {
//            return;
//        }
        // if includes/excludes project artifactId and groupId are full and not found
        // getLog().warn("Parameter validation: Include project ** not found.")
        // or MojoExecutionException

        final List<MavenProject> projects = mavenSession.getProjects();
        final List<String> outRows = addInternal(projects);

        printOut(outRows);
    }

    /**
     * Validate parameters provided via properties
     * either on the command line or using configuration element in pom.
     */
    private void validateParameters() {
        getLog().debug("includes=" + includes);
        getLog().debug("excludes=" + excludes);
    }

    /**
     * Add internal projects to the current project's dependencies.
     *
     * @param projects      List<MavenProject>
     * @return strings      List of strings ready for writing out
     */
    public List<String> addInternal(List<MavenProject> projects) {
        getLog().debug("Begin of projects:add-internal");
//        Comparator<MavenProject> comparator = getMavenProjectComparator(this.sortOrder);
        List<String> rows = new ArrayList<>();
//        projects.stream().filter(this::isIncluded).sorted(comparator).forEach(mavenProject -> {
//            rows.add(formatProject(mavenProject));
//        });
        MavenProject currentProject = this.mavenSession.getCurrentProject();
        getLog().debug(String.format("Current Project: %s:%s", currentProject.getGroupId(), currentProject.getArtifactId()));

        getLog().debug("Iterate through all projects in Maven Dependency Graph, i.e. the build.");
        mavenSession.getProjectDependencyGraph().getSortedProjects().forEach(project -> {
            getLog().debug(String.format("    %s:%s:%s",
                    project.getGroupId(),
                    project.getArtifactId(),
                    project.getVersion()
            ));
            if(!project.getArtifactId().equals(currentProject.getArtifactId()) && isIncluded(project)) {
                Dependency dependency = new Dependency();
                dependency.setGroupId(project.getGroupId());
                dependency.setArtifactId(project.getArtifactId());
                dependency.setVersion(project.getVersion());
                dependency.setScope("compile");
                @SuppressWarnings("unchecked") List<Dependency> currentProjectDependencies = currentProject.getDependencies();
                currentProjectDependencies.add(dependency);
//                new MarkupBuilder(xmlWriter).dependency {
//                    groupId(dependencyProject.getGroupId())
//                    artifactId(dependencyProject.getArtifactId())
//                    version(dependencyProject.getVersion())
//                }
//                xmlWriter.write("\n")
                getLog().info(String.format("Add dependency %s:%s:%s to project %s",
                        project.getGroupId(),
                        project.getArtifactId(),
                        project.getVersion(),
                        currentProject.getId()
                ));
            }
        });
        getLog().debug("End of iterate");

//        Files.createDirectories(Paths.get(project.getBuild().getDirectory()))
//        def String dependenciesFilePath = project.getBuild().getDirectory() + "/dependencies.xml"
//        log.info("Dependencies list written in {}", dependenciesFilePath)
//        def file = new File(dependenciesFilePath)
//        file.createNewFile()
//        file.text=xmlWriter
//                                    ]]></source>
        getLog().debug(":End of projects:add-internal");
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
     * Prepare string for use with Java's regexp libraries.
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
            return this.includes.stream().anyMatch(predicateForProjectId);
        } else {
            return this.includes.stream().anyMatch(predicateForProjectId) && this.excludes.stream().noneMatch(predicateForProjectId);
        }
    }

}
