package com.cloudbees.maven.license;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Process license information.
 *
 * @goal process
 * @requiresDependencyResolution runtime
 */
public class ProcessMojo extends AbstractMojo {
    /**
     * @component
     */
    public MavenProjectHelper projectHelper;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    public MavenProject project;

    /**
     * @component
     */
    public MavenProjectBuilder projectBuilder;

    /**
     * @component
     */
    public ArtifactFactory artifactFactory;

    /**
     * @parameter expression="${localRepository}"
     */
    public ArtifactRepository localRepository;

    /**
     * Specifies completion/generation/filtering scripts.
     *
     * This can be either a file or a directory. If it's a directory
     * all the files in it are assumed to be completer scripts.
     *
     * @parameter expression="${license.script}
     */
    public File script;

    /**
     * Specifies completion/generation/filtering script inline.
     *
     * @parameter
     */
    public String inlineScript;

    /**
     * If true, require all the dependencies to have license information after running
     * completion scripts, or fail the build.
     *
     * @parameter
     */
    public boolean requireCompleteLicenseInfo;

    /**
     * If true, generate "licenses.xml" that captures all the dependencies and its
     * licenses.
     *
     * @parameter expression="${license.generateLicenseXml}
     */
    public File generateLicenseXml;

    /**
     * If true, generate "licenses.html" as the visualization of {@code license.xml}
     *
     * @parameter expression="${license.generateLicenseHtml}
     */
    public File generateLicenseHtml;

    /**
     * Forbidden switch to disable and bypass all the checks.
     *
     * @parameter expression="${license.disableCheck}
     */
    public boolean disableCheck;

    /**
     * If true, attach the generated XML/HTML as artifacts (to be installed/deployed to Maven repositories.)
     *
     * @parameter expression="${license.attach}
     */
    public boolean attach;

    public void execute() throws MojoExecutionException {
        if (disableCheck)   return;

        GroovyShell shell = createShell(LicenseScript.class);

        List<LicenseScript> comp = parseScripts(script, shell);

        if (generateLicenseHtml!=null && generateLicenseXml==null) {// we need XML to be able to generate HTML
            try {
                generateLicenseXml = File.createTempFile("license","xml");
                generateLicenseXml.deleteOnExit();
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to generate a temporary file",e);
            }
        }

        if (generateLicenseXml!=null)
            comp.add((LicenseScript) shell.parse(getClass().getResourceAsStream("xmlgen.groovy"),"xmlgen.groovy"));

        if (generateLicenseHtml!=null)
            comp.add((LicenseScript) shell.parse(getClass().getResourceAsStream("htmlgen.groovy"),"htmlgen.groovy"));

        if (inlineScript!=null)
            comp.add((LicenseScript)shell.parse(inlineScript,"inlineScript"));

        for (LicenseScript s : comp) {
            s.project = project;
            s.mojo = this;
            s.run();    // setup
        }

        List<MavenProject> dependencies = new ArrayList<MavenProject>();

        // run against the project itself
        for (LicenseScript s : comp) {
            s.runCompleter(new CompleterDelegate(project, project));
        }
        dependencies.add(project);

        Map<Artifact,MavenProject> models = new HashMap<Artifact, MavenProject>();

        for (Artifact a : (Set<Artifact>)project.getArtifacts()) {
            Artifact pom = artifactFactory.createProjectArtifact(a.getGroupId(), a.getArtifactId(), a.getVersion());
            try {
                models.put(a, projectBuilder.buildFromRepository(pom, project.getRemoteArtifactRepositories(), localRepository));
            } catch (ProjectBuildingException x) {
                getLog().warn(x.getMessage());
            }
        }

        // filter them out
        for (LicenseScript s : comp) {
            s.runFilter(new FilterDelegate(models));
        }

        // filter out optional components
        for (Iterator<Entry<Artifact, MavenProject>> itr = models.entrySet().iterator(); itr.hasNext();) {
            Entry<Artifact, MavenProject> e =  itr.next();
            if (e.getKey().isOptional())
                itr.remove();
        }

        for (MavenProject e : models.values()) {
            // let the completion script intercept and process the licenses
            for (LicenseScript s : comp) {
                s.runCompleter(new CompleterDelegate(e, project));
            }
        }

        dependencies.addAll(models.values());

        if (requireCompleteLicenseInfo) {
            List<MavenProject> missing = new ArrayList<MavenProject>();
            for (MavenProject d : dependencies) {
                if (d.getLicenses().isEmpty())
                    missing.add(d);
            }
            if (!missing.isEmpty()) {
                StringBuilder buf = new StringBuilder("The following dependencies are missing license information:\n");
                for (MavenProject p : missing) {
                    buf.append("  "+p.getGroupId()+':'+p.getArtifactId()+':'+p.getVersion());
                    for (p=p.getParent(); p!=null; p=p.getParent())
                        buf.append(" -> "+p.getGroupId()+':'+p.getArtifactId()+':'+p.getVersion());
                    buf.append('\n');
                }
                buf.append("\nAdd/update your completion script to fill them, or run with -Dlicense.disableCheck to bypass the check.");
                throw new MojoExecutionException(buf.toString());
            }
        }

        for (LicenseScript s : comp) {
            s.runGenerator(new GeneratorDelegate(dependencies));
        }

        if (attach) {
            if (generateLicenseXml!=null)
                projectHelper.attachArtifact( project, "license.xml", null, generateLicenseXml );
            if (generateLicenseHtml!=null)
                projectHelper.attachArtifact( project, "license.html", null, generateLicenseHtml );
        }
    }

    private List<LicenseScript> parseScripts(File src, GroovyShell shell) throws MojoExecutionException {
        List<LicenseScript> comp = new ArrayList<LicenseScript>();
        if (src !=null) {
            try {
                if (src.isDirectory()) {
                    for (File script : src.listFiles())
                        comp.add((LicenseScript)shell.parse(script));
                } else {
                    comp.add((LicenseScript)shell.parse(src));
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to parse the script: "+ src,e);
            }
        }
        return comp;
    }

    private <T extends Script> GroovyShell createShell(Class<T> baseType) {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setScriptBaseClass(baseType.getName());
        return new GroovyShell(getClass().getClassLoader(),new Binding(),cc);
    }
}
