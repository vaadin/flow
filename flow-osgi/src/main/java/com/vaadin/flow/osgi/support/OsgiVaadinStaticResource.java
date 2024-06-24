/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.support;

/**
 * Used to declare a Vaadin Resource for use in OSGi. The resource is expected
 * to be in the same OSGi bundle as the class implementing this interface.
 * <p>
 * E.g. some static web resource could be inside classpath with path
 * "/META-INF/resources/VAADIN/static/{resourceName}" and if it should be
 * available by URI "/VAADIN/static/{resourceName}" then is the alias for the
 * path.
 * <p>
 * Another usecase is resources inside the <b>"frontend"</b> folder. You may
 * want to register static web resources available by the URI
 * "/frontend/mycomponent" (this is the alias) which are located inside a bundle
 * by the path "/META-INF/resources/frontend/mycomponent" (this path is a
 * standard path for the web resources in jar but it can be any path you want if
 * it's supposed to be used inside OSGi only).
 * <p>
 * To publish a resource, an implementation of this interface needs to be
 * registered as an OSGi service, which makes
 * <code>VaadinResourceTrackerComponent</code> automatically publish the
 * resource with the given name.
 *
 * @since 1.2
 */
public interface OsgiVaadinStaticResource {
    /**
     * Return the path where the resource is located inside the bundle.
     *
     * @return theme name, not null
     */
    String getPath();

    /**
     * Gets the name in the URI namespace at which the resources are registered.
     *
     * @return the URI alias
     */
    String getAlias();

    /**
     * Creates a new resource instance.
     *
     * @param path
     *            the resource path inside a bundle
     * @param alias
     *            the URI alias
     * @return a new resource instance
     */
    public static OsgiVaadinStaticResource create(String path, String alias) {
        return new OsgiVaadinStaticResource() {
            @Override
            public String getPath() {
                return path;
            }

            @Override
            public String getAlias() {
                return alias;
            }
        };
    }
}
