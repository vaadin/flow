/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import java.util.Random;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccess;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@PWA(name = "Live Reload View", shortName = "live-reload-view")
@Route(value = "com.vaadin.flow.uitest.ui.LiveReloadView", layout = ViewTestLayout.class)
public class LiveReloadView extends Div implements AppShellConfigurator {
    public static final String INSTANCE_IDENTIFIER = "instance-identifier";
    public static final String PAGE_RELOADING = "page-reloading";

    public static final String JAVA_LIVE_RELOAD_TRIGGER_BUTTON = "java-live-reload-trigger-button";

    public static final String FRONTEND_CODE_TEXT = "frontend-code-text";
    public static final String FRONTEND_CODE_UPDATE_BUTTON = "frontend-code-update-button";
    public static final String FRONTEND_CODE_RESET_BUTTON = "frontend-code-reset-button";

    public static final String CUSTOM_COMPONENT = "custom-component";

    private static final String FRONTEND_FILE = "custom-component.ts";
    private static File frontendFileBackup = null;

    private static final Random random = new Random();
    Integer instanceIdentifier = random.nextInt();

    @Tag("custom-component")
    @JsModule("./custom-component.ts")
    static class CustomComponent extends LitTemplate {
    }

    public LiveReloadView() {
        getStyle().set("display", "flex");
        getStyle().set("flex-direction", "column");
        getStyle().set("align-items", "flex-start");

        Span label = new Span(Integer.toString(instanceIdentifier));
        label.setId(INSTANCE_IDENTIFIER);
        add(label);

        NativeButton javaReloadButton = new NativeButton(
                "Trigger Java live reload");
        javaReloadButton.addClickListener(this::handleClickJavaLiveReload);
        javaReloadButton.setId(JAVA_LIVE_RELOAD_TRIGGER_BUTTON);
        add(javaReloadButton);

        Element codeArea = ElementFactory.createTextarea();
        codeArea.setProperty("id", FRONTEND_CODE_TEXT);
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

    // Java triggered live reload is faked as we do not have Trava JDK in test
    private void handleClickJavaLiveReload(ClickEvent<?> event) {
        addPageReloadingSpan();
        BrowserLiveReloadAccess liveReloadAccess = VaadinService.getCurrent()
                .getInstantiator().getOrCreate(BrowserLiveReloadAccess.class);
        BrowserLiveReload browserLiveReload = liveReloadAccess
                .getLiveReload(VaadinService.getCurrent());
        browserLiveReload.reload();
    }

    private void addPageReloadingSpan() {
        Span reloading = new Span("Reload triggered...");
        reloading.setId(PAGE_RELOADING);
        add(reloading);
    }

    public static void replaceFrontendFile(VaadinService vaadinService,
                                           String code) {
        File frontendFile = getFrontendFile(vaadinService);
        try {
            if (frontendFileBackup==null) {
                // make a backup so it can be restored at teardown
                frontendFileBackup = File.createTempFile("frontend", "ts");
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
        final String projectFrontendDir = FrontendUtils.getProjectFrontendDir(
                vaadinService.getDeploymentConfiguration());
        return new File(projectFrontendDir, FRONTEND_FILE);
    }
}
