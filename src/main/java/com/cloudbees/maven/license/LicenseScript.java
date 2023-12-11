package com.cloudbees.maven.license;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.Script;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class LicenseScript extends Script {
    public MavenProject project;
    public ProcessMojo mojo;

    private Closure completer;
    private Closure generator;
    private Closure filter;

    public LicenseScript() {}

    public LicenseScript(Binding binding) {
        super(binding);
    }

    public void complete(Closure closure) {
        this.completer = closure;
    }

    public void generate(Closure closure) {
        this.generator = closure;
    }

    public void filter(Closure closure) {
        this.filter = closure;
    }

    void runCompleter(CompleterDelegate delegate) {
        run(delegate, completer);
    }

    void runGenerator(GeneratorDelegate delegate) {
        run(delegate, generator);
    }

    void runFilter(FilterDelegate delegate) {
        run(delegate, filter);
    }

    private void run(Object delegate, Closure closure) {
        if (closure != null) {
            closure.setDelegate(delegate);
            closure.run();
        }
    }

    //
    // convenience methods exposed to script
    //

    public Log getLog() {
        return mojo.getLog();
    }
}
