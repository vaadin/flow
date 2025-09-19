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
package com.vaadin.flow.component.page;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.ui.LoadMode;

public class PageStylesheetTest {

    private UI ui;
    private Page page;
    private UIInternals uiInternals;

    @Before
    public void setUp() {
        ui = Mockito.mock(UI.class);
        uiInternals = new UIInternals(ui);
        Mockito.when(ui.getInternals()).thenReturn(uiInternals);
        page = new Page(ui);
    }

    @Test
    public void addStyleSheet_returnsRegistration() {
        Registration registration = page.addStyleSheet("styles.css");

        Assert.assertNotNull("addStyleSheet should return a Registration",
                registration);
    }

    @Test
    public void addStyleSheetWithLoadMode_returnsRegistration() {
        Registration registration = page.addStyleSheet("styles.css",
                LoadMode.LAZY);

        Assert.assertNotNull(
                "addStyleSheet with LoadMode should return a Registration",
                registration);
    }

    @Test
    public void removeRegistration_removesStylesheet() {
        // Add a stylesheet
        Registration registration = page.addStyleSheet("styles.css");

        // Initially there should be a pending dependency
        Assert.assertFalse("Should have pending dependencies", uiInternals
                .getDependencyList().getPendingSendToClient().isEmpty());

        // Clear pending to simulate that it was sent
        uiInternals.getDependencyList().clearPendingSendToClient();

        // Remove the stylesheet
        registration.remove();

        // Should have a pending removal
        Assert.assertFalse("Should have pending removals",
                uiInternals.getPendingStyleSheetRemovals().isEmpty());
    }

    @Test
    public void multipleStylesheets_canBeRemovedIndependently() {
        Registration reg1 = page.addStyleSheet("style1.css");
        Registration reg2 = page.addStyleSheet("style2.css");

        // Clear pending dependencies
        uiInternals.getDependencyList().clearPendingSendToClient();

        // Remove first stylesheet
        reg1.remove();

        Assert.assertEquals("Should have 1 pending removal", 1,
                uiInternals.getPendingStyleSheetRemovals().size());

        // Clear removals
        uiInternals.clearPendingStyleSheetRemovals();

        // Remove second stylesheet
        reg2.remove();

        Assert.assertEquals("Should have 1 pending removal", 1,
                uiInternals.getPendingStyleSheetRemovals().size());
    }
}