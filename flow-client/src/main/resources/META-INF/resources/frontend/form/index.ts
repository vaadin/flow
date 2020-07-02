export * from './Binder';
export * from './Field';
export * from './Models';
export * from './Validation';
export * from './Validators';

const $wnd = window as any;
$wnd.Vaadin = $wnd.Vaadin || {};
$wnd.Vaadin.registrations = $wnd.Vaadin.registrations || [];
$wnd.Vaadin.registrations.push({
  is: '@vaadin/form',
  version: /* updated-by-script */ '3.2-SNAPSHOT'
});
