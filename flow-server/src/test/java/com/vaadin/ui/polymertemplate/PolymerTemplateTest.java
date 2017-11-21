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

package com.vaadin.ui.polymertemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.nodefeature.AttachTemplateChildFeature;
import com.vaadin.flow.nodefeature.ElementData;
import com.vaadin.flow.nodefeature.ElementPropertyMap;
import com.vaadin.flow.nodefeature.NodeProperties;
import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.startup.CustomElementRegistry;
import com.vaadin.ui.Component;
import com.vaadin.ui.Page;
import com.vaadin.ui.Tag;
import com.vaadin.ui.UI;
import com.vaadin.util.HasCurrentService;

import elemental.json.JsonArray;
import net.jcip.annotations.NotThreadSafe;

/**
 * @author Vaadin Ltd.
 */
@NotThreadSafe
public class PolymerTemplateTest extends HasCurrentService {
    private static final String TAG = "FFS";

    private DeploymentConfiguration configuration;

    private List<Object> executionOrder = new ArrayList<>();

    private static class TestTemplateParser implements TemplateParser {

        private final Function<String, String> templateProducer;

        private int callCount;

        TestTemplateParser(Function<String, String> tagToTemplateContent) {
            templateProducer = tagToTemplateContent;
        }

        @Override
        public Element getTemplateContent(
                Class<? extends PolymerTemplate<?>> clazz, String tag) {
            callCount++;
            return Jsoup.parse(templateProducer.apply(tag));
        }
    }

    private static class SimpleTemplateParser extends TestTemplateParser {

        SimpleTemplateParser() {
            super(tag -> "<dom-module id='" + tag + "'></dom-module>");
        }

    }

    private static class TestPage extends Page {
        private final List<Serializable[]> params = new ArrayList<>();

        private TestPage() {
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

        public CustomComponent() {
            getElement().getNode().runWhenAttached(
                    ui -> ui.getPage().executeJavaScript("foo"));
        }

    }

    @Tag(TAG)
    public static class TestPolymerTemplate
            extends PolymerTemplate<ModelClass> {
        public TestPolymerTemplate() {
            super(new SimpleTemplateParser());
        }
    }

    @Tag(TAG)
    private static class IdChildTemplate extends PolymerTemplate<ModelClass> {

        @Id("child")
        private CustomComponent child;

        private final TestTemplateParser parser;

        public IdChildTemplate() {
            this(new TestTemplateParser(tag -> "<dom-module id='" + tag
                    + "'><template><div id='child'></template></dom-module>"));
        }

        IdChildTemplate(TestTemplateParser parser) {
            super(parser);
            this.parser = parser;
        }

    }

    @Tag("child-template")
    public static class TemplateChild extends PolymerTemplate<ModelClass> {
        public TemplateChild() {
            super(new SimpleTemplateParser());
        }
    }

    @Tag("parent-template")
    private static class TemplateInTemplate
            extends PolymerTemplate<ModelClass> {

        private final TestTemplateParser parser;

        public TemplateInTemplate() {
            this(new TestTemplateParser(tag -> "<dom-module id='" + tag
                    + "'><template><div><ffs></div><span></span><child-template></template></dom-module>"));
        }

        public TemplateInTemplate(TestTemplateParser parser) {
            super(parser);
            this.parser = parser;
        }

    }

    @Tag("parent-inject-child")
    private static class BundledTemplateInTemplate
            extends PolymerTemplate<ModelClass> {

        public BundledTemplateInTemplate() {
            super((clazz, tag) -> Jsoup.parse("<dom-module id='child-template'>"
                    + "<template><ffs></template></dom-module>"
                    + "<dom-module id='ffs'><template></template></dom-module>"
                    + "<dom-module id='" + tag
                    + "'><template><div><ffs></div><span></span><child-template></template></dom-module>"));
        }

    }

    @Tag("parent-inject-child")
    private static class TemplateInjectTemplate
            extends PolymerTemplate<ModelClass> {

        @Id("child")
        private TemplateChild child;

        public TemplateInjectTemplate() {
            super((clazz, tag) -> Jsoup.parse("<dom-module id='" + tag
                    + "'><template><child-template id='child'></template></dom-module>"));
        }

    }

    @Tag("parent-template")
    private static class TemplateWithChildInDomRepeat
            extends PolymerTemplate<ModelClass> {

        public TemplateWithChildInDomRepeat() {
            super((clazz, tag) -> Jsoup.parse("<dom-module id='" + tag
                    + "'><template><div>"
                    + "<dom-repeat items='[[messages]]'><template><child-template></template></dom-repeat>"
                    + "</div></template></dom-module>"));
        }

    }

    @Tag("parent-template")
    private static class TemplateWithDomRepeat
            extends PolymerTemplate<ModelClass> {

        private final TestTemplateParser parser;

        public TemplateWithDomRepeat() {
            this(new TestTemplateParser(tag -> "<dom-module id='" + tag
                    + "'><template><child-template>"
                    + "<dom-repeat items='[[messages]]'><template><div></template></dom-repeat>"
                    + "</template></dom-module>"));
        }

        TemplateWithDomRepeat(TestTemplateParser parser) {
            super(parser);
            this.parser = parser;
        }

    }

    @Tag(TAG)
    private static class TextNodesInHtmlTemplate
            extends PolymerTemplate<ModelClass> {

        private final TestTemplateParser parser;

        // @formatter:off
        private static String HTML_TEMPLATE = "<dom-module id='"+TAG+"'><template>\n"+
                "      <style>\n"+
                "      </style>\n"+
                "      <label></label>\n"+
                "      <child-template></child-template>\n"+
                "      \n"+
                "      <div class='content-wrap'></div><dom-module>";
        // @formatter:on

        public TextNodesInHtmlTemplate() {
            this(new TestTemplateParser(tag -> HTML_TEMPLATE));
        }

        public TextNodesInHtmlTemplate(TestTemplateParser parser) {
            super(parser);
            this.parser = parser;
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
            super(new TestTemplateParser(tag -> "<dom-module id='" + tag
                    + "'><div id='foo'></dom-module>"));
        }

    }

    private static class IdWrongTagChildTemplate extends IdChildTemplate {

        public IdWrongTagChildTemplate() {
            super(new TestTemplateParser(tag -> "<dom-module id='" + tag
                    + "'><label id='child'></dom-module>"));
        }

    }

    private static class TemplateWithoutTagAnnotation
            extends PolymerTemplate<ModelClass> {
    }

    @Tag(TAG)
    private static class NoModelTemplate extends PolymerTemplate<ModelClass> {

        NoModelTemplate() {
            super(new SimpleTemplateParser());
        }

    }

    @Tag("execution-child")
    public static class ExecutionChild extends PolymerTemplate<ModelClass> {
        public ExecutionChild() {
            super(new SimpleTemplateParser());
            getElement().getNode().runWhenAttached(
                    ui -> ui.getPage().executeJavaScript("bar"));
        }
    }

    @Tag("template-initializer-test")
    public class ExecutionOrder extends PolymerTemplate<TemplateModel> {
        @Id("div")
        public CustomComponent element;

        public ExecutionOrder() {
            super(new TestTemplateParser(tag -> "<dom-module id='" + tag
                    + "'><template><div id='div'></div><execution-child></execution-child></template></dom-module>"));
        }
    }

    @SuppressWarnings("serial")
    @Before
    public void setUp() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        executionOrder.clear();

        Field customElements = CustomElementRegistry.class
                .getDeclaredField("customElements");
        customElements.setAccessible(true);
        customElements.set(CustomElementRegistry.getInstance(),
                new AtomicReference<>());

        Map<String, Class<? extends Component>> map = new HashMap<>();
        map.put("child-template", TemplateChild.class);
        map.put("ffs", TestPolymerTemplate.class);
        map.put("execution-child", ExecutionChild.class);
        CustomElementRegistry.getInstance().setCustomElements(map);

        VaadinSession session = Mockito.mock(VaadinSession.class);
        UI ui = new UI() {
            private Page page = new Page(this) {

                @Override
                public ExecutionCanceler executeJavaScript(String expression,
                        Serializable... parameters) {
                    executionOrder.add(expression);
                    return () -> true;
                }
            };

            @Override
            public VaadinSession getSession() {
                return session;
            }

            @Override
            public Page getPage() {
                return page;
            }
        };
        VaadinService service = Mockito.mock(VaadinService.class);
        when(session.getService()).thenReturn(service);
        DefaultInstantiator instantiator = new DefaultInstantiator(service);
        when(service.getInstantiator()).thenReturn(instantiator);
        UI.setCurrent(ui);
    }

    @After
    public void tearDown() {
        UI.setCurrent(null);
    }

    @Override
    public VaadinService createService() {
        configuration = Mockito.mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(false);

        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        return service;
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
        doParseTemplate_hasIdChild_childIsRegisteredInFeature(
                new IdChildTemplate());
    }

    @Test
    public void parseCachedTemplate_hasIdChild_childIsRegisteredInFeature() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        // run in the production mode (with caching enabled) for the first time
        IdChildTemplate template = new IdChildTemplate();
        TestTemplateParser parser = template.parser;
        assertEquals(1, parser.callCount);
        // check the result for the first run
        doParseTemplate_hasIdChild_childIsRegisteredInFeature(template);

        // run in the production mode (with caching enabled) for the second time
        template = new IdChildTemplate(parser);
        // parser shouldn't be called
        assertEquals(1, parser.callCount);
        // the result should be the same
        doParseTemplate_hasIdChild_childIsRegisteredInFeature(template);
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
    public void parseTemplate_hasChildTemplate_elementsAreCreatedAndRequestIsSent() {
        doParseTemplate_hasChildTemplate_elementsAreCreatedAndRequestIsSent(
                new TemplateInTemplate());
    }

    @Test
    public void parseBundledTemplate_hasChildTemplate_elementsAreCreatedAndRequestIsSent() {
        doParseTemplate_hasChildTemplate_elementsAreCreatedAndRequestIsSent(
                new BundledTemplateInTemplate());
    }

    @Test
    public void parseTemplate_hasChildTemplateAndTemplateHtmlStyle_elementsAreCreatedAndRequestIsSent() {
        // Make a new HTML template which contains style on the top
        TemplateInTemplate template = new TemplateInTemplate(
                new TestTemplateParser(tag -> "<dom-module id='" + tag
                        + "'><template><style> a { width:100%; } </style><div><ffs></div><span></span>"
                        + "<child-template></template></dom-module>"));
        // Nothing should be changed in the logic
        doParseTemplate_hasChildTemplate_elementsAreCreatedAndRequestIsSent(
                template);
    }

    @Test
    public void parseCachedTemplate_hasChildTemplate_elementsAreCreatedAndRequestIsSent() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        // run in the production mode (with caching enabled) for the first time
        TemplateInTemplate template = new TemplateInTemplate();
        TestTemplateParser parser = template.parser;

        assertEquals(1, parser.callCount);
        // check the result for the first run
        doParseTemplate_hasChildTemplate_elementsAreCreatedAndRequestIsSent(
                template);

        // run in the production mode (with caching enabled) for the second time
        template = new TemplateInTemplate(parser);
        // parser shouldn't be called
        assertEquals(1, parser.callCount);
        // the result should be the same
        doParseTemplate_hasChildTemplate_elementsAreCreatedAndRequestIsSent(
                template);
    }

    @Test
    public void parseTemplate_hasTextNodesInTemplate_correctRequestIsSent() {
        doParseTemplate_hasTextNodesInTemplate_correctRequestIsSent(
                new TextNodesInHtmlTemplate());
    }

    @Test
    public void parseCachedTemplate_hasTextNodesInTemmplate_correctRequestIsSent() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        // run in the production mode (with caching enabled) for the first time
        TextNodesInHtmlTemplate template = new TextNodesInHtmlTemplate();
        TestTemplateParser parser = template.parser;
        doParseTemplate_hasTextNodesInTemplate_correctRequestIsSent(template);

        // run in the production mode (with caching enabled) for the second time
        template = new TextNodesInHtmlTemplate(parser);
        // parser shouldn't be called
        assertEquals(1, parser.callCount);
        // the result should be the same
        doParseTemplate_hasTextNodesInTemplate_correctRequestIsSent(template);
    }

    @Test(expected = IllegalStateException.class)
    public void parseTemplate_hasChildTemplateInsideDomRepeat_cantParse() {
        new TemplateWithChildInDomRepeat();
    }

    @Test
    public void parseTemplte_hasChildTemplateOutsideDomRepeat_elementIsCreated() {
        doParseTemplte_hasChildTemplateOutsideDomRepeat_elementIsCreated(
                new TemplateWithDomRepeat());
    }

    @Test
    public void parseCachedTemplte_hasChildTemplateOutsideDomRepeat_elementIsCreated() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        // run in the production mode (with caching enabled) for the first time
        TemplateWithDomRepeat template = new TemplateWithDomRepeat();
        TestTemplateParser parser = template.parser;
        doParseTemplte_hasChildTemplateOutsideDomRepeat_elementIsCreated(
                new TemplateWithDomRepeat());

        // run in the production mode (with caching enabled) for the second time
        template = new TemplateWithDomRepeat(parser);
        // parser shouldn't be called
        assertEquals(1, parser.callCount);
        // the result should be the same
        doParseTemplte_hasChildTemplateOutsideDomRepeat_elementIsCreated(
                template);
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

        assertEquals(1, templateNodes.size());

        StateNode child = templateNodes.get(0);
        String tag = child.getFeature(ElementData.class).getTag();
        assertEquals("label", tag);

        // check id in the JS call request
        assertEquals("labelId", page.params.get(0)[3]);

        assertNotNull(template.label);
        assertEquals(child, template.label.getNode());

        Assert.assertEquals("labelId",
                template.label.getAttribute(NodeProperties.ID));
    }

    @Test
    public void attachExistingElement_injectedByIDdChild_onlyOneElementIsCreate() {
        TemplateInjectTemplate template = new TemplateInjectTemplate();

        setupUI(template);

        AttachTemplateChildFeature feature = template.getStateNode()
                .getFeature(AttachTemplateChildFeature.class);
        List<StateNode> templateNodes = new ArrayList<>();
        feature.forEachChild(templateNodes::add);

        assertEquals(1, templateNodes.size());
        StateNode stateNode = templateNodes.get(0);

        assertEquals(stateNode, template.child.getStateNode());
    }

    @Test
    public void attachExistingComponent_elementIsCreatedAndRequestIsSent() {
        IdChildTemplate template = new IdChildTemplate();

        TestPage page = setupUI(template);

        AttachTemplateChildFeature feature = template.getStateNode()
                .getFeature(AttachTemplateChildFeature.class);
        List<StateNode> templateNodes = new ArrayList<>();
        feature.forEachChild(templateNodes::add);

        assertEquals(1, templateNodes.size());

        StateNode child = templateNodes.get(0);
        String tag = child.getFeature(ElementData.class).getTag();
        assertEquals("div", tag);

        // check id in the JS call request
        assertEquals("child", page.params.get(0)[3]);

        assertNotNull(template.child);
        assertEquals(child, template.child.getElement().getNode());

        assertTrue(template.child.getElement().getComponent().isPresent());

        assertTrue(template.child.getElement().getComponent()
                .get() instanceof CustomComponent);

        assertEquals(template.child,
                template.child.getElement().getComponent().get());
    }

    @Test
    public void executionOrder_attachByIdInvokedBeforeComponentIsCreated() {
        ExecutionOrder template = new ExecutionOrder();

        UI.getCurrent().add(template);

        Assert.assertEquals(4, executionOrder.size());

        // The order is important: "attachXXX" methods must be called before any
        // other JS execution for the same component.

        int index = executionOrder
                .indexOf("this.attachCustomElement($0, $1, $2, $3);");
        Assert.assertNotEquals(-1, index);

        Assert.assertEquals("bar", executionOrder.get(index + 1));

        index = executionOrder
                .indexOf("this.attachExistingElementById($0, $1, $2, $3);");
        Assert.assertEquals("foo", executionOrder.get(index + 1));
    }

    private List<Integer> convertIntArray(JsonArray array) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add((int) array.get(i).asNumber());
        }
        return list;
    }

    private TestPage setupUI(PolymerTemplate<?> template) {
        TestPage page = new TestPage();

        VaadinSession session = UI.getCurrent() == null ? null
                : UI.getCurrent().getSession();

        UI ui = new UI() {
            @Override
            public Page getPage() {
                return page;
            }

            @Override
            public VaadinSession getSession() {
                if (session != null) {
                    return session;
                }
                return super.getSession();
            }
        };
        ui.add(template);

        UI.setCurrent(ui);
        return page;
    }

    private void doParseTemplate_hasIdChild_childIsRegisteredInFeature(
            IdChildTemplate template) {
        UI ui = new UI();
        ui.add(template);

        AttachTemplateChildFeature feature = template.getElement().getNode()
                .getFeature(AttachTemplateChildFeature.class);
        AtomicInteger counter = new AtomicInteger(0);
        AtomicReference<StateNode> injected = new AtomicReference<>();
        feature.forEachChild(child -> {
            counter.incrementAndGet();
            injected.set(child);
        });
        assertEquals(1, counter.get());

        Assert.assertEquals("child", com.vaadin.flow.dom.Element
                .get(injected.get()).getAttribute(NodeProperties.ID));
    }

    private void doParseTemplate_hasChildTemplate_elementsAreCreatedAndRequestIsSent(
            PolymerTemplate<?> template) {
        TestPage page = setupUI(template);

        AttachTemplateChildFeature feature = template.getStateNode()
                .getFeature(AttachTemplateChildFeature.class);
        List<StateNode> templateNodes = new ArrayList<>();
        feature.forEachChild(templateNodes::add);

        assertEquals(2, templateNodes.size());
        StateNode child1 = templateNodes.get(0);
        StateNode child2 = templateNodes.get(1);
        String tag = child1.getFeature(ElementData.class).getTag();
        if ("child-template".equals(tag)) {
            assertEquals("ffs", child2.getFeature(ElementData.class).getTag());
        } else {
            assertEquals("ffs", child1.getFeature(ElementData.class).getTag());
            assertEquals("child-template",
                    child2.getFeature(ElementData.class).getTag());
        }

        Set<Object> paths = new HashSet<>();
        paths.add(convertIntArray((JsonArray) page.params.get(0)[3]));
        paths.add(convertIntArray((JsonArray) page.params.get(1)[3]));

        // check arrays of indices
        assertTrue(paths.contains(Arrays.asList(0, 0)));
        assertTrue(paths.contains(Arrays.asList(2)));
    }

    private void doParseTemplate_hasTextNodesInTemplate_correctRequestIsSent(
            TextNodesInHtmlTemplate template) {
        TestPage page = setupUI(template);

        JsonArray path = (JsonArray) page.params.get(0)[3];

        // check arrays of indices
        assertEquals(1, path.length());
        assertEquals(1, (int) path.get(0).asNumber());
    }

    private void doParseTemplte_hasChildTemplateOutsideDomRepeat_elementIsCreated(
            TemplateWithDomRepeat template) {
        UI ui = new UI();
        ui.add(template);

        AttachTemplateChildFeature feature = template.getStateNode()
                .getFeature(AttachTemplateChildFeature.class);
        List<StateNode> templateNodes = new ArrayList<>();
        feature.forEachChild(templateNodes::add);

        assertEquals(1, templateNodes.size());
    }
}
