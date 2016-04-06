package com.github.spirylics.web2app;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.maven.plugins.annotations.LifecyclePhase.INITIALIZE;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Install cordova
 */
@Mojo(name = "install-cordova", defaultPhase = INITIALIZE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class InstallCordova  extends Web2AppMojo {

    public void execute() throws MojoExecutionException {
        try {
            mavenProject.getProperties().put("cordova.version", "~6.0.0");
            InputStream templatePackageStream = getClass().getResourceAsStream("package.json");
            Path tempPath = Files.createTempDirectory("web2app-maven-plugin");
            File tempFile = tempPath.toFile();
            tempFile.deleteOnExit();
            Path packagePath = new File(tempFile, "package.json").toPath();
            Files.deleteIfExists(packagePath);
            Files.createDirectories(packagePath.getParent());
            Files.copy(templatePackageStream, packagePath);
            executeMojo(
                    plugin(groupId("org.apache.maven.plugins"), artifactId("maven-resources-plugin"), version("2.7")),
                    goal("copy-resources"),
                    configuration(
                            element(name("outputDirectory"), frontendWorkingDirectory.getAbsolutePath()),
                            element(name("overwrite"), "true"),
                            element(name("resources"),
                                    element("resource",
                                            element(name("directory"), tempFile.getAbsolutePath()),
                                            element(name("filtering"), "true")
                                    ))
                    ),
                    executionEnvironment(
                            mavenProject,
                            mavenSession,
                            pluginManager
                    )
            );
            Plugin frontendPlugin = plugin(groupId("com.github.eirslett"), artifactId("frontend-maven-plugin"), version("0.0.29"));
            executeMojo(
                    frontendPlugin,
                    goal("install-node-and-npm"),
                    configuration(
                            element(name("nodeVersion"), "v5.5.0"),
                            element(name("npmVersion"), "3.6.0"),
                            element(name("installDirectory"), frontendDirectory.getAbsolutePath())
                    ),
                    executionEnvironment(
                            mavenProject,
                            mavenSession,
                            pluginManager
                    )
            );
            executeMojo(
                    frontendPlugin,
                    goal("npm"),
                    configuration(
                            element(name("arguments"), "install"),
                            element(name("installDirectory"), frontendDirectory.getAbsolutePath()),
                            element(name("workingDirectory"), frontendWorkingDirectory.getAbsolutePath())
                    ),
                    executionEnvironment(
                            mavenProject,
                            mavenSession,
                            pluginManager
                    )
            );
        } catch (Exception e) {
            throw new MojoExecutionException("CORDOVA INSTALL FAILED", e);
        }
    }
}