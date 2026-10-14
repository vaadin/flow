/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.store;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.test.MainLayout;

@RoutePrefix("catalog")
@ParentLayout(MainLayout.class)
public class CatalogLayout extends VerticalLayout implements RouterLayout {

    private VerticalLayout rightSideLayout;

    public CatalogLayout() {
        rightSideLayout = new VerticalLayout();

        add(new H1("Product Catalog"));

        var leftSideLayout = new VerticalLayout();
        var layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.addAndExpand(leftSideLayout, rightSideLayout);

        for (int index = 0; index < 10; index++) {
            leftSideLayout.add(new RouterLink("Product " + index,
                    ProductView.class, index));
        }
        add(layout);
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        rightSideLayout.removeAll();
        if (content != null) {
            rightSideLayout.getElement().appendChild(content.getElement());
        }
    }

}
