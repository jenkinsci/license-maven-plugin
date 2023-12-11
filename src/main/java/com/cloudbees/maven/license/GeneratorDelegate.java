package com.cloudbees.maven.license;

import java.util.List;
import org.apache.maven.project.MavenProject;

/**
 * @author Kohsuke Kawaguchi
 */
public class GeneratorDelegate {
    protected List<MavenProject> dependencies;

    protected GeneratorDelegate(List<MavenProject> dependencies) {
        this.dependencies = dependencies;
    }
}
