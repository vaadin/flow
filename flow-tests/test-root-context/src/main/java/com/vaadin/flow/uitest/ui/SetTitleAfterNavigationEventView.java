/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import org.slf4j.LoggerFactory;

@Route(value = "com.vaadin.flow.uitest.ui.SetTitleAfterNavigationEventView", layout = ViewTestLayout.class)
public class SetTitleAfterNavigationEventView extends AbstractDivView
        implements HasDynamicTitle, AfterNavigationObserver {

    private String title = "my-initial-title";

    @Override
    public String getPageTitle() {
        LoggerFactory
                .getLogger(SetTitleAfterNavigationEventView.class.getName())
                .debug("HasDynamicTitle.getPageTitle() called. The current title value is = "
                        + title);
        return title;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        title = "my-changed-title-after-AfterNavigationEvent";
        LoggerFactory
                .getLogger(SetTitleAfterNavigationEventView.class.getName())
                .debug("AfterNavigationEvent listener called. The current title value is = "
                        + title);
    }
}
