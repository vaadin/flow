package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.server.frontend.installer.ProxyConfig;

public class ProxyFactory {

    static final String NPMRC_NOPROXY_PROPERTY_KEY = "noproxy";
    static final String NPMRC_HTTPS_PROXY_PROPERTY_KEY = "https-proxy";
    static final String NPMRC_PROXY_PROPERTY_KEY = "proxy";

    // Proxy config properties keys (for both system properties and environment
    // variables) can be either fully upper case or fully lower case
    static final String SYSTEM_NOPROXY_PROPERTY_KEY = "NOPROXY";
    static final String SYSTEM_HTTPS_PROXY_PROPERTY_KEY = "HTTPS_PROXY";
    static final String SYSTEM_HTTP_PROXY_PROPERTY_KEY = "HTTP_PROXY";

    /**
     * Read list of configured proxies in order from system properties, .npmrc
     * file in the project root folder, .npmrc file in user root folder and
     * system environment variables.
     *
     * @return list of configured proxies
     */
    public static List<ProxyConfig.Proxy> getProxies(File projectDirectory) {
        File projectNpmrc = new File(projectDirectory, ".npmrc");
        File userNpmrc = new File(FileUtils.getUserDirectory(), ".npmrc");
        List<ProxyConfig.Proxy> proxyList = new ArrayList<>();

        proxyList.addAll(readProxySettingsFromSystemProperties());
        proxyList.addAll(
                readProxySettingsFromNpmrcFile("user .npmrc", userNpmrc));
        proxyList.addAll(
                readProxySettingsFromNpmrcFile("project .npmrc", projectNpmrc));
        proxyList.addAll(readProxySettingsFromEnvironmentVariables());

        return proxyList;
    }

    private static List<ProxyConfig.Proxy> readProxySettingsFromNpmrcFile(
            String fileDescription, File npmrc) {
        if (!npmrc.exists()) {
            return Collections.emptyList();
        }

        try (FileReader fileReader = new FileReader(npmrc)) { // NOSONAR
            List<ProxyConfig.Proxy> proxyList = new ArrayList<>(2);
            Properties properties = new Properties();
            properties.load(fileReader);
            String noproxy = properties.getProperty(NPMRC_NOPROXY_PROPERTY_KEY);
            if (noproxy != null)
                noproxy = noproxy.replaceAll(",", "|");
            String httpsProxyUrl = properties
                    .getProperty(NPMRC_HTTPS_PROXY_PROPERTY_KEY);
            if (httpsProxyUrl != null) {
                proxyList.add(new ProxyConfig.Proxy(
                        "https-proxy - " + fileDescription, httpsProxyUrl,
                        noproxy));
            }
            String proxyUrl = properties.getProperty(NPMRC_PROXY_PROPERTY_KEY);
            if (proxyUrl != null) {
                proxyList.add(new ProxyConfig.Proxy(
                        "proxy - " + fileDescription, proxyUrl, noproxy));
            }
            return proxyList;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<ProxyConfig.Proxy> readProxySettingsFromSystemProperties() {
        List<ProxyConfig.Proxy> proxyList = new ArrayList<>(2);

        String noproxy = getNonNull(
                System.getProperty(SYSTEM_NOPROXY_PROPERTY_KEY),
                System.getProperty(SYSTEM_NOPROXY_PROPERTY_KEY.toLowerCase()));
        if (noproxy != null) {
            noproxy = noproxy.replaceAll(",", "|");
        }

        String httpsProxyUrl = getNonNull(
                System.getProperty(SYSTEM_HTTPS_PROXY_PROPERTY_KEY),
                System.getProperty(
                        SYSTEM_HTTPS_PROXY_PROPERTY_KEY.toLowerCase()));
        if (httpsProxyUrl != null) {
            proxyList.add(new ProxyConfig.Proxy("https-proxy - system",
                    httpsProxyUrl, noproxy));
        }

        String proxyUrl = getNonNull(
                System.getProperty(SYSTEM_HTTP_PROXY_PROPERTY_KEY),
                System.getProperty(
                        SYSTEM_HTTP_PROXY_PROPERTY_KEY.toLowerCase()));
        if (proxyUrl != null) {
            proxyList.add(
                    new ProxyConfig.Proxy("proxy - system", proxyUrl, noproxy));
        }

        return proxyList;
    }

    private static List<ProxyConfig.Proxy> readProxySettingsFromEnvironmentVariables() {
        List<ProxyConfig.Proxy> proxyList = new ArrayList<>(2);

        String noproxy = getNonNull(System.getenv(SYSTEM_NOPROXY_PROPERTY_KEY),
                System.getenv(SYSTEM_NOPROXY_PROPERTY_KEY.toLowerCase()));
        if (noproxy != null) {
            noproxy = noproxy.replaceAll(",", "|");
        }

        String httpsProxyUrl = getNonNull(
                System.getenv(SYSTEM_HTTPS_PROXY_PROPERTY_KEY),
                System.getenv(SYSTEM_HTTPS_PROXY_PROPERTY_KEY.toLowerCase()));
        if (httpsProxyUrl != null) {
            proxyList.add(new ProxyConfig.Proxy("https-proxy - env",
                    httpsProxyUrl, noproxy));
        }

        String proxyUrl = getNonNull(
                System.getenv(SYSTEM_HTTP_PROXY_PROPERTY_KEY),
                System.getenv(SYSTEM_HTTP_PROXY_PROPERTY_KEY.toLowerCase()));
        if (proxyUrl != null) {
            proxyList.add(
                    new ProxyConfig.Proxy("proxy - env", proxyUrl, noproxy));
        }

        return proxyList;
    }

    /**
     * Get the first non null value from the given array.
     *
     * @param valueArray
     *            array of values to get non null from
     * @return first non null value or null if no values found
     */
    private static String getNonNull(String... valueArray) {
        for (String value : valueArray) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

}
