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

import static com.vaadin.fusion.generator.Generator.TS;

class BarrelGenerator {
    public static final String BARREL_FILE_NAME = "endpoints";
    public static final String BARREL_NAME = BARREL_FILE_NAME + TS;
    private static final Logger log = LoggerFactory
            .getLogger(ConnectClientGenerator.class);

    private final Path outputFilePath;

    public BarrelGenerator(Path outputFolder) {
        outputFilePath = outputFolder.resolve(BARREL_NAME);
    }

    public void generate(OpenAPI openAPI) {
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

    public Path getOutputFilePath() {
        return outputFilePath;
    }
}
