/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import org.junit.Test;
import org.mockito.Mockito;

public class VaadinRequestTest {

    public static abstract class TestVaadinRequest implements VaadinRequest {

    }

    @Test
    public void getContentLengthLong_delegateToGetContentLength() {
        TestVaadinRequest request = Mockito.spy(TestVaadinRequest.class);
        request.getContentLengthLong();
        Mockito.verify(request).getContentLength();
    }

}
