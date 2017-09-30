package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.ui.Page;

/**
 * See {@link DependenciesLoadingAnnotationsUI} for more details about the test.
 * 
 * @author Vaadin Ltd.
 * @see DependenciesLoadingAnnotationsUI
 */
public class DependenciesLoadingPageApiUI extends DependenciesLoadingBaseUI {
    @Override
    protected void init(VaadinRequest request) {
        super.init(request);

        Page page = getUI().get().getPage();
        page.addJavaScript(
                "frontend:///com/vaadin/flow/uitest/ui/dependencies/inline.js",
                LoadMode.INLINE);
        page.addStyleSheet(
                "frontend:///com/vaadin/flow/uitest/ui/dependencies/inline.css",
                LoadMode.INLINE);
        page.addHtmlImport(
                "frontend:///com/vaadin/flow/uitest/ui/dependencies/inline.html",
                LoadMode.INLINE);
        page.addJavaScript(
                "frontend:///com/vaadin/flow/uitest/ui/dependencies/lazy.js",
                LoadMode.LAZY);
        page.addStyleSheet(
                "frontend:///com/vaadin/flow/uitest/ui/dependencies/lazy.css",
                LoadMode.LAZY);
        page.addHtmlImport(
                "frontend:///com/vaadin/flow/uitest/ui/dependencies/lazy.html",
                LoadMode.LAZY);
        page.addJavaScript(
                "frontend:///com/vaadin/flow/uitest/ui/dependencies/eager.js");
        page.addStyleSheet(
                "frontend:///com/vaadin/flow/uitest/ui/dependencies/eager.css");
        page.addHtmlImport(
                "frontend:///com/vaadin/flow/uitest/ui/dependencies/eager.html");
    }
}
