package com.vaadin.flow.testutil;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.vaadin.testbench.ElementQuery;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

@Element("vaadin-devmode-gizmo")
public class DevModeGizmoElement extends TestBenchElement {

    private List<TestBenchElement> getLogDivs(boolean onlyError) {
        ElementQuery<TestBenchElement> divs = getLogDivsQuery(onlyError);
        return divs.all();
    }

    private ElementQuery<TestBenchElement> getLogDivsQuery(boolean onlyError) {
        ElementQuery<TestBenchElement> divs = $("div")
                .attributeContains("class", "message");
        if (onlyError) {
            divs = divs.attributeContains("class", "error");
        }
        return divs;
    }

    public List<String> getLogRows() {
        return getLogDivs(false).stream().map(div -> div.getText())
                .collect(Collectors.toList());
    }

    public List<String> getErrorLogRows() {
        return getLogDivs(true).stream().map(div -> div.getText())
                .collect(Collectors.toList());
    }

    public String getFirstErrorLogRow() {
        return getLogDivsQuery(true).first().getText();
    }

    public String getLastErrorLogRow() {
        return getLogDivsQuery(true).last().getText();
    }

    public void waitForErrorMessage(Predicate<String> matcher) {
        waitUntil(driver -> {
            return getErrorLogRows().stream().anyMatch(matcher);
        });
    }

    public void waitForLastErrorMessageToMatch(Predicate<String> matcher) {
        waitUntil(driver -> {
            return matcher.test(getLastErrorLogRow());
        });

    }

    public boolean isExpanded() {
        return getPropertyBoolean("expanded");
    }

    public int getNumberOfLogRows() {
        return getLogDivsQuery(false).all().size();
    }

    public int getNumberOfErrorLogRows() {
        return getLogDivsQuery(true).all().size();
    }

}
