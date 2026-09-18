// Detect https://issues.chromium.org/issues/360159391
let dummy = document.createElement("div");
document.body.appendChild(dummy);
let property = "--foo-" + Date.now();
dummy.style.cssText = `${property}: 1; transition: ${property} 1ms step-start allow-discrete`;

/**
 * Detect if the browser is affected by the unregistered transition bug.
 * @see https://issues.chromium.org/issues/360159391
 * @type {Promise<boolean>}
 */
export default new Promise(resolve => {
	requestAnimationFrame(() => {
		setTimeout(_ => resolve(true), 30);
		dummy.addEventListener("transitionstart", _ => resolve(false));
		dummy.style.setProperty(property, "2");
	});
}).finally(_ => dummy.remove());
