package com.vaadin.server.communication;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.StreamReceiver;
import com.vaadin.server.StreamResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;

public class StreamRequestHandler implements RequestHandler {

    private static final char PATH_SEPARATOR = '/';

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            return false;
        }
        // remove leading '/'
        assert pathInfo.startsWith(Character.toString(PATH_SEPARATOR));
        pathInfo = pathInfo.substring(1);

        if (pathInfo.startsWith(StreamResourceRequestHandler.DYN_RES_PREFIX)) {
            StreamResource streamResource;
            session.lock();
            try {
                Optional<StreamResource> resource = StreamRequestHandler
                        .getPathUri(pathInfo)
                        .flatMap(session.getResourceRegistry()::getResource);
                if (!resource.isPresent()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND,
                            "Resource is not found for path=" + pathInfo);
                    return true;
                }
                streamResource = resource.get();
            } finally {
                session.unlock();
            }
            return new StreamResourceRequestHandler().handleRequest(session,
                    request, response, streamResource);
        } else if (pathInfo
                .startsWith(StreamReceiverRequestHandler.DYN_RES_PREFIX)) {
            StreamReceiver streamReceiver;
            session.lock();
            try {
                Optional<StreamReceiver> receiver = StreamRequestHandler
                        .getPathUri(pathInfo)
                        .flatMap(session.getResourceRegistry()::getReceiver);
                if (!receiver.isPresent()) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "No receiver registered for path '" + pathInfo
                                    + "'");
                    return true;
                }
                streamReceiver = receiver.get();
            } finally {
                session.unlock();
            }
            /*
             * URI pattern:
             * VAADIN/dynamic/file-upload/[UIID]/[NODEID]/[NAME]/[SECKEY]
             *
             * @see #generateURI
             */
            // strip away part until the data we are interested starts
            int startOfData = pathInfo.indexOf(StreamReceiverRequestHandler.DYN_RES_PREFIX)
                    + StreamReceiverRequestHandler.DYN_RES_PREFIX.length();

            String uppUri = pathInfo.substring(startOfData);
            // [0] UIid, [1] id, [2] name, [3] security key
            String[] parts = uppUri.split("/", 4);
            return new StreamReceiverRequestHandler().handleRequest(session,
                    request, response, streamReceiver, parts[0], parts[3]);
        }

        return false;
    }

    protected static Optional<URI> getPathUri(String path) {
        int index = path.lastIndexOf('/');
        boolean hasPrefix = index >= 0;
        if (!hasPrefix) {
            getLog().info("Unsupported path structure, path=" + path);
            return Optional.empty();
        }
        String prefix = path.substring(0, index + 1);
        String name = path.substring(prefix.length());
        // path info returns decoded name but space ' ' remains encoded '+'
        name = name.replace('+', ' ');
        try {
            URI uri = new URI(prefix
                    + URLEncoder.encode(name, StandardCharsets.UTF_8.name()));
            return Optional.of(uri);
        } catch (UnsupportedEncodingException e) {
            // UTF8 has to be supported
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            getLog().log(Level.INFO, "Path '" + path
                    + "' is not correct URI (it violates RFC 2396)", e);
            return Optional.empty();
        }
    }

    private static Logger getLog() {
        return Logger.getLogger(StreamResourceRequestHandler.class.getName());
    }
}
