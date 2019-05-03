package com.vaadin.flow.uitest.ui;

import java.util.Random;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.PreserveOnRefreshView")
@PreserveOnRefresh
public class PreserveOnRefreshView extends Div {

    public PreserveOnRefreshView() {
        // create unique content for this instance
        setText(Long.toString(new Random().nextInt()));
        setId("contents");
    }

}
