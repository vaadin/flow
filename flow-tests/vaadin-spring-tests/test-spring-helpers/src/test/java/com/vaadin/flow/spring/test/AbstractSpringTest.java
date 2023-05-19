package com.vaadin.flow.spring.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractSpringTest extends ChromeBrowserTest {

    private Properties properties;

    @Override
    protected String getTestURL(String... parameters) {
        return getTestURL(getRootURL(), getContextPath() + getTestPath(),
                parameters);
    }

    @Override
    protected int getDeploymentPort() {
        String proxyPort = getProperty("proxy.port");
        if (proxyPort != null) {
            return Integer.parseInt(proxyPort);
        }

        return super.getDeploymentPort();
    }

    protected String getContextRootURL() {
        return getRootURL() + getContextPath();
    }

    protected String getContextPath() {
        String proxyPublicPath = getProperty("proxy.path");
        if (proxyPublicPath != null) {
            return proxyPublicPath;
        }

        String contextPath = getProperty("server.servlet.contextPath");
        if (contextPath != null) {
            return contextPath;
        }

        return "";
    }

    private String getProperty(String key) {
        if (properties == null) {

            properties = new Properties();
            try {
                InputStream res = getClass()
                        .getResourceAsStream("/application.properties");
                if (res != null) {
                    properties.load(res);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return properties.getProperty(key);

    }

}
