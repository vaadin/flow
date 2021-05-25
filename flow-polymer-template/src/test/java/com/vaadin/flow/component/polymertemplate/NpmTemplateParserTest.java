/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.Locale;
import java.util.Properties;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.templatemodel.TemplateModel;

import static com.vaadin.flow.server.Constants.STATISTICS_JSON_DEFAULT;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;

public class NpmTemplateParserTest {

    private MockVaadinServletService service;
    @Mock
    private DeploymentConfiguration configuration;

    private ResourceProvider resourceProvider;

    @Before
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);

        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        Mockito.when(configuration.getFlowResourcesFolder()).thenReturn(
                "target/" + FrontendUtils.DEFAULT_FLOW_RESOURCES_FOLDER);

        Properties properties = new Properties();
        Mockito.when(configuration.getInitParameters()).thenReturn(properties);

        Instantiator instantiator = Mockito.mock(Instantiator.class);
        Mockito.when(instantiator.getServiceInitListeners())
                .thenReturn(Stream.empty());
        Mockito.when(instantiator.getDependencyFilters(Mockito.any()))
                .thenReturn(Stream.empty());
        Mockito.when(instantiator.getBootstrapListeners(Mockito.any()))
                .thenReturn(Stream.empty());
        Mockito.when(instantiator.getIndexHtmlRequestListeners(Mockito.any()))
                .thenReturn(Stream.empty());
        service = new MockVaadinServletService(configuration);
        service.init(instantiator);

        resourceProvider = service.getContext().getAttribute(Lookup.class)
                .lookup(ResourceProvider.class);
        Mockito.when(
                resourceProvider.getApplicationResource(Mockito.anyString()))
                .thenAnswer(invocation -> NpmTemplateParserTest.class
                        .getResource("/" + invocation.getArgument(0)));
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

        Assert.assertEquals("Expected template element to have 3 children", 3,
                templateContent.getTemplateElement().childNodeSize());

        Assert.assertEquals(
                "Template element should have contained a div element with the id 'test'",
                "div", templateContent.getTemplateElement()
                        .getElementById("test").tag().toString());
    }

    @Test
    public void getTemplateContent_polymer2TemplateStyleInsertion_contentParsedCorrectly() {
        ResourceProvider resourceProvider = service.getContext()
                .getAttribute(Lookup.class).lookup(ResourceProvider.class);
        Mockito.when(resourceProvider.getApplicationResource(
                VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn(NpmTemplateParser.class
                        .getResource("/" + VAADIN_SERVLET_RESOURCES
                                + "config/no-html-template.json"));

        TemplateParser parser = NpmTemplateParser.getInstance();
        TemplateData data = parser.getTemplateContent(
                NoHtmlTemplateContent.class, "no-html-template", service);
        Element templateElement = data.getTemplateElement();
        Assert.assertNotNull(templateElement);
        Elements divs = templateElement.getElementsByTag("div");
        Assert.assertEquals(1, divs.size());
        Assert.assertEquals("No Template", divs.get(0).text());
    }

    @Test
    public void getTemplateContent_polymer2TemplateStyleInsertion_severalDomModules_correctTemplateContentIsChosen() {
        ResourceProvider resourceProvider = service.getContext()
                .getAttribute(Lookup.class).lookup(ResourceProvider.class);
        Mockito.when(resourceProvider.getApplicationResource(
                VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn(NpmTemplateParser.class
                        .getResource("/" + VAADIN_SERVLET_RESOURCES
                                + "config/no-html-template.json"));

        TemplateParser parser = NpmTemplateParser.getInstance();
        TemplateData data = parser.getTemplateContent(
                SeveralDomModulesTemplateContent.class,
                "several-dom-modules-template", service);
        Element templateElement = data.getTemplateElement();
        Assert.assertNotNull(templateElement);
        Elements divs = templateElement.getElementsByTag("div");
        Assert.assertEquals(1, divs.size());
        Assert.assertEquals("Several Dom-Modules", divs.get(0).text());
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

    @Test
    public void getTypescriptTemplateContent_templateExists_getTemplateContent() {
        TemplateParser instance = NpmTemplateParser.getInstance();
        TemplateData templateContent = instance.getTemplateContent(MyForm.class,
                "my-form", service);

        Assert.assertEquals("Parent element ID not the expected one.",
                "my-form", templateContent.getTemplateElement().parent().id());

        Assert.assertEquals("Expected template element to have 2 children", 2,
                templateContent.getTemplateElement().childNodeSize());

        Assert.assertEquals(
                "Template element should have contained a div element with the id 'label'",
                "vaadin-text-field", templateContent.getTemplateElement()
                        .getElementById("nameField").tag().toString());
    }

    @Test(expected = IllegalStateException.class)
    public void should_throwException_when_LocalFileNotFound() {
        ResourceProvider resourceProvider = service.getContext()
                .getAttribute(Lookup.class).lookup(ResourceProvider.class);
        Mockito.when(resourceProvider.getApplicationResource(
                VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn(NpmTemplateParser.class
                        .getResource("/META-INF/resources/foo-bar.json"));
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
        TemplateParser.TemplateData templateContent = NpmTemplateParser
                .getInstance().getTemplateContent(LikeableBroken.class,
                        "likeable-element", service);

        Assert.assertEquals("Faulty html should not find elements", 0,
                templateContent.getTemplateElement().childNodeSize());
    }

    @Test
    public void nonLocalTemplate_shouldParseCorrectly() {
        TemplateParser.TemplateData templateContent = NpmTemplateParser
                .getInstance().getTemplateContent(HelloWorld.class,
                        HelloWorld.class.getAnnotation(Tag.class).value(),
                        service);

        Assert.assertEquals("Template should contain one child", 1,
                templateContent.getTemplateElement().childNodeSize());

        Assert.assertEquals("Template should have 2 divs", 2, templateContent
                .getTemplateElement().getElementsByTag("div").size());
        Assert.assertEquals("Template should have a paper-input", 1,
                templateContent.getTemplateElement()
                        .getElementsByTag("paper-input").size());
        Assert.assertEquals("Template should have a button", 1, templateContent
                .getTemplateElement().getElementsByTag("button").size());
    }

    @Test
    public void bableStats_shouldAlwaysParseCorrectly() {
        ResourceProvider resourceProvider = service.getContext()
                .getAttribute(Lookup.class).lookup(ResourceProvider.class);
        Mockito.when(resourceProvider.getApplicationResource(
                VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn(NpmTemplateParser.class
                        .getResource("/" + VAADIN_SERVLET_RESOURCES
                                + "config/babel_stats.json"));
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
        ResourceProvider resourceProvider = service.getContext()
                .getAttribute(Lookup.class).lookup(ResourceProvider.class);
        Mockito.when(resourceProvider.getApplicationResource(
                VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn(NpmTemplateParser.class
                        .getResource("/" + VAADIN_SERVLET_RESOURCES
                                + "config/template-in-template-stats.json"));
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

    @Test
    public void severalJsModuleAnnotations_theFirstFileDoesNotExist_fileWithContentIsChosen() {
        TemplateParser instance = NpmTemplateParser.getInstance();
        TemplateParser.TemplateData templateContent = instance
                .getTemplateContent(BrokenJsModuleAnnotation.class,
                        "likeable-element-view", service);

        Assert.assertEquals("Parent element ID not the expected one.",
                "likeable-element-view",
                templateContent.getTemplateElement().parent().id());
    }

    @Test
    public void severalJsModuleAnnotations_parserSelectsByName() {
        TemplateParser instance = NpmTemplateParser.getInstance();
        TemplateParser.TemplateData templateContent = instance
                .getTemplateContent(SeveralJsModuleAnnotations.class,
                        "likeable-element-view", service);

        Assert.assertEquals("Parent element ID not the expected one.",
                "likeable-element-view",
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

    @Tag("hello-world")
    @JsModule("./src/hello-world.js")
    public class HelloWorld extends PolymerTemplate<TemplateModel> {
    }

    @Tag("likeable-element")
    @JsModule("./frontend/likeable-element-view.js")
    public class LikeableView extends PolymerTemplate<TemplateModel> {
    }

    @Tag("likeable-element-view")
    @JsModule("./frontend/LikeableElement.js")
    @JsModule("./frontend/likeable-element-view.js")
    public class SeveralJsModuleAnnotations
            extends PolymerTemplate<TemplateModel> {
    }

    @Tag("likeable-element-view")
    @JsModule("./frontend/non-existant.js")
    @JsModule("./frontend/likeable-element-view.js")
    public class BrokenJsModuleAnnotation
            extends PolymerTemplate<TemplateModel> {
    }

    @Tag("review-list")
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

    @JsModule("./no-html-template.js")
    @Tag("no-template")
    public class NoHtmlTemplateContent extends PolymerTemplate<TemplateModel> {

    }

    @JsModule("./several-dom-modules-template.js")
    @Tag("several-dom-modules-template")
    public class SeveralDomModulesTemplateContent
            extends PolymerTemplate<TemplateModel> {

    }

    @Tag("my-form")
    @JsModule("./frontend/my-form.ts")
    public class MyForm extends PolymerTemplate<TemplateModel> {
    }

}
