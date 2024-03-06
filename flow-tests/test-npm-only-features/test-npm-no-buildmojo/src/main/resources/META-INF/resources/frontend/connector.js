/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
window.Vaadin.Flow.connector = {
  initLazy: function() {
    let htmlDivElement = document.createElement("div");
    htmlDivElement.setAttribute("id", "lazy-element");
    htmlDivElement.innerHTML = "I is the Lazy Element!";
    document.body.appendChild(htmlDivElement);
  }
};
