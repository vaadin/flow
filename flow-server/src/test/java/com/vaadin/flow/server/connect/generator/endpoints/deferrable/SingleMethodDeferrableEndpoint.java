package com.vaadin.flow.server.connect.generator.endpoints.deferrable;

import com.vaadin.flow.server.connect.Deferrable;
import com.vaadin.flow.server.connect.Endpoint;

@Endpoint
public class SingleMethodDeferrableEndpoint {

    @Deferrable
    public String hello(String userName) {
        return "Hello, "+userName;
    }

    public String hi(String userName) {
        return "Hi, "+userName;
    }

}
