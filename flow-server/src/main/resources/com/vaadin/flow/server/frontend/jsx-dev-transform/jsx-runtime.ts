/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import {
  Fragment as reactFragment,
  jsx as reactJsx,
  jsxs as reactJsxs,
} from "react/jsx-runtime";

export const Fragment = reactFragment;
export const jsx = reactJsx;
export const jsxs = reactJsxs;

throw new Error(
  "Do not use this transform for production builds. It is only meant for development."
);
