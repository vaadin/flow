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

    private String content() throws Exception {
        return Files.readString(workspaceFile().toPath(),
                StandardCharsets.UTF_8);
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
    void getOverrides_readsUnquotedNumericValueAsText() throws Exception {
        // Read as the number 1.0 the version would come back as "1.1" for
        // 1.10, changing the package.json hash and forcing a package install.
        Files.writeString(workspaceFile().toPath(), """
                overrides:
                  dep: 1.10
                """, StandardCharsets.UTF_8);

        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        assertEquals("1.10", workspace.getOverrides().get("dep"));
    }

    @Test
    void windowsLineEndings_arePreserved() throws Exception {
        Files.writeString(workspaceFile().toPath(),
                "packages:\r\n  - \"packages/*\"\r\noverrides:\r\n  dep: 1.0.0\r\n",
                StandardCharsets.UTF_8);

        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        workspace.setOverrides(Map.of("dep", "2.0.0"));
        assertTrue(workspace.save());

        assertEquals(
                "packages:\r\n  - \"packages/*\"\r\noverrides:\r\n  dep: \"2.0.0\"\r\n",
                content(),
                "Rewriting every line as changed is not an edit to one override");
    }

    @Test
    void addingOverride_preservesCommentsAndLayout() throws Exception {
        // Everything here is the user's: comments, four-space indentation,
        // quoting styles and entry order all have to come back out untouched.
        String original = """
                # Delay install of newly released packages
                minimumReleaseAge: 180 # 3h
                minimumReleaseAgeExclude:
                    - "@xdevsoftware/*"
                overrides:
                    # Remove unused packages
                    "yargs": "npm:empty-npm-package@1.0.0"
                    'open': "npm:empty-npm-package@1.0.0"
                packages:
                    - "packages/*"
                """;
        Files.writeString(workspaceFile().toPath(), original,
                StandardCharsets.UTF_8);

        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        Map<String, String> overrides = workspace.getOverrides();
        overrides.put("@vaadin/button", "25.2.6");
        workspace.setOverrides(overrides);
        assertTrue(workspace.save());

        assertEquals("""
                # Delay install of newly released packages
                minimumReleaseAge: 180 # 3h
                minimumReleaseAgeExclude:
                    - "@xdevsoftware/*"
                overrides:
                    # Remove unused packages
                    "yargs": "npm:empty-npm-package@1.0.0"
                    'open': "npm:empty-npm-package@1.0.0"
                    "@vaadin/button": "25.2.6"
                packages:
                    - "packages/*"
                """, content(),
                "Only the added override may differ from the original file");
    }

    @Test
    void changedAndRemovedOverrides_touchOnlyAffectedEntries()
            throws Exception {
        Files.writeString(workspaceFile().toPath(), """
                overrides:
                  # keeps the versions Vaadin does not manage
                  'yargs': "npm:empty-npm-package@1.0.0"
                  '@vaadin/grid': "25.2.5" # pinned, see CVE-123
                  '@vaadin/dropped': "25.2.5"
                  nested:
                    'glob': "10.4.5"
                """, StandardCharsets.UTF_8);

        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        Map<String, String> overrides = new LinkedHashMap<>();
        overrides.put("yargs", "npm:empty-npm-package@1.0.0");
        overrides.put("@vaadin/grid", "25.2.6");
        workspace.setOverrides(overrides);
        assertTrue(workspace.save());

        assertEquals("""
                overrides:
                  # keeps the versions Vaadin does not manage
                  'yargs': "npm:empty-npm-package@1.0.0"
                  '@vaadin/grid': "25.2.6" # pinned, see CVE-123
                  nested:
                    'glob': "10.4.5"
                """, content(),
                "Unchanged entries keep their quoting, a changed entry keeps its "
                        + "key and its note, removed entries disappear and an "
                        + "entry Flow does not manage is left alone");
    }

    @Test
    void emptyOverrides_keepEntriesFlowDoesNotManage() throws Exception {
        Files.writeString(workspaceFile().toPath(), """
                overrides:
                  nested:
                    'glob': "10.4.5"
                  '@vaadin/grid': "25.2.5"
                """, StandardCharsets.UTF_8);

        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        workspace.setOverrides(Map.of());
        assertTrue(workspace.save());

        assertEquals("""
                overrides:
                  nested:
                    'glob': "10.4.5"
                """, content(),
                "Clearing the overrides Flow manages must not take the user's "
                        + "own entries with it");
    }

    @Test
    void unusualIndentation_isWrittenWithoutFailing() throws Exception {
        // The emitter refuses an indent above ten columns, so the indentation
        // read from the file cannot be passed on unchecked.
        Files.writeString(workspaceFile().toPath(), """
                minimumReleaseAgeExclude: ["@vaadin/*",
                                           "@types/*"]
                """, StandardCharsets.UTF_8);

        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        workspace.setOverrides(Map.of("dep", "1.0.0"));

        assertTrue(workspace.save(),
                "Deeply indented YAML must still be written");
        assertTrue(content().contains("\"dep\": \"1.0.0\""),
                "Override must be written");
    }

    @Test
    void severalDocuments_leaveFileUntouched() throws Exception {
        // A stream of documents cannot be represented as one editable document,
        // and overwriting it would throw away everything but the first.
        String original = """
                overrides:
                  dep: 1.0.0
                ---
                packages:
                  - "packages/*"
                """;
        Files.writeString(workspaceFile().toPath(), original,
                StandardCharsets.UTF_8);

        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        assertFalse(workspace.canPersist(),
                "Callers must be able to see that overrides cannot be stored, "
                        + "as otherwise they redo the work on every build");
        workspace.setOverrides(Map.of("@vaadin/button", "25.2.6"));

        assertFalse(workspace.save());
        assertEquals(original, content(), "File must be left untouched");
    }

    @Test
    void aliasedOverrides_leavesFileUntouched() throws Exception {
        // The overrides block is the very same node as 'shared', so editing its
        // entries would silently rewrite that key as well.
        String original = """
                shared: &shared
                  dep: 1.0.0
                overrides: *shared
                """;
        Files.writeString(workspaceFile().toPath(), original,
                StandardCharsets.UTF_8);

        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        workspace.setOverrides(Map.of("@vaadin/button", "25.2.6"));

        assertFalse(workspace.save(),
                "An overrides block that cannot be edited safely is no change");
        assertEquals(original, content(), "File must be left untouched");
    }

    @Test
    void commentOnlyFile_writesOverridesWithoutFailing() throws Exception {
        Files.writeString(workspaceFile().toPath(), "# just a comment\n",
                StandardCharsets.UTF_8);

        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        workspace.setOverrides(Map.of("dep", "1.0.0"));
        assertTrue(workspace.save());

        assertEquals("""
                overrides:
                  "dep": "1.0.0"
                """, content());
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

    @Test
    void save_isIdempotent_whenOnlyLayoutDiffers() throws Exception {
        // The configuration Flow writes, in the layout pnpm leaves behind after
        // an install: its own allowBuilds block, and scalars unquoted where
        // Flow quotes them.
        Files.writeString(workspaceFile().toPath(), """
                allowBuilds:
                  esbuild: set this to true or false
                overrides:
                  dep: 1.0.0
                """, StandardCharsets.UTF_8);

        PnpmWorkspaceFile workspace = new PnpmWorkspaceFile(projectRoot);
        workspace.setOverrides(Map.of("dep", "1.0.0"));

        assertFalse(workspace.save(),
                "A file holding the same content in a different layout is not a "
                        + "change; reporting one triggers a needless package install");
    }
}
