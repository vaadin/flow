import { elementUpdated, expect, fixture, html } from '@open-wc/testing';
import sinon from 'sinon';
import './class-name-editor';

describe('class name editor', () => {
  let editor: HTMLElement;
  let classNameChangeEventSpy: sinon.SinonSpy;

  function getInput() {
    return editor
      .shadowRoot!.querySelector('vaadin-dev-tools-theme-text-input')!
      .shadowRoot!.querySelector('input') as HTMLInputElement;
  }

  function getErrorMessage() {
    return editor.shadowRoot!.querySelector('.error') as HTMLElement | null;
  }

  async function editClassName(className: string) {
    const input = getInput();
    input.value = className;
    input.dispatchEvent(new Event('change'));
    await elementUpdated(editor);
  }

  beforeEach(async () => {
    classNameChangeEventSpy = sinon.spy();
    editor = await fixture(html` <vaadin-dev-tools-theme-class-name-editor
      .className=${'test-class'}
      @class-name-change=${classNameChangeEventSpy}
    ></vaadin-dev-tools-theme-class-name-editor>`);
  });

  it('should show class name', () => {
    expect(getInput().value).to.equal('test-class');
  });

  it('should update class name', async () => {
    editor.className = 'custom-class';
    await elementUpdated(getInput());

    expect(getInput().value).to.equal('custom-class');
  });

  it('should dispatch class name change event', async () => {
    await editClassName('custom-class');

    expect(classNameChangeEventSpy.calledOnce).to.be.true;
    expect(classNameChangeEventSpy.args[0][0].detail.value).to.equal('custom-class');
  });

  it('should not dispatch class name change event if class name did not change', async () => {
    await editClassName('test-class');

    expect(classNameChangeEventSpy.called).to.be.false;
  });

  it('should show error message if class name is invalid', async () => {
    await editClassName('custom class');

    expect(getErrorMessage()).to.exist;

    await editClassName('custom-class');

    expect(getErrorMessage()).to.not.exist;
  });

  it('should validate class name', async () => {
    const invalidClassNames = [
      'custom class',
      ' custom-class',
      'custom-class ',
      '1custom-class',
      '--custom-class',
      '.custom-class',
      '#custom-class',
      '+custom-class'
    ];

    for (let i = 0; i < invalidClassNames.length; i++) {
      const className = invalidClassNames[i];
      await editClassName(className);
      expect(getErrorMessage()).to.exist;
    }
  });

  it('should not dispatch class name change event for invalid class name', async () => {
    await editClassName('custom class');

    expect(classNameChangeEventSpy.called).to.be.false;
  });

  it('should reset invalid state when setting className property', async () => {
    await editClassName('custom class');
    expect(getErrorMessage()).to.exist;

    editor.className = 'custom-class';
    await elementUpdated(editor);
    expect(getErrorMessage()).to.not.exist;
  });
});
