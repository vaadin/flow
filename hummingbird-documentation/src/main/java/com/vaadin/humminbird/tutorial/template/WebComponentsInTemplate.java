package com.vaadin.humminbird.tutorial.template;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.Uses;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.html.HtmlComponent;
import com.vaadin.ui.Template;

@CodeFor("tutorial-template-webcomponents.asciidoc")
public class WebComponentsInTemplate {
    @Uses(PaperInput.class)
    @Uses(PaperTextarea.class)
    @Uses(VaadinDatePicker.class)
    class Form extends Template {
        // Implementation omitted
    }

    @Tag("paper-input")
    @HtmlImport("bower_components/paper-input/paper-input.html")
    class PaperInput extends HtmlComponent {
    }

    @Tag("paper-textarea")
    @HtmlImport("bower_components/paper-textarea/paper-textarea.html")
    class PaperTextarea extends HtmlComponent {
    }

    @Tag("vaadin-combo-box")
    @HtmlImport("bower_components/vaadin-date-picker/vaadin-date-picker.html")
    class VaadinDatePicker extends HtmlComponent {
    }
}
