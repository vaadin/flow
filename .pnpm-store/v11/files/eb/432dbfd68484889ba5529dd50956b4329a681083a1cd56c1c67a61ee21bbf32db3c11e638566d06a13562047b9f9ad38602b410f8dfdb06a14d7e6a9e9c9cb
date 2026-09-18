# Constructible style sheets polyfill

[![CI](https://github.com/calebdwilliams/construct-style-sheets/actions/workflows/ci.yml/badge.svg)](https://github.com/calebdwilliams/construct-style-sheets/actions)
[![npm version](https://img.shields.io/npm/v/construct-style-sheets-polyfill.svg?style=flat)](https://npmjs.org/package/construct-style-sheets-polyfill 'View this project on npm')
[![codecov](https://codecov.io/gh/calebdwilliams/construct-style-sheets/branch/main/graph/badge.svg)](https://codecov.io/gh/calebdwilliams/construct-style-sheets)

This package is a polyfill for the [constructible style sheets/adopted style sheets specification](https://github.com/WICG/construct-stylesheets/blob/gh-pages/explainer.md). The full specificaiton is enabled by default in Google Chrome as of version 73.

Currently [Mozilla is considering implementation of the feature](https://github.com/mozilla/standards-positions/issues/103), marking it as "worth prototyping" while Apple has not publically signaled, they have been active in the standards discussions surrounding it.

## Use case

The constructible style sheets proposal is intended to allow for the dynamic
creation and sharing of style sheets, even across shadow boundaries. By adopting
a style sheet into a shadow root, the same sheet can be applied to multiple
nodes, including the document.

## How it works

This polyfill will create a new style element for every `DocumentOrShadowRoot`
into which the sheet is adopted. This is counter to the current proposal, but
updates to the style sheet using the `replace` or `replaceSync` methods should
update the relevant style elements with the updated content across all adopters.

No changes will occur in a browser that supports the feature by default.

## Support

This polyfill supports all modern browsers and IE 11.

For browsers that do not support the web components specification (currently
IE 11 and Edge) only the document-level style sheets adoption works.

### IE 11

To make this polyfill work with IE 11 you need the following tools:
- `Symbol` polyfill (with support for `Symbol.hasInstance`).
- [@babel/plugin-transform-instanceof](https://www.npmjs.com/package/@babel/plugin-transform-instanceof)
applied to your code that uses `instanceof` against `CSSStyleSheet`.

## Installation
This package is available on `npm` under the name `construct-style-sheet-polyfill`
and can be installed with [npm](https://docs.npmjs.com/getting-started),
[yarn](https://yarnpkg.com/en/docs/getting-started), [unpkg](https://unpkg.com)
or however else you consume dependencies.

### Example commands: 

npm:
```bash
npm i construct-style-sheets-polyfill
```

yarn:
```bash
yarn add construct-style-sheets-polyfill
```

unpkg:
```javascript
import 'https://unpkg.com/construct-style-sheets-polyfill';
```

## Usage

```javascript
const everythingTomato = new CSSStyleSheet();
everythingTomato
  .replace(
    `
* {
    color: tomato;
}
`,
  )
  .then(console.log); // will log the CSSStyleSheet object

document.adoptedStyleSheets = [everythingTomato];

class TestEl extends HTMLElement {
  constructor() {
    super();
    this.attachShadow({mode: 'open'});
    this.shadowRoot.adoptedStyleSheets = [everythingTomato];
  }

  connectedCallback() {
    this.shadowRoot.innerHTML = `<h1>This will be tomato colored, too</h1>`;
  }
}

customElements('test-el', TestEl);

const testEl = new TestEl();
document.body.appendChild(testEl);
```

The polyfill will append new `style` tags to the designated `DocumentOrShadowRoot`.
Manually removing the style node will cause a re-insertion of the styles at the
designated root. To remove a style sheet, you _must_ remove the style element
from the `element.adoptedStyleSheets` array. The behavior here is supposed to
emulate a `FrozenArray`, so modifying the array in question will have no effect
until the value is changed using a setter.

## A note about versioning

This packages doesn't necessarily follow semantic versioning. As the spec is still under consideration and implementation by browser vendors, the features supported by this package will change (generally following Chrome's implementation).