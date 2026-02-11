/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.TemplateParser;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.templatemodel.TemplateModel;

class DeprecatedIdTemplateComponentTest extends AbstractTemplateTest {

    private static final String TEMPLATE = " <template>  <div id='name'>{{name}}</div> </template>";

    @Tag("div")
    public static class EnabledDiv extends Component implements HasComponents {
    }

    @Test
    public void disableTemplateIdMappedComponentsOnTemplateDisabled() {
        Template template = new Template();
        template.setEnabled(false);
        getUI().add(template);

        Assertions.assertFalse(template.isEnabled(),
                "Template should be disabled");
        Assertions.assertFalse(template.getName().isEnabled(),
                "NameField should be disabled.");
        Assertions.assertNotNull(template.getElement().getAttribute("disabled"),
                "Attribute should have been added");
        Assertions.assertNotNull(
                template.getName().getElement().getAttribute("disabled"),
                "Attribute should have been added");

        template.setEnabled(true);

        Assertions.assertTrue(template.isEnabled(),
                "Template should be enabled");
        Assertions.assertTrue(template.getName().isEnabled(),
                "NameField should be enabled.");
        Assertions.assertNull(template.getElement().getAttribute("disabled"),
                "Attribute should have been removed");
        Assertions.assertNull(
                template.getName().getElement().getAttribute("disabled"),
                "Attribute should have been removed");
    }

    @Test
    public void templateIdMappedComponentDisabledStaysDisabled() {

        Template template = new Template();
        template.getName().setEnabled(false);
        getUI().add(template);

        Assertions.assertTrue(template.isEnabled(),
                "Template should be enabled");
        Assertions.assertNull(template.getElement().getAttribute("disabled"),
                "Attribute should not exist");
        Assertions.assertFalse(template.getName().isEnabled(),
                "NameField should be disabled.");
        Assertions.assertNotNull(
                template.getName().getElement().getAttribute("disabled"),
                "Attribute should not exist");

        template.setEnabled(false);

        Assertions.assertFalse(template.isEnabled(),
                "Template should be disabled");
        Assertions.assertFalse(template.getName().isEnabled(),
                "NameField should be disabled.");
        Assertions.assertNotNull(template.getElement().getAttribute("disabled"),
                "Attribute should have been added");
        Assertions.assertNotNull(
                template.getName().getElement().getAttribute("disabled"),
                "Attribute should have been added");

        template.setEnabled(true);

        Assertions.assertTrue(template.isEnabled(),
                "Template should be enabled");
        Assertions.assertNull(template.getElement().getAttribute("disabled"),
                "Attribute should have been removed");

        Assertions.assertFalse(template.getName().isEnabled(),
                "NameField should be disabled.");
        Assertions.assertNotNull(
                template.getName().getElement().getAttribute("disabled"),
                "Attribute should have been added");

    }

    @Test
    public void templatIdComponentChildrenGetEnabledCorrectly() {
        EnabledDiv child = new EnabledDiv();

        Template template = new Template();
        template.getName().add(child);

        getUI().add(template);

        Assertions.assertTrue(template.isEnabled(),
                "Template should be enabled");
        Assertions.assertTrue(template.getName().isEnabled(),
                "NameField should be enabled.");
        Assertions.assertTrue(child.isEnabled(),
                "NameField child should be enabled.");
        Assertions.assertNull(template.getElement().getAttribute("disabled"),
                "Attribute should not exist");
        Assertions.assertNull(
                template.getName().getElement().getAttribute("disabled"),
                "Attribute should not exist");
        Assertions.assertNull(child.getElement().getAttribute("disabled"),
                "Attribute should not exist");

        template.setEnabled(false);

        Assertions.assertFalse(template.isEnabled(),
                "Template should be disabled");
        Assertions.assertFalse(template.getName().isEnabled(),
                "NameField should be disabled.");
        Assertions.assertFalse(child.isEnabled(),
                "NameField child should be disabled.");
        Assertions.assertNotNull(template.getElement().getAttribute("disabled"),
                "Attribute should have been added");
        Assertions.assertNotNull(
                template.getName().getElement().getAttribute("disabled"),
                "Attribute should have been added");
        Assertions.assertNotNull(child.getElement().getAttribute("disabled"),
                "Attribute should have been added");

        template.setEnabled(true);
        template.getName().setEnabled(false);

        Assertions.assertTrue(template.isEnabled(),
                "Template should be enabled");
        Assertions.assertFalse(template.getName().isEnabled(),
                "NameField should be disabled.");
        Assertions.assertFalse(child.isEnabled(),
                "NameField child should be disabled.");
        Assertions.assertNull(template.getElement().getAttribute("disabled"),
                "Attribute should have been removed");
        Assertions.assertNotNull(
                template.getName().getElement().getAttribute("disabled"),
                "Attribute should have been added");
        Assertions.assertNotNull(child.getElement().getAttribute("disabled"),
                "Attribute should have been added");

    }

    static class TestTemplateParser implements TemplateParser {

        @Override
        public TemplateData getTemplateContent(
                Class<? extends PolymerTemplate<?>> clazz, String tag,
                VaadinService service) {
            Document doc = Jsoup.parse(TEMPLATE);
            return new TemplateData("",
                    doc.getElementsByTag("template").get(0));
        }

    }

    @Tag("registration-form")
    private static class Template extends PolymerTemplate<TemplateModel>
            implements HasEnabled {

        public Template() {
            super(new TestTemplateParser());
        }

        @Id
        private EnabledDiv name;

        public EnabledDiv getName() {
            return name;
        }

    }
}
