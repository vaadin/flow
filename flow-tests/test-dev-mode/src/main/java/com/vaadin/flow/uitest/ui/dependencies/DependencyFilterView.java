package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.dependencies.DependencyFilterView")
@JavaScript("./eager.js")
@StyleSheet("./non-existing.css")
public class DependencyFilterView extends DependenciesLoadingBaseView {

    public DependencyFilterView() {
        setId("filtered-css");
    }
}
