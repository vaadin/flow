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
