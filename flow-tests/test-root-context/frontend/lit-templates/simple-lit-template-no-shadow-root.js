/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

import { SimpleLitTemplateShadowRoot } from "./simple-lit-template-shadow-root.js";

export class SimpleLitTemplateNoShadowRoot extends SimpleLitTemplateShadowRoot {
	createRenderRoot() {
		return this;
	}
}
customElements.define("simple-lit-template-no-shadow-root", SimpleLitTemplateNoShadowRoot);
