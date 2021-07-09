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

public class BarrelGenerator {
    static final String BARREL_FILE_NAME = "endpoints";
    static final String BARREL_NAME = BARREL_FILE_NAME + TS;
    private static final Logger log = LoggerFactory
            .getLogger(VaadinConnectClientGenerator.class);

    private final Path outputPath;

    public BarrelGenerator(Path outputFolder) {
        outputPath = outputFolder.resolve(BARREL_NAME);
    }

    public void generate(OpenAPI openAPI) {
        List<Tag> tags = openAPI.getTags();

        String imports = tags.stream().map(tag -> String
                .format("import * as %1$s from \"./%1$s\";", tag.getName()))
                .collect(Collectors.joining("\n"));

        String exports = String.format("export {\n%s\n};",
                tags.stream().map(tag -> String.format("  %s", tag.getName()))
                        .collect(Collectors.joining(",\n")));

        String content = String.format("%s\n\n%s", imports, exports);

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
