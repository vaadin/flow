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

package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.internal.ExportsWebComponent;
import com.vaadin.flow.component.webcomponent.WebComponent;

public class InterfaceBasedExporter implements ExportsWebComponent<InterfaceBasedExporter.InterfaceBasedComponent> {
    @Override
    public String getTag() {
        return "interface-based";
    }

    @Override
    public Class<InterfaceBasedComponent> getComponentClass() {
        return InterfaceBasedComponent.class;
    }

    @Override
    public void configure(WebComponent<InterfaceBasedComponent> webComponent, InterfaceBasedComponent component) {

    }

    @Override
    public void preConfigure() {

    }

    @Override
    public void postConfigure() {

    }

    public static class InterfaceBasedComponent extends Div {
        public InterfaceBasedComponent() {
            Paragraph paragraph = new Paragraph("Hello world");
            paragraph.setId("paragraph");
            add(paragraph);
        }
    }
}
