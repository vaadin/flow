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

package com.vaadin.flow.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.annotations.Id;
import com.vaadin.annotations.Tag;
import com.vaadin.external.jsoup.Jsoup;
import com.vaadin.external.jsoup.nodes.Element;
import com.vaadin.flow.StateNode;
import com.vaadin.flow.nodefeature.AttachTemplateChildFeature;
import com.vaadin.flow.nodefeature.ElementData;
import com.vaadin.flow.nodefeature.ElementPropertyMap;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.server.CustomElementRegistry;
import com.vaadin.server.CustomElementRegistryAccess;
import com.vaadin.ui.Component;
import com.vaadin.ui.Page;
import com.vaadin.ui.UI;

import elemental.json.JsonArray;

/**
 * @author Vaadin Ltd.
 */
public class PolymerTemplateTest {
    private static final String TAG = "FFS";

    private static class TestPage extends Page {

        private List<Serializable[]> params = new ArrayList<>();

        public TestPage() {
            super(Mockito.mock(UI.class));
        }

        @Override
        public ExecutionCanceler executeJavaScript(String expression,
                Serializable... parameters) {
            params.add(parameters);
            return null;
        }

    }

    public interface ModelClass extends TemplateModel {
        void setMessage(String message);

        void setTitle(String title);

        String getMessage();

        String getTitle();
    }

    @Tag(Tag.DIV)
    public static class CustomComponent extends Component {

    }

    private static class TestTemplateParser implements TemplateParser {
        @Override
        public Element getTemplateContent(
                Class<? extends PolymerTemplate> clazz, String tag) {
            return Jsoup.parse("<dom-module id='" + tag + "'></dom-module>");
        }
    }

    @Tag(TAG)
    public static class TestPolymerTemplate
            extends PolymerTemplate<ModelClass> {
        public TestPolymerTemplate() {
            super(new TestTemplateParser());
        }
    }

    @Tag(TAG)
    private static class IdChildTemplate extends PolymerTemplate<ModelClass> {

        @Id("child")
        private CustomComponent child;

        public IdChildTemplate() {
            this((clazz, tag) -> Jsoup.parse("<dom-module id='" + tag
                    + "'><template><div id='child'></template></dom-module>"));
        }

        IdChildTemplate(TemplateParser parser) {
            super(parser);
        }

    }

    @Tag("child-template")
    public static class TemplateChild extends PolymerTemplate<ModelClass> {
        public TemplateChild() {
            super(new TestTemplateParser());
        }
    }

    @Tag("parent-templte")
    private static class TemplateInTemplate
            extends PolymerTemplate<ModelClass> {

        public TemplateInTemplate() {
            super((clazz, tag) -> Jsoup.parse("<dom-module id='" + tag
                    + "'><template><div><ffs></div><span></span><child-template></template></dom-module>"));
        }

    }

    @Tag("parent-templte")
    private static class TemplateWithChildInDomRepeat
            extends PolymerTemplate<ModelClass> {

        public TemplateWithChildInDomRepeat() {
            super((clazz, tag) -> Jsoup
                    .parse("<dom-module id='" + tag + "'><template><div>"
                            + "<dom-repeat items='[[messages]]'><template><child-template></template></dom-repeat>"
                            + "</div></template></dom-module>"));
        }

    }

    @Tag("parent-templte")
    private static class TemplateWithDomRepeat
            extends PolymerTemplate<ModelClass> {

        public TemplateWithDomRepeat() {
            super((clazz,
                    tag) -> Jsoup.parse("<dom-module id='" + tag
                            + "'><template><child-template>"
                            + "<dom-repeat items='[[messages]]'><template><div></template></dom-repeat>"
                            + "</template></dom-module>"));
        }

    }

    @Tag(TAG)
    private static class IdElementTemplate extends PolymerTemplate<ModelClass> {

        @Id("labelId")
        private com.vaadin.flow.dom.Element label;

        public IdElementTemplate() {
            this((clazz, tag) -> Jsoup.parse("<dom-module id='" + tag
                    + "'><label id='labelId'></dom-module>"));
        }

        IdElementTemplate(TemplateParser parser) {
            super(parser);
        }

    }

    private static class IdWrongElementTemplate extends IdElementTemplate {

        public IdWrongElementTemplate() {
            super((clazz, tag) -> Jsoup.parse("<dom-module id='" + tag
                    + "'><div id='foo'></dom-module>"));
        }

    }

    private static class IdWrongChildTemplate extends IdChildTemplate {

        public IdWrongChildTemplate() {
            super((clazz, tag) -> Jsoup.parse("<dom-module id='" + tag
                    + "'><div id='foo'></dom-module>"));
        }

    }

    private static class IdWrongTagChildTemplate extends IdChildTemplate {

        public IdWrongTagChildTemplate() {
            super((clazz, tag) -> Jsoup.parse("<dom-module id='" + tag
                    + "'><label id='child'></dom-module>"));
        }

    }

    private static class TemplateWithoutTagAnnotation
            extends PolymerTemplate<ModelClass> {
    }

    @Before
    public void setUp() {
        CustomElementRegistryAccess.resetRegistry();
        Map<String, Class<? extends Component>> map = new HashMap<>();
        map.put("child-template", TemplateChild.class);
        map.put("ffs", TestPolymerTemplate.class);
        CustomElementRegistry.getInstance().setCustomElements(map);
    }

    @Tag(TAG)
    private static class NoModelTemplate extends PolymerTemplate {

        NoModelTemplate() {
            super(new TestTemplateParser());
        }

    }

    @Test
    public void tagIsCorrect() {
        TestPolymerTemplate template = new TestPolymerTemplate();

        assertEquals(TAG, template.getElement().getTag());
    }

    @Test
    public void stateNodeIsInitialised() {
        TestPolymerTemplate template = new TestPolymerTemplate();
        StateNode stateNode = template.getStateNode();

        Map<String, Object> expectedState = new HashMap<>();
        expectedState.put("message", null);
        expectedState.put("title", null);

        assertTrue(stateNode.hasFeature(ElementPropertyMap.class));
        ElementPropertyMap modelMap = stateNode
                .getFeature(ElementPropertyMap.class);
        modelMap.getPropertyNames().forEach(key -> {
            assertTrue(expectedState.containsKey(key));
            assertEquals(expectedState.get(key), modelMap.getProperty(key));
        });
    }

    @Test
    public void updateOneOfModelValues() {
        String message = "message";
        TestPolymerTemplate template = new TestPolymerTemplate();
        ModelClass model = template.getModel();
        StateNode stateNode = template.getStateNode();

        model.setMessage(message);

        assertEquals(message, model.getMessage());
        assertNull(model.getTitle());

        Map<String, Object> expectedState = new HashMap<>();
        expectedState.put("message", message);
        expectedState.put("title", null);

        ElementPropertyMap modelMap = stateNode
                .getFeature(ElementPropertyMap.class);
        modelMap.getPropertyNames().forEach(key -> {
            assertTrue(expectedState.containsKey(key));
            assertEquals(expectedState.get(key), modelMap.getProperty(key));
        });
    }

    @Test(expected = IllegalStateException.class)
    public void noAnnotationTemplate() {
        new TemplateWithoutTagAnnotation();
    }

    @Test(expected = IllegalStateException.class)
    public void noModelTemplate() {
        new NoModelTemplate();
    }

    @Test
    public void parseTemplate_hasIdChild_childIsRegisteredInFeature() {
        IdChildTemplate template = new IdChildTemplate();

        UI ui = new UI();
        ui.add(template);

        AttachTemplateChildFeature feature = template.getElement().getNode()
                .getFeature(AttachTemplateChildFeature.class);
        AtomicInteger counter = new AtomicInteger(0);
        feature.forEachChild(child -> counter.incrementAndGet());
        Assert.assertEquals(1, counter.get());
    }

    @Test(expected = IllegalStateException.class)
    public void parseTemplate_hasWrongIdChild_exceptionIsThrown() {
        new IdWrongChildTemplate();
    }

    @Test(expected = IllegalStateException.class)
    public void parseTemplate_hasWrongIdChildElement_exceptionIsThrown() {
        new IdWrongElementTemplate();
    }

    @Test
    public void parseTemplate_hasChildTemplate_elemetsAreCreatedAndRequestIsSent() {
        TemplateInTemplate template = new TemplateInTemplate();

        TestPage page = setupUI(template);

        AttachTemplateChildFeature feature = template.getStateNode()
                .getFeature(AttachTemplateChildFeature.class);
        List<StateNode> templateNodes = new ArrayList<>();
        feature.forEachChild(templateNodes::add);

        Assert.assertEquals(2, templateNodes.size());
        StateNode child1 = templateNodes.get(0);
        StateNode child2 = templateNodes.get(1);
        String tag = child1.getFeature(ElementData.class).getTag();
        if ("child-template".equals(tag)) {
            Assert.assertEquals("ffs",
                    child2.getFeature(ElementData.class).getTag());
        } else {
            Assert.assertEquals("ffs",
                    child1.getFeature(ElementData.class).getTag());
            Assert.assertEquals("child-template",
                    child2.getFeature(ElementData.class).getTag());
        }

        Set<Object> paths = new HashSet<>();
        paths.add(convertIntArray((JsonArray) page.params.get(0)[3]));
        paths.add(convertIntArray((JsonArray) page.params.get(1)[3]));

        // check arrays of indices
        Assert.assertTrue(paths.contains(Arrays.asList(0, 0)));
        Assert.assertTrue(paths.contains(Arrays.asList(2)));
    }

    @Test(expected = IllegalStateException.class)
    public void parseTemplte_hasChildTemplateInsideDomRepeat_cantParse() {
        new TemplateWithChildInDomRepeat();
    }

    @Test
    public void parseTemplte_hasChildTemplateOutsideDomRepeat_elementIsCreated() {
        TemplateWithDomRepeat template = new TemplateWithDomRepeat();

        UI ui = new UI();
        ui.add(template);

        AttachTemplateChildFeature feature = template.getStateNode()
                .getFeature(AttachTemplateChildFeature.class);
        List<StateNode> templateNodes = new ArrayList<>();
        feature.forEachChild(templateNodes::add);

        Assert.assertEquals(1, templateNodes.size());
    }

    @Test(expected = IllegalStateException.class)
    public void injectIdComponent_wrongTag_throw() {
        new IdWrongTagChildTemplate();
    }

    @Test(expected = IllegalStateException.class)
    public void injectIdElement_wrongTag_throw() {
        new IdWrongElementTemplate();
    }

    @Test
    public void attachExistingElement_elementIsCreatedAndRequestIsSent() {
        IdElementTemplate template = new IdElementTemplate();

        TestPage page = setupUI(template);

        AttachTemplateChildFeature feature = template.getStateNode()
                .getFeature(AttachTemplateChildFeature.class);
        List<StateNode> templateNodes = new ArrayList<>();
        feature.forEachChild(templateNodes::add);

        Assert.assertEquals(1, templateNodes.size());

        StateNode child = templateNodes.get(0);
        String tag = child.getFeature(ElementData.class).getTag();
        Assert.assertEquals("label", tag);

        // check id in the JS call request
        Assert.assertEquals("labelId", page.params.get(0)[3]);

        Assert.assertNotNull(template.label);
        Assert.assertEquals(child, template.label.getNode());
    }

    @Test
    public void attachExistingComponent_elementIsCreatedAndRequestIsSent() {
        IdChildTemplate template = new IdChildTemplate();

        TestPage page = setupUI(template);

        AttachTemplateChildFeature feature = template.getStateNode()
                .getFeature(AttachTemplateChildFeature.class);
        List<StateNode> templateNodes = new ArrayList<>();
        feature.forEachChild(templateNodes::add);

        Assert.assertEquals(1, templateNodes.size());

        StateNode child = templateNodes.get(0);
        String tag = child.getFeature(ElementData.class).getTag();
        Assert.assertEquals("div", tag);

        // check id in the JS call request
        Assert.assertEquals("child", page.params.get(0)[3]);

        Assert.assertNotNull(template.child);
        Assert.assertEquals(child, template.child.getElement().getNode());

        Assert.assertTrue(
                template.child.getElement().getComponent().isPresent());

        Assert.assertTrue(template.child.getElement().getComponent()
                .get() instanceof CustomComponent);

        Assert.assertEquals(template.child,
                template.child.getElement().getComponent().get());
    }

    private List<Integer> convertIntArray(JsonArray array) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add((int) array.get(i).asNumber());
        }
        return list;
    }

    private TestPage setupUI(PolymerTemplate<?> template) {
        List<Object[]> args = new ArrayList<>();

        TestPage page = new TestPage();

        UI ui = new UI() {
            @Override
            public Page getPage() {
                return page;
            }
        };
        ui.add(template);
        return page;
    }
}
