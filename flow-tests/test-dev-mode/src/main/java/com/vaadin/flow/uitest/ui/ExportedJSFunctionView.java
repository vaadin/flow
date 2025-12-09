/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ExportedJSFunctionView", layout = ViewTestLayout.class)
@PageTitle("Exported JS function view")
public class ExportedJSFunctionView extends AbstractDivView {

    private int pollCount = 0;

    @Override
    protected void onShow() {
        Div version = new Div();
        version.setId("version");
        add(version);
        String client = "window.Vaadin.Flow.clients[Object.keys(Vaadin.Flow.clients).filter(k => k !== 'TypeScript')]";
        String versionJs = "var msg = '';" + "var versionInfoMethod = " + client
                + ".getVersionInfo;" + "if (versionInfoMethod) {"
                + "  msg += 'version: '+versionInfoMethod().flow;" + "} else {" //
                + "  msg += 'versionInfoMethod not published';" //
                + "}" //
                + "$0.innerHTML = msg;";
        getPage().executeJs(versionJs, version);

        Div productionMode = new Div();
        productionMode.setId("productionMode");
        add(productionMode);
        String productionModeJs = "var productionMode = " + client
                + ".productionMode;"
                + "$0.innerText = 'Production mode: '+productionMode;";
        getPage().executeJs(productionModeJs, productionMode);

        Div div = new Div();
        div.setId("poll");
        div.setText("Click to poll using JS API");
        getPage().executeJs("$0.addEventListener('click', function() {" + client
                + ".poll();" //
                + "});", div);
        add(div);

        Span pollCounter = new Span("No polls");
        pollCounter.setId("pollCounter");
        add(pollCounter);
        UI.getCurrent().addPollListener(e -> {
            pollCount++;
            pollCounter.setText("Poll called " + pollCount + " times");
        });
    }

}
