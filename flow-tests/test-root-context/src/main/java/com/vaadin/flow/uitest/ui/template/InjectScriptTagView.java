package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.template.InjectScriptTagView.InjectionModel;

@Route(value = "com.vaadin.flow.uitest.ui.template.InjectScriptTagView", layout = ViewTestLayout.class)
@Tag("inject-script-tag-template")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/InjectScriptTagTemplate.html")
@JsModule("InjectScriptTagTemplate.js")
public class InjectScriptTagView extends PolymerTemplate<InjectionModel> {

    public interface InjectionModel extends TemplateModel {
        String getValue();

        void setValue(String value);
    }

    public InjectScriptTagView() {
        getModel().setValue("<!-- <script>");
        Div slot = new Div(new Text("<!-- <script> --><!-- <script></script>"));
        slot.setId("slot-1");
        getElement().appendChild(slot.getElement());
    }

    @ClientCallable
    private void changeValue() {
        getModel().setValue("<!-- <SCRIPT>");
        getElement().removeAllChildren();
        Div slot = new Div(new Text("<!-- <SCRIPT> --><!-- <SCRIPT></SCRIPT>"));
        slot.setId("slot-2");
        getElement().appendChild(slot.getElement());
    }

}
