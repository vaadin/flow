package com.vaadin.base.devserver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import com.vaadin.flow.server.AbstractDeploymentConfiguration;
import com.vaadin.flow.server.SessionLockCheckStrategy;
import com.vaadin.flow.shared.communication.PushMode;

public class MockDeploymentConfiguration
        extends AbstractDeploymentConfiguration {

    private boolean productionMode = false;
    private boolean reuseDevServer = true;
    private boolean xsrfProtectionEnabled = true;
    private int heartbeatInterval = 300;
    private int maxMessageSuspendTimeout = 5000;
    private int webComponentDisconnect = 300;
    private boolean closeIdleSessions = false;
    private PushMode pushMode = PushMode.DISABLED;
    private String pushServletMapping = "";
    private Properties initParameters = new Properties();
    private Map<String, String> applicationOrSystemProperty = new HashMap<>();
    private boolean syncIdCheckEnabled = true;
    private boolean sendUrlsAsParameters = true;
    private boolean brotli = false;
    private boolean eagerServerLoad = false;
    private boolean devModeLiveReloadEnabled = false;
    private boolean devToolsEnabled = true;
    private SessionLockCheckStrategy sessionLockCheckStrategy = SessionLockCheckStrategy.ASSERT;

    public MockDeploymentConfiguration() {
        super(Collections.emptyMap());
    }

    @Override
    public boolean isProductionMode() {
        return productionMode;
    }

    @Override
    public boolean isRequestTiming() {
        return !productionMode;
    }

    public void setProductionMode(boolean productionMode) {
        this.productionMode = productionMode;
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
    public int getMaxMessageSuspendTimeout() {
        return maxMessageSuspendTimeout;
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
    public String getPushServletMapping() {
        return pushServletMapping;
    }

    public void setPushServletMapping(String pushServletMapping) {
        this.pushServletMapping = pushServletMapping;
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

    @Override
    public boolean isEagerServerLoad() {
        return this.eagerServerLoad;
    }

    @Override
    public boolean isDevModeLiveReloadEnabled() {
        return isDevToolsEnabled() && devModeLiveReloadEnabled;
    }

    @Override
    public boolean isDevToolsEnabled() {
        return devToolsEnabled;
    }

    public void setEagerServerLoad(boolean includeBootsrapInitialUidl) {
        this.eagerServerLoad = includeBootsrapInitialUidl;
    }

    public void setDevModeLiveReloadEnabled(boolean devModeLiveReloadEnabled) {
        this.devModeLiveReloadEnabled = devModeLiveReloadEnabled;
    }

    public void setDevToolsEnabled(boolean devToolsEnabled) {
        this.devToolsEnabled = devToolsEnabled;
    }

    @Override
    public SessionLockCheckStrategy getSessionLockCheckStrategy() {
        return sessionLockCheckStrategy;
    }

    public void setLockCheckStrategy(
            SessionLockCheckStrategy sessionLockCheckStrategy) {
        this.sessionLockCheckStrategy = sessionLockCheckStrategy;
    }
}
