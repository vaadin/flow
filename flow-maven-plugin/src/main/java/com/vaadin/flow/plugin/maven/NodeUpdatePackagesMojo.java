package com.vaadin.flow.plugin.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.vaadin.flow.server.frontend.AnnotationValuesExtractor;
import com.vaadin.flow.server.frontend.NodeUpdatePackages;
import com.vaadin.flow.server.frontend.NodeUpdater;

/**
 * Goal that updates <code>package.json</code> file with @NpmPackage annotations
 * defined in the classpath, and that creates <code>webpack.config.js</code> if
 * does not exist yet.
 */
@Mojo(name = "update-npm-dependencies", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class NodeUpdatePackagesMojo extends NodeUpdateAbstractMojo {

    /**
     * Copy the `webapp.config.js` from the specified URL if missing. Default is
     * the template provided by this plugin. Leave it blank to disable the
     * feature.
     */
    @Parameter(defaultValue = NodeUpdatePackages.WEBPACK_CONFIG)
    private String webpackTemplate;

    @Override
    protected NodeUpdater getUpdater() {
        if (updater == null) {
            AnnotationValuesExtractor extractor = new AnnotationValuesExtractor(getProjectClassPathUrls(project));
            updater = new NodeUpdatePackages(extractor, webpackTemplate, npmFolder.getPath(), flowPackagePath,
                    convertHtml);
        }
        return updater;
    }
}
