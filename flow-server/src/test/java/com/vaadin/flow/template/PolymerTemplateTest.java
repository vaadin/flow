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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.Id;
import com.vaadin.annotations.Tag;
import com.vaadin.external.jsoup.Jsoup;
import com.vaadin.external.jsoup.nodes.Element;
import com.vaadin.flow.StateNode;
import com.vaadin.flow.nodefeature.AttachExistingElementFeatureById;
import com.vaadin.flow.nodefeature.ElementPropertyMap;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

/**
 * @author Vaadin Ltd.
 */
public class PolymerTemplateTest {
    private static final String TAG = "FFS";

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
    private static class TestPolymerTemplate
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
                    + "'><div id='child'></dom-module>"));
        }

        IdChildTemplate(TemplateParser parser) {
            super(parser);
        }

    }

    private static class IdWrongChildTemplate extends IdChildTemplate {

        public IdWrongChildTemplate() {
            super((clazz, tag) -> Jsoup.parse("<dom-module id='" + tag
                    + "'><div id='foo'></dom-module>"));
        }

    }

    private static class TemplateWithoutTagAnnotation
            extends PolymerTemplate<ModelClass> {
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

        AttachExistingElementFeatureById feature = template.getElement()
                .getNode().getFeature(AttachExistingElementFeatureById.class);
        AtomicInteger counter = new AtomicInteger(0);
        feature.forEachChild(child -> counter.incrementAndGet());
        Assert.assertEquals(1, counter.get());
    }

    @Test(expected = IllegalStateException.class)
    public void parseTemplate_hasWrongIdChild_exceptionIsThrown() {
        new IdWrongChildTemplate();
    }

}
