/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
