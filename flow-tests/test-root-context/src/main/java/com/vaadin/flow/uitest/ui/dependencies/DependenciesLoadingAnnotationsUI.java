package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.shared.ui.LoadMode;

/**
 * See corresponding IT for more details.
 *
 * @author Vaadin Ltd.
 */
@JavaScript(value = "/com/vaadin/flow/uitest/ui/dependencies/inline.js", loadMode = LoadMode.INLINE)
@StyleSheet(value = "/com/vaadin/flow/uitest/ui/dependencies/inline.css", loadMode = LoadMode.INLINE)
@HtmlImport(value = "/com/vaadin/flow/uitest/ui/dependencies/inline.html", loadMode = LoadMode.INLINE)
@JavaScript(value = "/com/vaadin/flow/uitest/ui/dependencies/lazy.js", loadMode = LoadMode.LAZY)
@StyleSheet(value = "/com/vaadin/flow/uitest/ui/dependencies/lazy.css", loadMode = LoadMode.LAZY)
@HtmlImport(value = "/com/vaadin/flow/uitest/ui/dependencies/lazy.html", loadMode = LoadMode.LAZY)
@JavaScript("/com/vaadin/flow/uitest/ui/dependencies/eager.js")
@StyleSheet("/com/vaadin/flow/uitest/ui/dependencies/eager.css")
@HtmlImport("/com/vaadin/flow/uitest/ui/dependencies/eager.html")
public class DependenciesLoadingAnnotationsUI extends DependenciesLoadingBaseUI {
}
