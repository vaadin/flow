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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.dependencies.DynamicDependencyView")
public class DynamicDependencyView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (attachEvent.isInitialAttach()) {
            Div div = new Div();
            div.setId("new-component");
            add(div);

            attachEvent.getUI().getPage()
                    .addDynamicImport("return new Promise( "
                            + " function( resolve, regject){ "
                            + "   var div = document.createElement(\"div\");\n"
                            + "     div.setAttribute('id','dep');\n"
                            + "     div.textContent = document.querySelector('#new-component')==null;\n"
                            + "     document.body.appendChild(div);resolve('');}"
                            + ");");
        }
    }
}
