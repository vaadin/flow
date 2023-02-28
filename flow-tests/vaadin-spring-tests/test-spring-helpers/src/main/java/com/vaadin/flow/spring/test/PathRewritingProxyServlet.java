package com.vaadin.flow.spring.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.ee10.proxy.ProxyServlet.Transparent;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

public class PathRewritingProxyServlet extends Transparent {

    private String prefix;
    private String proxyTo;

    @Override
    public void init(ServletConfig config) throws ServletException {
        proxyTo = config.getInitParameter("proxyTo");
        prefix = config.getInitParameter("prefix");
        super.init(config);
    }

    @Override
    protected String filterServerResponseHeader(
            HttpServletRequest clientRequest, Response serverResponse,
            String headerName, String headerValue) {
        if (headerName.toLowerCase(Locale.ENGLISH).equals("set-cookie")) {
            // Set-Cookie: JSESSIONID=07E35F87D336463E597B5B0D32744660; Path=/;
            // HttpOnly
            return headerValue.replace("Path=/", "Path=" + prefix);
        } else if (headerName.equals("Location")) {
            // Location: http://localhost:8888/my/login/page
            if ((headerValue.startsWith("http://")
                    || headerValue.startsWith("https://"))
                    && !headerValue.startsWith(proxyTo)) {
                // External location
                return headerValue;
            }

            try {
                URL publicURL = new URL(
                        clientRequest.getRequestURL().toString());
                String hostAndBasePath = publicURL.getProtocol() + "://"
                        + publicURL.getHost() + ":" + publicURL.getPort()
                        + prefix + "/";

                if (headerValue.startsWith(proxyTo)) {
                    return headerValue.replace(proxyTo, hostAndBasePath);
                } else {
                    // Location: /foo/bar
                    return prefix + headerValue;
                }
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Unable to rewrite header "
                        + headerName + ": " + headerValue);
            }

        }
        return super.filterServerResponseHeader(clientRequest, serverResponse,
                headerName, headerValue);
    }
}
