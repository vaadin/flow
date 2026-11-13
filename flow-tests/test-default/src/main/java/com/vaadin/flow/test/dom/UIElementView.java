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
package com.vaadin.flow.test.dom;

import java.io.Serializable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.router.Route;

@Route("u-i-element")
public class UIElementView extends Div {

    @JsDefinition
    public interface ChildJs extends Serializable {

        @JsExpression("""
                var newElement = document.createElement('div');
                newElement.className='body-child';
                this.appendChild(newElement);
                """)
        void appendChild();
    }

    public UIElementView() {
        attachElement();

        NativeButton attachElement = new NativeButton("Attach Element via JS",
                event -> attachElement());
        add(attachElement);
    }

    private void attachElement() {
        getElement().executeJs(ChildJs.class).appendChild();
    }
}
