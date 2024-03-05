import { elementUpdated, expect, fixture, html } from '@open-wc/testing';
import sinon from 'sinon';
import { Overlay } from '@vaadin/overlay';
import '@vaadin/overlay';
import { RgbaStringColorPicker } from 'vanilla-colorful/rgba-string-color-picker';
import { ColorPicker, ColorPickerOverlayContent } from './color-picker';
import './color-picker';

describe('color-picker', () => {
  let colorPicker: ColorPicker;
  let colorChangeSpy: sinon.SinonSpy;
  let commitSpy: sinon.SinonSpy;
  let cancelSpy: sinon.SinonSpy;

  beforeEach(async () => {
    colorPicker = await fixture(html` <vaadin-dev-tools-color-picker></vaadin-dev-tools-color-picker>`);
    colorChangeSpy = sinon.spy();
    commitSpy = sinon.spy();
    cancelSpy = sinon.spy();
    colorPicker.addEventListener('color-picker-change', colorChangeSpy);
    colorPicker.addEventListener('color-picker-commit', commitSpy);
    colorPicker.addEventListener('color-picker-cancel', cancelSpy);
  });

  function getToggleButton() {
    return colorPicker.shadowRoot!.querySelector('#toggle') as HTMLElement;
  }

  function getOverlay() {
    return document.body.querySelector('vaadin-dev-tools-color-picker-overlay') as Overlay;
  }

  function getOverlayContent() {
    return getOverlay().querySelector('vaadin-dev-tools-color-picker-overlay-content') as ColorPickerOverlayContent;
  }

  async function openOverlay() {
    getToggleButton().click();
    await elementUpdated(getOverlayContent());
  }

  function getInternalPicker() {
    return getOverlayContent().shadowRoot!.querySelector(
      'vaadin-dev-tools-rgba-string-color-picker'
    ) as RgbaStringColorPicker;
  }

  function changeInternalPickerColor(color: string) {
    getInternalPicker().dispatchEvent(new CustomEvent('color-changed', { detail: { value: color } }));
  }

  function getSwatchesContainer() {
    return getOverlayContent().shadowRoot!.querySelector('.swatches') as HTMLElement;
  }

  function getSwatches() {
    return Array.from(getSwatchesContainer().querySelectorAll('.preview')) as HTMLElement[];
  }

  it('should show color preview', async () => {
    colorPicker.value = '#ff0000';
    await elementUpdated(colorPicker);

    const previewStyles = getComputedStyle(getToggleButton(), '::after');
    expect(previewStyles.backgroundColor).to.equal('rgb(255, 0, 0)');
  });

  it('should open overlay when clicking toggle button', () => {
    getToggleButton().click();

    const overlay = getOverlay();
    expect(overlay).to.exist;
    expect(overlay.opened).to.be.true;
  });

  it('should pass RGB color to internal color picker', async () => {
    colorPicker.value = '#ff0000';
    await elementUpdated(colorPicker);
    await openOverlay();

    const picker = getInternalPicker();
    expect(picker.color).to.equal('rgb(255, 0, 0)');
  });

  it('should update internal color picker when value changes', async () => {
    colorPicker.value = '#ff0000';
    await elementUpdated(colorPicker);
    await openOverlay();

    const picker = getInternalPicker();
    expect(picker.color).to.equal('rgb(255, 0, 0)');

    colorPicker.value = '#0000ff';
    await elementUpdated(colorPicker);
    expect(picker.color).to.equal('rgb(0, 0, 255)');
  });

  it('should dispatch change event when internal picker changes', async () => {
    colorPicker.value = '#ff0000';
    await elementUpdated(colorPicker);
    await openOverlay();

    changeInternalPickerColor('rgba(0, 255, 0, 1)');

    expect(colorChangeSpy.calledOnce).to.be.true;
    expect(colorChangeSpy.args[0][0].detail.value).to.be.equal('rgba(0, 255, 0, 1)');
  });

  it('should dispatch cancel event when closing overlay without modifying value', async () => {
    await openOverlay();

    getOverlay().dispatchEvent(new CustomEvent('vaadin-overlay-close'));

    expect(commitSpy.called).to.be.false;
    expect(cancelSpy.calledOnce).to.be.true;
  });

  it('should dispatch cancel event when closing overlay with escape', async () => {
    await openOverlay();

    changeInternalPickerColor('rgba(0, 255, 0, 1)');

    getOverlay().dispatchEvent(new CustomEvent('vaadin-overlay-escape-press'));
    getOverlay().dispatchEvent(new CustomEvent('vaadin-overlay-close'));

    expect(commitSpy.called).to.be.false;
    expect(cancelSpy.calledOnce).to.be.true;
  });

  it('should dispatch commit event when closing overlay after modifying value', async () => {
    await openOverlay();

    changeInternalPickerColor('rgba(0, 255, 0, 1)');

    getOverlay().dispatchEvent(new CustomEvent('vaadin-overlay-close'));

    expect(commitSpy.calledOnce).to.be.true;
    expect(cancelSpy.called).to.be.false;
  });

  it('should not render swatches if there are no presets', async () => {
    await openOverlay();

    expect(getSwatchesContainer()).to.not.exist;
  });

  it('should render swatches if there are presets', async () => {
    function assertSwatchColor(swatch: HTMLElement, color: string) {
      const style = getComputedStyle(swatch, '::after');
      expect(style.backgroundColor).to.equal(color);
    }

    colorPicker.presets = ['#f00', '#0f0', '#00f'];
    await elementUpdated(colorPicker);
    await openOverlay();

    expect(getSwatchesContainer()).to.exist;

    const swatches = getSwatches();
    expect(swatches.length).to.equal(3);
    assertSwatchColor(swatches[0], 'rgb(255, 0, 0)');
    assertSwatchColor(swatches[1], 'rgb(0, 255, 0)');
    assertSwatchColor(swatches[2], 'rgb(0, 0, 255)');
  });

  it('should dispatch change event when selecting swatch', async () => {
    colorPicker.presets = ['#f00', '#0f0', '#00f'];
    await elementUpdated(colorPicker);
    await openOverlay();

    const swatches = getSwatches();
    swatches[1].click();

    expect(colorChangeSpy.calledOnce).to.be.true;
    expect(colorChangeSpy.args[0][0].detail.value).to.be.equal('#0f0');
  });
});
