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

import jakarta.servlet.annotation.WebServlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem;
import io.quarkus.arc.deployment.CustomScopeBuildItem;
import io.quarkus.arc.deployment.IgnoreSplitPackageBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.IsProduction;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.QuarkusBuildCloseablesBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.quarkus.undertow.deployment.ServletDeploymentManagerBuildItem;
import io.quarkus.vertx.http.deployment.DefaultRouteBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.quarkus.websockets.client.deployment.ServerWebSocketContainerBuildItem;
import io.quarkus.websockets.client.deployment.WebSocketDeploymentInfoBuildItem;
import io.quarkus.websockets.client.deployment.WebsocketConfig;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.quarkus.QuarkusVaadinServlet;
import com.vaadin.quarkus.WebsocketHttpSessionAttachRecorder;
import com.vaadin.quarkus.annotation.NormalRouteScoped;
import com.vaadin.quarkus.annotation.NormalUIScoped;
import com.vaadin.quarkus.annotation.RouteScoped;
import com.vaadin.quarkus.annotation.UIScoped;
import com.vaadin.quarkus.annotation.VaadinServiceEnabled;
import com.vaadin.quarkus.annotation.VaadinServiceScoped;
import com.vaadin.quarkus.annotation.VaadinSessionScoped;
import com.vaadin.quarkus.context.RouteContextWrapper;
import com.vaadin.quarkus.context.RouteScopedContext;
import com.vaadin.quarkus.context.UIContextWrapper;
import com.vaadin.quarkus.context.UIScopedContext;
import com.vaadin.quarkus.context.VaadinServiceScopedContext;
import com.vaadin.quarkus.context.VaadinSessionScopedContext;
import com.vaadin.quarkus.deployment.vaadinplugin.VaadinBuildTimeConfig;
import com.vaadin.quarkus.deployment.vaadinplugin.VaadinPlugin;
import com.vaadin.quarkus.push.PushDispatchRecorder;

class VaadinQuarkusProcessor {

    private static final Logger LOG = LoggerFactory
            .getLogger(VaadinQuarkusProcessor.class);

    private static final String FEATURE = "vaadin-quarkus";

    private static final DotName ROUTE_ANNOTATION = DotName
            .createSimple(Route.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void indexOptionalVaadinDependencies(
            BuildProducer<IndexDependencyBuildItem> producer) {
        // Development dependencies built outside Flow, which ship no Jandex
        // index of their own. The Flow modules carry one.
        producer.produce(new IndexDependencyBuildItem("com.vaadin", "copilot"));
        producer.produce(
                new IndexDependencyBuildItem("com.vaadin", "ui-tests"));
    }

    /*
     * Removes vaadin-core-jandex artifact, if vaadin-jandex is also present
     */
    @BuildStep
    void removeUnusedJandexIndex(CurateOutcomeBuildItem curateOutcome,
            BuildProducer<RemovedResourceBuildItem> removedResourceProducer,
            BuildProducer<IgnoreSplitPackageBuildItem> ignoreSplitPackage) {
        Predicate<String> isVaadinJandex = Pattern
                .compile("vaadin(-core)?-jandex").asMatchPredicate();
        ApplicationModel applicationModel = curateOutcome.getApplicationModel();
        Set<String> vaadinIndexes = applicationModel.getDependencies().stream()
                .filter(archive -> "com.vaadin"
                        .equals(archive.getKey().getGroupId())
                        && isVaadinJandex
                                .test(archive.getKey().getArtifactId()))
                .map(archive -> archive.getKey().toGacString())
                .collect(Collectors.toSet());
        if (vaadinIndexes.size() > 1) {
            ArtifactKey artifactKey = ArtifactKey.of("com.vaadin",
                    "vaadin-core-jandex", null, "jar");
            // To prevent the vaadin-core-index to be indexed, it should add to
            // the removed resources, but producing a RemovedResourceBuildItem
            // does not prevent the split package processor to log all classes
            // present in both vaadin-jandex and vaadin-core-jandex The
            // removedResources map in ApplicationModel is computed before the
            // SplitPackageProcessor and it is mutable, but the javadocs don't
            // specify if updating it is allowed or not. To prevent issues in
            // the future, try to put the artifact in the collection, but
            // fallback to producing a RemovedResourceBuildItem and a
            // IgnoreSplitPackageBuildItem to prevent the verbose and useless
            // log.
            try {
                applicationModel.getRemovedResources().put(artifactKey,
                        Set.of());
            } catch (Exception ex) {
                removedResourceProducer.produce(
                        new RemovedResourceBuildItem(artifactKey, Set.of()));
                ignoreSplitPackage.produce(new IgnoreSplitPackageBuildItem(
                        Set.of("com.vaadin.*")));
            }
        }
    }

    @BuildStep
    public void build(
            final BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            final BuildProducer<BeanDefiningAnnotationBuildItem> additionalBeanDefiningAnnotationRegistry) {
        additionalBeanProducer.produce(AdditionalBeanBuildItem
                .unremovableOf(QuarkusVaadinServlet.class));

        // Make and Route annotated Component a bean for injection
        additionalBeanDefiningAnnotationRegistry
                .produce(new BeanDefiningAnnotationBuildItem(ROUTE_ANNOTATION));
    }

    @BuildStep
    public void ensureSingleVaadinExecutor(
            ValidationPhaseBuildItem validationPhaseBuildItem,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors) {

        DotName executorName = DotName
                .createSimple("java.util.concurrent.Executor");
        DotName vaadinServiceEnabledName = DotName
                .createSimple(VaadinServiceEnabled.class);

        // Look for Executor beans with @VaadinServiceEnabled annotation
        List<BeanInfo> candidates = validationPhaseBuildItem.getContext()
                .beans()
                .filter(info -> info.hasType(executorName) && info
                        .getQualifier(vaadinServiceEnabledName).isPresent())
                .stream().toList();

        if (candidates.size() > 1) {
            // Multiple beans found, throw an exception
            String candidatesInfo = candidates.stream().map(BeanInfo::toString)
                    .collect(Collectors.joining(", "));

            validationErrors.produce(
                    new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                            new IllegalStateException(
                                    "There must be at most one Executor bean annotated with @"
                                            + VaadinServiceEnabled.class
                                                    .getSimpleName()
                                            + " in the application. " + "Found "
                                            + candidates.size() + ": "
                                            + candidates)));
        }
    }

    @BuildStep
    void markVaadinServiceEnabledBeanUnremovable(
            BuildProducer<UnremovableBeanBuildItem> producer) {
        producer.produce(UnremovableBeanBuildItem.targetWithAnnotation(
                DotName.createSimple(VaadinServiceEnabled.class)));
    }

    @BuildStep
    public void specifyRouterLayoutBeans(CombinedIndexBuildItem item,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer) {
        Collection<ClassInfo> layouts = item.getComputingIndex()
                .getAllKnownImplementations(
                        DotName.createSimple(RouterLayout.class.getName()));
        for (ClassInfo layoutInfo : layouts) {
            additionalBeanProducer.produce(AdditionalBeanBuildItem
                    .unremovableOf(layoutInfo.name().toString()));
        }
    }

    @BuildStep
    public void specifyErrorViewsBeans(CombinedIndexBuildItem item,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer) {
        Collection<ClassInfo> errors = item.getComputingIndex()
                .getAllKnownImplementations(DotName
                        .createSimple(HasErrorParameter.class.getName()));
        for (ClassInfo errorInfo : errors) {
            additionalBeanProducer.produce(AdditionalBeanBuildItem
                    .unremovableOf(errorInfo.name().toString()));
        }
    }

    @BuildStep
    void mapVaadinServletPaths(final BeanArchiveIndexBuildItem beanArchiveIndex,
            final BuildProducer<ServletBuildItem> servletProducer) {
        final IndexView indexView = beanArchiveIndex.getIndex();

        // Collect all VaadinServlet instances and remove QuarkusVaadinServlet
        // and VaadinServlet from the list.
        Collection<ClassInfo> vaadinServlets = indexView
                .getAllKnownSubclasses(
                        DotName.createSimple(VaadinServlet.class.getName()))
                .stream()
                .filter(servlet -> !servlet.name().toString()
                        .equals(QuarkusVaadinServlet.class.getName())
                        && !servlet.name().toString()
                                .equals(VaadinServlet.class.getName()))
                .collect(Collectors.toList());

        // Register VaadinServlet instances annotated with @WebServlet
        vaadinServlets = registerUserServlets(servletProducer, vaadinServlets);
        // If no annotated VaadinServlet instances is registered, register
        // QuarkusVaadinServlet
        if (vaadinServlets.isEmpty()) {
            servletProducer.produce(ServletBuildItem
                    .builder(QuarkusVaadinServlet.class.getName(),
                            QuarkusVaadinServlet.class.getName())
                    .addMapping("/*").setAsyncSupported(true)
                    .setLoadOnStartup(1).build());
        }
    }

    @BuildStep
    ContextConfiguratorBuildItem registerVaadinServiceScopedContext(
            ContextRegistrationPhaseBuildItem phase) {
        return new ContextConfiguratorBuildItem(
                phase.getContext().configure(VaadinServiceScoped.class).normal()
                        .contextClass(VaadinServiceScopedContext.class));
    }

    @BuildStep
    CustomScopeBuildItem serviceScope() {
        return new CustomScopeBuildItem(VaadinServiceScoped.class);
    }

    @BuildStep
    ContextConfiguratorBuildItem registerVaadinSessionScopedContext(
            ContextRegistrationPhaseBuildItem phase) {
        return new ContextConfiguratorBuildItem(
                phase.getContext().configure(VaadinSessionScoped.class).normal()
                        .contextClass(VaadinSessionScopedContext.class));
    }

    @BuildStep
    CustomScopeBuildItem sessionScope() {
        return new CustomScopeBuildItem(VaadinSessionScoped.class);
    }

    @BuildStep
    ContextConfiguratorBuildItem registerUIScopedContext(
            ContextRegistrationPhaseBuildItem phase) {
        return new ContextConfiguratorBuildItem(
                phase.getContext().configure(NormalUIScoped.class).normal()
                        .contextClass(UIScopedContext.class));
    }

    @BuildStep
    ContextConfiguratorBuildItem registerPseudoUIScopedContext(
            ContextRegistrationPhaseBuildItem phase) {
        return new ContextConfiguratorBuildItem(
                phase.getContext().configure(UIScoped.class)
                        .contextClass(UIContextWrapper.class));
    }

    @BuildStep
    CustomScopeBuildItem normalUiScope() {
        return new CustomScopeBuildItem(NormalUIScoped.class);
    }

    @BuildStep
    CustomScopeBuildItem uiScope() {
        return new CustomScopeBuildItem(UIScoped.class);
    }

    @BuildStep
    ContextConfiguratorBuildItem registerRouteScopedContext(
            ContextRegistrationPhaseBuildItem phase) {
        return new ContextConfiguratorBuildItem(
                phase.getContext().configure(NormalRouteScoped.class).normal()
                        .contextClass(RouteScopedContext.class));
    }

    @BuildStep
    ContextConfiguratorBuildItem registerPseudoRouteScopedContext(
            ContextRegistrationPhaseBuildItem phase) {
        return new ContextConfiguratorBuildItem(
                phase.getContext().configure(RouteScoped.class)
                        .contextClass(RouteContextWrapper.class));
    }

    @BuildStep
    CustomScopeBuildItem normalRouteScope() {
        return new CustomScopeBuildItem(NormalRouteScoped.class);
    }

    @BuildStep
    CustomScopeBuildItem rRouteScope() {
        return new CustomScopeBuildItem(RouteScoped.class);
    }

    @BuildStep(onlyIf = IsProduction.class)
    void buildFrontendTask(CurateOutcomeBuildItem outcomeBuildItem,
            OutputTargetBuildItem outputTarget,
            ArchiveRootBuildItem archiveRoot,
            VaadinBuildTimeConfig vaadinConfig,
            QuarkusBuildCloseablesBuildItem closeablesBuildItem,

            // Declaring this parameter is what orders this build step before
            // the packaging one, which consumes GeneratedResourceBuildItem. It
            // is required even when no file ends up being emitted, otherwise
            // the application is packaged while the frontend build is still
            // running.
            BuildProducer<GeneratedResourceBuildItem> producer)
            throws BuildException {
        if (vaadinConfig.enabled()) {
            VaadinPlugin vaadinPlugin = VaadinPlugin.of(vaadinConfig,
                    outcomeBuildItem.getApplicationModel(),
                    outputTarget.getOutputDirectory());
            vaadinPlugin.prepareFrontend();

            // Allows the build to register additional files to be packaged into
            // the application
            BiConsumer<String, byte[]> emitter = vaadinPlugin
                    .createGeneratedResourceEmitter(
                            archiveRoot.getRootDirectories(), producer);
            vaadinPlugin.buildFrontend(emitter);

            // Register a task to clean the generated files
            closeablesBuildItem.add(vaadinPlugin::clean);
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupPush(ServletDeploymentManagerBuildItem deployment,
            WebSocketDeploymentInfoBuildItem webSocketDeploymentInfoBuildItem,
            ServerWebSocketContainerBuildItem serverWebSocketContainerBuildItem,
            BuildProducer<FilterBuildItem> filterProd,
            WebsocketHttpSessionAttachRecorder recorder) {

        filterProd.produce(new FilterBuildItem(recorder.createWebSocketHandler(
                webSocketDeploymentInfoBuildItem.getInfo(),
                serverWebSocketContainerBuildItem.getContainer(),
                deployment.getDeploymentManager()), 120));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Produce(DefaultRouteBuildItem.class)
    void announcePushDispatch(WebsocketConfig websocketConfig,
            PushDispatchRecorder recorder) {
        if (websocketConfig.dispatchToWorker()) {
            recorder.logDispatchOnWorker();
        }
    }

    private Collection<ClassInfo> registerUserServlets(
            BuildProducer<ServletBuildItem> servletProducer,
            Collection<ClassInfo> vaadinServlets) {
        Collection<ClassInfo> registeredServlets = new ArrayList<>(
                vaadinServlets);
        // TODO: check that we don't register 2 of the same mapping
        for (ClassInfo info : vaadinServlets) {
            final AnnotationInstance webServletInstance = info
                    .declaredAnnotation(
                            DotName.createSimple(WebServlet.class.getName()));
            if (webServletInstance == null) {
                LOG.warn(
                        "Found unexpected {} extends VaadinServlet without @WebServlet, skipping",
                        info.name());
                registeredServlets.remove(info);
                continue;
            }

            String servletName = Optional
                    .ofNullable(webServletInstance.value("name"))
                    .map(AnnotationValue::asString)
                    .orElse(info.name().toString());
            int loadOnStartup = Optional
                    .ofNullable(webServletInstance.value("loadOnStartup"))
                    .map(AnnotationValue::asInt).orElse(-1);
            final ServletBuildItem.Builder servletBuildItem = ServletBuildItem
                    .builder(servletName, info.name().toString());

            Stream.of(webServletInstance.value("value"),
                    webServletInstance.value("urlPatterns"))
                    .filter(Objects::nonNull)
                    .flatMap(value -> Stream.of(value.asStringArray()))
                    .forEach(servletBuildItem::addMapping);

            addWebInitParameters(webServletInstance, servletBuildItem);
            setAsyncSupportedIfDefined(webServletInstance, servletBuildItem);
            servletBuildItem
                    .setLoadOnStartup(loadOnStartup > 0 ? loadOnStartup : 1);
            if (loadOnStartup < 1) {
                LOG.warn(
                        "Vaadin Servlet needs to be eagerly loaded by setting load-on-startup to be greater than 0. "
                                + "Current value for '{}' is '{}', so it will be forced to '1'. "
                                + "Please set 'loadOnStartup' attribute on @WebServlet annotation to a value greater than 0.",
                        servletName, loadOnStartup);
            }
            servletProducer.produce(servletBuildItem.build());
        }
        return registeredServlets;
    }

    private void addWebInitParameters(AnnotationInstance webServletInstance,
            ServletBuildItem.Builder servletBuildItem) {
        // Add WebInitParam parameters to registration
        AnnotationValue initParams = webServletInstance.value("initParams");
        if (initParams != null) {
            for (AnnotationInstance initParam : initParams.asNestedArray()) {
                servletBuildItem.addInitParam(
                        initParam.value("name").asString(),
                        initParam.value().asString());
            }
        }
    }

    private void setAsyncSupportedIfDefined(
            AnnotationInstance webServletInstance,
            ServletBuildItem.Builder servletBuildItem) {
        final AnnotationValue asyncSupported = webServletInstance
                .value("asyncSupported");
        if (asyncSupported != null) {
            servletBuildItem.setAsyncSupported(asyncSupported.asBoolean());
        }
    }
}
