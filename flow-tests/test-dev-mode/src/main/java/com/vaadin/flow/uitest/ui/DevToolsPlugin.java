package com.vaadin.flow.uitest.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.vaadin.base.devserver.DevToolsInterface;
import com.vaadin.base.devserver.DevToolsMessageHandler;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.VaadinSession;

import elemental.json.JsonObject;

@JsModule(value = "./devtools-plugin.ts", developmentOnly = true)
public class DevToolsPlugin implements DevToolsMessageHandler {

    @Override
    public void handleConnect(DevToolsInterface devToolsInterface) {
        devToolsInterface.send("plugin-init", (JsonNode) null);
    }

    @Override
    public boolean handleMessage(String command, JsonObject data,
            DevToolsInterface devToolsInterface) {
        return handleMessage(command, JacksonUtils.mapElemental(data),
                devToolsInterface);
    }

    @Override
    public boolean handleMessage(String command, JsonNode data,
            DevToolsInterface devToolsInterface) {
        if (command.equals("plugin-query")) {
            String text = data.get("text").textValue();

            ObjectNode responseData = JacksonUtils.createObjectNode();
            responseData.put("text", "Response for " + text);
            devToolsInterface.send("plugin-response", responseData);

            VaadinSession session = VaadinSession.getCurrent();
            session.access(() -> {
                UI ui = session.getUIById(data.get("uiId").intValue());
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
