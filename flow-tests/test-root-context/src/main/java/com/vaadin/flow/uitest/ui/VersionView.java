package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.PageTitle;
import com.vaadin.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.VersionView", layout = ViewTestLayout.class)
@PageTitle("Version view")
public class VersionView extends AbstractDivView {

    @Override
    protected void onShow() {
        setId("version");
        String client = "window.vaadin.clients[Object.keys(window.vaadin.clients)]";
        String js = "var msg = '';"
                + "var versionInfoMethod = "+client+".getVersionInfo;"
                + "if (versionInfoMethod) {"
                + "  msg += 'version: '+versionInfoMethod().flow;"
                + "} else {" //
                + "  msg += 'versionInfoMethod not published';" //
                + "}" //
                + "document.getElementById('version').innerHTML = msg;";
        getPage().executeJavaScript(js);
    }

}
