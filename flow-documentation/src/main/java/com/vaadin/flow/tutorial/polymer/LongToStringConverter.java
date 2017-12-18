/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.tutorial.polymer;

import java.util.Optional;

import com.vaadin.flow.templatemodel.ModelConverter;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("polymer-templates/tutorial-template-model-converters.asciidoc")
public class LongToStringConverter implements ModelConverter<Long, String> {

    @Override
    public String toPresentation(Long modelValue) {
        return Optional.ofNullable(modelValue).map(Object::toString)
                .orElse(null);
    }

    @Override
    public Long toModel(String presentationValue) {
        return Optional.ofNullable(presentationValue).map(Long::valueOf)
                .orElse(null);
    }

}
