package com.github.spirylics.web2app;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static org.apache.maven.plugins.annotations.LifecyclePhase.COMPILE;

/**
 * Create cordova project
 */
@Mojo(name = "build", defaultPhase = COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class Build extends Web2AppMojo {

    @Override
    public void e() throws Exception {
        execCordova("build", appDirectory, "build", " --" + getBuildType());
    }

}