/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.quarkus.deployment;

import jakarta.inject.Inject;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.undertow.deployment.ServletDeploymentManagerBuildItem;
import io.quarkus.vertx.http.deployment.DefaultRouteBuildItem;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.managed.ManagedServiceInterceptor;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.container.JSR356AsyncSupport;
import org.atmosphere.cpr.AsyncSupportListener;
import org.atmosphere.cpr.AsyncSupportListenerAdapter;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereFrameworkListener;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEventListener;
import org.atmosphere.cpr.AtmosphereResourceImpl;
import org.atmosphere.cpr.BroadcastFilter;
import org.atmosphere.cpr.DefaultAnnotationProcessor;
import org.atmosphere.cpr.DefaultAtmosphereResourceFactory;
import org.atmosphere.cpr.DefaultAtmosphereResourceSessionFactory;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.atmosphere.cpr.DefaultBroadcasterFactory;
import org.atmosphere.cpr.DefaultMetaBroadcaster;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.atmosphere.interceptor.SuspendTrackerInterceptor;
import org.atmosphere.util.AbstractBroadcasterProxy;
import org.atmosphere.util.ExcludeSessionBroadcaster;
import org.atmosphere.util.SimpleBroadcaster;
import org.atmosphere.util.VoidAnnotationProcessor;
import org.atmosphere.websocket.protocol.SimpleHttpProtocol;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.Opcodes;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.MenuData;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import com.vaadin.flow.server.menu.AvailableViewInfo;
import com.vaadin.flow.server.menu.RouteParamType;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.signals.Id;
import com.vaadin.quarkus.deployment.nativebuild.AtmospherePatches;
import com.vaadin.quarkus.graal.AtmosphereDeferredInitializerRecorder;
import com.vaadin.quarkus.graal.DelayedSchedulerExecutorsFactory;

/**
 * A processor that applies necessary steps to build a native image for a Vaadin
 * application.
 * <p>
 * <ul>
 * <li>Patches Atmosphere
 * <li>Defers Atmosphere initialization at RUNTIME_INIT
 * <li>Generates stub classes for DAU integration if license checker is not
 * present at runtime
 * <li>Registers classes for reflection
 * <li>Registers the JDK proxies of the JavaScript definitions
 * </ul>
 */
public class VaadinQuarkusNativeProcessor {

    private static final DotName JS_DEFINITION = DotName
            .createSimple(JsDefinition.class);

    @BuildStep(onlyIf = IsNativeBuild.class)
    void patchAtmosphere(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> producer) {
        AtmospherePatches patcher = new AtmospherePatches(
                index.getComputingIndex());
        patcher.apply(producer);
    }

    @BuildStep(onlyIf = IsNativeBuild.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    @Produce(DefaultRouteBuildItem.class)
    void deferAtmosphereInit(AtmosphereDeferredInitializerRecorder recorder,
            ServletDeploymentManagerBuildItem deploymentManager) {
        recorder.initAtmosphere(deploymentManager.getDeploymentManager());
    }

    /*
     * If license-checker is not present at runtime, create stub Dau integration
     * classes that thrown an exception if invoked. This will happen only if the
     * application is build with subscription key and the license-checker is not
     * configured as explicit project dependency.
     */
    @BuildStep(onlyIf = IsNativeBuild.class)
    void generateDummyDauClassesIfLicenseCheckerIsNotPresent(
            BuildProducer<GeneratedNativeImageClassBuildItem> producer) {
        String dauIntegration = "com.vaadin.pro.licensechecker.dau.DauIntegration";
        if (!QuarkusClassLoader.isClassPresentAtRuntime(dauIntegration)) {
            try (ClassCreator classCreator = new ClassCreator(
                    (className, bytes) -> producer.produce(
                            new GeneratedNativeImageClassBuildItem(className,
                                    bytes)),
                    dauIntegration, null, Object.class.getName())) {
                MethodCreator methodCreator = classCreator.getMethodCreator(
                        "startTracking", void.class, String.class);
                methodCreator
                        .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                methodCreator.throwException(RuntimeException.class,
                        "DauIntegration.startTracking invoked but license-checker is not available");
                methodCreator.returnValue(null);

                methodCreator = classCreator.getMethodCreator("stopTracking",
                        void.class);
                methodCreator
                        .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                methodCreator.throwException(RuntimeException.class,
                        "DauIntegration.stopTracking invoked but license-checker is not available");
                methodCreator.returnValue(null);

                methodCreator = classCreator.getMethodCreator("trackUser",
                        void.class, String.class, String.class);
                methodCreator
                        .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                methodCreator.throwException(RuntimeException.class,
                        "DauIntegration.trackUser invoked but license-checker is not available");
                methodCreator.returnValue(null);

                methodCreator = classCreator.getMethodCreator("shouldEnforce",
                        boolean.class);
                methodCreator
                        .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                methodCreator.throwException(RuntimeException.class,
                        "DauIntegration.shouldEnforce invoked but license-checker is not available");
                methodCreator.returnValue(null);

                methodCreator = classCreator.getMethodCreator("newTrackingHash",
                        String.class);
                methodCreator
                        .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                methodCreator.throwException(RuntimeException.class,
                        "DauIntegration.newTrackingHash invoked but license-checker is not available");
                methodCreator.returnValue(null);
            }
            try (ClassCreator classCreator = new ClassCreator(
                    (className, bytes) -> producer.produce(
                            new GeneratedNativeImageClassBuildItem(className,
                                    bytes)),
                    "com.vaadin.pro.licensechecker.dau.EnforcementException",
                    null, RuntimeException.class.getName())) {
                MethodCreator ctorCreator = classCreator
                        .getConstructorCreator(new String[0]);
                ctorCreator.setModifiers(Opcodes.ACC_PUBLIC);
                ctorCreator.returnValue(null);
            }
        }
    }

    /*
     * Some Vaadin Java components like Grid and Combobox define methods using
     * VaadinSpringDataHelper utility from vaadin-spring module. In a Quarkus
     * application that module is usually not present, so we provide a stub
     * implementation that throws exception when methods are invoked.
     */
    @BuildStep(onlyIf = IsNativeBuild.class)
    void generateVaadinSpringDataHelpers(
            BuildProducer<GeneratedNativeImageClassBuildItem> producer) {
        String vaadinSpringDataHelperClassName = "com.vaadin.flow.spring.data.VaadinSpringDataHelpers";

        if (!QuarkusClassLoader
                .isClassPresentAtRuntime(vaadinSpringDataHelperClassName)) {
            ClassCreator.Builder builder = ClassCreator.interfaceBuilder()
                    .className(vaadinSpringDataHelperClassName)
                    .interfaces(Serializable.class.getName())
                    .classOutput((className, bytes) -> producer.produce(
                            new GeneratedNativeImageClassBuildItem(className,
                                    bytes)));
            try (ClassCreator classCreator = builder.build()) {

                MethodCreator methodCreator = classCreator.getMethodCreator(
                        "toSpringDataSort",
                        "Lorg/springframework/data/domain/Sort;",
                        "Lcom/vaadin/flow/data/provider/Query;");
                methodCreator
                        .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                methodCreator.throwException(RuntimeException.class,
                        "VaadinSpringDataHelpers.toSpringDataSort invoked but vaadin-spring is not available");
                methodCreator.returnValue(null);

                methodCreator = classCreator.getMethodCreator(
                        "toSpringPageRequest",
                        "Lorg/springframework/data/domain/PageRequest;",
                        "Lcom/vaadin/flow/data/provider/Query;");
                methodCreator
                        .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                methodCreator.throwException(RuntimeException.class,
                        "VaadinSpringDataHelpers.toSpringPageRequest invoked but vaadin-spring is not available");
                methodCreator.returnValue(null);

                methodCreator = classCreator.getMethodCreator(
                        "fromPagingRepository",
                        "Lcom/vaadin/flow/data/provider/CallbackDataProvider$FetchCallback;",
                        "Lcom/vaadin/flow/data/repository/PagingAndSortingRepository;");
                methodCreator
                        .setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                methodCreator.throwException(RuntimeException.class,
                        "VaadinSpringDataHelpers.fromPagingRepository invoked but vaadin-spring is not available");
                methodCreator.returnValue(null);
            }
        }
    }

    /*
     * Element.executeJs(Class) hands a JavaScript definition to
     * JsDefinitionProxy, which implements it with a JDK proxy, and a native
     * image only builds a proxy that is registered at build time. The interface
     * itself is registered for reflection as well, as that is how the
     * annotations the definition is validated against are read.
     */
    @BuildStep(onlyIf = IsNativeBuild.class)
    void registerJsDefinitionProxies(CombinedIndexBuildItem combinedIndex,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        Set<String> definitions = getJsDefinitions(combinedIndex.getIndex())
                .stream().map(classInfo -> classInfo.name().toString())
                .collect(Collectors.toSet());
        if (definitions.isEmpty()) {
            return;
        }
        definitions.stream().map(NativeImageProxyDefinitionBuildItem::new)
                .forEach(proxyDefinition::produce);
        reflectiveClass.produce(ReflectiveClassBuildItem
                .builder(definitions.toArray(String[]::new)).methods().build());
    }

    // Visible for testing
    Set<ClassInfo> getJsDefinitions(IndexView index) {
        return getAnnotatedClasses(index, JS_DEFINITION).stream()
                .filter(ClassInfo::isInterface).collect(Collectors.toSet());
    }

    @BuildStep(onlyIf = IsNativeBuild.class)
    void vaadinNativeSupport(CombinedIndexBuildItem combinedIndex,
            BuildProducer<RuntimeInitializedPackageBuildItem> runtimeInitializedPackage,
            BuildProducer<NativeImageResourcePatternsBuildItem> nativeImageResource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {

        IndexView index = combinedIndex.getIndex();

        // FlowShortcut.js and FlowWebPush.js are client-side helpers Flow reads
        // from the classpath at runtime. The rest of META-INF/frontend is
        // build-time input for Vite and is served from the production bundle,
        // so it is deliberately left out.
        nativeImageResource.produce(NativeImageResourcePatternsBuildItem
                .builder()
                .includeGlobs("META-INF/VAADIN/**", "com/vaadin/**",
                        "vaadin-i18n/**", "META-INF/frontend/FlowShortcut.js",
                        "META-INF/frontend/FlowWebPush.js",
                        "org/atmosphere/util/version.properties",
                        "META-INF/maven/com.vaadin/vaadin-core/pom.properties",
                        "vaadin-featureflags.properties")
                .build());

        runtimeInitializedPackage
                .produce(new RuntimeInitializedPackageBuildItem(
                        "org.atmosphere.util.analytics"));

        // JSON serialization
        reflectiveClass.produce(ReflectiveClassBuildItem
                .builder(AvailableViewInfo.class,
                        AvailableViewInfo.DetailSerializer.class,
                        AvailableViewInfo.DetailDeserializer.class,
                        MenuData.class, RouteParamType.class, Id.class,
                        Dependency.class)
                .constructors().methods().fields().build());

        Set<ClassInfo> classes = new HashSet<>();
        classes.add(index.getClassByName(AccessDeniedException.class));
        classes.add(index.getClassByName(NotFoundException.class));
        classes.addAll(
                getAnnotatedClasses(index, DotName.createSimple(Route.class)));
        classes.addAll(getAnnotatedClasses(index,
                DotName.createSimple(RouteAlias.class)));
        classes.addAll(
                getAnnotatedClasses(index, DotName.createSimple(Layout.class)));
        classes.addAll(
                getAnnotatedClasses(index, DotName.createSimple(Menu.class)));
        classes.addAll(getAnnotatedClasses(index,
                DotName.createSimple(AccessDeniedErrorRouter.class)));
        classes.addAll(
                index.getAllKnownImplementations(AppShellConfigurator.class));
        classes.addAll(getCommonComponentClasses(index));
        classes.addAll(
                index.getAllKnownSubclasses(AccessDeniedException.class));
        classes.addAll(index.getAllKnownSubclasses(NotFoundException.class));
        classes.addAll(index.getAllKnownSubclasses(Component.class));
        classes.addAll(index.getAllKnownSubclasses(RouterLayout.class));
        classes.addAll(index.getAllKnownSubclasses(HasErrorParameter.class));
        classes.addAll(index.getAllKnownSubclasses(ComponentEvent.class));
        classes.addAll(index.getAllKnownSubclasses(HasUrlParameter.class));
        classes.add(index.getClassByName(
                "com.vaadin.flow.component.littemplate.LitTemplateParser$LitTemplateParserFactory"));
        classes.addAll(index.getAllKnownSubclasses(
                "com.vaadin.flow.data.converter.Converter"));

        reflectiveClass
                .produce(
                        ReflectiveClassBuildItem
                                .builder(classes.stream()
                                        .filter(Objects::nonNull)
                                        .map(classInfo -> classInfo.name()
                                                .toString())
                                        .toArray(String[]::new))
                                .constructors().methods().fields().build());

        Set<ClassInfo> classesWithHierarchy = new HashSet<>();
        classesWithHierarchy.addAll(getJsonClasses(index));
        classesWithHierarchy.addAll(detectClientCallablesTypes(index));
        classesWithHierarchy.stream().map(
                c -> ReflectiveHierarchyBuildItem.builder(c.name()).build())
                .forEach(reflectiveHierarchy::produce);

        registerAtmosphereClasses(reflectiveClass);
    }

    Set<ClassInfo> detectClientCallablesTypes(IndexView index) {

        // Get all known subclasses of Component, including Component itself
        Set<DotName> componentClasses = new HashSet<>();
        componentClasses.add(DotName.createSimple(Component.class.getName()));
        index.getAllKnownSubclasses(Component.class).stream()
                .map(ClassInfo::name).forEach(componentClasses::add);

        // Return a predicate that checks if the declaring class is in the set
        Predicate<MethodInfo> componentPredicate = method -> componentClasses
                .contains(method.declaringClass().name());

        return index.getAnnotations(DotName.createSimple(ClientCallable.class))
                .stream().map(ann -> ann.target().asMethod())
                .filter(componentPredicate)
                .flatMap(m -> TypeInspector.collectTypes(m, index).stream())
                .collect(Collectors.toSet());
    }

    private Set<ClassInfo> getJsonClasses(IndexView index) {
        Set<ClassInfo> classes = new HashSet<>();
        Set<ClassInfo> jsonTypes = getAnnotatedClasses(index,
                DotName.createSimple(JsonSubTypes.class));
        DotName jsonSubTypes = DotName
                .createSimple(JsonSubTypes.class.getName());
        jsonTypes.stream()
                .filter(classInfo -> classInfo.hasAnnotation(jsonSubTypes))
                .flatMap(classInfo -> classInfo.annotation(jsonSubTypes).value()
                        .asArrayList().stream())
                .map(annotationValue -> index.getClassByName(
                        annotationValue.asNested().value().asClass().name()))
                .collect(Collectors.toCollection(() -> classes));
        classes.addAll(jsonTypes);
        return classes;
    }

    private Set<ClassInfo> getAnnotatedClasses(IndexView index,
            DotName annotation) {
        return index.getAnnotations(annotation).stream().filter(
                ann -> ann.target().kind() == AnnotationTarget.Kind.CLASS)
                .map(ann -> ann.target().asClass()).collect(Collectors.toSet());
    }

    private Stream<ClassInfo> collectClassesInPackage(IndexView index,
            String basePackage, boolean recursive) {
        Predicate<ClassInfo> predicate = recursive
                ? classInfo -> classInfo.name().packagePrefix() != null
                        && classInfo.name().packagePrefix()
                                .startsWith(basePackage)
                : classInfo -> classInfo.name().packagePrefix() != null
                        && classInfo.name().packagePrefix().equals(basePackage);
        return index.getKnownClasses().stream().filter(predicate);
    }

    /**
     * A common pattern in Flow components is to handle translations in classes
     * with a name ending in I18n and their potential inner classes, which are
     * serialized as JSON and sent to the client. An exception is the Upload
     * component, whose translations class has a capitalized N (UploadI18N).
     * <p>
     * Not private for testing purposes only.
     *
     * @param className
     *            the fully qualified class name to test
     * @return whether the name is that of a translations class or of one of its
     *         inner classes
     */
    static boolean isI18nClassName(String className) {
        // matches() anchors the whole name, so what used to be an alternation
        // of anchors inside the group is simply an optional group: the name
        // either ends at I18n/I18N or continues into an inner class.
        return className.matches(".*I18[nN](\\$.*)?");
    }

    // These should really go into the separate components but are here for now
    // to ease testing
    private Set<ClassInfo> getCommonComponentClasses(IndexView index) {
        Set<ClassInfo> classes = new HashSet<>();
        Stream.of("com.vaadin.flow.component.messages.MessageListItem",
                UI.class.getName()).map(index::getClassByName)
                .filter(Objects::nonNull).forEach(classes::add);
        LookupInitializer.getDefaultImplementations().stream()
                .map(Class::getName).map(index::getClassByName)
                .filter(Objects::nonNull).forEach(classes::add);

        Predicate<String> i18nClasses = VaadinQuarkusNativeProcessor::isI18nClassName;
        // Charts and Map configurations are serialized as JSON to be sent to
        // the client. All configuration classes need to be registered for
        // reflection.
        Predicate<String> componentsFilter = i18nClasses
                .or(className -> className
                        .startsWith("com.vaadin.flow.component.charts.model.")
                        || className.startsWith(
                                "com.vaadin.flow.component.map.configuration."));
        classes.addAll(collectClassesInPackage(index,
                "com.vaadin.flow.component", true)
                .filter(classInfo -> componentsFilter
                        .test(classInfo.name().toString()))
                .collect(Collectors.toSet()));
        return classes;
    }

    private static void registerAtmosphereClasses(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                AsyncSupportListenerAdapter.class, AtmosphereFramework.class,
                DefaultAnnotationProcessor.class,
                DefaultAtmosphereResourceFactory.class,
                SimpleHttpProtocol.class,
                AtmosphereResourceLifecycleInterceptor.class,
                TrackMessageSizeInterceptor.class,
                SuspendTrackerInterceptor.class,
                DefaultBroadcasterFactory.class, SimpleBroadcaster.class,
                DefaultBroadcaster.class, UUIDBroadcasterCache.class,
                VoidAnnotationProcessor.class,
                DefaultAtmosphereResourceSessionFactory.class,
                JSR356AsyncSupport.class, DefaultMetaBroadcaster.class,
                AtmosphereHandlerService.class, AbstractBroadcasterProxy.class,
                AsyncSupportListener.class, AtmosphereFrameworkListener.class,
                ExcludeSessionBroadcaster.class,
                AtmosphereResourceEventListener.class,
                AtmosphereInterceptor.class, BroadcastFilter.class,
                AtmosphereResource.class, AtmosphereResourceImpl.class,
                ManagedServiceInterceptor.class).constructors().methods()
                .build());

        reflectiveClass.produce(ReflectiveClassBuildItem
                .builder(AtmosphereFramework.DEFAULT_ATMOSPHERE_INTERCEPTORS
                        .toArray(Class[]::new))
                .constructors().methods().build());

        reflectiveClass.produce(ReflectiveClassBuildItem
                .builder(DelayedSchedulerExecutorsFactory.class).constructors()
                .build());
    }

    public static class IsNativeBuild implements BooleanSupplier {
        @Inject
        NativeConfig nativeConfig;

        @Override
        public boolean getAsBoolean() {
            return nativeConfig.enabled();
        }
    }

}
