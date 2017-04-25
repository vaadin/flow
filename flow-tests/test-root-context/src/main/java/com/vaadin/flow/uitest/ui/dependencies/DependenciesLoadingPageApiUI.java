package com.vaadin.flow.uitest.ui.dependencies;

import static com.vaadin.flow.uitest.ui.dependencies.DependenciesLoadingAnnotationsUI.createTestDiv;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Page;
import com.vaadin.ui.UI;

/**
 * See {@link DependenciesLoadingPageApiUI} for more details about the test.
 * @author Vaadin Ltd.
 * @see DependenciesLoadingPageApiUI
 */
public class DependenciesLoadingPageApiUI extends UI {
    @Override
    protected void init(VaadinRequest request) {
        Page page = getUI().get().getPage();
        page.addJavaScript("/com/vaadin/flow/uitest/ui/dependencies/non-blocking.js", false);
        page.addStyleSheet("/com/vaadin/flow/uitest/ui/dependencies/non-blocking.css", false);
        page.addHtmlImport("/com/vaadin/flow/uitest/ui/dependencies/non-blocking.html", false);
        page.addJavaScript("/com/vaadin/flow/uitest/ui/dependencies/blocking.js");
        page.addStyleSheet("/com/vaadin/flow/uitest/ui/dependencies/blocking.css", true);
        page.addHtmlImport("/com/vaadin/flow/uitest/ui/dependencies/blocking.html", true);

        createTestDiv(this);
    }
}
