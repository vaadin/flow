/*
 * A fixture the dev-loop ITs edit: DevLoopFrontendIT patches the string below
 * and asserts that `apply` escalates to a restart, because only a Vite build
 * can fold a frontend module into the dev bundle.
 *
 * Nothing imports it, for the same reason the theme is not activated: an
 * imported module is in the bundle's stats and would make every run in this
 * module pay a rebuild. greeting.ts next door is the imported one, and is a
 * fixture for a different question - whether a supertype's frontend imports
 * make an ordinary edit look like a frontend change.
 */
export function greeting(): string {
  return 'hello from the frontend';
}
