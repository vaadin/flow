/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component.littemplate;

import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;

public class LitTemplateTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private VaadinService service = Mockito.mock(VaadinService.class);

    @Tag("foo-bar")
    private static class TestLitTemplate extends LitTemplate {

        @Id("labelId")
        private com.vaadin.flow.dom.Element label;

        public TestLitTemplate(VaadinService service) {
            this((clazz, tag, svc) -> new LitTemplateParser.TemplateData("",
                    Jsoup.parse("<foo-bar id='" + tag
                            + "'><label id='labelId' someattribute .property-binding='foo' "
                            + "?attribute-binding='bar' another-binding='${bar}' "
                            + "another-attribute='baz' hidden></foo-bar>")),
                    service);
        }

        TestLitTemplate(LitTemplateParser parser, VaadinService service) {
            super(parser, service);
        }

    }

    @Tag("foo-bar")
    private static class DisabledElementTemplate extends LitTemplate {

        @Id("labelId")
        private Element label;

        public DisabledElementTemplate(VaadinService service) {
            this((clazz, tag, svc) -> new LitTemplateParser.TemplateData("",
                    Jsoup.parse("<foo-bar id='" + tag
                            + "'><label id='labelId' disabled></foo-bar>")),
                    service);
        }

        DisabledElementTemplate(LitTemplateParser parser,
                VaadinService service) {
            super(parser, service);
        }
    }

    @Tag("foo-bar")
    private static class ElementWithTextLitTemplate extends LitTemplate {

        @Id("labelId")
        private com.vaadin.flow.dom.Element label;

        @Id("hasHierarchy")
        private com.vaadin.flow.dom.Element div;

        public ElementWithTextLitTemplate(VaadinService service) {
            this((clazz, tag, svc) -> new LitTemplateParser.TemplateData("",
                    Jsoup.parse("<dom-module id='" + tag
                            + "'><label id='labelId'>foo bar</label>"
                            + "<div id='hasHierarchy'>baz <a>foo</a> bar</div></dom-module>")),
                    service);
        }

        ElementWithTextLitTemplate(LitTemplateParser parser,
                VaadinService service) {
            super(parser, service);
        }

    }

    // This is for checking LitTemplate implements HasStyle
    @Tag("custom-element-with-class")
    private static class ElementWithStyleClass extends LitTemplate {
        ElementWithStyleClass() {
            // Should be possible to modify the class name
            addClassName("custom-element-class-name");
        }
    }

    @Before
    public void setUp() {
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
    }

    @Test
    public void attachExistingElementWithAttributeValue_elementHasAttribute() {
        TestLitTemplate template = new TestLitTemplate(service);

        Assert.assertTrue(template.label.hasAttribute("id"));
        Assert.assertEquals("labelId", template.label.getAttribute("id"));

        Assert.assertTrue(template.label.hasProperty("someattribute"));
        Assert.assertNotNull(template.label.getProperty("someattribute"));
        Assert.assertEquals(Boolean.TRUE.toString(),
                template.label.getProperty("someattribute"));

        Assert.assertFalse(template.label.hasProperty("property-binding"));
        Assert.assertFalse(template.label.hasProperty("propertyBinding"));

        Assert.assertFalse(template.label.hasProperty("another-binding"));
        Assert.assertFalse(template.label.hasProperty("anotherBinding"));

        Assert.assertFalse(template.label.hasProperty("attribute-binding"));
        Assert.assertFalse(template.label.hasProperty("attributeBinding"));
        Assert.assertFalse(template.label.hasProperty("attribute-binding$"));
        Assert.assertFalse(template.label.hasProperty("attributeBinding$"));

        Assert.assertTrue(template.label.hasProperty("another-attribute"));
        Assert.assertEquals("baz",
                template.label.getProperty("another-attribute"));

        Assert.assertTrue(template.label.hasAttribute("hidden"));
        Assert.assertEquals(Boolean.TRUE.toString(),
                template.label.getAttribute("hidden"));
    }

    @Test
    public void attachExistingElementWithDisabledAttributeValue_exceptionIsThrown() {
        expectedEx.expect(IllegalAttributeException.class);
        expectedEx.expectMessage(
                Matchers.containsString("element 'label' with id 'labelId'"));

        DisabledElementTemplate template = new DisabledElementTemplate(service);
    }

    @Test
    public void attachExistingElementWithoutChildrenWithText_elementHasNoText() {
        ElementWithTextLitTemplate template = new ElementWithTextLitTemplate(
                service);

        // see #10106
        Assert.assertEquals("", template.label.getText());
    }

    @Test
    public void attachExistingElementWithChildrenWithText_elementHasNoText() {
        ElementWithTextLitTemplate template = new ElementWithTextLitTemplate(
                service);

        Assert.assertEquals("", template.div.getText());
    }

}
