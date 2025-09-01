import { ProductAndMessage, startPreTrial, tryAcquireLicense } from './License';

class PreTrial extends HTMLElement {
  #parentObserver: MutationObserver | null;
  #shadowRoot: ShadowRoot;
  #trialExpired: boolean;
  #startFailed: boolean | null;
  #licenseDownloadStatus: string | null;
  private remove: Function;

  constructor() {
    super();

    this.#parentObserver = null;
    this.#trialExpired = false;
    this.#startFailed = null;
    this.#licenseDownloadStatus = null;

    // Create a shadow DOM for encapsulation
    this.#shadowRoot = this.attachShadow({ mode: 'closed' });

    // Initialize the component
    this.render();
    this.setupProtection();
  }

  // Define the observed attributes for the web component
  static get observedAttributes(): string[] {
    return ['expired', 'start-failure', 'license-download'];
  }

  private render(): void {
    // Use template string for HTML structure
    const commonStyles = `
      <style>
        :host {
          position: fixed;
          bottom: 0;
          left: 0;
          right: 0;
          z-index: 9999;
          min-height: 100% !important;
          min-width: 100% !important;
          display: flex !important;
          visibility: visible !important;
          opacity: 1 !important;
          clip-path: none !important;
          text-indent: 0 !important;          
          background-color: rgba(0, 0, 0, 0.5);          
        }
        
        .container {
          background: white;
          border-radius: 0.5rem;
          box-sizing: border-box;
          color: #3f4d62;
          font-family: "nb_international_pro","ui-sans-serif","system-ui","-apple-system","BlinkMacSystemFont","Segoe UI","Roboto","Helvetica Neue","Arial","Noto Sans","sans-serif","Apple Color Emoji","Segoe UI Emoji","Segoe UI Symbol","Noto Color Emoji";
          font-size: 0.875rem;
          font-weight: normal;
          letter-spacing: 0.01em;
          line-height: 1.7;
          margin: auto;
          max-width: 30rem;
          padding: 1.5rem 1.5rem 1rem 1.5rem;
        }
        
        h2 {
          color: #0d1219;
          font-size: 1.5rem;
          line-height: 1.2;
          margin: 0 0 1.25rem 0;
        }
        
        p {
          margin: 0;
        }
        
        span.badge {
          background: #F1F5FB;
          border-radius: 4px;
          display: inline-block;
          font-size: 0.8125rem;
          font-weight: 600;
          line-height: 1.7;
          padding: 0 6px 0 4px;
        }
        
        span.badge svg {
          vertical-align: sub;
        }
        
        p:has(+ ul) {
          color: #0d1219;
          font-weight: 600;
          margin-top: 1.25rem;
        }
        
        ul {
          display: flex;
          flex-direction: column;
          gap: 0.5rem;
          list-style: none;
          margin: 0.5rem 0 1.5rem 0;
          padding: 0;
        }
        
        ul li {
          align-items: center;
          display: flex;
          gap: 6px;
        }
        
        ul li span {
          color: #0d1219;
          font-weight: 600;
        }
        
        hr {
          border-color: rgb(224, 233, 244);
          border-top: 0;
          margin: 1.5rem 0 0.75rem 0;
        }
        
        button {
          align-items: center;
          background: #F1F5FB;
          border: none;
          border-radius: 8px;
          display: flex;
          color: #0368DE;
          font-family: "nb_international_promono","ui-monospace","SFMono-Regular","Menlo","Monaco","Consolas","Liberation Mono","Courier New","monospace";
          font-size: inherit;
          font-weight: 500;
          height: 2.375rem;
          justify-content: center;
          line-height: 1.7;
          padding: 0;
          width: 100%;
        }
        
        button.primary {
          background: #0368DE;
          color: white;
          flex-direction: column;
          height: 4.5rem;
          margin-bottom: 0.5rem;
        }
        
        button.primary span + span {
          font-size: 0.8125rem;
          font-weight: normal;
        }
        
        hr + p {
          font-size: 0.8125rem;
          line-height: 1.7;
          text-align: center;
        }
        
        a {
          color: #0368DE;
        }
      </style>
     `;

    this.#shadowRoot.innerHTML = `
    ${commonStyles}
    <div class='container'>
      <h2>${this.#trialExpired ? 'Trial expired' : 'Get full access to all features'}</h2>
      <p>
        Vaadin Core is free and open-source. To use Pro components like <span class="badge">
        <svg width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 15V9.75h3V15h-3Zm-4.5 0V3h3v12h-3ZM3 15V6.75h3V15H3Z" fill="url(#a)"/>
          <defs>
            <linearGradient id="a" x1="9" y1="3" x2="9" y2="15" gradientUnits="userSpaceOnUse">
              <stop stop-color="#1A81FA"/>
              <stop offset="1" stop-color="#8854FC"/>
            </linearGradient>
          </defs>
        </svg>
        Charts</span> in your app, activate a free trial.
      </p>
      <p>Get full access:</p>
      <ul>
        <li>
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none">
            <path
              d="M4 21v-5c0-.55.196-1.02.588-1.412A1.926 1.926 0 0 1 6 14h12c.55 0 1.02.196 1.413.588.391.391.587.862.587 1.412v5H4Zm5-8c-1.383 0-2.563-.488-3.537-1.463C4.487 10.563 4 9.383 4 8s.487-2.563 1.463-3.537C6.437 3.487 7.617 3 9 3h6c1.383 0 2.563.487 3.538 1.463C19.512 5.437 20 6.617 20 8s-.488 2.563-1.462 3.537C17.562 12.512 16.383 13 15 13H9Zm-3 6h12v-3H6v3Zm3-8h6c.833 0 1.542-.292 2.125-.875A2.893 2.893 0 0 0 18 8c0-.833-.292-1.542-.875-2.125A2.893 2.893 0 0 0 15 5H9c-.833 0-1.542.292-2.125.875A2.893 2.893 0 0 0 6 8c0 .833.292 1.542.875 2.125A2.893 2.893 0 0 0 9 11Zm0-2c.283 0 .52-.096.713-.287A.968.968 0 0 0 10 8a.968.968 0 0 0-.287-.713A.968.968 0 0 0 9 7a.968.968 0 0 0-.713.287A.968.968 0 0 0 8 8c0 .283.096.52.287.713.192.191.43.287.713.287Zm6 0c.283 0 .52-.096.713-.287A.967.967 0 0 0 16 8a.967.967 0 0 0-.287-.713A.968.968 0 0 0 15 7a.968.968 0 0 0-.713.287A.967.967 0 0 0 14 8c0 .283.096.52.287.713.192.191.43.287.713.287Z"
              fill="url(#a)"
            />
            <defs>
              <linearGradient
                id="a"
                x1="12"
                y1="3"
                x2="12"
                y2="21"
                gradientUnits="userSpaceOnUse"
              >
                <stop stop-color="#1A81FA" />
                <stop offset="1" stop-color="#8854FC" />
              </linearGradient>
            </defs>
          </svg>
          <span>Vaadin Copilot</span> ⋅ AI-powered assistant for building UIs
        </li>
        <li>
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none">
            <path
              d="M16 20v-7h4v7h-4Zm-6 0V4h4v16h-4Zm-6 0V9h4v11H4Z"
              fill="url(#a)"
            />
            <defs>
              <linearGradient
                id="a"
                x1="12"
                y1="4"
                x2="12"
                y2="20"
                gradientUnits="userSpaceOnUse"
              >
                <stop stop-color="#1A81FA" />
                <stop offset="1" stop-color="#8854FC" />
              </linearGradient>
            </defs>
          </svg>
          <span>Pro Components</span> ⋅ Charts, Grid Pro, CRUD and more
        </li>
        <li>
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none">
            <path
              d="M10.95 15.55 16.6 9.9l-1.425-1.425L10.95 12.7l-2.1-2.1-1.425 1.425 3.525 3.525ZM12 22c-2.317-.583-4.23-1.913-5.737-3.988C4.754 15.938 4 13.633 4 11.1V5l8-3 8 3v6.1c0 2.533-.754 4.838-2.262 6.912C16.229 20.087 14.317 21.418 12 22Zm0-2.1c1.733-.55 3.167-1.65 4.3-3.3s1.7-3.483 1.7-5.5V6.375l-6-2.25-6 2.25V11.1c0 2.017.567 3.85 1.7 5.5s2.567 2.75 4.3 3.3Z"
              fill="url(#a)"
            />
            <defs>
              <linearGradient
                id="a"
                x1="12"
                y1="2"
                x2="12"
                y2="22"
                gradientUnits="userSpaceOnUse"
              >
                <stop stop-color="#1A81FA" />
                <stop offset="1" stop-color="#8854FC" />
              </linearGradient>
            </defs></svg
          ><span>Enterprise features</span> ⋅ Designer, Control Center
        </li>
      </ul>
      ${this.#startFailed ? `
        <div class='error'>
          <h4>Trial cannot be started</h4>
            ${this.#trialExpired ?
        'The trial period has expired. You can get an extended 30-day trial by logging in.'
        : 'The attempt to start the Trial failed. Please try again later or contact support.'}
        </div>`
      : ''
      }
      ${!this.#startFailed && this.#trialExpired ? '<p>You can get an extended 30-day trial by logging in.</p>' : ''}
      ${this.#licenseDownloadStatus === 'started' ? '<p><strong>Waiting for the license key to be downloaded...</strong></p>' : ''}
      ${this.#licenseDownloadStatus === 'failed' ? '<div class="error">Failed to download the license key. Please try again later.</div>' : ''}
      <button ${this.#licenseDownloadStatus === 'started' ? 'disabled' : ''} class='primary'>
        ${this.#trialExpired ?
          '<span>Extend trial 30 days</span>' :
          '<span>Start 7-day trial</span><span>No registration or credit card required</span>'}
      </button>
      <button ${this.#licenseDownloadStatus === 'started' ? 'disabled' : ''} class='secondary'>
        Activate your license
      </button>
      <hr>
      <p>
        During your trial, production builds will have a small watermark.
        By starting your trial, you agree to our <a href='https://vaadin.com/commercial-license-and-service-terms' target='_blank'>terms and conditions</a>.
      </p>
    </div>
      `;

    const actionButton = this.#shadowRoot.querySelector('button.primary')!;
    actionButton.addEventListener('click', () => {
      if (this.#trialExpired) {
        this.openNewWindow('https://vaadin.com/pricing');
      } else {
        startPreTrial();
      }
    });
    const loginButton = this.#shadowRoot.querySelector('button.secondary')!;
    loginButton.addEventListener('click', () => {
      tryAcquireLicense();
    });
  }

  private openNewWindow(url: string): void {
    const newWindow = window.open(url, '_blank');
    if (newWindow) {
      newWindow.opener = null;
    }
  }

  connectedCallback(): void {
    this.setupParentRemovalProtection();
  }

  disconnectedCallback(): void {
    this.cleanup();
  }

  attributeChangedCallback(name: string, oldValue: string | null, newValue: string | null): void {
    if (name === 'expired') {
      this.handleExpiredChange(newValue !== null && newValue !== 'false');
    } else if (name === 'start-failure') {
      this.handleStartFailed(newValue === 'expired');
    } else if (name === 'license-download') {
      this.handleLicenseDownload(newValue);
    }
  }

  private handleLicenseDownload(value: string | null) {
    if (this.#licenseDownloadStatus !== value) {
      this.#licenseDownloadStatus = value;
      this.render();
    }
  }

  private handleExpiredChange(value: boolean) {
    if (this.#trialExpired !== value) {
      this.#trialExpired = value;
      this.render();
    }
  }

  private handleStartFailed(expired: boolean) {
    if (this.#startFailed !== expired || this.#trialExpired !== expired) {
      this.#trialExpired = expired;
      this.#startFailed = true;
      this.render();
    }
  }

  private setupProtection(): void {
    const originalRemove = Element.prototype.remove;
    this.remove = function(this: PreTrial): void {
      console.debug('Attempt to remove vaadin-pretrial detected - restoring');
      const currentParent = this.parentNode;
      // Let the removal happen
      originalRemove.apply(this, arguments);
      // Re-add the component
      this.restoreSplashScreen(currentParent as Node);
    };

    // Protect against style changes
    this.protectStyles();
  }

  private setupParentRemovalProtection(): void {
    // Protect against removal from parent
    if (!this.#parentObserver && this.parentNode) {
      this.#parentObserver = new MutationObserver((mutations: MutationRecord[]) => {
        mutations.forEach((mutation: MutationRecord) => {
          if (mutation.type === 'childList') {
            mutation.removedNodes.forEach((node: Node) => {
              if (node === this) {
                console.debug('vaadin-pretrial removal detected - restoring');
                this.restoreSplashScreen(mutation.target);
              }
            });
          }
        });
      });

      this.#parentObserver.observe(this.parentNode, {
        childList: true, subtree: true
      });
    }
  }

  private protectStyles(): void {
    // Override style setters
    Object.defineProperty(this, 'style', {
      get(): object {
        // return an empty object to prevent programmatic changes
        return {};
      },
      set(_value: CSSStyleDeclaration): void {
        // prevent setting regular style object
      }
    });
  }

  private cleanup(): void {
    if (this.#parentObserver) {
      this.#parentObserver.disconnect();
    }
  }

  private restoreSplashScreen(maybeParentNode: Node | null): void {
    // Re-add the component if it's removed
    if (!maybeParentNode) {
      // Splash screen component not yet attached
      return;
    }
    setTimeout(() => {
      console.debug('Re-adding vaadin-pretrial component');
      const products = this.querySelector('[slot="products"]');
      if ((maybeParentNode as ParentNode).contains(this)) {
        (maybeParentNode as Node).removeChild(this);
      }
      const splashScreen = document.createElement('vaadin-pretrial');
      if (this.#trialExpired) {
        splashScreen.setAttribute('expired', 'true');
      }
      if (this.#startFailed) {
        splashScreen.setAttribute('start-failure', this.#trialExpired ? 'expired' : '');
      }
      if (products) {
        splashScreen.appendChild(products.cloneNode(true));
      }
      (maybeParentNode as Element).appendChild(splashScreen);
    }, 0);
  }
}

// Register the custom element
customElements.define('vaadin-pretrial', PreTrial);

export const showPreTrialSplashScreen = (shadowRoot: ShadowRoot | null, message: ProductAndMessage) => {
  if (shadowRoot && !shadowRoot.innerHTML.includes('vaadin-pretrial')) {
    const expiredPreTrial = message.preTrial?.trialState === 'EXPIRED';
    shadowRoot.innerHTML = `<slot></slot><vaadin-pretrial ${expiredPreTrial ? 'expired' : ''}>
      This application is using:
      <div slot='products'>
        This application is using:
        <ul>
          <li>${message.product.name}</li>
        </ul>
      </div>
    </vaadin-pretrial>`;
  }
};
export const preTrialStartFailed = (expired: boolean, shadowRoot: ShadowRoot | null) => {
  if (shadowRoot) {
    const element = shadowRoot.querySelector('vaadin-pretrial');
    element?.setAttribute('start-failure', expired ? 'expired' : '');
  }
};
export const updateLicenseDownloadStatus = (action: 'started' | 'failed' | 'completed', shadowRoot: ShadowRoot | null) => {
  if (shadowRoot) {
    const element = shadowRoot.querySelector('vaadin-pretrial');
    element?.setAttribute('license-download', action);
  }
};
