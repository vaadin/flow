package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Page;

/**
 * See {@link DependenciesLoadingPageApiUI} for more details about the test.
 * @author Vaadin Ltd.
 * @see DependenciesLoadingPageApiUI
 */
public class DependenciesLoadingPageApiUI extends DependenciesLoadingAnnotationsUI {
    @Override
    protected void init(VaadinRequest request) {
        super.init(request);

        Page page = getUI().get().getPage();
        page.addJavaScript("/com/vaadin/flow/uitest/ui/dependencies/non-blocking.js", false);
        page.addStyleSheet("/com/vaadin/flow/uitest/ui/dependencies/non-blocking.css", false);
        page.addHtmlImport("/com/vaadin/flow/uitest/ui/dependencies/non-blocking.html", false);
        page.addJavaScript("/com/vaadin/flow/uitest/ui/dependencies/blocking.js");
        page.addStyleSheet("/com/vaadin/flow/uitest/ui/dependencies/blocking.css");
        page.addHtmlImport("/com/vaadin/flow/uitest/ui/dependencies/blocking.html");
    }
}
