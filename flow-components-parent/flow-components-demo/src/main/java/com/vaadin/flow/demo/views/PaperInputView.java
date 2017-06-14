package com.vaadin.flow.demo.views;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.components.paper.input.PaperInput;
import com.vaadin.flow.html.Div;

@Tag("div")
@HtmlImport("frontend://bower_components/paper-input/paper-input.html")
public class PaperInputView extends DemoView {

    public PaperInputView() {
        Div div = new Div();
        add(div);

        PaperInput input = new PaperInput();
        div.add(input);
    }

    @Override
    public String getViewName() {
        return "Paper Input";
    }

}
