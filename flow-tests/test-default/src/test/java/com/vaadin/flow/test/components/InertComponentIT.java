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
package com.vaadin.flow.test.components;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import com.vaadin.flow.component.html.testbench.AnchorElement;
import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(InertComponentView.class)
public class InertComponentIT extends AbstractDefaultIT {

    @BrowserTest
    public void modalComponentAdded_inertButtonClicked_noNewComponentAdded() {
        open();

        final long initialBoxCount = getBoxCount();

        Optional<NativeButtonElement> newModalBoxButton = getNewModalBoxButton();

        newModalBoxButton.ifPresent(NativeButtonElement::click);

        validateBoxCount(initialBoxCount + 1, "Expected a new modal box.");

        newModalBoxButton.ifPresent(NativeButtonElement::click);

        validateBoxCount(initialBoxCount + 1,
                "Expected no new boxes as the button is now inert.");

        List<NativeButtonElement> removeButtons = getAll(
                NativeButtonElement.class, InertComponentView.REMOVE)
                .collect(Collectors.toList());
        removeButtons.get(removeButtons.size() - 1).click();

        validateBoxCount(initialBoxCount,
                "Expected the modal box was removed.");

        newModalBoxButton.ifPresent(NativeButtonElement::click);

        validateBoxCount(initialBoxCount + 1,
                "Expected a new modal box when button no longer inert.");
    }

    @BrowserTest
    public void modalComponentAdded_removedFromDom_othersStillInert() {
        open();

        final long initialBoxCount = getBoxCount();

        Optional<NativeButtonElement> newModalBoxButton = getNewModalBoxButton();

        newModalBoxButton.ifPresent(NativeButtonElement::click);

        validateBoxCount(initialBoxCount + 1, "Expected a new modal box.");

        // Remove the modal box from DOM
        ((JavascriptExecutor) getDriver())
                .executeScript("document.body.removeChild("
                        + "((v = document.querySelectorAll('[id^=\""
                        + InertComponentView.BOX
                        + "-\"]')) => v[v.length - 1])());");

        validateBoxCount(initialBoxCount,
                "Expected the modal box was removed from DOM.");

        newModalBoxButton.ifPresent(NativeButtonElement::click);

        validateBoxCount(initialBoxCount,
                "Expected no new box as UI still inert.");
    }

    @BrowserTest
    public void modalComponentAdded_routerLinkClicked_navigation() {
        open();

        final long initialBoxCount = getBoxCount();

        Optional<AnchorElement> linkToAnotherPage = getAll(AnchorElement.class,
                InertComponentView.LINK).findFirst();

        assertTrue(linkToAnotherPage.isPresent());

        getNewModalBoxButton().ifPresent(NativeButtonElement::click);

        validateBoxCount(initialBoxCount + 1, "Expected a new modal box.");

        linkToAnotherPage.get().click();

        waitForElementPresent(By.id(ModalDialogView.OPEN_MODAL_STRICT_BUTTON));

        assertNotNull(
                findElement(By.id(ModalDialogView.OPEN_MODAL_STRICT_BUTTON)));
    }

    private Optional<NativeButtonElement> getNewModalBoxButton() {
        return getAll(NativeButtonElement.class,
                InertComponentView.NEW_MODAL_BOX).findFirst();
    }

    private long getBoxCount() {
        return getAll(DivElement.class, InertComponentView.BOX).count();
    }

    private <T extends TestBenchElement> Stream<T> getAll(Class<T> elementClass,
            String idPrefix) {
        return $(elementClass).all().stream()
                .filter(e -> e.getAttribute("id").startsWith(idPrefix));
    }

    private void validateBoxCount(long initialBoxCount, String message) {
        Assertions.assertEquals(initialBoxCount, getBoxCount(), message);
    }
}
