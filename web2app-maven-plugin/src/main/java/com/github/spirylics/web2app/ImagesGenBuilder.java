package com.github.spirylics.web2app;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.Fraction;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;

public class ImagesGenBuilder {
    class Size {
        final Integer width;
        final Integer height;

        public Size(Integer width) {
            this(width, null);
        }

        public Size(Integer width, Integer height) {
            this.width = width;
            this.height = height;
        }

        public Size scale(Fraction ratio) {
            return new Size(
                    width == null ? null : scale(width, ratio),
                    height == null ? null : scale(height, ratio)
            );
        }

        public int scale(int value, Fraction ratio) {
            return ratio.multiplyBy(Fraction.getFraction(value, 1)).intValue();
        }

    }

    enum Type {
        REGULAR(""), PORTRAIT("port-"), LANDSCAPE("land-");

        final Map<String, Fraction> androidDensityRatioMap;

        Type(String androidPrefix) {
            this.androidDensityRatioMap = new ImmutableMap.Builder<String, Fraction>()
                    .put(androidPrefix + "ldpi", Fraction.getFraction(3, 16))
                    .put(androidPrefix + "mdpi", Fraction.getFraction(4, 16))
                    .put(androidPrefix + "hdpi", Fraction.getFraction(6, 16))
                    .put(androidPrefix + "xhdpi", Fraction.getFraction(8, 16))
                    .put(androidPrefix + "xxdpi", Fraction.getFraction(12, 16))
                    .put(androidPrefix + "xxxdpi", Fraction.getFraction(16, 16))
                    .build();
        }
    }

    final String platformPath;
    final String wwwPath;
    final String androidResourcePath;
    final String color;
    final List<String> platforms;
    final List<MojoExecutor.Element> images;

    public ImagesGenBuilder(String platformPath, String wwwPath, String color, List<String> platforms) {
        this.platformPath = platformPath;
        this.wwwPath = wwwPath;
        this.androidResourcePath = platformPath + "/android";
        this.color = color;
        this.platforms = platforms;
        this.images = Lists.newArrayList();
    }


    public ImagesGenBuilder addIcon(final String source) {
        return addImage(source, "icon.png", new Size(192), Type.REGULAR);
    }


    public ImagesGenBuilder addSplashscreen(final String source) {
        addImage(source, "screen.png", new Size(1280, 1920), Type.PORTRAIT)
                .addImage(source, "screen.png", new Size(1920, 1280), Type.LANDSCAPE);
        return this;
    }

    public ImagesGenBuilder addImage(final String source, final Size largestSize, final Type type) {
        return addImage(source, null, largestSize, type);
    }

    public ImagesGenBuilder addImage(final String source, final String destinationFilename, final Size largestSize, final Type type) {
        if (!Strings.isNullOrEmpty(source)) {
            addImageAsIs(
                    source.startsWith("/") ? source : wwwPath + "/" + source,
                    Strings.isNullOrEmpty(destinationFilename) ? FilenameUtils.getName(source) : destinationFilename,
                    largestSize,
                    type);
        }
        return this;
    }

    ImagesGenBuilder addImageAsIs(final String source, final String destinationFilename, final Size largestSize, final Type type) {
        platforms.forEach(platform -> {
            List<MojoExecutor.Element> images = null;
            switch (platform) {
                case "android":
                    images = android(source, destinationFilename, largestSize, type);
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

    public List<MojoExecutor.Element> android(String source, String destinationFileName, Size largestSize, Type type) {
        return type.androidDensityRatioMap.entrySet().stream().map(
                e -> toElement(
                        source,
                        String.format("%s/drawable-%s/%s", androidResourcePath, e.getKey(), destinationFileName),
                        largestSize.scale(e.getValue())))
                .collect(Collectors.toList());
    }

    public MojoExecutor.Element toElement(String source, String destination, Size size) {
        return size.height == null
                ? element("image",
                element(name("source"), source),
                element(name("destination"), destination),
                element(name("width"), String.valueOf(size.width)))
                : element("image",
                element(name("source"), source),
                element(name("destination"), destination),
                element(name("width"), String.valueOf(size.width)),
                element(name("cropWidth"), String.valueOf(size.width)),
                element(name("cropHeight"), String.valueOf(size.height)),
                element(name("color"), color));
    }
}
