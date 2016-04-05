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
import com.vaadin.hummingbird.template.ElementTemplateBuilder;
import com.vaadin.hummingbird.template.StaticBinding;
import com.vaadin.hummingbird.template.TemplateNode;
import com.vaadin.hummingbird.template.TextTemplateBuilder;

public class BasicTemplateView extends AbstractDivView {

    private static final TemplateNode templateNode;
    static {
        // <div id=bar>baz</div>
        // Should be parsed from a template once a parser is implemented
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .addAttribute("id", new StaticBinding("bar"))
                .addChild(new TextTemplateBuilder(new StaticBinding("baz")));
        templateNode = builder.build();
        templateNode.init(null);
    }

    @Override
    protected void onShow() {
        StateNode stateNode = new StateNode(TemplateNamespace.class);
        stateNode.getNamespace(TemplateNamespace.class)
                .setRootTemplate(templateNode);

        Element templateElement = Element.get(stateNode);

        getElement().appendChild(templateElement);
    }

}
