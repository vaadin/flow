import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import '../../../styles/shared-styles.js';
import './order-status-badge.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
class OrderCard extends PolymerElement {
  static get template() {
    return html`
    <style include="shared-styles">
      :host {
        display: block;
      }

      .content {
        display: block;
        width: 100%;
        margin-left: auto;
        margin-right: auto;
      }

      .wrapper {
        background: var(--lumo-base-color);
        background-image: linear-gradient(var(--lumo-tint-5pct), var(--lumo-tint-5pct));
        box-shadow: 0 3px 5px var(--lumo-shade-10pct);
        border-bottom: 1px solid var(--lumo-shade-10pct);
        display: flex;
        padding: var(--lumo-space-l) var(--lumo-space-m);
        cursor: pointer;
      }

      .main {
        color: var(--lumo-secondary-text-color);
        margin-right: var(--lumo-space-s);
        font-weight: bold;
      }

      .group-heading {
        margin: var(--lumo-space-l) var(--lumo-space-m) var(--lumo-space-s);
      }

      .secondary {
        color: var(--lumo-secondary-text-color);
      }

      .info-wrapper {
        display: flex;
        flex-direction: column-reverse;
        justify-content: flex-end;
      }

      .badge {
        margin: var(--lumo-space-s) 0;
        width: 100px;
      }

      .time-place {
        width: 120px;
      }

      .name-items {
        flex: 1;
      }

      .place,
      .secondary-time,
      .full-day,
      .goods {
        color: var(--lumo-secondary-text-color);
      }

      .time,
      .name,
      .short-day,
      .month {
        margin: 0;
      }

      .name {
        word-break: break-all;
        /* Non standard for WebKit */
        word-break: break-word;
        white-space: normal;
      }

      .goods {
        display: flex;
        flex-wrap: wrap;
      }

      .goods > div {
        box-sizing: border-box;
        width: 18em;
        flex: auto;
        padding-right: var(--lumo-space-l);
      }

      .goods-item {
        display: flex;
        align-items: baseline;
        font-size: var(--lumo-font-size-s);
        margin: var(--lumo-space-xs) 0;
      }

      .goods-item > .count {
        margin-right: var(--lumo-space-s);
        white-space: nowrap;
      }

      .goods-item > div {
        flex: auto;
        word-break: break-all;
        /* Non standard for WebKit */
        word-break: break-word;
        white-space: normal;
      }

      @media (min-width: 600px) {
        .info-wrapper {
          flex-direction: row;
        }

        .wrapper {
          border-radius: var(--lumo-border-radius);
        }

        .badge {
          margin: 0;
        }

        .content {
          max-width: 964px;
        }
      }
    </style>
    <div class="content">
      <div class="group-heading" hidden$="[[!header]]">
        <span class="main">[[header.main]]</span>
        <span class="secondary">[[header.secondary]]</span>
      </div>
      <div class="wrapper" on-click="_cardClick">
        <div class="info-wrapper">
          <order-status-badge class="badge" status="[[orderCard.state]]"></order-status-badge>
          <div class="time-place">
            <h3 class="time">[[orderCard.time]]</h3>
            <h3 class="short-day">[[orderCard.shortDay]]</h3>
            <h3 class="month">[[orderCard.month]]</h3>
            <div class="secondary-time">[[orderCard.secondaryTime]]</div>
            <div class="full-day">[[orderCard.fullDay]]</div>
            <div class="place">[[orderCard.place]]</div>
          </div>
        </div>
        <div class="name-items">
          <h3 class="name">[[orderCard.fullName]]</h3>
          <div class="goods">
            <template is="dom-repeat" items="[[orderCard.items]]">
              <div class="goods-item">
                <span class="count">[[item.quantity]]</span>
                <div>[[item.product.name]]</div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>
`;
  }

  static get is() {
    return 'order-card';
  }

  _cardClick() {
    this.dispatchEvent(new CustomEvent('card-click'));
  }
}

window.customElements.define(OrderCard.is, OrderCard);
