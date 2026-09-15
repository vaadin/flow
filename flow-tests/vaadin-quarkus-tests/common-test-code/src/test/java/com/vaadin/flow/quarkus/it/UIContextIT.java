/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it;

import java.io.IOException;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.quarkus.it.uicontext.UIContextRootView;
import com.vaadin.flow.quarkus.it.uicontext.UINormalScopedBeanView;
import com.vaadin.flow.quarkus.it.uicontext.UIScopedBean;
import com.vaadin.flow.quarkus.it.uicontext.UIScopedLabel;
import com.vaadin.flow.quarkus.it.uicontext.UIScopedView;

@QuarkusIntegrationTest
public class UIContextIT extends AbstractCdiIT {

    private String uiId;

    @Override
    protected String getTestPath() {
        return "/ui";
    }

    @BeforeEach
    public void setUp() throws Exception {
        resetCounts();
        open();
        uiId = getText(UIContextRootView.UIID_LABEL);
    }

    @Test
    public void beanDestroyedOnUIClose() throws IOException {
        assertCountEquals(UIScopedBean.DESTROY_COUNTER_KEY, 0);
        click(UIContextRootView.CLOSE_UI_BTN);
        assertCountEquals(UIScopedBean.DESTROY_COUNTER_KEY, 1);
    }

    @Test
    public void beanDestroyedOnSessionClose()
            throws IOException, InterruptedException {
        assertCountEquals(UIScopedBean.DESTROY_COUNTER_KEY, 0);
        click(UIContextRootView.CLOSE_SESSION_BTN);

        assertCountEquals(UIScopedBean.DESTROY_COUNTER_KEY, 1);
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
        String beanId = getText(UIContextRootView.UI_SCOPED_BEAN_ID);
        assertTextEquals(uiId, UIScopedLabel.ID);
        follow(UIContextRootView.INJECTER_LINK);
        assertTextEquals(beanId, UIContextRootView.UI_SCOPED_BEAN_ID);
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

    private void assertCountEquals(String key, int expectedCount)
            throws IOException {
        assertCountEquals(expectedCount, key + uiId);
    }
}
