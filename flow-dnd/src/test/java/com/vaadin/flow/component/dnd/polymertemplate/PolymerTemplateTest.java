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

package com.vaadin.flow.component.polymertemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.jsoup.Jsoup;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.HasCurrentService;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.TemplateModel;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class PolymerTemplateTest extends HasCurrentService {
    private static final String TAG = "FFS";

    private DeploymentConfiguration configuration;

    private List<Object> executionOrder = new ArrayList<>();
    private List<Serializable[]> executionParams = new ArrayList<>();

    // Field to prevent current instance from being garbage collected
    private UI ui;

    private static class TestTemplateParser implements TemplateParser {

        private final Function<String, String> templateProducer;

        private int callCount;

        TestTemplateParser(Function<String, String> tagToTemplateContent) {
            templateProducer = tagToTemplateContent;
        }

        @Override
        public TemplateData getTemplateContent(
                Class<? extends PolymerTemplate<?>> clazz, String tag,
                VaadinService service) {
            callCount++;
            return new TemplateData("",
                    Jsoup.parse(templateProducer.apply(tag)));
        }
    }

    private static class SimpleTemplateParser extends TestTemplateParser {

        SimpleTemplateParser() {
            super(tag -> "<dom-module id='" + tag + "' someattrtibute></dom-module>");
        }

    }

    public interface ModelClass extends TemplateModel {
        void setMessage(String message);

        void setTitle(String title);

        @AllowClientUpdates
        String getMessage();

        @AllowClientUpdates
        String getTitle();
    }

    public interface TestModel extends ModelClass {
        @AllowClientUpdates
        void setList(List<String> list);
    }

    @Tag(Tag.DIV)
    public static class CustomComponent extends Component {

        public CustomComponent() {
            getElement().getNode()
                    .runWhenAttached(ui -> ui.getPage().executeJs("foo"));
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
                    + "'><template><div id='child' someattrtibute></template></dom-module>"));
        }

        IdChildTemplate(TestTemplateParser parser) {
            super(parser);
            this.parser = parser;
        }

    }

    @Tag(TAG)
    private static class IdWithNoValueChildTemplate
            extends PolymerTemplate<ModelClass> {

        @Id
        private CustomComponent child;

        private final TestTemplateParser parser;

        public IdWithNoValueChildTemplate() {
            this(new TestTemplateParser(tag -> "<dom-module id='" + tag
                    + "'><template><div id='child'></template></dom-module>"));
        }

        IdWithNoValueChildTemplate(TestTemplateParser parser) {
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
    @Uses(TestPolymerTemplate.class)
    @Uses(TemplateChild.class)
    private static class TemplateInTemplate
            extends PolymerTemplate<ModelClass> {

        private final TestTemplateParser parser;

        public TemplateInTemplate() {
            this(new TestTemplateParser(tag -> "<dom-module id='" + tag
                    + "'><template><div><ffs></div><span></span><child-template someattrtibute></template></dom-module>"));
        }

        public TemplateInTemplate(TestTemplateParser parser) {
            super(parser);
            this.parser = parser;
        }

    }

    @Tag("parent-inject-child")
    @Uses(TestPolymerTemplate.class)
    @Uses(TemplateChild.class)
    private static class BundledTemplateInTemplate
            extends PolymerTemplate<ModelClass> {

        public BundledTemplateInTemplate() {
            super((clazz, tag, service) -> new TemplateData("",
                    Jsoup.parse("<dom-module id='child-template'>"
                            + "<template><ffs></template></dom-module>"
                            + "<dom-module id='ffs'><template></template></dom-module>"
                            + "<dom-module id='" + tag
                            + "'><template><div><ffs someattrtibute></div><span></span><child-template></template></dom-module>")));
        }

    }

    @Tag("parent-inject-child")
    private static class TemplateInjectTemplate
            extends PolymerTemplate<ModelClass> {

        @Id("child")
        private TemplateChild child;

        public TemplateInjectTemplate() {
            super((clazz, tag, service) -> new TemplateData("",
                    Jsoup.parse("<dom-module id='" + tag
                            + "'><template><child-template id='child'></template></dom-module>")));
        }

    }

    @Tag("parent-template")
    @Uses(TemplateChild.class)
    private static class TemplateWithChildInDomRepeat
            extends PolymerTemplate<ModelClass> {

        public TemplateWithChildInDomRepeat() {
            super((clazz, tag, service) -> new TemplateData("",
                    Jsoup.parse("<dom-module id='" + tag + "'><template><div>"
                            + "<dom-repeat items='[[messages]]'><template><child-template someattrtibute></template></dom-repeat>"
                            + "</div></template></dom-module>")));
        }

    }

    @Tag("parent-template")
    @Uses(TemplateChild.class)
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
    @Uses(TemplateChild.class)
    private static class TextNodesInHtmlTemplate
            extends PolymerTemplate<ModelClass> {

        private final TestTemplateParser parser;

        // @formatter:off
        private static String HTML_TEMPLATE = "<dom-module id='"+TAG+"'><template>\n"+
                "      <style>\n"+
                "      </style>\n"+
                "      <label></label>\n"+
                "      <child-template someattrtibute></child-template>\n"+
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
            this((clazz, tag, service) -> new TemplateData("",
                    Jsoup.parse("<dom-module id='" + tag
                            + "'><label id='labelId' someattrtibute></dom-module>")));
        }

        IdElementTemplate(TemplateParser parser) {
            super(parser);
        }

    }

    private static class IdWrongElementTemplate extends IdElementTemplate {

        public IdWrongElementTemplate() {
            super((clazz, tag, service) -> new TemplateData("",
                    Jsoup.parse("<dom-module id='" + tag
                            + "'><div id='foo'></dom-module>")));
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
    @SuppressWarnings("rawtypes")
    private static class NoModelTemplate extends PolymerTemplate {

        NoModelTemplate() {
            super(new SimpleTemplateParser());
        }

    }

    @Tag("execution-child")
    public static class ExecutionChild extends PolymerTemplate<ModelClass> {
        public ExecutionChild() {
            super(new SimpleTemplateParser());
            getElement().getNode()
                    .runWhenAttached(ui -> ui.getPage().executeJs("bar"));
        }
    }

    @Tag("template-initializer-test")
    @Uses(ExecutionChild.class)
    public static class ExecutionOrder extends PolymerTemplate<TemplateModel> {
        @Id("div")
        public CustomComponent element;

        public ExecutionOrder() {
            super(new TestTemplateParser(tag -> "<dom-module id='" + tag
                    + "'><template><div id='div'></div><execution-child someattrtibute></execution-child></template></dom-module>"));
        }
    }

    @Tag("init-model-test")
    public static class InitModelTemplate extends PolymerTemplate<TestModel> {

        public InitModelTemplate() {
            super(new SimpleTemplateParser());
        }
    }

    @Tag("template-initialization")
    @Uses(TestPolymerTemplate.class)
    @Uses(TemplateChild.class)
    public static class TemplateInitialization
            extends PolymerTemplate<TestModel> {

        @Id("foo")
        private TestPolymerTemplate child;

        public TemplateInitialization(TemplateParser parser) {
            super(parser);
        }
    }

    @Tag("another-template-initialization")
    @Uses(TestPolymerTemplate.class)
    @Uses(TemplateChild.class)
    public static class AnotherTemplateInitialization
            extends PolymerTemplate<TestModel> {

        @Id("bar")
        private TemplateChild child;

        public AnotherTemplateInitialization(TemplateParser parser) {
            super(parser);
        }
    }

    @SuppressWarnings("serial")
    @Before
    public void setUp() throws SecurityException,
            IllegalArgumentException {
        executionOrder.clear();
        executionParams.clear();

        VaadinSession session = Mockito.mock(VaadinSession.class);
        ui = new UI() {
            private Page page = new Page(this) {

                @Override
                public PendingJavaScriptResult executeJs(String expression,
                        Serializable... parameters) {
                    executionOrder.add(expression);
                    executionParams.add(parameters);
                    return null;
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

    @Test
    public void parseCachedTemplate_twoTemplatesWithInjetions_injectionsAreRegisteredInFeature() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        AtomicInteger parserCallCount = new AtomicInteger();

        TemplateParser parser = new TemplateParser() {

            @Override
            public TemplateData getTemplateContent(
                    Class<? extends PolymerTemplate<?>> clazz, String tag,
                    VaadinService service) {
                String content;
                parserCallCount.incrementAndGet();
                if (clazz.equals(TemplateInitialization.class)) {
                    content = "<dom-module id='" + tag + "'><template>"
                            + "<ffs id='foo' someattrtibute></ffs>"
                            + "<child-template></child-template>"
                            + "</template></dom-module>";
                } else {
                    content = "<dom-module id='" + tag + "'><template>"
                            + "<child-template id='bar'></child-template> <ffs></ffs>"
                            + "</template></dom-module>";
                }
                return new TemplateData("", Jsoup.parse(content));
            }
        };

        // run in the production mode (with caching enabled) for the first time
        TemplateInitialization template1 = new TemplateInitialization(parser);
        assertEquals(1, parserCallCount.get());

        assertTemplateInitialization(template1);

        // run in the production mode (with caching enabled) for the second time
        template1 = new TemplateInitialization(parser);
        // parser shouldn't be called
        assertEquals(1, parserCallCount.get());

        assertTemplateInitialization(template1);

        parserCallCount.set(0);

        // Now initialize another template

        // run in the production mode (with caching enabled) for the first time
        AnotherTemplateInitialization template2 = new AnotherTemplateInitialization(
                parser);
        assertEquals(1, parserCallCount.get());

        assertAnotherTemplateInitialization(template2);

        // run in the production mode (with caching enabled) for the second time
        template2 = new AnotherTemplateInitialization(parser);
        // parser shouldn't be called
        assertEquals(1, parserCallCount.get());

        assertAnotherTemplateInitialization(template2);
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
    public void parseTemplate_hasChildTemplate_elementIsCreatedAndSetAsVirtualChild() {
        doParseTemplate_hasChildTemplate_elementIsCreatedAndSetAsVirtualChild(
                new TemplateInTemplate());
    }

    @Test
    public void parseBundledTemplate_hasChildTemplate_elementIsCreatedAndSetAsVirtualChild() {
        doParseTemplate_hasChildTemplate_elementIsCreatedAndSetAsVirtualChild(
                new BundledTemplateInTemplate());
    }

    @Test
    public void parseTemplate_hasChildTemplateAndTemplateHtmlStyle_elementIsCreatedAndSetAsVirtualChild() {
        // Make a new HTML template which contains style on the top
        TemplateInTemplate template = new TemplateInTemplate(
                new TestTemplateParser(tag -> "<dom-module id='" + tag
                        + "'><template><style> a { width:100%; } </style><div><ffs someattrtibute></div><span></span>"
                        + "<child-template></template></dom-module>"));
        // Nothing should be changed in the logic
        doParseTemplate_hasChildTemplate_elementIsCreatedAndSetAsVirtualChild(
                template);
    }

    @Test
    public void parseCachedTemplate_hasChildTemplate_elementIsCreatedAndSetAsVirtualChild() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        // run in the production mode (with caching enabled) for the first time
        TemplateInTemplate template = new TemplateInTemplate();
        TestTemplateParser parser = template.parser;

        assertEquals(1, parser.callCount);
        // check the result for the first run
        doParseTemplate_hasChildTemplate_elementIsCreatedAndSetAsVirtualChild(
                template);

        // run in the production mode (with caching enabled) for the second time
        template = new TemplateInTemplate(parser);
        // parser shouldn't be called
        assertEquals(1, parser.callCount);
        // the result should be the same
        doParseTemplate_hasChildTemplate_elementIsCreatedAndSetAsVirtualChild(
                template);
    }

    @Test
    public void parseTemplate_hasTextNodesInTemplate_correctRequestIsSent() {
        doParseTemplate_hasTextNodesInTemplate_correctRequestIndicesPath(
                new TextNodesInHtmlTemplate());
    }

    @Test
    public void parseCachedTemplate_hasTextNodesInTemmplate_correctRequestIsSent() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        // run in the production mode (with caching enabled) for the first time
        TextNodesInHtmlTemplate template = new TextNodesInHtmlTemplate();
        TestTemplateParser parser = template.parser;
        doParseTemplate_hasTextNodesInTemplate_correctRequestIndicesPath(
                template);

        // run in the production mode (with caching enabled) for the second time
        template = new TextNodesInHtmlTemplate(parser);
        // parser shouldn't be called
        assertEquals(1, parser.callCount);
        // the result should be the same
        doParseTemplate_hasTextNodesInTemplate_correctRequestIndicesPath(
                template);
    }

    @Test(expected = IllegalStateException.class)
    public void parseTemplate_hasChildTemplateInsideDomRepeat_cantParse() {
        new TemplateWithChildInDomRepeat();
    }

    @Test
    public void parseTemplte_hasChildTemplateOutsideDomRepeat_elementIsCreated() {
        doParseTemplate_hasChildTemplateOutsideDomRepeat_elementIsCreated(
                new TemplateWithDomRepeat());
    }

    @Test
    public void parseCachedTemplte_hasChildTemplateOutsideDomRepeat_elementIsCreated() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        // run in the production mode (with caching enabled) for the first time
        TemplateWithDomRepeat template = new TemplateWithDomRepeat();
        TestTemplateParser parser = template.parser;
        doParseTemplate_hasChildTemplateOutsideDomRepeat_elementIsCreated(
                new TemplateWithDomRepeat());

        // run in the production mode (with caching enabled) for the second time
        template = new TemplateWithDomRepeat(parser);
        // parser shouldn't be called
        assertEquals(1, parser.callCount);
        // the result should be the same
        doParseTemplate_hasChildTemplateOutsideDomRepeat_elementIsCreated(
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
    public void attachExistingElement_elementIsCreatedAndSetAsVirtualChild() {
        IdElementTemplate template = new IdElementTemplate();

        VirtualChildrenList feature = template.getStateNode()
                .getFeature(VirtualChildrenList.class);
        List<StateNode> templateNodes = new ArrayList<>();
        feature.forEachChild(templateNodes::add);

        assertEquals(1, templateNodes.size());

        StateNode child = templateNodes.get(0);
        String tag = child.getFeature(ElementData.class).getTag();
        assertEquals("label", tag);

        assertNotNull(template.label);
        assertEquals(child, template.label.getNode());

        assertElementData(child, NodeProperties.INJECT_BY_ID, "labelId");
    }

    @Test
    public void attachExistingElement_injectedByIDdChild_onlyOneElementIsCreated() {
        TemplateInjectTemplate template = new TemplateInjectTemplate();

        VirtualChildrenList feature = template.getStateNode()
                .getFeature(VirtualChildrenList.class);

        List<StateNode> templateNodes = new ArrayList<>();
        feature.forEachChild(templateNodes::add);

        assertEquals(1, templateNodes.size());
        StateNode stateNode = templateNodes.get(0);

        assertEquals(stateNode, template.child.getStateNode());

        assertElementData(stateNode, NodeProperties.INJECT_BY_ID, "child");
    }

    @Test
    public void attachExistingComponent_elementIsCreatedAndSetAsVirtualChild() {
        IdChildTemplate template = new IdChildTemplate();
        attachComponentAndVerifyChild(template, template.child);
    }

    @Test
    public void attachExistingComponent_idWithNoValue_elementIsCreatedAndSetAsVirtualChild() {
        IdWithNoValueChildTemplate template = new IdWithNoValueChildTemplate();
        attachComponentAndVerifyChild(template, template.child);
    }

    private void attachComponentAndVerifyChild(PolymerTemplate<?> template,
                                               CustomComponent templateChild) {
        VirtualChildrenList feature = template.getStateNode()
                .getFeature(VirtualChildrenList.class);
        List<StateNode> templateNodes = new ArrayList<>();
        feature.forEachChild(templateNodes::add);

        assertEquals(1, templateNodes.size());

        StateNode child = templateNodes.get(0);
        String tag = child.getFeature(ElementData.class).getTag();
        assertEquals("div", tag);

        assertNotNull(templateChild);
        assertEquals(child, templateChild.getElement().getNode());

        assertTrue(templateChild.getElement().getComponent().isPresent());

        assertTrue(templateChild.getElement().getComponent()
                .get() instanceof CustomComponent);

        assertEquals(templateChild,
                templateChild.getElement().getComponent().get());
        assertElementData(child, NodeProperties.INJECT_BY_ID, "child");
    }

    @Test
    public void initModel_onlyExplicitelySetPropertiesAreSet() {
        InitModelTemplate template = new InitModelTemplate();

        template.getModel().setMessage("foo");

        ElementPropertyMap map = template.getElement().getNode()
                .getFeature(ElementPropertyMap.class);

        // message has been explicitly set
        Assert.assertTrue(map.hasProperty("message"));
        Assert.assertNotNull(map.getProperty("message"));
        // "list" is represented by StateNode so it's considered as explicitly
        // set
        Assert.assertTrue(map.hasProperty("list"));
        Assert.assertNotNull(map.getProperty("list"));

        // title has not been
        Assert.assertFalse(map.hasProperty("title"));
    }

    @Test
    public void initModel_requestPopulateModel_onlyUnsetPropertiesAreSent() {
        UI ui = UI.getCurrent();
        InitModelTemplate template = new InitModelTemplate();

        template.getModel().setMessage("foo");

        ui.add(template);

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        Assert.assertEquals(2, executionOrder.size());
        Assert.assertEquals("this.populateModelProperties($0, $1)",
                executionOrder.get(1));

        Serializable[] params = executionParams.get(1);
        JsonArray properties = (JsonArray) params[1];
        Assert.assertEquals(1, properties.length());
        Assert.assertEquals("title", properties.get(0).asString());
    }

    @Test
    public void initModel_sendUpdatableProperties() {
        UI ui = UI.getCurrent();
        InitModelTemplate template = new InitModelTemplate();

        ui.add(template);

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        Assert.assertEquals(2, executionOrder.size());
        Assert.assertEquals("this.registerUpdatableModelProperties($0, $1)",
                executionOrder.get(0));

        Serializable[] params = executionParams.get(0);
        JsonArray properties = (JsonArray) params[1];
        Assert.assertEquals(2, properties.length());

        Set<String> props = new HashSet<>();
        props.add(properties.get(0).asString());
        props.add(properties.get(1).asString());
        // all model properties except 'list' which has no getter
        Assert.assertTrue(props.contains("message"));
        Assert.assertTrue(props.contains("title"));
    }

    private void doParseTemplate_hasIdChild_childIsRegisteredInFeature(
            IdChildTemplate template) {
        VirtualChildrenList feature = template.getStateNode()
                .getFeature(VirtualChildrenList.class);
        assertEquals(1, feature.size());
    }

    private void doParseTemplate_hasChildTemplate_elementIsCreatedAndSetAsVirtualChild(
            PolymerTemplate<?> template) {
        VirtualChildrenList feature = template.getStateNode()
                .getFeature(VirtualChildrenList.class);
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
        assertTemplateInTempalte(child1);
        assertTemplateInTempalte(child2);
    }

    private void doParseTemplate_hasTextNodesInTemplate_correctRequestIndicesPath(
            TextNodesInHtmlTemplate template) {

        VirtualChildrenList feature = template.getStateNode()
                .getFeature(VirtualChildrenList.class);
        JsonObject object = (JsonObject) feature.get(0)
                .getFeature(ElementData.class).getPayload();
        JsonArray path = object.getArray(NodeProperties.PAYLOAD);

        // check arrays of indices
        assertEquals(1, path.length());
        assertEquals(1, (int) path.get(0).asNumber());
    }

    private void doParseTemplate_hasChildTemplateOutsideDomRepeat_elementIsCreated(
            TemplateWithDomRepeat template) {
        UI ui = new UI();
        ui.add(template);

        VirtualChildrenList feature = template.getStateNode()
                .getFeature(VirtualChildrenList.class);
        List<StateNode> templateNodes = new ArrayList<>();
        feature.forEachChild(templateNodes::add);

        assertEquals(1, templateNodes.size());
    }

    private void assertElementData(StateNode node, String type,
            String payload) {
        JsonObject object = (JsonObject) node.getFeature(ElementData.class)
                .getPayload();
        Assert.assertEquals(type, object.getString(NodeProperties.TYPE));
        Assert.assertEquals(payload, object.getString(NodeProperties.PAYLOAD));
    }

    private void assertTemplateInTempalte(StateNode node) {
        JsonObject object = (JsonObject) node.getFeature(ElementData.class)
                .getPayload();
        Assert.assertEquals(NodeProperties.TEMPLATE_IN_TEMPLATE,
                object.getString(NodeProperties.TYPE));

        Assert.assertTrue(
                object.get(NodeProperties.PAYLOAD) instanceof JsonArray);
    }

    private void assertTemplateInitialization(TemplateInitialization template) {
        VirtualChildrenList feature = template.getStateNode()
                .getFeature(VirtualChildrenList.class);
        assertEquals(2, feature.size());

        Optional<Component> child = com.vaadin.flow.dom.Element
                .get(feature.get(0)).getComponent();

        Assert.assertTrue(child.isPresent());
        Assert.assertEquals(TestPolymerTemplate.class, child.get().getClass());

        child = com.vaadin.flow.dom.Element.get(feature.get(1)).getComponent();

        Assert.assertTrue(child.isPresent());
        Assert.assertEquals(TemplateChild.class, child.get().getClass());
    }

    private void assertAnotherTemplateInitialization(
            AnotherTemplateInitialization template) {
        VirtualChildrenList feature = template.getStateNode()
                .getFeature(VirtualChildrenList.class);
        assertEquals(2, feature.size());

        Optional<Component> child = com.vaadin.flow.dom.Element
                .get(feature.get(0)).getComponent();

        Assert.assertTrue(child.isPresent());
        Assert.assertEquals(TemplateChild.class, child.get().getClass());

        child = com.vaadin.flow.dom.Element.get(feature.get(1)).getComponent();

        Assert.assertTrue(child.isPresent());
        Assert.assertEquals(TestPolymerTemplate.class, child.get().getClass());
    }
}
