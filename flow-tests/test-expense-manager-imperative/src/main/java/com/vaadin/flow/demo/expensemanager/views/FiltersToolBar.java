package com.vaadin.flow.demo.expensemanager.views;

import java.time.LocalDate;

import com.vaadin.annotations.EventHandler;
import com.vaadin.flow.components.paper.PaperButton;
import com.vaadin.flow.components.paper.PaperCheckbox;
import com.vaadin.flow.components.paper.PaperIconButton;
import com.vaadin.flow.components.paper.PaperInput;
import com.vaadin.flow.components.vaadin.VaadinComboBox;
import com.vaadin.flow.components.vaadin.VaadinDatePicker;
import com.vaadin.flow.demo.expensemanager.domain.Expense;
import com.vaadin.flow.demo.expensemanager.domain.ExpenseService;
import com.vaadin.flow.demo.expensemanager.domain.ExpenseService.Filters;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.html.Span;
import com.vaadin.flow.router.View;

public class FiltersToolBar extends Div implements View {

    private Span owed;
    private Span count;
    private OverView overViewComponent;
    private boolean opened = false;

    public Filters filters = new Filters();

    private VaadinDatePicker dateFrom;
    private VaadinDatePicker dateTo;
    private PaperInput inputMin;
    private PaperInput inputMax;
    private VaadinComboBox vaadinComboBox;
    private PaperCheckbox checkNew;
    private PaperCheckbox checkProgress;
    private PaperCheckbox checkReimb;
    private Div badge;
    private Div filtersContainer;

    private void updateUI() {
        overViewComponent.update(filters);
        badge.setText("" + filters.count());
    }

    public void setSummary(double d, int c) {
        owed.setText("$" + Expense.numFormat.format(d));
        count.setText("#" + c);
    }

    public FiltersToolBar(OverView overView) {
        overViewComponent = overView;

        addClassName("filters-toolbar");

        filtersContainer = new Div();
        add(filtersContainer);
        filtersContainer.setId("search-filters");

        Div div2 = new Div();
        filtersContainer.add(div2);
        div2.addClassName("filters");

        Div div3 = new Div();
        div2.add(div3);
        div3.setText("Filter Expenses");
        div3.addClassName("title");

        Div div4 = new Div();
        div2.add(div4);
        div4.addClassName("row");

        Div div5 = new Div();
        div4.add(div5);
        div5.addClassName("date");
        div5.addClassName("col");

        dateFrom = new VaadinDatePicker();
        div5.add(dateFrom);
        dateFrom.addValueChangedListener(e -> {
            filters.from = LocalDate.parse(e.value);
            updateUI();
        });
        dateFrom.setId("from");
        dateFrom.setAutoValidate(true);
        dateFrom.setLabel("From");
        clear(dateFrom);

        Span span = new Span();
        div5.add(span);
        span.setText("–");

        dateTo = new VaadinDatePicker();
        div5.add(dateTo);
        dateTo.addValueChangedListener(e -> {
            filters.to = LocalDate.parse(e.value);
            updateUI();
        });
        dateTo.setId("to");
        dateTo.setAutoValidate(true);
        dateTo.setLabel("To");
        clear(dateTo);

        Div div6 = new Div();
        div2.add(div6);
        div6.addClassName("row");

        Div div7 = new Div();
        div6.add(div7);
        div7.addClassName("total");
        div7.addClassName("col");

        inputMin = new PaperInput();
        div7.add(inputMin);
        inputMin.addValueChangedListener(e -> {
            filters.min = Double.valueOf(inputMin.getValue());
            updateUI();
        });

        inputMin.setId("min");
        inputMin.setLabel("Min");
        clear(inputMin);
        inputMin.setType("number");
        inputMin.setRequired(true);
        inputMin.setStep("any");

        Span span1 = new Span();
        div7.add(span1);
        span1.setText("–");

        inputMax = new PaperInput();
        div7.add(inputMax);
        inputMax.addValueChangedListener(e -> {
            filters.max = Double.valueOf(inputMax.getValue());
            updateUI();
        });
        inputMax.setId("max");
        inputMax.setLabel("Max");
        clear(inputMax);
        inputMax.setType("number");
        inputMax.setRequired(true);
        inputMax.setStep("any");

        Div div10 = new Div();
        div2.add(div10);
        div10.setText("");
        div10.addClassName("row");

        Div div11 = new Div();
        div10.add(div11);
        div11.addClassName("merchants");
        div11.addClassName("col");

        vaadinComboBox = new VaadinComboBox();
        div11.add(vaadinComboBox);
        vaadinComboBox.addValueChangedListener(e -> {
            filters.merch = e.value;
            updateUI();
        });
        vaadinComboBox.setLabel("Merchant");
        vaadinComboBox.setValue("");
        vaadinComboBox.setId("merchant");
        vaadinComboBox.setItems(ExpenseService.merchants);

        Div div12 = new Div();
        div2.add(div12);
        div12.addClassName("row");

        Div div13 = new Div();
        div12.add(div13);
        div13.addClassName("status");
        div13.addClassName("col");

        Span span2 = new Span();
        div13.add(span2);
        span2.setText("Status");
        span2.addClassName("caption");

        Div div14 = new Div();
        div13.add(div14);
        div14.addClassName("checkboxes");

        checkNew = new PaperCheckbox();
        div14.add(checkNew);
        checkNew.setText("New");
        checkNew.addCheckedChangedListener(e -> {
            filters.neW = e.checked;
            updateUI();
        });
        checkNew.setId("new");
        checkNew.setName("new");

        checkProgress = new PaperCheckbox();
        div14.add(checkProgress);
        checkProgress.setText("In Progress");
        checkProgress.addCheckedChangedListener(e -> {
            filters.prog = e.checked;
            updateUI();
        });
        checkProgress.setId("progress");
        checkProgress.setName("progress");

        checkReimb = new PaperCheckbox();
        div14.add(checkReimb);
        checkReimb.setText("Reimbursed");
        checkReimb.addCheckedChangedListener(e -> {
            filters.reim = e.checked;
            updateUI();
        });
        checkReimb.setId("reimbursed");
        checkReimb.setName("reimbursed");

        Div div15 = new Div();
        filtersContainer.add(div15);
        div15.setId("footer");

        Div div16 = new Div();
        div15.add(div16);
        div16.setId("buttons");

        PaperButton paperButton = new PaperButton();
        div16.add(paperButton);
        paperButton.setText("Clear Filters");
        paperButton.setId("clear-button");
        paperButton.addClickListener(e -> this.clearFilters());

        PaperButton paperButton1 = new PaperButton();
        div16.add(paperButton1);
        paperButton1.setText("Done");
        paperButton1.setId("done-button");
        paperButton1.addClickListener(e -> this.hideFilters());
        paperButton1.setRaised(true);

        Div div17 = new Div();
        add(div17);
        div17.setId("toolbar");

        Div div18 = new Div();
        div17.add(div18);
        div18.setId("total");

        Span span3 = new Span();
        div18.add(span3);
        span3.setText("To be reimbursed");

        owed = new Span();
        div18.add(owed);
        owed.addClassName("sum");
        count = new Span();
        div18.add(count);
        count.addClassName("sum");

        Div div19 = new Div();
        div17.add(div19);
        div19.setId("filters-toggle");
        div19.addClickListener(e -> this.toggleFilters());

        Span span5 = new Span();
        div19.add(span5);
        span5.setText("Filters");

        badge = new Div();
        div19.add(badge);
        badge.setText("0");
        badge.addClassName("count");

        PaperIconButton paperIconButton = new PaperIconButton();
        div19.add(paperIconButton);
        paperIconButton.setIcon("filter-list");
        paperIconButton.addClickListener(e -> openFilters());
    }

    private void openFilters() {
    }

    private void toggleFilters() {
        filtersContainer.getStyle().set("maxHeight",
                ((opened = !opened) ? "400" : "4") + "px");
    }

    private void hideFilters() {
    }

    private void clear(VaadinDatePicker picker) {
        picker.setValue("");
    }

    private void clear(PaperInput input) {
        input.setValue("");
    }

    @EventHandler
    protected void clearFilters() {
        dateFrom.setValue("");
        dateTo.setValue("");
        inputMin.setValue("");
        inputMax.setValue("");
        vaadinComboBox.setValue("");
        checkNew.setChecked(false);
        checkProgress.setChecked(false);
        checkReimb.setChecked(false);
        filters = new Filters();
        updateUI();
    }
}
