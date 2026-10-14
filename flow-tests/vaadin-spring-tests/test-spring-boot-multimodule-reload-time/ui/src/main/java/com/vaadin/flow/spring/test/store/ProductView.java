/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
                            .getNumber("event.detail.result"));

                    result.setText(String.format(
                            "Reload time by class change was [%s] ms",
                            event.getEventData()
                                    .getNumber("event.detail.result")));
                    result.setVisible(true);

                }).addEventData("event.detail.result");

        UI.getCurrent().getPage().executeJs(
                "window.benchmark.measureRender($0);", startTriggerButton);
    }
}
