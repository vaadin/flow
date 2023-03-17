/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.stalefiles;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGenerationTest;

public class StaleFilesTest extends AbstractEndpointGenerationTest {
    static private final String PACKAGE_PATH = StaleFilesTest.class.getPackage()
            .getName().replace(".", "/");

    public StaleFilesTest() {
        super(Arrays.asList(NewEndpoint.class));
    }

    @Test
    public void should_RemoveStaleGeneratedFiles() throws IOException {
        for (final File priorExistingFile : filesInOutputDirectory(
                // Theme generated files
                "theme.js", "theme.d.ts", "theme-test-case.generated.js",
                // Old endpoints
                "OldUserEndpoint.ts",
                // Old endpoints’ TypeScript interfaces and models
                "org/example/domain/OldUser.ts",
                "org/example/domain/OldUserModel.ts",
                "org/example/common/OldEntity.ts",
                "org/example/common/OldEntityModel.ts")) {
            priorExistingFile.getParentFile().mkdirs();
            priorExistingFile.createNewFile();
        }

        generateOpenApi(null);
        generateTsEndpoints();

        for (final File remainingFile : filesInOutputDirectory(
                // Theme files
                "theme.js", "theme.d.ts", "theme-test-case.generated.js",
                // New endpoint
                "NewEndpoint.ts",
                // New endpoint’s data definition
                PACKAGE_PATH + "/NewEndpoint/Account.ts",
                // New endpoint’s form model
                PACKAGE_PATH + "/NewEndpoint/AccountModel.ts")) {
            Assert.assertTrue(
                    String.format("Expected file '%s' to exist", remainingFile),
                    remainingFile.exists());
        }

        for (final File deletedFile : filesInOutputDirectory(
                // Old endpoints
                "OldEndpoint.ts",
                // Old endpoints’ TypeScript interfaces and models
                "org/example/domain/OldUser.ts",
                "org/example/domain/OldUserModel.ts",
                "org/example/common/OldEntity.ts",
                "org/example/common/OldEntityModel.ts",
                // Emptied directories
                "org/example/domain", "org/example/common", "org/example",
                "org")) {
            Assert.assertFalse(String.format("Expected file '%s' to not exist",
                    deletedFile), deletedFile.exists());
        }

    }

    private List<File> filesInOutputDirectory(String... paths) {
        return Arrays.stream(paths).map(FilenameUtils::separatorsToSystem)
                .map(pathname -> new File(outputDirectory.getRoot(), pathname))
                .collect(Collectors.toList());
    }
}
