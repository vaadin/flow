window.loadContent = async function() {
    await import('./another-bundle.js');
    const label = document.createElement('label');
    label.textContent = window.anotherBundle;
    document.getElementById('contentFromOtherBundle').appendChild(label);
}
