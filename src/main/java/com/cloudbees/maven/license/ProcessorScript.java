package com.cloudbees.maven.license;

import groovy.lang.Binding;
import groovy.lang.Script;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class ProcessorScript extends Script {
    protected Log log;

    protected MavenProject project;
    protected List<MavenProject> dependencies;

    public ProcessorScript() {
    }

    public ProcessorScript(Binding binding) {
        super(binding);
    }
}
