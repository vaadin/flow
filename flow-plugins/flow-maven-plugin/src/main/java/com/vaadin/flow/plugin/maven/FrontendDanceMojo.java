/*
 * Copyright 2000-2024 Vaadin Ltd.
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

import com.vaadin.flow.server.frontend.FrontendUtils;

/**
 * This is the hidden `vaadin:dance` to clean up the frontend files.
 *
 * @since
 */
@Mojo(name = "dance", defaultPhase = LifecyclePhase.PRE_CLEAN)
public class FrontendDanceMojo extends CleanFrontendMojo {

    @Override
    public void execute() throws MojoFailureException {
        if (FrontendUtils.isHillaUsed(frontendDirectory())) {
            getLog().warn(
                    """
                            The 'dance' goal is not meant to be used in Hilla projects as it delete 'package-lock.json' and also clearing out the content of 'package.json'.
                            """
                            .stripIndent());
        }
        runCleaning(new Options());
    }
}
