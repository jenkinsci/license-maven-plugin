package com.cloudbees.maven.license;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class GeneratorDelegate {
    protected ProcessMojo mojo;

    protected MavenProject project;
    protected List<MavenProject> dependencies;

    protected GeneratorDelegate(ProcessMojo mojo, MavenProject project, List<MavenProject> dependencies) {
        this.mojo = mojo;
        this.project = project;
        this.dependencies = dependencies;
    }

    public Log getLog() {
        return mojo.getLog();
    }
}
