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
package com.vaadin.flow.server.connect.generator.services.complexhierarchyservice;

import java.util.Arrays;

import org.junit.Test;

import com.vaadin.flow.server.connect.generator.services.AbstractServiceGenerationTest;
import com.vaadin.flow.server.connect.generator.services.complexhierarchymodel.GrandParentModel;
import com.vaadin.flow.server.connect.generator.services.complexhierarchymodel.Model;
import com.vaadin.flow.server.connect.generator.services.complexhierarchymodel.ParentModel;

public class ComplexHierarchyServiceTest extends AbstractServiceGenerationTest {
    public ComplexHierarchyServiceTest() {
        super(Arrays.asList(ComplexHierarchyService.class, Model.class,
                ParentModel.class, GrandParentModel.class));
    }

    @Test
    public void should_GenerateParentModel_When_UsingChildModel() {
        verifyOpenApiObjectAndGeneratedTs();
    }
}
