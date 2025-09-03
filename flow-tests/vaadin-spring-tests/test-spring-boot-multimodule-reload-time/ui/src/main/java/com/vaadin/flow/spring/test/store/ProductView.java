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
package com.vaadin.flow.spring.test.store;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.test.SpringDevToolsReloadUtils;

@Route(value = "product", layout = CatalogLayout.class, registerAtStartup = false)
@RouteAlias(value = "prod", layout = CatalogLayout.class)
public class ProductView extends VerticalLayout
        implements HasUrlParameter<Integer> {

    private Div productDetails;

    public ProductView() {
        productDetails = new Div();
        productDetails.setWidthFull();
        add(new H2("Product Details"));
        add(productDetails);
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent,
            @OptionalParameter Integer parameter) {
        productDetails.removeAll();
        productDetails.add(String.format("Showing product " + parameter));
        productDetails.add(new Hr());

        Span result = new Span();
        result.setId("result");
        result.setVisible(false);

        NativeButton startTriggerButton = SpringDevToolsReloadUtils
                .createReloadTriggerButton();

        productDetails.add(startTriggerButton, result);

        startTriggerButton.getElement()
                .addEventListener("componentready", event -> {

                    System.out.println("result: " + event.getEventData()
                            .get("event.detail.result").doubleValue());

                    result.setText(String
                            .format("Reload time by class change was [%s] ms",
                                    event.getEventData()
                                            .get("event.detail.result")
                                            .doubleValue()));
                    result.setVisible(true);

                }).addEventData("event.detail.result");

        UI.getCurrent().getPage().executeJs(
                "window.benchmark.measureRender($0);", startTriggerButton);
    }
}
