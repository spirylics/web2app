package com.github.spirylics.web2app;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_RESOURCES;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Import resoucres
 */
@Mojo(name = "import", defaultPhase = GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class Import extends Web2AppMojo {

    @Override
    public void e() throws Exception {
        importWebApp();
        importConfig();
        injectCordovaJs();
        importImages();
    }

    void importWebApp() throws Exception {
        FileUtils.deleteDirectory(getWwwDir());
        Files.createDirectories(getWwwDir().toPath());
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

    void importImages() throws Exception {
        Files.walkFileTree(getPlatformsDir().toPath(), new SimpleFileVisitor<Path>() {
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
        Xpp3Dom configuration = configuration(new ImagesGenBuilder(getPlatformsDir().getAbsolutePath(), getWwwDir().getAbsolutePath(), themeColor, getPlatforms())
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