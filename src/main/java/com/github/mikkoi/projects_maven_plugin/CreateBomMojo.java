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
    @Parameter(property = "projects" + ".createBom" + ".path", defaultValue = "DEFAULT_TO_BE_REPLACED")
    String bomPath;

    /**
     * BOM path. Filepath to write the new pom.xml.
     */
    @Parameter(property = "projects" + ".createBom" + ".groupId", alias = "groupId")
    String bomGroupId;

    /**
     * BOM path. Filepath to write the new pom.xml.
     */
    @Parameter(property = "projects" + ".createBom" + ".artifactId", alias = "artifactId")
    String bomArtifactId;

    /**
     * BOM path. Filepath to write the new pom.xml.
     */
    @Parameter(property = "projects" + ".createBom" + ".version", alias = "version")
    String bomVersion;

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
     * Include by project [groupId:]artifactId.
     * Default value: all projects included.
     * If includes list contains any items, they are evaluated first.
     * Then excludes are excluded from them.
     */
    @Parameter(property = "projects" + ".createBom" + ".includes")
    private List<String> includes;

    /**
     * Exclude by project [groupId:]artifactId.
     * Default value: No projects excluded.
     */
    @Parameter(property = "projects" + ".createBom" + ".excludes")
    List<String> excludes;

    /**
     * Sorting order: maven | alphabetic, default: maven
     */
    @Parameter(property = "projects" + ".createBom" + ".sortOrder", defaultValue = "maven")
    String sortOrder;

    /**
     * Validate parameters provided via properties
     * either on the command line or using configuration element in pom.
     */
    private void validateParameters() throws MojoExecutionException {
        getLog().debug("includes=" + includes.toString());
        getLog().debug("excludes=" + excludes.toString());
        getLog().debug("sortOrder=" + sortOrder);
        getLog().debug("bomPath=" + bomPath);

        for ( String a : includes ) {
            if (a == null) {
                throw new MojoExecutionException(includes, "Failure in parameter", "Failure in parameter 'includes'. String is null");
            }
        }
        for ( String a : excludes ) {
            if (a == null) {
                throw new MojoExecutionException(excludes, "Failure in parameter", "Failure in parameter 'excludes'. String is null");
            }
        }
        if(! (sortOrder.equals("maven") || sortOrder.equals("alphabetical"))) {
            throw new MojoExecutionException(sortOrder, "Failure in parameter", "Failure in parameter 'sortOrder'. Allowed values: 'maven', 'alphabetical'.");
        }
        if(bomPath.equals("DEFAULT_TO_BE_REPLACED")) {
            // Replace with default: target/bom/pom.xml
            bomPath = mavenSession.getCurrentProject().getBuild().getDirectory() + "/bom/pom.xml";
        } else if (bomPath.isEmpty()) {
            throw new MojoExecutionException(bomPath, "Failure in parameter", "Failure in parameter 'bomPath'. String is null");
        }
        // Validate bomPath
        try {
//            createDirectories(Paths.get(bomPath).toAbsolutePath());
            Files.createDirectories(Paths.get(bomPath).getParent());
        } catch (IOException e) {
            throw new MojoExecutionException(bomPath, "Failure in parameter", String.format("Failure in parameter 'bomPath'. Path '%s' not found", Paths.get(bomPath)));
        }
        if(Paths.get(bomPath).getFileName() == null) {
            throw new MojoExecutionException(bomPath, "Failure in parameter", String.format("Failure in parameter 'bomPath'. Path '%s' has no file", Paths.get(bomPath)));
        }
        getLog().debug("bomPath(resolved)=" + bomPath);
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
            comparator = (o1, o2) -> 0;
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
     * Create directories from string
     * @param filePath String
     * @throws IOException failure
     */
    static void createDirectories(Path filePath) throws IOException {
//        final Path path = Paths.get(filePath);
        try {
            Files.createDirectories(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

        // Clear dependencyManagement and fill it with all projects in the build.
        getLog().debug(String.format("model.getDependencyManagement=%s", model.getDependencyManagement()));
//        getLog().debug(String.format("model.getDependencyManagement.getDependencies=%s", model.getDependencyManagement().getDependencies()));
//        model.getDependencyManagement().getDependencies().clear();
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

//        // Replace the current pom.xml
//        final Path pomPath = Paths.get(currentProject.getFile().getPath());
//        try {
//            writePOM(pomPath, model);
//            getLog().info(String.format("Original POM replaced in %s", pomPath));
//        } catch (IOException e) {
//            getLog().error(String.format("Cannot overwrite POM %s", pomPath), e);
//        }

        // Save the BOM POM
        final String bomPath = this.bomPath;
        try {
            writePOM(Paths.get(this.bomPath).toAbsolutePath(), model);
            getLog().info(String.format("BOM saved in %s", bomPath));
        } catch (IOException e) {
            getLog().error(String.format("Cannot write POM %s", bomPath), e);
        }

        getLog().debug(":End of createBom");
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

//    /**
//     * <p>performCheckins.</p>
//     *
//     * @param releaseDescriptor  a {@link org.apache.maven.shared.release.config.ReleaseDescriptor} object
//     * @param releaseEnvironment a {@link org.apache.maven.shared.release.env.ReleaseEnvironment} object
//     * @param reactorProjects    a {@link java.util.List} object
//     * @param message            a {@link java.lang.String} object
//     * @throws org.apache.maven.shared.release.scm.ReleaseScmRepositoryException if any.
//     * @throws org.apache.maven.shared.release.ReleaseExecutionException         if any.
//     * @throws org.apache.maven.shared.release.scm.ReleaseScmCommandException    if any.
//     */
//    protected void performCheckins(
//            ReleaseDescriptor releaseDescriptor,
//            ReleaseEnvironment releaseEnvironment,
//            List<MavenProject> reactorProjects,
//            String message)
//            throws ReleaseScmRepositoryException, ReleaseExecutionException, ReleaseScmCommandException {
//
//        getLogger().info("Checking in modified POMs...");
//
//        ScmRepository repository;
//        ScmProvider provider;
//        try {
//            repository = scmRepositoryConfigurator.getConfiguredRepository(
//                    releaseDescriptor, releaseEnvironment.getSettings());
//
//            repository.getProviderRepository().setPushChanges(releaseDescriptor.isPushChanges());
//
//            repository.getProviderRepository().setWorkItem(releaseDescriptor.getWorkItem());
//
//            provider = scmRepositoryConfigurator.getRepositoryProvider(repository);
//        } catch (ScmRepositoryException e) {
//            throw new ReleaseScmRepositoryException(e.getMessage(), e.getValidationMessages());
//        } catch (NoSuchScmProviderException e) {
//            throw new ReleaseExecutionException("Unable to configure SCM repository: " + e.getMessage(), e);
//        }
//
//        if (releaseDescriptor.isCommitByProject()) {
//            for (MavenProject project : reactorProjects) {
//                List<File> pomFiles = createPomFiles(releaseDescriptor, project);
//                ScmFileSet fileSet = new ScmFileSet(project.getFile().getParentFile(), pomFiles);
//
//                checkIn(provider, repository, fileSet, releaseDescriptor, message);
//            }
//        } else {
//            List<File> pomFiles = createPomFiles(releaseDescriptor, reactorProjects);
//            ScmFileSet fileSet = new ScmFileSet(new File(releaseDescriptor.getWorkingDirectory()), pomFiles);
//
//            checkIn(provider, repository, fileSet, releaseDescriptor, message);
//        }
//    }
//
//    /**
//     * <p>Copied and modified from org.apache.maven.shared.release.phase.AbstractScmCommitPhase.</p>
//     */
//    private void checkIn(
//            ScmProvider provider,
//            ScmRepository repository,
//            ScmFileSet fileSet,
//            ReleaseDescriptor releaseDescriptor,
//            String message)
//            throws ReleaseExecutionException, ReleaseScmCommandException {
//        CheckInScmResult result;
//        try {
//            result = provider.checkIn(repository, fileSet, (ScmVersion) null, message);
//        } catch (ScmException e) {
//            throw new ReleaseExecutionException("An error is occurred in the check-in process: " + e.getMessage(), e);
//        }
//
//        if (!result.isSuccess()) {
//            throw new ReleaseScmCommandException("Unable to commit files", result);
//        }
//        if (releaseDescriptor.isRemoteTagging()) {
//            releaseDescriptor.setScmReleasedPomRevision(result.getScmRevision());
//        }
//    }

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

        validateParameters();

        createBom(mavenSession);
    }

}
