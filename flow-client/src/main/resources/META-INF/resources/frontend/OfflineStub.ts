import {css, html, LitElement } from 'lit-element';

export class OfflineStub extends LitElement {

  static get styles() {
    return css`
      .page {
        font-family: -apple-system, BlinkMacSystemFont, "Roboto", "Segoe UI", Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
        font-size: 1rem;
        line-height: 1.625;
        font-weight: 300;
        -webkit-font-smoothing: antialiased;
        color: hsla(214, 96%, 96%, 0.9);
        background: hsl(214, 35%, 21%);
        word-break: break-word;
        padding: 0;
        margin: 0;
      }
      .offline {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        z-index: 10000;
        padding: 0 24px;
        min-height: 100vh;
      }

      .offline .content {
        width: 100%;
        max-width: 50em;
        margin: 0 auto;
      }

      .offline .content .message {
        flex: 1;
        box-sizing: border-box;
        height: 100%;
      }

      .offline .content .message h2 {
        text-align: center;
        font-size: 1.75rem;
        margin-bottom: 0.5em;
        font-weight: 600;
        color: hsl(214, 100%, 98%);
      }

      .offline .content .message p {
        margin-top: 0.5em;
        margin-bottom: 0.75em;
      }

      @media (min-width: 800px) {
        .offline {
            padding: 0 48px;
        }

        .offline .content .title h1,
        .offline .content .message h2 {
            text-align: left;
        }

        .offline .content .message {
            height: auto;
        }
      }
    `;
  }

  render() {
    return html`
      <div class="page">
        <div class="offline">
          <div class="content">
            <section class="message">
              <h2>You are offline</h2>
              <p>This route requires an internet connection to work. You do not seem to have access to the server right now. Check your internet connection and try reloading the page to use the application.</p>
            </section>
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define('vaadin-offline-stub', OfflineStub);
