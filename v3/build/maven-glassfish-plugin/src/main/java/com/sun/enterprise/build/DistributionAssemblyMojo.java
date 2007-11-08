package com.sun.enterprise.build;

import com.sun.enterprise.module.ManifestConstants;
import com.sun.enterprise.module.impl.Jar;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.ZipFileSet;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;

/**
 * Creates a glassfish distribution image.
 *
 * @goal assemble
 * @phase package
 * @requiresProject
 * @requiresDependencyResolution runtime
 *
 * @author Kohsuke Kawaguchi
 */
public class DistributionAssemblyMojo extends AbstractMojo {
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The directory where the final image will be created.
     *
     * @parameter expression="${outputDirectory}" default-value="${project.build.directory}"
     */
    protected File outputDirectory;

    /**
     * The file name of the created distribution image.
     *
     * @parameter expression="${finalName}" default-value="${project.build.finalName}.zip"
     */
    protected String finalName;

    public void execute() throws MojoExecutionException, MojoFailureException {

        Set artifacts = project.getArtifacts();

        Set<Artifact> images = findArtifactsOfType(artifacts,"zip");
        if(images.size()>1)
            throw new MojoExecutionException("More than one base image zip dependency is specified: "+images);
        if(images.isEmpty())
            throw new MojoExecutionException("No base image zip dependency is given");

        Artifact baseImage = images.iterator().next();

        // find all maven modules
        Set<Artifact> modules = findArtifactsOfScope(artifacts, "runtime");

        outputDirectory.mkdirs();

        // create a zip file
        Zip zip = new Zip();
        zip.setProject(new Project());
        File target = new File(outputDirectory, finalName);
        zip.setDestFile(target);

        // add the base image jar as <zipgroupfileset>
        ZipFileSet zfs = new ZipFileSet();
        zfs.setSrc(baseImage.getFile());
        zfs.setPrefix("glassfish");
        zfs.setDirMode("755");
        zfs.setFileMode("644"); // work around for http://issues.apache.org/bugzilla/show_bug.cgi?id=42122
        zip.addZipfileset(zfs);

        // then put all modules
        for (Artifact a : modules) {
            zfs = new ZipFileSet();
            zfs.setFile(a.getFile());
            zfs.setPrefix(isModule(a) ? "glassfish/lib" : "glassfish/lib/jars");
            zip.addZipfileset(zfs);
        }

        getLog().info("Creating the distribution");
        long time = System.currentTimeMillis();
        zip.execute();
        getLog().info("Packaging took "+(System.currentTimeMillis()-time)+"ms");

        project.getArtifact().setFile(target);
        // normally I shouldn't have to do this. Maven is supposed to pick up
        // the glassfish-distribution artifact handler definition from components.xml
        // and use that.
        // but because of what seems like an ordering issue, I can't get this work.
        // ArtifactHandlerManager just don't get glassfish-distribution ArtifactHandler.
        // so to make this work, I'm overwriting artifact handler here as a work around.
        // This may be somewhat unsafe, as other processing could have already
        // happened with the old incorrect artifact handler, but at least this
        // seems to make the deploy/install phase work.
        project.getArtifact().setArtifactHandler(new DistributionArtifactHandler());
    }

    private boolean isModule(Artifact a) throws MojoExecutionException {
        try {
            Jar jar = Jar.create(a.getFile());
            if (jar.getManifest()==null) {
                return false;
            }
            Attributes attributes = jar.getManifest().getMainAttributes();
            String name = attributes.getValue(ManifestConstants.BUNDLE_NAME);
            return name!=null;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to open "+a.getFile(),e);
        }
    }

    /**
     * Returns a set of {@link Artifact}s that have the given type.
     */
    private Set<Artifact> findArtifactsOfType(Set<Artifact> artifacts, String type) {
        Set<Artifact> r = new HashSet<Artifact>();

        for(Artifact a : artifacts) {
            String t = a.getType();
            if(t==null)  t="jar"; // see http://maven.apache.org/pom.html
            if(!t.equals(type))
                continue;

            if (getLog().isDebugEnabled()) {
                getLog().debug("Including " + a.getGroupId() + ":" + a.getArtifactId() + ":"+ a.getVersion());
                getLog().debug("From dependency trail : ");
                for (int i=a.getDependencyTrail().size()-1;i>=0;i--) {
                    getLog().debug(" " + a.getDependencyTrail().get(i).toString());
                }
                getLog().debug("");
            }
            r.add(a);
        }

        return r;
    }

    private Set<Artifact> findArtifactsOfScope(Set<Artifact> artifacts, String scope) {
        Set<Artifact> r = new HashSet<Artifact>();

        for(Artifact a : artifacts) {
            String s = a.getScope();
            if(!s.equals(scope))
                continue;


            if (getLog().isDebugEnabled()) {
                getLog().debug("Including " + a.getGroupId() + ":" + a.getArtifactId() + ":"+ a.getVersion());
                getLog().debug("From dependency trail : ");
                for (int i=a.getDependencyTrail().size()-1;i>=0;i--) {
                    getLog().debug(" " + a.getDependencyTrail().get(i).toString());
                }
                getLog().debug("");
            }
            
            r.add(a);
        }

        return r;
    }
}
