package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.uitest.ui.AbstractDivView;
import com.vaadin.ui.event.AttachEvent;
import com.vaadin.ui.html.Div;

/**
 * This class provides test base for IT test that check dependencies being
 * loaded correctly.
 *
 * @author Vaadin Ltd.
 * @see DependenciesLoadingAnnotationsView
 */
class DependenciesLoadingBaseView extends AbstractDivView {
    static final String PRELOADED_DIV_ID = "preloadedDiv";
    static final String INLINE_CSS_TEST_DIV_ID = "inlineCssTestDiv";
    static final String DOM_CHANGE_TEXT = "I appear after inline and eager dependencies and before lazy";

    protected DependenciesLoadingBaseView() {
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
