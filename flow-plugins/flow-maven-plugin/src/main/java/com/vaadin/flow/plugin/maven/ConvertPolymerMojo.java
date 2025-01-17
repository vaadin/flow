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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.vaadin.flow.plugin.base.ConvertPolymerCommand;

/**
 * A Maven goal that converts Polymer-based source files to Lit.
 */
@Mojo(name = "convert-polymer")
public class ConvertPolymerMojo extends FlowModeAbstractMojo {

    /**
     * A path to a specific file or directory that needs to be converted. By
     * default, the goal scans and tries to convert all {@code *.js} and
     * {@code *.java} files in the project except for the {@code node_modules}
     * folder.
     */
    @Parameter(property = "vaadin.path")
    private String path;

    /**
     * Whether to enforce Lit 1 compatible imports.
     */
    @Parameter(property = "vaadin.useLit1", defaultValue = "${false}")
    private boolean useLit1;

    /**
     * Whether to disable the usage of the JavaScript optional chaining operator
     * (?.) in the output.
     */
    @Parameter(property = "vaadin.disableOptionalChaining", defaultValue = "${false}")
    private boolean disableOptionalChaining;

    @Override
    protected void executeInternal() throws MojoFailureException {
        if (isHillaUsed(frontendDirectory())) {
            getLog().warn(
                    """
                            The 'convert-polymer' goal is not meant to be used in Hilla projects as polymer templates are not supported.
                            """
                            .stripIndent());
        }
        try (ConvertPolymerCommand command = new ConvertPolymerCommand(this,
                path, useLit1, disableOptionalChaining)) {
            command.execute();
        } catch (Exception e) {
            throw new MojoFailureException(
                    "Could not execute convert-polymer goal.", e);
        }
    }
}
