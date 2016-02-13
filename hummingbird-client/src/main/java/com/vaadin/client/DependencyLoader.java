package com.vaadin.client;

import com.google.gwt.core.client.JsArrayString;
import com.vaadin.client.ResourceLoader.ResourceLoadEvent;
import com.vaadin.client.ResourceLoader.ResourceLoadListener;
import com.vaadin.client.bootstrap.Bootstrapper;

public class DependencyLoader {

    private Registry registry;

    public DependencyLoader(Registry registry) {
        this.registry = registry;
    }

    public void loadStyleDependencies(JsArrayString dependencies) {
        // Assuming no reason to interpret in a defined order
        ResourceLoadListener resourceLoadListener = new ResourceLoadListener() {
            @Override
            public void onLoad(ResourceLoadEvent event) {
                Bootstrapper.endDependencyLoading();
            }

            @Override
            public void onError(ResourceLoadEvent event) {
                Console.error(event.getResourceUrl()
                        + " could not be loaded, or the load detection failed because the stylesheet is empty.");
                // The show must go on
                onLoad(event);
            }
        };
        ResourceLoader loader = ResourceLoader.get();
        for (int i = 0; i < dependencies.length(); i++) {
            String url = translateVaadinUri(dependencies.get(i));
            Bootstrapper.startDependencyLoading();
            loader.loadStylesheet(url, resourceLoadListener);
        }
    }

    public void loadScriptDependencies(final JsArrayString dependencies) {
        if (dependencies.length() == 0) {
            return;
        }

        // Listener that loads the next when one is completed
        ResourceLoadListener resourceLoadListener = new ResourceLoadListener() {
            @Override
            public void onLoad(ResourceLoadEvent event) {
                if (dependencies.length() != 0) {
                    String url = translateVaadinUri(dependencies.shift());
                    Bootstrapper.startDependencyLoading();
                    // Load next in chain (hopefully already preloaded)
                    event.getResourceLoader().loadScript(url, this);
                }
                // Call start for next before calling end for current
                Bootstrapper.endDependencyLoading();
            }

            @Override
            public void onError(ResourceLoadEvent event) {
                Console.error(event.getResourceUrl() + " could not be loaded.");
                // The show must go on
                onLoad(event);
            }
        };

        ResourceLoader loader = ResourceLoader.get();

        // Start chain by loading first
        String url = translateVaadinUri(dependencies.shift());
        Bootstrapper.startDependencyLoading();
        loader.loadScript(url, resourceLoadListener);

        for (int i = 0; i < dependencies.length(); i++) {
            String preloadUrl = translateVaadinUri(dependencies.get(i));
            loader.loadScript(preloadUrl, null);
        }
    }

    /**
     * Run the URI through all protocol translators.
     *
     * @param uri
     *            the URI to translate
     * @return the translated URI
     */
    private String translateVaadinUri(String uri) {
        return registry.getURIResolver().resolveVaadinUri(uri);
    }

}
