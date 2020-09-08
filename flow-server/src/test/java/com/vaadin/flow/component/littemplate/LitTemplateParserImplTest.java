package com.vaadin.flow.component.littemplate;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;

import java.util.Collections;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplateParser.TemplateData;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

public class LitTemplateParserImplTest {

    @Mock
    VaadinContext context;
    @Mock
    VaadinService service;
    @Mock
    DeploymentConfiguration configuration;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(service.getDependencyFilters())
                .thenReturn(Collections.emptyList());
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        Mockito.when(service.getClassLoader())
                .thenAnswer(invocation -> this.getClass().getClassLoader());
        Mockito.when(service.getContext()).thenReturn(context);
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenAnswer((Answer<String>) invocation -> {
                    Object[] args = invocation.getArguments();
                    return (String) args[1];
                });
    }

    @Test
    public void getTemplateContent_rootElementParsed() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        TemplateData templateContent = instance
                .getTemplateContent(MyLitElement.class, "my-element", service);

        Assert.assertEquals("Parent element ID not the expected one.",
                "my-element",
                templateContent.getTemplateElement().parent().id());

        Assert.assertEquals("Expected template element to have 2 children", 2,
                templateContent.getTemplateElement().childNodeSize());

        Assert.assertEquals(
                "Template element should have contained a div element with the id 'test'",
                "div", templateContent.getTemplateElement()
                        .getElementById("test").tag().toString());
    }

    @Test
    public void getTemplateContent_localFileTemplateExists_useLocalFileContent() {
        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        TemplateData templateContent = instance.getTemplateContent(
                MyLitElementView.class, "my-lit-element-view", service);

        Assert.assertEquals("Parent element ID not the expected one.",
                "my-lit-element-view",
                templateContent.getTemplateElement().parent().id());

        Assert.assertEquals("Expected template element to have 3 children", 3,
                templateContent.getTemplateElement().childNodeSize());

        Assert.assertEquals(
                "Template element should have contained a div element with the id 'test'",
                "div", templateContent.getTemplateElement()
                        .getElementById("label").tag().toString());
    }

    @Test
    public void getTemplateContent_localFileNotFound_returnsNull() {
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenReturn("META-INF/resources/foo-bar.json");
        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        Assert.assertNull(instance.getTemplateContent(FooView.class, "foo-view",
                service));
    }

    @Test
    public void getTemplateContent_sourceNotFoundInStatsFile_returnsNull() {
        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        Assert.assertNull(instance.getTemplateContent(FooView.class, "foo-view",
                service));
    }

    @Test
    public void getTemplateContent_sourceFileWithFaultyTemplateGetter_returnsNull() {
        // If the template getter can not be found it should result in no
        // template element children
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenReturn(VAADIN_SERVLET_RESOURCES + "config/stats.json");
        LitTemplateParser.TemplateData templateContent = LitTemplateParserImpl
                .getInstance()
                .getTemplateContent(MyFaulty.class, "my-element", service);

        Assert.assertNull(templateContent);
    }

    @Test
    public void getTemplateContent_renderIsDefinedInSuperClass_returnsNull() {
        // If the template getter can not be found it should result in no
        // template element children
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenReturn(VAADIN_SERVLET_RESOURCES + "config/stats.json");
        LitTemplateParser.TemplateData templateContent = LitTemplateParserImpl
                .getInstance().getTemplateContent(MyFaulty.class,
                        "my-super-lit-element", service);

        Assert.assertNull(templateContent);
    }

    @Test
    public void getTemplateContent_nonLocalTemplate_rootElementParsed() {
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenReturn(VAADIN_SERVLET_RESOURCES + "config/stats.json");
        LitTemplateParser.TemplateData templateContent = LitTemplateParserImpl
                .getInstance().getTemplateContent(HelloWorld.class,
                        HelloWorld.class.getAnnotation(Tag.class).value(),
                        service);

        Assert.assertEquals("Template should contain one child", 2,
                templateContent.getTemplateElement().childNodeSize());

        Assert.assertEquals("Template should have 3 divs", 3, templateContent
                .getTemplateElement().getElementsByTag("div").size());
    }

    @Test
    public void severalJsModuleAnnotations_theFirstFileDoesNotExist_fileWithContentIsChosen() {
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenReturn(VAADIN_SERVLET_RESOURCES + "config/stats.json");

        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        LitTemplateParser.TemplateData templateContent = instance
                .getTemplateContent(BrokenJsModuleAnnotation.class,
                        "my-lit-element-view", service);

        Assert.assertEquals("Parent element ID not the expected one.",
                "my-lit-element-view",
                templateContent.getTemplateElement().parent().id());
    }

    @Test
    public void severalJsModuleAnnotations_parserSelectsByName() {
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenReturn(VAADIN_SERVLET_RESOURCES + "config/stats.json");

        LitTemplateParser instance = LitTemplateParserImpl.getInstance();
        LitTemplateParser.TemplateData templateContent = instance
                .getTemplateContent(SeveralJsModuleAnnotations.class,
                        "my-lit-element-view", service);

        Assert.assertEquals("Parent element ID not the expected one.",
                "my-lit-element-view",
                templateContent.getTemplateElement().parent().id());

        // Two JS module annotations with almost the same content.
        // The first one contains a string "Tag name doesn't match the JS module
        // name", the second one doesn't contain this string.
        // The second module should be chosen since its name matches the tag
        // name
        Assert.assertThat(templateContent.getTemplateElement().html(),
                CoreMatchers.not(CoreMatchers.containsString(
                        "Tag name doesn't match the JS module name")));
    }

    @Tag("my-element")
    @JsModule("./frontend/MyLitElement.js")
    public class MyLitElement extends LitTemplate {
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

    @Tag("my-lit-element-view")
    @JsModule("./frontend/non-existant.js")
    @JsModule("./frontend/my-lit-element-view.js")
    public class BrokenJsModuleAnnotation extends LitTemplate {
    }

    @Tag("my-lit-element-view")
    @JsModule("./frontend/MyLitElement.js")
    @JsModule("./frontend/my-lit-element-view.js")
    public class SeveralJsModuleAnnotations extends LitTemplate {
    }

}
