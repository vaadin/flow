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
package com.vaadin.flow.uitest.ui.signal;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * View for testing binding width and height state to a Signal.
 */
@Route(value = "com.vaadin.flow.uitest.ui.signal.BindWidthHeightView", layout = ViewTestLayout.class)
public class BindWidthHeightView extends Div {

    private final ValueSignal<String> widthSignal = new ValueSignal<>("300px");
    private final ValueSignal<String> heightSignal = new ValueSignal<>("300px");

    public BindWidthHeightView() {
        setWidth("500px");
        setHeight("500px");
        getStyle().setBorder("1px dotted green");

        Span target = new Span();
        target.getStyle().setDisplay(Style.Display.INLINE_BLOCK);
        target.getStyle().setBackground("lightgray");
        target.getStyle().setBorder("2px solid black");
        target.setId("target");

        Span widthInfo = new Span();
        Span heightInfo = new Span();
        target.add(widthInfo, new Span(" x "), heightInfo);

        widthInfo.bindText(widthSignal);
        heightInfo.bindText(heightSignal);

        target.bindWidth(widthSignal);
        target.bindHeight(heightSignal);

        NativeButton setWidth100Button = new NativeButton(
                "widthSignal.set(\"100%\")", e -> widthSignal.set("100%"));
        setWidth100Button.setId("width-100-pct");

        NativeButton setWidthNullButton = new NativeButton(
                "widthSignal.set(null)", e -> widthSignal.set(null));
        setWidthNullButton.setId("width-null");

        NativeButton setHeight100Button = new NativeButton(
                "heightSignal.set(\"100%\")", e -> heightSignal.set("100%"));
        setHeight100Button.setId("height-100-pct");

        NativeButton setHeightNullButton = new NativeButton(
                "heightSignal.set(null)", e -> heightSignal.set(null));
        setHeightNullButton.setId("height-null");

        add(target, new Div(setWidth100Button, setWidthNullButton,
                setHeight100Button, setHeightNullButton));
    }

}
