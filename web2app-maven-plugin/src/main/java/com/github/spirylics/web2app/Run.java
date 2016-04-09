package com.github.spirylics.web2app;

import org.apache.maven.plugins.annotations.Mojo;

/**
 * Run cordova project
 */
@Mojo(name = "run")
public class Run extends Web2AppMojo {

    @Override
    public void e() throws Exception {
        runOrEmulate("run");
    }

}