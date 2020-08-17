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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.tests.data.bean.Person;
import joptsimple.internal.Strings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AbstractFilterableLazyDataViewTest extends BaseLazyDataViewTest {

    // TODO: rework tests
//    private DataCommunicator<Person> dataCommunicator;
//    private AbstractFilterableLazyDataView<Person, PersonFilter> dataView;
//    private TestComponent component;
//
//    @Before
//    public void setup() {
//        initMocks();
//        component = new TestComponent();
//        ui.add(component);
//
//        dataCommunicator = getDataCommunicator(component);
//        dataCommunicator.setPageSize(100);
//        component.setDataProvider(getDataProvider());
//        dataView = component.getFilterableLazyDataView();
//    }
//
//    @Test
//    public void setFilter_noFilter_filterApplied() {
//        final PersonFilter filter = new PersonFilter(null, null, 5);
//
//        dataView.addItemCountChangeListener(event -> Assert.assertEquals(
//                "ItemCountChangeEvent contains incorrect item count", 10,
//                event.getItemCount()));
//        dataView.setFilter(filter);
//
//        dataCommunicator.setRequestedRange(0, 100);
//        fakeClientCommunication();
//
//        List<Person> filteredPersons = dataView.getItems()
//                .collect(Collectors.toList());
//        Assert.assertEquals("Invalid filtered item count returned", 10,
//                filteredPersons.size());
//        filteredPersons.forEach(person -> Assert
//                .assertEquals("Invalid person age", 5, person.getAge()));
//    }
//
//    @Test
//    public void setFilter_filterAlreadyPresent_oldFilterReplaced() {
//        // Choosing the defined or undefined mode should not affect the
//        // filtering, but anyway do the filtering for undefined mode
//        // just to check that
//        dataView.setItemCountUnknown();
//        // Sets the component initial filter
//        component.setFilter(new PersonFilter("John0", "Doe0", -1));
//        // New filter
//        final PersonFilter filter = new PersonFilter("John1", "Doe1", -1);
//
//        dataView.addItemCountChangeListener(event -> Assert.assertEquals(
//                "ItemCountChangeEvent contains incorrect item count", 1,
//                event.getItemCount()));
//        // Sets the new filter
//        dataView.setFilter(filter);
//
//        dataCommunicator.setRequestedRange(0, 100);
//        fakeClientCommunication();
//
//        List<Person> filteredPersons = dataView.getItems()
//                .collect(Collectors.toList());
//        Assert.assertEquals("Invalid filtered item count returned", 1,
//                filteredPersons.size());
//        Assert.assertEquals("Invalid filtered value: first name",
//                filter.getFirstName(),
//                filteredPersons.iterator().next().getFirstName());
//        Assert.assertEquals("Invalid filtered value: last name",
//                filter.getLastName(),
//                filteredPersons.iterator().next().getLastName());
//    }
//
//    @Test
//    public void removeFilter_filterAlreadySet_filterRemoved() {
//        // Sets the component initial filter
//        component.setFilter(new PersonFilter("John0", "Doe0", -1));
//
//        dataCommunicator.setRequestedRange(0, 100);
//        fakeClientCommunication();
//
//        List<Person> filteredPersons = dataView.getItems()
//                .collect(Collectors.toList());
//        Assert.assertEquals("Invalid filtered item count returned", 1,
//                filteredPersons.size());
//
//        dataView.removeFilter();
//
//        dataCommunicator.setRequestedRange(0, 100);
//        fakeClientCommunication();
//
//        filteredPersons = dataView.getItems().collect(Collectors.toList());
//        Assert.assertEquals("Invalid non-filtered item count returned", 100,
//                filteredPersons.size());
//    }
//
//    @Test
//    public void setFilterCombiner_filterAlreadySet_oldAndNewFiltersApplied() {
//        // Sets the component initial filter
//        component.setFilter(new PersonFilter(null, null, 5));
//        // Sets a new filter
//        final PersonFilter filter = new PersonFilter("John45", "Doe45", -1);
//
//        dataCommunicator.setRequestedRange(0, 100);
//        fakeClientCommunication();
//
//        List<Person> filteredPersons = dataView.getItems()
//                .collect(Collectors.toList());
//        Assert.assertEquals("Invalid filtered item count returned", 10,
//                filteredPersons.size());
//
//        dataView.setFilterCombiner((newFilter, oldFilter) -> new PersonFilter(
//                newFilter.getFirstName(), newFilter.getLastName(),
//                oldFilter.getAge()));
//        dataView.setFilter(filter);
//
//        dataCommunicator.setRequestedRange(0, 100);
//        fakeClientCommunication();
//
//        filteredPersons = dataView.getItems().collect(Collectors.toList());
//        Assert.assertEquals("Invalid filtered item count returned", 1,
//                filteredPersons.size());
//    }
//
//    @Test
//    public void setItemCountCallbackWithFilter() {
//        dataView.addItemCountChangeListener(event -> Assert.assertEquals(
//                "Invalid item count returned", 42, event.getItemCount()));
//        dataCommunicator.setRequestedRange(0, 50);
//        dataView.setItemCountCallbackWithFilter(query -> 42);
//        fakeClientCommunication();
//    }
//
//    @Tag("test-component")
//    private class TestComponent extends Component implements
//            HasFilterableLazyDataView<Person, PersonFilter, AbstractFilterableLazyDataView<Person, PersonFilter>> {
//
//        private PersonFilter filter = null;
//
//        private SerializableConsumer<PersonFilter> filterSlot = filter -> {
//        };
//
//        private DataCommunicator<Person> dataCommunicator;
//
//        public <C> void setDataProvider(DataProvider<Person, C> dataProvider,
//                SerializableFunction<PersonFilter, C> filterConverter) {
//            dataCommunicator = AbstractFilterableLazyDataViewTest.this.dataCommunicator;
//
//            SerializableConsumer<C> dataCommunicatorFilterSlot = dataCommunicator
//                    .setDataProvider(dataProvider,
//                            filterConverter.apply(this.filter));
//
//            filterSlot = filter -> {
//                if (!Objects.equals(filter, this.filter)) {
//                    dataCommunicatorFilterSlot
//                            .accept(filterConverter.apply(filter));
//                    this.filter = filter;
//                }
//            };
//        }
//
//        public void setDataProvider(
//                DataProvider<Person, PersonFilter> dataProvider) {
//            setDataProvider(dataProvider, SerializableFunction.identity());
//        }
//
//        @Override
//        public AbstractFilterableLazyDataView<Person, PersonFilter> setItemsWithFilter(
//                BackEndDataProvider<Person, PersonFilter> dataProvider) {
//            setDataProvider(dataProvider);
//            return getFilterableLazyDataView();
//        }
//
//        @Override
//        public <Q> AbstractFilterableLazyDataView<Person, PersonFilter> setItemsWithConvertedFilter(
//                CallbackDataProvider.FetchCallback<Person, Q> fetchCallback,
//                CallbackDataProvider.CountCallback<Person, Q> countCallback,
//                SerializableFunction<PersonFilter, Q> filterConverter) {
//            setDataProvider(DataProvider.fromFilteringCallbacks(fetchCallback,
//                    countCallback), filterConverter);
//            return getFilterableLazyDataView();
//        }
//
//        @Override
//        public AbstractFilterableLazyDataView<Person, PersonFilter> getFilterableLazyDataView() {
//            return new AbstractFilterableLazyDataView<Person, PersonFilter>(
//                    dataCommunicator, this, filterSlot, () -> this.filter) {
//            };
//        }
//
//        public PersonFilter getFilter() {
//            return filter;
//        }
//
//        public void setFilter(PersonFilter filter) {
//            this.filterSlot.accept(filter);
//        }
//    }
//
//    private DataProvider<Person, PersonFilter> getDataProvider() {
//        return DataProvider.fromFilteringCallbacks(
//                query -> getFilteredPersons(query).skip(query.getOffset())
//                        .limit(query.getLimit()),
//                query -> (int) getFilteredPersons(query).count());
//    }
//
//    private Stream<Person> getFilteredPersons(
//            Query<Person, PersonFilter> query) {
//        return IntStream.range(0, 100).mapToObj(
//                index -> new Person("John" + index, "Doe" + index, index % 10))
//                .filter(person -> query.getFilter()
//                        .map(personFilter -> personFilter.apply(person))
//                        .orElse(true));
//    }
//
//    private static class PersonFilter {
//        private String firstName;
//        private String lastName;
//        private int age;
//
//        public String getFirstName() {
//            return firstName;
//        }
//
//        public String getLastName() {
//            return lastName;
//        }
//
//        public int getAge() {
//            return age;
//        }
//
//        public PersonFilter(String firstName, String lastName, int age) {
//            this.firstName = firstName;
//            this.lastName = lastName;
//            this.age = age;
//        }
//
//        public boolean apply(Person person) {
//            boolean result = true;
//            if (!Strings.isNullOrEmpty(firstName)) {
//                result = firstName.equalsIgnoreCase(person.getFirstName());
//            }
//            if (!Strings.isNullOrEmpty(lastName)) {
//                result &= lastName.equalsIgnoreCase(person.getLastName());
//            }
//            if (age > 0) {
//                result &= age == person.getAge();
//            }
//            return result;
//        }
//    }
}
