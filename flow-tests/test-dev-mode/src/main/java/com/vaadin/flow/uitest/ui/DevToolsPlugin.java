package com.vaadin.flow.uitest.ui;

import com.vaadin.base.devserver.DevToolsInterface;
import com.vaadin.base.devserver.DevToolsMessageHandler;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.server.VaadinSession;

import elemental.json.Json;
import elemental.json.JsonObject;

@JsModule(value = "./devtools-plugin.ts", developmentOnly = true)
public class DevToolsPlugin implements DevToolsMessageHandler {

    @Override
    public void handleConnect(DevToolsInterface devToolsInterface) {
        devToolsInterface.send("plugin-init", null);
    }

    @Override
    public boolean handleMessage(String command, JsonObject data,
            DevToolsInterface devToolsInterface) {
        if (command.equals("plugin-query")) {
            String text = data.getString("text");

            JsonObject responseData = Json.createObject();
            responseData.put("text", "Response for " + text);
            devToolsInterface.send("plugin-response", responseData);

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
