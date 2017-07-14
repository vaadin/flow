package com.vaadin.flow.tutorial.polymer;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.vaadin.flow.template.model.ModelConverter;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("tutorial-template-model-converters.asciidoc")
public class DateToDateBeanConverter implements ModelConverter<Date, DateBean> {

    @Override
    public Class<Date> getApplicationType() {
        return Date.class;
    }

    @Override
    public Class<DateBean> getModelType() {
        return DateBean.class;
    }

    @Override
    public DateBean toModel(Date applicationValue) {
        if (applicationValue == null) {
            return null;
        }
        DateBean bean = new DateBean();
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(applicationValue);
        bean.setDay(Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)));
        bean.setMonth(Integer.toString(calendar.get(Calendar.MONTH) + 1));
        bean.setYear(Integer.toString(calendar.get(Calendar.YEAR)));
        return bean;
    }

    @Override
    public Date toApplication(DateBean modelValue) {
        if (modelValue == null) {
            return null;
        }
        int year = Integer.parseInt(modelValue.getYear());
        int day = Integer.parseInt(modelValue.getDay());
        int month = Integer.parseInt(modelValue.getMonth()) - 1;
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.set(year, month, day);
        return calendar.getTime();
    }

}
