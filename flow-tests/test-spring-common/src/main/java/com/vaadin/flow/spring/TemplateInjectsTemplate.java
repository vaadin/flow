package com.vaadin.flow.spring;

import com.vaadin.flow.model.TemplateModel;
import com.vaadin.router.Route;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.polymertemplate.Id;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

@Tag("parent-template")
@HtmlImport("/components/ParentTemplate.html")
@Route("parent-template")
public class TemplateInjectsTemplate extends PolymerTemplate<TemplateModel> {

    @Id("child")
    private ChildTemplate template;

    @Id("div")
    private Div div;

    public TemplateInjectsTemplate() {
        template.getElement().setProperty("foo", "bar");
        div.setText("baz");
    }
}