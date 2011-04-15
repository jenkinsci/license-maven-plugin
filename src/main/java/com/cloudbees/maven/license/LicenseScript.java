package com.cloudbees.maven.license;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.Script;
import org.apache.maven.project.MavenProject;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class LicenseScript extends Script {
    public MavenProject project;
    Closure completer;
    Closure generator;

    public LicenseScript() {
    }

    public LicenseScript(Binding binding) {
        super(binding);
    }

    public void complete(Closure closure) {
        this.completer = closure;
    }

    public void generate(Closure closure) {
        this.generator = closure;
    }

    public void runCompleter(CompleterDelegate delegate) {
        if (completer!=null) {
            completer.setDelegate(delegate);
            completer.run();
        }
    }

    public void runGenerator(GeneratorDelegate delegate) {
        if (generator!=null) {
            generator.setDelegate(delegate);
            generator.run();
        }
    }
}
