package com.vaadin.hummingbird.demo.expensemanager.views;

import com.vaadin.annotations.EventHandler;
import com.vaadin.hummingbird.components.paper.PaperFab;
import com.vaadin.hummingbird.components.vaadin.VaadinGrid;
import com.vaadin.hummingbird.components.vaadin.VaadinGrid.Column;
import com.vaadin.hummingbird.components.vaadin.VaadinGrid.Render;
import com.vaadin.hummingbird.demo.expensemanager.domain.ExpenseService;
import com.vaadin.hummingbird.demo.expensemanager.domain.ExpenseService.Filters;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.router.View;
import com.vaadin.ui.UI;

import elemental.json.JsonArray;

public class ExpensesList extends Div implements View {

    private final OverView overView;
    public final VaadinGrid grid;
    private Filters filters = new Filters();
    private int col, asc;

    public ExpensesList(OverView overView) {
        this.overView = overView;

        addClassName("expenses-list");
        setId("list");

        grid = new VaadinGrid();
        this.add(grid);

        grid.addItemsRequestedListener(e -> {
            JsonArray items = ExpenseService.INSTANCE.toJson(filters, col, asc,
                    0, 500);
            e.callback(items, items.length());
        });

        grid.setColumns(
                new Column().setName("date").setWidth(120).setSortable(true)
                        .setSortDirection("desc"),
                new Column().setName("merchant").setFlex(1).setMinWidth(120)
                        .setSortable(true),

                new Column().setName("total").setWidth(90).setSortable(true)
                        .setRenderer(new Render().setDataJs(
                                "element.textContent = '$ ' + cell.data.toFixed(2)")),

                new Column().setName("status").setWidth(120).setSortable(true)
                        .setRenderer(new Render().setHtml("<span></span>")
                                .setSelector("span").setDataJs(
                                        "element.textContent = cell.data; element.className = cell.data;")),

                new Column().setName("comment").setMinWidth(260).setFlex(2));

        grid.setFrozenColumns(1);
        grid.setId("expenses");

        grid.addSelectedChangedListener(e -> {
            if (e.selected.size() > 0) {
                String id = ExpenseService.INSTANCE.getExpenseIdByPosition(
                        e.selected.get(0), filters, col, asc);
                UI.getCurrent().navigateTo("expense/" + id);
            }
        });

        grid.addSortOrderChangeListener(e -> overView.update(e.column, e.dir));

        PaperFab paperFab = new PaperFab();
        add(paperFab);
        paperFab.setIcon("add");
        paperFab.addClickListener(e -> this.showExpenseEditor());
        ;
        paperFab.setId("add-button");
    }

    @EventHandler
    protected void showExpenseEditor() {
        UI.getCurrent().navigateTo("expense");
    }

    private void delete(int idx) {
        String id = ExpenseService.INSTANCE.getExpenseIdByPosition(idx, filters,
                col, asc);
        ExpenseService.INSTANCE
                .delete(ExpenseService.INSTANCE.getExpense(id).orElse(null));
        overView.update(filters);
    }

    public void update(Filters filters, int col, int asc) {
        this.filters = filters;
        this.col = col;
        this.asc = asc;
        grid.setSize(ExpenseService.INSTANCE.getSize(filters));
        UI.getCurrent().getPage().executeJavaScript("$0.reset()", grid);
    }
}
