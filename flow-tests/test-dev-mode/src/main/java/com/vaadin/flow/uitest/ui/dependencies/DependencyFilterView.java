package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.dependencies.DependencyFilterView")
@JavaScript("frontend://com/vaadin/flow/uitest/ui/dependencies/eager.js")
public class DependencyFilterView extends DependenciesLoadingBaseView {
}
