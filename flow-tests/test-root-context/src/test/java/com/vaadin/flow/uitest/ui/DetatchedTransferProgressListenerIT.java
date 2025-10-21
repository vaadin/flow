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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import static com.vaadin.flow.uitest.ui.DetatchedTransferProgressListenerView.DOWNLOAD_AND_REMOVE;
import static com.vaadin.flow.uitest.ui.DetatchedTransferProgressListenerView.REMOVED_COMPONENT_DONE;

public class DetatchedTransferProgressListenerIT
        extends AbstractStreamResourceIT {

    @Test
    public void downloadRemovesComponent_successfullyUpdatetsUI()
            throws IOException {
        open();

        WebElement link = findElement(By.id(DOWNLOAD_AND_REMOVE));
        link.click();

        try {
            waitUntil(
                    driver -> isElementPresent(By.id(REMOVED_COMPONENT_DONE)));
        } catch (TimeoutException e) {
            Assert.fail("Success element never present.");
        }

    }

    private void waitForStatus(String id, String status) {
        waitUntil(driver -> {
            WebElement element = findElement(By.id(id));
            return element.getText().equals(status);
        });
    }
}
