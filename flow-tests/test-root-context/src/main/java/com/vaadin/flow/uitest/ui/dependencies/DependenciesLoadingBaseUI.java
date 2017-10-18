package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.ui.html.Div;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;
import com.vaadin.ui.event.AttachEvent;

/**
 * This class provides test base for IT test that check dependencies being
 * loaded correctly.
 *
 * @author Vaadin Ltd.
 * @see DependenciesLoadingAnnotationsUI
 */
class DependenciesLoadingBaseUI extends UI {
    static final String PRELOADED_DIV_ID = "preloadedDiv";
    static final String INLINE_CSS_TEST_DIV_ID = "inlineCssTestDiv";
    static final String DOM_CHANGE_TEXT = "I appear after inline and eager dependencies and before lazy";

    @Override
    protected void init(VaadinRequest request) {
        add(
            createDiv(PRELOADED_DIV_ID, "Preloaded div"),
            createDiv(INLINE_CSS_TEST_DIV_ID, "A div for testing inline css")
        );
    }

    private Div createDiv(String id, String text) {
        Div div = new Div();
        div.setId(id);
        div.setText(text);
        return div;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // See eager.js for attachTestDiv code
        getPage().executeJavaScript("attachTestDiv($0)", DOM_CHANGE_TEXT);
    }
}
