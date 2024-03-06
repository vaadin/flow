/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
window.logMessage = function (msg) {
    var d = document.createElement("div");
    d.innerText = msg;
    d.classList.add("message");
    document.body.appendChild(d);
};
logMessage("Messagehandler initialized in module 1");
