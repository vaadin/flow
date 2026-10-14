/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.plugin.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import com.vaadin.flow.plugin.base.CleanFrontendUtil;
import com.vaadin.flow.plugin.base.CleanFrontendUtil.CleanFrontendException;
import com.vaadin.flow.plugin.base.CleanOptions;

/**
 * Goal that cleans the frontend files to a clean state.
 * <p>
 * Deletes Vaadin dependencies from package.json, the generated frontend folder
 * and the npm/pnpm-related files and folders:
 * <ul>
 * <li>node_modules
 * <li>pnpm-lock.yaml
 * <li>package-lock.json
 * </ul>
 *
 * @since 9.0
 */
@Mojo(name = "clean-frontend", defaultPhase = LifecyclePhase.PRE_CLEAN)
public class CleanFrontendMojo extends FlowModeAbstractMojo {

    @Override
    protected void executeInternal() throws MojoFailureException {
        try {
            CleanFrontendUtil.runCleaning(this, new CleanOptions());
        } catch (CleanFrontendException e) {
            throw new MojoFailureException(e.getMessage(), e.getCause());
        }
    }

}
