/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component.littemplate.internal;

import java.util.Properties;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.littemplate.LitTemplateParser;
import com.vaadin.flow.component.littemplate.LitTemplateParser.TemplateData;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.MockVaadinServletService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LitTemplateParserImplTest {

    private MockVaadinServletService service;
    @Mock
    DeploymentConfiguration configuration;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        Mockito.when(configuration.getBuildFolder()).thenReturn("target");

        Properties properties = new Properties();
        Mockito.when(configuration.getInitParameters()).thenReturn(properties);

        Instantiator instantiator = Mockito.mock(Instantiator.class);
        Mockito.when(instantiator.getServiceInitListeners())
                .thenReturn(Stream.empty());
        Mockito.when(instantiator.getDependencyFilters(Mockito.any()))
                .thenReturn(Stream.empty());
        Mockito.when(instantiator.getIndexHtmlRequestListeners(Mockito.any()))
                .thenReturn(Stream.empty());
        service = new MockVaadinServletService(configuration);
        service.init(instantiator);

        ResourceProvider resourceProvider = service.getContext()
                .getAttribute(Lookup.class).lookup(ResourceProvider.class);
        Mockito.when(
                resourceProvider.getApplicationResource(Mockito.anyString()))
                .thenAnswer(invoc -> LitTemplateParserImpl.class
                        .getClassLoader().getResource(invoc.getArgument(0)));
    }

    @Test
    void getTemplateContent_rootElementParsed() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        TemplateData templateContent = instance
                .getTemplateContent(MyLitElement.class, "my-element", service);

        assertEquals("my-element",
                templateContent.getTemplateElement().parent().id(),
                "Parent element ID not the expected one.");

        assertEquals(2, templateContent.getTemplateElement().childNodeSize(),
                "Expected template element to have 2 children");

        assertEquals("div",
                templateContent.getTemplateElement().getElementById("test")
                        .tag().toString(),
                "Template element should have contained a div element with the id 'test'");
    }

    @Test
    void getTemplateContent_templateParsedGreedly_rootElementParsed() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        TemplateData templateContent = instance.getTemplateContent(
                MyGreedyLitElement.class, "my-greedy-element", service);

        assertEquals("my-greedy-element",
                templateContent.getTemplateElement().parent().id(),
                "Parent element ID not the expected one.");

        assertEquals(2, templateContent.getTemplateElement().childNodeSize(),
                "Expected template element to have 2 children");

        assertEquals("div",
                templateContent.getTemplateElement().getElementById("test")
                        .tag().toString(),
                "Template element should have contained a div element with the id 'test'");
    }

    @Test
    void getTemplateContent_localFileTemplateExists_useLocalFileContent() {
        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        TemplateData templateContent = instance.getTemplateContent(
                MyLitElementView.class, "my-lit-element-view", service);

        assertEquals("my-lit-element-view",
                templateContent.getTemplateElement().parent().id(),
                "Parent element ID not the expected one.");

        assertEquals(3, templateContent.getTemplateElement().childNodeSize(),
                "Expected template element to have 3 children");

        assertEquals("div",
                templateContent.getTemplateElement().getElementById("label")
                        .tag().toString(),
                "Template element should have contained a div element with the id 'label'");
    }

    @Test
    void getTypescriptTemplateContent_templateExists_getTemplateContent() {
        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        TemplateData templateContent = instance.getTemplateContent(MyForm.class,
                "my-form", service);

        assertEquals("my-form",
                templateContent.getTemplateElement().parent().id(),
                "Parent element ID not the expected one.");

        assertEquals(2, templateContent.getTemplateElement().childNodeSize(),
                "Expected template element to have 2 children");

        assertEquals("vaadin-text-field",
                templateContent.getTemplateElement().getElementById("nameField")
                        .tag().toString(),
                "Template element should have contained a div element with the id 'label'");
    }

    @Test
    void getTemplateContent_localFileNotFound_returnsNull() {
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenReturn("META-INF/resources/foo-bar.json");
        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        assertNull(instance.getTemplateContent(FooView.class, "foo-view",
                service));
    }

    @Test
    void getTemplateContent_sourceNotFoundInStatsFile_returnsNull() {
        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        assertNull(instance.getTemplateContent(FooView.class, "foo-view",
                service));
    }

    @Test
    void getTemplateContent_sourceFileWithFaultyTemplateGetter_returnsNull() {
        // If the template getter can not be found it should result in no
        // template element children
        LitTemplateParser.TemplateData templateContent = LitTemplateParserImpl
                .getInstance()
                .getTemplateContent(MyFaulty.class, "my-element", service);

        assertNull(templateContent);
    }

    @Test
    void getTemplateContent_renderIsDefinedInSuperClass_returnsNull() {
        // If the template getter can not be found it should result in no
        // template element children
        LitTemplateParser.TemplateData templateContent = LitTemplateParserImpl
                .getInstance().getTemplateContent(MyFaulty.class,
                        "my-super-lit-element", service);

        assertNull(templateContent);
    }

    @Test
    void getTemplateContent_nonLocalTemplate_rootElementParsed() {
        LitTemplateParser.TemplateData templateContent = LitTemplateParserImpl
                .getInstance().getTemplateContent(HelloWorld.class,
                        HelloWorld.class.getAnnotation(Tag.class).value(),
                        service);

        assertEquals(2, templateContent.getTemplateElement().childNodeSize(),
                "Template should contain one child");

        assertEquals(3, templateContent.getTemplateElement()
                .getElementsByTag("div").size(), "Template should have 3 divs");
    }

    @Test
    void getTemplateContent_nonLocalTemplateInTargetFolder_rootElementParsed() {
        LitTemplateParser.TemplateData templateContent = LitTemplateParserImpl
                .getInstance().getTemplateContent(HelloWorld2.class,
                        HelloWorld2.class.getAnnotation(Tag.class).value(),
                        service);

        assertEquals(2, templateContent.getTemplateElement().childNodeSize(),
                "Template should contain one child");

        assertEquals(3, templateContent.getTemplateElement()
                .getElementsByTag("div").size(), "Template should have 3 divs");
    }

    @Test
    void severalJsModuleAnnotations_theFirstFileDoesNotExist_fileWithContentIsChosen() {
        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        LitTemplateParser.TemplateData templateContent = instance
                .getTemplateContent(BrokenJsModuleAnnotation.class,
                        "my-lit-element-view", service);

        assertEquals("my-lit-element-view",
                templateContent.getTemplateElement().parent().id(),
                "Parent element ID not the expected one.");
    }

    @Test
    void severalJsModuleAnnotations_parserSelectsByName() {
        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        LitTemplateParser.TemplateData templateContent = instance
                .getTemplateContent(SeveralJsModuleAnnotations.class,
                        "my-lit-element-view", service);

        assertEquals("my-lit-element-view",
                templateContent.getTemplateElement().parent().id(),
                "Parent element ID not the expected one.");

        // Two JS module annotations with almost the same content.
        // The first one contains a string "Tag name doesn't match the JS module
        // name", the second one doesn't contain this string.
        // The second module should be chosen since its name matches the tag
        // name
        MatcherAssert.assertThat(templateContent.getTemplateElement().html(),
                CoreMatchers.not(CoreMatchers.containsString(
                        "Tag name doesn't match the JS module name")));
    }

    @Tag("my-element")
    @JsModule("./frontend/MyLitElement.js")
    public class MyLitElement extends LitTemplate {
    }

    @Tag("my-greedy-element")
    @JsModule("./frontend/MyGreedyLitElement.js")
    public class MyGreedyLitElement extends LitTemplate {
    }

    @Tag("my-lit-element-view")
    @JsModule("./frontend/my-lit-element-view.js")
    public class MyLitElementView extends LitTemplate {
    }

    @Tag("foo-view")
    @JsModule("/bar/foo.js")
    public class FooView extends LitTemplate {
    }

    @Tag("my-element")
    @JsModule("./frontend/MyElementFaultyMethods.js")
    public class MyFaulty extends LitTemplate {
    }

    @Tag("my-super-lit-element")
    @JsModule("./frontend/MySuperLitElement.js")
    public class MySuperLitElement extends LitTemplate {
    }

    @Tag("hello-world")
    @JsModule("./src/hello-world-lit.js")
    public class HelloWorld extends LitTemplate {
    }

    @Tag("hello-world")
    @JsModule("./src/hello-world2.js")
    public class HelloWorld2 extends LitTemplate {
    }

    @Tag("my-lit-element-view")
    @JsModule("./frontend/non-existent.js")
    @JsModule("./frontend/my-lit-element-view.js")
    public class BrokenJsModuleAnnotation extends LitTemplate {
    }

    @Tag("my-lit-element-view")
    @JsModule("./frontend/MyLitElement.js")
    @JsModule("./frontend/my-lit-element-view.js")
    public class SeveralJsModuleAnnotations extends LitTemplate {
    }

    @Tag("my-form")
    @JsModule("./frontend/my-form.ts")
    public class MyForm extends LitTemplate {
    }
}
