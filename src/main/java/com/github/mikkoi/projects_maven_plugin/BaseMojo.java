package com.github.mikkoi.projects_maven_plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.List;

public abstract class BaseMojo extends AbstractMojo {

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    MavenSession mavenSession;

    /**
     * Skip execution
     */
    @Parameter(property = "projects" + ".skip", defaultValue = "false", alias = "skip")
    boolean skip;

    /**
     * Force output to STDOUT instead of using [INFO] logging level.
     */
    @Parameter(property = "projects" + ".forceStdout", defaultValue = "false", alias = "forceStdoutTwo")
    boolean forceStdout;

    /**
     * Run only in the project which is at execution root, i.e. the dir in which `mvn` is executed.
     */
    @Parameter(property = "projects" + ".runOnlyAtExecutionRoot", defaultValue = "true")
    boolean runOnlyAtExecutionRoot;

    /**
     * Give warning instead of error.
     */
    @Parameter(property = "projects" + ".errorIsWarning", defaultValue = "false")
    boolean errorIsWarning;

    /**
     * Is this project the last project in the reactor?
     *
     * @return true if last project (including only project when only one project)
     */
    boolean isLastProjectInReactor(MavenSession mavenSession) {
        MavenProject currentProject = this.mavenSession.getCurrentProject();
        getLog().debug(String.format("Current Project: %s:%s", currentProject.getGroupId(), currentProject.getArtifactId()));
        MavenProject topLevelProject = this.mavenSession.getTopLevelProject();
        getLog().debug(String.format("Top Level Project: %s:%s", topLevelProject.getGroupId(), topLevelProject.getArtifactId()));

        List<MavenProject> sortedProjects = mavenSession.getProjectDependencyGraph().getSortedProjects();
        MavenProject lastProject = sortedProjects.isEmpty() ? mavenSession.getCurrentProject() : sortedProjects.get(sortedProjects.size() - 1);
        // : sortedProjects.getLast();
        if (getLog().isDebugEnabled()) {
            getLog().debug("Current project: '" + mavenSession.getCurrentProject().getName() + "', Last project to execute based on dependency graph: '" + lastProject.getName() + "'");
        }
        return mavenSession.getCurrentProject().equals(lastProject);
    }

    /**
     * Print out the rows.
     * if forceStdout is set, then print to STDOUT, otherwise use logger.
     *
     * @param outRows List of strings for printing.
     */
    void printOut(List<String> outRows) {
        outRows.forEach(row -> {
            if (this.forceStdout) {
                System.out.println(row);
            } else {
                getLog().info(row);
            }
        });
    }

}
