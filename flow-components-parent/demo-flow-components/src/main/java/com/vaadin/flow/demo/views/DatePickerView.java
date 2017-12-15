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
package com.vaadin.flow.demo.views;

import java.time.LocalDate;
import java.util.Arrays;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.datepicker.DatePicker;
import com.vaadin.ui.datepicker.DatePicker.DatePickerI18n;

/**
 * View for {@link DatePicker} demo.
 */
@Route(value = "vaadin-date-picker", layout = MainLayout.class)
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-date-picker.html")
@ComponentDemo(name = "Date Picker")
public class DatePickerView extends DemoView {

    @Override
    protected void initView() {
        createSimpleDatePicker();
        createMinAndMaxDatePicker();
        createFinnishDatePicker();
        createStartAndEndDatePickers();
    }

    private void createSimpleDatePicker() {
        Div message = createMessageDiv("simple-picker-message");

        // begin-source-example
        // source-example-heading: Simple date picker
        DatePicker datePicker = new DatePicker();

        datePicker.addValueChangeListener(event -> {
            LocalDate selectedDate = event.getValue();
            if (selectedDate != null) {
                message.setText("Day: " + selectedDate.getDayOfMonth()
                        + "\nMonth: " + selectedDate.getMonthValue()
                        + "\nYear: " + selectedDate.getYear());
            } else {
                message.setText("No date is selected");
            }
        });
        // end-source-example

        datePicker.setId("simple-picker");
        addCard("Simple date picker", datePicker, message);
    }

    private void createMinAndMaxDatePicker() {
        Div message = createMessageDiv("min-and-max-picker-message");

        // begin-source-example
        // source-example-heading: Date picker with min and max
        DatePicker datePicker = new DatePicker();
        datePicker.setLabel("Select a day within this month");
        datePicker.setPlaceholder("Date within this month");

        LocalDate now = LocalDate.now();

        datePicker.setMin(now.withDayOfMonth(1));
        datePicker.setMax(now.withDayOfMonth(now.lengthOfMonth()));

        datePicker.addValueChangeListener(event -> {
            LocalDate selectedDate = event.getValue();
            if (selectedDate != null) {
                message.setText("Day: " + selectedDate.getDayOfMonth()
                        + "\nMonth: " + selectedDate.getMonthValue()
                        + "\nYear: " + selectedDate.getYear());
            } else {
                message.setText("No date is selected");
            }
        });
        // end-source-example

        datePicker.setId("min-and-max-picker");
        addCard("Date picker with min and max", datePicker, message);
    }

    private void createFinnishDatePicker() {
        Div message = createMessageDiv("finnish-picker-message");

        // begin-source-example
        // source-example-heading: Internationalized date picker
        DatePicker datePicker = new DatePicker();
        datePicker.setLabel("Finnish date picker");
        datePicker.setPlaceholder("Syntymäpäivä");

        datePicker.setI18n(new DatePickerI18n().setWeek("viikko")
                .setCalendar("kalenteri").setClear("tyhjennä")
                .setToday("tänään").setCancel("peruuta").setFirstDayOfWeek(1)
                .setMonthNames(Arrays.asList("tammiku", "helmikuu", "maaliskuu",
                        "huhtikuu", "toukokuu", "kesäkuu", "heinäkuu", "elokuu",
                        "syyskuu", "lokakuu", "marraskuu", "joulukuu"))
                .setWeekdays(Arrays.asList("sunnuntai", "maanantai", "tiistai",
                        "keskiviikko", "torstai", "perjantai", "lauantai"))
                .setWeekdaysShort(Arrays.asList("su", "ma", "ti", "ke", "to",
                        "pe", "la")));

        datePicker.addValueChangeListener(event -> {
            LocalDate selectedDate = event.getValue();
            if (selectedDate != null) {
                int weekday = selectedDate.getDayOfWeek().getValue() % 7;
                String weekdayName = datePicker.getI18n().getWeekdays()
                        .get(weekday);

                int month = selectedDate.getMonthValue() - 1;
                String monthName = datePicker.getI18n().getMonthNames()
                        .get(month);

                message.setText("Day of week: " + weekdayName + "\nMonth: "
                        + monthName);
            } else {
                message.setText("No date is selected");
            }
        });
        // end-source-example

        datePicker.setId("finnish-picker");
        addCard("Internationalized date picker", datePicker, message);
    }

    private void createStartAndEndDatePickers() {
        Div message = createMessageDiv("start-and-end-message");

        // begin-source-example
        // source-example-heading: Two linked date pickers
        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setLabel("Start");
        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setLabel("End");

        startDatePicker.addValueChangeListener(event -> {
            LocalDate selectedDate = event.getValue();
            LocalDate endDate = endDatePicker.getValue();
            if (selectedDate != null) {
                endDatePicker.setMin(selectedDate.plusDays(1));
                if (endDate == null) {
                    endDatePicker.open();
                    message.setText("Select the ending date");
                } else {
                    message.setText(
                            "Selected period:\nFrom " + selectedDate.toString()
                                    + " to " + endDate.toString());
                }
            } else {
                endDatePicker.setMin(null);
                message.setText("Select the starting date");
            }
        });

        endDatePicker.addValueChangeListener(event -> {
            LocalDate selectedDate = event.getValue();
            LocalDate startDate = startDatePicker.getValue();
            if (selectedDate != null) {
                startDatePicker.setMax(selectedDate.minusDays(1));
                if (startDate != null) {
                    message.setText(
                            "Selected period:\nFrom " + startDate.toString()
                                    + " to " + selectedDate.toString());
                } else {
                    message.setText("Select the starting date");
                }
            } else {
                startDatePicker.setMax(null);
                if (startDate != null) {
                    message.setText("Select the ending date");
                } else {
                    message.setText("No date is selected");
                }
            }
        });
        // end-source-example

        startDatePicker.setId("start-picker");
        endDatePicker.setId("end-picker");
        addCard("Two linked date pickers", startDatePicker, endDatePicker,
                message);

    }

    private Div createMessageDiv(String id) {
        Div message = new Div();
        message.setId(id);
        message.getStyle().set("whiteSpace", "pre");
        return message;
    }

}
