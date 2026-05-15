/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.flowsecurity.SecurityUtils;
import com.vaadin.flow.spring.flowsecurity.service.BankService;

@Route(value = "private", layout = MainView.class)
@PageTitle("Private View")
@PermitAll
@Menu(order = 1.5)
public class PrivateView extends Div {

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

        getStyle().set("display", "flex").set("flex-direction", "column");

        updateBalanceText();
        balanceSpan.setId("balanceText");
        add(balanceSpan);
        add(new NativeButton("Apply for a loan", this::applyForLoan));
        add(new NativeButton("Apply for a huge loan",
                this::applyForHugeLoanUsingExecutor));

        NativeButton globalRefresh = new NativeButton(
                "Send global refresh event", e -> Broadcaster.sendMessage());
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

    private void applyForLoan(ClickEvent<NativeButton> e) {
        bankService.applyForLoan();
        updateBalanceText();
    }

    private void applyForHugeLoanUsingExecutor(ClickEvent<NativeButton> e) {
        Div waitDialog = createModal("Processing loan application...");
        waitDialog.setId("waitDialog");
        add(waitDialog);
        UI ui = getUI().get();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    bankService.applyForHugeLoan();
                } catch (Exception e) {
                    getUI().get().access(() -> {
                        Div notification = new Div(
                                "Application failed: " + e.getMessage());
                        notification.addClassName("notification");
                        add(notification);
                    });

                }
                ui.access(() -> {
                    updateBalanceText();
                    remove(waitDialog);
                });
            }
        };
        executor.execute(runnable);
    }

    private static Div createModal(String text) {
        Div backdrop = new Div();
        backdrop.getStyle().set("position", "fixed").set("inset", "0")
                .set("background", "rgba(0, 0, 0, 0.4)").set("display", "flex")
                .set("align-items", "center").set("justify-content", "center")
                .set("z-index", "1000");
        Div panel = new Div(new Span(text));
        panel.getStyle().set("background", "white").set("padding", "1rem")
                .set("border-radius", "0.5rem");
        backdrop.add(panel);
        return backdrop;
    }
}
