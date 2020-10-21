import { LitElement } from 'lit-element';

import '@vaadin/vaadin-login/vaadin-login-overlay';
import styles from './test-styles.css';

// Regression test for flow#9167 (`styles` assignment will cause a type
// error if `styles` imported from `./test-styles.css` is not CSSResult
export class CSSImportingTest extends LitElement {

    static styles = styles;

}
