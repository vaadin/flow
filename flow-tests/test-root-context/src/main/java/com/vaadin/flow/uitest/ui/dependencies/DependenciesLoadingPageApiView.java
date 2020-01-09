package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * See {@link DependenciesLoadingAnnotationsView} for more details about the
 * test.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 * @see DependenciesLoadingAnnotationsView
 */
@Route("com.vaadin.flow.uitest.ui.dependencies.DependenciesLoadingPageApiView")
public class DependenciesLoadingPageApiView
        extends DependenciesLoadingBaseView {

    public DependenciesLoadingPageApiView() {
        super();
        Page page = UI.getCurrent().getPage();
        page.addJavaScript(
                "frontend://com/vaadin/flow/uitest/ui/dependencies/inline.js",
                LoadMode.INLINE);
        page.addStyleSheet(
                "frontend://com/vaadin/flow/uitest/ui/dependencies/inline.css",
                LoadMode.INLINE);
        // TODO
        // page.addHtmlImport(
        // "frontend://com/vaadin/flow/uitest/ui/dependencies/inline.html",
        // LoadMode.INLINE);
        page.addJavaScript(
                "frontend://com/vaadin/flow/uitest/ui/dependencies/lazy.js",
                LoadMode.LAZY);
        page.addStyleSheet(
                "frontend://com/vaadin/flow/uitest/ui/dependencies/lazy.css",
                LoadMode.LAZY);
        // TODO
        // page.addHtmlImport(
        // "frontend://com/vaadin/flow/uitest/ui/dependencies/lazy.html",
        // LoadMode.LAZY);
        page.addJavaScript(
                "frontend://com/vaadin/flow/uitest/ui/dependencies/eager.js");
        page.addStyleSheet(
                "frontend://com/vaadin/flow/uitest/ui/dependencies/eager.css");
        // TODO
        // page.addHtmlImport(
        // "frontend://com/vaadin/flow/uitest/ui/dependencies/eager.html");
    }
}
