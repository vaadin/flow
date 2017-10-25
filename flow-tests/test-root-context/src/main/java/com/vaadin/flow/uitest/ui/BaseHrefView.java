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

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.HasUrlParameter;
import com.vaadin.router.Route;
import com.vaadin.router.WildcardParameter;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.ui.html.Anchor;

@Route(value = "com.vaadin.flow.uitest.ui.BaseHrefView", layout = ViewTestLayout.class)
public class BaseHrefView extends AbstractDivView
        implements HasUrlParameter<String> {

    public BaseHrefView() {
        add(new Anchor("link", "My link"));
    }

    @Override
    public void setParameter(BeforeNavigationEvent event,
            @WildcardParameter String parameter) {
        // Noop
    }
}
