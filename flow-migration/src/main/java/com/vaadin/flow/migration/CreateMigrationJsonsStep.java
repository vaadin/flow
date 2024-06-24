/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
