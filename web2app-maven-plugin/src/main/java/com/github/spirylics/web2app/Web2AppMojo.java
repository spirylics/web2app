package com.github.spirylics.web2app;


import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public abstract class Web2AppMojo extends AbstractMojo {

    /**
     * Directory where will be installed node & co
     */
    @Parameter(defaultValue = "${project.basedir}/frontend", readonly = true, required = true)
    File frontendDirectory;

    /**
     * Directory where will be installed frontend dependencies
     */
    @Parameter(defaultValue = "${project.basedir}/working", readonly = true, required = true)
    File frontendWorkingDirectory;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    File buildDirectory;

    @Parameter(defaultValue = "${project.basedir}/working/node_modules/cordova/bin/cordova", readonly = true, required = true)
    File cordovaExec;

    @Parameter(defaultValue = "${project.build.directory}/${project.name}-${project.version}", readonly = true, required = true)
    File appDirectory;

    @Parameter(readonly = true, required = true)
    Dependency dependency;

    @Parameter(defaultValue = "**", readonly = true, required = true)
    String dependencyIncludes;

    @Parameter(defaultValue = "META-INF/,WEB-INF/", readonly = true, required = true)
    String dependencyExcludes;

    @Parameter(defaultValue = "${project.groupId}", readonly = true, required = true)
    String appGroup;

    @Parameter(defaultValue = "${project.artifactId}", readonly = true, required = true)
    String appName;

    @Parameter(readonly = true, required = true)
    List<String> platforms = Arrays.asList("browser");

    @Parameter(readonly = true, required = true)
    List<String> plugins = Arrays.asList();

    /**
     * Maven project
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject mavenProject;

    /**
     * Maven session
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession mavenSession;

    @Component
    protected BuildPluginManager pluginManager;
}
