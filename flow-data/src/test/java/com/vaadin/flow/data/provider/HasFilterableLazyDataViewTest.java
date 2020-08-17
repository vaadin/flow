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

import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HasFilterableLazyDataViewTest {

    // TODO: rework tests
//    @Tag("test-component")
//    private static class TestComponent<T> extends Component implements
//            HasFilterableLazyDataView<T, String, AbstractFilterableLazyDataView<T, String>> {
//
//        private String filter = null;
//
//        private SerializableConsumer<String> filterSlot = (filter) -> {
//        };
//
//        private DataCommunicator<T> dataCommunicator;
//
//        public TestComponent() {
//            dataCommunicator = new DataCommunicator<>((item, jsonObject) -> {
//            }, null, null, getElement().getNode());
//        }
//
//        @Override
//        public AbstractFilterableLazyDataView<T, String> setItemsWithFilter(
//                BackEndDataProvider<T, String> dataProvider) {
//            filterSlot = dataCommunicator.setDataProvider(dataProvider, null);
//            return getFilterableLazyDataView();
//        }
//
//        @Override
//        public <Q> AbstractFilterableLazyDataView<T, String> setItemsWithConvertedFilter(
//                CallbackDataProvider.FetchCallback<T, Q> fetchCallback,
//                CallbackDataProvider.CountCallback<T, Q> countCallback,
//                SerializableFunction<String, Q> filterConverter) {
//            SerializableConsumer<Q> dataCommunicatorFilterSlot = dataCommunicator
//                    .setDataProvider(DataProvider.fromFilteringCallbacks(
//                            fetchCallback, countCallback), null);
//            filterSlot = (filter) -> dataCommunicatorFilterSlot
//                    .accept(filterConverter.apply(filter));
//            return getFilterableLazyDataView();
//        }
//
//        @Override
//        public AbstractFilterableLazyDataView<T, String> getFilterableLazyDataView() {
//            return new AbstractFilterableLazyDataView<T, String>(
//                    dataCommunicator, this, filterSlot, this::getFilter) {
//            };
//        }
//
//        public DataCommunicator<T> getDataCommunicator() {
//            return dataCommunicator;
//        }
//
//        public String getFilter() {
//            return filter;
//        }
//    }
//
//    @Rule
//    public ExpectedException expectedException = ExpectedException.none();
//
//    @Test
//    public void setItemsCountCallback_switchesToDefinedSize_throwsOnSizeQuery() {
//        TestComponent<String> testComponent = new TestComponent<>();
//        // uses a NOOP count callback that will throw when called
//        testComponent
//                .setItemsWithFilter(query -> Stream.of("foo", "bar", "baz"));
//
//        Assert.assertFalse(testComponent.getFilterableLazyDataView()
//                .getDataCommunicator().isDefinedSize());
//
//        testComponent.getFilterableLazyDataView()
//                .setItemCountFromDataProvider();
//
//        Assert.assertTrue(testComponent.getFilterableLazyDataView()
//                .getDataCommunicator().isDefinedSize());
//
//        expectedException.expect(IllegalStateException.class);
//        expectedException.expectMessage(
//                "Trying to use exact size with a lazy loading component"
//                        + " without either providing a count callback for the"
//                        + " component to fetch the count of the items or a data"
//                        + " provider that implements the size query. Provide the "
//                        + "callback for fetching item count with%n"
//                        + "component.getFilterableLazyDataView().withDefinedSize(CallbackDataProvider.CountCallback);"
//                        + "%nor switch to undefined size with%n"
//                        + "component.getFilterableLazyDataView().withUndefinedSize();");
//        // to make things fail, just need to call size() which will trigger a
//        // size query
//        //
//        // Although we don't have getSize() method for lazy data view, it is
//        // still possible for developer to call getItemCount() from
//        // dataCommunicator.
//        testComponent.getDataCommunicator().getItemCount();
//    }
}
