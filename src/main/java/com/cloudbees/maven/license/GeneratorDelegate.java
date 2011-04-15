package com.cloudbees.maven.license;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class GeneratorDelegate {
    protected List<MavenProject> dependencies;

    protected GeneratorDelegate(List<MavenProject> dependencies) {
        this.dependencies = dependencies;
    }
}
