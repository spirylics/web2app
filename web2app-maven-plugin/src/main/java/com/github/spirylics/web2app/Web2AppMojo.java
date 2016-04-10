package com.github.spirylics.web2app;


import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

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

    @Parameter(defaultValue = "${platforms}", readonly = true, required = true)
    private List<String> platforms = Arrays.asList("browser");

    @Parameter(readonly = true, required = true)
    List<String> plugins = Arrays.asList();

    @Parameter(defaultValue = "", readonly = true, required = true)
    String icon;

    @Parameter(defaultValue = "", readonly = true, required = true)
    String splashscreen;

    @Parameter(readonly = true, required = false)
    String themeColor = null;

    @Parameter(defaultValue = "${build.type}", readonly = true, required = true)
    private BuildType buildType;

    @Parameter(defaultValue = "${sign.keystore}", readonly = true, required = true)
    File signKeystore;

    @Parameter(defaultValue = "${sign.keypass}", readonly = true, required = true)
    String signKeypass;

    @Parameter(defaultValue = "${sign.storepass}", readonly = true, required = false)
    String signStorepass;

    @Parameter(defaultValue = "${sign.alias}", readonly = true, required = false)
    String signAlias;

    @Parameter(defaultValue = "SHA1withRSA", readonly = true, required = false)
    String signAlg;

    @Parameter(defaultValue = "SHA1", readonly = true, required = false)
    String signDigestalg;

    @Parameter(defaultValue = "RSA", readonly = true, required = false)
    String signKeyAlg;

    @Parameter(defaultValue = "2048", readonly = true, required = false)
    String signKeySize;

    @Parameter(defaultValue = "99999", readonly = true, required = false)
    String signValidity;

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

    public Map<String, String> getPlatformsDeviceMap() {
        return platforms.stream().map(d -> d.split("#")).collect(Collectors.toMap(
                d -> d[0],
                d -> d.length == 2 ? d[1] : ""
        ));
    }

    public List<String> getPlatforms() {
        return platforms.stream().map(d -> d.split("#")[0]).collect(Collectors.toList());
    }

    public BuildType getBuildType() {
        return buildType == null ? BuildType.release : buildType;
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

    void exec(String action, File dir, CommandLine cmdLine) {
        String label = action + ": " + cmdLine;
        try {
            DefaultExecutor executor = new DefaultExecutor();
            executor.setWorkingDirectory(dir);
            int exitValue = executor.execute(cmdLine);
            if (exitValue == 0) {
                getLog().info(label + ": OK");
            } else {
                throw new MojoExecutionException("EXEC FAILURE: " + label);
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException("EXEC FAILURE: " + label, e);
        }
    }

    void execCordova(String action, File dir, String... args) {
        CommandLine commandLine = new CommandLine(cordovaExec);
        commandLine.addArguments(args);
        exec(action, dir, commandLine);
    }

    void runOrEmulate(String runOrEmulate) {
        getPlatformsDeviceMap().entrySet().forEach(e -> {
            try {
                List<String> args = Lists.newArrayList(runOrEmulate, e.getKey());
                if (!Strings.isNullOrEmpty(e.getValue())) {
                    args.add("--target=\"" + e.getValue() + "\"");
                }
                execCordova(runOrEmulate + " " + Joiner.on(" ").join(args), appDirectory, Iterables.toArray(args, String.class));
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
    }

    protected String getVersion(String name) {
        return mavenProject.getProperties().getProperty(name + ".version");
    }

    protected void execMojo(String groupId, String artifactId, Xpp3Dom configuration, String goal, String... goals) throws MojoExecutionException {
        List<String> goalList = Lists.newArrayList(goals);
        goalList.add(0, goal);
        for (String g : goalList) {
            executeMojo(
                    plugin(groupId, artifactId, getVersion(artifactId)),
                    g,
                    configuration,
                    executionEnvironment(
                            mavenProject,
                            mavenSession,
                            pluginManager
                    )
            );
        }
    }

    @Override
    public final void execute() throws MojoExecutionException {
        mavenProject.getProperties().putIfAbsent("node.version", "v5.10.1");
        mavenProject.getProperties().putIfAbsent("npm.version", "3.8.3");
        mavenProject.getProperties().putIfAbsent("cordova.version", "*");
        mavenProject.getProperties().putIfAbsent("ios-sim.version", "*");
        mavenProject.getProperties().putIfAbsent("ios-deploy.version", "*");
        mavenProject.getProperties().putIfAbsent("exec-maven-plugin.version", "1.4.0");
        mavenProject.getProperties().putIfAbsent("maven-dependency-plugin.version", "2.10");
        mavenProject.getProperties().putIfAbsent("maven-resources-plugin.version", "2.7");
        mavenProject.getProperties().putIfAbsent("image-maven-plugin.version", "1.2.1");
        mavenProject.getProperties().putIfAbsent("frontend-maven-plugin.version", "0.0.29");

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
