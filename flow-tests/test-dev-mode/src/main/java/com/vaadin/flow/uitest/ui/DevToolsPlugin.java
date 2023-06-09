package com.vaadin.flow.uitest.ui;

import com.vaadin.base.devserver.DevToolsMessageHandler;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.server.VaadinSession;

import elemental.json.JsonObject;

@JsModule(value = "./devtools-plugin.ts", developmentOnly = true)
public class DevToolsPlugin implements DevToolsMessageHandler {

    @Override
    public boolean handleDevToolsMessage(String command, JsonObject data) {
        if (command.equals("modifyUI")) {

            String text = data.getString("text");
            VaadinSession session = VaadinSession.getCurrent();
            session.access(() -> {
                UI ui = session.getUIById((int) data.getNumber("uiId"));
                ui.getPage().executeJs("""
                        const div = document.createElement('div');
                        div.innerText = $0;
                        div.id = 'injected';
                        document.querySelector('#outlet').append(div)
                        """, text);
            });

            return true;
        }
        return false;
    }

}
