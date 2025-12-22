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
package com.vaadin.viteapp.views.empty;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;

@Route("")
@JsModule("@vaadin/test-package-outside-npm/index.js")
@JsModule("@vaadin/test-package2-outside-npm/index.js")
@JsModule("./toplevelawait-main.js")
@CssImport("./image.css")
public class MainView extends Div {

    public static final String PLANT = "plant";
    public static final String HIDEPLANT = "hideplant";
    public static final String OUTSIDE = "outsideButton";
    public static final String OUTSIDE_RESULT = "outsideResult";

    public MainView() {
        Image img = new Image("themes/vite-production/images/plant.png",
                "placeholder plant");
        img.setId(PLANT);
        img.setWidth("200px");
        add(img);

        add(new H2("This place intentionally left empty"));
        add(new Paragraph("It’s a place where you can grow your own UI 🤗"));

        NativeButton button = new NativeButton("Show/hide plant", e -> {
            img.setVisible(!img.isVisible());
        });
        button.setId(HIDEPLANT);
        add(button);
        setSizeFull();
        getStyle().set("text-align", "center");

        NativeButton checkOutsideJs = new NativeButton("Check outside JS",
                e -> {
                    getElement().executeJs(OUTSIDE_RESULT
                            + ".innerText = window.packageOutsideNpm() + ' - ' + window.package2OutsideNpm();");
                });
        checkOutsideJs.setId(OUTSIDE);
        add(checkOutsideJs);
        Paragraph outsideStatus = new Paragraph();
        outsideStatus.setId(OUTSIDE_RESULT);
        add(outsideStatus);
    }

}
