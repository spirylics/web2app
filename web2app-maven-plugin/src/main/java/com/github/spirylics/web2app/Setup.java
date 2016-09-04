package com.github.spirylics.web2app;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
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
		generatNpmPackage(frontendWorkingDirectory);
		execMojo("com.github.eirslett", "frontend-maven-plugin",
				configuration(element(name("nodeVersion"), getVersion("node")),
						element(name("npmVersion"), getVersion("npm")),
						element(name("installDirectory"), frontendDirectory.getAbsolutePath())),
				"install-node-and-npm");
		execMojo("com.github.eirslett", "frontend-maven-plugin",
				configuration(element(name("arguments"), "install"),
						element(name("installDirectory"), frontendDirectory.getAbsolutePath()),
						element(name("workingDirectory"), frontendWorkingDirectory.getAbsolutePath())),
				"npm");
	}

	void generatNpmPackage(File dest) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		NpmModel.Package npmPackage = new NpmModel.Package().setName(mavenProject.getName())
				.setVersion(mavenProject.getVersion())
				.addDependency("cordova", mavenProject.getProperties().getProperty("version.cordova"))
				.addConfig("unsafe-perm", true).addScript("prebuild", "npm install");
		if (getPlatforms().contains("ios")) {
			npmPackage.addDependency("ios-sim", mavenProject.getProperties().getProperty("version.ios-sim"))
					.addDependency("ios-deploy", mavenProject.getProperties().getProperty("version.ios-deploy"));
		}
		if (!dest.isDirectory()) {
			dest.mkdirs();
		}
		mapper.writeValue(new File(dest, "package.json"), npmPackage);
	}
}