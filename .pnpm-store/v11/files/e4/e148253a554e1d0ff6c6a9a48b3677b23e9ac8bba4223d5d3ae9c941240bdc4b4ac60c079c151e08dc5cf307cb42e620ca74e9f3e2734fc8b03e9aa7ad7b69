<header class="wa-split">

<h1 data-version="{{ pkg.version }}"><img src="assets/logo.svg" class="logo" width="60"> Style <span>Observer</span></h1>

<nav>
	<a href="/api">API</a>
	<span class="readme-only">·</span>
	<a href="/tests">Tests</a>
	<a href="https://github.com/leaverou/style-observer" target="_blank">
		<i class="fab fa-github"></i>
	</a>
	<hr class="readme-only" />
</nav>

</header>
<div class="page">
<aside>

- [Install](#install)
- [Usage](#usage)
- [Future Work](#future-work)
- [Limitations & Caveats](#limitations-%26-caveats)
- [Prior Art](#prior-art)

</aside>
<main>

<p class="blurb">
A robust, production-ready library to observe CSS property changes.
Detects browser bugs and works around them, so you don't have to.
</p>

[![npm](https://img.shields.io/npm/v/style-observer)](https://www.npmjs.com/package/style-observer)
[![gzip size](https://img.shields.io/badge/gzip-2.73kB-blue)](https://pkg-size.dev/style-observer)

- <span>✅</span> Observe changes to custom properties
- <span>✅</span> Observe changes to standard properties (except `transition` and `animation`)
- <span>✅</span> Observe changes on any element (including those in Shadow DOM)
- <span>✅</span> [Lightweight](https://pkg-size.dev/style-observer), ESM-only code, with no dependencies
- <span>✅</span> [200+ unit tests](tests) you can run in your browser of choice
- <span>✅</span> Throttling per element
- <span>✅</span> Does not overwrite existing transitions

## Compatibility

<div class="scrollable">
<table>
<thead>
<tr>
	<th>Feature</th>
	<th><i class="fab fa-chrome"></i> Chrome</th>
	<th><i class="fab fa-safari"></i> Safari</th>
	<th><i class="fab fa-firefox"></i> Firefox</th>
	<th>% of global users</th>
</tr>
</thead>
<tbody>
<tr>
	<td>Custom properties</td>
	<td>117</td>
	<td>17.4</td>
	<td>129</td>
	<td><a href="https://caniuse.com/mdn-css_properties_transition-behavior">89%</a></td>
</tr>
<tr>
	<td>Custom properties (registered with an animatable type)</td>
	<td>97</td>
	<td>16.4</td>
	<td>128</td>
	<td><a href="https://caniuse.com/mdn-api_css_registerproperty_static">93%</a></td>
</tr>
<tr>
	<td>Standard properties (discrete)
	<br><small class="compat wa-caption-s">Except <code>transition</code>, <code>animation</code></small>
	</td>
	<td>117</td>
	<td>17.4</td>
	<td>129</td>
	<td><a href="https://caniuse.com/mdn-css_properties_transition-behavior">89%</a></td>
</tr>
<tr>
	<td>Standard properties (animatable)</td>
	<td>97</td>
	<td>15.4</td>
	<td>104</td>
	<td>95%</td>
</tr>
</tbody>
</table>
</div>

## Install

The quickest way is to just include straight from the [Netlify](https://www.netlify.com/) CDN:

```js
import StyleObserver from "https://observe.style/index.js";
```

This will always point to the latest version, so it may be a good idea to eventually switch to a local version that you can control.
E.g. you can use npm:

```sh
npm install style-observer
```

and then, if you use a bundler like Rollup or Webpack:

```js
import StyleObserver from "style-observer";
```

and if you don’t:

```js
import StyleObserver from "node_modules/style-observer/dist/index.js";
```

## Usage

You can first create the observer instance and then observe, like a `MutationObserver`.
The simplest use is observing a single property on a single element:

```js
const observer = new StyleObserver(records => console.log(records));
observer.observe(document.querySelector("#my-element"), "--my-custom-property");
```

You can also observe multiple properties on multiple elements:

```js
const observer = new StyleObserver(records => console.log(records));
const properties = ["color", "--my-custom-property"];
const targets = document.querySelectorAll(".my-element");
observer.observe(targets, properties);
```

You can also provide both targets and properties when creating the observer,
which will also call `observe()` for you:

```js
import StyleObserver from "style-observer";

const observer = new StyleObserver(callback, {
	targets: document.querySelectorAll(".my-element"),
	properties: ["color", "--my-custom-property"],
});
```

Both targets and properties can be either a single value or an iterable.

Note that the observer will not fire immediately for the initial state of the elements (i.e. it behaves like `MutationObserver`, not like `ResizeObserver`).

### Records

Just like other observers, changes that happen too close together (set the `throttle` option to configure) will only invoke the callback once,
with an array of records, one for each change.

Each record is an object with the following properties:

- `target`: The element that changed
- `property`: The property that changed
- `value`: The new value of the property
- `oldValue`: The previous value of the property

## Future Work

- Observe pseudo-elements
- `immediate` convenience option that fires the callback immediately for every observed element

## Limitations & Caveats

- You cannot observe changes on elements **not connected to a document**. However, once the elements become connected again, the observer will pick up any changes that happened while they were disconnected.
- You cannot observe changes to `transition` and `animation` properties (and their constituent properties).
- You cannot observe changes **caused by CSS animations** (follow [#87](https://github.com/LeaVerou/style-observer/issues/87) for updates).
- Changes caused due to a slotted element being moved to a different slot will not be picked up.

### Changing `transition` properties after observing

If you change the `transition`/`transition-*` properties dynamically on elements you are observing after you start observing them,
the easiest way to ensure the observer continues working as expected is to call `observer.updateTransition(targets)` to regenerate the `transition` property the observer uses to detect changes.

If running JS is not an option, you can also do it manually:

1. Add `, var(--style-observer-transition, --style-observer-noop)` at the end of your `transition` property.
   E.g. if instead of `transition: 1s background` you'd set `transition: 1s background, var(--style-observer-transition, --style-observer-noop)`.
2. Make sure to also set `transition-behavior: allow-discrete;`.

## Prior Art

The quest for a JS style observer has been long and torturous.

- Early attempts used polling. Notable examples were [`ComputedStyleObserver` by Keith Clark](https://github.com/keithclark/ComputedStyleObserver)
  and [`StyleObserver` by PixelsCommander](https://github.com/PixelsCommander/StyleObserver)
- [Jane Ori](https://propjockey.io) was the first to do better than polling, her [css-var-listener](https://github.com/propjockey/css-var-listener) using a combination of observers and events.
- [css-variable-observer](https://github.com/fluorumlabs/css-variable-observer) by [Artem Godin](https://github.com/fluorumlabs) pioneered using transition events to observe property changes, and used an ingenious hack based on `font-variation-settings` to observe CSS property changes.
- Four years later, [Bramus Van Damme](https://github.com/bramus) pioneered a way to do it "properly" in [style-observer](https://github.com/bramus/style-observer),
  thanks to [`transition-behavior: allow-discrete`](https://caniuse.com/mdn-css_properties_transition-behavior) becoming Baseline and even [blogged about all the bugs he encountered along the way](https://www.bram.us/2024/08/31/introducing-bramus-style-observer-a-mutationobserver-for-css/).

While `StyleObserver` builds on this body of work, it is not a fork of any of them.
It was written from scratch with the explicit goal of extending browser support and robustness.
[Read the blog post](https://lea.verou.me/2025/style-observer/) for more details.

</main>
</div>
<footer>
<hr class="readme-only" />

By [Lea Verou](https://lea.verou.me/) and [Dmitry Sharabin](https://d12n.me/).

</footer>
