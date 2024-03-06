/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
document.body.addEventListener("click", function(e) {
  if (e.target != document.body) {
    // Ignore clicks on other elements
    return;
  }
  var d = document.createElement("div");
  d.innerText = "Click on body, reported by JavaScript click handler";
  d.classList.add("body-click-added");
  document.body.appendChild(d);
});
