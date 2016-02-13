package com.vaadin.client;

import com.google.gwt.http.client.URL;
import com.vaadin.shared.VaadinUriResolver;

public class URIResolver extends VaadinUriResolver {
    private Registry registry;

    public URIResolver(Registry registry) {
        this.registry = registry;
    }

    @Override
    protected String getVaadinDirUrl() {
        return registry.getApplicationConfiguration().getVaadinDirUrl();
    }

    @Override
    protected String getServiceUrlParameterName() {
        return registry.getApplicationConfiguration()
                .getServiceUrlParameterName();
    }

    @Override
    protected String getServiceUrl() {
        return registry.getApplicationConfiguration().getServiceUrl();
    }

    @Override
    protected String encodeQueryStringParameterValue(String queryString) {
        return URL.encodeQueryString(queryString);
    }

}
