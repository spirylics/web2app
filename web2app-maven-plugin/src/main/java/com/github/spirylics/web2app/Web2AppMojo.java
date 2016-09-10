package com.github.spirylics.web2app;


import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.environment.EnvironmentUtils;
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

    @Parameter(defaultValue = "${web2app.clear}")
    Boolean clear;

    /**
     * Directory where will be installed node & co
     */
    @Parameter(defaultValue = "${project.basedir}/frontend", required = true)
    File frontendDirectory;

    /**
     * Directory where will be installed frontend dependencies
     */
    @Parameter(defaultValue = "${project.basedir}/working", required = true)
    File frontendWorkingDirectory;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    File buildDirectory;

    @Parameter(defaultValue = "${project.basedir}/working/node_modules/cordova/bin/cordova", required = true)
    File cordovaExec;

    @Parameter(defaultValue = "${project.build.directory}/${project.name}-${project.version}", required = true)
    File appDirectory;

    @Parameter(required = true)
    Dependency dependency;

    @Parameter(defaultValue = "**", required = true)
    String dependencyIncludes;

    @Parameter(defaultValue = "META-INF/,WEB-INF/", required = true)
    String dependencyExcludes;

    @Parameter(defaultValue = "${app.group}", required = true)
    String appGroup;

    @Parameter(defaultValue = "${app.name}", required = true)
    String appName;

    @Parameter(defaultValue = "${app.version}", required = true)
    String appVersion;

    @Parameter(defaultValue = "${app.version.code}", required = true)
    String appVersionCode;

    @Parameter(defaultValue = "${app.description}")
    String appDescription;

    @Parameter(defaultValue = "${app.author.email}")
    String appAuthorEmail;

    @Parameter(defaultValue = "${app.author.site}")
    String appAuthorSite;

    @Parameter(defaultValue = "${app.content}", required = true)
    String appContent;

    @Parameter(defaultValue = "${app.icon}", required = true)
    String appIcon;

    @Parameter(defaultValue = "${app.splashscreen}", required = true)
    String appSplashscreen;

    @Parameter(defaultValue = "${app.themeColor}", required = true)
    String appThemeColor = null;

    @Parameter(defaultValue = "${project.basedir}/config.xml", required = true)
    File appConfig;

    @Parameter(defaultValue = "${platforms}", required = true)
    private List<String> platforms = Arrays.asList("browser");

    @Parameter(required = true)
    List<String> plugins = Arrays.asList();

    @Parameter(defaultValue = "${build.type}", required = true)
    private BuildType buildType;

    @Parameter(defaultValue = "${sign.keystore}", required = true)
    File signKeystore;

    @Parameter(defaultValue = "${sign.keypass}", required = true)
    String signKeypass;

    @Parameter(defaultValue = "${sign.storepass}")
    String signStorepass;

    @Parameter(defaultValue = "${sign.alias}")
    String signAlias;

    @Parameter(defaultValue = "SHA1withRSA")
    String signAlg;

    @Parameter(defaultValue = "SHA1")
    String signDigestalg;

    @Parameter(defaultValue = "RSA")
    String signKeyAlg;

    @Parameter(defaultValue = "2048")
    String signKeySize;

    @Parameter(defaultValue = "99999")
    String signValidity;

    /**
     * Maven project
     */
    @Parameter(defaultValue = "${project}", required = true)
    protected MavenProject mavenProject;


    /**
     * Maven ProjectHelper.
     */
    @Component
    MavenProjectHelper projectHelper;

    /**
     * Maven session
     */
    @Parameter(defaultValue = "${session}", required = true)
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
            Files.write(htmlFile.toPath(), content.replaceFirst(
                    "</head>",
                    String.format("\t<script type=\"text/javascript\" src=\"%s\"></script>\n</head>", scriptSrc)).getBytes());
            getLog().info(String.format("append script %s on %s", scriptSrc, htmlFile.getAbsolutePath()));
        }
    }

    void exec(String action, File dir, CommandLine cmdLine) {
        String label = action + ": " + cmdLine;
        try {
            DefaultExecutor executor = new DefaultExecutor();
            executor.setWorkingDirectory(dir);
            Map<String, String> environment = EnvironmentUtils.getProcEnvironment();
            environment.put("PATH", environment.get("PATH") + ":" + new File(frontendDirectory, "node").getAbsolutePath());
            int exitValue = executor.execute(cmdLine, environment);
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
        return mavenProject.getProperties().getProperty("version." + name);
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
        mavenProject.getProperties().putIfAbsent("version.node", "v5.10.1");
        mavenProject.getProperties().putIfAbsent("version.npm", "3.8.3");
        mavenProject.getProperties().putIfAbsent("version.cordova", "*");
        mavenProject.getProperties().putIfAbsent("version.ios-sim", "*");
        mavenProject.getProperties().putIfAbsent("version.ios-deploy", "*");
        mavenProject.getProperties().putIfAbsent("version.exec-maven-plugin", "1.4.0");
        mavenProject.getProperties().putIfAbsent("version.maven-dependency-plugin", "2.10");
        mavenProject.getProperties().putIfAbsent("version.maven-resources-plugin", "2.7");
        mavenProject.getProperties().putIfAbsent("version.image-maven-plugin", "1.2.1");
        mavenProject.getProperties().putIfAbsent("version.frontend-maven-plugin", "0.0.29");

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
