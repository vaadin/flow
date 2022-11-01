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
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.vaadin.polymer2lit.ServerConverter;

@Mojo(name = "convert-polymer-server")
public class ConvertPolymerServerMojo extends FlowModeAbstractMojo {

    @Parameter(defaultValue = "**/*.java")
    private String glob;

    @Override
    public void execute() throws MojoFailureException {
        try {
            for (Path filePath : getFilePathsByGlob(glob)) {
                logInfo("Processing " + filePath.toString() + "...");

                ServerConverter.convertFile(filePath);
            }
        } catch (Exception e) {
            throw new MojoFailureException(
                    "Could not execute convert-polymer-server goal.", e);
        }
    }

    private List<Path> getFilePathsByGlob(String glob) throws IOException {
        PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + glob);

        try (Stream<Path> walk = Files.walk(project.getBasedir().toPath())) {
            return walk.filter(path -> matcher.matches(path)).collect(Collectors.toList());
        }
    }
}
