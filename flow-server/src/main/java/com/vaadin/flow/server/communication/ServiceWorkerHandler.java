package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.server.Manifest;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ManifestRegistry;

public class ServiceWorkerHandler implements RequestHandler {

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {

        VaadinServletRequest httpRequest = (VaadinServletRequest) request;
        ManifestRegistry registry = ManifestRegistry.getInstance(
                httpRequest.getServletContext());
        boolean swRequest = "/sw.js".equals(httpRequest.getPathInfo());

        if (!registry.getPwaConfiguration().isServiceWorkerDisabled()
                && swRequest) {
            PrintWriter writer = response.getWriter();
            List<String> precacheFiles = registry.getIcons().stream()
                    .map(icon -> icon.href()).collect(Collectors.toList());
            precacheFiles.add(registry.getPwaConfiguration().getOfflinePath());

            writer.write("importScripts('https://storage.googleapis.com/workbox-cdn/releases/3.2.0/workbox-sw.js');\n\n");

            writer.write("workbox.precaching.precacheAndRoute([\n");
            writer.write(precacheFiles.stream().map(fileName -> "'"
                    + fileName.replaceAll("'", "\\'") + "'")
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

            response.setContentType("application/javascript");
        }

        return swRequest;
    }
}
