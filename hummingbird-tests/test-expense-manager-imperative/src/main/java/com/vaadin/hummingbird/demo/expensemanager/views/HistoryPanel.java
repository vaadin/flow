package com.vaadin.hummingbird.demo.expensemanager.views;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Id;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Title;
import com.vaadin.hummingbird.components.vaadin.charts.VaadinChart.DataSeries;
import com.vaadin.hummingbird.demo.expensemanager.domain.ExpenseService;
import com.vaadin.hummingbird.demo.expensemanager.domain.ExpenseService.Filters;
import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.ui.Template;

import elemental.json.JsonArray;

@Title("OverView")
@HtmlImport("bower_components/expense-manager/src/elements.html")
@StyleSheet("over-view.css")
public class HistoryPanel extends Template implements View {

    public interface HistoryBean extends TemplateModel {
        void setChartCategories(String s);
    }

    protected HistoryBean getModel() {
        return (HistoryBean)super.getModel();
    }

    @Id("barchart-data")
    private DataSeries barChartData;
    @Id("columnchart-data")
    private DataSeries columnChartData;

    public void update() {
    }

    public void update(Filters filters) {
        StringBuilder categories = new StringBuilder();
        JsonArray dataArray = ExpenseService.INSTANCE.computeChartData(categories, filters);
        getModel().setChartCategories(categories.toString());
        barChartData.getElement().setPropertyJson("data", dataArray);
        columnChartData.getElement().setPropertyJson("data", dataArray);
    }

// TODO: make this panel use declarative language.
//    this.getElement().setProperty("id", "history-panel");
//    add(new H2("Last 12 Months in Total"));
//
//    VaadinChart barChart = new VaadinBarChart();
//
//    DataSeries series = new DataSeries("My data");
//    for (int i : Arrays.asList(1, 2, 1, 3)) {
//        series.addItem(i);
//    }
//    barChart.addSeries(series);
//
//    DataSeries series2 = new DataSeries("My data 2");
//    for (int i : Arrays.asList(3, 1, 2, 1)) {
//        series2.addItem(i);
//    }
//    barChart.addSeries(series2);
//
//    XAxis x = new XAxis();
//    x.setCategories("Africa", "America", "Asia", "Europe", "Oceania");
//    x.setTitle((String) null);
//    barChart.addxAxis(x);
//
//    add(barChart);
}
