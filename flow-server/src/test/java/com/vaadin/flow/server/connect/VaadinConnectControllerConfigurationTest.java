package com.vaadin.flow.server.connect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.context.ApplicationContext;

public class VaadinConnectControllerConfigurationTest {

    @Test
    public void should_NotOverrideVisibility_When_JacksonPropertiesProvideVisibility() {
        ApplicationContext contextMock = mock(ApplicationContext.class);
        VaadinEndpointProperties endpointPropertiesMock = mock(VaadinEndpointProperties.class);
        VaadinConnectControllerConfiguration configuration = new VaadinConnectControllerConfiguration(endpointPropertiesMock);
        
        JacksonProperties mockJacksonProperties = mock(JacksonProperties.class);
        when(contextMock.getBean(JacksonProperties.class))
                .thenReturn(mockJacksonProperties);
        when(mockJacksonProperties.getVisibility())
                .thenReturn(Collections.singletonMap(PropertyAccessor.ALL,
                        JsonAutoDetect.Visibility.PUBLIC_ONLY));

        ObjectMapper objectMapper = configuration.vaadinEndpointMapper(contextMock);

        verify(contextMock, times(1)).getBean(JacksonProperties.class);

        try{
                String result = objectMapper.writeValueAsString(new Entity());
                assertEquals("{\"name\":\"Bond\"}", result);
        }catch(Exception e){
                fail("Failed to write an entity");
        }

    }

    public class Entity {
        @SuppressWarnings("unused")
        private String codeNumber = "007";
        private String name = "Bond";
        private String firstName = "James";

        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        protected String getFirstName() {
                return firstName;
        }

        protected void setFirstName(String firstName) {
                this.firstName = firstName;
        }
    }
}
