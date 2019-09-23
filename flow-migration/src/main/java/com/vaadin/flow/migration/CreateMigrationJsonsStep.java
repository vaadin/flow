/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.migration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Step which creates {@code bower.json} and {@code package.json} files.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class CreateMigrationJsonsStep {

    private File targetDir;

    /**
     * Creates a new instance.
     *
     * @param target
     *            the target folder.
     */
    public CreateMigrationJsonsStep(File target) {
        targetDir = target;
    }

    /**
     * Creates {@code bower.json} and {@code package.json} files filled with
     * paths provided by the {@code paths} parameter.
     *
     * @param paths
     *            path of files to migrate
     * @throws IOException
     */
    public void createJsons(List<String> paths) throws IOException {
        String bowerTemplate = readResource("/migration/bower.json");
        String filesList = paths.stream().collect(Collectors.joining("\", \""));
        filesList = "\"" + filesList + "\"";

        bowerTemplate = bowerTemplate.replace("%files_list%", filesList);
        String packageTemplate = readResource("/migration/package.json");
        packageTemplate = packageTemplate.replace("%files_list%", filesList);

        Files.write(new File(targetDir, "bower.json").toPath(),
                Collections.singleton(bowerTemplate));

        Files.write(new File(targetDir, "package.json").toPath(),
                Collections.singleton(packageTemplate));
    }

    private String readResource(String resourcePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(CreateMigrationJsonsStep.class
                        .getResourceAsStream(resourcePath),
                        StandardCharsets.UTF_8))) {
            return reader.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
