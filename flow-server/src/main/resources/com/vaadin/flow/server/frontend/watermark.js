// Define the custom element
class VaadinWatermark extends HTMLElement {
  constructor() {
    super();

    this.observer = null;
    this.parentObserver = null;

    // Create a shadow DOM for encapsulation
    const shadowRoot = this.attachShadow({ mode: 'closed' });

    // Initialize the component
    this.render(shadowRoot);
    this.setupProtection(shadowRoot);
  }

  render(shadowRoot) {
    // Use template string for HTML structure
    shadowRoot.innerHTML = `
            <style>
                :host {
                    position: fixed;
                    bottom: 0;
                    left: 0;
                    right: 0;
                    text-align: center;
                    z-index: 9999;
                    min-height: 46px !important;
                    min-width: 100% !important;
                    display: block !important;
                    visibility: visible !important;
                    opacity: 1 !important;
                    clip-path: none !important;
                    text-indent: 0 !important;
                }
                
                .container {
                    display: inline-flex;
                    align-items: center;
                    background-color: #4285f4;
                    color: white;
                    padding: 10px;
                    border-radius: 10px 10px 0 0;
                }
                
                .logo {
                    margin-right: 10px;
                }
                
                .message {
                    margin-right: 10px;
                }
                
                a {
                    color: white;
                    text-decoration: underline;
                }
                
                button {
                    background: none;
                    border: none;
                    color: white;
                    cursor: pointer;
                    padding: 0;
                }
            </style>
            
            <div class='container'>
                <span class='logo'>vaadin }></span>
                <span class='message'>
                    Commercial features require a subscription, <a href='https://vaadin.com/pricing' target='_blank' class='link'>read more</a>.
                </span>
            </div>
        `;
  }

  connectedCallback() {
    this.setupParentRemovalProtection();
  }

  disconnectedCallback() {
    this.cleanup();
  }

  setupProtection(shadowRoot) {

    const originalRemove = Element.prototype.remove;
    this.remove = function() {
      console.debug('Attempt to remove watermark detected - restoring');
      const currentParent = this.parentNode;
      // Let the removal happen
      originalRemove.apply(this, arguments);
      // Re-add the component
      this.restoreWatermark(currentParent);
    };

    if (!this.observer && shadowRoot) {
      this.observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
          if (mutation.type === 'childList' || mutation.type === 'characterData') {
            console.debug('Watermark content modification detected - restoring');
            this.restoreWatermark(this.parentNode);
          }
          if (mutation.type === 'attributes') {
            console.debug('Watermark attribute modification detected - restoring');
            this.restoreWatermark(this.parentNode);
          }
        });
      });

      this.observer.observe(shadowRoot, {
        childList: true,
        subtree: true,
        characterData: true,
        attributes: true,
        attributeOldValue: true
      });
    }

    // Protect against style changes
    this.protectStyles();
  }

  setupParentRemovalProtection() {
    // Protect against removal from parent
    if (!this.parentObserver && this.parentNode) {
      this.parentObserver = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
          if (mutation.type === 'childList') {
            mutation.removedNodes.forEach((node) => {
              if (node === this) {
                console.debug('Watermark removal detected - restoring');
                this.restoreWatermark(mutation.target);
              }
            });
          }
        });
      });

      this.parentObserver.observe(this.parentNode, {
        childList: true, subtree: true
      });
    }
  }

  protectStyles() {
    // Override style setters
    Object.defineProperty(this, 'style', {
      get() {
        // return an empty object to prevent programmatic changes
        return {};
      },
      set(value) {
        // prevent setting regular style object
      }
    });
  }

  cleanup() {
    if (this.observer) {
      this.observer.disconnect();
    }
    if (this.parentObserver) {
      this.parentObserver.disconnect();
    }
  }

  restoreWatermark(maybeParentNode) {
    // Re-add the component if it's removed
    if (!maybeParentNode) {
      // Watermark component not yet attached
      return;
    }
    setTimeout(() => {
      console.debug('Re-adding watermark component');
      if (maybeParentNode.contains(this) === true) {
        maybeParentNode.removeChild(this);
      }
      maybeParentNode.appendChild(document.createElement('vaadin-watermark'));
    }, 0);
  }
}

// Register the custom element
customElements.define('vaadin-watermark', VaadinWatermark);
