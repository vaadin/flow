/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.testutil.TestUtils;
import com.vaadin.tests.util.MockOptions;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Add-on {@code .ts} sources referenced by {@code @JsModule} are copied into
 * {@code <frontend>/generated/jar-resources}, which the generated project
 * {@code tsconfig.json} deliberately excludes so that project type-checking
 * rules are not enforced on third party sources.
 * <p>
 * Since rolldown 1.1.2 (Vite 8.1) a tsconfig only governs the files it
 * <em>owns</em>: an imported-but-excluded file is owned by no project and is
 * therefore transformed with no {@code compilerOptions} at all. Add-on sources
 * lose {@code experimentalDecorators} (raw decorators reach the browser, which
 * fails with {@code SyntaxError}) and {@code useDefineForClassFields: false}
 * (class fields shadow Lit reactive property accessors).
 * <p>
 * The bundler maintainers consider the ownership model intentional and
 * recommend giving the excluded folder a tsconfig of its own, which is what
 * these tests require Flow to generate.
 *
 * @see <a href=
 *      "https://github.com/vaadin/flow/issues/24982">vaadin/flow#24982</a>
 * @see <a href=
 *      "https://github.com/rolldown/rolldown/issues/10281">rolldown/rolldown#10281</a>
 */
class JarResourcesTsConfigTest extends NodeUpdateTestUtil {

    @TempDir
    File temporaryFolder;

    private File npmFolder;
    private File frontendFolder;
    private File jarResourcesFolder;
    private Options options;

    @BeforeEach
    void setUp() throws IOException {
        npmFolder = Files.createTempDirectory(temporaryFolder.toPath(), "proj")
                .toFile();
        frontendFolder = new File(npmFolder, "src/main/frontend");
        jarResourcesFolder = new File(frontendFolder,
                "generated/jar-resources");
        Files.createDirectories(jarResourcesFolder.toPath());

        options = new MockOptions(getClassFinder(), npmFolder)
                .withFrontendDirectory(frontendFolder)
                .withJarFrontendResourcesFolder(jarResourcesFolder);
    }

    @Test
    void projectTsConfig_excludesJarResources() throws Exception {
        new TaskGenerateTsConfig(options).execute();

        assertTrue(
                readTsConfig(new File(npmFolder, "tsconfig.json"))
                        .get("exclude").toString().contains("jar-resources"),
                "The project tsconfig is expected to exclude jar-resources; "
                        + "if that is no longer the case, the jar-resources "
                        + "tsconfig is not needed either");
    }

    @Test
    void jarResourcesTsConfig_isGenerated() throws Exception {
        new TaskGenerateTsConfig(options).execute();

        assertTrue(new File(jarResourcesFolder, "tsconfig.json").exists(),
                "Add-on sources in the jar-resources folder are excluded from "
                        + "the project tsconfig and need a tsconfig of their "
                        + "own to be transformed by the bundler");
    }

    @Test
    void jarResourcesTsConfig_ownsTheFolderContents() throws Exception {
        new TaskGenerateTsConfig(options).execute();
        ObjectNode tsConfig = readTsConfig(
                new File(jarResourcesFolder, "tsconfig.json"));

        JsonNode include = tsConfig.get("include");
        assertNotNull(include,
                "The jar-resources tsconfig must declare its own 'include', "
                        + "otherwise the folder contents are owned by no "
                        + "project and are transformed without compilerOptions");
        assertFalse(include.isEmpty(), "'include' must not be empty");

        JsonNode exclude = tsConfig.get("exclude");
        assertNotNull(exclude,
                "The jar-resources tsconfig must declare an empty 'exclude', "
                        + "otherwise an inherited 'exclude' resolved against "
                        + "the project tsconfig excludes the folder again");
        assertTrue(exclude.isEmpty(), "'exclude' must be empty");
    }

    @Test
    void jarResourcesTsConfig_keepsDecoratorAndClassFieldSemantics()
            throws Exception {
        new TaskGenerateTsConfig(options).execute();
        JsonNode compilerOptions = resolveCompilerOptions(
                new File(jarResourcesFolder, "tsconfig.json"));

        assertEquals(true,
                compilerOptions.get("experimentalDecorators").asBoolean(),
                "Add-on sources must keep the legacy decorator transform, "
                        + "otherwise raw decorators reach the browser");
        assertEquals(false,
                compilerOptions.get("useDefineForClassFields").asBoolean(),
                "Add-on sources must keep set semantics for class fields, "
                        + "otherwise Lit reactive properties stop working");
    }

    @Test
    void jarResourcesTsConfig_survivesFrontendFileCopying() throws Exception {
        new TaskGenerateTsConfig(options).execute();
        File tsConfig = new File(jarResourcesFolder, "tsconfig.json");
        assertTrue(tsConfig.exists(), "tsconfig.json should be generated");

        // Copying add-on resources prunes files it does not itself handle
        options.copyResources(
                Set.of(TestUtils.getTestJar("jar-with-modern-frontend.jar")));
        new TaskCopyFrontendFiles(options).execute();

        assertTrue(tsConfig.exists(),
                "Copying add-on frontend resources must not delete the "
                        + "generated jar-resources tsconfig.json");
    }

    /**
     * Reads the compiler options of the given config, merging in the ones
     * inherited through {@code extends} so that the test accepts either a
     * self-contained config or one extending the project config.
     */
    private JsonNode resolveCompilerOptions(File tsConfigFile)
            throws IOException {
        ObjectNode tsConfig = readTsConfig(tsConfigFile);
        ObjectNode resolved = JacksonUtils.createObjectNode();
        if (tsConfig.has("extends")) {
            File parent = new File(tsConfigFile.getParentFile(),
                    tsConfig.get("extends").asString()).getCanonicalFile();
            assertTrue(parent.exists(),
                    "The extended config '" + parent
                            + "' must exist, a missing config makes the "
                            + "bundler fail with 'Tsconfig not found'");
            resolved.setAll((ObjectNode) resolveCompilerOptions(parent));
        }
        if (tsConfig.has("compilerOptions")) {
            resolved.setAll((ObjectNode) tsConfig.get("compilerOptions"));
        }
        return resolved;
    }

    private ObjectNode readTsConfig(File file) throws IOException {
        assertTrue(file.exists(), "Expected '" + file + "' to exist");
        // tsconfig templates use line comments, which JSON does not allow
        String json = Files.readString(file.toPath(), UTF_8)
                .replaceAll("(?m)^\\s*//.*", "");
        return JacksonUtils.readTree(json);
    }
}
