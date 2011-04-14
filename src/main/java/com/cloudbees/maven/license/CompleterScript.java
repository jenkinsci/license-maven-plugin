package com.cloudbees.maven.license;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.Script;
import org.apache.maven.model.License;
import org.apache.maven.project.MavenProject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.*;

/**
 * Base class for completer scripts that define convenience methods.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class CompleterScript extends Script {
    public MavenProject dependency, project;

    public CompleterScript() {
    }

    public CompleterScript(Binding binding) {
        super(binding);
    }

    /**
     * If the current dependency matches the given groupId/artifactId, execute the body
     *
     * @param criteria
     *      A string like "groupId:artifactId" that matches the current dependency
     *      ("*" is allowed for wildcard), or a collection/array of them.
     */
    public void match(Object criteria, Closure body) {
        if (criteria instanceof Object[])
            criteria = Arrays.asList((Object[])criteria);
        if (criteria instanceof String)
            criteria = Collections.singleton(criteria);

        for (String c : (Collection<String>) criteria) {
            String[] tokens = c.split(":");
            if (tokens.length<2)
                throw new IllegalArgumentException("Invalid matcher '"+c+"'. Expecting GROUPID:ARTIFACTID");
            
            if (matchToken(dependency.getGroupId(), tokens[0])
             && matchToken(dependency.getArtifactId(), tokens[1])
            && (tokens.length<=2 || matchToken(dependency.getVersion(), tokens[2]))) {
                body.call();
            }
        }
    }

    private boolean matchToken(String actual, String expected) {
        return expected.equals("*") || actual.equals(expected);
    }

    /**
     * Verifies that the license of the current dependency is the ones listed in the 'expected' argument,
     * rewrite it with the specified one. This is for completing missing license info, and for
     * selecting one from multi-licensed dependencies.
     */
    public void rewriteLicense(Collection<License> expected, License to) {
        List<License> actual = dependency.getLicenses();
        IllegalStateException error = new IllegalStateException("Expecting " + toString(expected) + " but found " + toString(actual) + " for dependency " + toString(dependency));

        if (expected.size()!= actual.size())
            throw error;

        OUTER:
        for (License e : expected) {
            for (License a : actual) {
                if (e.getName().equals(a.getName()))
                    continue OUTER;
            }
            throw error;
        }

        dependency.setLicenses(singletonList(to));
    }

    /**
     * Creates a new license object.
     */
    public License license(String name, String url) {
        License l = new License();
        l.setName(name);
        l.setUrl(url);
        return l;
    }

    private String toString(Collection<License> lics) {
        StringBuilder buf = new StringBuilder();
        for (License lic : lics) {
            if (buf.length()>0) buf.append(',');
            buf.append(lic.getName());
        }
        return "["+buf.toString()+"]";
    }

    private String toString(MavenProject p) {
        return p.getGroupId()+":"+p.getArtifactId()+":"+p.getVersion();
    }

}
