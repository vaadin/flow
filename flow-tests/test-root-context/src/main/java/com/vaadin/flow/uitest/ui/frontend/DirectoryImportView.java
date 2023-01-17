package com.vaadin.flow.uitest.ui.frontend;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;


@JsModule("importdir.js")
@Route(value = "com.vaadin.flow.uitest.ui.frontend.DirectoryImportView", layout = ViewTestLayout.class)
public class DirectoryImportView extends Div {

    @Tag("a-directory-component")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/ImportDir.html")
    public static class DirectoryComponent extends PolymerTemplate<TemplateModel> {

    }

    public DirectoryImportView() {
        add(new DirectoryComponent());
    }
}
