package com.vaadin.flow.demo.expensemanager.views;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Title;
import com.vaadin.flow.components.paper.PaperButton;
import com.vaadin.flow.components.paper.PaperIconButton;
import com.vaadin.flow.components.paper.PaperInput;
import com.vaadin.flow.components.paper.PaperTextarea;
import com.vaadin.flow.components.vaadin.VaadinComboBox;
import com.vaadin.flow.components.vaadin.VaadinDatePicker;
import com.vaadin.flow.components.vaadin.VaadinUpload;
import com.vaadin.flow.demo.expensemanager.domain.Expense;
import com.vaadin.flow.demo.expensemanager.domain.ExpenseService;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.html.H2;
import com.vaadin.flow.html.Image;
import com.vaadin.flow.html.Span;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.View;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

import elemental.json.Json;
import elemental.json.JsonArray;

@Title("EditView")
@HtmlImport("bower_components/expense-manager/src/elements.html")
@StyleSheet("edit-view.css")
public class EditView extends Div implements View {

    Expense original;
    Expense current;
    private H2 caption;
    private VaadinComboBox merchant;
    private PaperInput total;
    private VaadinDatePicker date;
    private PaperTextarea comment;
    private VaadinUpload uploader;
    private Span error;
    private Image image;

    public EditView() {
        addClassName("edit-view");
        setId("edit");

        Div div1 = new Div();
        add(div1);
        div1.setId("dialog");

        Div div2 = new Div();
        div1.add(div2);
        div2.addClassName("main-layout");

        caption = new H2();
        div2.add(caption);

        Span span = new Span();
        div2.add(span);
        span.setText("");
        span.addClassName("flex");

        PaperIconButton paperIconButton = new PaperIconButton();
        div2.add(paperIconButton);
        paperIconButton.setText("");
        paperIconButton.setIcon("close");
        paperIconButton.addClickListener(e -> this.close());
        paperIconButton.addClassName("close-button");
        paperIconButton.addClassName("self-start");

        Div div3 = new Div();
        div1.add(div3);
        div3.addClassName("wrapper");

        Div form = new Div();
        div3.add(form);
        form.addClassName("form");
        form.setId("form");

        merchant = new VaadinComboBox();
        form.add(merchant);
        merchant.setId("merchant");
        merchant.setName("merchant");
        merchant.setLabel("Merchant");
        merchant.setAllowCustomValue(true);
        merchant.setRequired(true);
        merchant.setItems(ExpenseService.merchants);

        total = new PaperInput();
        form.add(total);
        total.setId("total");
        total.setName("total");
        total.setLabel("Total");

        Div div5 = new Div();
        total.add(div5);
        div5.setText("$");
        div5.getElement().setAttribute("prefix", true);

        date = new VaadinDatePicker();
        form.add(date);
        date.setText("");
        date.setId("date");
        date.setName("date");
        date.setLabel("Date");
        date.setRequired(true);

        comment = new PaperTextarea();
        form.add(comment);
        comment.setId("comment");
        comment.setName("comment");
        comment.setLabel("Comment");

        uploader = new VaadinUpload();
        form.add(uploader);
        uploader.addUploadSuccessListener(e -> uploadSuccess());
        uploader.setId("receipt");
        uploader.setTarget("upload");
        uploader.setAccept("image/*");
        uploader.setMaxFiles(1);

        Div div6 = new Div();
        uploader.add(div6);
        div6.addClassName("file-list");

        Div div7 = new Div();
        div6.add(div7);
        div7.addClassName("receipt-wrapper");

        image = new Image();
        div7.add(image);
        image.setAlt("");

        Div div8 = new Div();
        div1.add(div8);
        div8.addClassName("buttons-layout");

        PaperButton paperButton = new PaperButton();
        div8.add(paperButton);
        paperButton.setText("Save");
        paperButton.setRaised(true);
        paperButton.addClickListener(e -> save());
        paperButton.addClassName("save-button");

        PaperButton paperButton1 = new PaperButton();
        div8.add(paperButton1);
        paperButton1.setText("Cancel");
        paperButton1.addClickListener(e -> close());
        paperButton1.addClassName("cancel-button");

        PaperButton paperButton2 = new PaperButton();
        div8.add(paperButton2);
        paperButton2.setText("Delete");
        paperButton2.addClickListener(e -> delete());
        paperButton2.setId("delete");
        paperButton2.addClassName("delete-button");

        error = new Span();
        div1.add(error);
        error.setId("error");
    }

    JsonArray toJsArray(Object... list) {
        JsonArray r = Json.instance().createArray();
        for (Object o : list) {
            r.set(r.length(), o.toString());
        }
        return r;
    }

    @Override
    public void onLocationChange(LocationChangeEvent locationChangeEvent) {
        Optional<Expense> expense = ExpenseService.INSTANCE
                .getExpense(locationChangeEvent.getPathParameter("id"));
        current = expense.orElse(new Expense());
        if (!expense.isPresent()) {
            current.setStatus("New");
            caption.setText("New Expense");
            image.setSrc("");
        } else {
            caption.setText("Edit Expense");
            merchant.setValue(current.getMerchant());
            total.setValue(current.getTotal().toString());
            date.setValue(current.getDate().format(Expense.formatter));
            comment.setValue(current.getComment());
            image.setSrc(current.getReceiptUrl());
        }
    }

    @EventHandler
    protected void save() {
        error.setText(null);

        current.setComment(comment.getValue());

        if (merchant.getValue() == null) {
            error.setText("Please set a merchant");
            return;
        }
        current.setMerchant(merchant.getValue());

        String stringValue = total.getValue();
        if (stringValue == null || stringValue.isEmpty()) {
            stringValue = "0";
        }
        Double value = Double.parseDouble(stringValue);
        if (value < 1) {
            error.setText("Please set a total");
            return;
        }
        current.setTotal(value);

        current.setDate(LocalDate.parse(date.getValue()));
        if (date.getValue() == null) {
            error.setText("Please set a date");
            return;
        }

        if (image.getSrc() != null) {
            current.setReceiptUrl(image.getSrc());
        }

        ExpenseService.INSTANCE.save(current);

        VaadinSession.getCurrent().getSession()
                .removeAttribute("receipt-upload");
        UI.getCurrent().navigateTo("overview");
    }

    @EventHandler
    protected void delete() {
        ExpenseService.INSTANCE.delete(current);
        UI.getCurrent().navigateTo("overview");
    }

    @EventHandler
    protected void close() {
        UI.getCurrent().navigateTo("overview");
    }

    @EventHandler
    protected void uploadSuccess() {
        Object o = VaadinSession.getCurrent().getSession()
                .getAttribute("receipt-upload");
        if (o != null) {
            image.setSrc(String.valueOf(o));
        }
    }

}
