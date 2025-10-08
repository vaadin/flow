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
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.FrontendLiveReloadView", layout = ViewTestLayout.class)
public class FrontendLiveReloadView extends AbstractLiveReloadView {
    public static final String FRONTEND_CODE_TEXT = "frontend-code-text";
    public static final String FRONTEND_CODE_UPDATE_BUTTON = "frontend-code-update-button";
    public static final String FRONTEND_CODE_RESET_BUTTON = "frontend-code-reset-button";

    public static final String CUSTOM_COMPONENT = "custom-component";

    private static final String FRONTEND_FILE = "custom-component.ts";
    private static File frontendFileBackup = null;

    @Tag("custom-component")
    @JsModule("./custom-component.ts")
    static class CustomComponent extends LitTemplate {
    }

    public FrontendLiveReloadView() {
        Element codeArea = ElementFactory.createTextarea();
        codeArea.setProperty("id", FRONTEND_CODE_TEXT);
        codeArea.setAttribute("rows", "10");
        codeArea.getStyle().set("width", "500px");
        getElement().appendChild(codeArea);
        File frontendFile = getFrontendFile(VaadinService.getCurrent());
        try {
            String code = FileUtils.readFileToString(frontendFile,
                    StandardCharsets.UTF_8);
            codeArea.setText(code);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Element updateFrontend = ElementFactory
                .createButton("Replace frontend code");
        updateFrontend.setProperty("id", FRONTEND_CODE_UPDATE_BUTTON);
        updateFrontend.setAttribute("onclick",
                "fetch('update_frontend', { method: 'POST', body: document.getElementById('"
                        + FRONTEND_CODE_TEXT + "').value})");
        getElement().appendChild(updateFrontend);

        Element resetFrontend = ElementFactory
                .createButton("Reset frontend code");
        resetFrontend.setProperty("id", FRONTEND_CODE_RESET_BUTTON);
        resetFrontend.setAttribute("onclick", "fetch('reset_frontend')");
        getElement().appendChild(resetFrontend);

        CustomComponent customComponent = new CustomComponent();
        customComponent.setId(CUSTOM_COMPONENT);
        add(customComponent);
    }

    public static void replaceFrontendFile(VaadinService vaadinService,
            String code) {
        File frontendFile = getFrontendFile(vaadinService);
        try {
            if (frontendFileBackup == null) {
                // make a backup so it can be restored at teardown
                frontendFileBackup = File.createTempFile(
                        FrontendUtils.DEFAULT_FRONTEND_DIR, "ts");
                FileUtils.copyFile(frontendFile, frontendFileBackup);
            }
            FileUtils.write(frontendFile, code, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void resetFrontendFile(VaadinService vaadinService) {
        if (frontendFileBackup != null) {
            File frontendFile = getFrontendFile(vaadinService);
            try {
                FileUtils.copyFile(frontendFileBackup, frontendFile);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static File getFrontendFile(VaadinService vaadinService) {
        final File projectFrontendDir = FrontendUtils.getProjectFrontendDir(
                vaadinService.getDeploymentConfiguration());
        return new File(projectFrontendDir, FRONTEND_FILE);
    }
}
