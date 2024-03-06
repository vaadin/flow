/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import dev.hilla.Endpoint;

/**
 * A test class.
 */
@Endpoint
public class MyEndpoint {

    /**
     * Foo endpoint.
     * 
     * @param bar
     */
    public void foo(String bar) {
    }

    /**
     * Baz endpoint.
     * 
     * @param baz
     * @return
     */
    public String bar(String baz) {
        return baz;
    }

}
