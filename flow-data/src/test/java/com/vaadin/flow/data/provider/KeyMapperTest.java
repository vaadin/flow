/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.data.provider;

import org.junit.Assert;
import org.junit.Test;

public class KeyMapperTest {

    @Test
    public void refreshWithOldItem_remapsIdentity() {
        // Use identity-based KeyMapper (default)
        KeyMapper<String> mapper = new KeyMapper<>();

        String key = mapper.key("old");
        Assert.assertTrue(mapper.has("old"));

        mapper.refresh("new", "old");

        Assert.assertFalse(mapper.has("old"));
        Assert.assertTrue(mapper.has("new"));
        Assert.assertEquals("new", mapper.get(key));
    }

    @Test
    public void refreshWithOldItem_unknownOldItem_noEffect() {
        KeyMapper<String> mapper = new KeyMapper<>();
        mapper.key("existing");

        mapper.refresh("new", "unknown");

        Assert.assertTrue(mapper.has("existing"));
        Assert.assertFalse(mapper.has("new"));
    }
}
