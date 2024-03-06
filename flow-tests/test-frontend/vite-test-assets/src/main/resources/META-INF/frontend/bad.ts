/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
let hello: any; // Must be initialized

function foo(err: any) { // parameter declared but not read
  this.emit('end'); // `this` implicitly has type `any`
}

function func1(bar) { // Parameter 'bar' implicitly has an 'any' type.
    const baz = "a"; // Local declared but never used

  return bar + 'a';
}

(window as any).bad = function() {
    return "good";
}
