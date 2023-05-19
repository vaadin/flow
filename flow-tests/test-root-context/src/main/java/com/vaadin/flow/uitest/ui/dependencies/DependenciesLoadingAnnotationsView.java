package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
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
@JavaScript(value = "./dependencies/inline.js", loadMode = LoadMode.INLINE)
@StyleSheet(value = "./dependencies/inline.css", loadMode = LoadMode.INLINE)
@JavaScript(value = "./dependencies/lazy.js", loadMode = LoadMode.LAZY)
@StyleSheet(value = "./dependencies/lazy.css", loadMode = LoadMode.LAZY)
@JavaScript("./dependencies/eager.js")
@StyleSheet("./dependencies/eager.css")
@JsModule("./dependencies/eager-module.js")
public class DependenciesLoadingAnnotationsView
        extends DependenciesLoadingBaseView {

    public DependenciesLoadingAnnotationsView() {
        super("");
    }
}
