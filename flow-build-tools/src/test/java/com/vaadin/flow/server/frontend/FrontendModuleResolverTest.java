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
import java.net.URL;
import java.nio.file.Files;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FrontendModuleResolverTest {

    @TempDir
    File frontendFolder;

    private ClassFinder classFinder;

    @BeforeEach
    void setUp() throws IOException {
        classFinder = mock(ClassFinder.class);
        URL somewhere = FrontendModuleResolverTest.class
                .getResource("FrontendModuleResolverTest.class");
        when(classFinder.getResource(
                Constants.RESOURCES_FRONTEND_DEFAULT + "/addon.js"))
                .thenReturn(somewhere);
        when(classFinder
                .getResource(Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT
                        + "/legacy/index.mjs"))
                .thenReturn(somewhere);
        Files.writeString(new File(frontendFolder, "probe.ts").toPath(), "");
        Files.writeString(new File(frontendFolder, "styles.css").toPath(), "");
        new File(frontendFolder, "folder").mkdirs();
        Files.writeString(new File(frontendFolder, "folder/index.js").toPath(),
                "");
    }

    @ParameterizedTest
    @ValueSource(strings = { "./probe", "./probe.ts", "./styles.css?inline",
            "./folder", "./folder/", "./addon", "./addon.js", "./legacy",
            "declared", "declared/sub/path.js", "@scope/declared",
            "@scope/declared/sub", "Frontend/missing.js",
            "@vaadin/flow-frontend/missing.js", "../outside.js", "styles.css" })
    void foundOrNotToBeTold_resolves(String module) {
        assertNull(createResolver(Set.of("declared", "@scope/declared"))
                .findUnresolvableReason(module), module);
    }

    @ParameterizedTest
    @ValueSource(strings = { "./missing.js", "./probe.js", "./folder/other",
            "./styles.css.map?inline" })
    void fileNotFound_saysWhereItWasLookedFor(String module) {
        String reason = createResolver(Set.of()).findUnresolvableReason(module);

        assertNotNull(reason, module);
        assertTrue(reason.contains(frontendFolder.getPath()), reason);
        assertTrue(reason.contains(Constants.RESOURCES_FRONTEND_DEFAULT),
                reason);
    }

    @ParameterizedTest
    @ValueSource(strings = { "undeclared", "undeclared/sub",
            "@scope/undeclared/sub", "@scope/declared-not" })
    void packageNotDeclared_namesThePackage(String module) {
        String reason = createResolver(Set.of("declared", "@scope/declared"))
                .findUnresolvableReason(module);

        assertNotNull(reason, module);
        String packageName = module.startsWith("@")
                ? module.split("/")[0] + "/" + module.split("/")[1]
                : module.split("/")[0];
        assertTrue(reason.contains("'" + packageName + "'"), reason);
    }

    @Test
    void packagesNotKnown_anyPackageResolves() {
        assertNull(createResolver(null).findUnresolvableReason("undeclared"));
    }

    private FrontendModuleResolver createResolver(Set<String> packages) {
        Options options = new Options(mock(Lookup.class), classFinder, null)
                .withFrontendDirectory(frontendFolder);
        return new FrontendModuleResolver(options,
                LoggerFactory.getLogger(FrontendModuleResolverTest.class),
                () -> packages);
    }
}
