/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.io.Serializable;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipException;

import org.apache.commons.io.IOUtils;
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
        extends PathMatchingResourcePatternResolver implements Serializable {

    private static final String JAR_PROTOCOL = "jar:";
    private static final String JAR_KEY = ".jar!/";
    private static final String JAR_EXTENSION = ".jar";
    private static final String PACKAGE_PROPERTIES_PATH = "META-INF/VAADIN/package.properties";
    private static final String BLOCKED_JARS_LIST_PATH = "/META-INF/VAADIN/blocked-jars.list";

    /**
     * The property key for allowed packages.
     */
    public static final String ALLOWED_PACKAGES_PROPERTY = "vaadin.allowed-packages";
    /**
     * The property key for blocked packages.
     */
    public static final String BLOCKED_PACKAGES_PROPERTY = "vaadin.blocked-packages";
    /**
     * The property key for blocked JAR file.
     */
    public static final String BLOCKED_JAR_PROPERTY = "vaadin.blocked-jar";

    /**
     * Jar filename patterns for excluded jars.
     */
    private static final List<String> DEFAULT_SCAN_NEVER_JAR = Stream.of(
            "antlr", "logback-classic", "logback-classic-core",
            "commons-codec-*.*.*.jar", "commons-fileupload",
            "commons-io-*.*.*.jar", "commons-logging", "commons-exec",
            "commons-lang*-*.*.*.jar", "jackson-databind-", "jackson-core-",
            "jackson-datatype-", "jackson-annotations-", "jackson-module-",
            "jackson-datatype-", "atmosphere-runtime", "byte-buddy",
            "commons-compress", "aspectjweaver", "hibernate-core",
            "hibernate-commons", "hibernate-validator", "jboss-logging",
            "selenium-", "slf4j-simple-", "slf4j-api-", "spring-*.*.*.jar",
            "spring-webmvc-*.*.*.jar", "spring-aop-*.*.*.jar",
            "spring-beans-*.*.*.jar", "spring-context-*.*.*.jar",
            "spring-core-*.*.*.jar", "spring-jcl-*.*.*.jar",
            "spring-expression-*.*.*.jar", "spring-websocket-*.*.*.jar",
            "spring-web-*.*.*.jar", "snakeyaml-*.*.jar", "javax.", "jakarta.",
            "kotlin-reflect-", "kotlin-stdlib-", "gwt-elemental",
            "javassist-*.*.*-*.jar", "javaparser-core-*.*.*.jar",
            "javaparser-symbol", "oshi-core-*.*.*.jar",
            "micrometer-observation-*.*.*.jar", "micrometer-commons-*.*.*.jar",
            "nimbus-jose-jwt", "jooq-*.*.*.jar", "jooq-*-*.*.*.jar",
            "directory-watcher-*.*.*.jar", "classgraph", "jsoup-*.*.*.jar",
            "throw-if-servlet3", "ph-css-*.*.*.jar", "ph-commons-*.*.*.jar",
            "gentyref-*.*.*.vaadin1.jar", "asm-*.*.jar", "asm-commons-*.*.jar",
            "asm-tree-*.*.jar", "jetty-", "tomcat-", "classmate-*.*.*.jar",
            "reflections-*.*.*.jar", "jna-*.*.*.jar", "jna-platform-*.*.*.jar",
            "jcip-annotations-*.*.*.jar", "activation-*.*.*.jar",
            "httpcore5-*.*.*.jar", "httpcore5-h2-*.*.*.jar",

            "hilla-engine-core-", "hilla-engine-runtime-", "hilla-parser-jvm-",
            "hilla-runtime-plugin-").toList();

    private final Map<String, PackageInfo> propertiesCache = new HashMap<>();

    private List<String> blockedJarsList;

    private record PackageInfo(Set<String> allowedPackages,
            Set<String> blockedPackages,
            boolean blockedJar) implements Serializable {
    }

    /**
     * Creates a new instance of the resolver.
     *
     * @param resourceLoader
     *            the resource loader to use
     */
    public FilterableResourceResolver(ResourceLoader resourceLoader) {
        super(resourceLoader);
        initBlockedJars();
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
        if (rootDirPath == null) {
            rootDirPath = original.getURI().toString();
        }
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
        String jarName = resolveJarName(rootDirResource.getURI());
        if (jarName != null && blockedJarsList.stream()
                .anyMatch(pattern -> jarNamePatternMatch(jarName, pattern))) {
            return Set.of();
        }
        String key = cachePackageProperties(path, rootDirResource, rootDirUrl);

        if (isBlockedJar(rootDirResource, key)) {
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
            try {
                String jarName = resolveJarName(res.getURI());
                if (jarName != null && blockedJarsList.stream().anyMatch(
                        pattern -> jarNamePatternMatch(jarName, pattern))) {
                    return true;
                }
            } catch (IOException e) {
                getLogger().warn("Failed to resolve path for resource {}", res,
                        e);
            }
            String key = cachePackageProperties(res);
            return isBlockedJar(res, key);
        });
        return result;
    }

    /**
     * Matches given jarName with the pattern. if pattern doesn't contain '*',
     * then match is based on startsWith(pattern). If pattern has one or more
     * '*', pattern is split into array and each part is matched with startsWith
     * for each part in the given jarName. '*' match any character 0-n times
     * except '-' or content of the part following `*`. <br/>
     * <br/>
     * For example, "spring-*.*.*.jar" pattern matches to "spring-1.0.0.jar",
     * "spring-abc.1.0.jar", "spring-abc.1.0.0.jar" but NOT
     * "spring-abc-1.0.0.jar" or "spring-1.0.jar". <br/>
     * <br/>
     * String operations are handled from left to right, where content of `*` is
     * substring starting from end of the previous String to beginning of the
     * first occurrence of the next part in the parts array. <br/>
     * <br/>
     * "spring-*-*.*.*.jar" pattern matches to "spring-foo-1.0.0.jar",
     * "spring-foo-bar.1.0.jar" but NOT "spring-foo-bar-1.0.0.jar" or
     * "spring-1.0.0.jar".<br/>
     * <br/>
     * "spring-*_*.*.jar" match "spring-abc.1_0.0.jar" due to the order '*' is a
     * substring part by part from left to right. <br/>
     * <br/>
     * Method is not using much regex to get optimal performance.
     */
    boolean jarNamePatternMatch(String jarName, String pattern) {
        if (pattern.contains("*")) {
            if (pattern.equals("*")) {
                return true;
            }
            var parts = pattern.split("\\*");
            String remainingName = jarName;
            int nextPartIndex = 0;
            int partIndex = 0;
            for (String part : parts) {
                if (!remainingName.startsWith(part)) {
                    return false;
                }
                if ((partIndex + 1) >= parts.length) {
                    return true;
                }
                remainingName = remainingName.substring(part.length());
                nextPartIndex = remainingName.indexOf(parts[partIndex + 1]);
                if (nextPartIndex == -1) {
                    return false;
                }
                if (remainingName.substring(0, nextPartIndex).contains("-")) {
                    return false;
                }
                remainingName = remainingName.substring(nextPartIndex);
                partIndex++;
            }
        }
        return jarName.startsWith(pattern);
    }

    private String resolveJarName(URI resourceURI) {
        String resourcePath = resourceURI.getPath();
        if (resourcePath == null) {
            resourcePath = resourceURI.toString();
        }
        int index = resourcePath.lastIndexOf(JAR_EXTENSION);
        if (index > -1) {
            String jarName = resourcePath.substring(0,
                    index + JAR_EXTENSION.length());
            index = jarName.lastIndexOf("/");
            if (index > -1) {
                return jarName.substring(index + 1);
            }
            return jarName;
        }
        return null;
    }

    private String cachePackageProperties(String path, Resource rootDirResource,
            URL rootDirUrl) throws IOException {
        String key = path;
        if (isJar(path)) {
            key = pathToKey(path);
            if (!propertiesCache.containsKey(key)) {
                propertiesCache.put(key, readPackageProperties(rootDirUrl, path,
                        doResolveRootDirResource(rootDirResource)));
                getLogger().trace("Caching package.properties of JAR {}", path);
            }
        } else if (!propertiesCache.containsKey(path)) {
            Resource resource = doFindPathMatchingFileResources(rootDirResource,
                    PACKAGE_PROPERTIES_PATH).stream().findFirst().orElse(null);
            Properties properties = resource != null
                    ? PropertiesLoaderUtils.loadProperties(resource)
                    : null;
            propertiesCache.put(path, createPackageInfo(properties));
            getLogger().trace("Caching package.properties of directory {}",
                    path);
        }
        return key;
    }

    private String cachePackageProperties(Resource res) {
        String key = null;
        try {
            Resource rootDirResource = convertClassLoaderURL(res.getURL());
            String rootDirPath = rootDirResource.getURI().toString();
            String rootPath = rootDirResource.getURI().getPath();
            key = rootPath;
            if (rootPath != null && isJar(rootDirPath)) {
                String jarPath = toJarPath(rootDirPath);
                key = pathToKey(rootPath);
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
                propertiesCache.put(rootPath, createPackageInfo(properties));
                getLogger().trace("Caching package.properties of directory {}",
                        rootPath);
            }

        } catch (IOException e) {
            getLogger().warn("Failed to find {} for path {}",
                    PACKAGE_PROPERTIES_PATH, res, e);
        }
        return key;
    }

    /**
     * Returns whether the given resource is a blocked jar and shouldn't be
     * included.
     *
     * @param resource
     *            the resource to check
     * @param key
     *            the key for the package info
     * @return {@code true} if the resource is a blocked jar, {@code false}
     *         otherwise
     */
    protected boolean isBlockedJar(Resource resource, String key) {
        if (resource != null && key != null) {
            PackageInfo pkgInfo = propertiesCache.get(key);
            return pkgInfo != null && pkgInfo.blockedJar();
        }
        return false;
    }

    /**
     * See {@link super#doFindPathMatchingJarResources(Resource, URL, String)}.
     * This method is slightly adjusted from the origin to just read
     * META-INF/VAADIN/package.properties and transform it to Properties object.
     */
    private PackageInfo readPackageProperties(URL jarPathURL, String jarPath,
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
                    return prop != null ? createPackageInfo(prop) : null;
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
        PackageInfo packageInfo = propertiesCache.get(rootPath);
        if (packageInfo == null) {
            return defaultValue;
        }

        if (!packageInfo.allowedPackages().isEmpty()) {
            return packageInfo.allowedPackages().stream()
                    .anyMatch(targetPath::startsWith);
        } else if (!packageInfo.blockedPackages().isEmpty()) {
            return packageInfo.blockedPackages().stream()
                    .noneMatch(targetPath::startsWith);
        }
        return defaultValue;
    }

    private PackageInfo createPackageInfo(Properties properties) {
        if (properties == null) {
            return null;
        }
        Set<String> allowedPackages = Stream
                .of(properties.getProperty(ALLOWED_PACKAGES_PROPERTY, "")
                        .split(","))
                .filter(pkg -> !pkg.isBlank()).map(String::trim)
                .map(pkg -> pkg.replace(".", "/")).collect(Collectors.toSet());
        Set<String> blockedPackages = Stream
                .of(properties.getProperty(BLOCKED_PACKAGES_PROPERTY, "")
                        .split(","))
                .filter(pkg -> !pkg.isBlank()).map(String::trim)
                .map(pkg -> pkg.replace(".", "/")).collect(Collectors.toSet());
        boolean blockedJar = Boolean.parseBoolean(
                properties.getProperty(BLOCKED_JAR_PROPERTY, "false"));
        return new PackageInfo(allowedPackages, blockedPackages, blockedJar);
    }

    private void initBlockedJars() {
        blockedJarsList = DEFAULT_SCAN_NEVER_JAR;
        URL url = getClass().getResource(BLOCKED_JARS_LIST_PATH);
        if (url == null) {
            return;
        }
        try {
            String content = IOUtils.toString(url, StandardCharsets.UTF_8);
            if (content != null) {
                if (content.isBlank()) {
                    blockedJarsList = Collections.emptyList();
                } else {
                    blockedJarsList = Arrays.asList(content.split("\\R"));
                }
            }
        } catch (IOException e) {
            getLogger().error(
                    "Failed to read {}. Falling back to default list of blocked jars.",
                    BLOCKED_JARS_LIST_PATH, e);
        }
    }
}
