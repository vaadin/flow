/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.spring.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.web.session.SessionInformationExpiredEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UidlExpiredSessionStrategyTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest(
            "GET", "/app/");

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private final SessionInformation session = new SessionInformation(
            "principal", "1234", Date.from(Instant.now()));

    @Mock
    private FilterChain filterChain;

    @Test
    void filterChainAvailable_requestContinues()
            throws IOException, ServletException {
        var event = new SessionInformationExpiredEvent(session, request,
                response, filterChain);

        new UidlExpiredSessionStrategy().onExpiredSessionDetected(event);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(response.getRedirectedUrl()).isNull();
    }

    @Test
    void noFilterChain_redirectsToApplicationRoot()
            throws IOException, ServletException {
        request.setContextPath("/app");
        var event = new SessionInformationExpiredEvent(session, request,
                response);

        new UidlExpiredSessionStrategy().onExpiredSessionDetected(event);

        assertThat(response.getRedirectedUrl()).isEqualTo("/app/");
    }
}
