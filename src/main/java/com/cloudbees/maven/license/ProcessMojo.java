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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
     * Specifies a completion script to fill in / correct entries.
     *
     * This can be either a file or a directory. If it's a directory
     * all the files in it are assumed to be completer scripts.
     *
     * @parameter
     */
    public File script;

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
     * @parameter expression="${license.generateLicenseXml}
     */
    public File generateLicenseHtml;

    public void execute() throws MojoExecutionException {
        List<LicenseScript> comp = parseScripts(script);

        if (generateLicenseHtml!=null && generateLicenseXml==null) {// we need XML to be able to generate HTML
            try {
                generateLicenseXml = File.createTempFile("license","xml");
                generateLicenseXml.deleteOnExit();
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to generate a temporary file",e);
            }
        }

        if (generateLicenseXml!=null)
            comp.add((LicenseScript)createShell(LicenseScript.class).parse(getClass().getResourceAsStream("xmlgen.groovy")));

        if (generateLicenseHtml!=null)
            comp.add((LicenseScript)createShell(LicenseScript.class).parse(getClass().getResourceAsStream("htmlgen.groovy")));

        for (LicenseScript s : comp) {
            s.run();    // setup
        }


        List<MavenProject> dependencies = new ArrayList<MavenProject>();

        // run against the project itself
        for (LicenseScript s : comp) {
            s.runCompleter(new CompleterDelegate(project, project));
        }
        dependencies.add(project);

        try {
            Map<Artifact,MavenProject> models = new HashMap<Artifact, MavenProject>();
            Set<String> plugins = new HashSet<String>();

            // TODO: push out the filtering logic to a groovy script and remove Jenkins dependency
            for (Artifact a : project.getArtifacts()) {

                Artifact pom = artifactFactory.createProjectArtifact(a.getGroupId(), a.getArtifactId(), a.getVersion());
                MavenProject model = projectBuilder.buildFromRepository(pom, project.getRemoteArtifactRepositories(), localRepository);

                if (model.getPackaging().equals("hpi"))
                    plugins.add(a.getId());

                if (a.isOptional())     continue;   // optional components don't ship

                models.put(a,model);
            }

            OUTER:
            for (Artifact a : models.keySet()) {
                MavenProject model = models.get(a);

                if(a.getDependencyTrail().size() >= 1 && plugins.contains(a.getDependencyTrail().get(1)))
                    continue;   // ignore transitive dependencies through other plugins

                // if the dependency goes through jenkins core, we don't need to bundle it in the war
                // because jenkins-core comes in the <provided> scope, I think this is a bug in Maven that it puts such
                // dependencies into the artifact list.
                for (String trail : a.getDependencyTrail()) {
                    if (trail.contains(":hudson-core:") || trail.contains(":jenkins-core:"))
                        continue OUTER;
                }

                    // let the completion script intercept and process the licenses
                for (LicenseScript s : comp) {
                    s.runCompleter(new CompleterDelegate(model, project));
                }

                dependencies.add(model);
            }
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException("Failed to parse into dependencies",e);
        }

        if (requireCompleteLicenseInfo) {
            List<MavenProject> missing = new ArrayList<MavenProject>();
            for (MavenProject d : dependencies) {
                if (d.getLicenses().isEmpty())
                    missing.add(d);
            }
            if (!missing.isEmpty()) {
                StringBuilder buf = new StringBuilder("The following dependencies are missing license information:\n");
                for (MavenProject p : missing)
                    buf.append("  "+p.getGroupId()+':'+p.getArtifactId()+':'+p.getVersion()+'\n');
                throw new MojoExecutionException(buf.toString());
            }
        }

        for (LicenseScript s : comp) {
            s.runGenerator(new GeneratorDelegate(this, project, dependencies));
        }
    }

    private List<LicenseScript> parseScripts(File src) throws MojoExecutionException {
        List<LicenseScript> comp = new ArrayList<LicenseScript>();
        if (src !=null) {
            try {
                GroovyShell shell = createShell(LicenseScript.class);
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
