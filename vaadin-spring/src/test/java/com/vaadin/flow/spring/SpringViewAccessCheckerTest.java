package com.vaadin.flow.spring;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.ViewAccessChecker;

@SpringBootTest(classes = { SpringViewAccessChecker.class })
@Deprecated(forRemoval = true)
class SpringViewAccessCheckerTest {

    @MockitoBean
    private AccessAnnotationChecker annotationChecker;
    @MockitoBean
    private Authentication authentication;

    @Autowired
    private ViewAccessChecker checker;

    public static class TestView extends Component {

    }

    @Test
    void viewAccessControlWorksWithoutRequest() {
        checker.enable();
        AtomicBoolean accessChecked = new AtomicBoolean(false);

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_fake"));

        Mockito.when(authentication.getAuthorities())
                .thenReturn((Collection) grantedAuthorities);
        SecurityContextHolder.setContext(new SecurityContext() {

            @Override
            public Authentication getAuthentication() {
                return authentication;
            }

            @Override
            public void setAuthentication(Authentication authentication) {
            }

        });
        Mockito.when(annotationChecker.hasAccess(Mockito.any(Class.class),
                Mockito.any(), Mockito.any())).thenAnswer(answer -> {
                    Principal principal = answer.getArgument(1);
                    Function<String, Boolean> roleChecker = answer
                            .getArgument(2);

                    Assert.assertEquals(
                            "Principal from security context should have been passed to checker",
                            authentication, principal);

                    Assert.assertTrue(
                            "Role should have been checked from the context holder",
                            roleChecker.apply("fake"));
                    Assert.assertFalse(
                            "Role should have been checked from the context holder",
                            roleChecker.apply("fake2"));
                    accessChecked.set(true);
                    return true;
                });

        BeforeEnterEvent beforeEnterEvent = Mockito
                .mock(BeforeEnterEvent.class);
        Mockito.when(beforeEnterEvent.getNavigationTarget())
                .thenReturn((Class) TestView.class);
        checker.beforeEnter(beforeEnterEvent);
        Assert.assertTrue("Annotation checker should have been invoked",
                accessChecked.get());
    }
}
