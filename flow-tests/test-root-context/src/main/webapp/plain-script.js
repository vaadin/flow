/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
var div = document.createElement("div");
div.id="added-from-src-script";
div.textContent="Hello from src script";
document.body.insertBefore(div, document.body.firstChild);
