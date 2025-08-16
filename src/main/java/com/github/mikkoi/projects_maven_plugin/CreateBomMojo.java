package com.github.mikkoi.projects_maven_plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Create a BOM POM from the current POM.
 * Remove all dependencies, profiles, build and properties from the current POM.
 * Empty dependencyManagement section.
 * Put all projects of the current build to its dependencyManagement section.
 */
@Mojo(name = "create-bom")
public class CreateBomMojo extends BaseMojo {

    /**
     * Skip execution
     */
    @Parameter(property = "projects" + ".createBom" + ".skip", defaultValue = "false")
    boolean thisMojoSkip;

    /**
     * BOM path. Filepath to write the new pom.xml.
     */
    @Parameter(property = "projects" + ".createBom" + ".bomFilepath", alias = "bomFilepath", defaultValue = "DEFAULT_TO_BE_REPLACED")
    String bomFilepath;

    /**
     * BOM file groupId.
     */
    @Parameter(property = "projects" + ".createBom" + ".bomGroupId", alias = "bomGroupId")
    String bomGroupId;

    /**
     * BOM file artifactId.
     */
    @Parameter(property = "projects" + ".createBom" + ".bomArtifactId", alias = "bomArtifactId")
    String bomArtifactId;

    /**
     * BOM file version.
     */
    @Parameter(property = "projects" + ".createBom" + ".bomVersion", alias = "bomVersion")
    String bomVersion;

    /**
     * BOM name.
     */
    @Parameter(property = "projects" + ".createBom" + ".bomName", alias = "bomName")
    String bomName;

    /**
     * Include the projects of the current build.
     */
    @Parameter(property = "projects" + ".createBom" + ".includeProjects", alias = "includeProjects", defaultValue = "true")
    boolean includeProjects;

    /**
     * Include the dependencies of the current build.
     * Not implemented.
     */
    @Parameter(property = "projects" + ".createBom" + ".includeDependencies", alias = "includeDependencies", defaultValue = "false")
    boolean includeDependencies;

    /**
     * Error if same dependency is included several times with different versions.
     * Not implemented.
     */
    @Parameter(property = "projects" + ".createBom" + ".errorDependencyConvergence", alias = "errorDependencyConvergence", defaultValue = "true")
    boolean errorDependencyConvergence;

    /**
     * Make the bom part of the current project by adding it as a module.
     */
    @Parameter(property = "projects" + ".createBom" + ".attachToCurrentProject", alias = "attachToCurrentProject", defaultValue = "false")
    boolean attachToCurrentProject;

    private List<String> includes;
    /**
     * Include by project [groupId:]artifactId.
     * Default value: all projects included.
     * If includes list contains any items, they are evaluated first.
     * Then excludes are excluded from them.
     */
    @Parameter(property = "projects" + ".createBom" + ".includes", alias = "includes")
    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    private List<String> excludes;
    /**
     * Exclude by project [groupId:]artifactId.
     * Default value: No projects excluded.
     */
    @Parameter(property = "projects" + ".createBom" + ".excludes", alias = "excludes")
    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    /**
     * Sorting order: maven | alphabetic, default: maven
     */
    @Parameter(property = "projects" + ".createBom" + ".sortOrder", defaultValue = "maven")
    String sortOrder;

    /**
     * Validate parameters provided via properties
     * either on the command line or using configuration element in pom.
     */
    private void validateAndPrepareParameters() throws MojoExecutionException {
        getLog().debug("includes=" + includes.toString());
        getLog().debug("excludes=" + excludes.toString());
        getLog().debug("sortOrder=" + sortOrder);
        getLog().debug("bomPath=" + bomFilepath);
        getLog().debug("bomGroupId=" + bomGroupId);
        getLog().debug("bomArtifactId=" + bomArtifactId);
        getLog().debug("bomVersion=" + bomVersion);
        getLog().debug("bomName=" + bomName);

        for ( String a : includes ) {
            if (a == null) {
                throw new MojoExecutionException(includes, "Failure in parameter", "Failure in parameter 'includes'. String is null");
            }
        }
        if (includes.isEmpty()) {
            includes.add("*");
        }

        for ( String a : excludes ) {
            if (a == null) {
                throw new MojoExecutionException(excludes, "Failure in parameter", "Failure in parameter 'excludes'. String is null");
            }
        }

        if(! (sortOrder.equals("maven") || sortOrder.equals("alphabetical"))) {
            throw new MojoExecutionException(sortOrder, "Failure in parameter", "Failure in parameter 'sortOrder'. Allowed values: 'maven', 'alphabetical'.");
        }

        if(bomFilepath.equals("DEFAULT_TO_BE_REPLACED")) {
            // Replace with default: target/bom/pom.xml
            bomFilepath = mavenSession.getCurrentProject().getBuild().getDirectory() + "/bom/pom.xml";
        } else if (bomFilepath.isEmpty()) {
            throw new MojoExecutionException(bomFilepath, "Failure in parameter", "Failure in parameter 'bomPath'. String is null");
        }

        // Validate bomPath
        try {
            Path pathParent = Paths.get(bomFilepath).getParent();
            getLog().debug("pathParent=" + pathParent);
            if(pathParent != null) {
                Files.createDirectories(pathParent);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(bomFilepath, "Failure in parameter", String.format("Failure in parameter 'bomPath'. Cannot create path '%s'", Paths.get(bomFilepath)));
        }
        getLog().debug("bomPath(resolved)=" + bomFilepath);
    }

    /**
     * Create a comparator which compares Maven project groupId, artifactId and type
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
                String o1type = o1.getPackaging();
                String o2type = o2.getPackaging();
                int r = o1GroupId.compareTo(o2groupId);
                if (r != 0) {
                    return r;
                }
                int s = o1artifactId.compareTo(o2artifactId);
                if (s != 0) {
                    return s;
                }
                return o1type.compareTo(o2type);
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
            t = ".*:" + t + ":.*";
        }
        return t;
    }

    /**
     * Write a POM.
     *
     * @param path  String, path to file.
     * @param model Maven POM {@link org.apache.maven.model.Model}.
     */
    static void writePOM(Path path, Model model) throws IOException {
        final ModelWriter modelWriter = new DefaultModelWriter();
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            modelWriter.write(outputStream, null, model);
        }
    }

    /**
     * Convert list of projects to a list of strings.
     *
     * @param mavenSession MavenSession
     */
    void createBom(MavenSession mavenSession) {
        getLog().debug(String.format("Begin of createBom(%s)", mavenSession));
        final MavenProject currentProject = mavenSession.getCurrentProject();

        // Create a new model so we don't pollute the current one.
        final Model model = currentProject.getModel().clone();
        getLog().debug(String.format("model=%s", model));

        // Remove from model the following elements:
        model.setBuild(null);
        model.setDependencies(null);
        model.setPluginRepositories(null);
        model.getDependencies().clear();
        model.getProfiles().clear();
        model.setPrerequisites(null);
        model.setRepositories(null);
        model.setProperties(null);
        model.setReporting(null);
        model.setParent(null);
        model.setModules(null);

        // Add new details where available
        if (this.bomGroupId != null && !this.bomGroupId.isEmpty()) {
            model.setGroupId(this.bomGroupId);
        }
        if (this.bomArtifactId != null && !this.bomArtifactId.isEmpty()) {
            model.setArtifactId(this.bomArtifactId);
        }
        if (this.bomVersion != null && !this.bomVersion.isEmpty()) {
            model.setVersion(this.bomVersion);
        }
        if (this.bomName != null && !this.bomName.isEmpty()) {
            model.setName(this.bomName);
        }

        // Clear dependencyManagement and fill it with all projects in the build.
        getLog().debug(String.format("model.getDependencyManagement=%s", model.getDependencyManagement()));
        model.setDependencyManagement(new DependencyManagement());

        if(includeProjects) {
            final List<MavenProject> projects = mavenSession.getProjects();
            Comparator<MavenProject> comparator = getMavenProjectComparator(this.sortOrder);
            projects.stream().filter(this::isIncluded).sorted(comparator).forEach(mavenProject -> {
                final Dependency dependency = new Dependency();
                dependency.setGroupId(mavenProject.getGroupId());
                dependency.setArtifactId(mavenProject.getArtifactId());
                dependency.setVersion(mavenProject.getVersion());
                dependency.setType(mavenProject.getArtifact().getType());
                model.getDependencyManagement().addDependency(dependency);
            });
        }

        // Save the BOM POM
        try {
            writePOM(Paths.get(this.bomFilepath).toAbsolutePath(), model);
            getLog().info(String.format("BOM saved in %s", this.bomFilepath));
        } catch (IOException e) {
            getLog().error(String.format("Cannot write POM %s", this.bomFilepath), e);
        }

        if(attachToCurrentProject) {
            final Model currentModel = currentProject.getOriginalModel();
            final List<String> modules = currentModel.getModules();
            modules.add(this.bomFilepath);
            java.io.File modelFile = currentModel.getPomFile();
            java.nio.file.Path p = Paths.get(modelFile.getAbsolutePath());
            try {
                writePOM(p, currentModel);
            } catch (IOException e) {
                getLog().error(String.format("Cannot write POM %s", p), e);
            }
        }
        getLog().debug(":End of createBom");
    }

    public boolean isIncluded(MavenProject mavenProject) {
        String projectGroupId = mavenProject.getGroupId();
        String projectArtifactId = mavenProject.getArtifactId();
        String projectType = mavenProject.getPackaging();
        String projectId = projectGroupId + ":" + projectArtifactId + ":" + projectType;

        // Match from the end of the id, artifactId alone is enough.
        Predicate<String> predicateForProjectId = s -> projectId.matches(convertStringForMatching(s));
        return this.includes.stream().anyMatch(predicateForProjectId) && this.excludes.stream().noneMatch(predicateForProjectId);
    }

    /**
     * The main entry point for mojo.
     */
    @Override
    public void execute() throws MojoExecutionException {
        if (skip || thisMojoSkip) {
            getLog().info("Skip execution ...");
            return;
        }
        MavenProject currentProject = this.mavenSession.getCurrentProject();
        getLog().debug(String.format("Current Project: %s:%s", currentProject.getGroupId(), currentProject.getArtifactId()));
        MavenProject topLevelProject = this.mavenSession.getTopLevelProject();
        getLog().debug(String.format("Top Level Project: %s:%s", topLevelProject.getGroupId(), topLevelProject.getArtifactId()));

        if (runOnlyAtExecutionRoot && !this.mavenSession.getCurrentProject().isExecutionRoot()) {
            getLog().debug("runOnlyAtExecutionRoot && !this.mavenSession.getCurrentProject().isExecutionRoot()");
            return;
        }

        validateAndPrepareParameters();

        createBom(mavenSession);
    }

}
