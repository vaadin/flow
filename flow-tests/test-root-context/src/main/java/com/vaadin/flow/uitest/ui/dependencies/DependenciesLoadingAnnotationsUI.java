package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;

/**
 * See corresponding IT for more details.
 *
 * @author Vaadin Ltd.
 */
@JavaScript(value = "/com/vaadin/flow/uitest/ui/dependencies/non-blocking.js", blocking = false)
@StyleSheet(value = "/com/vaadin/flow/uitest/ui/dependencies/non-blocking.css", blocking = false)
@HtmlImport(value = "/com/vaadin/flow/uitest/ui/dependencies/non-blocking.html", blocking = false)
@JavaScript("/com/vaadin/flow/uitest/ui/dependencies/blocking.js")
@StyleSheet("/com/vaadin/flow/uitest/ui/dependencies/blocking.css")
@HtmlImport("/com/vaadin/flow/uitest/ui/dependencies/blocking.html")
public class DependenciesLoadingAnnotationsUI extends DependenciesLoadingBaseUI {
}
