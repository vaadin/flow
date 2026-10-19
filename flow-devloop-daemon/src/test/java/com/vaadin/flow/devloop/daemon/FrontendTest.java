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
package com.vaadin.flow.devloop.daemon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every judgement the frontend leg makes is here rather than in
 * {@code TransactionEngine}, precisely so it can be tested from a directory and
 * a string. This is where the dev-bundle/Vite split, the classification and the
 * escalation reasons are pinned.
 */
class FrontendTest {

    @TempDir
    private Path app;

    private final List<String> properties = new java.util.ArrayList<>();

    @AfterEach
    void clearProperties() {
        properties.forEach(System::clearProperty);
    }

    // --- where the folder comes from ----------------------------------------

    @Test
    void root_prefersTheOverrideOverEverythingElse() throws IOException {
        Path convention = directory("src/main/frontend");
        Path elsewhere = directory("custom-ui");
        buildInfo(convention);
        property("vaadin.dev.frontend", elsewhere.toString());

        Frontend frontend = frontend();

        assertEquals(elsewhere, frontend.root().orElseThrow());
        assertEquals(Frontend.Source.OVERRIDE, frontend.source());
    }

    @Test
    void root_overrideMayBeRelativeToTheApplication() throws IOException {
        Path elsewhere = directory("custom-ui");
        property("vaadin.dev.frontend", "custom-ui");

        assertEquals(elsewhere, frontend().root().orElseThrow());
    }

    @Test
    void root_readsTheFolderFlowItselfResolved() throws IOException {
        // The token is preferred over the convention because Flow wrote it
        // after resolving <frontendDirectory>, which the convention cannot see.
        Path configured = directory("ui");
        directory("src/main/frontend");
        buildInfo(configured);

        Frontend frontend = frontend();

        assertEquals(configured, frontend.root().orElseThrow());
        assertEquals(Frontend.Source.BUILD_INFO, frontend.source());
    }

    @Test
    void root_buildInfoNamingAMissingFolder_fallsBackToTheConvention()
            throws IOException {
        Path convention = directory("src/main/frontend");
        buildInfo(app.resolve("gone"));

        assertEquals(convention, frontend().root().orElseThrow());
    }

    @Test
    void root_malformedBuildInfo_fallsBackToTheConvention() throws IOException {
        Path convention = directory("src/main/frontend");
        Path token = app.resolve("target/classes/META-INF/VAADIN/config");
        Files.createDirectories(token);
        Files.writeString(token.resolve("flow-build-info.json"), "{ oops");

        assertEquals(convention, frontend().root().orElseThrow());
    }

    @Test
    void root_legacyFrontendFolder_isFoundWhenTheDefaultIsAbsent()
            throws IOException {
        Path legacy = directory("frontend");

        Frontend frontend = frontend();

        assertEquals(legacy, frontend.root().orElseThrow());
        assertEquals(Frontend.Source.CONVENTION, frontend.source());
    }

    @Test
    void root_noFrontendFolderAtAll_isEmpty() {
        assertEquals(Optional.empty(), frontend().root());
    }

    @Test
    void root_aFolderAppearingLater_isPickedUpWithoutARestart()
            throws IOException {
        Frontend frontend = frontend();
        assertTrue(frontend.root().isEmpty());

        Path created = directory("src/main/frontend");

        assertEquals(created, frontend.root().orElseThrow());
    }

    // --- what a change means -------------------------------------------------

    @Test
    void kind_buildOutputAndDependencies_areIgnored() {
        assertKind(Frontend.Kind.IGNORED, "generated/vaadin.ts",
                "generated/flow/generated-flow-imports.js",
                "node_modules/foo/index.js");
    }

    @Test
    void kind_editorLeftovers_areIgnored() {
        assertKind(Frontend.Kind.IGNORED, ".DS_Store", "views/.styles.css.swp",
                "views/main.ts~", "~main.ts");
    }

    @Test
    void kind_indexHtml_isServedFromDisk() {
        // Dev mode reads it per request, which is why the bundle's own hash
        // check skips it.
        assertKind(Frontend.Kind.SERVED_LIVE, "index.html");
    }

    @Test
    void kind_themeStylesheets_arePushable() {
        assertKind(Frontend.Kind.THEME_CSS, "themes/my-theme/styles.css",
                "themes/my-theme/components/vaadin-button.css",
                "themes/my-theme/document.css");
    }

    @Test
    void kind_themeAssets_areServedFromDisk() {
        assertKind(Frontend.Kind.SERVED_LIVE, "themes/my-theme/logo.png",
                "themes/my-theme/fonts/body.woff2");
    }

    @Test
    void kind_themeJson_isBundledRatherThanPushed() {
        // The bundle validation compares theme.json's contents, so changing it
        // needs the bundle rebuilt - it is not a stylesheet.
        assertKind(Frontend.Kind.BUNDLED, "themes/my-theme/theme.json");
    }

    @Test
    void kind_everythingElse_isBundled() {
        // index.tsx is not index.html: it is hashed into the bundle.
        assertKind(Frontend.Kind.BUNDLED, "index.tsx", "views/main.ts",
                "views/main.tsx", "util.js", "styles/app.css",
                "themes/loose-file.css");
    }

    // --- the plan ------------------------------------------------------------

    @Test
    void plan_themeCssInBundleMode_isPushedAndNeverEscalates()
            throws IOException {
        directory("src/main/frontend");
        Frontend.Plan plan = plan(false, List.of("themes/t/styles.css"),
                List.of());

        assertEquals(1, plan.themeCss().size());
        assertEquals("", plan.escalation());
        assertTrue(plan.hasWork());
    }

    @Test
    void plan_bundledFileInBundleMode_escalatesToARestart() throws IOException {
        directory("src/main/frontend");
        Frontend.Plan plan = plan(false, List.of("views/main.ts"), List.of());

        assertEquals("frontend changed (dev bundle rebuild)",
                plan.escalation());
    }

    @Test
    void plan_deletionOutranksEverything() throws IOException {
        directory("src/main/frontend");
        Frontend.Plan plan = plan(false, List.of("views/main.ts"),
                List.of("views/gone.ts", "views/also-gone.ts"));

        assertEquals("2 frontend file(s) removed (dev bundle rebuild)",
                plan.escalation());
    }

    @Test
    void plan_themeJsonAlone_saysSo() throws IOException {
        directory("src/main/frontend");
        Frontend.Plan plan = plan(false, List.of("themes/t/theme.json"),
                List.of());

        assertEquals("theme.json changed (dev bundle rebuild)",
                plan.escalation());
    }

    @Test
    void plan_inViteMode_nothingEscalates() throws IOException {
        // Vite watches the folder itself and applied all of this on save,
        // deletions included.
        directory("src/main/frontend");
        Frontend.Plan plan = plan(true,
                List.of("views/main.ts", "themes/t/theme.json"),
                List.of("views/gone.ts"));

        assertEquals("", plan.escalation());
        assertTrue(plan.vite());
        assertEquals(3, plan.size());
    }

    @Test
    void plan_onlyIgnoredFiles_isNoWorkAtAll() throws IOException {
        directory("src/main/frontend");
        Frontend.Plan plan = plan(false, List.of("generated/vaadin.ts"),
                List.of());

        assertFalse(plan.hasWork());
        assertEquals("", plan.escalation());
    }

    @Test
    void plan_servedLiveOnly_needsAReloadButNotARestart() throws IOException {
        directory("src/main/frontend");
        Frontend.Plan plan = plan(false,
                List.of("index.html", "themes/t/logo.png"), List.of());

        assertEquals(2, plan.servedLive().size());
        assertEquals("", plan.escalation());
    }

    // --- helpers -------------------------------------------------------------

    private Frontend frontend() {
        return Frontend.of(Reactor.Module.of(app, "app"));
    }

    private void assertKind(Frontend.Kind expected, String... relatives) {
        for (String relative : relatives) {
            assertEquals(expected, Frontend.kindOfRelative(relative), relative);
        }
    }

    private Frontend.Plan plan(boolean vite, List<String> modified,
            List<String> deleted) {
        Frontend frontend = frontend();
        Path root = frontend.root().orElseThrow();
        return frontend.plan(modified.stream().map(root::resolve).toList(),
                deleted.stream().map(root::resolve).toList(), vite);
    }

    private Path directory(String relative) throws IOException {
        return Files.createDirectories(app.resolve(relative));
    }

    private void buildInfo(Path frontendFolder) throws IOException {
        Path config = app.resolve("target/classes/META-INF/VAADIN/config");
        Files.createDirectories(config);
        Files.writeString(config.resolve("flow-build-info.json"),
                "{\n  \"frontendFolder\" : \""
                        + Json.escape(frontendFolder.toString())
                        + "\",\n  \"frontend.hotdeploy\" : true\n}\n");
    }

    private void property(String name, String value) {
        System.setProperty(name, value);
        properties.add(name);
    }
}
