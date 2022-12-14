package com.vaadin.flow.spring.security;

import jakarta.servlet.http.HttpSession;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedHttpSession;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

public class VaadinAwareSecurityContextHolderStrategyTest {

    private VaadinAwareSecurityContextHolderStrategy vaadinAwareSecurityContextHolderStrategy;

    @Before
    public void setup() {
        vaadinAwareSecurityContextHolderStrategy = new VaadinAwareSecurityContextHolderStrategy();
        CurrentInstance.clearAll();
    }

    @After
    public void teardown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void currentSessionOverrides() {
        VaadinSession vaadinSession = Mockito.mock(VaadinSession.class);
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        Mockito.when(vaadinSession.getSession())
                .thenReturn(new WrappedHttpSession(httpSession));
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(httpSession.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .thenReturn(securityContext);
        VaadinSession.setCurrent(vaadinSession);

        vaadinAwareSecurityContextHolderStrategy
                .setContext(Mockito.mock(SecurityContext.class));
        Assert.assertEquals(securityContext,
                vaadinAwareSecurityContextHolderStrategy.getContext());
    }

    @Test
    public void detachedSessionWorks() {
        VaadinSession vaadinSession = Mockito.mock(VaadinSession.class);
        Mockito.when(vaadinSession.getSession()).thenReturn(null);
        VaadinSession.setCurrent(vaadinSession);

        SecurityContext explicit = Mockito.mock(SecurityContext.class);
        vaadinAwareSecurityContextHolderStrategy.setContext(explicit);
        Assert.assertEquals(explicit,
                vaadinAwareSecurityContextHolderStrategy.getContext());
    }

    @Test
    public void explicitUsedWhenNoSessionAvailable() {
        SecurityContext explicit = Mockito.mock(SecurityContext.class);
        vaadinAwareSecurityContextHolderStrategy.setContext(explicit);
        Assert.assertEquals(explicit,
                vaadinAwareSecurityContextHolderStrategy.getContext());
    }
}
