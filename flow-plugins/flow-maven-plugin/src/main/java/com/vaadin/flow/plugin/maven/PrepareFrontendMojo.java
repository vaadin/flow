/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.plugin.maven;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.vaadin.flow.plugin.base.BuildFrontendUtil;

/**
 * This goal checks that node and npm tools are installed and creates or updates
 * `package.json` and the frontend build tool configuration files.
 * <p>
 * Copies frontend resources available inside `.jar` dependencies to
 * `node_modules` when building a jar package.
 *
 * @since 2.0
 */
@Mojo(name = "prepare-frontend", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class PrepareFrontendMojo extends FlowModeAbstractMojo {

    @Override
    protected void executeInternal()
            throws MojoExecutionException, MojoFailureException {
        if (productionMode != null) {
            logWarn("The <productionMode>" + productionMode
                    + "</productionMode> Maven parameter no longer has any effect and can be removed. Production mode is automatically enabled when you run the build-frontend target.");
        }

        // propagate info via System properties and token file
        File tokenFile = BuildFrontendUtil.propagateBuildInfo(this);

        // Inform m2eclipse that the directory containing the token file has
        // been updated in order to trigger server re-deployment (#6103)
        triggerRefresh(tokenFile.getParentFile());

        try {
            BuildFrontendUtil.prepareFrontend(this);
        } catch (Exception exception) {
            throw new MojoFailureException(
                    "Could not execute prepare-frontend goal.", exception);
        }

    }

}
