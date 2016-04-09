package com.github.spirylics.web2app;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.*;
import java.util.Collection;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

/**
 * Package in zip
 */
@Mojo(name = "package", defaultPhase = PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class Package extends Web2AppMojo {

    @Override
    public void e() throws Exception {
        File destination = new File(buildDirectory, appName + ".zip");
        destination.delete();
        addFilesToZip(getPlatformsDir(), destination);
        projectHelper.attachArtifact(mavenProject, "zip", destination);
    }

    private void addFilesToZip(File source, File destination) throws IOException, ArchiveException {
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

    private String getEntryName(File source, File file) throws IOException {
        int index = source.getAbsolutePath().length() + 1;
        String path = file.getCanonicalPath();
        return path.substring(index);
    }

}