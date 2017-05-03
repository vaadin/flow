package com.vaadin.flow.uitest.ui.scroll;

import com.vaadin.flow.html.Anchor;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.html.H1;
import com.vaadin.flow.uitest.ui.AbstractDivView;

public class ScrollView extends AbstractDivView {

    public ScrollView() {
        Div scrollY = new Div();
        scrollY.setText("ScrollY:");
        scrollY.setWidth("100px");
        scrollY.setId("scrolly");
        scrollY.addClickListener(event -> {
            getUI().get().getPage().executeJavaScript(
                    "document.getElementById('scrolly').textContent = '' + window.scrollY;");
        });
        Div scrollX = new Div();
        scrollX.setText("ScrollX:");
        scrollX.setWidth("100px");
        scrollX.setId("scrollx");
        scrollX.addClickListener(event -> {
            getUI().get().getPage().executeJavaScript(
                    "document.getElementById('scrollx').textContent = '' + window.scrollX;");
        });

        Div historyLength = new Div();
        historyLength.setText("historyIndex");
        historyLength.setWidth("100px");
        historyLength.setId("historylength");
        historyLength.addClickListener(event -> {
            getUI().get().getPage().executeJavaScript(
                    "document.getElementById('historylength').textContent = '' + (history.state ? history.state.historyIndex : 'nan')");
        });

        Div floatingMenu = new Div(new H1(getClass().getSimpleName()),
                createLink("ScrollView"), createLink("ScrollView#row3"),
                createLink("ScrollView2"), createLink("ScrollView2#row5"),
                createLink("ScrollView3"),
                new Anchor(
                        "com.vaadin.flow.uitest.ui.scroll.ScrollView3#row10",
                        "ScrollView3#row10"),
                scrollY, scrollX, historyLength);
        floatingMenu.getStyle().set("float", "right").set("position", "fixed")
                .set("right", "0").set("top", "20px");

        add(floatingMenu);

        for (int i = 1; i <= getRows(); i++) {
            Div div = new Div();
            div.setId("row" + i);
            div.setText("Row " + i);
            div.setHeight("500px");
            div.setWidth("2000px");
            add(div);
        }
    }

    private Anchor createLink(String href) {
        Anchor a1 = new Anchor(
                "com.vaadin.flow.uitest.ui.scroll." + href, href);
        a1.getElement().setAttribute("router-link", true);
        a1.getStyle().set("display", "block");
        return a1;
    }

    protected int getRows() {
        return 3;
    }

}
