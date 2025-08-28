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
          margin: auto;
          max-width: 30rem;
          padding: 1.5rem;
        }
        
        h2 {
          color: #0d1219;
          font-size: 1.5rem;
          line-height: 1.2;
          margin: 0 0 1rem 0;
        }
        
        p {
          
        }
        
        span.badge {
          font-size: 12px;
          padding: 0 6px 0 4px;
          background: #F1F5FB;
          border-radius: 4px;
          line-height: 18px;
          display: inline-block;
          font-weight: 600;
        }
        
        span.badge svg {
          margin-inline-end: 2px;
          vertical-align: text-bottom;
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
      <p>
         <slot name='products'></slot>
      </p>
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
      <p>
        By activating the trial, you agree to the <a href='https://vaadin.com/commercial-license-and-service-terms'
          class='terms-link' target='_blank'>terms and conditions</a>.
      </p>
      <p>
        This trial includes all commercial tools and components. Production builds during the trial will be watermarked.
      </p>
      ${this.#licenseDownloadStatus === 'started' ? '<p><strong>Waiting for the license key to be downloaded...</strong></p>' : ''}
      ${this.#licenseDownloadStatus === 'failed' ? '<div class="error">Failed to download the license key. Please try again later.</div>' : ''}
      <div class='button-container'>
        <button ${this.#licenseDownloadStatus === 'started' ? 'disabled' : ''} class='action-button'>${this.#trialExpired ? 'Extend trial 30 days' : 'Try for 7 days'}</button>
        <button ${this.#licenseDownloadStatus === 'started' ? 'disabled' : ''} class='login-button'>
          Log in / Sign up
          <svg xmlns='http://www.w3.org/2000/svg'  width='16' height='16' viewBox='0 0 16 16'>
            <path fill='#444' d='M14 16v-11l-1 1v9h-12v-12h9l1-1h-11v14z'></path>
            <path fill='#444' d='M16 0h-5l1.8 1.8-6.8 6.8 1.4 1.4 6.8-6.8 1.8 1.8z'></path>
          </svg>                  
        </button>          
      </div>
    </div>
      `;

    const actionButton = this.#shadowRoot.querySelector('button.action-button')!;
    actionButton.addEventListener('click', () => {
      if (this.#trialExpired) {
        this.openNewWindow('https://vaadin.com/pricing');
      } else {
        startPreTrial();
      }
    });
    const loginButton = this.#shadowRoot.querySelector('button.login-button')!;
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
