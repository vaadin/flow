package com.vaadin.flow.client.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import com.vaadin.flow.client.ClientResources;

@Component(immediate = true, service = ClientResources.class, scope = ServiceScope.SINGLETON)
public class OsgiClientResources implements ClientResources {

    @Override
    public InputStream getResource(String path) {
        Bundle bundle = FrameworkUtil.getBundle(OsgiClientResources.class);
        URL resource = bundle.getResource(path);
        try {
            return resource.openConnection().getInputStream();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

}