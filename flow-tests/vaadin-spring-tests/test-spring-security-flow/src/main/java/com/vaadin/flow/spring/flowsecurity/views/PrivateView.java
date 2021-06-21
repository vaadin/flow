package com.vaadin.flow.spring.flowsecurity.views;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import javax.annotation.security.PermitAll;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.flowsecurity.SecurityUtils;
import com.vaadin.flow.spring.flowsecurity.service.BankService;

@Route(value = "private", layout = MainView.class)
@PageTitle("Private View")
@PermitAll
public class PrivateView extends VerticalLayout {

    private BankService bankService;
    private Span balanceSpan = new Span();
    private SecurityUtils utils;

    public PrivateView(BankService bankService, SecurityUtils utils) {
        this.bankService = bankService;
        this.utils = utils;

        updateBalanceText();
        balanceSpan.setId("balanceText");
        add(balanceSpan);
        add(new Button("Apply for a loan", this::applyForLoan));
        Upload upload = new Upload();
        ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
        upload.setReceiver((filename, mimeType) -> {
            return imageStream;
        });
        upload.addSucceededListener(e -> {
            Paragraph p = new Paragraph("Loan application uploaded by "
                    + utils.getAuthenticatedUserInfo().getFullName());
            p.setId("uploadText");
            add(p);
            Image image = new Image(new StreamResource("image.png",
                    () -> new ByteArrayInputStream(imageStream.toByteArray())),
                    "image");
            image.setId("uploadImage");
            add(image);
        });
        add(new H4("Upload your loan application"));
        add(upload);
    }

    private void updateBalanceText() {
        String name = utils.getAuthenticatedUserInfo().getFullName();
        BigDecimal balance = bankService.getBalance();
        this.balanceSpan.setText(String.format(
                "Hello %s, your bank account balance is $%s.", name, balance));

    }

    private void applyForLoan(ClickEvent<Button> e) {
        bankService.applyForLoan();
        updateBalanceText();
    }
}
