/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.shared.ui;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.json.JsonObject;

/**
 * @author Vaadin Ltd.
 */
public class DependencyTest {

    @Test
    public void checkJsonSerialization() {
        Dependency dependency = new Dependency(Dependency.Type.HTML_IMPORT, "url", LoadMode.INLINE);

        JsonObject dependencyJson = dependency.toJson();

        assertThat("No contents should be present in json now",
                dependencyJson.hasKey(Dependency.KEY_CONTENTS), is(false));
        assertThat("Dependency type should match corresponding enum name in pojo",
                dependencyJson.getString(Dependency.KEY_TYPE), is(dependency.getType().name()));
        assertThat("Dependency url should match corresponding url in pojo",
                dependencyJson.getString(Dependency.KEY_URL), is(dependency.getUrl()));
        assertThat("Dependency load mode should match corresponding enum name in pojo",
                dependencyJson.getString(Dependency.KEY_LOAD_MODE), is(dependency.getLoadMode().name()));
    }
}
