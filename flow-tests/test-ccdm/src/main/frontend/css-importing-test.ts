import { LitElement } from 'lit';

import '@vaadin/login';
import styles from './test-styles.css?inline';

// Regression test for flow#9167 (`styles` assignment will cause a type
// error if `styles` imported from `./test-styles.css` is not a
// CSSResultGroup
export class CSSImportingTest extends LitElement {
  static styles = styles;
}
