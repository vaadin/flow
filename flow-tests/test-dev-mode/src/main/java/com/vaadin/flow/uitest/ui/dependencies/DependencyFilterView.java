/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.dependencies.DependencyFilterView")
@JavaScript("./eager.js")
@StyleSheet("./non-existing.css")
public class DependencyFilterView extends DependenciesLoadingBaseView {

    public DependencyFilterView() {
        super("");
        Div filtered = new Div();
        filtered.setText("filtered");
        filtered.setId("filtered-css");
        add(filtered);
    }
}
