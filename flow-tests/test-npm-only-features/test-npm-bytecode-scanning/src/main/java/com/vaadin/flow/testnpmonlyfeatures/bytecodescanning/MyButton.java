package com.vaadin.flow.testnpmonlyfeatures.bytecodescanning;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("my-button")
@JsModule("./my-button.js")
public class MyButton extends PolymerTemplate<MyButton.MyButtonModel> {

    public interface MyButtonModel extends TemplateModel {

    }
}
