/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

let res;
const promise = new Promise((resolve, reject) => {
    res = resolve;
});

setTimeout(() => {
    window.othervalue = "This is the value set in other.js";
    res();
}, 500);


await promise;
