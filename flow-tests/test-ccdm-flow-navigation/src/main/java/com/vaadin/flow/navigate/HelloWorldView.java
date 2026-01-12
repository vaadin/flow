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
package com.vaadin.flow.navigate;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "hello")
@PageTitle("Hello World")
public class HelloWorldView extends Span implements BeforeEnterObserver {
    public static final String NAVIGATE_ABOUT = "navigate-about";
    public static final String IS_CONNECTED_ON_INIT = "is-connected-on-init";
    public static final String IS_CONNECTED_ON_ATTACH = "is-connected-on-attach";

    private final Span isConnectedOnInit = new Span("");
    private final Span isConnectedOnAttach = new Span("");
    private Pre paramsComponent;

    public HelloWorldView() {
        setId("hello-world-view");
        NativeButton toAbout = new NativeButton("Say hello",
                e -> getUI().get().navigate("about"));
        toAbout.setId(NAVIGATE_ABOUT);
        add(toAbout);

        isConnectedOnInit.setId(IS_CONNECTED_ON_INIT);
        updateIsConnected(isConnectedOnInit);
        add(new Paragraph(new Text("Connected on init: "), isConnectedOnInit));

        isConnectedOnAttach.setId(IS_CONNECTED_ON_ATTACH);
        isConnectedOnAttach.addAttachListener(
                event -> updateIsConnected(isConnectedOnAttach));
        add(new Paragraph(new Text("Connected on attach: "),
                isConnectedOnAttach));

        RouterLink specialLink = new RouterLink("Special char view",
                SpecialCharactersView.class);
        specialLink.setId("navigate-special");
        add(specialLink);

        paramsComponent = new Pre();
        paramsComponent.setId("params");
        add(paramsComponent);
    }

    private void updateIsConnected(Span output) {
        output.getElement()
                .executeJs("this.textContent=String(this.isConnected)");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Map<String, List<String>> params = event.getLocation()
                .getQueryParameters().getParameters();
        String paramInfo = "";
        for (Entry<String, List<String>> param : params.entrySet()) {
            paramInfo += param.getKey() + ": " + param.getValue().get(0);
        }

        paramsComponent.setText(paramInfo);

    }
}
