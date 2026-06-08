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
package com.vaadin.cdi.itest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.parallel.Browser;

@RunWith(Arquillian.class)
@RunAsClient
abstract public class AbstractCdiTest extends ChromeBrowserTest {

    @ArquillianResource
    protected URL deploymentUrl;

    @Override
    protected String getRootURL() {
        return super.getRootURL() + deploymentUrl.getPath();
    }

    @Override
    protected int getDeploymentPort() {
        return deploymentUrl.getPort();
    }

    @Override
    protected String getTestPath() {
        return "/";
    }

    @Override
    public void setup() throws Exception {
        setDesiredCapabilities(Browser.CHROME.getDesiredCapabilities());
        super.setup();
    }

    protected void click(String elementId) {
        findElement(By.id(elementId)).click();
    }

    protected void follow(String linkText) {
        findElement(By.linkText(linkText)).click();
    }

    protected String getText(String id) {
        return findElement(By.id(id)).getText();
    }

    protected void assertCountEquals(int expectedCount, String counter)
            throws IOException {
        Assert.assertEquals(expectedCount, getCount(counter));
    }

    protected void waitForCount(int expectedCount, String counter) {
        waitUntil(driver -> {
            try {
                return getCount(counter) == expectedCount;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, 10);
    }

    protected void assertTextEquals(String expectedText, String elementId) {
        Assert.assertEquals(expectedText, getText(elementId));
    }

    protected void resetCounts() throws IOException {
        slurp("?resetCounts");
    }

    protected int getCount(String id) throws IOException {
        getCommandExecutor().waitForVaadin();
        String line = slurp("?getCount=" + id);
        return Integer.parseInt(line);
    }

    private String slurp(String uri) throws IOException {
        URL url = new URL(getRootURL() + uri);
        InputStream is = url.openConnection().getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = reader.readLine();
        reader.close();
        return line;
    }
}
