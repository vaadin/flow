/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.template.angular;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.TemplateElementStateProviderTest;
import com.vaadin.flow.dom.TemplateElementStateProviderTest.NullTemplateResolver;
import com.vaadin.flow.dom.impl.TemplateElementStateProvider;
import com.vaadin.flow.nodefeature.ModelList;
import com.vaadin.flow.nodefeature.ModelMap;
import com.vaadin.flow.nodefeature.TemplateMap;
import com.vaadin.flow.template.angular.parser.TemplateParser;
import com.vaadin.flow.template.angular.parser.TemplateResolver;
import com.vaadin.ui.AngularTemplate;

public class AngularTemplateNodeTest {

    private TemplateResolver nullTemplateResolver = new NullTemplateResolver();

    private TemplateNode parse(String html) {
        return TemplateParser.parse(html, nullTemplateResolver);
    }

    private Element getElement(TemplateNode templateNode) {
        return TemplateElementStateProviderTest.createElement(templateNode);
    }

    @Test
    public void singleElementLookup() {
        TemplateNode templateNode = parse("<div id='foo'><div>");
        Element div = getElement(templateNode);
        Element foundDiv = templateNode.findElement(div.getNode(), "foo").get();
        assertEquals(div, foundDiv);
    }

    @Test
    public void unknownElementLookup() {
        TemplateNode templateNode = parse("<div><div>");
        assertEquals(Optional.empty(), templateNode.findElement(
                TemplateElementStateProvider.createRootNode(), "foo"));
    }

    @Test
    public void childElementLookup() {
        TemplateNode templateNode = parse("<span><div id='foo'><div></span>");
        Element div = getElement(templateNode).getChild(0);
        Element foundDiv = templateNode.findElement(div.getNode(), "foo").get();
        assertEquals(div, foundDiv);
    }

    @Test
    public void forLoopElement() {
        TemplateNode templateNode = parse(
                "<span><div *ngFor='let item of items' id='foo'><div></span>");

        StateNode node = TemplateElementStateProvider.createRootNode();
        StateNode listNode = new StateNode(ModelList.class);
        ModelMap.get(node).setValue("items", listNode);
        StateNode item1 = TemplateElementStateProvider
                .createSubModelNode(ModelMap.class);
        StateNode item2 = TemplateElementStateProvider
                .createSubModelNode(ModelMap.class);
        listNode.getFeature(ModelList.class).add(item1);
        listNode.getFeature(ModelList.class).add(item2);

        assertEquals(Optional.empty(),
                templateNode.findElement(node, "foo"));
    }

    @HtmlTemplate("main.html")
    public static class MainWithSubTemplate extends AngularTemplate {
    }

    @Test
    public void elementInIncludedFile() {
        MainWithSubTemplate template = new MainWithSubTemplate();
        StateNode stateNode = template.getElement().getNode();
        TemplateNode templateNode = stateNode.getFeature(TemplateMap.class)
                .getRootTemplate();

        assertEquals("<span id=\"main\">Main template</span>",
                templateNode.findElement(stateNode, "main").get()
                        .getOuterHTML());
        assertEquals("<span id=\"sub\">Sub template</span>", templateNode
                .findElement(stateNode, "sub").get().getOuterHTML());
    }

}
