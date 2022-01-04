import { color } from '@vaadin/vaadin-lumo-styles';
import { typography } from '@vaadin/vaadin-lumo-styles';

const tpl = document.createElement('template');
tpl.innerHTML = `<style>
  ${color.cssText}
  ${typography.cssText}
</style>`;
document.head.appendChild(tpl.content);
