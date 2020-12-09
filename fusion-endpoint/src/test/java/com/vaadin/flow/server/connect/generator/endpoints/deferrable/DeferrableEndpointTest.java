package com.vaadin.flow.server.connect.generator.endpoints.deferrable;

import java.util.Arrays;

import com.vaadin.flow.server.connect.generator.endpoints.AbstractEndpointGenerationTest;

import org.junit.Test;

public class DeferrableEndpointTest extends AbstractEndpointGenerationTest {
    
    public DeferrableEndpointTest() {
        super(Arrays.asList(WholeClassDeferrableEndpoint.class, SingleMethodDeferrableEndpoint.class));
    }
    
    @Test
    public void should_DeferrableResult_When_ASingleMethod_Or_TheWholeClass_IsMarkedAsDeferrable() {
        verifyOpenApiObjectAndGeneratedTs();
    }
}
