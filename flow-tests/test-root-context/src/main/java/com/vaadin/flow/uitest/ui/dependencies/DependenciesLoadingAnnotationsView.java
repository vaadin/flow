package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.router.Route;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.JavaScript;
import com.vaadin.ui.common.StyleSheet;

/**
 * See corresponding IT for more details.
 *
 * @author Vaadin Ltd.
 */
@Route("com.vaadin.flow.uitest.ui.dependencies.DependenciesLoadingAnnotationsView")
@JavaScript(value = "frontend://com/vaadin/flow/uitest/ui/dependencies/inline.js", loadMode = LoadMode.INLINE)
@StyleSheet(value = "frontend://com/vaadin/flow/uitest/ui/dependencies/inline.css", loadMode = LoadMode.INLINE)
@HtmlImport(value = "frontend://com/vaadin/flow/uitest/ui/dependencies/inline.html", loadMode = LoadMode.INLINE)
@JavaScript(value = "frontend://com/vaadin/flow/uitest/ui/dependencies/lazy.js", loadMode = LoadMode.LAZY)
@StyleSheet(value = "frontend://com/vaadin/flow/uitest/ui/dependencies/lazy.css", loadMode = LoadMode.LAZY)
@HtmlImport(value = "frontend://com/vaadin/flow/uitest/ui/dependencies/lazy.html", loadMode = LoadMode.LAZY)
@JavaScript("frontend://com/vaadin/flow/uitest/ui/dependencies/eager.js")
@StyleSheet("frontend://com/vaadin/flow/uitest/ui/dependencies/eager.css")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/dependencies/eager.html")
public class DependenciesLoadingAnnotationsView
        extends DependenciesLoadingBaseView {
}
