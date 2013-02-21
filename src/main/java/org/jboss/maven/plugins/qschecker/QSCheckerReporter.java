/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.maven.plugins.qschecker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.PlexusContainer;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;


/**
 * @author rafaelbenevides
 * 
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, 
    requiresDependencyResolution = ResolutionScope.COMPILE, 
    requiresProject = true, 
    aggregator = true, 
    threadSafe = true)
public class QSCheckerReporter extends AbstractMavenReport {

    @Component
    private PlexusContainer container;

    @Component
    private Renderer siteRenderer;

    @Component
    private MavenProject mavenProject;

    @Component
    private BuildPluginManager pluginManager;

    @Parameter(property = "session", required = true, readonly = true)
    private MavenSession mavenSession;

    @Parameter(property = "reactorProjects", readonly = true)
    private List<MavenProject> reactorProjects;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    @Override
    public String getDescription(Locale locale) {
        return "Quickstarts violations";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    @Override
    public String getName(Locale locale) {
        return "Quickstarts Checker Report";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    @Override
    public String getOutputName() {
        return "qschecker";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    @Override
    protected Renderer getSiteRenderer() {
        return siteRenderer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    @Override
    protected String getOutputDirectory() {
        return mavenProject.getModel().getReporting().getOutputDirectory();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    @Override
    protected MavenProject getProject() {
        return mavenProject;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        try {
            executeJXRAndSitePlugins();
            
            List<QSChecker>  checkers = container.lookupList(QSChecker.class);
            startReport(checkers, locale);
    
            Map<String, List<Violation>> globalFilesViolations = new TreeMap<String, List<Violation>>();
            for (QSChecker checker : checkers) {
                Map<String, List<Violation>> checkerViolations = checker.check(mavenProject, reactorProjects, getLog());
                addCheckerViolationsToGlobalFilesViolations(globalFilesViolations, checkerViolations);
            }
            doFileSummary(globalFilesViolations);
            doFileReports(globalFilesViolations);
        } catch (Exception e) {
            throw new MavenReportException(e.getMessage(), e);
        }
        endReport();

    }

    /**
     * @throws MojoExecutionException
     * 
     */
    private void executeJXRAndSitePlugins() throws MojoExecutionException {
        //Execute JXR Plugin
        executeMojo(
                plugin(
                        groupId( "org.apache.maven.plugins"),
                        artifactId("maven-jxr-plugin"),
                        version("2.3")
                ), 
                goal("aggregate"), 
                configuration(
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager));
   
        //Execute JXR Plugin
        executeMojo(
                plugin(
                        groupId( "org.apache.maven.plugins"),
                        artifactId("maven-jxr-plugin"),
                        version("2.3")
                ), 
                goal("test-aggregate"), 
                configuration(
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager));
        
        //Execute Site Plugin
        executeMojo(
                plugin(
                        groupId( "org.apache.maven.plugins"),
                        artifactId("maven-site-plugin"),
                        version("3.2")
                ), 
                goal("site"), 
                configuration(
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager));
    }

    /**
     * End the HTML report
     * 
     */
    private void endReport() {
        Sink sink = getSink();
        sink.body_();
        sink.flush();
        sink.close();
    }

    /**
     * Prints a File and each violations it have.
     * 
     * @param fileViolations
     */
    private void doFileReports(Map<String, List<Violation>> fileViolations) {
        Sink sink = getSink();

        sink.section1(); //Start Section 1
        sink.sectionTitle1();
        sink.text("Files Violations");
        sink.sectionTitle1_();
        
        // File Sections
        for (String file : fileViolations.keySet()) {
            sink.anchor( file.replace( '/', '.' ) );
            sink.anchor_();

            sink.section2(); //Section 2 start
            sink.sectionTitle2();
            sink.text(file);
            sink.sectionTitle2_();
            
            sink.table();
            // Headers
            sink.tableRow();
            sink.tableHeaderCell();
            sink.text("Checker");
            sink.tableHeaderCell_();
            
            sink.tableHeaderCell();
            sink.text("Message");
            sink.tableHeaderCell_();
            
            sink.tableHeaderCell();
            sink.text("Line num.");
            sink.tableHeaderCell_();
            sink.tableRow_();

            // Each file violation
            List<Violation> violations = fileViolations.get(file);
            for (Violation violation : violations) {
                
                sink.tableRow();

                sink.tableCell();
                sink.text(violation.getSourceChecker().getSimpleName());
                sink.tableCell_();
                
                sink.tableCell();
                sink.text(violation.getViolationMessage());
                sink.tableCell_();
                
                sink.tableCell();
                
                //Only Java files has XREF
                File xrefSource = new File(mavenProject.getModel().getReporting().getOutputDirectory() + "/xref/" + file.replaceAll( "\\.java$", ".html" ));
                if (xrefSource.exists()){
                    String linelink = xrefSource.getAbsolutePath() + "#" + violation.getLineNumber();
                    sink.link(linelink);
                }
                File xrefTestSource = new File(mavenProject.getModel().getReporting().getOutputDirectory() + "/xref-test/" + file.replaceAll( "\\.java$", ".html" ));
                if (xrefTestSource.exists()){
                    String linelink = xrefTestSource.getAbsolutePath() + "#" + violation.getLineNumber();
                    sink.link(linelink);
                }
                sink.text(String.valueOf(violation.getLineNumber()));
                sink.link_();
                sink.tableCell_();
                
                sink.tableRow_();
            }
            sink.table_();
            sink.section2_(); //End Section 2
        }
        sink.section1_(); // End Section 1
    }

    /**
     * Prints file summary
     * 
     * @param filesViolations
     */
    private void doFileSummary(Map<String, List<Violation>> filesViolations) {
        Sink sink = getSink();
        sink.section1(); //Start Section 1
        sink.sectionTitle1();
        sink.text("Files");
        sink.sectionTitle2_();

        sink.table();
        // Headers
        sink.tableRow();
        sink.tableHeaderCell();
        sink.text("File");
        sink.tableHeaderCell_();
        
        sink.tableHeaderCell();
        sink.text("Violations qtd.");
        sink.tableHeaderCell_();
        sink.tableRow();
        
        for (String file: filesViolations.keySet()){
            sink.tableRow();
            sink.tableCell();
            sink.link("#" + file.replace( '/', '.' ) );
            sink.text(file);
            sink.link_();
            sink.tableCell_();
 
            sink.tableCell();
            sink.text(String.valueOf(filesViolations.get(file).size()));
            sink.tableCell_();
            sink.tableRow();
            
        }
        sink.table_();
        sink.section1_(); //End Section 1
        
    }

    /**
     * Add all violations found by a Checker to tha global File Violation
     * 
     * @param filesViolations
     * @param checkerViolations
     */
    private void addCheckerViolationsToGlobalFilesViolations(Map<String, List<Violation>> filesViolations, Map<String, List<Violation>> checkerViolations) {
        for (String file : checkerViolations.keySet()) {
            List<Violation> ckviolations = checkerViolations.get(file);
            if (filesViolations.get(file) == null) {
                getLog().debug("New violations for file: " + file);
                filesViolations.put(file, new ArrayList<Violation>());
            }
            filesViolations.get(file).addAll(ckviolations);
        }
    }

    /**
     * Start the Reporter HTML
     * @param checkers 
     * 
     * @param locale
     * @param sink
     * 
     */
    private void startReport(List<QSChecker> checkers, Locale locale) {
        Sink sink = getSink();
        sink.head();
        sink.title();
        sink.text(getName(locale));
        sink.title_();
        sink.head_();
        sink.body();
        
        sink.section1(); //Section 1 Start
        sink.sectionTitle1();
        sink.text("Quickstart Check Results");
        sink.sectionTitle1_();
        
        sink.text("The following Checkers were used: ");
        for(QSChecker checker: checkers){
            sink.text(checker.getClass().getSimpleName() + ",") ;
        }
        
        sink.section1_(); //Section 1 End
    }

}
