class VaadinCommercialBanner extends HTMLElement {
  constructor() {
    super();
    // Create a shadow DOM for encapsulation
    const shadowRoot = this.attachShadow({ mode: 'closed' });

    // Initialize the component
    this.render(shadowRoot);
  }

  render(shadowRoot) {
    const vaadinLogo = `
      <svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 167.54 38.66'>
        <path d='M167.54 19.32a2.28 2.28 0 0 0-1.34-2.07l-.09-.05-14.71-7.95a2.55 2.55 0 0 0-3.82 2.21 2.48 2.48 0 0 0 1.74 2.42l10.16 5.45-10.14 5.44a2.49 2.49 0 0 0-1.75 2.42 2.55 2.55 0 0 0 3.81 2.21l14.7-8h.06a2.28 2.28 0 0 0 1.37-2.09' />
        <path d='M43.68 19.56a10.75 10.75 0 1 0-5 9.26l.26-.16.11.28a2.38 2.38 0 0 0 4.63-.94zm-10.79 5.29a5.29 5.29 0 1 1 5.29-5.29 5.29 5.29 0 0 1-5.29 5.29m34.94-5.29a10.7 10.7 0 1 0-5 9.26l.26-.16.12.28a2.35 2.35 0 0 0 2.2 1.55A2.4 2.4 0 0 0 67.83 28zm-10.76 5.29a5.29 5.29 0 1 1 5.29-5.29 5.29 5.29 0 0 1-5.29 5.29M87.35 29a2.56 2.56 0 0 0 4.93-1V2.91a2.62 2.62 0 0 0-5.24 0v7.4l-.19-.16a10.55 10.55 0 0 0-5.64-1.63 11 11 0 1 0 5.89 20.37l.17-.11zm-6.1-4.21a5.2 5.2 0 1 1 5.2-5.2 5.21 5.21 0 0 1-5.2 5.2m43.54-12.95C123 10 120.45 9 117.11 9a11.92 11.92 0 0 0-4.56 1.16 3 3 0 0 0-5.81.81v17a3 3 0 0 0 6 0V19c0-1.88 1.39-3.91 4.43-3.91s4.23 2.09 4.23 3.58v9.41a3 3 0 0 0 6 0V19a10.45 10.45 0 0 0-2.52-7.21M18.62 8.21a2.79 2.79 0 0 0-2.73 1.94l-5.13 11.06-5.14-11.06a2.79 2.79 0 0 0-2.73-1.94 2.89 2.89 0 0 0-2.52 4.31l8 17.09a2.3 2.3 0 0 0 2.43 1.32 2.31 2.31 0 0 0 2.43-1.33l7.94-17.07a2.89 2.89 0 0 0-2.51-4.32m80.85.02a3 3 0 0 0-3 2.89v16.72a3 3 0 1 0 6 0V11.12a3 3 0 0 0-3-2.9m0-8.11a3.13 3.13 0 1 1-3.12 3.13A3.12 3.12 0 0 1 99.51.11' />
        <path d='M145.7 19.31a2 2 0 0 0-2-2h-.49a1.93 1.93 0 0 1-1.92-1.93V4.95A4.94 4.94 0 0 0 136.31 0h-3.81a1.9 1.9 0 0 0-.93 3.55 1.89 1.89 0 0 0 .93.24h1.19a1.93 1.93 0 0 1 1.93 1.93v9.44a4.07 4.07 0 0 0 3.46 4 6 6 0 0 0 1 .1v.09a6 6 0 0 0-1 .1 4.07 4.07 0 0 0-3.45 4v9.42a1.93 1.93 0 0 1-1.93 1.93h-1.19a1.89 1.89 0 0 0-.93.24 1.9 1.9 0 0 0 .93 3.55h3.81a4.94 4.94 0 0 0 4.94-4.95V23.31a1.93 1.93 0 0 1 1.92-1.93h.49a2 2 0 0 0 2-2z' />
      </svg>
    `;
    const closeIcon = `
      <svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' preserveAspectRatio='xMidYMid meet'>
        <path d='M12.96 4.46l-1.42-1.42-3.54 3.55-3.54-3.55-1.42 1.42 3.55 3.54-3.55 3.54 1.42 1.42 3.54-3.55 3.54 3.55 1.42-1.42-3.55-3.54 3.55-3.54z'></path>
      </svg>`;

    const styles = `
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
                
                .logo, .message {
                    margin-right: 10px;
                }
                a {
                    color: white;
                    text-decoration: underline;
                }
                
                .close {
                    color: white;
                    cursor: pointer;
                }

                svg {
                  height: 1em; /* Match the current font size */
                  width: auto; /* Maintain aspect ratio */
                  vertical-align: text-bottom;
                  fill: white;
                }
            </style>`;

    shadowRoot.innerHTML = `
            ${styles}
            <div class='container'>
                <span class='logo'>${vaadinLogo}</span>
                <span class='message'>
                    Commercial features require a subscription, <a href='https://vaadin.com/pricing' target='_blank' class='link'>read more</a>.
                </span>
                <span class='close'>${closeIcon}</span>
            </div>
        `;

    shadowRoot.querySelector('.close').addEventListener('click', () => {
      this.remove();
    });
  }

}

// Register the custom element
customElements.define('vaadin-commercial-banner', VaadinCommercialBanner);
