/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component.html;

import org.junit.Assert;
import org.junit.Test;

public class AnchorTargetValueTest {

    @Test
    public void fromString_notEnum_objectHasValueAndEquals() {
        AnchorTargetValue value = AnchorTargetValue.forString("foo");
        Assert.assertEquals("foo", value.getValue());

        AnchorTargetValue value1 = AnchorTargetValue.forString("foo");
        Assert.assertEquals(value, value1);
        Assert.assertEquals(value.hashCode(), value1.hashCode());
    }

    @Test
    public void fromString_enumValue_resultIsEnum() {
        AnchorTargetValue value = AnchorTargetValue
                .forString(AnchorTarget.TOP.getValue());
        Assert.assertEquals(AnchorTarget.TOP, value);
    }
}
