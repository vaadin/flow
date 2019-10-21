/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.flow.server.connect.generator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generates the Vaadin Connect Client file, based on the application
 * properties, if provided.
 */
public class VaadinConnectClientGenerator {
    static final String ENDPOINT = "vaadin.connect.endpoint";
    static final String DEFAULT_ENDPOINT = "/connect";
    
    private static final String CLIENT_FILE_NAME = "connect-client.default";
    
    public static final String DEFAULT_GENERATED_CONNECT_CLIENT_NAME = CLIENT_FILE_NAME + ".ts";
    public static final String DEFAULT_GENERATED_CONNECT_CLIENT_IMPORT_PATH = "./" + CLIENT_FILE_NAME;


  private final String endpoint;
  
  private static final Logger log = LoggerFactory
          .getLogger(VaadinConnectClientGenerator.class);

  /**
   * Creates the generator, getting the data needed for the generation out of
   * the application properties.
   *
   * @param applicationProperties
   *          the properties with the data required for the generation
   */
  public VaadinConnectClientGenerator(
      PropertiesConfiguration applicationProperties) {
    this.endpoint = applicationProperties.getString(ENDPOINT, DEFAULT_ENDPOINT);
  }

  /**
   * Generates the client file in the file specified.
   *
   * @param outputFilePath
   *          the file to generate the default client into
   */
  public void generateVaadinConnectClientFile(Path outputFilePath) {
    String generatedDefaultClientTs = getDefaultClientTsTemplate()
        .replace("{{ENDPOINT}}", endpoint);
    
    try {
        log.info("Writing output to {}", outputFilePath);
        FileUtils.writeStringToFile(outputFilePath.toFile(),
                generatedDefaultClientTs, StandardCharsets.UTF_8);
    } catch (IOException e) {
        String errorMessage = String.format(
                "Error while writing OpenAPI json file at %s",
                outputFilePath.toString());
        log.error(errorMessage, outputFilePath, e);
    }
  }

  private String getDefaultClientTsTemplate() {
    try (BufferedReader bufferedReader = new BufferedReader(
        new InputStreamReader(
            getClass().getClassLoader()
                .getResourceAsStream("connect-client.default.ts.template"),
            StandardCharsets.UTF_8))) {
      return bufferedReader.lines().collect(Collectors.joining("\n"));
    } catch (IOException e) {
      throw new IllegalStateException(
          "Unable to read connect-client.default.ts.template", e);
    }
  }
}
