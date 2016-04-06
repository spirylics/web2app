package com.github.spirylics.web2app;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Create cordova project
 */
@Mojo(name = "create-project", defaultPhase = GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class CreateProject extends Web2AppMojo {

    public void execute() throws MojoExecutionException {
        try {
            setup();
            importWebApp();
            importConfig();
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

    void setup() throws Exception {
        Files.createDirectories(buildDirectory.toPath());
        if (buildDirectory.list().length == 0) {
            execCordova("create cordova project", buildDirectory, "create", appDirectory.getAbsolutePath(), appGroup, appName);
            FileUtils.deleteDirectory(getWwwDir());
            Files.createDirectories(getWwwDir().toPath());
        } else {
            getLog().info("cordova project already created");
        }
        for (String platform : platforms) {
            File platformDir = getPlatformDir(platform);
            if (platformDir.exists()) {
                getLog().info(platform + " platform already exists");
            } else {
                execCordova("add " + platform + " platform", appDirectory, "platforms", "add", platform);
                Files.walkFileTree(platformDir.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        String filename = file.toFile().getName();
                        if (filename.endsWith(".png") && !filename.matches(".*ic_.*.png")) {
                            Files.deleteIfExists(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        for (String plugin : plugins) {
            execCordova("add " + plugin + " plugin", appDirectory, "plugin", "add", plugin);
        }
    }

    void importWebApp() throws MojoExecutionException {
        executeMojo(
                plugin("org.apache.maven.plugins", "maven-dependency-plugin", "2.10"),
                goal("unpack"),
                configuration(
                        element(name("overWriteReleases"), "false"),
                        element(name("overWriteSnapshots"), "true"),
                        element(name("artifactItems"),
                                element("artifactItem",
                                        element(name("groupId"), dependency.getGroupId()),
                                        element(name("artifactId"), dependency.getArtifactId()),
                                        element(name("classifier"), dependency.getClassifier()),
                                        element(name("version"), dependency.getVersion()),
                                        element(name("type"), dependency.getType()),
                                        element(name("overWrite"), "true"),
                                        element(name("outputDirectory"), getWwwDir().getAbsolutePath()),
                                        element(name("includes"), dependencyIncludes),
                                        element(name("excludes"), dependencyExcludes)
                                ))
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }

    void importConfig() throws MojoExecutionException {
        executeMojo(
                plugin(groupId("org.apache.maven.plugins"), artifactId("maven-resources-plugin"), version("2.7")),
                goal("copy-resources"),
                configuration(
                        element(name("outputDirectory"), appDirectory.getAbsolutePath()),
                        element(name("overwrite"), "true"),
                        element(name("resources"),
                                element("resource",
                                        element(name("directory"), appConfig.getParentFile().getAbsolutePath()),
                                        element(name("filtering"), "true"),
                                        element(name("includes"), element(name("include"), appConfig.getName()))
                                ))
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }

}