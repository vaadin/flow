/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.dependencies.DynamicDependencyView")
public class DynamicDependencyView extends Div {
    private final Div newComponent = new Div();

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (attachEvent.isInitialAttach()) {
            newComponent.setId("new-component");
            add(newComponent);

            attachEvent.getUI().getPage()
                    .addDynamicImport("return new Promise( "
                            + " function( resolve, reject){ "
                            + "   var div = document.createElement(\"div\");\n"
                            + "     div.setAttribute('id','dep');\n"
                            + "     div.textContent = document.querySelector('#new-component')==null;\n"
                            + "     document.body.appendChild(div);resolve('');}"
                            + ");");

            add(createLoadButton("nopromise", "Load non-promise dependency",
                    "document.querySelector('#new-component').textContent = 'import has been run'"));
            add(createLoadButton("throw", "Load throwing dependency",
                    "throw Error('Throw on purpose')"));
            add(createLoadButton("reject", "Load rejecting dependency",
                    "return new Promise(function(resolve, reject) { reject(Error('Reject on purpose')); });"));
        }
    }

    private NativeButton createLoadButton(String id, String name,
            String expression) {
        NativeButton button = new NativeButton(name, event -> {
            UI.getCurrent().getPage().addDynamicImport(expression);
            newComponent.setText("Div updated for " + id);
        });
        button.setId(id);
        return button;
    }
}
