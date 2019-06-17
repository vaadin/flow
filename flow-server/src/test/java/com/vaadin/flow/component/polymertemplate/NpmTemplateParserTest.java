package com.vaadin.flow.component.polymertemplate;

import java.util.Collections;
import java.util.Locale;

import org.jsoup.UncheckedIOException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.WebpackDevServerPort;
import com.vaadin.flow.templatemodel.TemplateModel;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;

public class NpmTemplateParserTest {
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
        Mockito.when(context.getAttribute(WebpackDevServerPort.class))
                .thenReturn(new WebpackDevServerPort(6666));
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
                "div", templateContent.getTemplateElement()
                        .getElementById("test").tag().toString());
    }

    @Test
    public void should_FindCorrectDataInBeverageStats() {
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenReturn(VAADIN_SERVLET_RESOURCES
                        + "config/stats-beverage.json");

        TemplateParser instance = NpmTemplateParser.getInstance();
        TemplateParser.TemplateData templateContent = instance
                .getTemplateContent(ReviewList.class, "likeable-element",
                        service);

        Assert.assertEquals("Parent element ID not the expected one.",
                "likeable-element",
                templateContent.getTemplateElement().parent().id());

        Assert.assertEquals("Expected template element to have 2 children", 2,
                templateContent.getTemplateElement().childNodeSize());

        Assert.assertEquals(
                "Template element should have contained a div element with the id 'search'",
                "vaadin-text-field", templateContent.getTemplateElement()
                        .getElementById("search").tag().toString());
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
                "div", templateContent.getTemplateElement()
                        .getElementById("test").tag().toString());
    }

    @Test(expected = UncheckedIOException.class)
    public void should_throwException_when_LocalFileNotFound() {
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
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
    public void sourceFileWithFaultyTemplateGetter_shouldJustReturnEmptyTemplateElement() {
        // If the template getter can not be found it should result in no
        // template element children
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenReturn(VAADIN_SERVLET_RESOURCES + "config/stats.json");
        TemplateParser.TemplateData templateContent = NpmTemplateParser
                .getInstance().getTemplateContent(LikeableFaulty.class,
                        "likeable-element", service);

        Assert.assertEquals("Faulty template getter should not find elements",
                0, templateContent.getTemplateElement().childNodeSize());
    }

    @Test
    public void faultyTemplateStyle_shouldReturnEmptyTemplateElement() {
        // Template with no closing style tag should parse as one big style tag
        // and thus
        // the document body should have no elements.
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenReturn(VAADIN_SERVLET_RESOURCES + "config/stats.json");
        TemplateParser.TemplateData templateContent = NpmTemplateParser
                .getInstance().getTemplateContent(LikeableBroken.class,
                        "likeable-element", service);

        Assert.assertEquals("Faulty html should not find elements", 0,
                templateContent.getTemplateElement().childNodeSize());
    }

    @Test
    public void bableStats_shouldAlwaysParseCorrectly() {
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString())).thenReturn(
                        VAADIN_SERVLET_RESOURCES + "config/babel_stats.json");
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
                "button", templateContent.getTemplateElement()
                        .getElementById("button").tag().toString());

        Assert.assertEquals(
                "Template element should have contained a div element with the id 'content'",
                "div", templateContent.getTemplateElement()
                        .getElementById("content").tag().toString());
    }

    /*
     * This example is for :
     *
     * @formatter:off
     * <pre>
     * <code>
     * static get template() {
        return html`
           <div>Parent Template</div>
            <div>
            <div>Placeholder</div>

            <child-template id="child"></child-template>

            </div>
            <style>

                parent-template {
                    width: 100%;
                }
            </style>
    `;
      }
      </code>
     * </pre>
     *
     * @formatter:on
     */
    @Test
    public void hierarchicalTemplate_templateHasChild_childHasCorrectPosition() {
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenReturn(VAADIN_SERVLET_RESOURCES
                        + "config/template-in-template-stats.json");
        TemplateParser instance = NpmTemplateParser.getInstance();
        TemplateParser.TemplateData templateContent = instance
                .getTemplateContent(ParentTemplate.class, "parent-template",
                        service);

        Element templateElement = templateContent.getTemplateElement();
        Assert.assertEquals(3, templateElement.children().size());
        Element parentDiv = templateElement.child(1);
        Assert.assertEquals("div",
                parentDiv.tag().getName().toLowerCase(Locale.ENGLISH));
        Elements children = parentDiv.children();
        Assert.assertEquals(2, children.size());
        Assert.assertEquals("div",
                parentDiv.child(0).tag().getName().toLowerCase(Locale.ENGLISH));
        Assert.assertEquals("child-template",
                parentDiv.child(1).tag().getName().toLowerCase(Locale.ENGLISH));
    }

    @Tag("likeable-element")
    @JsModule("./frontend/LikeableElement.js")
    public class Likeable extends PolymerTemplate<TemplateModel> {
    }

    @Tag("likeable-element")
    @JsModule("./frontend/LikeableElementFaultyMethods.js")
    public class LikeableFaulty extends PolymerTemplate<TemplateModel> {
    }

    @Tag("likeable-element")
    @JsModule("./frontend/LikeableElementBrokenHtml.js")
    public class LikeableBroken extends PolymerTemplate<TemplateModel> {
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

    @JsModule("./ParentTemplate.js")
    @Tag("parent-template")
    @Uses(ChildTemplate.class)
    public class ParentTemplate extends PolymerTemplate<TemplateModel> {

    }

    @JsModule("./ChildTemplate.js")
    @Tag("child-template")
    public class ChildTemplate extends PolymerTemplate<TemplateModel> {

    }
}
