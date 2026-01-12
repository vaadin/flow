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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.NamespacedElementsView", layout = ViewTestLayout.class)
public class NamespacedElementsView extends AbstractDivView {

    @Override
    protected void onShow() {

        add(new Paragraph(
                "Testing svg and math elements (that needs to be instantiated with custom NS)"));

        var svg = new Element("svg");

        var rect = new Element("rect").setAttribute("x", "0")
                .setAttribute("y", "0").setAttribute("width", "100")
                .setAttribute("height", "100");
        var circle = new Element("circle").setAttribute("cx", "50")
                .setAttribute("cy", "50").setAttribute("r", "40")
                .setAttribute("fill", "white");
        svg.appendChild(rect, circle);

        /*
         * <math> <mfrac> <mn>1</mn> <mn>3</mn> </mfrac> </math>
         */
        var math = new Element("math");
        var mfrac = new Element("mfrac");
        math.appendChild(mfrac);
        var mn1 = new Element("mn");
        mn1.setText("1");
        var mn3 = new Element("mn");
        mn3.setText("3");
        mfrac.appendChild(mn1, mn3);

        getElement().appendChild(svg, math);

    }

}
