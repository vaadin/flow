import { css } from 'lit';

export const sharedStyles = css`
  :host {
    display: block;
  }

  .property {
    display: flex;
    align-items: baseline;
    padding: var(--theme-editor-section-horizontal-padding);
  }

  .property .property-name {
    flex: 0 0 auto;
    width: 100px;
  }

  .property .property-name .modified {
    display: inline-block;
    width: 6px;
    height: 6px;
    background: orange;
    border-radius: 3px;
    margin-left: 3px;
  }
  
  .property .property-editor {
    flex: 1 1 0;
  }

  .input {
    width: 100%;
    box-sizing: border-box;
    padding: 0.25rem 0.375rem;
    color: inherit;
    background: rgba(0, 0, 0, 0.2);
    border-radius: 0.25rem;
    border: none;
  }
`;
