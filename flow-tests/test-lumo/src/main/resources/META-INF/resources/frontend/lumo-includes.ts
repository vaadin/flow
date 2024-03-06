/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { color } from '@vaadin/vaadin-lumo-styles';
import { typography } from '@vaadin/vaadin-lumo-styles';

const tpl = document.createElement('template');
tpl.innerHTML = `<style>
  ${color.cssText}
  ${typography.cssText}
</style>`;
document.head.appendChild(tpl.content);
