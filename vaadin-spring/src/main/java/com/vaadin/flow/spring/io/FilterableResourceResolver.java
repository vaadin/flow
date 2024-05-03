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

/**
 * A {@link PathMatchingResourcePatternResolver} that allows filtering resources
 * by package properties. The resolver reads META-INF/VAADIN/package.properties
 * from JAR files and directories. The properties file can contain a list of the
 * allowed or blocked packages. If it contains both, the allowed packages take
 * precedence. Allowed packages are mapped with the key
 * "vaadin.allowed-packages". Blocked packages are mapped with the key
 * "vaadin.blocked-packages".
 *
 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
 */
public class FilterableResourceResolver
        extends PathMatchingResourcePatternResolver {

    private static final String JAR_PROTOCOL = "jar:";
    private static final String JAR_KEY = ".jar!/";
    private static final String PACKAGE_PROPERTIES_PATH = "META-INF/VAADIN/package.properties";

    /**
     * The property key for allowed packages.
     */
    public static final String ALLOWED_PACKAGES_PROPERTY = "vaadin.allowed-packages";
    /**
     * The property key for blocked packages.
     */
    public static final String BLOCKED_PACKAGES_PROPERTY = "vaadin.blocked-packages";

    private final Map<String, Properties> propertiesCache = new HashMap<>();

    /**
     * Creates a new instance of the resolver.
     *
     * @param resourceLoader
     *            the resource loader to use
     */
    public FilterableResourceResolver(ResourceLoader resourceLoader) {
        super(resourceLoader);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(FilterableResourceResolver.class);
    }

    private String toJarPath(String path) {
        return path.substring(0, path.lastIndexOf(JAR_KEY) + JAR_KEY.length());
    }

    private String pathToKey(String path) {
        String key = path.substring(0, path.lastIndexOf(JAR_KEY));
        if (key.startsWith(JAR_PROTOCOL)) {
            // clear the jar: prefix
            key = key.substring(4);
        }
        return key;
    }

    /**
     * Checks if the given path is a JAR file.
     *
     * @param path
     *            the path to check. Not null.
     * @return {@code true} if the path is a JAR file, {@code false} otherwise
     */
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
            }
        }
        return super.resolveRootDirResource(original);
    }

    /**
     * Find all resources in jar files that match the given location pattern via
     * the Ant-style PathMatcher. Supports additional filtering based on allowed
     * or blocked packages in package.properties.
     *
     * @param rootDirResource
     *            the root directory as Resource
     * @param rootDirUrl
     *            the pre-resolved root directory URL
     * @param subPattern
     *            the sub pattern to match (below the root directory)
     * @return a mutable Set of matching Resource instances
     * @throws IOException
     *             in case of I/O error
     */
    @Override
    protected Set<Resource> doFindPathMatchingJarResources(
            Resource rootDirResource, URL rootDirUrl, String subPattern)
            throws IOException {
        String path = rootDirResource.getURI().toString();
        cachePackageProperties(path, rootDirResource, rootDirUrl);

        if (isBlockedJar(rootDirResource)) {
            return Collections.emptySet();
        }
        return super.doFindPathMatchingJarResources(rootDirResource, rootDirUrl,
                subPattern);
    }

    /**
     * Find all class path resources with the given path via the configured
     * ClassLoader. Called by findAllClassPathResources(String). Supports
     * additional filtering based on allowed or blocked packages in
     * package.properties.
     *
     * @param path
     *            the absolute path within the class path (never a leading
     *            slash)
     * @return a mutable Set of matching Resource instances
     * @throws IOException
     *             in case of I/O errors
     */
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
                String jarPath = pathToKey(path);
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
                String jarPath = toJarPath(rootDirPath);
                String key = pathToKey(rootPath);
                if (!propertiesCache.containsKey(key)) {
                    propertiesCache.put(key, readPackageProperties(null,
                            jarPath, rootDirResource));
                    getLogger().trace("Caching package.properties of JAR {}",
                            rootPath);
                }
            } else if (!propertiesCache.containsKey(rootPath)) {
                Resource resource = doFindPathMatchingFileResources(
                        rootDirResource, PACKAGE_PROPERTIES_PATH).stream()
                        .findFirst().orElse(null);
                Properties properties = resource != null
                        ? PropertiesLoaderUtils.loadProperties(resource)
                        : null;
                propertiesCache.put(rootPath, properties);
                getLogger().trace("Caching package.properties of directory {}",
                        rootPath);
            }

        } catch (IOException e) {
            getLogger().warn("Failed to find " + PACKAGE_PROPERTIES_PATH
                    + " for path " + res, e);
        }
    }

    /**
     * Returns whether the given resource is a blocked jar and shouldn't be
     * included.
     *
     * @param resource
     *            the resource to check
     * @return {@code true} if the resource is a blocked jar, {@code false}
     *         otherwise
     */
    protected boolean isBlockedJar(Resource resource) {
        // placeholder to handle case of package.properties with
        // vaadin.blocked-jar=true
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

    /**
     * Check if the target path is allowed by the package properties.
     *
     * @param rootPath
     *            Root path as a key for the cached properties
     * @param targetPath
     *            relative path to check
     * @param defaultValue
     *            default value to return if the properties are not found
     * @return {@code true} if the target path is allowed by the package
     *         properties,
     */
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
