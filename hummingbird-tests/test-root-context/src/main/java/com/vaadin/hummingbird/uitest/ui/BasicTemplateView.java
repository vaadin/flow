/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.namespace.TemplateNamespace;
import com.vaadin.hummingbird.template.TemplateNode;
import com.vaadin.hummingbird.template.TemplateParser;

public class BasicTemplateView extends AbstractDivView {

    private static final TemplateNode templateNode = TemplateParser
            .parse("<div id=bar>baz<input></div>");

    @Override
    protected void onShow() {
        StateNode stateNode = new StateNode(TemplateNamespace.class);
        stateNode.getNamespace(TemplateNamespace.class)
                .setRootTemplate(templateNode);

        Element templateElement = Element.get(stateNode);

        getElement().appendChild(templateElement);
    }

}
