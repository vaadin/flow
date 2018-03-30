package com.vaadin.flow.server.communication;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultPushConfiguratorTest {

    @Test
    public void shouldApplyConfiguration() {
        PushConfiguration configuration = spy(MockPushConfiguration.class);
        Push pushAnnotation = mock(Push.class);
        when(pushAnnotation.value()).thenReturn(PushMode.MANUAL);
        when(pushAnnotation.transport()).thenReturn(Transport.LONG_POLLING);

        new DefaultPushConfigurator().configurePush(configuration, pushAnnotation);

        verify((PushConfiguration.WithConnectionFactory)configuration).setPushConnectionFactory(any());
        verify(configuration).setPushMode(PushMode.MANUAL);
        verify(configuration).setTransport(Transport.LONG_POLLING);
    }

    public abstract static class MockPushConfiguration implements PushConfiguration, PushConfiguration.WithConnectionFactory {

    }
}
