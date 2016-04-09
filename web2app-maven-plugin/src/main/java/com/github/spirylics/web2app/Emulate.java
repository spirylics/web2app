package com.github.spirylics.web2app;

import org.apache.maven.plugins.annotations.Mojo;

/**
 * Emulate cordova project
 */
@Mojo(name = "emulate")
public class Emulate extends Web2AppMojo {

    @Override
    public void e() throws Exception {
        runOrEmulate("emulate");
    }

}