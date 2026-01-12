/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.spring.flowsecurity.views;

import jakarta.annotation.security.PermitAll;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.concurrent.Executor;

import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.flowsecurity.SecurityUtils;
import com.vaadin.flow.spring.flowsecurity.service.BankService;

@Route(value = "private", layout = MainView.class)
@PageTitle("Private View")
@PermitAll
public class PrivateView extends VerticalLayout {

    private BankService bankService;
    private Span balanceSpan = new Span();
    private SecurityUtils utils;
    private DelegatingSecurityContextExecutor executor;
    private Registration registration;

    public PrivateView(BankService bankService, SecurityUtils utils,
            Executor executor) {
        this.bankService = bankService;
        this.utils = utils;
        this.executor = new DelegatingSecurityContextExecutor(executor);

        updateBalanceText();
        balanceSpan.setId("balanceText");
        add(balanceSpan);

        Button applyForLoan = new Button("Apply for a loan",
                this::applyForLoan);
        applyForLoan.setId("applyForLoan");
        add(applyForLoan);

        Button applyForHugeLoan = new Button("Apply for a huge loan",
                this::applyForHugeLoanUsingExecutor);
        applyForHugeLoan.setId("applyForHugeLoan");
        add(applyForHugeLoan);

        Button globalRefresh = new Button("Send global refresh event",
                e -> Broadcaster.sendMessage());
        globalRefresh.setId("sendRefresh");
        add(globalRefresh);

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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getUI().setPollInterval(1000);
        registration = Broadcaster.addMessageListener(e -> {
            getUI().get().access(() -> this.updateBalanceText());
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        detachEvent.getUI().setPollInterval(-1);
        registration.remove();
    }

    private void updateBalanceText() {
        String name = utils.getAuthenticatedUserInfo().getFullName();
        BigDecimal balance = bankService.getBalance();
        this.balanceSpan.setText(String.format(
                "Hello %s, your bank account balance is $%s.", name, balance));
    }

    private void applyForLoan(ClickEvent<Button> e) {
        try {
            bankService.applyForLoan();
            updateBalanceText();
        } catch (Exception ex) {
            getUI().get().access(() -> {
                Notification.show("Application failed: " + ex.getMessage());
            });
        }
    }

    private void applyForHugeLoanUsingExecutor(ClickEvent<Button> e) {
        try {
            bankService.applyForHugeLoan();
            updateBalanceText();
        } catch (Exception ex) {
            getUI().get().access(() -> {
                Notification.show("Application failed: " + ex.getMessage());
            });
        }
    }
}
