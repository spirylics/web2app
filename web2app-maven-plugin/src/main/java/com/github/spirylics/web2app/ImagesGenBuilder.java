package com.github.spirylics.web2app;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.math.Fraction;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

        public Size flip() {
            return new Size(height, width);
        }

    }

    enum Type {
        REGULAR(""), PORTRAIT("-port"), LANDSCAPE("-land");

        final String androidPrefixDirectory;

        Type(String androidPrefixDirectory) {
            this.androidPrefixDirectory = androidPrefixDirectory;
        }
    }

    final String platformPath;
    final String wwwPath;
    final String androidResourcePath;
    final String color;
    final List<String> platforms;
    final List<MojoExecutor.Element> images = Lists.newArrayList();
    final Map<String, Function<String, ImagesGenBuilder>> platformToIcon = new ImmutableMap.Builder<String, Function<String, ImagesGenBuilder>>()
            .put("android", source -> addAndroidIcon(source))
            .build();
    final Map<String, Function<String, ImagesGenBuilder>> platformToSplashscreen = new ImmutableMap.Builder<String, Function<String, ImagesGenBuilder>>()
            .put("android", source -> addAndroidSplashscreen(source))
            .build();
    final Map<String, Fraction> androidDensityRatioMap = new ImmutableMap.Builder<String, Fraction>()
            .put("ldpi", Fraction.getFraction(3, 16))
            .put("mdpi", Fraction.getFraction(4, 16))
            .put("hdpi", Fraction.getFraction(6, 16))
            .put("xhdpi", Fraction.getFraction(8, 16))
            .put("xxhdpi", Fraction.getFraction(12, 16))
            .put("xxxdpi", Fraction.getFraction(16, 16))
            .build();
    final Map<String, Size> androidDensitySplashPortraitMap = new ImmutableMap.Builder<String, Size>()
            .put("ldpi", new Size(200, 300))
            .put("mdpi", new Size(320, 480))
            .put("hdpi", new Size(480, 800))
            .put("xhdpi", new Size(720, 1280))
            .put("xxhdpi", new Size(960, 1600))
            .put("xxxdpi", new Size(1280, 1920))
            .build();
    final Map<String, Size> androidDensitySplashLandscapeMap = androidDensitySplashPortraitMap.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().flip()));

    public ImagesGenBuilder(String platformPath, String wwwPath, String color, List<String> platforms) {
        this.platformPath = platformPath;
        this.wwwPath = wwwPath;
        this.androidResourcePath = platformPath + "/android";
        this.color = color;
        this.platforms = platforms;
    }

    public ImagesGenBuilder addIcon(final String source) {
        return apply(platformToIcon, source);
    }

    public ImagesGenBuilder addSplashscreen(final String source) {
        return apply(platformToSplashscreen, source);
    }

    ImagesGenBuilder apply(Map<String, Function<String, ImagesGenBuilder>> platformToFn, String source) {
        platformToFn.entrySet().stream().filter(e -> platforms.contains(e.getKey()))
                .forEach(e -> e.getValue().apply(source));
        return this;
    }

    public ImagesGenBuilder addAndroidIcon(String source) {
        return this.addAndroidImage(source, "icon.png", Type.REGULAR, new Size(192));
    }

    public ImagesGenBuilder addAndroidSplashscreen(String source) {
        return this
                .addImage(source, getAndroidDestinationSizeMap(androidResourcePath, "splashscreen.png", Type.PORTRAIT, androidDensitySplashPortraitMap))
                .addImage(source, getAndroidDestinationSizeMap(androidResourcePath, "splashscreen.png", Type.LANDSCAPE, androidDensitySplashLandscapeMap));
    }

    public ImagesGenBuilder addAndroidImage(String source, String destinationFileName, Type type, Size largestSize) {
        return this.addImage(source, getAndroidDestinationSizeMap(androidResourcePath, destinationFileName, type, largestSize));
    }

    public ImagesGenBuilder addImage(String source, Map<String, Size> destinationSizeMap) {
        this.images.addAll(toElements(source, destinationSizeMap));
        return this;
    }

    public List<MojoExecutor.Element> toElements(String source, Map<String, Size> destinationSizeMap) {
        if (Strings.isNullOrEmpty(source)) {
            return Arrays.asList();
        } else {
            return destinationSizeMap.entrySet()
                    .stream()
                    .map(e -> toElement(getAbsolutePath(source), e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
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

    String getAbsolutePath(String source) {
        return source.startsWith("/") ? source : wwwPath + "/" + source;
    }

    Map<String, Size> getAndroidDestinationSizeMap(String resourcePath, String destinationFileName, Type type, Size largestSize) {
        return androidDensityRatioMap.entrySet().stream().collect(Collectors.toMap(
                e -> String.format("%s/drawable%s-%s/%s", resourcePath, type.androidPrefixDirectory, e.getKey(), destinationFileName),
                e -> largestSize.scale(e.getValue())
        ));
    }

    Map<String, Size> getAndroidDestinationSizeMap(String resourcePath, String destinationFileName, Type type, Map<String, Size> densitySizeMap) {
        return densitySizeMap.entrySet().stream().collect(Collectors.toMap(
                e -> String.format("%s/drawable%s-%s/%s", resourcePath, type.androidPrefixDirectory, e.getKey(), destinationFileName),
                e -> e.getValue()
        ));
    }

    public MojoExecutor.Element build() {
        return element("images", Iterables.toArray(images, MojoExecutor.Element.class));
    }
}
