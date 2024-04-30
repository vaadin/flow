/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.flow.spring.io;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ResourceUtils;

public class FilterableResourceResolver
        extends PathMatchingResourcePatternResolver {

    private static final String JAR_PROTOCOL = "jar:";
    private static final String JAR_KEY = ".jar!/";
    private static final String PACKAGE_PROPERTIES_PATH = "META-INF/VAADIN/package.properties";

    public static final String ALLOWED_PACKAGES_PROPERTY = "vaadin.allowed-packages";
    public static final String BLOCKED_PACKAGES_PROPERTY = "vaadin.blocked-packages";

    private Map<String, Properties> propertiesCache = new HashMap<>();

    public FilterableResourceResolver(ResourceLoader resourceLoader) {
        super(resourceLoader);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(FilterableResourceResolver.class);
    }

    protected boolean isJar(String path) {
        return path.lastIndexOf(JAR_KEY) != -1;
    }

    private Resource doResolveRootDirResource(Resource original)
            throws IOException {
        String rootDirPath = original.getURI().getPath();
        if (rootDirPath != null) {
            int index = rootDirPath.lastIndexOf(JAR_KEY);
            if (index != -1) {
                String jarPath = rootDirPath.substring(0,
                        index + JAR_KEY.length());
                return new UrlResource(jarPath);
                // return new ClassPathResource(jarPath, getClassLoader());
            }
        }
        return super.resolveRootDirResource(original);
    }

    @Override
    protected Set<Resource> doFindPathMatchingJarResources(
            Resource rootDirResource, URL rootDirUrl, String subPattern)
            throws IOException {
        String path = rootDirResource.getURI().toString();
        cachePackageProperties(path, rootDirResource, rootDirUrl);

        if(isBlockedJar(rootDirResource)) {
            return Collections.emptySet();
        }
        return super.doFindPathMatchingJarResources(rootDirResource, rootDirUrl,
                subPattern);
    }

    @Override
    protected Set<Resource> doFindAllClassPathResources(String path)
            throws IOException {
        var result = super.doFindAllClassPathResources(path);
        result.removeIf(res -> {
            cachePackageProperties(res);
            return isBlockedJar(res);
        });
        return result;
    }

    private void cachePackageProperties(String path, Resource rootDirResource,
            URL rootDirUrl) throws IOException {
        if (!propertiesCache.containsKey(path)) {
            if (isJar(path)) {
                String jarPath = path.substring(0, path.lastIndexOf(JAR_KEY));
                if (jarPath.startsWith("jar:")) {
                    jarPath = jarPath.substring(4);
                }
                propertiesCache.put(jarPath, readPackageProperties(rootDirUrl,
                        path, doResolveRootDirResource(rootDirResource)));
                getLogger().trace("Caching package.properties of JAR {}", path);
            } else {
                Resource resource = doFindPathMatchingFileResources(
                        rootDirResource, PACKAGE_PROPERTIES_PATH).stream()
                        .findFirst().orElse(null);
                Properties properties = resource != null
                        ? PropertiesLoaderUtils.loadProperties(resource)
                        : null;
                propertiesCache.put(path, properties);
                getLogger().trace("Caching package.properties of directory {}",
                        path);
            }
        }
    }

    private void cachePackageProperties(Resource res) {
        try {
            Resource rootDirResource = convertClassLoaderURL(res.getURL());
            String rootDirPath = rootDirResource.getURI().toString();
            String rootPath = rootDirResource.getURI().getPath();
            if (rootPath != null && isJar(rootDirPath)) {
                int index = rootDirPath.lastIndexOf(JAR_KEY);
                if (index != -1) {
                    String jarPath = rootDirPath.substring(0,
                            index + JAR_KEY.length());
                    String key = rootPath.substring(0,
                            rootPath.lastIndexOf(JAR_KEY));
                    if (key.startsWith(JAR_PROTOCOL)) {
                        key = key.substring(4);
                    }
                    if (!propertiesCache.containsKey(key)) {
                        propertiesCache.put(key, readPackageProperties(null,
                                jarPath, rootDirResource));
                        getLogger().trace(
                                "Caching package.properties of JAR {}",
                                rootPath);
                    }
                }
            } else if (!propertiesCache.containsKey(rootPath)) {
                Resource resource = doFindPathMatchingFileResources(
                        rootDirResource, PACKAGE_PROPERTIES_PATH).stream()
                        .findFirst().orElse(null);
                Properties properties = resource != null
                        ? PropertiesLoaderUtils.loadProperties(resource)
                        : null;
                propertiesCache.put(rootPath, properties);
                getLogger().trace(
                        "Caching package.properties of directory {}",
                        rootPath);
            }

        } catch (IOException e) {
            getLogger().warn("Failed to find " + PACKAGE_PROPERTIES_PATH
                    + " for path " + res, e);
        }
    }

    private boolean isBlockedJar(Resource resource) {
        // TODO handle case of package.properties with vaadin.blocked-jar=true
        return false;
    }

    /**
     * See {@link super#doFindPathMatchingJarResources(Resource, URL, String)}.
     * This method is slightly adjusted from the origin to just read
     * META-INF/VAADIN/package.properties and transform it to Properties object.
     */
    private Properties readPackageProperties(URL jarPathURL, String jarPath,
            Resource rootDirResource) throws IOException {
        URLConnection con = null;
        JarFile jarFile;
        String jarFileUrl;
        String urlFile = null;
        boolean closeJarFile;
        if (jarPathURL != null) {
            con = jarPathURL.openConnection();
            urlFile = jarPathURL.getPath();
        }
        if (con instanceof JarURLConnection jarCon) {
            jarFile = jarCon.getJarFile();
            closeJarFile = !jarCon.getUseCaches();
        } else {
            // No JarURLConnection -> need to resort to URL file parsing.
            // We'll assume URLs of the format "jar:path!/entry", with the
            // protocol
            // being arbitrary as long as following the entry format.
            // We'll also handle paths with and without leading "file:"
            // prefix.
            urlFile = urlFile != null ? urlFile : jarPath;
            try {
                int separatorIndex = urlFile
                        .indexOf(ResourceUtils.WAR_URL_SEPARATOR);
                if (separatorIndex == -1) {
                    separatorIndex = urlFile
                            .indexOf(ResourceUtils.JAR_URL_SEPARATOR);
                }
                if (separatorIndex != -1) {
                    jarFileUrl = urlFile.substring(0, separatorIndex);
                    jarFile = getJarFile(jarFileUrl);
                } else {
                    jarFile = new JarFile(urlFile);
                }
                closeJarFile = true;
            } catch (ZipException ex) {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Skipping invalid jar class path entry ["
                            + urlFile + "]");
                }
                return null;
            }
        }

        try {
            if (getLogger().isTraceEnabled()) {
                getLogger().trace("Looking for package.properties in jar file ["
                        + rootDirResource + "]");
            }
            for (Enumeration<JarEntry> entries = jarFile.entries(); entries
                    .hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                String entryPath = entry.getName();
                if (entryPath.endsWith(PACKAGE_PROPERTIES_PATH)) {
                    Resource resource = doFindPathMatchingFileResources(
                            rootDirResource, PACKAGE_PROPERTIES_PATH).stream()
                            .findFirst().orElseGet(() -> {
                                try {
                                    return rootDirResource.createRelative(
                                            PACKAGE_PROPERTIES_PATH);
                                } catch (IOException e) {
                                    getLogger().warn(
                                            "Could not read package.properties",
                                            e);
                                    return null;
                                }
                            });
                    Properties prop = resource != null
                            ? PropertiesLoaderUtils.loadProperties(resource)
                            : null;
                    if (getLogger().isTraceEnabled()) {
                        getLogger().trace("Read package.properties: [{}]",
                                prop);
                    }
                    return prop;
                }
            }
            return null;
        } finally {
            if (closeJarFile) {
                jarFile.close();
            }
        }
    }

    protected boolean isAllowedByPackageProperties(String rootPath,
                                                   String targetPath, boolean defaultValue) {
        Properties properties = propertiesCache.get(rootPath);
        if (properties == null) {
            return defaultValue;
        }

        List<String> allowedPackages = Stream.of(properties
                        .getProperty(ALLOWED_PACKAGES_PROPERTY, "").split(","))
                .filter(pkg -> !pkg.isBlank()).toList();
        List<String> blockedPackages = Stream.of(properties
                        .getProperty(BLOCKED_PACKAGES_PROPERTY, "").split(","))
                .filter(pkg -> !pkg.isBlank()).toList();
        if (!allowedPackages.isEmpty()) {
            return allowedPackages.stream().anyMatch(targetPath::startsWith);
        } else if (!blockedPackages.isEmpty()) {
            return blockedPackages.stream().noneMatch(targetPath::startsWith);
        }
        return defaultValue;
    }
}
