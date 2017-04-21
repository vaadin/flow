package com.vaadin.flow.uitest.ui;

import com.vaadin.annotations.Title;
import com.vaadin.flow.html.Div;
import com.vaadin.server.FlowCustomElements;

@Title("Registered custom elements view")
public class CustomElementMappingView extends AbstractDivView {

    @Override
    protected void onShow() {
        removeAll();
        FlowCustomElements.customElements.keySet().forEach(key -> addKey(key));

    }

    private void addKey(String key) {
        Div titleView = new Div();
        titleView.setText(key);
        titleView.getElement().setAttribute("custom", true);
        add(titleView);
    }

}
