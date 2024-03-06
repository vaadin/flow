/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;

public class VaadinServletRequestTest {

    @Test
    public void getContentLengthLong_delegateToServletRequestGetContentLengthLong() {
        HttpServletRequest servletRequest = Mockito
                .mock(HttpServletRequest.class);

        VaadinServletRequest request = new VaadinServletRequest(servletRequest,
                null);
        request.getContentLengthLong();
        Mockito.verify(servletRequest).getContentLengthLong();
    }
}
