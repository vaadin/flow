package com.vaadin.flow.misc.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@Route("preserve-on-refresh-title-view")
@PreserveOnRefresh
@PageTitle("Initial Title")
public class PreserveOnRefreshTitleView extends Div {

    public PreserveOnRefreshTitleView() {
        NativeButton updateTitle = new NativeButton("Update Title", e -> {
            getUI().ifPresent(ui -> ui.getPage().setTitle("Updated Title"));
        });
        updateTitle.setId("update-title");
        add(updateTitle);
    }
}
