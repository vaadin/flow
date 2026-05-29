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
package com.vaadin.flow.micrometer.client;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * One client-side measurement, deserialized by Flow's JSON codec from the
 * browser-side collector.
 */
public class ClientSample implements Serializable {

    private String name;
    private Map<String, String> tags;
    private double valueMs;
    private long ts;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getTags() {
        return tags == null ? Collections.emptyMap() : tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public double getValueMs() {
        return valueMs;
    }

    public void setValueMs(double valueMs) {
        this.valueMs = valueMs;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }
}
