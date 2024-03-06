/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { LitElement } from 'lit';

import '@vaadin/vaadin-login/vaadin-login-overlay';
import styles from './test-styles.css';

// Regression test for flow#9167 (`styles` assignment will cause a type
// error if `styles` imported from `./test-styles.css` is not a
// CSSResultGroup
export class CSSImportingTest extends LitElement {

    static styles = styles;

}
