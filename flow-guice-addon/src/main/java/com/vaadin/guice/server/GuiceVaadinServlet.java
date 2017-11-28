package com.vaadin.guice.server;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.guice.annotation.Controller;
import com.vaadin.guice.annotation.Import;
import com.vaadin.guice.annotation.OverrideBindings;
import com.vaadin.guice.annotation.PackagesToScan;
import com.vaadin.server.*;
import com.vaadin.ui.UI;
import org.reflections.Reflections;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.concat;
import static com.google.inject.Guice.createInjector;
import static com.google.inject.util.Modules.override;
import static java.lang.reflect.Modifier.isAbstract;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

/**
 * Subclass of the standard {@link com.vaadin.server.VaadinServlet Vaadin servlet}
 *
 * @author Bernd Hopp (bernd@vaadin.com)
 */
@SuppressWarnings("unused")
public class GuiceVaadinServlet extends VaadinServlet implements SessionInitListener {

    private static final Class<? super Provider<Injector>> injectorProviderType = new TypeLiteral<Provider<Injector>>() {
    }.getRawType();
    private final Map<Class<? extends UI>, Set<Class<?>>> controllerCache = new HashMap<>();
    private UIScope uiScope;
    private Injector injector;
    private VaadinSessionScope vaadinSessionScoper;
    private Set<Class<?>> controllerClasses;
    private Set<Class<? extends SessionInitListener>> sessionInitListenerClasses;
    private Set<Class<? extends SessionDestroyListener>> sessionDestroyListenerClasses;
    private Set<Class<? extends ServiceDestroyListener>> serviceDestroyListeners;
    private Set<Class<? extends UI>> uiClasses;
    private Set<Class<? extends RequestHandler>> requestHandlerClasses;
    private Set<Class<? extends VaadinServiceInitListener>> vaadinServiceInitListenerClasses;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        final String initParameter = servletConfig.getInitParameter("packagesToScan");

        final String[] packagesToScan;

        final boolean annotationPresent = getClass().isAnnotationPresent(PackagesToScan.class);

        if (!isNullOrEmpty(initParameter)) {
            checkState(
                    !annotationPresent,
                    "%s has both @PackagesToScan-annotation and an 'packagesToScan'-initParam",
                    getClass()
            );
            packagesToScan = initParameter.split(",");
        } else if (annotationPresent) {
            packagesToScan = getClass().getAnnotation(PackagesToScan.class).value();
        } else {
            throw new IllegalStateException("no packagesToScan-initParameter found and no @PackagesToScan-annotation present, please configure the packages to be scanned");
        }

        Reflections reflections = new Reflections((Object[]) packagesToScan);

        final Set<Annotation> importAnnotations = stream(getClass().getAnnotations())
                .filter(annotation -> annotation.annotationType().isAnnotationPresent(Import.class))
                .collect(toSet());

        //import packages
        importAnnotations
                .stream()
                .map(annotation -> annotation.annotationType().getAnnotation(Import.class))
                .filter(i -> i.packagesToScan().length != 0)
                .forEach(i -> reflections.merge(new Reflections((Object[]) i.packagesToScan())));

        //import modules
        final Set<Module> modulesFromAnnotations = importAnnotations
                .stream()
                .map(annotation -> createModule(annotation.annotationType().getAnnotation(Import.class).value(), reflections, annotation))
                .collect(toSet());

        Set<Class<? extends Module>> modulesFromAnnotationClasses = modulesFromAnnotations
                .stream()
                .map(Module::getClass)
                .collect(toSet());

        final Set<Module> modulesFromPath = nonAbstractTypes(reflections.getSubTypesOf(Module.class))
                .stream()
                .filter(moduleClass -> !modulesFromAnnotationClasses.contains(moduleClass))
                .filter(moduleClass -> !VaadinModule.class.equals(moduleClass))
                .map(moduleClass -> createModule(moduleClass, reflections, null))
                .collect(toSet());

        Iterable<Module> allModules = concat(
                modulesFromAnnotations,
                modulesFromPath
        );

        List<Module> nonOverrideModules = new ArrayList<>();
        List<Module> overrideModules = new ArrayList<>();

        for (Module module : allModules) {
            if (module.getClass().isAnnotationPresent(OverrideBindings.class)) {
                overrideModules.add(module);
            } else {
                nonOverrideModules.add(module);
            }
        }

        /*
         * combine bindings from the static modules in {@link GuiceVaadinServletConfiguration#modules()} with those bindings
         * from dynamically loaded modules, see {@link RuntimeModule}.
         * This is done first so modules can install their own reflections.
        */
        Module combinedModules = override(nonOverrideModules).with(overrideModules);

        this.uiClasses = nonAbstractTypes(reflections.getSubTypesOf(UI.class));
        this.vaadinServiceInitListenerClasses = nonAbstractTypes(reflections.getSubTypesOf(VaadinServiceInitListener.class));
        this.requestHandlerClasses = nonAbstractTypes(reflections.getSubTypesOf(RequestHandler.class));
        this.controllerClasses = nonAbstractTypes(reflections.getTypesAnnotatedWith(Controller.class));

        this.controllerClasses.forEach(
            controllerClass -> checkArgument(
                !UI.class.isAssignableFrom(controllerClass.getAnnotation(Controller.class).value()),
                "UI's are not instantiated by guice and must therefore not be used as a value for @Controller"
            )
        );

        this.sessionInitListenerClasses = nonAbstractTypes(reflections.getSubTypesOf(SessionInitListener.class))
                .stream()
                .filter(cls -> !VaadinServlet.class.isAssignableFrom(cls))
                .collect(toSet());

        this.sessionDestroyListenerClasses = nonAbstractTypes(reflections.getSubTypesOf(SessionDestroyListener.class));
        this.serviceDestroyListeners = nonAbstractTypes(reflections.getSubTypesOf(ServiceDestroyListener.class));
        this.uiScope = new UIScope();
        this.vaadinSessionScoper = new VaadinSessionScope();

        VaadinModule vaadinModule = new VaadinModule(this);

        this.injector = createInjector(vaadinModule, combinedModules);

        super.init(servletConfig);
    }

    private <U> Set<Class<? extends U>> nonAbstractTypes(Set<Class<? extends U>> types) {
        return types
                .stream()
                .filter(t -> !isAbstract(t.getModifiers()))
                .collect(toSet());
    }

    @Override
    protected void servletInitialized() throws ServletException {
        final VaadinService vaadinService = VaadinService.getCurrent();

        vaadinService.addSessionInitListener(this);

        sessionInitListenerClasses
                .stream()
                .map(getInjector()::getInstance)
                .forEach(vaadinService::addSessionInitListener);

        sessionDestroyListenerClasses
                .stream()
                .map(getInjector()::getInstance)
                .forEach(vaadinService::addSessionDestroyListener);

        serviceDestroyListeners
                .stream()
                .map(getInjector()::getInstance)
                .forEach(vaadinService::addServiceDestroyListener);
    }

    @Override
    protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) throws ServiceException {
        return new GuiceVaadinServletService(this, deploymentConfiguration);
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        VaadinSession session = event.getSession();

        requestHandlerClasses
                .stream()
                .map(getInjector()::getInstance)
                .forEach(session::addRequestHandler);
    }


    UIScope getUiScope() {
        return uiScope;
    }

    Set<Class<? extends UI>> getUiClasses() {
        return uiClasses;
    }

    VaadinSessionScope getVaadinSessionScoper() {
        return vaadinSessionScoper;
    }

    Iterator<VaadinServiceInitListener> getServiceInitListeners() {
        return vaadinServiceInitListenerClasses
                .stream()
                .map(key -> (VaadinServiceInitListener) getInjector().getInstance(key))
                .iterator();
    }

    Set<Class<?>> getControllerClasses() {
        return controllerClasses;
    }

    private Module createModule(Class<? extends Module> moduleClass, Reflections reflections, Annotation annotation) {

        for (Constructor<?> constructor : moduleClass.getDeclaredConstructors()) {

            Object[] initArgs = new Object[constructor.getParameterCount()];

            Class<?>[] parameterTypes = constructor.getParameterTypes();

            boolean allParameterTypesResolved = true;

            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];

                if (Reflections.class.equals(parameterType)) {
                    initArgs[i] = reflections;
                } else if (injectorProviderType.equals(parameterType)) {
                    initArgs[i] = (Provider<Injector>) this::getInjector;
                } else if (annotation != null && annotation.annotationType().equals(parameterType)) {
                    initArgs[i] = annotation;
                } else {
                    allParameterTypesResolved = false;
                    break;
                }
            }

            if (!allParameterTypesResolved) {
                continue;
            }

            constructor.setAccessible(true);

            try {
                return (Module) constructor.newInstance(initArgs);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        throw new IllegalStateException("no suitable constructor found for %s" + moduleClass);
    }

    Injector getInjector() {
        return checkNotNull(injector, "injector is not set up yet");
    }
}
