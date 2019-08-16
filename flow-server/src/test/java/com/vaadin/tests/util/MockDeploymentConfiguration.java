package com.vaadin.tests.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import com.vaadin.flow.server.AbstractDeploymentConfiguration;
import com.vaadin.flow.shared.communication.PushMode;

public class MockDeploymentConfiguration
        extends AbstractDeploymentConfiguration {

    private boolean productionMode = false;
    private boolean enableDevServer = true;
    private boolean reuseDevServer = true;
    private boolean compatibilityMode = false;
    private boolean xsrfProtectionEnabled = true;
    private int heartbeatInterval = 300;
    private int webComponentDisconnect = 300;
    private boolean closeIdleSessions = false;
    private PushMode pushMode = PushMode.DISABLED;
    private String pushURL = "";
    private Properties initParameters = new Properties();
    private Map<String, String> applicationOrSystemProperty = new HashMap<>();
    private boolean syncIdCheckEnabled = true;
    private boolean sendUrlsAsParameters = true;
    private boolean brotli = false;

    @Override
    public boolean isProductionMode() {
        return productionMode;
    }

    @Override
    public boolean isBowerMode() {
        return compatibilityMode;
    }

    @Override
    public boolean isRequestTiming() {
        return !productionMode;
    }

    public void setProductionMode(boolean productionMode) {
        this.productionMode = productionMode;
    }

    public void setEnableDevServer(boolean enableDevServer) {
        this.enableDevServer = enableDevServer;
    }

    @Override
    public boolean enableDevServer() {
        return enableDevServer;
    }

    public void setReuseDevServer(boolean reuseDevServer) {
        this.reuseDevServer = reuseDevServer;
    }

    @Override
    public boolean reuseDevServer() {
        return reuseDevServer;
    }

    @Override
    public boolean isXsrfProtectionEnabled() {
        return xsrfProtectionEnabled;
    }

    @Override
    public boolean isSyncIdCheckEnabled() {
        return syncIdCheckEnabled;
    }

    public void setSyncIdCheckEnabled(boolean syncIdCheckEnabled) {
        this.syncIdCheckEnabled = syncIdCheckEnabled;
    }

    public void setXsrfProtectionEnabled(boolean xsrfProtectionEnabled) {
        this.xsrfProtectionEnabled = xsrfProtectionEnabled;
    }

    @Override
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    @Override
    public int getWebComponentDisconnect() {
        return webComponentDisconnect;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    @Override
    public boolean isCloseIdleSessions() {
        return closeIdleSessions;
    }

    public void setCloseIdleSessions(boolean closeIdleSessions) {
        this.closeIdleSessions = closeIdleSessions;
    }

    @Override
    public PushMode getPushMode() {
        return pushMode;
    }

    public void setPushMode(PushMode pushMode) {
        this.pushMode = pushMode;
    }

    @Override
    public String getPushURL() {
        return pushURL;
    }

    public void setPushURL(String pushURL) {
        this.pushURL = pushURL;
    }

    @Override
    public Properties getInitParameters() {
        return initParameters;
    }

    public void setInitParameter(String key, String value) {
        initParameters.setProperty(key, value);
    }

    public void setApplicationOrSystemProperty(String key, String value) {
        applicationOrSystemProperty.put(key, value);
    }

    @Override
    public <T> T getApplicationOrSystemProperty(String propertyName,
            T defaultValue, Function<String, T> converter) {
        if (applicationOrSystemProperty.containsKey(propertyName)) {
            return converter
                    .apply(applicationOrSystemProperty.get(propertyName));
        } else {
            return defaultValue;
        }
    }

    @Override
    public boolean isSendUrlsAsParameters() {
        return sendUrlsAsParameters;
    }

    @Override
    public boolean isBrotli() {
        return brotli;
    }

    public void setBrotli(boolean brotli) {
        this.brotli = brotli;
    }

    public void setCompatibilityMode(boolean compatibility) {
        compatibilityMode = compatibility;
    }

}
