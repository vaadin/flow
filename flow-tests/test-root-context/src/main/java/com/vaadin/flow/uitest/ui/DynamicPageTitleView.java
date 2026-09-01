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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.DynamicPageTitle;
import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.router.PageTitleGenerator;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DynamicPageTitleView", layout = ViewTestLayout.class)
@DynamicPageTitle(DynamicPageTitleView.Generator.class)
public class DynamicPageTitleView extends AbstractDivView {

    public static class Generator implements PageTitleGenerator {
        @Override
        public String generatePageTitle(PageTitleContext context) {
            return "generated dynamic title";
        }
    }

    @Override
    protected void onShow() {
        removeAll();
        Div div = new Div();
        div.setText("Dynamic page title view");
        div.setId("dynamic-page-title");
        add(div);
    }
}
