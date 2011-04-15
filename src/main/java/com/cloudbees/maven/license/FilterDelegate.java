package com.cloudbees.maven.license;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class FilterDelegate {
    public final Map<Artifact,MavenProject> models;

    public FilterDelegate(Map<Artifact, MavenProject> models) {
        this.models = models;
    }
}
