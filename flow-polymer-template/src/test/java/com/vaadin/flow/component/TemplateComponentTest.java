/**
 * Copyright (C) 2022-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.TemplateParser;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.templatemodel.TemplateModel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateComponentTest extends AbstractTemplateTest {

    private static final String TEMPLATE = " <template>  <div id='name'>{{name}}</div> </template>";

    @Tag("div")
    public static class EnabledDiv extends Component implements HasComponents {
    }

    @Test
    void disableTemplateIdMappedComponentsOnTemplateDisabled() {

        Template template = new Template();
        template.setEnabled(false);
        getUI().add(template);

        assertFalse(template.isEnabled(), "Template should be disabled");
        assertFalse(template.getName().isEnabled(),
                "NameField should be disabled.");
        assertNotNull(template.getElement().getAttribute("disabled"),
                "Attribute should have been added");
        assertNotNull(template.getName().getElement().getAttribute("disabled"),
                "Attribute should have been added");

        template.setEnabled(true);

        assertTrue(template.isEnabled(), "Template should be enabled");
        assertTrue(template.getName().isEnabled(),
                "NameField should be enabled.");
        assertNull(template.getElement().getAttribute("disabled"),
                "Attribute should have been removed");
        assertNull(template.getName().getElement().getAttribute("disabled"),
                "Attribute should have been removed");
    }

    @Test
    void templateIdMappedComponentDisabledStaysDisabled() {

        Template template = new Template();
        template.getName().setEnabled(false);
        getUI().add(template);

        assertTrue(template.isEnabled(), "Template should be enabled");
        assertNull(template.getElement().getAttribute("disabled"),
                "Attribute should not exist");
        assertFalse(template.getName().isEnabled(),
                "NameField should be disabled.");
        assertNotNull(template.getName().getElement().getAttribute("disabled"),
                "Attribute should not exist");

        template.setEnabled(false);

        assertFalse(template.isEnabled(), "Template should be disabled");
        assertFalse(template.getName().isEnabled(),
                "NameField should be disabled.");
        assertNotNull(template.getElement().getAttribute("disabled"),
                "Attribute should have been added");
        assertNotNull(template.getName().getElement().getAttribute("disabled"),
                "Attribute should have been added");

        template.setEnabled(true);

        assertTrue(template.isEnabled(), "Template should be enabled");
        assertNull(template.getElement().getAttribute("disabled"),
                "Attribute should have been removed");

        assertFalse(template.getName().isEnabled(),
                "NameField should be disabled.");
        assertNotNull(template.getName().getElement().getAttribute("disabled"),
                "Attribute should have been added");

    }

    @Test
    void templatIdComponentChildrenGetEnabledCorrectly() {
        EnabledDiv child = new EnabledDiv();

        Template template = new Template();
        template.getName().add(child);

        getUI().add(template);

        assertTrue(template.isEnabled(), "Template should be enabled");
        assertTrue(template.getName().isEnabled(),
                "NameField should be enabled.");
        assertTrue(child.isEnabled(), "NameField child should be enabled.");
        assertNull(template.getElement().getAttribute("disabled"),
                "Attribute should not exist");
        assertNull(template.getName().getElement().getAttribute("disabled"),
                "Attribute should not exist");
        assertNull(child.getElement().getAttribute("disabled"),
                "Attribute should not exist");

        template.setEnabled(false);

        assertFalse(template.isEnabled(), "Template should be disabled");
        assertFalse(template.getName().isEnabled(),
                "NameField should be disabled.");
        assertFalse(child.isEnabled(), "NameField child should be disabled.");
        assertNotNull(template.getElement().getAttribute("disabled"),
                "Attribute should have been added");
        assertNotNull(template.getName().getElement().getAttribute("disabled"),
                "Attribute should have been added");
        assertNotNull(child.getElement().getAttribute("disabled"),
                "Attribute should have been added");

        template.setEnabled(true);
        template.getName().setEnabled(false);

        assertTrue(template.isEnabled(), "Template should be enabled");
        assertFalse(template.getName().isEnabled(),
                "NameField should be disabled.");
        assertFalse(child.isEnabled(), "NameField child should be disabled.");
        assertNull(template.getElement().getAttribute("disabled"),
                "Attribute should have been removed");
        assertNotNull(template.getName().getElement().getAttribute("disabled"),
                "Attribute should have been added");
        assertNotNull(child.getElement().getAttribute("disabled"),
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
