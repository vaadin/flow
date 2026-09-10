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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.web.session.SessionInformationExpiredEvent;

import com.vaadin.flow.shared.ApplicationConstants;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UidlExpiredSessionStrategyTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpServletRequest request;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpServletResponse response;

    private SessionInformationExpiredEvent event;

    @BeforeEach
    void setup() {
        var session = new SessionInformation("principal", "1234",
                Date.from(Instant.now()));
        event = new SessionInformationExpiredEvent(session, request, response);
        when(request.getHttpServletMapping().getPattern()).thenReturn("/");
    }

    @Test
    void internalRequest_writesRefreshTokenForContextRoot() throws IOException {
        markAsUidlRequest();
        when(request.getContextPath()).thenReturn("");

        new UidlExpiredSessionStrategy().onExpiredSessionDetected(event);

        verify(response.getWriter()).write("Vaadin-Refresh: /");
    }

    @Test
    void internalRequestWithContextPath_refreshTokenKeepsContextPath()
            throws IOException {
        markAsUidlRequest();
        when(request.getContextPath()).thenReturn("/app");

        new UidlExpiredSessionStrategy().onExpiredSessionDetected(event);

        verify(response.getWriter()).write("Vaadin-Refresh: /app/");
    }

    @Test
    void internalRequestWithCustomUrl_refreshTokenPointsToCustomUrl()
            throws IOException {
        markAsUidlRequest();
        when(request.getContextPath()).thenReturn("");

        new UidlExpiredSessionStrategy("/login")
                .onExpiredSessionDetected(event);

        verify(response.getWriter()).write("Vaadin-Refresh: /login");
    }

    @Test
    void externalRequest_redirectsToContextRelativeUrl() throws IOException {
        when(request.getContextPath()).thenReturn("/app");
        when(response.encodeRedirectURL(anyString()))
                .thenAnswer(i -> i.getArguments()[0]);

        new UidlExpiredSessionStrategy("/login")
                .onExpiredSessionDetected(event);

        verify(response).sendRedirect("/app/login");
    }

    private void markAsUidlRequest() {
        when(request.getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(ApplicationConstants.REQUEST_TYPE_UIDL);
    }
}
