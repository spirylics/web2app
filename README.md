# web2app

Web2app transforms a web application, from a war dependency to an application based on cordova.

## Prerequisites

JDK8+, Maven3+ and SDKs of each platforms like android sdk or xcode.

## Features

web2app creates an application lifecycle to generate applications for each platforms taking care of boring business.

* setup: Install node cordova locally - comfort test of new version and multiproject management

* create: Make cordova project and add platforms and plugins required

* import: Import usefull webapp resources from war dependency, configuration, inject cordova javascript and generate scaled pictures for each platforms

* build: Build project

* package: Sign archives if necessary and make a zip with all platforms

In more you can::

* Generate a keystore at the beginning to release your application:
        
        mvn web2app:genkeystore
    
* Run on an emulator

        mvn web2app:emulate -Dplatforms=android,ios
    
        mvn web2app:emulate -Dplatforms=android#nexus5,ios#iphone6
    
* Run on a device
    
        mvn web2app:run -Dplatforms=android,ios
        
        mvn web2app:run -Dplatforms=android#12345,ios#12345
    

## Examples

```xml
    <properties>
        <app.group>${project.groupId}</app.group>
        <app.name>${project.artifactId}</app.name>
        <app.id>${app.group}.${app.name}</app.id>
        <app.version>${project.version}</app.version>
        <app.version.code>1</app.version.code>
        <app.description>${project.description}</app.description>
        <app.author.email>web2app@nope.mail</app.author.email>
        <app.author.site>${project.url}</app.author.site>
        <app.content>CarStore.html</app.content>
        <app.icon>icons/ic_launcher.png</app.icon>
        <app.splashscreen>${project.basedir}/splashscreen.png</app.splashscreen>
        <app.themeColor>0xF8F8F8</app.themeColor>
        <platforms>browser,android,ios</platforms>
        <build.type>release</build.type>
        <sign.keystore>${basedir}/app.keystore</sign.keystore>
        <sign.alias>${app.name}</sign.alias>
        <sign.keypass>azerty</sign.keypass>
        <sign.storepass>${sign.keypass}</sign.storepass>
        <version.node>v5.10.1</version.node>
        <version.npm>3.8.3</version.npm>
        <version.cordova>6.1.1</version.cordova>
    </properties>
    <build>
        <plugins>
            <plugin>
                <groupId>com.github.spirylics.web2app</groupId>
                <artifactId>web2app-maven-plugin</artifactId>
                <version>${project.parent.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>setup</goal>
                            <goal>create</goal>
                            <goal>import</goal>
                            <goal>build</goal>
                            <goal>package</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <plugins>
                        <plugin>https://github.com/robertklein/cordova-ios-security.git</plugin>
                        <plugin>cordova-plugin-device</plugin>
                        <plugin>cordova-plugin-splashscreen</plugin>
                        <plugin>cordova-plugin-statusbar</plugin>
                        <plugin>ionic-plugin-keyboard</plugin>
                        <plugin>cordova-plugin-inappbrowser</plugin>
                        <plugin>cordova-plugin-crosswalk-webview</plugin>
                        <plugin>cordova-plugin-geolocation</plugin>
                        <plugin>cordova-plugin-networkactivityindicator</plugin>
                        <plugin>cordova-plugin-network-information</plugin>
                    </plugins>
                    <dependency>
                        <groupId>com.gwtplatform</groupId>
                        <artifactId>gwtp-carstore</artifactId>
                        <version>1.2.1</version>
                        <type>war</type>
                    </dependency>
                </configuration>
            </plugin>
        </plugins>
    </build>
```
