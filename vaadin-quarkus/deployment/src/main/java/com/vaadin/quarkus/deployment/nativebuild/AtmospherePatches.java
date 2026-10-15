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
package com.vaadin.quarkus.deployment.nativebuild;

import jakarta.servlet.ServletConnection;

import java.util.Arrays;
import java.util.function.BiFunction;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;

/**
 * Byte-code patches required to make Atmosphere work in a native image.
 * <p>
 * Atmosphere is compiled against the Servlet 5 API, so its implementations of
 * the servlet API still declare methods that Servlet 6 removed, some of them
 * with types it no longer has, and lack the ones it added. The patches align
 * them with Servlet 6.
 */
public class AtmospherePatches {

    private final IndexView index;

    public AtmospherePatches(IndexView index) {
        this.index = index;
    }

    public void apply(BuildProducer<BytecodeTransformerBuildItem> producer) {
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
