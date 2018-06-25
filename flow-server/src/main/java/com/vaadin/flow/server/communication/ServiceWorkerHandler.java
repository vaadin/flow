package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.dom.Icon;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.PWARegistry;

public class ServiceWorkerHandler implements RequestHandler {

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {

        VaadinServletRequest httpRequest = (VaadinServletRequest) request;
        PWARegistry registry = request.getService().getPwaRegistry();

        boolean swRequest = registry.getPwaConfiguration()
                .relServiceWorkerPath().equals(httpRequest.getPathInfo());

        if (!registry.getPwaConfiguration().isServiceWorkerDisabled()
                && swRequest) {
            response.setContentType("application/javascript");
            PrintWriter writer = response.getWriter();

            // List of icons for precache
            List<String> precacheFiles = registry.getIcons().stream()
                    .filter(Icon::cached)
                    .map(icon -> icon.cache()).collect(Collectors.toList());

            // Add offline page to precache
            precacheFiles.add(registry.offlinePageCache());

            // Google Workbox precache
            writer.write("importScripts('https://storage.googleapis.com/workbox-cdn/releases/3.2.0/workbox-sw.js');\n\n");

            writer.write("workbox.precaching.precacheAndRoute([\n");
            writer.write(precacheFiles.stream()
                    .collect(Collectors.joining( ",\n" )));
            writer.write("\n]);\n");

            // Offline fallback
            String offlineFallback =
                    "self.addEventListener('fetch', function(event) {\n"
                            + "  var request = event.request;\n"
                            + "  if (request.mode === 'navigate') {\n"
                            + "    event.respondWith(\n"
                            + "      fetch(request)\n"
                            + "        .catch(function() {\n"
                            + "          return caches.match('"
                            + registry.getPwaConfiguration().getOfflinePath() +  "');\n"
                            + "        })\n"
                            + "    );\n"
                            + "  }\n" + "});";

            writer.write(offlineFallback);
            writer.close();
        }

        return swRequest;
    }
}
