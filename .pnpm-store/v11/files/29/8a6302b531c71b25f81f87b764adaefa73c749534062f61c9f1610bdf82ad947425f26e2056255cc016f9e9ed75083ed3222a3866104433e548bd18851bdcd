/**
 * @license
 * Copyright (c) 2017 Anton Korzunov
 * SPDX-License-Identifier: MIT
 */

/**
 * @fileoverview
 *
 * This module includes JS code copied from the `aria-hidden` package:
 * https://github.com/theKashey/aria-hidden/blob/master/src/index.ts
 */

export declare type Undo = () => void;

/**
 * Marks everything except given node(or nodes) as aria-hidden
 */
export declare const hideOthers: (
  originalTarget: Element | Element[],
  parentNode?: HTMLElement,
  markerName?: string,
) => Undo;

/**
 * Marks everything except given node(or nodes) as inert
 */
export declare const inertOthers: (
  originalTarget: Element | Element[],
  parentNode?: HTMLElement,
  markerName?: string,
) => Undo;

/**
 * Returns true if the current browser support `inert` attribute.
 */
export declare const supportsInert: boolean;

/**
 * Automatic function to "suppress" DOM elements - _hide_ or _inert_ in the best possible way.
 */
export declare const suppressOthers: (
  originalTarget: Element | Element[],
  parentNode?: HTMLElement,
  markerName?: string,
) => Undo;
