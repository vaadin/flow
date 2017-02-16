package com.vaadin.hummingbird.demo.expensemanager.views;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Title;
import com.vaadin.hummingbird.components.iron.IronIcon;
import com.vaadin.hummingbird.components.paper.PaperButton;
import com.vaadin.hummingbird.components.paper.PaperHeaderPanel;
import com.vaadin.hummingbird.components.paper.PaperToolbar;
import com.vaadin.hummingbird.demo.expensemanager.domain.ExpenseService;
import com.vaadin.hummingbird.demo.expensemanager.domain.ExpenseService.Filters;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.H1;
import com.vaadin.hummingbird.html.Span;
import com.vaadin.hummingbird.router.LocationChangeEvent;
import com.vaadin.hummingbird.router.View;
import com.vaadin.ui.UI;

@Title("OverView")
@HtmlImport("bower_components/expense-manager/src/elements.html")
@StyleSheet("over-view.css")
public class OverView extends Div implements View {

    private FiltersToolBar filtersToolBar;
    private ExpensesList expensesList;
    private HistoryPanel historyPanel;

    private Filters filters = new Filters();
    private int col = 0;
    private int asc = -1;

    private void update() {
        historyPanel.update(filters);
        filtersToolBar.setSummary(ExpenseService.INSTANCE.getTotal(filters),
                ExpenseService.INSTANCE.getSize(filters));
        expensesList.update(filters, col, asc);
    }

    public void update(Filters filters) {
        this.filters = filters;
        update();
    }

    public void update(int col, int asc) {
        this.col = col;
        this.asc = asc;
        update();
    }

    @Override
    public void onLocationChange(LocationChangeEvent locationChangeEvent) {
        update();
    }

    public OverView() {
        addClassName("overview-page");
        addClassName("fit");
        addClassName("flex");

        PaperHeaderPanel paperHeaderPanel = new PaperHeaderPanel();
        add(paperHeaderPanel);

        PaperToolbar paperToolbar = new PaperToolbar();
        paperHeaderPanel.add(paperToolbar);
        paperHeaderPanel.add(paperToolbar);

        H1 h1 = new H1("Expense Manager");
        paperToolbar.add(h1);

        IronIcon ironIcon = new IronIcon();
        paperToolbar.add(ironIcon);
        ironIcon.setId("sync");
        ironIcon.setIcon("notification:sync");
        ironIcon.setHidden(true);
        ironIcon.setTitle("Syncing…");

        Span span = new Span();
        paperToolbar.add(span);
        span.addClassName("flex");

        PaperButton paperButton = new PaperButton();
        paperToolbar.add(paperButton);
        paperButton.setText("Info");
        paperButton.addClickListener(e -> this.openInfoWindow());
        paperButton.addClassName("about-button");

        Div div1 = new Div();
        paperHeaderPanel.add(div1);
        div1.addClassName("content");

        filtersToolBar = new FiltersToolBar(this);
        div1.add(filtersToolBar);

        Div div2 = new Div();
        div1.add(div2);
        div2.addClassName("content-panel");

        expensesList = new ExpensesList(this);
        div2.add(expensesList);

        historyPanel = new HistoryPanel();
        div2.add(historyPanel);
    }

    private void openInfoWindow() {
        UI.getCurrent().navigateTo("info");
    }

    protected void logout() {
        UI.getCurrent().navigateTo("login");
    }

}
