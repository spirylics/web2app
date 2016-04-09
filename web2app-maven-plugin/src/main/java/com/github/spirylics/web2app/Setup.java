package com.github.spirylics.web2app;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.nio.file.Path;

import static org.apache.maven.plugins.annotations.LifecyclePhase.INITIALIZE;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Install cordova
 */
@Mojo(name = "setup", defaultPhase = INITIALIZE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class Setup extends Web2AppMojo {

    @Override
    public void e() throws Exception {
        try {
            if (BooleanUtils.isTrue(clear)) {
                FileUtils.deleteDirectory(frontendDirectory);
                FileUtils.deleteDirectory(frontendWorkingDirectory);
            }
            mavenProject.getProperties().put("cordova.version", "~6.1.1");
            Path packagePath = getTempPathFromResource("package.json");
            executeMojo(
                    plugin(groupId("org.apache.maven.plugins"), artifactId("maven-resources-plugin"), version("2.7")),
                    goal("copy-resources"),
                    configuration(
                            element(name("outputDirectory"), frontendWorkingDirectory.getAbsolutePath()),
                            element(name("overwrite"), "true"),
                            element(name("resources"),
                                    element("resource",
                                            element(name("directory"), packagePath.getParent().toFile().getAbsolutePath()),
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
                            element(name("nodeVersion"), "v5.10.1"),
                            element(name("npmVersion"), "3.8.3"),
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