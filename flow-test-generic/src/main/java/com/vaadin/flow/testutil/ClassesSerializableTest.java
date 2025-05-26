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

package com.vaadin.flow.testutil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import static java.lang.reflect.Modifier.isStatic;
import static org.junit.Assert.fail;

/**
 * A superclass for serialization testing. The test scans all the classpath and
 * tries to serialize every single class (except ones from whitelist) in the
 * classpath. Subclasses may adjust the whitelist by overriding
 * {@link #getExcludedPatterns()}, {@link #getBasePackages()},
 * {@link #getJarPattern()}
 *
 * @since 1.0
 */

public abstract class ClassesSerializableTest extends ClassFinder {

    private final Class<?> COMPONENT_CLASS = loadComponent(
            "com.vaadin.flow.component.Component");
    private final Class<?> DIV_CLASS = loadComponent(
            "com.vaadin.flow.component.html.Div");

    private final Class<?> UI_CLASS = loadComponent(
            "com.vaadin.flow.component.UI");

    @SuppressWarnings("WeakerAccess")
    protected Stream<String> getExcludedPatterns() {
        return Stream.of(
                "com\\.vaadin\\.flow\\.data\\.validator\\.BeanValidator\\$LazyFactoryInitializer",
                "com\\.vaadin\\.flow\\.internal\\.BeanUtil\\$LazyValidationAvailability",
                ".*\\.fileupload2\\..*", ".*\\.slf4j\\..*",
                ".*\\.testbench\\..*", ".*\\.testutil\\..*",
                // Various utils with inner classes
                ".*\\.demo\\..*", "com\\.vaadin\\..*Util(s)?(\\$\\w+)?$",
                "com\\.vaadin\\.flow\\.osgi\\.support\\..*",
                "com\\.vaadin\\.flow\\.server\\.osgi\\..*",
                "com\\.vaadin\\.signals\\..*",
                "com\\.vaadin\\.base\\.devserver\\.DevServerOutputTracker.*",
                "com\\.vaadin\\.base\\.devserver\\.viteproxy\\..*",
                "com\\.vaadin\\.base\\.devserver\\.stats..*",
                "com\\.vaadin\\.flow\\.internal\\.VaadinContextInitializer",
                "com\\.vaadin\\.flow\\.internal\\.ApplicationClassLoaderAccess",
                "com\\.vaadin\\.base\\.devserver\\.BrowserLauncher",
                "com\\.vaadin\\.base\\.devserver\\.BrowserLiveReloadAccessorImpl",
                "com\\.vaadin\\.base\\.devserver\\.DebugWindowConnection",
                "com\\.vaadin\\.base\\.devserver\\.DebugWindowConnection\\$DevToolsInterfaceImpl",
                "com\\.vaadin\\.base\\.devserver\\.DevModeHandlerManagerImpl",
                "com\\.vaadin\\.base\\.devserver\\.DevServerWatchDog",
                "com\\.vaadin\\.base\\.devserver\\.DevServerWatchDog\\$WatchDogServer",
                "com\\.vaadin\\.base\\.devserver\\.DevToolsInterface",
                "com\\.vaadin\\.base\\.devserver\\.DevToolsMessageHandler",
                "com\\.vaadin\\.base\\.devserver\\.ExternalDependencyWatcher",
                "com\\.vaadin\\.base\\.devserver\\.FileWatcher",
                "com\\.vaadin\\.base\\.devserver\\.NamedDaemonThreadFactory",
                "com\\.vaadin\\.base\\.devserver\\.IdeIntegration",
                "com\\.vaadin\\.base\\.devserver\\.OpenInCurrentIde.*",
                "com\\.vaadin\\.base\\.devserver\\.RestartMonitor",
                "com\\.vaadin\\.base\\.devserver\\.ThemeLiveUpdater",
                "com\\.vaadin\\.base\\.devserver\\.editor..*",
                "com\\.vaadin\\.base\\.devserver\\.themeeditor..*",
                "com\\.vaadin\\.base\\.devserver\\.util\\.BrowserLauncher",
                "com\\.vaadin\\.base\\.devserver\\.util\\.net\\.PortProber",
                "com\\.vaadin\\.base\\.devserver\\.util\\.net\\.FixedIANAPortRange",
                "com\\.vaadin\\.base\\.devserver\\.util\\.net\\.EphemeralPortRangeDetector",
                "com\\.vaadin\\.base\\.devserver\\.util\\.net\\.LinuxEphemeralPortRangeDetector",
                "com\\.vaadin\\.flow\\.data\\.provider\\.InMemoryDataProviderHelpers",
                "com\\.vaadin\\.flow\\.di\\.InstantiatorFactory",
                "com\\.vaadin\\.flow\\.di\\.Lookup(\\$.*)?",
                "com\\.vaadin\\.flow\\.di\\.ResourceProvider",
                "com\\.vaadin\\.flow\\.di\\.AbstractLookupInitializer",
                "com\\.vaadin\\.flow\\.di\\.LookupInitializer(\\$.*)?",
                "com\\.vaadin\\.flow\\.di\\.OneTimeInitializerPredicate",
                "com\\.vaadin\\.flow\\.dom\\.ElementConstants",
                "com\\.vaadin\\.flow\\.component\\.board\\.internal\\.FunctionCaller",
                "com\\.vaadin\\.flow\\.component\\.grid\\.ColumnGroupHelpers",
                "com\\.vaadin\\.flow\\.component\\.textfield\\.SlotHelpers",
                "com\\.vaadin\\.flow\\.component\\.orderedlayout\\.FlexConstants",
                "com\\.vaadin\\.flow\\.component\\.PropertyDescriptors(\\$.*)?",
                "com\\.vaadin\\.flow\\.component\\.Shortcuts",
                "com\\.vaadin\\.flow\\.component\\.dnd\\.osgi\\.DndConnectorResource",
                "com\\.vaadin\\.flow\\.component\\.internal\\.DeadlockDetectingCompletableFuture",
                "com\\.vaadin\\.flow\\.function\\.VaadinApplicationInitializationBootstrap",
                "com\\.vaadin\\.flow\\.hotswap\\.HotswapCompleteEvent",
                "com\\.vaadin\\.flow\\.hotswap\\.Hotswapper",
                "com\\.vaadin\\.flow\\.hotswap\\.VaadinHotswapper",
                "com\\.vaadin\\.flow\\.internal\\.BrowserLiveReloadAccessor",
                "com\\.vaadin\\.flow\\.internal\\.BrowserLiveReloadAccess",
                "com\\.vaadin\\.flow\\.internal\\.BrowserLiveReload",
                "com\\.vaadin\\.flow\\.internal\\.BrowserLiveReloadImpl",
                "com\\.vaadin\\.flow\\.internal\\.DevModeHandlerManager",
                "com\\.vaadin\\.flow\\.internal\\.DevModeHandler",
                "com\\.vaadin\\.flow\\.internal\\.JsonSerializer",
                "com\\.vaadin\\.flow\\.internal\\.JsonCodec",
                "com\\.vaadin\\.flow\\.internal\\.JacksonCodec",
                "com\\.vaadin\\.flow\\.internal\\.ReflectionCacheHotswapper",
                "com\\.vaadin\\.flow\\.internal\\.UsageStatistics(\\$.*)?",
                "com\\.vaadin\\.flow\\.internal\\.nodefeature\\.NodeFeatureRegistry",
                "com\\.vaadin\\.flow\\.internal\\.nodefeature\\.NodeFeatures",
                "com\\.vaadin\\.flow\\.internal\\.CustomElementNameValidator",
                "com\\.vaadin\\.flow\\.router\\.HighlightActions",
                "com\\.vaadin\\.flow\\.router\\.HighlightConditions",
                "com\\.vaadin\\.flow\\.router\\.ParameterDeserializer",
                "com\\.vaadin\\.flow\\.router\\.NavigationStateBuilder",
                "com\\.vaadin\\.flow\\.router\\.AbstractRouteNotFoundError\\$LazyInit",
                "com\\.vaadin\\.flow\\.router\\.internal\\.RouteRegistryHotswapper",
                "com\\.vaadin\\.flow\\.internal\\.JavaScriptSemantics",
                "com\\.vaadin\\.flow\\.internal\\.nodefeature\\.NodeProperties",
                "com\\.vaadin\\.flow\\.internal\\.AnnotationReader",
                "com\\.vaadin\\.flow\\.server\\.StaticFileHandlerFactory",
                "com\\.vaadin\\.flow\\.server\\.dau\\.DauEnforcementException",
                "com\\.vaadin\\.flow\\.server\\.dau\\.FlowDauIntegration",
                "com\\.vaadin\\.flow\\.server\\.dau\\.FlowDauIntegration\\$TrackingDetails",
                "com\\.vaadin\\.flow\\.server\\.communication\\.ServerRpcHandler\\$LazyInvocationHandlers",
                "com\\.vaadin\\.flow\\.server\\.VaadinServletRequest",
                "com\\.vaadin\\.flow\\.server\\.VaadinServletResponse",
                "com\\.vaadin\\.flow\\.server\\.auth\\.NavigationContext",
                "com\\.vaadin\\.flow\\.server\\.menu\\.AvailableViewInfo\\$.+",
                "com\\.vaadin\\.flow\\.server\\.startup\\.AnnotationValidator",
                "com\\.vaadin\\.flow\\.server\\.startup\\.AppShellPredicate",
                "com\\.vaadin\\.flow\\.server\\.startup\\.ApplicationConfigurationFactory",
                "com\\.vaadin\\.flow\\.server\\.startup\\.ApplicationRouteRegistry\\$RouteRegistryServletContextListener",
                "com\\.vaadin\\.flow\\.server\\.startup\\.ApplicationRouteRegistry\\$OSGiRouteRegistry",
                "com\\.vaadin\\.flow\\.server\\.startup\\.ApplicationRouteRegistry\\$OSGiDataCollector",
                "com\\.vaadin\\.flow\\.server\\.startup\\.ClassLoaderAwareServletContainerInitializer",
                "com\\.vaadin\\.flow\\.server\\.startup\\.VaadinServletContextStartupInitializer",
                "com\\.vaadin\\.flow\\.server\\.startup\\.VaadinContextStartupInitializer",
                "com\\.vaadin\\.flow\\.server\\.startup\\.ServletDeployer",
                "com\\.vaadin\\.flow\\.server\\.startup\\.ServletDeployer\\$StubServletConfig",
                "com\\.vaadin\\.flow\\.server\\.startup\\.ServletContextListeners",
                "com\\.vaadin\\.flow\\.server\\.startup\\.DeferredServletContextInitializers(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.startup\\.DevModeInitializer(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.startup\\.LookupServletContainerInitializer(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.communication.JSR356WebsocketInitializer(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.BootstrapHandler(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.InlineTargets",
                "com\\.vaadin\\.flow\\.server\\.AppShellSettings",
                "com\\.vaadin\\.flow\\.server\\.communication\\.IndexHtmlResponse",
                "com\\.vaadin\\.flow\\.server\\.communication\\.PushHandler(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.communication\\.PushRequestHandler(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.communication\\.JavaScriptBootstrapHandler(\\$.*)?",
                "com\\.vaadin\\.flow\\.internal\\.menu\\.MenuRegistry(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.menu\\.MenuConfiguration(\\$.*)?",
                "com\\.vaadin\\.flow\\.templatemodel\\.PathLookup",
                "com\\.vaadin\\.flow\\.server\\.startup\\.ErrorNavigationTargetInitializer",
                "com\\.vaadin\\.flow\\.server\\.startup\\.RouteRegistryInitializer",
                "com\\.vaadin\\.flow\\.server\\.startup\\.WebComponentConfigurationRegistryInitializer",
                "com\\.vaadin\\.flow\\.server\\.VaadinResponse",
                "com\\.vaadin\\.flow\\.component\\.Key",
                "com\\.vaadin\\.flow\\.server\\.VaadinRequest",
                "com\\.vaadin\\.flow\\.server\\.DevServerWatchDog(\\$.*)?",
                "com\\.vaadin\\.flow\\.router\\.DefaultRoutePathProvider",
                "com\\.vaadin\\.flow\\.router\\.RoutePathProvider",
                "com\\.vaadin\\.flow\\.router\\.RouteNotFoundError\\$LazyInit",
                "com\\.vaadin\\.flow\\.router\\.internal\\.RouteSegment\\$RouteSegmentValue",
                // De-facto abstract class
                "com\\.vaadin\\.flow\\.component\\.HtmlComponent",
                // De-facto abstract class
                "com\\.vaadin\\.flow\\.component\\.HtmlContainer",
                "com\\.vaadin\\.flow\\.component\\.template\\.internal\\.AttributeInitializationStrategy",
                "com\\.vaadin\\.flow\\.component\\.template\\.internal\\.PropertyInitializationStrategy",
                "com\\.vaadin\\.flow\\.component\\.template\\.internal\\.ElementInitializationStrategy",
                "com\\.vaadin\\.flow\\.component\\.template\\.internal\\.AbstractInjectableElementInitializer",
                "com\\.vaadin\\.flow\\.dom\\.impl\\.ThemeListImpl\\$ThemeListIterator",
                "com\\.vaadin\\.flow\\.templatemodel\\.PropertyMapBuilder(\\$.*)?",
                "com\\.vaadin\\.flow\\.internal\\.ReflectionCache",
                "com\\.vaadin\\.flow\\.component\\.template\\.internal\\.IdCollector",
                "com\\.vaadin\\.flow\\.component\\.template\\.internal\\.InjectableFieldConsumer",
                "com\\.vaadin\\.flow\\.component\\.template\\.internal\\.ParserData",
                "com\\.vaadin\\.flow\\.component\\.internal\\.ComponentMetaData(\\$.*)?",
                "com\\.vaadin\\.flow\\.component\\.internal\\.ComponentTracker",
                "com\\.vaadin\\.flow\\.dom\\.ElementFactory",
                "com\\.vaadin\\.flow\\.dom\\.NodeVisitor",
                "com\\.vaadin\\.flow\\.internal\\.nodefeature\\.NodeList(\\$.*)?",
                "com\\.vaadin\\.flow\\.templatemodel\\.PropertyFilter",
                "com\\.vaadin\\.flow\\.internal\\.ReflectTools(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.FutureAccess",
                "com\\.vaadin\\.flow\\.internal\\.nodefeature\\.ElementPropertyMap\\$PutResult",
                "com\\.vaadin\\.flow\\.client\\.osgi\\.OSGiClientStaticResource(\\$.*)?",
                "com\\.vaadin\\.flow\\.osgi\\.support\\.OsgiVaadinContributor(\\$.*)?",
                "com\\.vaadin\\.flow\\.osgi\\.support\\.OsgiVaadinStaticResource(\\$.*)?",
                "com\\.vaadin\\.flow\\.osgi\\.support\\.VaadinResourceTrackerComponent(\\$.*)?",
                "com\\.vaadin\\.flow\\.client\\.osgi\\..*",
                "com\\.vaadin\\.flow\\.data\\.osgi\\..*",
                "com\\.vaadin\\.flow\\.push\\.osgi\\.PushOsgiStaticResource",
                "com\\.vaadin\\.flow\\.component\\.internal\\.HtmlImportParser",
                "com\\.vaadin\\.flow\\.server\\.VaadinService\\$.*VaadinThreadFactory",
                "com\\.vaadin\\.flow\\.server\\.webcomponent\\.WebComponentGenerator",
                "com\\.vaadin\\.flow\\.server\\.communication\\.WebComponentBootstrapHandler(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.streams\\.TransferContext",
                "com\\.vaadin\\.flow\\.server\\.streams\\.DownloadEvent",
                "com\\.vaadin\\.flow\\.server\\.communication\\.StreamRequestHandler\\$PathData",
                "com\\.vaadin\\.flow\\.server\\.streams\\.UploadEvent",
                "com\\.vaadin\\.flow\\.server\\.streams\\.UploadMetadata",

                "com\\.vaadin\\.flow\\.server\\.DevModeHandler(\\$.*)?",
                // Frontend tasks classes which are not stored anywhere but used
                // only once
                "com\\.vaadin\\.flow\\.server\\.frontend\\.scanner\\..*",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.CssBundler",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.FrontendTools",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.JarContentsManager",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.VersionsJsonConverter",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.VersionsJsonFilter",

                "com\\.vaadin\\.flow\\.server\\.frontend\\.AbstractUpdateImports",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.FallibleCommand",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.AbstractFileGeneratorFallibleCommand",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.GeneratedFilesSupport",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.NodeTasks",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.NodeUpdater",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.Task.*",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.AbstractTaskClientGenerator",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.EndpointGeneratorTaskFactory",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.CvdlProducts",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.GenerateMainImports",

                "com\\.vaadin\\.flow\\.server\\.frontend\\.webpush\\.WebPushSubscription",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.webpush\\.WebPushRegistration",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.webpush\\.WebPushMessage",

                // Flow client classes
                "com\\.vaadin\\.client\\..*",
                "com\\.vaadin\\.flow\\.linker\\.ClientEngineLinker",
                "com\\.vaadin\\.flow\\.linker\\.ClientEngineLinker\\$Script",

                // Node downloader classes
                "com\\.vaadin\\.flow\\.server\\.frontend\\.installer\\.DefaultArchiveExtractor",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.installer\\.ArchiveExtractor",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.installer\\.DefaultFileDownloader(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.installer\\.FileDownloader(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.installer\\.NodeInstaller",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.installer\\.NodeInstaller\\$InstallData",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.installer\\.Platform",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.installer\\.ProxyConfig\\$Proxy",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.installer\\.ProxyConfig",
                "com\\.vaadin\\.flow\\.server\\.frontend\\.ProxyFactory",

                // Various test classes
                ".*\\.test(s)?\\..*", ".*Test.*",
                "com\\.vaadin\\.flow\\.server\\.MockVaadinServletService",
                "com\\.vaadin\\.flow\\.server\\.MockServletServiceSessionSetup",
                "com\\.vaadin\\.flow\\.server\\.MockServletConfig",
                "com\\.vaadin\\.flow\\.server\\.MockServletContext",
                "com\\.vaadin\\.flow\\.templatemodel\\.Bean",
                "com\\.vaadin\\.flow\\.internal\\.HasCurrentService",
                "com\\.vaadin\\.flow\\.component\\.ValueChangeMonitor",
                "com\\.vaadin\\.flow\\.templatemodel\\.BeanContainingBeans(\\$.*)?");
    }

    /**
     * Performs actual serialization/deserialization
     *
     * @param <T>
     *            the type of the instance
     * @param instance
     *            the instance
     * @return the copy of the source object
     * @throws Throwable
     *             if something goes wrong.
     */
    @SuppressWarnings({ "UnusedReturnValue", "WeakerAccess" })
    public <T> T serializeAndDeserialize(T instance) throws Throwable {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bs);
        out.writeObject(instance);
        byte[] data = bs.toByteArray();
        ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(data));

        @SuppressWarnings("unchecked")
        T readObject = (T) in.readObject();

        return readObject;
    }

    /**
     * The method is called right after a class instantiation and might be
     * overriden by subclasses to reset thread local values (ex. current UI).
     *
     * @see #setupThreadLocals
     */
    @SuppressWarnings("WeakerAccess")
    protected void resetThreadLocals() {
    }

    /**
     * The method is called right a class instantiation and might be overriden
     * by subclasses to install some necessary thread local values (ex. current
     * UI).
     *
     * @see #resetThreadLocals
     */
    @SuppressWarnings("WeakerAccess")
    protected void setupThreadLocals() {
    }

    /**
     * Tests that all the relevant classes and interfaces under
     * {@link #getBasePackages} implement Serializable.
     *
     * @throws Throwable
     *             serialization goes wrong
     */
    @Test
    public void classesSerializable() throws Throwable {
        List<String> rawClasspathEntries = getRawClasspathEntries();

        List<String> classes = new ArrayList<>();
        List<Pattern> excludes = getExcludedPatterns().map(Pattern::compile)
                .collect(Collectors.toList());
        for (String location : rawClasspathEntries) {
            if (!isTestClassPath(location)) {
                classes.addAll(findServerClasses(location, excludes));
            }
        }

        ArrayList<Field> nonSerializableFunctionFields = new ArrayList<>();

        List<Class<?>> nonSerializableClasses = new ArrayList<>();
        for (String className : classes) {
            Class<?> cls = Class.forName(className);
            // Don't add classes that have a @Ignore annotation on the class
            if (isTestClass(cls)) {
                continue;
            }

            // report fields that use lambda types that won't be serializable
            // (also in synthetic classes)
            Stream.of(cls.getDeclaredFields())
                    .filter(field -> isFunctionalType(field.getGenericType()))
                    .filter(field -> !isStatic(field.getModifiers()))
                    .forEach(nonSerializableFunctionFields::add);

            // skip annotations and synthetic classes
            if (cls.isAnnotation() || cls.isSynthetic()) {
                continue;
            }

            if (!cls.isInterface()
                    && !Modifier.isAbstract(cls.getModifiers())) {
                serializeAndDeserialize(cls);
                serializeAndDeserializeInsideContainer(cls);
            }

            // report non-serializable classes and interfaces
            if (!Serializable.class.isAssignableFrom(cls)) {
                nonSerializableClasses.add(cls);
                // TODO easier to read when testing
                // System.err.println(cls);
            }
        }

        // useful failure message including all non-serializable classes and
        // interfaces
        if (!nonSerializableClasses.isEmpty()) {
            failSerializableClasses(nonSerializableClasses);
        }

        if (!nonSerializableFunctionFields.isEmpty()) {
            failSerializableFields(nonSerializableFunctionFields);
        }
    }

    private void serializeAndDeserialize(Class<?> clazz) {
        try {
            Object instance = instantiate(clazz);
            serializeAndDeserialize(instance);
        } catch (Throwable e) {
            throw new AssertionError(clazz.getName(), e);
        }
    }

    private void serializeAndDeserializeInsideContainer(Class<?> clazz) {
        try {
            if (DIV_CLASS == null || COMPONENT_CLASS == null) {
                return;
            }
            if (!COMPONENT_CLASS.isAssignableFrom(clazz)) {
                return;
            }
            if (UI_CLASS != null && UI_CLASS.isAssignableFrom(clazz)) {
                return;
            }
            Object div = instantiate(DIV_CLASS);
            Object instance = instantiate(clazz);
            if (instance == null) {
                return;
            }

            Object divElement = getElement(div);
            Optional<Method> setChild = Stream
                    .of(divElement.getClass().getMethods())
                    .filter(method -> "setChild".equals(method.getName()))
                    .findFirst();
            setChild.get().invoke(divElement, 0, getElement(instance));
            serializeAndDeserialize(div);
        } catch (Throwable e) {
            throw new AssertionError(clazz.getName(), e);
        }
    }

    private Object getElement(Object obj) throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Method method = obj.getClass().getMethod("getElement");
        return method.invoke(obj);
    }

    private Object instantiate(Class<?> clazz) throws InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Optional<Constructor<?>> defaultCtor = Stream
                .of(clazz.getDeclaredConstructors())
                .filter(ctor -> ctor.getParameterCount() == 0).findFirst();
        if (!defaultCtor.isPresent()) {
            return null;
        }
        defaultCtor.get().setAccessible(true);
        setupThreadLocals();
        try {
            return defaultCtor.get().newInstance();
        } finally {
            resetThreadLocals();
        }
    }

    private void failSerializableFields(
            List<Field> nonSerializableFunctionFields) {
        String nonSerializableString = nonSerializableFunctionFields.stream()
                .map(field -> String.format("%s.%s",
                        field.getDeclaringClass().getName(), field.getName()))
                .collect(Collectors.joining(", "));

        fail("Fields with functional types that are not serializable: "
                + nonSerializableString);
    }

    private void failSerializableClasses(
            List<Class<?>> nonSerializableClasses) {
        StringBuilder nonSerializableString = new StringBuilder();
        for (Class<?> c : nonSerializableClasses) {
            nonSerializableString.append(",\n").append(c.getName());
            if (c.isAnonymousClass()) {
                nonSerializableString.append("(super: ")
                        .append(c.getSuperclass().getName())
                        .append(", interfaces: ");
                for (Class<?> i : c.getInterfaces()) {
                    nonSerializableString.append(i.getName()).append(",");
                }
                nonSerializableString.append(")");
            }
        }
        fail("Serializable not implemented by the following classes and interfaces: "
                + nonSerializableString);

    }

    private Class<?> loadComponent(String fqn) {
        try {
            return Class.forName(fqn);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}
