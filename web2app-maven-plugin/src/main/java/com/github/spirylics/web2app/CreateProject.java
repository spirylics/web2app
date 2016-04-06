package com.github.spirylics.web2app;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Make cordova project
 */
@Mojo(name = "create-project", defaultPhase = GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class CreateProject extends Web2AppMojo {

    public void execute() throws MojoExecutionException {
        try {
//            Files.createDirectories(buildDirectory.toPath());
//            if (buildDirectory.list().length == 0) {
//                execCordova("create cordova project", buildDirectory, "create", appDirectory.getAbsolutePath(), appGroup, appName);
//                FileUtils.deleteDirectory(getWwwDir());
//                Files.createDirectories(getWwwDir().toPath());
//            } else {
//                getLog().info("cordova project already created");
//            }
//            for (String platform : platforms) {
//                File platformDir = getPlatformDir(platform);
//                if (platformDir.exists()) {
//                    getLog().info(platform + " platform already exists");
//                } else {
//                    execCordova("add " + platform + " platform", appDirectory, "platforms", "add", platform);
//                    Files.walkFileTree(platformDir.toPath(), new SimpleFileVisitor<Path>() {
//                        @Override
//                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
//                                throws IOException {
//                            String filename = file.toFile().getName();
//                            if (filename.endsWith(".png") && !filename.matches(".*ic_.*.png")) {
//                                Files.deleteIfExists(file);
//                            }
//                            return FileVisitResult.CONTINUE;
//                        }
//                    });
//                }
//            }
//            for (String plugin : plugins) {
//                execCordova("add " + plugin + " plugin", appDirectory, "plugin", "add", plugin);
//            }
            importWebApp();
        } catch (Exception e) {
            if (e instanceof MojoExecutionException) {
                throw (MojoExecutionException) e;
            } else {
                throw new MojoExecutionException("create cordova project FAILED", e);
            }
        }
    }

    void execCordova(String action, File dir, String... args) throws Exception {
        List<String> argList = Lists.newArrayList(cordovaExec.getAbsolutePath());
        argList.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(Iterables.toArray(argList, String.class));
        pb.directory(dir);
        Process p = pb.start();
        int result = p.waitFor();
        String info = CharStreams.toString(new InputStreamReader(p.getInputStream()));
        if (result == 0) {
            getLog().info(info);
            getLog().info(action + ": OK");
        } else {
            throw new MojoExecutionException(
                    this,
                    CharStreams.toString(new InputStreamReader(p.getErrorStream())),
                    info);
        }
    }

    void importWebApp() throws MojoExecutionException {
//        PluginExecution pluginExecution = new PluginExecution();
//        pluginExecution.setGoals(Arrays.asList("unpack"));
//        pluginExecution.setId("fi");
//        pluginExecution.setConfiguration(configuration(
//                element(name("overWriteReleases"), "false"),
//                element(name("overWriteSnapshots"), "true"),
//                element(name("artifactItems"),
//                        element("artifactItem",
//                                element(name("groupId"), dependency.getGroupId()),
//                                element(name("artifactId"), dependency.getArtifactId()),
////                                        element(name("classifier"), dependency.getClassifier()),
//                                element(name("version"), dependency.getVersion()),
//                                element(name("type"), dependency.getType()),
//                                element(name("overWrite"), "true"),
//                                element(name("outputDirectory"), getWwwDir().getAbsolutePath())
//                        ))
//        ));
//        dependencyPlugin.setExecutions(Arrays.asList(pluginExecution));
//        executeMojo(dependencyPlugin, goal("unpack"),
//                configuration(
//                        element(name("overWriteReleases"), "false"),
//                        element(name("overWriteSnapshots"), "true"),
//                        element(name("artifactItems"),
//                                element("artifactItem",
//                                        element(name("groupId"), dependency.getGroupId()),
//                                        element(name("artifactId"), dependency.getArtifactId()),
////                                        element(name("classifier"), dependency.getClassifier()),
//                                        element(name("version"), dependency.getVersion()),
//                                        element(name("type"), dependency.getType()),
//                                        element(name("overWrite"), "true"),
//                                        element(name("outputDirectory"), getWwwDir().getAbsolutePath())
//                                ))
//                ),
//                executionEnvironment(
//                        mavenProject,
//                        mavenSession,
//                        pluginManager
//                ));
        Dependency dependency2 = new Dependency();
        dependency2.setGroupId("org.codehaus.plexus");
        dependency2.setArtifactId("plexus-archiver");
        dependency2.setVersion("3.1");
        Dependency dependency3 = new Dependency();
        dependency3.setGroupId("org.apache.axis2");
        dependency3.setArtifactId("axis2-aar-maven-plugin");
        dependency3.setVersion("1.7.1");
        Dependency dependency4 = new Dependency();
        dependency4.setGroupId("org.apache.maven");
        dependency4.setArtifactId("maven-archiver");
        dependency4.setVersion("3.0.0");
        Dependency dependency6 = new Dependency();
        dependency6.setGroupId("org.codehaus.plexus");
        dependency6.setArtifactId("plexus-utils");
        dependency6.setVersion("3.0.22");
        Plugin dependencyPlugin = plugin("org.apache.maven.plugins", "maven-dependency-plugin", "2.10");
        Dependency dependency5 = new Dependency();
        dependency5.setGroupId("org.codehaus.plexus");
        dependency5.setArtifactId("plexus-archiver");
        dependency5.setVersion("1.0-alpha-7");
        dependencyPlugin.removeDependency(dependency5);
        executeMojo(
                dependencyPlugin,
                goal("unpack"),
                configuration(
                        element(name("overWriteReleases"), "false"),
                        element(name("overWriteSnapshots"), "true"),
//                        element(name("local"), "${project.properties.local}"),
                        element(name("artifactItems"),
                                element("artifactItem",
                                        element(name("groupId"), dependency.getGroupId()),
                                        element(name("artifactId"), dependency.getArtifactId()),
//                                        element(name("classifier"), dependency.getClassifier()),
                                        element(name("version"), dependency.getVersion()),
                                        element(name("type"), dependency.getType()),
                                        element(name("overWrite"), "true"),
                                        element(name("outputDirectory"), getWwwDir().getAbsolutePath())
                                ))
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );

//        executeMojo(
//                plugin(
//                        groupId("org.apache.maven.plugins"),
//                        artifactId("maven-dependency-plugin"),
//                        version("2.0")
//                ),
//                goal("copy-dependencies"),
//                configuration(
//                        element(name("outputDirectory"), "${project.build.directory}/foo")
//                ),
//                executionEnvironment(
//                        mavenProject,
//                        mavenSession,
//                        pluginManager
//                )
//        );
    }

    File getPlatformsDir() {
        return new File(appDirectory, "platforms");
    }

    File getPlatformDir(String name) {
        return new File(getPlatformsDir(), name);
    }

    File getWwwDir() {
        return new File(appDirectory, "www");
    }

}