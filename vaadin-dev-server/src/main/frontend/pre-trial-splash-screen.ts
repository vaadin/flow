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
          max-width: 32rem;
          padding: 1.5rem 1.5rem 1rem 1.5rem;
        }
        
        h2 {
          color: #0d1219;
          font-size: 1.5rem;
          line-height: 1.2;
          margin: 0 0 1rem 0;
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
          gap: 0.25rem;
        }
        
        ul li span:first-of-type {
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
          font-weight: 600;
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
        }
        
        button.primary span + span {
          font-size: 0.8125rem;
          font-weight: normal;
        }
        
        button.primary + button.secondary {
          margin-top: 0.5rem;
        }
        
        hr + p {
          font-size: 0.8125rem;
          line-height: 1.7;
          text-align: center;
        }
        
        a {
          color: #0368DE;
        }
        
        .error {
          background: #ffedee;
          border-radius: 0.75rem;
          display: flex;
          flex-direction: column;
          gap: 0.25rem;
          margin-top: 1.5rem;
          padding: 1rem 1.5rem;
        }
        
        .error h3 {
          color: #0d1219;
          font-size: inherit;
          line-height: inherit;
          margin: 0;
        }
        
        .error a {
          font-weight: 600;
        }
      </style>
     `;

    this.#shadowRoot.innerHTML = `
    ${commonStyles}
    <div class='container'>
      ${this.#trialExpired ? `
        <h2>Trial expired</h2>
        <p>
          Vaadin Core is free and open-source. Sign in to keep using 
          <span class="badge">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M3 15.75V12C3 11.5875 3.14687 11.2344 3.44062 10.9406C3.73437 10.6469 4.0875 10.5 4.5 10.5H13.5C13.9125 10.5 14.2656 10.6469 14.5594 10.9406C14.8531 11.2344 15 11.5875 15 12V15.75H3ZM6.75 9.75C5.7125 9.75 4.82812 9.38438 4.09687 8.65313C3.36562 7.92188 3 7.0375 3 6C3 4.9625 3.36562 4.07813 4.09687 3.34688C4.82812 2.61563 5.7125 2.25 6.75 2.25H11.25C12.2875 2.25 13.1719 2.61563 13.9031 3.34688C14.6344 4.07813 15 4.9625 15 6C15 7.0375 14.6344 7.92188 13.9031 8.65313C13.1719 9.38438 12.2875 9.75 11.25 9.75H6.75ZM4.5 14.25H13.5V12H4.5V14.25ZM6.75 8.25H11.25C11.875 8.25 12.4062 8.03125 12.8437 7.59375C13.2812 7.15625 13.5 6.625 13.5 6C13.5 5.375 13.2812 4.84375 12.8437 4.40625C12.4062 3.96875 11.875 3.75 11.25 3.75H6.75C6.125 3.75 5.59375 3.96875 5.15625 4.40625C4.71875 4.84375 4.5 5.375 4.5 6C4.5 6.625 4.71875 7.15625 5.15625 7.59375C5.59375 8.03125 6.125 8.25 6.75 8.25ZM6.75 6.75C6.9625 6.75 7.14062 6.67813 7.28437 6.53438C7.42812 6.39063 7.5 6.2125 7.5 6C7.5 5.7875 7.42812 5.60938 7.28437 5.46563C7.14062 5.32188 6.9625 5.25 6.75 5.25C6.5375 5.25 6.35937 5.32188 6.21562 5.46563C6.07187 5.60938 6 5.7875 6 6C6 6.2125 6.07187 6.39063 6.21562 6.53438C6.35937 6.67813 6.5375 6.75 6.75 6.75ZM11.25 6.75C11.4625 6.75 11.6406 6.67813 11.7844 6.53438C11.9281 6.39063 12 6.2125 12 6C12 5.7875 11.9281 5.60938 11.7844 5.46563C11.6406 5.32188 11.4625 5.25 11.25 5.25C11.0375 5.25 10.8594 5.32188 10.7156 5.46563C10.5719 5.60938 10.5 5.7875 10.5 6C10.5 6.2125 10.5719 6.39063 10.7156 6.53438C10.8594 6.67813 11.0375 6.75 11.25 6.75Z" fill="url(#paint0_linear_85_182)"/>
            <defs>
              <linearGradient id="paint0_linear_85_182" x1="9" y1="2.25" x2="9" y2="15.75" gradientUnits="userSpaceOnUse">
                <stop stop-color="#1A81FA"/>
                <stop offset="1" stop-color="#8854FC"/>
              </linearGradient>
            </defs>
          </svg>
          Vaadin Copilot</span>,
          <span class="badge">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M12 15V9.75H15V15H12ZM7.5 15V3H10.5V15H7.5ZM3 15V6.75H6V15H3Z" fill="url(#paint0_linear_85_186)"/>
              <defs>
                <linearGradient id="paint0_linear_85_186" x1="9" y1="3" x2="9" y2="15" gradientUnits="userSpaceOnUse">
                  <stop stop-color="#1A81FA"/>
                  <stop offset="1" stop-color="#8854FC"/>
                </linearGradient>
              </defs>
            </svg>
            Pro components
          </span> and 
          <span class="badge">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M8.2125 11.6625L12.45 7.425L11.3812 6.35625L8.2125 9.525L6.6375 7.95L5.56875 9.01875L8.2125 11.6625ZM9 16.5C7.2625 16.0625 5.82812 15.0656 4.69687 13.5094C3.56562 11.9531 3 10.225 3 8.325V3.75L9 1.5L15 3.75V8.325C15 10.225 14.4344 11.9531 13.3031 13.5094C12.1719 15.0656 10.7375 16.0625 9 16.5ZM9 14.925C10.3 14.5125 11.375 13.6875 12.225 12.45C13.075 11.2125 13.5 9.8375 13.5 8.325V4.78125L9 3.09375L4.5 4.78125V8.325C4.5 9.8375 4.925 11.2125 5.775 12.45C6.625 13.6875 7.7 14.5125 9 14.925Z" fill="url(#paint0_linear_85_190)"/>
              <defs>
                <linearGradient id="paint0_linear_85_190" x1="9" y1="1.5" x2="9" y2="16.5" gradientUnits="userSpaceOnUse">
                  <stop stop-color="#1A81FA"/>
                  <stop offset="1" stop-color="#8854FC"/>
                </linearGradient>
              </defs>
            </svg>
            Team features
          </span> for 30 more days.
        </p>
        <p>Continue getting full access to:</p>
        ${this.getProductsList()}
        <button ${this.#licenseDownloadStatus === 'started' ? 'disabled' : ''} class='primary'>
          <span>Extend trial 30 days</span>
          <span>Sign up ⋅ No credit card required</span>
        </button>
        ` : `
        <h2>Get full access to all features</h2>
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
        ${this.getProductsList()}
        <button ${this.#licenseDownloadStatus === 'started' ? 'disabled' : ''} class='primary'>
          <span>Start 7-day trial</span>
          <span>No registration or credit card required</span>
        </button>
        <button ${this.#licenseDownloadStatus === 'started' ? 'disabled' : ''} class='secondary'>
          Activate your license
        </button>
        `
      }
      ${this.#startFailed ? `
        <div class='error'>
          <h3>Trial failed to start</h3>
          <p>Something went wrong while starting your trial. Try again in a moment. If the issue persists, <a href="https://pages.vaadin.com/contact" target="_blank">contact our support team</a>.</p>
        </div>`
      : ''
      }
      ${this.#licenseDownloadStatus === 'started' ? '<p><strong>Waiting for the license key to be downloaded...</strong></p>' : ''}
      ${this.#licenseDownloadStatus === 'failed' ? '<div class="error">Failed to download the license key. Please try again later.</div>' : ''}
      <hr>
      <p>
        By starting your trial, you agree to our <a href='https://vaadin.com/commercial-license-and-service-terms' target='_blank'>terms and conditions</a>.
      </p>
    </div>
    `;

    const primaryButton = this.#shadowRoot.querySelector('button.primary')!;
    primaryButton?.addEventListener('click', () => {
        this.dispatchEvent(new CustomEvent('primary-button-click', {
          detail: {
            expired: this.#trialExpired
          }
        }));
    });
    const secondaryButton = this.#shadowRoot.querySelector('button.secondary')!;
    secondaryButton?.addEventListener('click', () => {
        this.dispatchEvent(new CustomEvent('secondary-button-click'));
    });
  }

  private getProductsList(): string {
    return `
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
            <span>Vaadin Copilot</span><span>⋅</span><span>AI-powered assistant for building UIs</span>
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
            <span>Pro components</span><span>⋅</span><span>Charts, Grid Pro, CRUD and more</span>
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
          >
            <span>Team features</span><span>⋅</span><span>Acceleration Kits</span>
          </li>
        </ul>
    `;
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

function primaryButtonClickListener(event: CustomEvent) {
  if (event.detail.expired) {
    tryAcquireLicense();
  } else {
    startPreTrial();
  }
}

function secondaryButtonClickListener() {
  tryAcquireLicense();
}

export const showPreTrialSplashScreen = (shadowRoot: ShadowRoot | null, message: ProductAndMessage) => {
  if (shadowRoot && !shadowRoot.querySelector('vaadin-pretrial')) {
    const expiredPreTrial = message.preTrial?.trialState === 'EXPIRED';
    const preTrialElement = document.createElement('vaadin-pretrial');
    if (expiredPreTrial) {
      preTrialElement.setAttribute('expired', '');
    }
    const productsDiv = document.createElement('div');
    productsDiv.setAttribute('slot', 'products');
    productsDiv.innerHTML = `
      This application is using:
      <ul>
        <li>${message.product.name}</li>
      </ul>
    `;
    preTrialElement.appendChild(productsDiv);

    preTrialElement.addEventListener('secondary-button-click', secondaryButtonClickListener as EventListener);
    preTrialElement.addEventListener('primary-button-click', primaryButtonClickListener as EventListener);

    shadowRoot.innerHTML = `<slot></slot>`;
    shadowRoot.appendChild(preTrialElement);
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
