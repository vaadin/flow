/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ExtendedClientDetailsView", layout = ViewTestLayout.class)
public class ExtendedClientDetailsView extends AbstractDivView {

    @Override
    protected void onShow() {
        Div screenWidth = createDiv("sw");
        Div screenHeight = createDiv("sh");
        Div windowInnerWidth = createDiv("ww");
        Div windowInnerHeight = createDiv("wh");
        Div bodyElementWidth = createDiv("bw");
        Div bodyElementHeight = createDiv("bh");
        Div devicePixelRatio = createDiv("pr");
        Div touchDevice = createDiv("td");

        // the sizing values cannot be set with JS but pixel ratio and touch
        // support can be faked
        NativeButton setValuesButton = new NativeButton("Set test values",
                event -> {
                    getUI().ifPresent(ui -> ui.getPage()
                            .executeJs("{"
                                    + "window.devicePixelRatio = 2.0;"
                                    + "navigator.msMaxTouchPoints = 1;"
                                    + "}"));
                });
        setValuesButton.setId("set-values");

        NativeButton fetchDetailsButton = new NativeButton(
                "Fetch client details", event -> {
                    getUI().ifPresent(ui -> ui.getPage()
                            .retrieveExtendedClientDetails(details -> {
                                screenWidth
                                        .setText("" + details.getScreenWidth());
                                screenHeight.setText(
                                        "" + details.getScreenHeight());
                                windowInnerWidth.setText(
                                        "" + details.getWindowInnerWidth());
                                windowInnerHeight.setText(
                                        "" + details.getWindowInnerHeight());
                                bodyElementWidth.setText(
                                        "" + details.getBodyClientWidth());
                                bodyElementHeight.setText(
                                        "" + details.getBodyClientHeight());
                                devicePixelRatio.setText(
                                        "" + details.getDevicePixelRatio());
                                touchDevice
                                        .setText("" + details.isTouchDevice());
                            }));
                });
        fetchDetailsButton.setId("fetch-values");

        add(setValuesButton, fetchDetailsButton);
    }

    private Div createDiv(String id) {
        Div div = new Div();
        div.setId(id);
        add(div);
        return div;
    }
}
