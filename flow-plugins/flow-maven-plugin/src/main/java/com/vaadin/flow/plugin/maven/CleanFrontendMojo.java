/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
