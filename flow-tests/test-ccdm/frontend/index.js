window.createVaadinButton = async function() {
  await import('@vaadin/vaadin-button');
  const vaadinButton = document.createElement('vaadin-button');
  vaadinButton.textContent = 'Vaadin Button';
  document.getElementById('contentFromJs').appendChild(vaadinButton);
}