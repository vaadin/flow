/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.Hr;
import com.vaadin.hummingbird.router.View;
import com.vaadin.server.WebBrowser;
import com.vaadin.ui.AttachEvent;
import com.vaadin.ui.Html;
import com.vaadin.ui.UI;

public class InfoView extends Div implements View {

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
        removeAll();
        add(new Button("Refresh", e -> {
            update(ui);
        }));
        info("<b>Browser</b>");
        WebBrowser webBrowser = ui.getSession().getBrowser();
        info("Address: " + webBrowser.getAddress());
        String os = "";
        String browser = "";
        String device = "";

        if (webBrowser.isAndroid()) {
            device += "Android ";
        }
        if (webBrowser.isIOS()) {
            device += "iOS ";
        }
        if (webBrowser.isIPad()) {
            device += "iPad ";
        }
        if (webBrowser.isIPhone()) {
            device += "iPhone ";
        }
        if (webBrowser.isWindowsPhone()) {
            device += "Windows Phone ";
        }
        if (webBrowser.isLinux()) {
            os += "Linux ";
        }
        if (webBrowser.isMacOSX()) {
            os += "Mac ";
        }
        if (webBrowser.isWindows()) {
            os += "Windows ";
        }
        if (webBrowser.isTouchDevice()) {
            browser += "Touch device ";
        }
        if (webBrowser.isChrome()) {
            browser += "Chrome ";
        }
        if (webBrowser.isEdge()) {
            browser += "Edge ";
        }
        if (webBrowser.isFirefox()) {
            browser += "Firefox ";
        }
        if (webBrowser.isIE()) {
            browser += "IE ";
        }
        if (webBrowser.isPhantomJS()) {
            browser += "PhantomJS ";
        }
        if (webBrowser.isSafari()) {
            browser += "Safari ";
        }
        info("Browser: " + browser.trim());
        info("Device: " + device.trim());
        info("Os: " + os.trim());

        if (webBrowser.isTooOldToFunctionProperly()) {
            info("Too old to function properly ");
        }
        info("User-agent: " + webBrowser.getBrowserApplication());
        info("Browser major: " + webBrowser.getBrowserMajorVersion());
        info("Browser minor: " + webBrowser.getBrowserMinorVersion());
        info("Screen height: " + webBrowser.getScreenHeight());
        info("Screen width: " + webBrowser.getScreenWidth());
        info("Locale: " + webBrowser.getLocale());

        if (webBrowser.isSecureConnection()) {
            info("Secure connection (https): ");
        }
        add(new Hr());
        info("<b>Push configuration</b>");
        info("Push mode: " + ui.getPushConfiguration().getPushMode());
        info("Push transport: " + ui.getPushConfiguration().getTransport());

        add(new Hr());
        info("<b>Deployment configuration</b>");
        info("Heartbeat interval: "
                + ui.getSession().getConfiguration().getHeartbeatInterval());
        info("Router configurator class: " + ui.getSession().getConfiguration()
                .getRouterConfiguratorClassName());
        info("UI class: "
                + ui.getSession().getConfiguration().getUIClassName());
        info("Close idle sessions: "
                + ui.getSession().getConfiguration().isCloseIdleSessions());
        info("Send URLs as parameters: "
                + ui.getSession().getConfiguration().isSendUrlsAsParameters());
        info("Sync id enabled: "
                + ui.getSession().getConfiguration().isSyncIdCheckEnabled());
        info("XSRF protection enabled: "
                + ui.getSession().getConfiguration().isXsrfProtectionEnabled());
        info("Production mode: "
                + ui.getSession().getConfiguration().isProductionMode());

    }

    private void info(String string) {
        Html html = new Html("<div>" + string + "</div>");
        add(html);

    }
}
