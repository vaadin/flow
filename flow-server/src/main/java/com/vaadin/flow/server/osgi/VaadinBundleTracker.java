/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.osgi;

import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.utils.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.HandlerHelper;

/**
 * Bundle tracker to discover all classes in active bundles.
 * <p>
 * The tracker scans for all classes in active bundles which have
 * <b>Vaadin-OSGi-Extender</b> header and report them to the {@link OSGiAccess}
 * instance.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 *
 * @author Vaadin Ltd
 * @since 1.2
 */
public class VaadinBundleTracker extends BundleTracker<Bundle> {

    private final Bundle flowServerBundle;

    private Executor executor = Executors.newSingleThreadExecutor();

    private final AtomicReference<ServiceRegistration<Servlet>> servletPushRegistration = new AtomicReference<>();
    private final AtomicReference<ServiceRegistration<Servlet>> servletClientRegistration = new AtomicReference<>();

    /**
     * Dedicated servlet for serving resources in Flow bundles.
     */
    private static class ResourceServlet extends HttpServlet {

        private final Bundle bundle;
        private final String resourceDirPath;

        public ResourceServlet(Bundle bundle, String resourceDirPath) {
            this.bundle = bundle;
            this.resourceDirPath = resourceDirPath;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                resp.setStatus(HttpURLConnection.HTTP_NOT_FOUND);
                return;
            }
            if (HandlerHelper.isPathUnsafe(pathInfo)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            URL resource = bundle.getResource(resourceDirPath + pathInfo);
            if (resource == null) {
                resp.setStatus(HttpURLConnection.HTTP_NOT_FOUND);
                return;
            }
            try (InputStream stream = resource.openStream()) {
                IOUtils.copy(stream, resp.getOutputStream());
            }
        }
    }

    /**
     * Creates a new instance of a bundle tracker.
     *
     * @param context
     *            the {@code BundleContext} against which the tracking is done
     */
    public VaadinBundleTracker(BundleContext context) {
        super(context, Bundle.ACTIVE | Bundle.RESOLVED, null);
        flowServerBundle = context.getBundle();
    }

    @Override
    public Bundle addingBundle(Bundle bundle, BundleEvent event) {
        if ((bundle.getState() & Bundle.ACTIVE) != 0) {
            // Don't scan every individual bundle until flow-server is active
            if (flowServerBundle.equals(bundle)) {
                // First: scan for servlet context initializers in flow-server
                // bundle to reuse the same logic
                executor.execute(this::scanContextInitializers);
                // Now scan all active bundles for all classes instead of
                // scanning every inidividual activated bundle/
                executor.execute(this::scanActiveBundles);
            } else if (isPushModule(bundle)) {
                registerPushResources(bundle);
            } else if (isClientModule(bundle)) {
                registerClientResources(bundle);
            } else if ((flowServerBundle.getState() & Bundle.ACTIVE) != 0) {
                // If flow-server bundle is already active then scan bundle for
                // classes
                executor.execute(() -> scanActivatedBundle(bundle));
            }
        } else if (event != null && (event.getType() & BundleEvent.STOPPED) > 0
                && isVaadinExtender(bundle)) {
            if (isPushModule(bundle)) {
                unregisterPushResource(bundle);
            } else if (isClientModule(bundle)) {
                unregisterClientResource(bundle);
            } else if (isVaadinExtender(bundle)) {
                // Remove all bundle classes once the bundle becomes stopped
                OSGiAccess.getInstance()
                        .removeScannedClasses(bundle.getBundleId());
            }
        }
        return bundle;
    }

    private void registerPushResources(Bundle pushBundle) {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
                "/VAADIN/static/push/*");
        servletPushRegistration.compareAndSet(null,
                pushBundle.getBundleContext().registerService(Servlet.class,
                        new ResourceServlet(pushBundle,
                                "/META-INF/resources/VAADIN/static/push"),
                        properties));
    }

    private void unregisterPushResource(Bundle pushBundle) {
        ServiceRegistration<Servlet> registration = servletPushRegistration
                .get();
        if (registration != null && registration.getReference().getBundle()
                .getBundleId() == pushBundle.getBundleId()) {
            registration.unregister();
            servletPushRegistration.compareAndSet(registration, null);
        }
    }

    private void registerClientResources(Bundle clientBundle) {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("osgi.http.whiteboard.servlet.pattern",
                "/VAADIN/static/client/*");
        servletClientRegistration.compareAndSet(null,
                clientBundle.getBundleContext().registerService(Servlet.class,
                        new ResourceServlet(clientBundle,
                                "/META-INF/resources/VAADIN/static/client"),
                        properties));
    }

    private void unregisterClientResource(Bundle clientBundle) {
        ServiceRegistration<Servlet> registration = servletClientRegistration
                .get();
        if (registration != null && registration.getReference().getBundle()
                .getBundleId() == clientBundle.getBundleId()) {
            registration.unregister();
            servletClientRegistration.compareAndSet(registration, null);
        }
    }

    private boolean isPushModule(Bundle bundle) {
        return "com.vaadin.flow.push".equals(bundle.getSymbolicName());
    }

    private boolean isClientModule(Bundle bundle) {
        return "com.vaadin.flow.client".equals(bundle.getSymbolicName());
    }

    @SuppressWarnings("unchecked")
    private void scanContextInitializers() {
        Map<Long, Collection<Class<?>>> map = new HashMap<>();
        scanClasses(flowServerBundle, map, this::handleFlowServerClassError);
        Collection<Class<?>> classes = map.get(flowServerBundle.getBundleId());

        Predicate<Class<?>> isInterface = Class::isInterface;

        Collection<Class<? extends ServletContainerInitializer>> initializers = classes
                .stream()
                .filter(ServletContainerInitializer.class::isAssignableFrom)
                .filter(isInterface.negate())
                .map(clazz -> (Class<? extends ServletContainerInitializer>) clazz)
                .collect(Collectors.toList());
        OSGiAccess.getInstance().setServletContainerInitializers(initializers);
    }

    private void scanActivatedBundle(Bundle bundle) {
        if (!isActive(bundle) || !isVaadinExtender(bundle)) {
            return;
        }
        if (OSGiAccess.getInstance().hasInitializers()) {
            Map<Long, Collection<Class<?>>> map = new HashMap<>();
            scanClasses(bundle, map, this::handleBundleClassError);
            OSGiAccess.getInstance().addScannedClasses(map);
        } else {
            executor.execute(() -> scanActivatedBundle(bundle));
        }
    }

    private void scanActiveBundles() {
        Map<Long, Collection<Class<?>>> map = new HashMap<>();
        Stream.of(flowServerBundle.getBundleContext().getBundles())
                .filter(this::isActive).filter(this::isVaadinExtender)
                .forEach(activeBundle -> scanClasses(activeBundle, map,
                        this::handleBundleClassError));
        OSGiAccess.getInstance().addScannedClasses(map);
    }

    private boolean isActive(Bundle bundle) {
        return (bundle.getState() & Bundle.ACTIVE) > 0;
    }

    private boolean isVaadinExtender(Bundle bundle) {
        return !flowServerBundle.equals(bundle) && Boolean.TRUE.toString()
                .equals(bundle.getHeaders().get("Vaadin-OSGi-Extender"));
    }

    private void handleFlowServerClassError(String className,
            Throwable throwable) {
        LoggerFactory.getLogger(VaadinBundleTracker.class)
                .trace("Couldn't load class '{}'", className, throwable);
    }

    private void handleBundleClassError(String className, Throwable throwable) {
        LoggerFactory.getLogger(VaadinBundleTracker.class)
                .warn("Couldn't load class '{}'", className, throwable);
    }

    private void scanClasses(Bundle bundle, Map<Long, Collection<Class<?>>> map,
            BiConsumer<String, Throwable> throwableHandler) {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);

        // get all .class resources of this bundle
        Collection<String> classes = wiring.listResources("/", "*.class",
                /*
                 * Two options: recursive to visit all resources including
                 * sub-directories, and limit resources only to the current
                 * wiring (bundle) avoiding possibly returned classes from
                 * dependencies
                 */
                BundleWiring.LISTRESOURCES_RECURSE
                        | BundleWiring.LISTRESOURCES_LOCAL);

        Collection<Class<?>> bundleClasses = new ArrayList<>();

        for (String clazz : classes) {
            String className = clazz.replaceAll("\\.class$", "").replace('/',
                    '.');
            if (bundle.equals(flowServerBundle)
                    && !className.startsWith("com.vaadin")) {
                continue;
            }
            if ("module-info".equals(className)) {
                // New modular Java info class which we are not interested in
                continue;
            }
            try {
                bundleClasses.add(bundle.loadClass(className));
            } catch (ClassNotFoundException | NoClassDefFoundError exception) {
                throwableHandler.accept(className, exception);
            }
        }
        map.put(bundle.getBundleId(), bundleClasses);
    }

}
