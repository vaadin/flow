package com.vaadin.ui;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.annotations.JavaScriptModule;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.StateNode;

public class JS {

    public static <T> T get(Class<T> javascriptInterface, Component scope) {
        return get(javascriptInterface, scope.getElement());
    }

    public static <T> T get(Class<T> javascriptInterface, Element element) {

        for (Method m : javascriptInterface.getDeclaredMethods()) {
            if (m.getReturnType() != Void.TYPE) {
                throw new IllegalArgumentException("The method " + m.getName()
                        + " in " + javascriptInterface.getName() + " returns "
                        + m.getReturnType().getName()
                        + " but only void methods are supported");
            }
        }
        if (!javascriptInterface.isInterface()) {
            throw new IllegalArgumentException("The given javascript class "
                    + javascriptInterface.getName() + " must be an interface");
        }
        // TODO Cache

        return javascriptInterface.cast(Proxy.newProxyInstance(
                javascriptInterface.getClassLoader(),
                new Class[] { javascriptInterface }, new InvocationHandler() {

                    @Override
                    public Object invoke(Object self, Method interfaceMethod,
                            Object[] parameters) throws Throwable {
                        if (interfaceMethod
                                .getDeclaringClass() != javascriptInterface) {
                            return interfaceMethod.invoke(self, parameters);
                        }

                        StateNode node = element.getNode();
                        node.runAttached(() -> {
                            String namespace = JSPublisher.ensurePublished(
                                    node.getRoot(), javascriptInterface);

                            // Create namespace.method($0,$1,$2....,$n);
                            StringBuilder b = new StringBuilder();
                            b.append(namespace).append(".")
                                    .append(interfaceMethod.getName())
                                    .append("(");
                            for (int i = 0; i < parameters.length; i++) {
                                if (i != 0) {
                                    b.append(",");
                                }
                                b.append("$").append(i);
                            }
                            b.append(");");

                            node.enqueueRpc(b.toString(), parameters);
                        });
                        return null;
                    }
                }));

    }

    public static void main(String[] args) {
        int pl = 10;
        System.out.println(IntStream.rangeClosed(0, pl - 1)
                .mapToObj(i -> ("$" + i)).collect(Collectors.joining(",")));

    }

    public static String getResource(Class<?> javascriptInterface) {
        JavaScriptModule jsModule = javascriptInterface
                .getAnnotation(JavaScriptModule.class);
        if (jsModule == null) {
            throw new IllegalArgumentException(
                    "Javascript interface " + javascriptInterface.getName()
                            + " must be annotated with @"
                            + JavaScriptModule.class.getSimpleName());
        }
        String jsResource = jsModule.value();
        if (jsResource.equals("")) {
            jsResource = javascriptInterface.getClass().getSimpleName() + ".js";
        }
        return jsResource;
    }

}
