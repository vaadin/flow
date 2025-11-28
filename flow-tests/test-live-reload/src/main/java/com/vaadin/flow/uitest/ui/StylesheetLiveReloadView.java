/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.CssBundler;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.StylesheetLiveReloadView", layout = ViewTestLayout.class)
@StyleSheet("context://css/view/view.css")
public class StylesheetLiveReloadView extends AbstractLiveReloadView {

    public StylesheetLiveReloadView() {
        add(makeDiv("appshell-style", "css/styles.css"));
        add(makeDiv("appshell-imported", "css/imported.css"));
        add(makeDiv("appshell-nested-imported",
                "css/nested/nested-imported.css"));
        add(makeDiv("view-style", "css/view/view.css"));
        add(makeDiv("view-imported", "css/view/imported.css"));
        add(makeDiv("view-nested-imported",
                "css/view/nested/nested-imported.css"));
    }

    private Div makeDiv(String cssClass, String resourceFile) {
        Div div = new Div();
        div.setId(cssClass);
        div.setText("Style defined in " + resourceFile);
        div.addClassName(cssClass);

        // Simulate Flow Hotswapper handling of CSS change
        NativeButton reloadButton = new NativeButton(
                "Trigger Stylesheet live reload", ev -> {
                    String cssContent = getContentForFile(resourceFile);
                    BrowserLiveReloadAccessor
                            .getLiveReloadFromService(
                                    VaadinService.getCurrent())
                            .ifPresent(reload -> reload.update(
                                    "context://" + resourceFile, cssContent));
                });
        reloadButton.setId("reload-" + cssClass);
        reloadButton.getElement().setAttribute("test-resource-file-path",
                resourceFile);
        div.add(reloadButton);
        return div;
    }

    private String getContentForFile(String cssFile) {
        DeploymentConfiguration configuration = VaadinService.getCurrent()
                .getDeploymentConfiguration();
        File projectFolder = configuration.getProjectFolder();
        String outputFolder = configuration.getBuildFolder() + "/classes";
        Path root = Paths.get(projectFolder.getAbsolutePath(), outputFolder,
                "META-INF", "resources");
        Path filePath = root.resolve(cssFile).normalize();
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        try {
            return CssBundler.inlineImportsForPublicResources(
                    filePath.toFile().getParentFile(), filePath.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to inline CSS content", e);
        }
    }
}
