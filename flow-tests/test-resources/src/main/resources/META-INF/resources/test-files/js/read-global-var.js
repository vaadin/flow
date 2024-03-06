/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
var log = document.createElement("div");
log.id = "read-global-var-text";
log.textContent = "Second script loaded. Global variable (window.globalVar) is: '" + window.globalVar+"'";
document.body.insertBefore(log, null);
