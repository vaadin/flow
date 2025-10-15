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
package com.vaadin.flow.testutil;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;

import com.vaadin.flow.testutil.net.SimpleProxy;

public abstract class ChromeBrowserTestWithProxy
        extends AbstractBrowserConsoleTest {

    private static AtomicInteger availablePort = new AtomicInteger(2000);
    private SimpleProxy proxySession;
    private Integer proxyPort = null;

    private String rootUrl;

    @Override
    public void setup() throws Exception {
        super.setup();
        connectProxy();
    }

    @After
    public void teardownProxy() {
        disconnectProxy();
    }

    @Override
    public void checkIfServerAvailable() {
        rootUrl = super.getRootURL();
        try {
            super.checkIfServerAvailable();
        } finally {
            rootUrl = null;
        }
    }

    protected Integer getProxyPort() {
        if (proxyPort == null) {
            // Assumes we can use any port >= 2000,
            // except for 2049 in Firefox...
            proxyPort = availablePort.addAndGet(1);
            if (proxyPort == 2049) {
                // Restricted in Firefox, see
                // http://www-archive.mozilla.org/projects/netlib/PortBanning.html#portlist
                proxyPort = availablePort.addAndGet(1);
            }
        }
        return proxyPort;
    }

    /**
     * Disconnects the proxy if active
     */
    protected void disconnectProxy() {
        if (proxySession == null) {
            return;
        }
        proxySession.disconnect();
        proxySession = null;
    }

    /**
     * Ensure the proxy is active. Does nothing if the proxy is already active.
     */
    protected void connectProxy() throws IOException {
        if (proxySession != null) {
            return;
        }
        for (int i = 0; i < 10; i++) {
            // Potential workaround for problem with establishing many ssh
            // connections at the same time
            try {
                createProxy(getProxyPort());
                break;
            } catch (IOException e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException exception) {
                    new RuntimeException(exception);
                }
                if (i == 9) {
                    throw new RuntimeException(
                            "All 10 attempts to connect a proxy failed", e);
                }
            }
        }
    }

    private void createProxy(int proxyPort) throws IOException {
        proxySession = new SimpleProxy(proxyPort, getDeploymentHostname(),
                getDeploymentPort());
        proxySession.start();
    }

    @Override
    protected String getRootURL() {
        if (rootUrl != null) {
            return rootUrl;
        }
        return "http://" + getDeploymentHostname() + ":" + getProxyPort();
    }

}
