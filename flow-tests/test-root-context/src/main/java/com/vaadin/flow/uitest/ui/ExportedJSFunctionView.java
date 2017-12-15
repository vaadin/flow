package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.ExportedJSFunctionView", layout = ViewTestLayout.class)
@PageTitle("Exported JS function view")
public class ExportedJSFunctionView extends AbstractDivView {

    @Override
    protected void onShow() {
        Div version = new Div();
        version.setId("version");
        add(version);
        String client = "window.vaadin.clients[Object.keys(window.vaadin.clients)]";
        String versionJs = "var msg = '';" + "var versionInfoMethod = " + client
                + ".getVersionInfo;" + "if (versionInfoMethod) {"
                + "  msg += 'version: '+versionInfoMethod().flow;" + "} else {" //
                + "  msg += 'versionInfoMethod not published';" //
                + "}" //
                + "$0.innerHTML = msg;";
        getPage().executeJavaScript(versionJs, version);

        Div productionMode = new Div();
        productionMode.setId("productionMode");
        add(productionMode);
        String productionModeJs = "var productionMode = " + client+".productionMode;"
                + "$0.innerText = 'Production mode: '+productionMode;";
        getPage().executeJavaScript(productionModeJs, productionMode);
    }

}
