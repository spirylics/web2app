package com.github.spirylics.web2app;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.FilenameUtils;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.util.Arrays;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;

public class ImagesGenBuilder {

    final String platformPath;
    final String androidResourcePath;
    final String color;
    final List<String> platforms;
    final List<MojoExecutor.Element> images;

    public ImagesGenBuilder(String platformPath, String color, List<String> platforms) {
        this.platformPath = platformPath;
        this.androidResourcePath = platformPath + "/android";
        this.color = color;
        this.platforms = platforms;
        this.images = Lists.newArrayList();
    }

    public ImagesGenBuilder addImage(String source) {
        platforms.forEach(platform -> {
            List<MojoExecutor.Element> images = null;
            switch (platform) {
                case "android":
                    images = android(source, 36, 48, 72, 96, 128);
                    break;
            }
            if (images != null) {
                this.images.addAll(images);
            }
        });
        return this;
    }

    public MojoExecutor.Element build() {
        return element("images", Iterables.toArray(images, MojoExecutor.Element.class));
    }

    public List<MojoExecutor.Element> android(
            String source,
            int ldpiWidth,
            int mdpiWidth,
            int hdpiWidth,
            int xhdpiWidth,
            int xxhdpiWidth) {
        String filename = FilenameUtils.getName(source);
        return Arrays.asList(
                imageElement(source, androidResourcePath + "/drawable-ldpi/" + filename, "" + ldpiWidth),
                imageElement(source, androidResourcePath + "/drawable-mdpi/" + filename, "" + mdpiWidth),
                imageElement(source, androidResourcePath + "/drawable-hdpi/" + filename, "" + hdpiWidth),
                imageElement(source, androidResourcePath + "/drawable-xhdpi/" + filename, "" + xhdpiWidth),
                imageElement(source, androidResourcePath + "/drawable-xxhdpi/" + filename, "" + xxhdpiWidth)
        );
    }

    public MojoExecutor.Element imageElement(
            String source,
            String destination,
            String width) {
        return element("image",
                element(name("source"), source),
                element(name("destination"), destination),
                element(name("width"), width));
    }

    public MojoExecutor.Element imageElement(
            String source,
            String destination,
            String width,
            String cropWidth,
            String cropHeight,
            String color) {
        return element("image",
                element(name("source"), source),
                element(name("destination"), destination),
                element(name("width"), width),
                element(name("cropWidth"), cropWidth),
                element(name("cropHeight"), cropHeight),
                element(name("color"), color));
    }

}
