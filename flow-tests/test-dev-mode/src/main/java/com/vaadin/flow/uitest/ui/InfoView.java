/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.InfoView", layout = ViewTestLayout.class)
public class InfoView extends Div {

    public InfoView() {
        setClassName("infoContainer");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (attachEvent.isInitialAttach()) {
            update(attachEvent.getUI());
        }
    }

    private void update(UI ui) {
        VaadinSession session = ui.getSession();
        WebBrowser webBrowser = session.getBrowser();
        DeploymentConfiguration deploymentConfiguration = session
                .getConfiguration();
        List<String> device = new ArrayList<>();
        List<String> os = new ArrayList<>();
        List<String> browser = new ArrayList<>();

        removeAll();
        add(new NativeButton("Refresh", e -> {
            update(ui);
        }));

        header("Browser");
        info("Address", webBrowser.getAddress());

        add(device, "Android", webBrowser.isAndroid());
        add(device, "iOS", webBrowser.isIOS());
        add(device, "iPad", webBrowser.isIPad());
        add(device, "iPhone", webBrowser.isIPhone());
        add(device, "Windows Phone", webBrowser.isWindowsPhone());

        info("Device", device.stream().collect(Collectors.joining(", ")));

        add(os, "Linux", webBrowser.isLinux());
        add(os, "Mac", webBrowser.isMacOSX());
        add(os, "Windows", webBrowser.isWindows());

        info("Os", os.stream().collect(Collectors.joining(", ")));

        add(browser, "Chrome", webBrowser.isChrome());
        add(browser, "Edge", webBrowser.isEdge());
        add(browser, "Firefox", webBrowser.isFirefox());
        add(browser, "IE", webBrowser.isIE());
        add(browser, "Safari", webBrowser.isSafari());

        info("Browser", browser.stream().collect(Collectors.joining(", ")));

        if (webBrowser.isTooOldToFunctionProperly()) {
            header("Browser is too old to function properly");
        }
        info("User-agent", webBrowser.getBrowserApplication());
        info("Browser major", webBrowser.getBrowserMajorVersion());
        info("Browser minor", webBrowser.getBrowserMinorVersion());
        info("Locale", webBrowser.getLocale());

        info("Secure connection (https)", webBrowser.isSecureConnection());

        separator();

        header("Push configuration");
        info("Push mode", ui.getPushConfiguration().getPushMode());
        info("Push transport", ui.getPushConfiguration().getTransport());

        separator();

        header("Deployment configuration");
        info("Heartbeat interval",
                deploymentConfiguration.getHeartbeatInterval());
        info("UI class", deploymentConfiguration.getUIClassName());
        info("Close idle sessions",
                deploymentConfiguration.isCloseIdleSessions());
        info("Send URLs as parameters",
                deploymentConfiguration.isSendUrlsAsParameters());
        info("Sync id enabled", deploymentConfiguration.isSyncIdCheckEnabled());
        info("XSRF protection enabled",
                deploymentConfiguration.isXsrfProtectionEnabled());
        info("Production mode", deploymentConfiguration.isProductionMode());

    }

    private void add(List<String> collection, String value, boolean add) {
        if (add) {
            collection.add(value);
        }
    }

    private void separator() {
        add(new Hr());
    }

    private void header(String header) {
        new Html("<div><b>" + header + "</b></div>");
    }

    private void info(String header, Object value) {
        add(new Html("<div>" + header + ": " + value + "</div>"));
    }
}
