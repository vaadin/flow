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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DataProviderTest {

    @Test
    @SuppressWarnings("unchecked")
    void notifyItemsAddedAndRemoved_notOverridden_refreshesAll() {
        DataProvider<String, Void> dataProvider = mock(DataProvider.class,
                CALLS_REAL_METHODS);

        dataProvider.notifyItemsAdded(List.of("added"));
        dataProvider.notifyItemsRemoved(List.of("removed"));

        verify(dataProvider, times(2)).refreshAll();
    }
}
