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
package com.vaadin.flow.uitest.ui.littemplate;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.littemplate.InnerTemplateVisibilityView", layout = ViewTestLayout.class)
public class InnerTemplateVisibilityView extends AbstractDivView {

    public static final String TOGGLE_INNER_VISIBILITY_BUTTON_ID = "toggleInnerVisibility";
    public static final String TOGGLE_OUTER_VISIBILITY_BUTTON_ID = "toggleOuterVisibility";
    public static final String INNER_ID = "inner";
    public static final String OUTER_ID = "outer";

    @Tag("lit-template-inner")
    @JsModule("./lit-templates/lit-template-inner.js")
    public static class Inner extends LitTemplate {
        public Inner() {
        }
    }

    @Tag("lit-template-outer")
    @JsModule("./lit-templates/lit-template-outer.js")
    public static class Outer extends LitTemplate {
        @Id("inner")
        Inner inner;

        public Outer() {
        }
    }

    public InnerTemplateVisibilityView() {
        Outer outer = new Outer();
        outer.setId(OUTER_ID);
        outer.inner.setId(INNER_ID);

        NativeButton toggleOuterVisibilityButton = new NativeButton(
                "Toggle visibility of outer",
                e -> outer.setVisible(!outer.isVisible()));
        toggleOuterVisibilityButton.setId(TOGGLE_OUTER_VISIBILITY_BUTTON_ID);

        NativeButton toggleInnerVisibility = new NativeButton(
                "Toggle visibility of inner",
                e -> outer.inner.setVisible(!outer.inner.isVisible()));
        toggleInnerVisibility.setId(TOGGLE_INNER_VISIBILITY_BUTTON_ID);

        add(toggleOuterVisibilityButton, toggleInnerVisibility, outer);
    }
}
