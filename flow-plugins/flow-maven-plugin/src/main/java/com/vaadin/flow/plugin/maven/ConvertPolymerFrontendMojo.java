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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.vaadin.polymer2lit.FrontendConverter;

@Mojo(name = "convert-polymer-frontend")
public class ConvertPolymerFrontendMojo extends FlowModeAbstractMojo {

    @Parameter(defaultValue = "**/*.js")
    private String glob;

    @Override
    public void execute() {
        try (FrontendConverter converter = new FrontendConverter()) {

            for (Path filePath : getFilePathsByGlob(glob)) {
                try {
                    logInfo("Processing " + filePath.toString());

                    converter.convertFile(filePath);
                } catch (IOException | InterruptedException e) {
                    logError("Processing has failed", e);
                }
            }

        } catch (IOException e) {
            logError("Could not create an instance of FrontendConvertor", e);
        }
    }

    private List<Path> getFilePathsByGlob(String glob) throws IOException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);

        try (Stream<Path> walk = Files.walk(project.getBasedir().toPath())) {
            return walk
                .filter(path -> !path.toString().contains("node_modules"))
                .filter(path -> matcher.matches(path))
                .collect(Collectors.toList());
        }
    }
}
