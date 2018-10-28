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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.component.page.Page.ExecutionCanceler;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.ValueProvider;

public class RendererUtilTest {

    private static class TestUIInternals extends UIInternals {

        private List<JavaScriptInvocation> invocations = new ArrayList<>();

        public TestUIInternals(UI ui) {
            super(ui);
        }

        @Override
        public ExecutionCanceler addJavaScriptInvocation(
                JavaScriptInvocation invocation) {
            invocations.add(invocation);
            return () -> true;
        }

    }

    private static class TestUI extends UI {

        private UIInternals internals;

        @Override
        public UIInternals getInternals() {
            if (internals == null) {
                internals = new TestUIInternals(this);
            }
            return internals;
        }
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

        assertJSExecutions(ui, internals, contentTemplate, templateDataHost);

        ui.getElement().removeAllChildren();
        internals.invocations.clear();

        assertJSExecutions(ui, internals, contentTemplate, templateDataHost);
    }

    private void assertJSExecutions(UI ui, TestUIInternals internals,
            Element contentTemplate, Element templateDataHost) {
        ui.getElement().appendChild(contentTemplate);
        ui.getElement().appendChild(templateDataHost);

        internals.getStateTree().runExecutionsBeforeClientResponse();

        Assert.assertEquals(2, internals.invocations.size());

        JavaScriptInvocation invocation = internals.invocations.get(0);

        Set<String> expressons = new HashSet<>();
        expressons.add(invocation.getExpression());
        expressons.add(internals.invocations.get(1).getExpression());

        Assert.assertTrue(
                "The javascript executions don't contain dataHost assignement",
                expressons.remove("$0.__dataHost = $1;"));
        Assert.assertEquals(
                "$0.foo = function(e) {Vaadin.Flow.sendEventMessage("
                        + templateDataHost.getNode().getId()
                        + ", 'foo', {key: e.model ? e.model.__data.item.key : e.target.__dataHost.__data.item.key})}",
                expressons.iterator().next());
    }
}
