/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import './toplevelawait-other.js';

window.topLevelAwaitValue = window.othervalue;
console.log('The value set in other is: ' + window.topLevelAwaitValue);
