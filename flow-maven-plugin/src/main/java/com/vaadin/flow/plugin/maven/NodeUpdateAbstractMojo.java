package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.plugin.common.FlowPluginFileUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.NodeUpdater;

/**
 * Common stuff for node update mojos.
 */
public abstract class NodeUpdateAbstractMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Enable or disable legacy components annotated only with
     * {@link HtmlImport}.
     */
    @Parameter(defaultValue = "true")
    protected boolean convertHtml;

    /**
     * The folder where `package.json` file is located. Default is current dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    protected File npmFolder;

    /**
     * The relative path to the Flow package. Always relative to
     * {@link NodeUpdater#npmFolder}.
     */
    @Parameter(defaultValue = "/node_modules/" + NodeUpdater.FLOW_PACKAGE)
    protected String flowPackagePath;

    protected NodeUpdater updater;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Do nothing when bower mode
        if (Boolean.getBoolean("vaadin." + Constants.SERVLET_PARAMETER_BOWER_MODE)) {
            String goal = this.getClass().equals(NodeUpdateImportsMojo.class) ? "update-imports"  : "update-npm-dependencies";
            getLog().info("Skipped '" + goal + "' goal because `vaadin.bowerMode` is set.");
            return;
        }
        getUpdater().execute();
    }

    protected abstract NodeUpdater getUpdater();

    static URL[] getProjectClassPathUrls(MavenProject project) {
        final List<String> runtimeClasspathElements;
        try {
            runtimeClasspathElements = project.getRuntimeClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalStateException(String.format(
                    "Failed to retrieve runtime classpath elements from project '%s'",
                    project), e);
        }
        return runtimeClasspathElements.stream().map(File::new)
                .map(FlowPluginFileUtils::convertToUrl).toArray(URL[]::new);
    }
}
