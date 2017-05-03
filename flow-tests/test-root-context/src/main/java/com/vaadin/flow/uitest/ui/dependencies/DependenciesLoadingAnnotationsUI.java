package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.flow.html.Div;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

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
public class DependenciesLoadingAnnotationsUI extends UI {
    static final String PRELOADED_DIV_ID = "preloadedDiv";

    @Override
    protected void init(VaadinRequest request) {
        Div div = new Div();
        div.setId(PRELOADED_DIV_ID);
        div.setText("Preloaded div");
        add(div);
    }
}
