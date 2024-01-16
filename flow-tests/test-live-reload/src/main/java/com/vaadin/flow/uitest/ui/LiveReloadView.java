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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Random;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccess;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.LiveReloadView", layout = ViewTestLayout.class)
@CssImport("./styles.css")
public class LiveReloadView extends Div {

    public static final String INSTANCE_IDENTIFIER = "instance-identifier";
    public static final String PAGE_RELOADING = "page-reloading";

    public static final String JAVA_LIVE_RELOAD_TRIGGER_BUTTON = "java-live-reload-trigger-button";
    public static final String WEBPACK_LIVE_RELOAD_TRIGGER_BUTTON = "webpack-live-reload-trigger-button";

    private static final Random random = new Random();
    Integer instanceIdentifier = random.nextInt();

    public LiveReloadView() {
        Span label = new Span(Integer.toString(instanceIdentifier));
        label.setId(INSTANCE_IDENTIFIER);
        add(label);

        NativeButton javaReloadButton = new NativeButton(
                "Trigger Java live reload");
        javaReloadButton.addClickListener(this::handleClickJavaLiveReload);
        javaReloadButton.setId(JAVA_LIVE_RELOAD_TRIGGER_BUTTON);
        add(javaReloadButton);

        NativeButton webpackReloadButton = new NativeButton(
                "Trigger Webpack live reload");
        webpackReloadButton
                .addClickListener(this::handleClickWebpackLiveReload);
        webpackReloadButton.setId(WEBPACK_LIVE_RELOAD_TRIGGER_BUTTON);
        add(webpackReloadButton);
    }

    // Java triggered live reload is faked as we do not have Trava JDK in test
    private void handleClickJavaLiveReload(ClickEvent event) {
        addPageReloadingSpan();
        BrowserLiveReloadAccess liveReloadAccess = VaadinService.getCurrent()
                .getInstantiator().getOrCreate(BrowserLiveReloadAccess.class);
        BrowserLiveReload browserLiveReload = liveReloadAccess
                .getLiveReload(VaadinService.getCurrent());
        browserLiveReload.reload();
    }

    // Touch a frontend file to trigger webpack reload
    private void handleClickWebpackLiveReload(ClickEvent event) {
        addPageReloadingSpan();
        DeploymentConfiguration configuration = VaadinService.getCurrent()
                .getDeploymentConfiguration();
        String projectFrontendDir = configuration.getStringProperty(
                FrontendUtils.PARAM_FRONTEND_DIR,
                FrontendUtils.DEFAULT_FRONTEND_DIR);
        File styleFile = new File(projectFrontendDir, "styles.css");
        try {
            BufferedWriter out = null;
            try {
                FileWriter fstream = new FileWriter(styleFile, true);
                out = new BufferedWriter(fstream);
                out.write(String.format(
                        "\nbody { background-color: rgb(%d,%d,%d); }",
                        random.nextInt(256), random.nextInt(256),
                        random.nextInt(256)));
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void addPageReloadingSpan() {
        Span reloading = new Span("Reload triggered...");
        reloading.setId(PAGE_RELOADING);
        add(reloading);
    }
}
