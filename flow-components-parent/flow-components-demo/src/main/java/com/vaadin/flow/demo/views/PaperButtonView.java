package com.vaadin.flow.demo.views;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.components.paper.button.PaperButton;
import com.vaadin.flow.html.Div;

@Tag("div")
@HtmlImport("frontend://bower_components/paper-button/paper-button.html")
public class PaperButtonView extends DemoView {

    public PaperButtonView() {
        Div div = new Div();
        add(div);

        PaperButton button = new PaperButton();
        button.getElement().setText("Button");
        div.add(button);
    }

    @Override
    public String getViewName() {
        return "Paper Button";
    }
}
