/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.devmode;

import java.io.Serializable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("exported-js-function")
@PageTitle("Exported JS function view")
public class ExportedJSFunctionView extends Div {

    @JsDefinition
    public interface ClientJs extends Serializable {

        @JsExpression("""
                const client = window.Vaadin.Flow.clients[Object.keys(Vaadin.Flow.clients).filter(k => k !== 'TypeScript')];
                var msg = '';
                var versionInfoMethod = client.getVersionInfo;
                if (versionInfoMethod) {
                  msg += 'version: '+versionInfoMethod().flow;
                } else {
                  msg += 'versionInfoMethod not published';
                }
                $0.innerHTML = msg;
                """)
        void showVersion(Element target);

        @JsExpression("""
                const client = window.Vaadin.Flow.clients[Object.keys(Vaadin.Flow.clients).filter(k => k !== 'TypeScript')];
                var productionMode = client.productionMode;
                $0.innerText = 'Production mode: '+productionMode;
                """)
        void showProductionMode(Element target);

        @JsExpression("""
                $0.addEventListener('click', function() {
                  window.Vaadin.Flow.clients[Object.keys(Vaadin.Flow.clients).filter(k => k !== 'TypeScript')].poll();
                });
                """)
        void pollOnClick(Element target);
    }

    private int pollCount = 0;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!attachEvent.isInitialAttach()) {
            return;
        }
        UI ui = attachEvent.getUI();
        ClientJs clientJs = ui.getPage().executeJs(ClientJs.class);

        Div version = new Div();
        version.setId("version");
        add(version);
        clientJs.showVersion(version.getElement());

        Div productionMode = new Div();
        productionMode.setId("productionMode");
        add(productionMode);
        clientJs.showProductionMode(productionMode.getElement());

        Div div = new Div();
        div.setId("poll");
        div.setText("Click to poll using JS API");
        clientJs.pollOnClick(div.getElement());
        add(div);

        Span pollCounter = new Span("No polls");
        pollCounter.setId("pollCounter");
        add(pollCounter);
        ui.addPollListener(e -> {
            pollCount++;
            pollCounter.setText("Poll called " + pollCount + " times");
        });
    }

}
