/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
// Import an element
import { LitElement, html } from 'lit';
import { SimpleLitTemplateShadowRoot } from './MyLitElement.js';
export class MySuperLitElement extends MyLitElement { createRenderRoot() { return this; }} customElements.define('my-super-lit-element', MySuperLitElement);
