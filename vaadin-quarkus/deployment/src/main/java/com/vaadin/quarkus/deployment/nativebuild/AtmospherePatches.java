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
package com.vaadin.quarkus.deployment.nativebuild;

import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletConnection;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.inject.InjectableObjectFactory;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.vaadin.flow.server.communication.PushRequestHandler;
import com.vaadin.quarkus.graal.AtmosphereDeferredInitializer;
import com.vaadin.quarkus.graal.AtmosphereServletConfig;

/**
 * Byte-code patches required to make Atmosphere work in a native image.
 */
public class AtmospherePatches {

    static final DotName EXECUTORS_FACTORY = DotName
            .createSimple("org.atmosphere.util.ExecutorsFactory");
    static final DotName DELAYED_EXECUTORS_FACTORY = DotName.createSimple(
            "com.vaadin.quarkus.graal.DelayedSchedulerExecutorsFactory");

    private final IndexView index;

    public AtmospherePatches(IndexView index) {
        this.index = index;
    }

    public void apply(BuildProducer<BytecodeTransformerBuildItem> producer) {
        producer.produce(
                new BytecodeTransformerBuildItem(EXECUTORS_FACTORY.toString(),
                        patchAtmosphereExecutorsFactory()));
        producer.produce(new BytecodeTransformerBuildItem(
                DELAYED_EXECUTORS_FACTORY.toString(),
                patchDelayedSchedulerExecutorsFactory()));
        producer.produce(new BytecodeTransformerBuildItem(
                AtmosphereFramework.class.getName(),
                patchAtmosphereFramework()));
        producer.produce(new BytecodeTransformerBuildItem(
                InjectableObjectFactory.class.getName(),
                patchInjectableObjectFactory()));

        // Servlet 6 patches
        producer.produce(new BytecodeTransformerBuildItem(
                "org.atmosphere.cpr.AtmosphereRequest",
                patchHttpServletRequest()));
        producer.produce(new BytecodeTransformerBuildItem(
                "org.atmosphere.cpr.AtmosphereRequestImpl",
                patchHttpServletRequest()));
        producer.produce(new BytecodeTransformerBuildItem(
                "org.atmosphere.cpr.AtmosphereRequestImpl$NoOpsRequest",
                patchHttpServletRequest()));
        producer.produce(new BytecodeTransformerBuildItem(
                "org.atmosphere.cpr.AtmosphereResponse",
                patchHttpServletResponse()));
        producer.produce(new BytecodeTransformerBuildItem(
                "org.atmosphere.cpr.AtmosphereResponseImpl",
                patchHttpServletResponse()));
        producer.produce(new BytecodeTransformerBuildItem(
                "org.atmosphere.util.FakeHttpSession", patchHttpSession()));

        producer.produce(new BytecodeTransformerBuildItem(
                PushRequestHandler.class.getName(), patchPushRequestHandler()));
    }

    /**
     * Patches Atmosphere {@code ExecutorsFactory} class to make sure the
     * {@code getScheduler} method returns a {@code DelayedSchedulerExecutor}
     * that is lazily initialized at RUNTIME_STATIC phase. The original method
     * is renamed to {@code getScheduler_original} to be later called by
     * {@code DelayedSchedulerExecutor}.
     *
     * <pre>
     * {@code
     *      public static ScheduledExecutorService getScheduler_original(final AtmosphereConfig config) { ... }
     *
     *      public static ScheduledExecutorService getScheduler(final AtmosphereConfig config) {
     *          return DelayedSchedulerExecutorsFactory.getScheduler(config);
     *      }
     * }
     * </pre>
     */
    private BiFunction<String, ClassVisitor, ClassVisitor> patchAtmosphereExecutorsFactory() {
        return (className, classVisitor) -> {
            ClassInfo classInfo = index.getClassByName(EXECUTORS_FACTORY);
            ClassTransformer transformer = new ClassTransformer(
                    EXECUTORS_FACTORY.toString());

            MethodInfo getSchedulerMethod = classInfo.method("getScheduler",
                    Type.create(DotName.createSimple(AtmosphereConfig.class),
                            Type.Kind.CLASS));
            transformer.modifyMethod(MethodDescriptor.of(getSchedulerMethod))
                    .rename("getScheduler_original");

            try (MethodCreator creator = transformer
                    .addMethod(MethodDescriptor.of(getSchedulerMethod))) {

                creator.setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC);

                creator.returnValue(creator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(
                                DELAYED_EXECUTORS_FACTORY.toString(),
                                "getScheduler", ScheduledExecutorService.class,
                                AtmosphereConfig.class),
                        creator.getMethodParam(0)));
            }
            return transformer.applyTo(classVisitor);
        };
    }

    /**
     * Patches {@code DelayedSchedulerExecutorsFactory} to replace the
     * {@code newScheduler} method body to call to make a call to the original
     * {@code ExecutorsFactory.getScheduler}.
     *
     * <pre>
     * {@code
     * private static ScheduledExecutorService newScheduler(
     *         AtmosphereConfig config) {
     *     return ExecutorsFactory.getScheduler_original(config);
     * }
     * }
     * </pre>
     */
    private BiFunction<String, ClassVisitor, ClassVisitor> patchDelayedSchedulerExecutorsFactory() {
        return (className, classVisitor) -> {
            ClassInfo classInfo = index
                    .getClassByName(DELAYED_EXECUTORS_FACTORY);
            ClassTransformer transformer = new ClassTransformer(
                    EXECUTORS_FACTORY.toString());

            MethodInfo newSchedulerMethod = classInfo.method("newScheduler",
                    Type.create(DotName.createSimple(AtmosphereConfig.class),
                            Type.Kind.CLASS));

            transformer.removeMethod(MethodDescriptor.of(newSchedulerMethod));

            try (MethodCreator creator = transformer
                    .addMethod(MethodDescriptor.of(newSchedulerMethod))) {

                creator.setModifiers(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE);

                creator.returnValue(creator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(EXECUTORS_FACTORY.toString(),
                                "getScheduler_original",
                                ScheduledExecutorService.class,
                                AtmosphereConfig.class),
                        creator.getMethodParam(0)));
            }
            return transformer.applyTo(classVisitor);
        };
    }

    /**
     * Patches {@code AtmosphereFramework} to replace the {@code info()} method
     * with a no-op implementation, to prevent executors to be initialized
     * during the STATIC_INIT phase.
     */
    private BiFunction<String, ClassVisitor, ClassVisitor> patchAtmosphereFramework() {
        return (className, classVisitor) -> {
            ClassInfo classInfo = index.getClassByName(className);
            ClassTransformer transformer = new ClassTransformer(className);

            MethodInfo info = classInfo.method("info");
            transformer.removeMethod(MethodDescriptor.of(info));

            try (MethodCreator creator = transformer
                    .addMethod(MethodDescriptor.of(info))) {
                creator.setModifiers(Opcodes.ACC_PRIVATE);
                creator.returnValue(null);
            }
            return transformer.applyTo(classVisitor);
        };
    }

    /**
     * Patches Flow {@code PushRequestHandler} to replace the
     * {@code initAtmosphere} method with a new version that calls the original
     * method with a {@code AtmosphereServletConfig} wrapper and then registers
     * the AtmosphereFramework instance for deferred initialization.
     *
     * <pre>
     * {@code
     *      static AtmosphereFramework initAtmosphere_original(ServletConfig vaadinServletConfig) { ... }
     *
     *      static AtmosphereFramework initAtmosphere(ServletConfig vaadinServletConfig) {
     *         var framework = initAtmosphere_original(new AtmosphereServletConfig(vaadinServletConfig));
     *         AtmosphereDeferredInitializer.register(vaadinServletConfig, framework);
     *         return framework;
     *      }
     * }
     * </pre>
     *
     */
    private BiFunction<String, ClassVisitor, ClassVisitor> patchPushRequestHandler() {
        return (className, classVisitor) -> {
            ClassInfo classInfo = index.getClassByName(className);
            ClassTransformer transformer = new ClassTransformer(className);

            MethodInfo initAtmosphereMethod = classInfo.method("initAtmosphere",
                    Type.create(ServletConfig.class));
            transformer.modifyMethod(MethodDescriptor.of(initAtmosphereMethod))
                    .rename("initAtmosphere_original");

            try (MethodCreator creator = transformer
                    .addMethod(MethodDescriptor.of(initAtmosphereMethod))) {

                creator.setModifiers(Opcodes.ACC_STATIC);
                AssignableResultHandle configWrapper = creator
                        .createVariable(AtmosphereServletConfig.class);
                creator.assign(configWrapper, creator.newInstance(
                        MethodDescriptor.ofMethod(AtmosphereServletConfig.class,
                                "<init>", void.class, ServletConfig.class),
                        creator.getMethodParam(0)));

                AssignableResultHandle framework = creator
                        .createVariable(AtmosphereFramework.class);
                creator.assign(framework, creator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(
                                initAtmosphereMethod.declaringClass().name()
                                        .toString(),
                                "initAtmosphere_original",
                                AtmosphereFramework.class, ServletConfig.class),
                        configWrapper));
                creator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(
                                AtmosphereDeferredInitializer.class, "register",
                                void.class, ServletConfig.class,
                                AtmosphereFramework.class),
                        creator.getMethodParam(0), framework);
                creator.returnValue(framework);
            }
            return transformer.applyTo(classVisitor);
        };
    }

    /**
     * Patches Atmosphere {@code InjectableObjectFactory} to remove the
     * {@code injectableServiceLoader} field and its initialization that is
     * causing issue during STATIC_INIT phase in native build, and replaces
     * field usages with a direct calls to
     * {@code ServiceLoader.load(Injectable.class)}.
     */
    private BiFunction<String, ClassVisitor, ClassVisitor> patchInjectableObjectFactory() {
        return (className, classVisitor) -> {
            ClassTransformer transformer = new ClassTransformer(className);
            transformer.removeField("injectableServiceLoader",
                    ServiceLoader.class);
            return new ClassVisitor(Gizmo.ASM_API_VERSION,
                    transformer.applyTo(classVisitor)) {

                @Override
                public MethodVisitor visitMethod(int access, String name,
                        String descriptor, String signature,
                        String[] exceptions) {
                    // Only transform the configure method
                    MethodVisitor mv = super.visitMethod(access, name,
                            descriptor, signature, exceptions);
                    if ("<init>".equals(name) || "configure".equals(name)) {
                        return new InjectableObjectFactoryFieldUsageRemovalMethodVisitor(
                                mv, access, name, descriptor, signature,
                                exceptions);
                    }
                    return mv;
                }
            };
        };
    }

    // Servlet 6 API
    private BiFunction<String, ClassVisitor, ClassVisitor> patchHttpServletRequest() {
        return (className, classVisitor) -> {
            ClassInfo classInfo = index.getClassByName(className);
            ClassTransformer transformer = new ClassTransformer(className);

            // Delete methods removed in Servlet 6 API
            removeMethod(transformer, classInfo, "getRealPath", String.class,
                    String.class);
            removeMethod(transformer, classInfo, "isRequestedSessionIdFromUrl",
                    boolean.class);

            // Implement missing method added in Servlet 6 API
            // Only for AtmosphereRequestImpl$NoOpsRequest, that does not extend
            // HttpServletRequestWrapper
            if (classInfo.method("getRequestId") == null) {
                try (MethodCreator creator = transformer
                        .addMethod("getRequestId", String.class)) {
                    creator.returnNull();
                }
            }
            if (classInfo.method("getProtocolRequestId") == null) {
                try (MethodCreator creator = transformer
                        .addMethod("getProtocolRequestId", String.class)) {
                    creator.returnNull();
                }
            }
            if (classInfo.method("getServletConnection") == null) {
                try (MethodCreator creator = transformer.addMethod(
                        "getServletConnection", ServletConnection.class)) {
                    creator.returnNull();
                }
            }
            return transformer.applyTo(classVisitor);
        };
    }

    private BiFunction<String, ClassVisitor, ClassVisitor> patchHttpServletResponse() {
        return (className, classVisitor) -> {
            ClassInfo classInfo = index.getClassByName(className);
            ClassTransformer transformer = new ClassTransformer(className);

            // Delete methods removed in Servlet 6 API
            removeMethod(transformer, classInfo, "encodeUrl", String.class,
                    String.class);
            removeMethod(transformer, classInfo, "encodeRedirectUrl",
                    String.class, String.class);
            removeMethod(transformer, classInfo, "setStatus", void.class,
                    int.class, String.class);

            if ("org.atmosphere.cpr.AtmosphereResponseImpl".equals(className)) {
                // Redefine setStatus(int,String) to redirect to setStatus(int)
                try (MethodCreator creator = transformer.addMethod("setStatus",
                        void.class, int.class, String.class)) {
                    creator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(className, "setStatus",
                                    "V", "I"),
                            creator.getThis(), creator.getMethodParam(0));
                    creator.returnValue(null);
                }
            }
            return transformer.applyTo(classVisitor);
        };
    }

    private static void removeMethod(ClassTransformer transformer,
            ClassInfo classInfo, String methodName, Class<?> returnType,
            Class<?>... parameterTypes) {
        if (classInfo.method(methodName, Arrays.stream(parameterTypes)
                .map(Type::create).toList()) != null) {
            transformer.removeMethod(methodName, returnType,
                    (Object[]) parameterTypes);
        }
    }

    private BiFunction<String, ClassVisitor, ClassVisitor> patchHttpSession() {
        return (className, classVisitor) -> {
            ClassInfo classInfo = index.getClassByName(className);
            ClassTransformer transformer = new ClassTransformer(className);

            // Delete methods removed in Servlet 6 API
            if (classInfo.method("getSessionContext") != null) {
                transformer.removeMethod("getSessionContext",
                        "jakarta.servlet.http.HttpSessionContext");
            }
            removeMethod(transformer, classInfo, "getValue", Object.class,
                    String.class);
            removeMethod(transformer, classInfo, "getValueNames",
                    String[].class);
            removeMethod(transformer, classInfo, "putValue", void.class,
                    String.class, Object.class);
            removeMethod(transformer, classInfo, "removeValue", void.class,
                    String.class);
            return transformer.applyTo(classVisitor);
        };
    }
}
