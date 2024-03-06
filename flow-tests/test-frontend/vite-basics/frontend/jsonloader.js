/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
window.loadJson = (resultHandler) => {
    import('./my.json').then(result => resultHandler(JSON.stringify(result.default)));
}
