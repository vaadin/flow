package com.vaadin.fusion.generator;

import java.io.File;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import io.swagger.codegen.v3.config.CodegenConfigurator;
import io.swagger.v3.oas.models.OpenAPI;

public class Generator {
    public static final String MODEL = "Model";
    public static final String OPTIONAL_SUFFIX = " | undefined";
    public static final String TS = ".ts";
    public static final String MODEL_TS = MODEL + TS;
    private final VaadinConnectClientGenerator clientGenerator;
    private final GeneratorDirectory outputDirectory;
    private final OpenAPIParser parser;
    private final BarrelGenerator barrelGenerator;

    public Generator(File openApiJsonFile, File generatedFrontendDirectory) {
        this(openApiJsonFile, generatedFrontendDirectory, null, null);
    }

    public Generator(File openApiJsonFile, File generatedFrontendDirectory,
            Properties properties) {
        this(openApiJsonFile, generatedFrontendDirectory, properties, null);
    }

    public Generator(File openApiJsonFile, File generatedFrontendDirectory,
            String defaultClientPath) {
        this(openApiJsonFile, generatedFrontendDirectory, null,
                defaultClientPath);
    }

    public Generator(File openApiJsonFile, File generatedFrontendDirectory,
            Properties properties, String defaultClientPath) {
        Objects.requireNonNull(openApiJsonFile);
        Objects.requireNonNull(generatedFrontendDirectory);

        outputDirectory = new GeneratorDirectory(generatedFrontendDirectory);
        parser = openApiJsonFile.exists()
                ? new OpenAPIParser(openApiJsonFile, outputDirectory,
                        TypescriptCodeGenerator.class, defaultClientPath)
                : null;
        clientGenerator = properties != null
                ? new VaadinConnectClientGenerator(outputDirectory.toPath(),
                        properties)
                : null;
        barrelGenerator = new BarrelGenerator(outputDirectory.toPath());
    }

    public void start() {
        if (parser == null) {
            outputDirectory.clean();
            return;
        }

        try {
            OpenAPI openAPI = parser.parseOpenAPI();
            boolean hasGeneratedSuccessfully = generateTypescriptCode(openAPI);

            if (clientGenerator != null && hasGeneratedSuccessfully) {
                clientGenerator.generate();
                barrelGenerator.generate(openAPI);
            }
        } catch (IllegalStateException e) {
            outputDirectory.clean();
            throw e;
        }
    }

    private boolean generateTypescriptCode(OpenAPI openAPI) {
        CodegenConfigurator configurator = parser.getConfigurator();

        Set<File> files = TypescriptCodeGenerator.generateFiles(
                configurator.toClientOptInput().openAPI(openAPI));

        outputDirectory.clean(files);

        return files.stream()
                .anyMatch(file -> file.getName().endsWith(Generator.TS));
    }
}
