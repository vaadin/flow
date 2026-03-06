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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.StylesheetLiveReloadView", layout = ViewTestLayout.class)
@StyleSheet("context://css/view/view.css")
@StyleSheet("context://css/view/for-deletion.css")
public class StylesheetLiveReloadView extends AbstractLiveReloadView {

    public StylesheetLiveReloadView() {
        add(makeDiv("appshell-style", "css/styles.css"));
        add(makeDiv("appshell-imported", "css/imported.css"));
        add(makeDiv("appshell-nested-imported",
                "css/nested/nested-imported.css"));
        add(makeDiv("appshell-image", "css/images/gobo.png"));
        add(makeDiv("view-style", "css/view/view.css"));
        add(makeDiv("view-imported", "css/view/imported.css"));
        add(makeDiv("view-nested-imported",
                "css/view/nested/nested-imported.css"));
        add(makeDiv("view-image", "css/images/viking.png"));
        add(makeDivForDelete());
    }

    private Div makeDiv(String id, String resourceFilePath) {
        Div div = new Div();
        div.setId(id);
        div.setText("Style defined in " + resourceFilePath);
        div.addClassName(id);
        div.getElement().setAttribute("test-resource-file-path",
                resourceFilePath);
        return div;
    }

    private Div makeDivForDelete() {
        Div div = new Div();
        div.setId("view-style-deleted");
        div.setText("Style defined in css/view/for-deletion.css (delete test)");
        div.addClassName("view-style-deleted");
        div.getElement().setAttribute("test-resource-file-path",
                "css/view/for-deletion.css");
        return div;
    }
}
