package com.vaadin.flow.server.communication;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.OutputStream;

import com.vaadin.flow.dom.Icon;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ManifestRegistry;

public class IconHandler implements RequestHandler {
    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {

        VaadinServletRequest httpRequest = (VaadinServletRequest) request;
        ManifestRegistry registry = ManifestRegistry.getInstance(
                httpRequest.getServletContext());


        for (Icon icon : registry.getIcons()) {
            if (icon.relHref().equals(httpRequest.getPathInfo())
                    && registry.hasImage(icon)) {
                response.setContentType(icon.type());
                OutputStream out = response.getOutputStream();
                ImageIO.write(registry.getImage(icon),
                        "png", out);
                out.close();
                return true;
            }
        }
        return false;
    }
}
