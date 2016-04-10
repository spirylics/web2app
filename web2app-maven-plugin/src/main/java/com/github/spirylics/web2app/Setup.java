package com.github.spirylics.web2app;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
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
        if (BooleanUtils.isTrue(clear)) {
            FileUtils.deleteDirectory(frontendDirectory);
            FileUtils.deleteDirectory(frontendWorkingDirectory);
        }
        Path packagePath = getTempPathFromResource("package.json");
        execMojo("org.apache.maven.plugins", "maven-resources-plugin"
                , configuration(
                        element(name("outputDirectory"), frontendWorkingDirectory.getAbsolutePath()),
                        element(name("overwrite"), "true"),
                        element(name("resources"),
                                element("resource",
                                        element(name("directory"), packagePath.getParent().toFile().getAbsolutePath()),
                                        element(name("filtering"), "true")
                                ))
                ), "copy-resources");
        execMojo("com.github.eirslett", "frontend-maven-plugin"
                , configuration(
                        element(name("nodeVersion"), getVersion("node")),
                        element(name("npmVersion"), getVersion("npm")),
                        element(name("installDirectory"), frontendDirectory.getAbsolutePath())
                ), "install-node-and-npm");
        execMojo("com.github.eirslett", "frontend-maven-plugin"
                , configuration(
                        element(name("arguments"), "install"),
                        element(name("installDirectory"), frontendDirectory.getAbsolutePath()),
                        element(name("workingDirectory"), frontendWorkingDirectory.getAbsolutePath())
                ), "npm");
    }
}