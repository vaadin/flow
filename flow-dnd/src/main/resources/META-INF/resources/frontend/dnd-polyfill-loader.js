if (
  (/iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream) ||
  (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
) {
  window.Vaadin.__forceApplyMobileDragDrop = true;
  import('./vaadin-mobile-drag-drop.js');
}
