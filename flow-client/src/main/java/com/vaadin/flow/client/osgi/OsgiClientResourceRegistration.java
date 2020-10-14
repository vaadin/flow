package com.vaadin.flow.client.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@Component(immediate = true)
public class OsgiClientResourceRegistration {

    private static class ResourceServlet extends HttpServlet {

        private final Bundle bundle;

        private ResourceServlet(Bundle bundle) {
            this.bundle = bundle;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                resp.setStatus(HttpURLConnection.HTTP_NOT_FOUND);
                return;
            }
            String path = "/META-INF/resources" + pathInfo;
            URL resource = bundle.getResource(path);
            if (resource == null) {
                resp.setStatus(HttpURLConnection.HTTP_NOT_FOUND);
                return;
            }
            try (InputStream inputStream = resource.openConnection()
                    .getInputStream()) {
                IOUtils.copy(inputStream, resp.getOutputStream());
            }
        }

    }

    @Activate
    void activate(BundleContext ctx) throws NamespaceException {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED,
                true);
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
                "/VAADIN/static/client/*");
        ctx.registerService(Servlet.class, new ResourceServlet(ctx.getBundle()),
                properties);
    }
}