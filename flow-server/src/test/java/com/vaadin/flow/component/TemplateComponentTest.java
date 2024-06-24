/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.templatemodel.TemplateModel;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class TemplateComponentTest {

    UI ui;

    private static final String template_file = "<dom-module id='registration-form'>"
            + " <template>" + "  <div id='name'>{{name}}</div>" + " </template>"
            + " <script>" + "  class RegistrationForm extends Polymer.Element {"
            + "   static get is() {return 'registration-form'}" + "  }"
            + "  customElements.define(RegistrationForm.is, RegistrationForm);"
            + " </script>" + "</dom-module>";

    private MockServletServiceSessionSetup mocks;

    @Tag("div")
    public static class EnabledDiv extends Component implements HasComponents {
    }

    @Before
    public void init() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        mocks.getDeploymentConfiguration().setCompatibilityMode(true);

        mocks.getServlet().addServletContextResource("/registration-form.html",
                template_file);

        ui = new UI();
        ui.getInternals().setSession(mocks.getSession());

        CurrentInstance.setCurrent(ui);
    }

    @After
    public void tearDown() {
        mocks.cleanup();
    }

    @Test
    public void disableTemplateIdMappedComponentsOnTemplateDisabled() {

        Template template = new Template();
        template.setEnabled(false);
        ui.add(template);

        Assert.assertFalse("Template should be disabled", template.isEnabled());
        Assert.assertFalse("NameField should be disabled.",
                template.getName().isEnabled());
        Assert.assertNotNull("Attribute should have been added",
                template.getElement().getAttribute("disabled"));
        Assert.assertNotNull("Attribute should have been added",
                template.getName().getElement().getAttribute("disabled"));

        template.setEnabled(true);

        Assert.assertTrue("Template should be enabled", template.isEnabled());
        Assert.assertTrue("NameField should be enabled.",
                template.getName().isEnabled());
        Assert.assertNull("Attribute should have been removed",
                template.getElement().getAttribute("disabled"));
        Assert.assertNull("Attribute should have been removed",
                template.getName().getElement().getAttribute("disabled"));
    }

    @Test
    public void templateIdMappedComponentDisabledStaysDisabled() {

        Template template = new Template();
        template.getName().setEnabled(false);
        ui.add(template);

        Assert.assertTrue("Template should be enabled", template.isEnabled());
        Assert.assertNull("Attribute should not exist",
                template.getElement().getAttribute("disabled"));
        Assert.assertFalse("NameField should be disabled.",
                template.getName().isEnabled());
        Assert.assertNotNull("Attribute should not exist",
                template.getName().getElement().getAttribute("disabled"));

        template.setEnabled(false);

        Assert.assertFalse("Template should be disabled", template.isEnabled());
        Assert.assertFalse("NameField should be disabled.",
                template.getName().isEnabled());
        Assert.assertNotNull("Attribute should have been added",
                template.getElement().getAttribute("disabled"));
        Assert.assertNotNull("Attribute should have been added",
                template.getName().getElement().getAttribute("disabled"));

        template.setEnabled(true);

        Assert.assertTrue("Template should be enabled", template.isEnabled());
        Assert.assertNull("Attribute should have been removed",
                template.getElement().getAttribute("disabled"));

        Assert.assertFalse("NameField should be disabled.",
                template.getName().isEnabled());
        Assert.assertNotNull("Attribute should have been added",
                template.getName().getElement().getAttribute("disabled"));

    }

    @Test
    public void templatIdComponentChildrenGetEnabledCorrectly() {
        EnabledDiv child = new EnabledDiv();

        Template template = new Template();
        template.getName().add(child);

        ui.add(template);

        Assert.assertTrue("Template should be enabled", template.isEnabled());
        Assert.assertTrue("NameField should be enabled.",
                template.getName().isEnabled());
        Assert.assertTrue("NameField child should be enabled.",
                child.isEnabled());
        Assert.assertNull("Attribute should not exist",
                template.getElement().getAttribute("disabled"));
        Assert.assertNull("Attribute should not exist",
                template.getName().getElement().getAttribute("disabled"));
        Assert.assertNull("Attribute should not exist",
                child.getElement().getAttribute("disabled"));

        template.setEnabled(false);

        Assert.assertFalse("Template should be disabled", template.isEnabled());
        Assert.assertFalse("NameField should be disabled.",
                template.getName().isEnabled());
        Assert.assertFalse("NameField child should be disabled.",
                child.isEnabled());
        Assert.assertNotNull("Attribute should have been added",
                template.getElement().getAttribute("disabled"));
        Assert.assertNotNull("Attribute should have been added",
                template.getName().getElement().getAttribute("disabled"));
        Assert.assertNotNull("Attribute should have been added",
                child.getElement().getAttribute("disabled"));

        template.setEnabled(true);
        template.getName().setEnabled(false);

        Assert.assertTrue("Template should be enabled", template.isEnabled());
        Assert.assertFalse("NameField should be disabled.",
                template.getName().isEnabled());
        Assert.assertFalse("NameField child should be disabled.",
                child.isEnabled());
        Assert.assertNull("Attribute should have been removed",
                template.getElement().getAttribute("disabled"));
        Assert.assertNotNull("Attribute should have been added",
                template.getName().getElement().getAttribute("disabled"));
        Assert.assertNotNull("Attribute should have been added",
                child.getElement().getAttribute("disabled"));

    }

    @Tag("registration-form")
    @HtmlImport("/registration-form.html")
    private static class Template extends PolymerTemplate<TemplateModel>
            implements HasEnabled {
        @Id
        private EnabledDiv name;

        public EnabledDiv getName() {
            return name;
        }

    }
}
