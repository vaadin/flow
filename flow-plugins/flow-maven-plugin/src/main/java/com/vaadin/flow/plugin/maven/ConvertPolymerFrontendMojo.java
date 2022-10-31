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

import java.io.IOException;

import org.apache.maven.plugins.annotations.Mojo;

import com.vaadin.polymer2lit.FrontendConverter;

@Mojo(name = "convert-polymer-frontend")
public class ConvertPolymerFrontendMojo extends FlowModeAbstractMojo {

    @Override
    public void execute() {
        try (FrontendConverter converter = new FrontendConverter()) {

            try {
                converter.convertFile("test");
            } catch (IOException | InterruptedException e) {
                logError("Could not convert the provided file.", e);
            }

        } catch (IOException e) {
            logError("Could not resolve convertor.js executable.", e);
            return;
        }


    }
}
