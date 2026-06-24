/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { JSXSource, Fragment as reactFragment, jsxDEV as reactJsxDEV } from 'react/jsx-dev-runtime';

export const Fragment = reactFragment;

export function jsxDEV(
  type: React.ElementType,
  props: unknown,
  key: React.Key | undefined,
  isStatic: boolean,
  source?: JSXSource,
  self?: unknown
): React.ReactElement {
  const realFreeze = Object.freeze;
  try {
    (Object as any).freeze = undefined; // prevent React from freezing the element

    const reactElement: any = reactJsxDEV(type, props, key, isStatic, source, self);
    if (source && !reactElement._source) {
      // When running with React 19, put the source information on the _debugInfo array, 
      // which will be transferred to the fiber node by React
      reactElement._debugInfo ??= [];
      reactElement._debugInfo.source = source;
    }
    realFreeze(reactElement);
    return reactElement;
  } finally {
    (Object as any).freeze = realFreeze;
  }
}
