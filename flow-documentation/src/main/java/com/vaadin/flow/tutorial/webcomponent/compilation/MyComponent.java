package com.vaadin.flow.tutorial.webcomponent.compilation;

import com.vaadin.flow.component.HtmlImport;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.tutorial.webcomponent.compilation.MyComponent.MyModel;

@CodeFor("web-components/tutorial-webcomponents-es5.asciidoc")
@Tag("my-component")
@HtmlImport("components/my-component.html")
public class MyComponent extends PolymerTemplate<MyModel> {

    interface MyModel extends TemplateModel {

    }

}
