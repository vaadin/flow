package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.template.InjectScriptTagView.InjectionModel;

@Route(value = "com.vaadin.flow.uitest.ui.template.InjectScriptTagView", layout = ViewTestLayout.class)
@Tag("inject-script-tag-template")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/InjectScriptTagTemplate.html")
public class InjectScriptTagView extends PolymerTemplate<InjectionModel> {

    public interface InjectionModel extends TemplateModel {
        String getValue();

        void setValue(String value);
    }

    public InjectScriptTagView() {
        getModel().setValue("<!-- <script>");
        getElement()
                .appendChild(new Text("<!-- <script> --><!-- <script></script>")
                        .getElement());
    }

    @ClientCallable
    private void changeValue() {
        getModel().setValue("<!-- <SCRIPT>");
        getElement().removeAllChildren();
        getElement()
                .appendChild(new Text("<!-- <script> --><!-- <script></script>")
                        .getElement());
    }

}
