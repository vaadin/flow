package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * See corresponding IT for more details.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
@Route("com.vaadin.flow.uitest.ui.dependencies.DependenciesLoadingAnnotationsView")
@JavaScript(value = "frontend://com/vaadin/flow/uitest/ui/dependencies/inline.js", loadMode = LoadMode.INLINE)
@StyleSheet(value = "frontend://com/vaadin/flow/uitest/ui/dependencies/inline.css", loadMode = LoadMode.INLINE)
@JavaScript(value = "frontend://com/vaadin/flow/uitest/ui/dependencies/lazy.js", loadMode = LoadMode.LAZY)
@StyleSheet(value = "frontend://com/vaadin/flow/uitest/ui/dependencies/lazy.css", loadMode = LoadMode.LAZY)
@JavaScript("frontend://com/vaadin/flow/uitest/ui/dependencies/eager.js")
@StyleSheet("frontend://com/vaadin/flow/uitest/ui/dependencies/eager.css")
public class DependenciesLoadingAnnotationsView
        extends DependenciesLoadingBaseView {
}
