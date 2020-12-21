package com.vaadin.flow.server.connect.generator.endpoints.collectionendpoint;

import com.vaadin.flow.server.connect.generator.endpoints.AbstractEndpointGenerationTest;
import com.vaadin.flow.server.connect.generator.endpoints.AbstractEndpointGeneratorBaseTest;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 * com.vaadin.flow.server.connect.generator.endpoints.collectionendpoint.IterableEndpointGenerationTest, created on 21/12/2020 23.00
 * @author nikolaigorokhov
 */
public class IterableEndpointGenerationTest extends AbstractEndpointGenerationTest {

  public IterableEndpointGenerationTest() {
    super(Collections.singletonList(IterableEndpoint.class));
  }

  @Test
  public void should_ConvertIterableIntoArrayInTS() {
    verifyOpenApiObjectAndGeneratedTs();
  }
}
