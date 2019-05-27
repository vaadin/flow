package com.vaadin.flow.uitest.ui.template.collections;

import java.util.List;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.template.collections.ToggleNullListView", layout = ViewTestLayout.class)
public class ToggleNullListView extends AbstractDivView {
    static String TOGGLE_BUTTON_ID = "toggleButton";

    public ToggleNullListView() {
        Div container = new Div();

        ServerModelNullListTemplate template = new ServerModelNullListTemplate();
        NativeButton button = createButton("Toggle template", TOGGLE_BUTTON_ID,
                event -> {
            if (template.getParent().isPresent()) {
                container.remove(template);
            } else {
                container.add(template);
            }
        });
        add(button, container);
    }

    @Tag("server-model-null-list")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/collections/ServerModelNullList.html")
    @JsModule("ServerModelNullList.js")
    public static class ServerModelNullListTemplate
            extends PolymerTemplate<ServerModelNullListTemplate.Model> {
        public interface Model extends TemplateModel {
            List<String> getNullList();

            void setNullList(List<String> nullList);
        }
    }
}
