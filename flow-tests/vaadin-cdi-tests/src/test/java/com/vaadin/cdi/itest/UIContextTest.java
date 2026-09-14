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

import java.io.File;
import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.cdi.itest.uicontext.UIContextRootView;
import com.vaadin.cdi.itest.uicontext.UINormalScopedBeanView;
import com.vaadin.cdi.itest.uicontext.UIScopedLabel;
import com.vaadin.cdi.itest.uicontext.UIScopedView;

public class UIContextTest extends AbstractCdiTest {

    private String uiId;

    @Deployment(testable = false)
    public static WebArchive deployment() {
        return ArchiveProvider.createWebArchive("ui-context",
                webArchive -> webArchive
                        .addPackage(UIContextRootView.class.getPackage())
                        .addAsResource(new File("target/classes/META-INF")));
    }

    @Before
    public void setUp() throws Exception {
        resetCounts();
        open();
        uiId = getText(UIContextRootView.UIID_LABEL);
    }

    @Test
    public void beanDestroyedOnUIClose() throws IOException {
        assertDestroyCountEquals(0);
        click(UIContextRootView.CLOSE_UI_BTN);
        assertDestroyCountEquals(1);
    }

    @Test
    public void beanDestroyedOnSessionClose() throws IOException {
        assertDestroyCountEquals(0);
        click(UIContextRootView.CLOSE_SESSION_BTN);
        assertDestroyCountEquals(1);
    }

    @Test
    public void viewSurvivesNavigation() {
        follow(UIContextRootView.UISCOPED_LINK);
        assertTextEquals("", UIScopedView.VIEWSTATE_LABEL);
        click(UIScopedView.SETSTATE_BTN);
        assertTextEquals(UIScopedView.UISCOPED_STATE,
                UIScopedView.VIEWSTATE_LABEL);
        follow(UIScopedView.ROOT_LINK);
        follow(UIContextRootView.UISCOPED_LINK);
        assertTextEquals(UIScopedView.UISCOPED_STATE,
                UIScopedView.VIEWSTATE_LABEL);
    }

    @Test
    public void sameScopedComponentInjectedInOtherView() {
        assertTextEquals(uiId, UIScopedLabel.ID);
        follow(UIContextRootView.INJECTER_LINK);
        assertTextEquals(uiId, UIScopedLabel.ID);
    }

    @Test
    public void observerCalledOnInstanceAttachedToUI() {
        click(UIContextRootView.TRIGGER_EVENT_BTN);
        assertTextEquals(UIContextRootView.EVENT_PAYLOAD, UIScopedLabel.ID);
    }

    @Test
    public void normalScopedBeanInjectedToLargerScopeChangesWithActiveUI() {
        follow(UIContextRootView.NORMALSCOPED_LINK);
        assertTextEquals(uiId, UINormalScopedBeanView.UIID_LABEL);
        open();
        uiId = getText(UIContextRootView.UIID_LABEL);
        follow(UIContextRootView.NORMALSCOPED_LINK);
        assertTextEquals(uiId, UINormalScopedBeanView.UIID_LABEL);
    }

    private void assertDestroyCountEquals(int expectedCount)
            throws IOException {
        assertCountEquals(expectedCount, UIScopedLabel.DESTROY_COUNT + uiId);
    }
}
