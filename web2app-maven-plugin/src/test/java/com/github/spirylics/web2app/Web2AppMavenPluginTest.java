package com.github.spirylics.web2app;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

public class Web2AppMavenPluginTest {

    @SuppressWarnings("deprecation")
    ArtifactRepository repository = new DefaultArtifactRepository("central", "http://repo.maven.apache.org/maven2",
            new DefaultRepositoryLayout());

    @Rule
    public MojoRule rule = new MojoRule();

    private <M extends Mojo> M getMojo(String goal) throws Exception {
        File testBaseDir = new File(getClass().getResource("pom.xml").getFile()).getParentFile();
        Web2AppMojo mojo = (Web2AppMojo) rule.lookupConfiguredMojo(testBaseDir, goal);
        File localRepositoryFile = new File(testBaseDir, "repo");
        mojo.mavenProject.setRemoteArtifactRepositories(Arrays.asList(repository));
        DefaultRepositorySystemSession repositorySystemSession = (DefaultRepositorySystemSession) mojo.mavenSession.getRepositorySession();
        repositorySystemSession.setLocalRepositoryManager(
                new SimpleLocalRepositoryManagerFactory().newInstance(
                        repositorySystemSession, new LocalRepository(localRepositoryFile)));
        mojo.mavenSession.getRequest().setLocalRepository(new StubArtifactRepository(localRepositoryFile.getAbsolutePath()));
        return (M) mojo;
    }

    @Test
    public void testBuild() throws Exception {
        getMojo("install-cordova").execute();
    }

}
