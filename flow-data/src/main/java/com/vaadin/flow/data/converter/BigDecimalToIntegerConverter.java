/*
 * Copyright 2000-2024 Vaadin Ltd.
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

package com.vaadin.flow.data.converter;

import java.math.BigDecimal;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;

/**
 * A converter that converts from {@link BigDecimal} to {@link Integer} and
 * back.
 *
 * @since 24.5
 */
public class BigDecimalToIntegerConverter
        implements Converter<BigDecimal, Integer> {

    @Override
    public Result<Integer> convertToModel(BigDecimal value,
            ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }

        return Result.ok(value.intValue());
    }

    @Override
    public BigDecimal convertToPresentation(Integer value,
            ValueContext context) {
        if (value == null) {
            return null;
        }

        return BigDecimal.valueOf(value);
    }
}
