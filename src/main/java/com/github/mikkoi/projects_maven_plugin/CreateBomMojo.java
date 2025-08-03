package com.github.mikkoi.projects_maven_plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelWriter;
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
     * Exclude by project [groupId:]artifactId.
     * Default value: No projects excluded.
     * If includes list contains any items, they are evaluated first.
     * Then excludes are excluded from them.
     */
    @Parameter(property = "projects" + ".createBom" + ".excludes")
    List<String> excludes;
    /**
     * Sorting order: maven | alphabetic, default: maven
     */
    @Parameter(property = "projects" + ".createBom" + ".sortOrder", defaultValue = "maven")
    String sortOrder;
    /**
     * Include by project [groupId:]artifactId.
     * Default value: all projects included.
     */
    @Parameter(property = "projects" + ".createBom" + ".includes")
    private List<String> includes;

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
     * The main entry point for mojo.
     */
    @Override
    public void execute() {
        if (skip || thisMojoSkip) {
            return;
        }
        MavenProject currentProject = this.mavenSession.getCurrentProject();
        getLog().debug(String.format("Current Project: %s:%s", currentProject.getGroupId(), currentProject.getArtifactId()));
        MavenProject topLevelProject = this.mavenSession.getTopLevelProject();
        getLog().debug(String.format("Top Level Project: %s:%s", topLevelProject.getGroupId(), topLevelProject.getArtifactId()));

        if (runOnlyAtExecutionRoot && !isLastProjectInReactor(this.mavenSession)) {
            return;
        }

        validateParameters();

        createBom(mavenSession);
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
        getLog().debug("Begin of createBom:");
        final MavenProject currentProject = mavenSession.getCurrentProject();
        final Model model = currentProject.getModel();

        // Backup the original pom.xml
        final Path backupPomPath = Paths.get(currentProject.getFile().getPath() + ".bak");
        try {
            writePOM(backupPomPath, model);
            getLog().info(String.format("Backup POM written in %s", backupPomPath));
        } catch (IOException e) {
            getLog().error(String.format("Cannot write Backup POM %s", backupPomPath), e);
        }

        // Remove from model the following elements:
        model.setBuild(null);
        model.setDependencies(null);
        model.setPluginRepositories(null);
        model.getDependencies().clear();
        model.getProfiles().clear();
        model.setPrerequisites(null);
        model.setRepositories(null);

        // Clear dependencyManagement and fill it with all projects in the build.
        model.getDependencyManagement().getDependencies().clear();
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

        // Replace the current pom.xml
        final Path pomPath = Paths.get(currentProject.getFile().getPath());
        try {
            writePOM(pomPath, model);
            getLog().info(String.format("Original POM replaced in %s", pomPath));
        } catch (IOException e) {
            getLog().error(String.format("Cannot overwrite POM %s", pomPath), e);
        }


//        final ModelWriter modelWriter = new DefaultModelWriter();
////        final java.nio.file.Path backupPomPath = Paths.get(currentProject.getBasedir().getPath());
//        final java.nio.file.Path backupPomPath = Paths.get(currentProject.getFile().getPath() + ".bak");
//        final File backupPom = new File(orgPomFilePath);
//        BasicFileAttributeView basicFileAttributeView = Files.get
////        final File backupPom = Files.createFile(backupPomPath);
//        final File orgPomFilePath = currentProject.getFile();
////        final java.nio.file.attribute.FileAttribute<
//        modelWriter.write(backupPom, null, currentProject.getModel());
//        orgPomFile.createNewFile();
//        modelWriter.write(orgPomFile, null, project.getModel());
//        Files.createFile()

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

}
