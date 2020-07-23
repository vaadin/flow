/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.component.Component;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import elemental.json.JsonValue;

/**
 * Base class for lazy data view unit tests. Provides common mocks and util
 * methods.
 */
public abstract class BaseLazyDataViewTest {

    protected DataCommunicatorTest.MockUI ui;
    @Mock
    protected ArrayUpdater arrayUpdater;

    protected void initMocks() {
        MockitoAnnotations.initMocks(this);
        ui = new DataCommunicatorTest.MockUI();

        ArrayUpdater.Update update = new ArrayUpdater.Update() {

            @Override
            public void clear(int start, int length) {

            }

            @Override
            public void set(int start, List<JsonValue> items) {

            }

            @Override
            public void commit(int updateId) {

            }
        };

        Mockito.when(arrayUpdater.startUpdate(Mockito.anyInt()))
                .thenReturn(update);
    }

    protected <T> DataCommunicator<T> getDataCommunicator(Component component) {
        return new DataCommunicator<T>((item, jsonObject) -> {
        }, arrayUpdater, null, component.getElement().getNode());
    }

    protected void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }
}
