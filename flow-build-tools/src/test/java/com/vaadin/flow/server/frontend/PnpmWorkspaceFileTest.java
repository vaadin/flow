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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PnpmWorkspaceFileTest {

    @TempDir
    File projectRoot;

    private File workspaceFile() {
        return new File(projectRoot, PnpmWorkspaceFile.WORKSPACE_FILE);
    }

    @Test
    void noFile_getOverridesEmpty_saveCreatesNothing() throws Exception {
        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        assertTrue(workspace.getOverrides().isEmpty());
        assertFalse(workspace.save(),
                "Nothing to write when there are no overrides");
        assertFalse(workspaceFile().exists());
    }

    @Test
    void setOverrides_writesBlock_andReloads() throws Exception {
        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        Map<String, String> overrides = new LinkedHashMap<>();
        overrides.put("workbox-build>glob", "10.4.5");
        workspace.setOverrides(overrides);
        assertTrue(workspace.save());
        assertTrue(workspaceFile().exists());

        PnpmWorkspaceFile reloaded = new PnpmWorkspaceFile(projectRoot);
        assertEquals("10.4.5",
                reloaded.getOverrides().get("workbox-build>glob"));
    }

    @Test
    void preservesUnrelatedContent() throws Exception {
        Files.writeString(workspaceFile().toPath(), """
                packages:
                  - packages/*
                shamefully-hoist: true
                """, StandardCharsets.UTF_8);

        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        workspace.setOverrides(Map.of("dep", "1.2.3"));
        workspace.save();

        String content = Files.readString(workspaceFile().toPath(),
                StandardCharsets.UTF_8);
        assertTrue(content.contains("packages:"),
                "User packages section must be preserved");
        assertTrue(content.contains("shamefully-hoist"),
                "User settings must be preserved");
        assertTrue(content.contains("dep"), "Override must be written");

        // Clearing overrides (e.g. once the last override is removed) keeps the
        // file and its unrelated user content.
        PnpmWorkspaceFile cleared = new PnpmWorkspaceFile(projectRoot);
        cleared.setOverrides(Map.of());
        cleared.save();

        assertTrue(workspaceFile().exists(),
                "File with other user content must not be deleted");
        content = Files.readString(workspaceFile().toPath(),
                StandardCharsets.UTF_8);
        assertTrue(content.contains("packages:"),
                "User packages section must survive clearing overrides");
        assertTrue(content.contains("shamefully-hoist"),
                "User settings must survive clearing overrides");
        assertTrue(new PnpmWorkspaceFile(projectRoot).getOverrides().isEmpty(),
                "Overrides must be removed");
    }

    @Test
    void emptyDocument_deletesFile() throws Exception {
        Files.writeString(workspaceFile().toPath(), """
                overrides:
                  dep: 1.0.0
                """, StandardCharsets.UTF_8);

        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        workspace.setOverrides(Map.of());
        assertTrue(workspace.save(), "Emptied file should be removed");
        assertFalse(workspaceFile().exists());
    }

    @Test
    void getOverrides_coercesUnquotedNumericValue() throws Exception {
        Files.writeString(workspaceFile().toPath(), """
                overrides:
                  dep: 1.0
                """, StandardCharsets.UTF_8);

        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        assertEquals("1.0", workspace.getOverrides().get("dep"));
    }

    @Test
    void save_isIdempotent_whenUnchanged() throws Exception {
        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        workspace.setOverrides(Map.of("dep", "1.0.0"));
        assertTrue(workspace.save());

        PnpmWorkspaceFile again = new PnpmWorkspaceFile(projectRoot);
        again.setOverrides(Map.of("dep", "1.0.0"));
        assertFalse(again.save(),
                "Rewriting identical content should report no change");
    }
}
