package com.vaadin.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.vaadin.annotations.JavaScriptModule;
import com.vaadin.hummingbird.kernel.RootNode;

public class JSPublisher {

    public static String ensurePublished(RootNode root,
            Class<?> javascriptInterface) {

        String namespace = javascriptInterface.getName().replace(".", "_");

        if (!isPublished(root, namespace)) {
            setPublished(root, namespace);
            JavaScriptModule jsModule = javascriptInterface
                    .getAnnotation(JavaScriptModule.class);
            if (jsModule == null) {
                throw new IllegalArgumentException(
                        "Javascript interface " + javascriptInterface.getName()
                                + " must be annotated with @"
                                + JavaScriptModule.class.getSimpleName());
            }
            String jsResource = jsModule.value();
            try (InputStream is = javascriptInterface
                    .getResourceAsStream(jsResource)) {
                if (is == null) {
                    throw new IllegalArgumentException("Javascript resource "
                            + jsResource + " not found using class loader");
                }

                String content = IOUtils.toString(is);
                root.enqueueRpc(root, "modules.publish($0,$1)", namespace,
                        content);
            } catch (IOException e) {
                throw new IllegalArgumentException("Javascript resource "
                        + jsResource + " could not be loaded", e);
            }

        }

        return "modules." + namespace;
    }

    private static void setPublished(RootNode root, String namespace) {
        List<Object> published = root.getMultiValued(JS.class);
        published.add(namespace);

    }

    private static boolean isPublished(RootNode root, String namespace) {
        // TODO Should be set and not list
        List<Object> published = root.getMultiValued(JS.class);
        return published.contains(namespace);
    }

}
