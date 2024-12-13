package com.vaadin.flow.plugin.maven;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;


public abstract class ReflectorIsolatedClassLoader extends URLClassLoader {
    protected ReflectorIsolatedClassLoader(final URL[] urls, final ClassLoader parent) {
        super(urls, parent);
    }

    protected ReflectorIsolatedClassLoader(final URL[] urls) {
        super(urls);
    }

    protected ReflectorIsolatedClassLoader(
            final URL[] urls,
            final ClassLoader parent,
            final URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    protected ReflectorIsolatedClassLoader(final String name, final URL[] urls, final ClassLoader parent) {
        super(name, urls, parent);
    }

    protected ReflectorIsolatedClassLoader(
            final String name,
            final URL[] urls,
            final ClassLoader parent,
            final URLStreamHandlerFactory factory) {
        super(name, urls, parent, factory);
    }

    public abstract URL[] urlsToScan();
}
