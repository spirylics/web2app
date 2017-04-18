package com.github.spirylics.web2app;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.math.Fraction;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
        REGULAR("mipmap", ""), PORTRAIT("drawable", "-port"), LANDSCAPE("drawable", "-land");

        final String androidFirstPrefix;
        final String androidSecondPrefix;

        Type(String androidFirstPrefix, String androidSecondPrefix) {
            this.androidFirstPrefix = androidFirstPrefix;
            this.androidSecondPrefix = androidSecondPrefix;
        }
    }

    final String platformPath;
    final String wwwPath;
    final String androidResourcePath;
    final String iosResourcePath;
    final String color;
    final List<String> platforms;
    final List<MojoExecutor.Element> images = Lists.newArrayList();
    final Map<String, Function<String, ImagesGenBuilder>> platformToIcon = new ImmutableMap.Builder<String, Function<String, ImagesGenBuilder>>()
            .put("android", source -> addAndroidIcon(source))
            .put("ios", source -> addIosIcon(source))
            .build();
    final Map<String, Function<String, ImagesGenBuilder>> platformToSplashscreen = new ImmutableMap.Builder<String, Function<String, ImagesGenBuilder>>()
            .put("android", source -> addAndroidSplashscreen(source))
            .put("ios", source -> addIosSplashscreen(source))
            .build();
    final Map<String, Fraction> androidDensityRatioMap = new ImmutableMap.Builder<String, Fraction>()
            .put("ldpi", Fraction.getFraction(3, 16))
            .put("mdpi", Fraction.getFraction(4, 16))
            .put("hdpi", Fraction.getFraction(6, 16))
            .put("xhdpi", Fraction.getFraction(8, 16))
            .put("xxhdpi", Fraction.getFraction(12, 16))
            .put("xxxhdpi", Fraction.getFraction(16, 16))
            .build();
    final Map<String, Size> androidDensitySplashPortraitMap = new ImmutableMap.Builder<String, Size>()
            .put("ldpi", new Size(200, 300))
            .put("mdpi", new Size(320, 480))
            .put("hdpi", new Size(480, 800))
            .put("xhdpi", new Size(720, 1280))
            .put("xxhdpi", new Size(960, 1600))
            .put("xxxhdpi", new Size(1280, 1920))
            .build();
    final Map<String, Size> androidDensitySplashLandscapeMap = androidDensitySplashPortraitMap.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().flip()));
    final Map<String, Size> iosIconSizeMap = new ImmutableMap.Builder<String, Size>()
            .put("", new Size(57))
            .put("@2x", new Size(114))
            .put("-40", new Size(40))
            .put("-40@2x", new Size(80))
            .put("-50", new Size(50))
            .put("-50@2x", new Size(100))
            .put("-60@2x", new Size(120))
            .put("-60@3x", new Size(180))
            .put("-72", new Size(72))
            .put("-72@2x", new Size(144))
            .put("-76", new Size(76))
            .put("-76@2x", new Size(152))
            .put("-83.5@2x.png", new Size(167))
            .put("-small", new Size(29))
            .put("-small@2x", new Size(58))
            .put("-small@3x", new Size(87))
            .build();
    final Map<String, Size> iosSplashSizeMap = new ImmutableMap.Builder<String, Size>()
            .put("@2x~iphone", new Size(640, 960))
            .put("-568h@2x~iphone", new Size(640, 1136))
            .put("-667h", new Size(750, 1334))
            .put("-736h", new Size(1242, 2208))
            .put("-Landscape@2x~ipad", new Size(2048, 1536))
            .put("-Landscape-736h", new Size(2208, 1242))
            .put("-Landscape~ipad", new Size(1024, 768))
            .put("-Portrait@2x~ipad", new Size(1538, 2046))
            .put("-Portrait~ipad", new Size(768, 1024))
            .put("~iphone", new Size(320, 480))
            .build();

    public ImagesGenBuilder(String platformPath, String wwwPath, String color, List<String> platforms) throws IOException {
        this.platformPath = platformPath;
        this.wwwPath = wwwPath;
        this.androidResourcePath = platformPath + "/android/res";
        File iosPlatform = new File(platformPath, "ios");
        this.iosResourcePath = iosPlatform.exists() ? Files.find(iosPlatform.toPath(), 2,
                (path, basicFileAttributes) ->
                        path.toFile().isDirectory() && "Images.xcassets".equalsIgnoreCase(path.toFile().getName()))
                .findFirst().get().toFile().getAbsolutePath() : null;
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

    public ImagesGenBuilder addIosIcon(String source) {
        return addIosImage(source, iosIconSizeMap, "AppIcon.appiconset/icon%s.png");
    }

    public ImagesGenBuilder addIosSplashscreen(String source) {
        return addIosImage(source, iosSplashSizeMap, "LaunchImage.launchimage/Default%s.png");
    }

    public ImagesGenBuilder addIosImage(String source, Map<String, Size> imageSizeMap, String format) {
        return addImage(source, imageSizeMap.entrySet().stream().collect(Collectors.toMap(
                e -> String.format(iosResourcePath + "/" + format, e.getKey()),
                e -> e.getValue()
        )));
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
                e -> String.format("%s/%s%s-%s/%s", resourcePath, type.androidFirstPrefix, type.androidSecondPrefix, e.getKey(), destinationFileName),
                e -> largestSize.scale(e.getValue())
        ));
    }

    Map<String, Size> getAndroidDestinationSizeMap(String resourcePath, String destinationFileName, Type type, Map<String, Size> densitySizeMap) {
        return densitySizeMap.entrySet().stream().collect(Collectors.toMap(
                e -> String.format("%s/%s%s-%s/%s", resourcePath, type.androidFirstPrefix, type.androidSecondPrefix, e.getKey(), destinationFileName),
                e -> e.getValue()
        ));
    }

    public MojoExecutor.Element build() {
        return element("images", Iterables.toArray(images, MojoExecutor.Element.class));
    }
}
