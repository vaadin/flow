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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebElement;

import com.vaadin.testbench.TestBenchElement;

public class UploadIT extends AbstractSpringTest {

    private static final String UPLOAD_ID = "upl";

    @Test
    @Ignore
    public void multiFileUpload() throws Exception {
        open();

        waitUntil(driver -> {
            By selector = By.id(UPLOAD_ID);
            return isElementPresent(selector)
                    && findElement(selector).isDisplayed();
        });

        File tempFile = createTempFile("foo");

        TestBenchElement input = $(TestBenchElement.class).id(UPLOAD_ID)
                .$(TestBenchElement.class).id("fileInput");
        setLocalFileDetector(input);
        input.sendKeys(tempFile.getPath());

        waitUntil(driver -> isElementPresent(By.className("uploaded-text")));
        WebElement uploadedText = findElement(By.className("uploaded-text"));
        Assert.assertEquals("foo", uploadedText.getText());
    }

    @Test
    public void uploadComponentIsInitialized() {
        open();

        waitUntil(driver -> isElementPresent(By.id(UPLOAD_ID))
                && findElement(By.id(UPLOAD_ID)).isDisplayed());

        List<TestBenchElement> inputs = $(TestBenchElement.class).id(UPLOAD_ID)
                .$("input").all();
        Assert.assertFalse(
                "Upload element is not initialized: it doesn't contain "
                        + "any child element (so it has no element in the shadow root)",
                inputs.isEmpty());
    }

    @Override
    protected String getTestPath() {
        return "/multipart-upload";
    }

    private File createTempFile(String content) throws IOException {
        File tempFile = File.createTempFile("TestFileUpload", ".txt");
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
        writer.write(content);
        writer.close();
        tempFile.deleteOnExit();
        return tempFile;
    }

    private void setLocalFileDetector(WebElement element) throws Exception {
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
