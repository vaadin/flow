package com.vaadin.flow.tutorial.webcomponent.compilation;

import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.polymertemplate.PolymerTemplate;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.tutorial.webcomponent.compilation.MyComponent.MyModel;

@CodeFor("web-components/tutorial-webcomponents-es5.asciidoc")
@Tag("my-component")
@HtmlImport("frontend://components/my-component.html")
public class MyComponent extends PolymerTemplate<MyModel> {

    interface MyModel extends TemplateModel {

    }

}
