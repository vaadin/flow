/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.internal.ResponseWriter;

import static com.vaadin.flow.server.Constants.VAADIN_MAPPING;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;

/**
 * Handles sending of resources from the WAR root (web content) or
 * META-INF/resources in the case that {@link VaadinServlet} is mapped using
 * "/*".
 * <p>
 * This class is primarily meant to be used during developing time. For a
 * production mode site you should consider serving static resources directly
 * from the servlet (using a default servlet if such exists) or through a stand
 * alone static file server.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class StaticFileServer implements StaticFileHandler {
    static final String PROPERTY_FIX_INCORRECT_WEBJAR_PATHS = Constants.VAADIN_PREFIX
            + "fixIncorrectWebjarPaths";
    private static final Pattern INCORRECT_WEBJAR_PATH_REGEX = Pattern
            .compile("^/frontend[-\\w/]*/webjars/");

    private final ResponseWriter responseWriter;
    private final VaadinService vaadinService;
    private DeploymentConfiguration deploymentConfiguration;
    private DevModeHandler devModeHandler;

    // Matcher to match string starting with '/themes/[theme-name]/'
    public static final Pattern APP_THEME_PATTERN = Pattern
            .compile("^\\/themes\\/[\\s\\S]+?\\/");

    // Mapped uri is for the jar file
    static final Map<URI, Integer> openFileSystems = new HashMap<>();
    private static final Object fileSystemLock = new Object();

    /**
     * Constructs a file server.
     *
     * @param vaadinService
     *            vaadin service for the deployment, not <code>null</code>
     */
    public StaticFileServer(VaadinService vaadinService) {
        this.vaadinService = vaadinService;
        deploymentConfiguration = vaadinService.getDeploymentConfiguration();
        responseWriter = new ResponseWriter(deploymentConfiguration);

        this.devModeHandler = DevModeHandlerManager
                .getDevModeHandler(vaadinService).orElse(null);
    }

    private boolean resourceIsDirectory(URL resource) {
        if (resource.getPath().endsWith("/")) {
            return true;
        }
        URI resourceURI = null;
        try {
            resourceURI = resource.toURI();
        } catch (URISyntaxException e) {
            getLogger().debug("Syntax error in uri from getStaticResource", e);
            // Return false as we couldn't determine if the resource is a
            // directory.
            return false;
        }

        if ("jar".equals(resource.getProtocol())) {
            // Get the file path in jar
            final String pathInJar = resource.getPath()
                    .substring(resource.getPath().indexOf('!') + 1);
            try {
                FileSystem fileSystem = getFileSystem(resourceURI);
                // Get the file path inside the jar.
                final Path path = fileSystem.getPath(pathInJar);

                return Files.isDirectory(path);
            } catch (IOException e) {
                getLogger().debug("failed to read jar file contents", e);
            } finally {
                closeFileSystem(resourceURI);
            }
        }

        // If not a jar check if a file path directory.
        return "file".equals(resource.getProtocol())
                && Files.isDirectory(Paths.get(resourceURI));
    }

    /**
     * Get the file URI for the resource jar file. Returns give URI if
     * URI.scheme is not of type jar.
     *
     * The URI for a file inside a jar is composed as
     * 'jar:file://...pathToJar.../jarFile.jar!/pathToFile'
     *
     * the first step strips away the initial scheme 'jar:' leaving us with
     * 'file://...pathToJar.../jarFile.jar!/pathToFile' from which we remove the
     * inside jar path giving the end result
     * 'file://...pathToJar.../jarFile.jar'
     *
     * @param resourceURI
     *            resource URI to get file URI for
     * @return file URI for resource jar or given resource if not a jar schemed
     *         URI
     */
    private URI getFileURI(URI resourceURI) {
        if (!"jar".equals(resourceURI.getScheme())) {
            return resourceURI;
        }
        try {
            String scheme = resourceURI.getRawSchemeSpecificPart();
            int jarPartIndex = scheme.indexOf("!/");
            if (jarPartIndex != -1) {
                scheme = scheme.substring(0, jarPartIndex);
            }
            return new URI(scheme);
        } catch (URISyntaxException syntaxException) {
            throw new IllegalArgumentException(syntaxException.getMessage(),
                    syntaxException);
        }
    }

    // Package protected for feature verification purpose
    FileSystem getFileSystem(URI resourceURI) throws IOException {
        synchronized (fileSystemLock) {
            URI fileURI = getFileURI(resourceURI);
            if (!fileURI.getScheme().equals("file")) {
                throw new IOException("Can not read scheme '"
                        + fileURI.getScheme() + "' for resource " + resourceURI
                        + " and will determine this as not a folder");
            }

            if (openFileSystems.computeIfPresent(fileURI,
                    (key, value) -> value + 1) != null) {
                // Get filesystem is for the file to get the correct provider
                return FileSystems.getFileSystem(resourceURI);
            }
            // Opened filesystem is for the file to get the correct provider
            FileSystem fileSystem = getNewOrExistingFileSystem(resourceURI);
            openFileSystems.put(fileURI, 1);
            return fileSystem;
        }
    }

    private FileSystem getNewOrExistingFileSystem(URI resourceURI)
            throws IOException {
        try {
            return FileSystems.newFileSystem(resourceURI,
                    Collections.emptyMap());
        } catch (FileSystemAlreadyExistsException fsaee) {
            getLogger().trace(
                    "Tried to get new filesystem, but it already existed for target uri.",
                    fsaee);
            return FileSystems.getFileSystem(resourceURI);
        }
    }

    // Package protected for feature verification purpose
    void closeFileSystem(URI resourceURI) {
        synchronized (fileSystemLock) {
            try {
                URI fileURI = getFileURI(resourceURI);
                final Integer locks = openFileSystems.computeIfPresent(fileURI,
                        (key, value) -> value - 1);
                if (locks != null && locks == 0) {
                    openFileSystems.remove(fileURI);
                    // Get filesystem is for the file to get the correct
                    // provider
                    FileSystems.getFileSystem(resourceURI).close();
                }
            } catch (IOException ioe) {
                getLogger().error("Failed to close FileSystem for '{}'",
                        resourceURI);
                getLogger().debug("Exception closing FileSystem", ioe);
            }
        }
    }

    @Override
    public boolean serveStaticResource(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String filenameWithPath = getRequestFilename(request);
        if (filenameWithPath.endsWith("/")) {
            // Directories are not static resources although
            // servletContext.getResource will return a URL for them, at
            // least with Jetty
            return false;
        } else if (HandlerHelper.getPublicInternalFolderPaths().stream()
                .anyMatch(path -> filenameWithPath.equals(path))) {
            return false;
        }

        if (HandlerHelper.isPathUnsafe(filenameWithPath)) {
            getLogger().info(HandlerHelper.UNSAFE_PATH_ERROR_MESSAGE_PATTERN,
                    filenameWithPath);
            response.setStatus(HttpStatusCode.BAD_REQUEST.getCode());
            return true;
        }

        if (devModeHandler != null && !"/index.html".equals(filenameWithPath)
                && devModeHandler.serveDevModeRequest(request, response)) {
            // We don't know what the dev server can serve, but if it served
            // something we are happy. There is always an index.html in the dev
            // server but
            // we never want to serve that one directly.
            return true;
        }

        URL resourceUrl = null;
        if (APP_THEME_PATTERN.matcher(filenameWithPath).find()) {
            resourceUrl = vaadinService.getClassLoader()
                    .getResource(VAADIN_WEBAPP_RESOURCES + "VAADIN/static/"
                            + filenameWithPath.replaceFirst("^/", ""));

        } else if (!"/index.html".equals(filenameWithPath)) {
            // index.html needs to be handled by IndexHtmlRequestHandler
            resourceUrl = vaadinService.getClassLoader()
                    .getResource(VAADIN_WEBAPP_RESOURCES
                            + filenameWithPath.replaceFirst("^/", ""));
        }

        if (resourceUrl == null) {
            resourceUrl = getStaticResource(filenameWithPath);
        }
        if (resourceUrl == null && shouldFixIncorrectWebjarPaths()
                && isIncorrectWebjarPath(filenameWithPath)) {
            // Flow issue #4601
            resourceUrl = getStaticResource(
                    fixIncorrectWebjarPath(filenameWithPath));
        }

        if (resourceUrl == null) {
            // Not found in webcontent or in META-INF/resources in some JAR
            return false;
        }

        if (resourceIsDirectory(resourceUrl)) {
            // Directories are not static resources although
            // servletContext.getResource will return a URL for them, at
            // least with Jetty
            return false;
        }

        // There is a resource!

        // Intentionally writing cache headers also for 304 responses
        writeCacheHeaders(filenameWithPath, response);

        long timestamp = writeModificationTimestamp(resourceUrl, request,
                response);
        if (browserHasNewestVersion(request, timestamp)) {
            // Browser is up to date, nothing further to do than set the
            // response code
            response.setStatus(HttpStatusCode.NOT_MODIFIED.getCode());
            return true;
        }
        responseWriter.writeResponseContents(filenameWithPath, resourceUrl,
                request, response);
        return true;
    }

    /**
     * Returns a URL to the static Web resource at the given URI or null if no
     * file found.
     * <p>
     * The resource will be exposed via HTTP (available as a static web
     * resource). The {@code null} return value means that the resource won't be
     * exposed as a Web resource even if it's a resource available via
     * {@link ServletContext}.
     *
     * @param path
     *            the path for the resource
     * @return the resource located at the named path to expose it via Web, or
     *         {@code null} if there is no resource at that path or it should
     *         not be exposed
     * @see VaadinService#getStaticResource(String)
     */
    protected URL getStaticResource(String path) {
        return vaadinService.getStaticResource(path);
    }

    // When referring to webjar resources from application stylesheets (loaded
    // using @StyleSheet) using relative paths, the paths will be different in
    // development mode and in production mode. The reason is that in production
    // mode, the CSS is incorporated into the bundle and when this happens,
    // the relative paths are changed so that they end up pointing to paths like
    // 'frontend-es6/webjars' instead of just 'webjars'.

    // There is a similar problem when referring to webjar resources from
    // application stylesheets inside HTML custom styles (loaded using
    // @HtmlImport). In this case, the paths will also be changed in production.
    // For example, if the HTML file resides in 'frontend/styles' and refers to
    // 'webjars/foo', the path will be changed to refer to
    // 'frontend/styles/webjars/foo', which is incorrect. You could add '../../'
    // to the path in the HTML file but then it would not work in development
    // mode.

    // These paths are changed deep inside the Polymer build chain. It was
    // easier to fix the StaticFileServer to take the incorrect path names
    // into account than fixing the Polymer build chain to generate correct
    // paths. Hence, these methods:

    private boolean shouldFixIncorrectWebjarPaths() {
        return deploymentConfiguration.isProductionMode()
                && deploymentConfiguration.getBooleanProperty(
                        PROPERTY_FIX_INCORRECT_WEBJAR_PATHS, false);
    }

    private boolean isIncorrectWebjarPath(String requestFilename) {
        return INCORRECT_WEBJAR_PATH_REGEX.matcher(requestFilename).lookingAt();
    }

    private String fixIncorrectWebjarPath(String requestFilename) {
        return INCORRECT_WEBJAR_PATH_REGEX.matcher(requestFilename)
                .replaceAll("/webjars/");
    }

    /**
     * Writes the modification timestamp info for the file into the response.
     *
     * @param resourceUrl
     *            the internal URL of the file
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @return the written timestamp or -1 if no timestamp was written
     */
    protected long writeModificationTimestamp(URL resourceUrl,
            HttpServletRequest request, HttpServletResponse response) {
        // Find the modification timestamp
        long lastModifiedTime;
        URLConnection connection = null;
        try {
            connection = resourceUrl.openConnection();
            lastModifiedTime = connection.getLastModified();
            // Remove milliseconds to avoid comparison problems (milliseconds
            // are not returned by the browser in the "If-Modified-Since"
            // header).
            lastModifiedTime = lastModifiedTime - lastModifiedTime % 1000;
            response.setDateHeader("Last-Modified", lastModifiedTime);
            return lastModifiedTime;
        } catch (Exception e) {
            getLogger().trace(
                    "Failed to find out last modified timestamp. Continuing without it.",
                    e);
        } finally {
            try {
                // Explicitly close the input stream to prevent it
                // from remaining hanging
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4257700
                if (connection != null) {
                    InputStream is = connection.getInputStream();
                    if (is != null) {
                        is.close();
                    }
                }
            } catch (IOException e) {
                getLogger().warn("Error closing URLConnection input stream", e);
            }
        }
        return -1L;
    }

    /**
     * Writes cache headers for the file into the response.
     *
     * @param filenameWithPath
     *            the name and path of the file being sent
     * @param response
     *            the response object
     */
    protected void writeCacheHeaders(String filenameWithPath,
            HttpServletResponse response) {
        int resourceCacheTime = getCacheTime(filenameWithPath);
        String cacheControl;
        if (!deploymentConfiguration.isProductionMode()) {
            cacheControl = "no-cache";
        } else if (resourceCacheTime > 0) {
            cacheControl = "max-age=" + resourceCacheTime;
        } else {
            cacheControl = "public, max-age=0, must-revalidate";
        }
        response.setHeader("Cache-Control", cacheControl);
    }

    /**
     * Returns the (decoded) requested file name, relative to the context path.
     * <p>
     * Package private for testing purposes.
     *
     * @param request
     *            the request object
     * @return the requested file name, starting with a {@literal /}
     */
    String getRequestFilename(HttpServletRequest request) {
        // http://localhost:8888/context/servlet/folder/file.js
        // ->
        // /servlet/folder/file.js
        //
        // http://localhost:8888/context/servlet/VAADIN/folder/file.js
        // ->
        // /VAADIN/folder/file.js
        //
        // http://localhost:8888/context/servlet/sw.js
        // ->
        // /sw.js
        if (request.getPathInfo() == null) {
            return request.getServletPath();
        } else if (request.getPathInfo().startsWith("/" + VAADIN_MAPPING)
                || APP_THEME_PATTERN.matcher(request.getPathInfo()).find()
                || request.getPathInfo().startsWith("/sw.js")) {
            return request.getPathInfo();
        }
        return request.getServletPath() + request.getPathInfo();
    }

    /**
     * Calculates the cache lifetime for the given filename in seconds.
     * <p>
     * By default filenames containing ".nocache." return 0, filenames
     * containing ".cache." return one year and all other files return 1 hour.
     *
     * @param filenameWithPath
     *            the name of the file being sent
     * @return cache lifetime for the given filename in seconds
     */
    protected int getCacheTime(String filenameWithPath) {
        /*
         * GWT conventions:
         *
         * - files containing .nocache. will not be cached.
         *
         * - files containing .cache. will be cached for one year.
         *
         * https://developers.google.com/web-toolkit/doc/latest/
         * DevGuideCompilingAndDebugging#perfect_caching
         */
        if (filenameWithPath.contains(".nocache.")) {
            return 0;
        }
        if (filenameWithPath.contains(".cache.")) {
            return 60 * 60 * 24 * 365;
        }
        /*
         * For all other files, the browser is allowed to cache for 1 hour
         * without checking if the file has changed.
         */
        return 3600;
    }

    /**
     * Checks if the browser has an up to date cached version of requested
     * resource using the "If-Modified-Since" header.
     *
     * @param request
     *            The HttpServletRequest from the browser.
     * @param resourceLastModifiedTimestamp
     *            The timestamp when the resource was last modified. -1 if the
     *            last modification time is unknown.
     * @return true if the If-Modified-Since header tells the cached version in
     *         the browser is up to date, false otherwise
     */
    protected boolean browserHasNewestVersion(HttpServletRequest request,
            long resourceLastModifiedTimestamp) {
        assert resourceLastModifiedTimestamp >= -1L;

        if (resourceLastModifiedTimestamp == -1L) {
            // We do not know when it was modified so the browser cannot have an
            // up-to-date version
            return false;
        }
        /*
         * The browser can request the resource conditionally using an
         * If-Modified-Since header. Check this against the last modification
         * time.
         */
        try {
            // If-Modified-Since represents the timestamp of the version cached
            // in the browser
            long headerIfModifiedSince = request
                    .getDateHeader("If-Modified-Since");

            if (headerIfModifiedSince >= resourceLastModifiedTimestamp) {
                // Browser has this an up-to-date version of the resource
                return true;
            }
        } catch (Exception e) {
            // Failed to parse header.
            getLogger().trace("Unable to parse If-Modified-Since", e);
        }
        return false;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(StaticFileServer.class.getName());
    }

}
