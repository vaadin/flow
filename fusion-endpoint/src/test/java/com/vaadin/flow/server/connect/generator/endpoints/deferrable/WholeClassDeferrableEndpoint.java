package com.vaadin.flow.server.connect.generator.endpoints.deferrable;

import com.vaadin.flow.server.connect.Deferrable;
import com.vaadin.flow.server.connect.Endpoint;

@Endpoint
@Deferrable
public class WholeClassDeferrableEndpoint {
    
    public String hello(String userName) {
        return "Hello, "+userName;
    }

    public String hi(String userName) {
        return "Hi, "+userName;
    }
}
