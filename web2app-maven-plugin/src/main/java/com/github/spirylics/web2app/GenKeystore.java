package com.github.spirylics.web2app;

import org.apache.maven.plugins.annotations.Mojo;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Generate keystore
 */
@Mojo(name = "genkeystore")
public class GenKeystore extends Web2AppMojo {

    @Override
    public void e() throws Exception {
        executeMojo(
                plugin("org.codehaus.mojo", "exec-maven-plugin", "1.4.0"),
                goal("exec"),
                configuration(
                        element("executable", "keytool"),
                        element(name("arguments"),
                                element("argument", "-genkey"),
                                element("argument", "-v"),
                                element("argument", "-keystore"),
                                element("argument", signKeystore.getAbsolutePath()),
                                element("argument", "-keypass"),
                                element("argument", signKeypass),
                                element("argument", "-storepass"),
                                element("argument", signStorepass),
                                element("argument", "-alias"),
                                element("argument", signAlias),
                                element("argument", "-keyAlg"),
                                element("argument", signKeyAlg),
                                element("argument", "-keysize"),
                                element("argument", signKeySize),
                                element("argument", "-validity"),
                                element("argument", signValidity)
                        )
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }

}