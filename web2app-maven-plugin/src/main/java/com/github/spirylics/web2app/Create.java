package com.github.spirylics.web2app;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Create cordova project
 */
@Mojo(name = "create", defaultPhase = GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class Create extends Web2AppMojo {

    @Override
    public void e() throws Exception {
        init();
        importWebApp();
        importConfig();
        injectCordovaJs();
        importImages();
    }

    void init() throws Exception {
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

    void injectCordovaJs() throws IOException {
        appendScript(getContentFile(), "cordova.js");
    }

    void importImages() throws MojoExecutionException {
        Xpp3Dom configuration = configuration(new ImagesGenBuilder(getPlatformsDir().getAbsolutePath(), getWwwDir().getAbsolutePath(), themeColor, platforms)
                .addIcon(icon)
                .addSplashscreen(splashscreen)
                .build());
        executeMojo(
                plugin("com.filmon.maven", "image-maven-plugin", "1.2.1"),
                "scale",
                configuration,
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
        executeMojo(
                plugin("com.filmon.maven", "image-maven-plugin", "1.2.1"),
                "crop",
                configuration,
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }

}