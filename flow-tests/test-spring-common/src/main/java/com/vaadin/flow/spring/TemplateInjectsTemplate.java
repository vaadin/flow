package com.vaadin.flow.spring;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.model.TemplateModel;
<<<<<<< HEAD
import com.vaadin.flow.polymertemplate.Id;
import com.vaadin.flow.polymertemplate.PolymerTemplate;
import com.vaadin.router.Route;
=======
import com.vaadin.flow.router.Route;
>>>>>>> refs/heads/master
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;

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