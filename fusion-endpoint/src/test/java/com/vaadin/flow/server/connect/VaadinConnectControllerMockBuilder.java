package com.vaadin.flow.server.connect;

import static org.mockito.Mockito.mock;

import com.vaadin.flow.server.connect.auth.VaadinConnectAccessChecker;

import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

public class VaadinConnectControllerMockBuilder {
    private ApplicationContext applicationContext;

    public VaadinConnectControllerMockBuilder withApplicationContext(ApplicationContext applicationContext){
       this.applicationContext = applicationContext;
       return this;
    }

    public VaadinConnectController build(){
        VaadinConnectController controller = Mockito.spy(
            new VaadinConnectController(null,
                      mock(EndpointNameChecker.class),
                      mock(ExplicitNullableTypeChecker.class),
                      applicationContext)
        );
        Mockito.doReturn(mock(VaadinConnectAccessChecker.class)).when(controller).getAccessChecker(Mockito.any());
        return controller;
    }
}
