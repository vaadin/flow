/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */

// CSS test fixtures:
export const basicRuleset = `
body {
  margin: 0;
  padding: 0px
}
`;

export const atRules = `
@import url('foo.css');

@font-face {
  font-family: foo;
}

@charset 'foo';
`;

export const keyframes = `
@keyframes foo {
  from {
    fiz: 0%;
  }

  99.9% {
    fiz: 100px;
    buz: true;
  }
}
`;

export const customProperties = `
:root {
  --qux: vim;
  --foo: {
    bar: baz;
  };
}
`;

export const extraSemicolons = `
:host {
  margin: 0;;;
  padding: 0;;
  ;display: block;
};
`;

export const declarationsWithNoValue = `
foo;
bar 20px;

div {
  baz;
}
`;

export const minifiedRuleset = '.foo{bar:baz}div .qux{vim:fet;}';

export const psuedoRuleset = '.foo:bar:not(#rif){baz:qux}';

export const dataUriRuleset = '.foo{bar:url(qux;gib)}';

export const pathologicalComments = `
.foo {
  bar: /*baz*/vim;
}
/* unclosed
@fiz {
  --huk: {
    /* buz */
    baz: lur;
  };
}
@gak wiz;`;
