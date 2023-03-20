/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vaadin.fusion.generator.MainGenerator.TS;

/**
 * Performs the generation of a barrel file for importing all endpoints at once
 * at the frontend side.
 *
 * Barrel file re-exports all endpoints methods combining them under an simple
 * object with the endpoint name. It may be considered as a class with static
 * methods.
 */
class BarrelGenerator {
    public static final String BARREL_FILE_NAME = "endpoints";
    public static final String BARREL_NAME = BARREL_FILE_NAME + TS;
    private static final Logger log = LoggerFactory
            .getLogger(ClientAPIGenerator.class);

    private final Path outputFilePath;

    BarrelGenerator(Path outputFolder) {
        outputFilePath = outputFolder.resolve(BARREL_NAME);
    }

    void generate(OpenAPI openAPI) {
        List<Tag> tagList = openAPI.getTags();

        if (tagList == null || tagList.isEmpty()) {
            return;
        }

        String imports = tagList.stream().map(tag -> String
                .format("import * as %1$s from \"./%1$s\";", tag.getName()))
                .collect(Collectors.joining("\n"));

        String exports = String.format("export {\n%s\n};",
                tagList.stream()
                        .map(tag -> String.format("  %s", tag.getName()))
                        .collect(Collectors.joining(",\n")));

        String content = String.format("%s\n\n%s", imports, exports);

        try {
            log.info("writing file {}", outputFilePath);
            FileUtils.writeStringToFile(outputFilePath.toFile(), content,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            String errorMessage = String.format("Error writing file at %s",
                    outputFilePath.toString());
            log.error(errorMessage, outputFilePath, e);
        }
    }

    Path getOutputFilePath() {
        return outputFilePath;
    }
}
