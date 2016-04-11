package com.github.spirylics.web2app;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.*;
import java.nio.file.Files;
import java.util.Collection;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

/**
 * Sign & Package in zip
 */
@Mojo(name = "package", defaultPhase = PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class Package extends Web2AppMojo {

    @Override
    public void e() throws Exception {
        if (BuildType.release.equals(getBuildType())) {
            signAndroid();
        }
        zip();
    }

    void signAndroid() throws MojoExecutionException, IOException {
        File apkDir = new File(getPlatformDir("android"), "/build/outputs/apk");
        if (apkDir.exists()) {
            Files.find(apkDir.toPath(), 1,
                    (path, basicFileAttributes) -> path.toFile().getName().matches(".*-release-.*apk$"))
                    .forEach(p -> {
                        File apkFile = p.toFile();

                        CommandLine sign = new CommandLine("jarsigner");
                        sign.addArguments("-verbose");
                        sign.addArguments("-sigalg");
                        sign.addArguments(signAlg);
                        sign.addArguments("-digestalg");
                        sign.addArguments(signDigestalg);
                        sign.addArguments("-keystore");
                        sign.addArguments(signKeystore.getAbsolutePath());
                        sign.addArguments("-keypass");
                        sign.addArguments(signKeypass);
                        sign.addArguments("-storepass");
                        sign.addArguments(signStorepass);
                        sign.addArguments(apkFile.getName());
                        sign.addArguments(signAlias);
                        exec("sign", apkDir, sign);

                        CommandLine verify = new CommandLine("jarsigner");
                        verify.addArguments("-verify");
                        verify.addArguments(apkFile.getName());
                        exec("verify", apkDir, verify);

                        CommandLine zipalign = new CommandLine("zipalign");
                        zipalign.addArguments("-v");
                        zipalign.addArguments("4");
                        zipalign.addArguments(apkFile.getName());
                        zipalign.addArguments(apkFile.getName().replace("-unsigned", ""));
                        exec("zipalign", apkDir, zipalign);
                    });
        }
    }

    void zip() throws IOException, ArchiveException {
        File destination = new File(buildDirectory, appName + ".zip");
        destination.delete();
        addFilesToZip(appDirectory, destination);
        projectHelper.attachArtifact(mavenProject, "zip", destination);
    }

    void addFilesToZip(File source, File destination) throws IOException, ArchiveException {
        OutputStream archiveStream = new FileOutputStream(destination);
        ArchiveOutputStream archive = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream);
        Collection<File> fileList = FileUtils.listFiles(source, null, true);
        for (File file : fileList) {
            String entryName = getEntryName(source, file);
            ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
            archive.putArchiveEntry(entry);
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
            IOUtils.copy(input, archive);
            input.close();
            archive.closeArchiveEntry();
        }
        archive.finish();
        archiveStream.close();
    }

    String getEntryName(File source, File file) throws IOException {
        int index = source.getAbsolutePath().length() + 1;
        String path = file.getCanonicalPath();
        return path.substring(index);
    }

}