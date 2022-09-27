package com.vaadin.flow.spring.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.ProxyServlet.Transparent;

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
            URL publicURL;
            try {
                publicURL = new java.net.URL(
                        clientRequest.getRequestURL().toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return headerValue;
            }
            String hostAndBasePath = publicURL.getProtocol() + "://"
                    + publicURL.getHost() + ":" + publicURL.getPort() + prefix
                    + "/";

            return headerValue.replace(proxyTo, hostAndBasePath);
        }
        return super.filterServerResponseHeader(clientRequest, serverResponse,
                headerName, headerValue);
    }
}
