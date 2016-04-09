package com.github.spirylics.web2app;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.nio.file.Files;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

/**
 * Create cordova project
 */
@Mojo(name = "create", defaultPhase = GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class Create extends Web2AppMojo {

    @Override
    public void e() throws Exception {
        Files.createDirectories(buildDirectory.toPath());
        if (buildDirectory.list().length == 0) {
            execCordova("create cordova project", buildDirectory, "create", appDirectory.getAbsolutePath(), appGroup, appName);
        } else {
            getLog().info("cordova project already created");
        }
        for (String platform : getPlatforms()) {
            File platformDir = getPlatformDir(platform);
            if (platformDir.exists()) {
                getLog().info(platform + " platform already exists");
            } else {
                execCordova("add " + platform + " platform", appDirectory, "platforms", "add", platform);
            }
        }
        for (String plugin : plugins) {
            execCordova("add " + plugin + " plugin", appDirectory, "plugin", "add", plugin);
        }
    }

}