package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.PrintWriter;

import com.vaadin.flow.dom.Icon;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.PWARegistry;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class PWAHandler implements RequestHandler {


    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {

        VaadinServletRequest httpRequest = (VaadinServletRequest) request;
        PWARegistry registry = request.getService().getPwaRegistry();
        PwaConfiguration config = registry.getPwaConfiguration();
        if (!config.isManifestDisabled() &&
                config.relManifestPath().equals(httpRequest.getPathInfo())) {
            response.setContentType("application/json");
            JsonObject manifestData = Json.createObject();
            manifestData.put("name", config.getAppName());
            manifestData.put("short_name", config.getAppName());
            manifestData.put("display", config.getDisplay());
            manifestData.put("background_color", config.getBackgroundColor());
            manifestData.put("theme_color", config.getThemeColor());
            manifestData.put("start_url", config.getStartUrl());

            JsonArray icons = Json.createArray();
            int iconIndex = 0;
            for (Icon icon : registry.getManifestIcons()) {
                JsonObject iconData = Json.createObject();
                iconData.put("src", icon.href());
                iconData.put("sizes", icon.sizes());
                iconData.put("type", icon.type());
                icons.set(iconIndex++, iconData);
            }
            manifestData.put("icons", icons);

            PrintWriter writer = response.getWriter();
            writer.write(manifestData.toJson());
            writer.close();
            return true;
        }

        return false;
    }



}
