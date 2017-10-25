/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.router.event.BeforeNavigationObserver;
import com.vaadin.ui.Page;
import com.vaadin.ui.UI;
import com.vaadin.ui.html.Div;

public abstract class AbstractDivView extends Div
        implements BeforeNavigationObserver {

    public AbstractDivView() {
    }

    protected void onShow() {

    }

    @Override
    public void beforeNavigation(BeforeNavigationEvent event) {
        onShow();
    }

    protected Page getPage() {
        // getUI not available in onLocationChange so leaving getCurrent here
        // for now
        return UI.getCurrent().getPage();
    }

}
