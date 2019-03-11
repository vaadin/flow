package com.vaadin.flow.component.polymertemplate;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.templatemodel.TemplateModel;

public class NpmTemplateParserTest {
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
        Mockito.when(configuration
                .getStringProperty(Mockito.anyString(), Mockito.anyString()))
                .thenAnswer((Answer<String>) invocation -> {
                    Object[] args = invocation.getArguments();
                    return (String) args[1];
                });
    }

    @Test
    public void should_FindCorrectDataInStats() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        TemplateParser instance = NpmTemplateParser.getInstance();
        TemplateParser.TemplateData templateContent = instance
                .getTemplateContent(Likeable.class, "likeable-element",
                        service);

        Assert.assertEquals("Parent element ID not the expected one.",
                "likeable-element",
                templateContent.getTemplateElement().parent().id());

        Assert.assertEquals("Expected template element to have 2 children", 2,
                templateContent.getTemplateElement().childNodeSize());

        Assert.assertEquals(
                "Template element should have contained a div element with the id 'test'",
                "div",
                templateContent.getTemplateElement().getElementById("test")
                        .tag().toString());
    }

    @Test
    public void should_FindCorrectDataInBeverageStats() {
        Mockito.when(configuration
                .getStringProperty(Mockito.anyString(), Mockito.anyString()))
                .thenReturn("/build/stats-beverage.json");

        TemplateParser instance = NpmTemplateParser.getInstance();
        TemplateParser.TemplateData templateContent = instance
                .getTemplateContent(ReviewList.class, "likeable-element",
                        service);

        Assert.assertEquals("Parent element ID not the expected one.",
                "likeable-element",
                templateContent.getTemplateElement().parent().id());

        Assert.assertEquals("Expected template element to have 29 children", 29,
                templateContent.getTemplateElement().childNodeSize());

        Assert.assertEquals(
                "Template element should have contained a div element with the id 'search'",
                "vaadin-text-field",
                templateContent.getTemplateElement().getElementById("search")
                        .tag().toString());
    }

    @Test
    public void shouldnt_UseStats_when_LocalFileTemplateExists() {
        TemplateParser instance = NpmTemplateParser.getInstance();
        TemplateParser.TemplateData templateContent = instance
                .getTemplateContent(LikeableView.class, "likeable-element-view",
                        service);

        Assert.assertEquals("Parent element ID not the expected one.",
                "likeable-element-view",
                templateContent.getTemplateElement().parent().id());

        Assert.assertEquals("Expected template element to have 2 children", 2,
                templateContent.getTemplateElement().childNodeSize());

        Assert.assertEquals(
                "Template element should have contained a div element with the id 'test'",
                "div",
                templateContent.getTemplateElement().getElementById("test")
                        .tag().toString());
    }

    @Test(expected = IllegalStateException.class)
    public void should_throwException_when_LocalFileNotFound() {
        Mockito.when(configuration
                .getStringProperty(Mockito.anyString(), Mockito.anyString()))
                .thenReturn("META-INF/resources/foo-bar.json");
        TemplateParser instance = NpmTemplateParser.getInstance();
        instance.getTemplateContent(FooView.class, "foo-view", service);
    }

    @Test(expected = IllegalStateException.class)
    public void should_throwException_when_ResourceNotFoundInStatsFile() {
        TemplateParser instance = NpmTemplateParser.getInstance();
        instance.getTemplateContent(FooView.class, "foo-view", service);
    }

    @Test
    public void bableStats_shouldAlwaysParseCorrectly() {
        Mockito.when(configuration.getStringProperty(Mockito.anyString(), Mockito.anyString()))
                .thenReturn("build/babel_stats.json");
        TemplateParser instance = NpmTemplateParser.getInstance();
        TemplateParser.TemplateData templateContent = instance
                .getTemplateContent(MyComponent.class, "my-component", service);

        Assert.assertEquals("Parent element ID not the expected one.",
                "my-component",
                templateContent.getTemplateElement().parent().id());

        Assert.assertEquals("Expected template element to have 2 children", 2,
                templateContent.getTemplateElement().childNodeSize());

        Assert.assertEquals(
                "Template element should have contained a div element with the id 'button'",
                "button",
                templateContent.getTemplateElement().getElementById("button")
                        .tag().toString());

        Assert.assertEquals(
                "Template element should have contained a div element with the id 'content'",
                "div",
                templateContent.getTemplateElement().getElementById("content")
                        .tag().toString());
    }

    @Tag("likeable-element")
    @JsModule("./frontend/LikeableElement.js")
    public class Likeable extends PolymerTemplate<TemplateModel> {
    }

    @Tag("likeable-element")
    @JsModule("./frontend/likeable-element-view.js")
    public class LikeableView extends PolymerTemplate<TemplateModel> {
    }

    @Tag("review-list")
    @HtmlImport("frontend://src/views/reviewslist/reviews-list.html")
    @JsModule("./src/views/reviewslist/reviews-list")
    public class ReviewList extends PolymerTemplate<TemplateModel> {
    }

    @Tag("foo-view")
    @JsModule("/bar/foo.js")
    public class FooView extends PolymerTemplate<TemplateModel> {
    }

    @Tag("likeable-element")
    @JsModule("./my-component.js")
    public class MyComponent extends PolymerTemplate<TemplateModel> {
    }
}
