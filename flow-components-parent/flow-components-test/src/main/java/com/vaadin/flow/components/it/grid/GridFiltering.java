package com.vaadin.flow.components.it.grid;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.data.provider.CallbackDataProvider;
import com.vaadin.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.flow.components.it.TestView;
import com.vaadin.ui.grid.Grid;
import com.vaadin.ui.textfield.TextField;

public class GridFiltering extends TestView {

    private static final Set<String> DATA = getData();

    public GridFiltering() {
        Grid<String> grid = new Grid<>();

        DataProvider<String, String> dataProvider = new CallbackDataProvider<>(
                query -> findAnyMatching(query.getFilter()).stream(),
                query -> countAnyMatching(query.getFilter()));

        ConfigurableFilterDataProvider<String, Void, String> filteredDataProvider = dataProvider
                .withConfigurableFilter();

        grid.setDataProvider(filteredDataProvider);

        grid.addColumn("Data", item -> item);

        TextField field = new TextField();
        field.addValueChangeListener(
                event -> filteredDataProvider.setFilter(field.getValue()));

        add(field, grid);
    }

    private Collection<String> findAnyMatching(Optional<String> filter) {
        if (filter.isPresent()) {
            return filter(filter).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private int countAnyMatching(Optional<String> filter) {
        if (filter.isPresent()) {
            return (int) filter(filter).count();
        }
        return 0;
    }

    private Stream<String> filter(Optional<String> filter) {
        return DATA.stream().filter(item -> item.contains(filter.get()));
    }

    private static final Set<String> getData() {
        Set<String> set = new LinkedHashSet<>();
        set.add("foo");
        set.add("bar");
        set.add("baz");
        return set;
    }
}