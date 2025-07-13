package com.github.mikkoi.maven.plugin.projects;

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
     * Is this project the last project in the reactor?
     *
     * @return true if last project (including only project when only one project)
     */
    boolean isLastProjectInReactor(MavenSession mavenSession) {
        List<MavenProject> sortedProjects = mavenSession.getProjectDependencyGraph().getSortedProjects();
        MavenProject lastProject = sortedProjects.isEmpty() ? mavenSession.getCurrentProject() : sortedProjects.get(sortedProjects.size() - 1);
        // : sortedProjects.getLast();
        if (getLog().isDebugEnabled()) {
            getLog().debug("Current project: '" + mavenSession.getCurrentProject().getName() + "', Last project to execute based on dependency graph: '" + lastProject.getName() + "'");
        }
        return mavenSession.getCurrentProject().equals(lastProject);
    }

}
