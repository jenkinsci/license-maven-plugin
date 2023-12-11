package com.cloudbees.maven.license;

import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * Base class for filter scripts that define convenience methods.
 *
 * <p>
 * Filter phase gets all the resolved dependencies and can add/remove some dependencies
 * from the final list.
 *
 * @author Kohsuke Kawaguchi
 */
public class FilterDelegate {
    public final Map<Artifact, MavenProject> models;

    public FilterDelegate(Map<Artifact, MavenProject> models) {
        this.models = models;
    }
}
