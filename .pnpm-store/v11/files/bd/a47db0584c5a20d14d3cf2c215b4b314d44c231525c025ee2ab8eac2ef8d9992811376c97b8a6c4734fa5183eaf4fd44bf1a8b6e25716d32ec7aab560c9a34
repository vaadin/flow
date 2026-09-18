let style;

/**
 * Adopt CSS into a document or shadow root.
 * @param {string} css - The CSS to adopt.
 * @param {Document|ShadowRoot} [root=globalThis.document] - The document or shadow root to adopt the CSS into.
 */
export default function adoptCSS (css, root = globalThis.document) {
	// Ensure root is always a document
	root = root.ownerDocument ?? root;
	let window = root.defaultView;
	if (root.adoptedStyleSheets) {
		let sheet = new window.CSSStyleSheet();
		sheet.replaceSync(css);

		if (Object.isFrozen(root.adoptedStyleSheets)) {
			root.adoptedStyleSheets = [...root.adoptedStyleSheets, sheet];
		}
		else {
			root.adoptedStyleSheets.push(sheet);
		}
	}
	else {
		style ??= root.head.appendChild(root.createElement("style"));

		style.insertAdjacentText("beforeend", css);
	}
}
