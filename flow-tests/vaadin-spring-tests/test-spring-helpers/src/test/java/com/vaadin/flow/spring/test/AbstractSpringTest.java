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
package com.vaadin.flow.spring.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractSpringTest extends ChromeBrowserTest {

    private Properties properties;

    @Override
    protected String getTestURL(String... parameters) {
        return getTestURL(getRootURL(), getContextPath() + getTestPath(),
                parameters);
    }

    @Override
    protected int getDeploymentPort() {
        String proxyPort = getProperty("proxy.port");
        if (proxyPort != null) {
            return Integer.parseInt(proxyPort);
        }

        return super.getDeploymentPort();
    }

    protected String getContextRootURL() {
        return getRootURL() + getContextPath();
    }

    protected String getContextPath() {
        String proxyPublicPath = getProperty("proxy.path");
        if (proxyPublicPath != null) {
            return proxyPublicPath;
        }

        String contextPath = getProperty("server.servlet.contextPath");
        if (contextPath != null) {
            return contextPath;
        }

        return "";
    }

    private String getProperty(String key) {
        if (properties == null) {

            properties = new Properties();
            try {
                InputStream res = getClass()
                        .getResourceAsStream("/application.properties");
                if (res != null) {
                    properties.load(res);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return properties.getProperty(key);

    }

}
