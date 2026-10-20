/*
 * A fixture the dev-loop ITs edit: DevLoopFrontendIT patches the string below
 * and asserts that `apply` escalates to a restart, because only a Vite build
 * can fold a frontend module into the dev bundle.
 *
 * Nothing imports it, for the same reason the theme is not activated: an
 * imported module is in the bundle's stats and would make every run in this
 * module pay a rebuild. The daemon cannot tell the difference and must not try
 * to - it has no dependencies, so it cannot read stats.json - so every
 * non-theme frontend file is treated as bundled. That conservative rule is
 * exactly what this fixture pins.
 */
export function greeting(): string {
  return 'hello from the frontend';
}
