package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.dependencies.DependencyFilterView")
@HtmlImport("replaceme://something-that-doesnt-exist.html")
@JavaScript("frontend://com/vaadin/flow/uitest/ui/dependencies/eager.js")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/dependencies/eager.html")
public class DependencyFilterView extends DependenciesLoadingBaseView {
}
