package com.vaadin.flow.spring.test;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Proxy {

    private Server server;
    @Value("${proxy.port:-1}")
    private int proxyPort;
    @Value("${proxy.path:''}")
    private String proxyPath;

    @PostConstruct
    public void start() throws Exception {
        if (proxyPort <= 0) {
            return;
        }
        server = new Server(proxyPort);

        // Setup proxy servlet
        ServletContextHandler context = new ServletContextHandler("/",
                ServletContextHandler.SESSIONS);
        server.setHandler(context);

        ServletHolder proxyServlet = new ServletHolder(
                PathRewritingProxyServlet.class);
        proxyServlet.setInitParameter("proxyTo", "http://localhost:8888/");
        proxyServlet.setInitParameter("prefix", proxyPath);
        context.addServlet(proxyServlet, "/*");

        server.start();
    }

    @PreDestroy
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

}
