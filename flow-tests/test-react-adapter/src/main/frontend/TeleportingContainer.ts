/**
 * Stands in for an overlay-based component such as a dialog. Opening moves the
 * content into a separate overlay element, which disconnects and reconnects
 * every child within the same task.
 */
class TeleportingContainer extends HTMLElement {
  open() {
    const overlay = document.createElement('div');
    overlay.setAttribute('part', 'overlay');
    // Moving the content into a detached element disconnects it...
    overlay.append(...this.childNodes);
    // ...and attaching the overlay reconnects it, without yielding.
    this.appendChild(overlay);
  }
}

customElements.define('teleporting-container', TeleportingContainer);
