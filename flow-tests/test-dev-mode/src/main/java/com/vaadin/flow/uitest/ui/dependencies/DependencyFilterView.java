package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.dependencies.DependencyFilterView")
@StyleSheet("replaceme://something-that-doesnt-exist.css")
@JavaScript("frontend://com/vaadin/flow/uitest/ui/dependencies/eager.js")
public class DependencyFilterView extends DependenciesLoadingBaseView {
}
