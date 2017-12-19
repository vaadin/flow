package com.vaadin.flow.tutorial.polymer;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.vaadin.flow.templatemodel.ModelConverter;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("polymer-templates/tutorial-template-model-converters.asciidoc")
public class DateToDateBeanConverter implements ModelConverter<Date, DateBean> {

    @Override
    public DateBean toPresentation(Date modelValue) {
        if (modelValue == null) {
            return null;
        }
        DateBean bean = new DateBean();
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(modelValue);
        bean.setDay(Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)));
        bean.setMonth(Integer.toString(calendar.get(Calendar.MONTH) + 1));
        bean.setYear(Integer.toString(calendar.get(Calendar.YEAR)));
        return bean;
    }

    @Override
    public Date toModel(DateBean presentationValue) {
        if (presentationValue == null) {
            return null;
        }
        int year = Integer.parseInt(presentationValue.getYear());
        int day = Integer.parseInt(presentationValue.getDay());
        int month = Integer.parseInt(presentationValue.getMonth()) - 1;
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.set(year, month, day);
        return calendar.getTime();
    }

}
