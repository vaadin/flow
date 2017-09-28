/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.tutorial.databinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.CallbackDataProvider.CountCallback;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.provider.SortOrder;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.tutorial.databinding.Person.Department;

@CodeFor("binding-data/tutorial-flow-data-provider.asciidoc")
public class DataProviders {

    public static class PersonSort {

    }

    public class PersonFilter {
        public final String namePrefix;
        public final Department department;

        public PersonFilter(String namePrefix, Department department) {
            this.namePrefix = namePrefix;
            this.department = department;
        }
    }

    public enum Status {
        OK, ERROR;
        public String getLabel() {
            return "";
        }
    }

    public interface PersonService {

        List<Person> fetchPersons(int offset, int limit);

        List<Person> fetchPersons(int offset, int limit,
                List<PersonSort> sortOrders);

        int getPersonCount();

        // @formatter:off
        PersonSort createSort(
                String propertyName,
                boolean descending);

        List<Person> fetchPersons(
                int offset,
                int limit,
                String namePrefix);
        // @formatter:on

        int getPersonCount(String namePrefix);

        List<Person> fetchPersons(int offset, int limit, String namePrefix,
                Department department);

        // @formatter:off
        int getPersonCount(
                String namePrefix,
                Department department);
        // @formatter:on

        Person save(Person person);
    }

    // @formatter:off
    public class PersonDataProvider
        extends AbstractBackEndDataProvider<Person, String> {

        private Department departmentFilter;

        public void setDepartmentFilter(Department department) {
          this.departmentFilter = department;
          refreshAll();
        }

        @Override
        protected Stream<Person> fetchFromBackEnd(Query<Person, String> query) {
          return getPersonService().fetchPersons(
            query.getOffset(),
            query.getLimit(),
            query.getFilter().orElse(null),
            departmentFilter
          ).stream();
        }

        @Override
        protected int sizeInBackEnd(Query<Person, String> query) {
          return getPersonService().getPersonCount(
            query.getFilter().orElse(null),
            departmentFilter
          );
        }
      }
    // @formatter:on

    public void combobox() {
        ComboBox<Status> comboBox = new ComboBox<>();
        comboBox.setItemLabelGenerator(Status::getLabel);

        // Sets items as a collection
        comboBox.setItems(EnumSet.allOf(Status.class));

        List<Status> itemsToShow = null;

        // @formatter:off
        /*
        comboBox.setItems(
                (itemCaption, filterText) -> itemCaption.startsWith(filterText),
                itemsToShow);
        */
        // @formatter:on
    }

    public void grid() {
        Grid<Person> grid = new Grid<>();
        grid.addColumn(Person::getName).setHeader("Name");
        grid.addColumn(person -> Integer.toString(person.getYearOfBirth()))
                .setHeader("Year of birth");

        // Sets items using varargs
        // @formatter:off
        grid.setItems(
                new Person("George Washington", 1732),
                new Person("John Adams", 1735),
                new Person("Thomas Jefferson", 1743),
                new Person("James Madison", 1751)
              );
        // @formatter:on

        grid.addColumn(Person::getName).setHeader("Name")
                // Override default natural sorting
                .setComparator(Comparator
                        .comparing(person -> person.getName().toLowerCase()));
    }

    public void listDataProvider() {
        List<Person> persons = Collections.emptyList();
        Button button = new Button();

        // @formatter:off
        ListDataProvider<Person> dataProvider =
                DataProvider.ofCollection(persons);

              dataProvider.setSortOrder(Person::getName,
                SortDirection.ASCENDING);

              Grid<Person> grid = new Grid<>();
              // The grid shows the persons sorted by name
              grid.setDataProvider(dataProvider);

              // Makes the combo box show persons in descending order
              button.addClickListener(event -> {
                dataProvider.setSortOrder(Person::getName,
                  SortDirection.DESCENDING);
              });
         // @formatter:on
    }

    public void captionFilter() {
        List<Person> persons = Collections.emptyList();
        ComboBox<Person.Department> departmentSelect = new ComboBox<>();

        // @formatter:off
        ListDataProvider<Person> dataProvider = DataProvider
                .ofCollection(persons);

        ComboBox<Person> comboBox = new ComboBox<>();
        comboBox.setDataProvider(dataProvider);

        departmentSelect.addValueChangeListener(event -> {
            Department selectedDepartment = event.getValue();
            if (selectedDepartment != null) {
                dataProvider.setFilterByValue(
                        Person::getDepartment,
                        selectedDepartment);
            } else {
                dataProvider.clearFilters();
            }
        });
        //@formatter:on
    }

    public void refresh() {
        List<Person> persons = Collections.emptyList();

        // @formatter:off
        ListDataProvider<Person> dataProvider =
                new ListDataProvider<>(persons);

          Button addPersonButton = new Button("Add person",
            clickEvent -> {
              persons.add(new Person("James Monroe", 1758));

              dataProvider.refreshAll();
          });

          Button modifyPersonButton = new Button("Modify person",
            clickEvent -> {
              Person personToChange = persons.get(0);

              personToChange.setName("Changed person");

              dataProvider.refreshItem(personToChange);
          });
        //@formatter:on
    }

    public void serviceDataProvider() {
        DataProvider<Person, Void> dataProvider = DataProvider.fromCallbacks(
                // First callback fetches items based on a query
                query -> {
                    // The index of the first item to load
                    int offset = query.getOffset();

                    // The number of items to load
                    int limit = query.getLimit();

                    List<Person> persons = getPersonService()
                            .fetchPersons(offset, limit);

                    return persons.stream();
                },
                // Second callback fetches the number of items for a query
                query -> getPersonService().getPersonCount());

        Grid<Person> grid = new Grid<>();
        grid.setDataProvider(dataProvider);

        // Columns are configured in the same way as before
    }

    public void sorting() {
        //@formatter:off
        DataProvider<Person, Void> dataProvider = DataProvider.fromCallbacks(
                query -> {
                  List<PersonSort> sortOrders = new ArrayList<>();
                  for(SortOrder<String> queryOrder : query.getSortOrders()) {
                    PersonSort sort = getPersonService().createSort(
                      // The name of the sorted property
                      queryOrder.getSorted(),
                      // The sort direction for this property
                      queryOrder.getDirection() == SortDirection.DESCENDING);
                    sortOrders.add(sort);
                  }

                  return getPersonService().fetchPersons(
                      query.getOffset(),
                      query.getLimit(),
                      sortOrders
                    ).stream();
                },
                // The number of persons is the same regardless of ordering
                query -> getPersonService().getPersonCount()
              );

      //@formatter:on
    }

    public void filter() {
        TextField searchField = new TextField();

      //@formatter:off
        DataProvider<Person, String> personProvider = getPersonProvider();

        ConfigurableFilterDataProvider<Person, Void, String> wrapper =
          personProvider.withConfigurableFilter();

        Grid<Person> grid = new Grid<>();
        grid.setDataProvider(personProvider);
        grid.addColumn(Person::getName).setHeader("Name");

        searchField.addValueChangeListener(event -> {
          String filter = event.getValue();
          if (filter.trim().isEmpty()) {
            // null disables filtering
            filter = null;
          }

          wrapper.setFilter(filter);
        });
      //@formatter:on
    }

    public void configurableFilter() {
      //@formatter:off
        DataProvider<Person, Set<String>> personProvider = getPersonsProvider();

        ConfigurableFilterDataProvider<Person, String, Set<String>> wrapper =
          personProvider.withConfigurableFilter(
            (String queryFilter, Set<String> configuredFilters) -> {
              Set<String> combinedFilters = new HashSet<>();
              combinedFilters.addAll(configuredFilters);
              combinedFilters.add(queryFilter);
              return combinedFilters;
            }
          );

        wrapper.setFilter(Collections.singleton("John"));

        ComboBox<Person> comboBox = new ComboBox<>();
        comboBox.setDataProvider(wrapper);
      //@formatter:on
    }

    public void moreFiltering() {
        DataProvider<Person, String> dataProvider = DataProvider
                .fromFilteringCallbacks(query -> {
                    // getFilter returns Optional<String>
                    String filter = query.getFilter().orElse(null);
                    return getPersonService().fetchPersons(query.getOffset(),
                            query.getLimit(), filter).stream();
                }, query -> {
                    String filter = query.getFilter().orElse(null);
                    return getPersonService().getPersonCount(filter);
                });
    }

    public void multipleFilteringParameters() {
        String someText = null;
        Department someDepartment = null;

        DataProvider<Person, PersonFilter> dataProvider = DataProvider
                .fromFilteringCallbacks(query -> {
                    PersonFilter filter = query.getFilter().orElse(null);
                    return getPersonService()
                            .fetchPersons(query.getOffset(), query.getLimit(),
                                    filter != null ? filter.namePrefix : null,
                                    filter != null ? filter.department : null)
                            .stream();
                }, query -> {
                    PersonFilter filter = query.getFilter().orElse(null);
                    return getPersonService().getPersonCount(
                            filter != null ? filter.namePrefix : null,
                            filter != null ? filter.department : null);
                });

        // For use with ComboBox without any department filter
        // @formatter:off
        DataProvider<Person, String> onlyString = dataProvider.withConvertedFilter(
          filterString -> new PersonFilter(filterString, null)
        );

        // For use with some external filter, e.g. a search form
        ConfigurableFilterDataProvider<Person, Void, PersonFilter> everythingConfigurable =
          dataProvider.withConfigurableFilter();
        everythingConfigurable.setFilter(
          new PersonFilter(someText, someDepartment));

        // For use with ComboBox and separate department filtering
        ConfigurableFilterDataProvider<Person, String, Department> mixed =
          dataProvider.withConfigurableFilter(
            // Can be shortened as PersonFilter::new
            (filterText, department) -> {
              return new PersonFilter(filterText, department);
            }
          );
        mixed.setFilter(someDepartment);
        // @formatter:on
    }

    public void refreshItem() {
        FetchCallback<Person, String> fetchCallback = null;
        CountCallback<Person, String> sizeCallback = null;
        DataProvider<Person, PersonFilter> dataProvider = null;
        PersonService service = null;

        DataProvider<Person, String> allPersonsWithId = new CallbackDataProvider<>(
                fetchCallback, sizeCallback, Person::getId);

        Grid<Person> persons = new Grid<>();
        persons.setDataProvider(allPersonsWithId);
        persons.addColumn(Person::getName).setHeader("Name");

        Button modifyPersonButton = new Button("Modify person", clickEvent -> {
            Person personToChange = allPersonsWithId.fetch(
                    new Query<>(0, 1, Collections.emptyList(), null, null))
                    .findFirst().get();

            personToChange.setName("Changed person");

            Person newInstance = service.save(personToChange);
            dataProvider.refreshItem(newInstance);
        });
    }

    public void sortOrderProvider() {
        Grid<Person> grid = new Grid<>();

        grid.addColumn(person -> person.getName() + " " + person.getLastName())
                .setHeader("Name").setSortOrderProvider(
                        // Sort according to last name, then first name
                        direction -> Stream.of(
                                new QuerySortOrder("lastName", direction),
                                new QuerySortOrder("firstName", direction)));

        // Will be sortable by the user
        // When sorting by this column, the query will have a SortOrder
        // where getSorted() returns "name"
        grid.addColumn(Person::getName).setHeader("Name")
                .setSortProperty("name");

        // Will not be sortable since no sorting info is given
        grid.addColumn(Person::getYearOfBirth).setHeader("Year of birth");
    }

    public void filteringBy() {
        // @formatter:off
        /*
         * not implemented

        ComboBox<Person> comboBox = new ComboBox<>();
        List<Person> persons = null;

        ListDataProvider<Person> dataProvider = DataProvider
                .ofCollection(persons);

        comboBox.setDataProvider(
                dataProvider.filteringBy((person, filterText) -> {
                    if (person.getName().contains(filterText)) {
                        return true;
                    }

                    if (person.getEmail().contains(filterText)) {
                        return true;
                    }

                    return false;
                }));
         */
        // @formatter:on
    }

    public void withConvertedFilter() {
        DataProvider<Person, Set<String>> personProvider = getProvider();

        ComboBox<Person> comboBox = new ComboBox<>();

        DataProvider<Person, String> converted = personProvider
                .withConvertedFilter(
                        filterText -> Collections.singleton(filterText));

        comboBox.setDataProvider(converted);
    }

    private DataProvider<Person, Set<String>> getPersonsProvider() {
        return null;
    }

    private DataProvider<Person, String> getPersonProvider() {
        return null;
    }

    private PersonService getPersonService() {
        return null;
    }

    private DataProvider<Person, Set<String>> getProvider() {
        return null;
    }
}
