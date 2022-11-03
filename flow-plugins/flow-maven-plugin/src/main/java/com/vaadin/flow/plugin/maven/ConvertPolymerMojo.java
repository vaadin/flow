/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import com.vaadin.flow.plugin.base.ConvertPolymerUtil;

@Mojo(name = "convert-polymer")
public class ConvertPolymerMojo extends FlowModeAbstractMojo {

    @Parameter(property = "serverGlob", defaultValue = "**/*.java")
    private String serverGlob;

    @Parameter(property = "frontendGlob", defaultValue = "**/*.js")
    private String frontendGlob;

    @Parameter(property = "useLit1", defaultValue = "${false}")
    private boolean useLit1;

    @Parameter(property = "disableOptionalChaining", defaultValue = "${false}")
    private boolean disableOptionalChaining;

    @Override
    public void execute() throws MojoFailureException {
        try {
            ConvertPolymerUtil.convertFrontend(this, frontendGlob, useLit1,
                    disableOptionalChaining);
            ConvertPolymerUtil.convertServer(this, serverGlob);
        } catch (Exception e) {
            throw new MojoFailureException(
                    "Could not execute convert-polymer-to-lit goal.", e);
        }
    }
}
