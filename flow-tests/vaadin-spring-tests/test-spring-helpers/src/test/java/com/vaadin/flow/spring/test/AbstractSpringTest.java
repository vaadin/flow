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
package com.vaadin.flow.spring.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

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

    /**
     * Selects the given file in the native {@code <input type="file">} located
     * by id. The file is transferred to the browser first, so this works with
     * both locally run and remote browsers.
     *
     * @param inputId
     *            the id of the file input element
     * @param file
     *            the local file to upload
     */
    protected void uploadFileToNativeInput(String inputId, File file) {
        TestBenchElement input = $(TestBenchElement.class).id(inputId);
        setLocalFileDetector(input);
        input.sendKeys(file.getAbsolutePath());
    }

    /*
     * A remote browser cannot read a file path typed into the input, so the
     * local file has to be transferred to it. On a locally run browser this is
     * a no-op.
     */
    private void setLocalFileDetector(WebElement element) {
        if (getRunLocallyBrowser() != null) {
            return;
        }
        if (element instanceof WrapsElement) {
            element = ((WrapsElement) element).getWrappedElement();
        }
        if (element instanceof RemoteWebElement) {
            ((RemoteWebElement) element)
                    .setFileDetector(new LocalFileDetector());
        } else {
            throw new IllegalArgumentException(
                    "Expected argument of type RemoteWebElement, received "
                            + element.getClass().getName());
        }
    }

}
