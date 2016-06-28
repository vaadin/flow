package com.vaadin.hummingbird.uitest.ui.scroll;

import com.vaadin.hummingbird.event.ComponentEventListener;
import com.vaadin.hummingbird.html.Anchor;
import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.H1;
import com.vaadin.hummingbird.html.event.ClickEvent;
import com.vaadin.hummingbird.uitest.ui.AbstractDivView;
import com.vaadin.server.Command;

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

        Div historyLength = new Div();
        historyLength.setText("historyPosition");
        historyLength.setWidth("100px");
        historyLength.setId("historylength");
        historyLength.addClickListener(event -> {
            getUI().get().getPage().executeJavaScript(
                    "document.getElementById('historylength').textContent = '' + (history.state ? history.state.historyPosition : 'nan')");
        });

        Div floatingMenu = new Div(new H1(getClass().getSimpleName()),
                createLink("ScrollView"), createLink("ScrollView#row3"),
                createLink("ScrollView2"), createLink("ScrollView2#row5"),
                createLink("ScrollView3"),
                new Anchor(
                        "com.vaadin.hummingbird.uitest.ui.scroll.ScrollView3#row10",
                        "ScrollView3#row10"),
                scrollY, historyLength);
        floatingMenu.getStyle().set("float", "right").set("position", "fixed")
                .set("right", "0").set("top", "20px");

        add(floatingMenu);

        for (int i = 1; i <= getRows(); i++) {
            Div div = new Div();
            div.setId("row" + i);
            div.setText("Row " + i);
            div.setHeight("500px");
            add(div);
        }
    }

    private static Button createButton(String id,
            ComponentEventListener<ClickEvent> listener) {
        Button button = new Button(id, listener);
        button.setId(id);
        return button;
    }

    private Button createActionButton(String text, Command command) {
        return createButton(text, e -> command.execute());
    }

    private Anchor createLink(String href) {
        Anchor a1 = new Anchor(
                "com.vaadin.hummingbird.uitest.ui.scroll." + href, href);
        a1.getElement().setAttribute("routerLink", true);
        a1.getStyle().set("display", "block");
        return a1;
    }

    protected int getRows() {
        return 3;
    }

}
