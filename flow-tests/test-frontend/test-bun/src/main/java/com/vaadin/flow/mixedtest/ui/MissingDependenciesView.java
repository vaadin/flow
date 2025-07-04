/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.mixedtest.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/*
 * View for manual testing. Automatic end-to-end testing is not possible since
 * the only difference should be in the server log.
 */
@Route
public class MissingDependenciesView extends Div {
    /**
     * Class that has a frontend dependency annotation that won't be noticed
     * because this class is only referenced using reflection.
     */
    @Tag("div")
    @JsModule("./my-component.js")
    public static class Unreferenced extends Component {
        public Unreferenced() {
            getElement().setText("I'm not directly referenced from bytecode");
        }
    }

    public MissingDependenciesView() {
        try {
            Component unreferenced = (Component) Class.forName(
                    "com.vaadin.flow.mixedtest.ui.MissingDependenciesView$Unreferenced")
                    .getDeclaredConstructor().newInstance();

            // Uncomment to test behavior when the component is referenced
            // new Unreferenced();

            add(unreferenced);
        } catch (Exception e) {
            e.printStackTrace();

            add(new Span("Could not create unreferenced component instance: "
                    + e.getMessage()));
        }
    }
}
