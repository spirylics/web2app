package com.github.spirylics.web2app;


import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public abstract class Web2AppMojo extends AbstractMojo {

    @Parameter(defaultValue = "${web2app.clear}", readonly = true, required = false)
    Boolean clear;

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

    @Parameter(defaultValue = "${app.group}", readonly = true, required = true)
    String appGroup;

    @Parameter(defaultValue = "${app.name}", readonly = true, required = true)
    String appName;

    @Parameter(defaultValue = "${app.version}", readonly = true, required = true)
    String appVersion;

    @Parameter(defaultValue = "${app.version.code}", readonly = true, required = true)
    String appVersionCode;

    @Parameter(defaultValue = "${app.description}", readonly = true, required = true)
    String appDescription;

    @Parameter(defaultValue = "${app.author.email}", readonly = true, required = true)
    String appAuthorEmail;

    @Parameter(defaultValue = "${app.author.site}", readonly = true, required = true)
    String appAuthorSite;

    @Parameter(defaultValue = "${project.basedir}/config.xml", readonly = true, required = true)
    File appConfig;

    @Parameter(defaultValue = "index.html", readonly = true, required = true)
    String appContent;

    @Parameter(readonly = true, required = true)
    List<String> platforms = Arrays.asList("browser");

    @Parameter(readonly = true, required = true)
    List<String> plugins = Arrays.asList();

    @Parameter(defaultValue = "", readonly = true, required = true)
    String icon;

    @Parameter(defaultValue = "", readonly = true, required = true)
    String splashscreen;

    @Parameter(readonly = true, required = false)
    String themeColor = null;

    /**
     * Maven project
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject mavenProject;


    /**
     * Maven ProjectHelper.
     */
    @Component
    MavenProjectHelper projectHelper;

    /**
     * Maven session
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession mavenSession;

    @Component
    protected BuildPluginManager pluginManager;

    File tempDir;

    protected File getTempFile(String name) throws IOException {
        if (tempDir == null) {
            Path tempPath = Files.createTempDirectory("web2app-maven-plugin");
            tempDir = tempPath.toFile();
            tempDir.deleteOnExit();
        }
        return new File(tempDir, name);
    }

    protected Path getTempPathFromResource(String resourceName) throws IOException {
        InputStream templatePackageStream = getClass().getResourceAsStream(resourceName);
        Path path = getTempFile(resourceName).toPath();
        Files.deleteIfExists(path);
        Files.createDirectories(path.getParent());
        Files.copy(templatePackageStream, path);
        return path;
    }

    protected File getPlatformsDir() {
        return new File(appDirectory, "platforms");
    }

    protected File getPlatformDir(String name) {
        return new File(getPlatformsDir(), name);
    }

    protected File getWwwDir() {
        return new File(appDirectory, "www");
    }

    protected File getContentFile() {
        return new File(getWwwDir(), appContent);
    }

    protected void appendScript(File htmlFile, String scriptSrc) throws IOException {
        String content = FileUtils.readFileToString(htmlFile);
        if (!content.contains(scriptSrc)) {
            content.replaceFirst(
                    "</head>",
                    String.format("\t<script type=\"text/javascript\" src=\"%s\"></script>\n</head>", scriptSrc));
            Files.write(htmlFile.toPath(), content.getBytes());
        }
    }

    void execCordova(String action, File dir, String... args) throws Exception {
        CommandLine commandLine = new CommandLine(cordovaExec);
        commandLine.addArguments(args);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(dir);
        int exitValue = executor.execute(commandLine);
        if (exitValue == 0) {
            getLog().info(action + ": OK");
        } else {
            throw new MojoExecutionException(action + ": FAILED");
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            e();
        } catch (Exception e) {
            if (e instanceof MojoExecutionException) {
                throw (MojoExecutionException) e;
            } else {
                throw new MojoExecutionException(getClass().getSimpleName() + " FAILED", e);
            }
        }
    }

    protected abstract void e() throws Exception;
}
