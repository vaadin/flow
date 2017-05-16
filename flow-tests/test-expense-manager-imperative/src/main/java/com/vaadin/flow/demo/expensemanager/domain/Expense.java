package com.vaadin.flow.demo.expensemanager.domain;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.commons.beanutils.BeanUtils;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A simple DTO for the expenses demo.
 *
 * Serializable and cloneable Java Object that are typically persisted in the
 * database and can also be easily converted to different formats like JSON.
 */
public final class Expense implements Serializable {

    private LocalDate date = LocalDate.now();
    private Integer id;
    private String merchant = "";
    private Double total = 0d;
    private String status = "";
    private String comment = "";
    private String receiptUrl = "";

    public final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public final static DecimalFormat numFormat = new DecimalFormat("#,###.00");

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate dstr) {
        this.date = dstr;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public Double getTotal() {
        // TODO: remove this when [value]="expense.total.toFixed(2)" works in templates
        return Double.valueOf(numFormat.format(total));
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }

    public Expense copy() {
        try {
            return (Expense) BeanUtils.cloneBean(this);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Unable to clone the instance", e);
        }
    }

    @Override
    public String toString() {
        return toJson().toJson();
    }

    public JsonValue toJson() {
        JsonObject o = Json.instance().createObject();
        o.put("id", id);
        o.put("merchant", merchant);
        o.put("total", total);
        o.put("status", status);
        o.put("comment", comment);
        o.put("date", getDate().format(formatter));
        return o;
    }

    public String toString2() {
        return "Expense [date=" + getDate() + ", id=" + id + ", merchant=" + merchant + ", total=" + total
                + ", status=" + status + ", comment=" + comment + "]";
    }

}
