package com.vaadin.flow.uitest.ui.frontend;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@JsModule("./importdir.js")
@Route(value = "com.vaadin.flow.uitest.ui.frontend.DirectoryImportView", layout = ViewTestLayout.class)
public class DirectoryImportView extends Div {

    @Tag("a-directory-component")
    public static class DirectoryComponent extends Component {

    }

    public DirectoryImportView() {
        add(new DirectoryComponent());
    }
}
