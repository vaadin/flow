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
package io.swagger.parser;

import java.util.Collections;
import java.util.List;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.extensions.SwaggerParserExtension;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

/**
 * A copy of the original OpenAPIParser to avoid problems with flow. Using the
 * original one makes Flow CompositeTest and CompositeTextNodeTest tests fail.
 * 
 * NOTE: This is a copy of the original file which is under the Apache
 * License 2.0 as described at https://swagger.io/license/
 *
 * @author manolo
 * @since 3.0
 *
 */
public class OpenAPIParser {
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options) { // NOSONAR
        SwaggerParseResult output = null;
        for(SwaggerParserExtension extension : getExtensions()) {
            output = extension.readLocation(url, auth, options);
            if(output != null && output.getOpenAPI() != null) {
                return output;
            }
        }
        return output;
    }

    public SwaggerParseResult readContents(String swaggerAsString, List<AuthorizationValue> auth, ParseOptions options) { // NOSONAR
        SwaggerParseResult output = null;

        for(SwaggerParserExtension extension : getExtensions()) {
            output = extension.readContents(swaggerAsString, auth, options);
            if(output != null && output.getOpenAPI() != null) {
                return output;
            }
        }

        return output;
    }

    protected List<SwaggerParserExtension> getExtensions() {
        // Returning always the OpenAPIV3 version
        // It is safe to return it since we are using V3
        // in flow CCDM generators.
        return Collections.singletonList(new OpenAPIV3Parser());
    }
}
