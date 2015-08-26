package com.vaadin.ui;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.vaadin.hummingbird.kernel.RootNode;
import com.vaadin.hummingbird.kernel.StateNode;

public class JS {

    public static <T> T get(Class<T> javascriptInterface, Component scope) {

        for (Method m : javascriptInterface.getDeclaredMethods()) {

        }
        // TODO Validate interface methods (void return)
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

                        // FIXME Currently only works for attached components
                        StateNode node = scope.getElement().getNode();

                        RootNode root = node.getRoot();

                        String namespace = JSPublisher.ensurePublished(root,
                                javascriptInterface);
                        StringBuilder b = new StringBuilder();
                        b.append(namespace).append(".")
                                .append(interfaceMethod.getName()).append("(");

                        for (int i = 0; i < parameters.length; i++) {
                            if (i != 0) {
                                b.append(",");
                            }
                            b.append("$").append(i);
                        }
                        b.append(");");

                        root.enqueueRpc(node, b.toString(), parameters);

                        return null;
                    }
                }));
    }

}
