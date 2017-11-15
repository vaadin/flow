package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.PageTitle;
import com.vaadin.router.Route;
import com.vaadin.ui.html.Div;

@Route(value = "com.vaadin.flow.uitest.ui.VersionView", layout = ViewTestLayout.class)
@PageTitle("Version view")
public class VersionView extends AbstractDivView {

    @Override
    protected void onShow() {
        setId("version");
        getPage().executeJavaScript(
                "document.getElementById('version').innerText = 'version: '+window.vaadin.clients.view.getVersionInfo().flow");
    }

}
