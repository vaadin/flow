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
package com.vaadin.flow.uitest.ui.dependencies;

import java.io.Serializable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.uitest.ui.AbstractDivView;

/**
 * This class provides test base for IT test that check dependencies being
 * loaded correctly.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class DependenciesLoadingBaseView extends AbstractDivView {

    @JsDefinition
    public interface AttachTestDivJs extends Serializable {

        // See eager.js for attachTestDiv code
        @JsExpression("window.attachTestDiv($0)")
        void attachTestDiv(String text);
    }

    static final String PRELOADED_DIV_ID = "preloadedDiv";
    static final String INLINE_CSS_TEST_DIV_ID = "inlineCssTestDiv";
    static final String DOM_CHANGE_TEXT = "I appear after inline and eager dependencies and before lazy";

    protected DependenciesLoadingBaseView(String cssSuffix) {
        add(createDiv(PRELOADED_DIV_ID + cssSuffix, "Preloaded div"),
                createDiv(INLINE_CSS_TEST_DIV_ID + cssSuffix,
                        "A div for testing inline css"));
    }

    private Div createDiv(String id, String text) {
        Div div = new Div();
        div.setId(id);
        div.setText(text);
        return div;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getPage().executeJs(AttachTestDivJs.class)
                .attachTestDiv(DOM_CHANGE_TEXT);
    }

}
