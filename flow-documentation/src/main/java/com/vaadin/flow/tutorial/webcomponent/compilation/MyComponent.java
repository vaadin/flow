package com.vaadin.flow.tutorial.webcomponent.compilation;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.tutorial.webcomponent.compilation.MyComponent.MyModel;

@CodeFor("tutorial-webcomponents-es5.asciidoc")
@Tag("my-component")
@HtmlImport("frontend://components/my-component.html")
public class MyComponent extends PolymerTemplate<MyModel> {

    interface MyModel extends TemplateModel {

    }

}
