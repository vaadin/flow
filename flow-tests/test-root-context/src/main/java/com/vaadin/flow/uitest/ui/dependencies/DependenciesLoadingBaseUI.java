package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.html.Div;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.AttachEvent;
import com.vaadin.ui.UI;

/**
 * This class provides test base for IT test that check dependencies being loaded correctly.
 *
 * @author Vaadin Ltd.
 * @see DependenciesLoadingAnnotationsUI
 */
class DependenciesLoadingBaseUI extends UI {
    static final String PRELOADED_DIV_ID = "preloadedDiv";
    static final String DOM_CHANGE_TEXT = "I appear after blocking dependencies and before non-blocking";

    @Override
    protected void init(VaadinRequest request) {
        Div div = new Div();
        div.setId(PRELOADED_DIV_ID);
        div.setText("Preloaded div");
        add(div);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // See blocking.js for attachTestDiv code
        getPage().executeJavaScript(
                "attachTestDiv($0)",
                DOM_CHANGE_TEXT);
    }
}
