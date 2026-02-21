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

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HasLazyDataViewTest {

    @Tag("test-component")
    private static class TestComponent extends Component implements
            HasLazyDataView<String, Void, AbstractLazyDataView<String>> {

        private DataCommunicator<String> dataCommunicator;

        public TestComponent() {
            dataCommunicator = new DataCommunicator<>((item, jsonObject) -> {
            }, null, null, getElement().getNode());
        }

        @Override
        public AbstractLazyDataView<String> setItems(
                BackEndDataProvider<String, Void> dataProvider) {
            dataCommunicator.setDataProvider(dataProvider, null);
            return getLazyDataView();
        }

        @Override
        public AbstractLazyDataView<String> getLazyDataView() {
            return new AbstractLazyDataView<String>(dataCommunicator, this) {
            };
        }

        public DataCommunicator<String> getDataCommunicator() {
            return dataCommunicator;
        }
    }

    @Test
    void setItemsCountCallback_switchesToDefinedSize_throwsOnSizeQuery() {
        TestComponent testComponent = new TestComponent();
        // uses a NOOP count callback that will throw when called
        testComponent.setItems(query -> Stream.of("foo", "bar", "baz"));

        assertFalse(testComponent.getLazyDataView().getDataCommunicator()
                .isDefinedSize());

        testComponent.getLazyDataView().setItemCountFromDataProvider();

        assertTrue(testComponent.getLazyDataView().getDataCommunicator()
                .isDefinedSize());

        // to make things fail, just need to call size() which will trigger a
        // size query
        //
        // Although we don't have getSize() method for lazy data view, it is
        // still possible for developer to call getItemCount() from
        // dataCommunicator.
        assertThrows(IllegalStateException.class,
                () -> testComponent.getDataCommunicator().getItemCount());
    }

}
