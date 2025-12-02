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
import java.nio.file.Paths;
import java.util.List;

import com.vaadin.base.devserver.PublicStyleSheetBundler;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.StylesheetLiveReloadView", layout = ViewTestLayout.class)
@StyleSheet("context://css/view/view.css")
@StyleSheet("context://css/view/for-deletion.css")
public class StylesheetLiveReloadView extends AbstractLiveReloadView {

    private final PublicStyleSheetBundler bundler;

    public StylesheetLiveReloadView() {
        DeploymentConfiguration configuration = VaadinService.getCurrent()
                .getDeploymentConfiguration();
        File projectFolder = configuration.getProjectFolder();
        String outputFolder = configuration.getBuildFolder() + "/classes";
        File root = Paths.get(projectFolder.getAbsolutePath(), outputFolder,
                "META-INF", "resources").toFile();
        bundler = PublicStyleSheetBundler.forResourceLocations(List.of(root));

        add(makeDiv("appshell-style", "css/styles.css", "css/styles.css"));
        add(makeDiv("appshell-imported", "css/imported.css", "css/styles.css"));
        add(makeDiv("appshell-nested-imported",
                "css/nested/nested-imported.css", "css/styles.css"));
        add(makeDiv("appshell-image", "css/images/gobo.png", "css/styles.css"));
        add(makeDiv("view-style", "css/view/view.css", "css/view/view.css"));
        add(makeDiv("view-imported", "css/view/imported.css",
                "css/view/view.css"));
        add(makeDiv("view-nested-imported",
                "css/view/nested/nested-imported.css", "css/view/view.css"));
        add(makeDiv("view-image", "css/images/viking.png",
                "css/view/view.css"));
        add(makeDivForDelete());
    }

    private Div makeDiv(String cssClass, String resourceFileToChange,
            String mainCssFile) {
        Div div = new Div();
        div.setId(cssClass);
        div.setText("Style defined in " + resourceFileToChange);
        div.addClassName(cssClass);

        // Simulate Flow Hotswapper handling of CSS change
        NativeButton reloadButton = new NativeButton(
                "Trigger Stylesheet live reload", ev -> {
                    String bundledCssContent = getContentForFile(mainCssFile);
                    BrowserLiveReloadAccessor
                            .getLiveReloadFromService(
                                    VaadinService.getCurrent())
                            .ifPresent(reload -> reload.update(
                                    "context://" + mainCssFile,
                                    bundledCssContent));
                });
        reloadButton.setId("reload-" + cssClass);
        reloadButton.getElement().setAttribute("test-resource-file-path",
                resourceFileToChange);
        div.add(reloadButton);
        return div;
    }

    private Div makeDivForDelete() {
        // Separate element to test deletion of a stylesheet file
        Div deleteDiv = new Div();
        deleteDiv.setId("view-style-deleted");
        deleteDiv.setText("Style defined in css/view/view.css (delete test)");
        // Use the same class so initial style applies before deletion
        deleteDiv.addClassName("view-style-deleted");

        NativeButton deleteButton = new NativeButton(
                "Trigger Stylesheet delete",
                ev -> BrowserLiveReloadAccessor
                        .getLiveReloadFromService(VaadinService.getCurrent())
                        .ifPresent(reload -> reload.update(
                                "context://css/view/for-deletion.css", null)));
        deleteButton.setId("delete-view-style-deleted");
        deleteButton.getElement().setAttribute("test-resource-file-path",
                "css/view/for-deletion.css");
        deleteDiv.add(deleteButton);
        return deleteDiv;
    }

    private String getContentForFile(String cssFile) {
        String contextPath = VaadinRequest.getCurrent() != null
                ? VaadinRequest.getCurrent().getContextPath()
                : "";
        return bundler.bundle(cssFile, contextPath)
                .orElseThrow(AssertionError::new);
    }
}
