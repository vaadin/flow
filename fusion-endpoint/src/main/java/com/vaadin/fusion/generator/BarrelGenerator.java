package com.vaadin.fusion.generator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vaadin.fusion.generator.Generator.TS;

public class BarrelGenerator {
    static final String BARREL_FILE_NAME = "index";
    static final String BARREL_NAME = BARREL_FILE_NAME + TS;
    private static final Logger log = LoggerFactory
            .getLogger(VaadinConnectClientGenerator.class);

    Path outputPath;

    public BarrelGenerator(File outputFolder) {
        outputPath = outputFolder.toPath().resolve(BARREL_NAME);
    }

    public void generate(OpenAPI openAPI) {
        String content = openAPI.getTags().stream().map(tag -> String
                .format("export * as %1$s from \"./%1$s\"", tag.getName()))
                .collect(Collectors.joining("\n"));

        try {
            log.info("writing file {}", outputPath);
            FileUtils.writeStringToFile(outputPath.toFile(), content,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            String errorMessage = String.format("Error writing file at %s",
                    outputPath.toString());
            log.error(errorMessage, outputPath, e);
        }
    }
}
