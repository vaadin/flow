/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.test.dependencies;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;

/**
 * Test base for the views that check that dependencies are loaded correctly.
 */
public class DependenciesLoadingBaseView extends Div {

    static final String PRELOADED_DIV_ID = "preloadedDiv";
    static final String INLINE_CSS_TEST_DIV_ID = "inlineCssTestDiv";
    static final String DOM_CHANGE_TEXT = "I appear after inline and eager dependencies and before lazy";

    protected DependenciesLoadingBaseView() {
        add(createDiv(PRELOADED_DIV_ID, "Preloaded div"), createDiv(
                INLINE_CSS_TEST_DIV_ID, "A div for testing inline css"));
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
        attachEvent.getUI().getPage().executeJs("window.attachTestDiv($0)",
                DOM_CHANGE_TEXT);
    }

}
