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
        super("WebRes");
        Page page = UI.getCurrent().getPage();
        page.addJavaScript("/dependencies/inline.js", LoadMode.INLINE);
        page.addStyleSheet("/dependencies/inline.css", LoadMode.INLINE);
        page.addJavaScript("/dependencies/lazy.js", LoadMode.LAZY);
        page.addStyleSheet("/dependencies/lazy.css", LoadMode.LAZY);
        page.addJavaScript("/dependencies/eager.js");
        page.addStyleSheet("/dependencies/eager.css");
        page.addJsModule("/dependencies/eager-module.js");
    }

}
