package com.vaadin.flow.plugin.maven;

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.vaadin.flow.server.frontend.AnnotationValuesExtractor;
import com.vaadin.flow.server.frontend.NodeUpdateImports;
import com.vaadin.flow.server.frontend.NodeUpdater;

/**
 * Goal that updates main.js file with @JsModule, @HtmlImport and @Theme
 * annotations defined in the classpath.
 */
@Mojo(name = "update-imports", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class NodeUpdateImportsMojo extends NodeUpdateAbstractMojo {
    /**
     * Name of the JavaScript file to update.
     */
    @Parameter(defaultValue = "${project.basedir}/" + NodeUpdateImports.MAIN_JS)
    private String jsFile;

    @Override
    public void execute() {
        getUpdater().execute();
    }

    protected NodeUpdater getUpdater() {
        if (updater == null) {
            AnnotationValuesExtractor extractor = new AnnotationValuesExtractor(getProjectClassPathUrls(project));
            updater = new NodeUpdateImports(extractor, jsFile, npmFolder.getPath(), flowPackagePath, convertHtml);
        }
        return updater;
    }

    public File getFlowPackage() {
        return getUpdater().getFlowPackage();
    }
}
