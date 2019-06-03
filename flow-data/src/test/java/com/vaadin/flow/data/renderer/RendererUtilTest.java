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
package com.vaadin.flow.data.renderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.ValueProvider;

public class RendererUtilTest {

    private static class TestUIInternals extends UIInternals {

        private List<PendingJavaScriptInvocation> invocations = new ArrayList<>();

        public TestUIInternals(UI ui) {
            super(ui);
        }

        @Override
        public void addJavaScriptInvocation(
                PendingJavaScriptInvocation invocation) {
            invocations.add(invocation);
        }

    }

    private static class TestUI extends UI {

        private UIInternals internals;

        @Override
        public UIInternals getInternals() {
            if (internals == null) {
                internals = new TestUIInternals(this);
                internals.setAppId("TESTUIinternalsapp");
            }
            return internals;
        }
    }

    @Test
    public void registerEventHandlers_elementsAreAlreadyAttached_setupEvenHandlers() {
        UI ui = new TestUI();

        Element contentTemplate = new Element(Tag.DIV);
        Element templateDataHost = new Element(Tag.SPAN);
        attachElements(ui, contentTemplate, templateDataHost);

        TestUIInternals internals = (TestUIInternals) ui.getInternals();

        Renderer<String> renderer = new Renderer<>();

        renderer.setEventHandler("foo", value -> {
        });

        RendererUtil.registerEventHandlers(renderer, contentTemplate,
                templateDataHost, ValueProvider.identity());

        assertJSExecutions(ui, internals, contentTemplate, templateDataHost);
    }

    @Test
    public void registerEventHandlers_setupEvenHandlersOnAttach() {
        UI ui = new TestUI();
        TestUIInternals internals = (TestUIInternals) ui.getInternals();

        Renderer<String> renderer = new Renderer<>();

        renderer.setEventHandler("foo", value -> {
        });

        Element contentTemplate = new Element(Tag.DIV);
        Element templateDataHost = new Element(Tag.SPAN);

        RendererUtil.registerEventHandlers(renderer, contentTemplate,
                templateDataHost, ValueProvider.identity());

        attachElements(ui, contentTemplate, templateDataHost);
        assertJSExecutions(ui, internals, contentTemplate, templateDataHost);

        ui.getElement().removeAllChildren();
        internals.invocations.clear();

        attachElements(ui, contentTemplate, templateDataHost);
        assertJSExecutions(ui, internals, contentTemplate, templateDataHost);
    }

    private void attachElements(UI ui, Element contentTemplate,
            Element templateDataHost) {
        ui.getElement().appendChild(contentTemplate);
        ui.getElement().appendChild(templateDataHost);
    }

    private void assertJSExecutions(UI ui, TestUIInternals internals,
            Element contentTemplate, Element templateDataHost) {
        internals.getStateTree().runExecutionsBeforeClientResponse();

        Collections.sort(internals.invocations,
                new Comparator<PendingJavaScriptInvocation>() {
                    @Override
                    public int compare(PendingJavaScriptInvocation o1,
                            PendingJavaScriptInvocation o2) {
                        return o1.getInvocation().getExpression()
                                .compareTo(o2.getInvocation().getExpression());
                    }
                });
        Assert.assertEquals(2, internals.invocations.size());

        List<String> expressions = new ArrayList<>();
        expressions.add(
                internals.invocations.get(0).getInvocation().getExpression());
        expressions.add(
                internals.invocations.get(1).getInvocation().getExpression());
        Assert.assertEquals(
                "The javascript executions don't contain dataHost assignement",
                "$0.__dataHost = $1;", expressions.get(0));
        Assert.assertEquals(
                "$0.foo = function(e) {Vaadin.Flow.clients[$1].sendEventMessage("
                        + templateDataHost.getNode().getId()
                        + ", 'foo', {key: e.model ? e.model.__data.item.key : e.target.__dataHost.__data.item.key})}",
                expressions.get(1));
        Assert.assertEquals("TESTUIinternalsapp", internals.invocations.get(1)
                .getInvocation().getParameters().get(1));
    }
}
