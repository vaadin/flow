/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/*
 * Browser feature support that server-side APIs rely on: installs the
 * helpers (window.Vaadin.Flow.elementResize, clipboard, download, ...) and
 * their listeners, and collects the browser details sent to the server.
 * Shared by Flow.ts and the bootstrap of exported web components, so that
 * both entry points get the same functionality.
 */
import './Clipboard';
import './Download';
import './ElementResize';
import { currentFullscreenState } from './Fullscreen';
import './Geolocation';
import { currentVisibility } from './PageVisibility';
import { currentScreenOrientationAngle, currentScreenOrientationType } from './ScreenOrientation';
import './WakeLock';
import { isShareSupported } from './WebShare';

const $wnd = window as any;

/**
 * Collects browser and device details (screen size, timezone, initial state
 * of the browser features above, etc.) as parameters for the server.
 */
export async function collectBrowserDetails(): Promise<Record<string, string>> {
  const params: Record<string, any> = {};

  /* Screen height and width */
  params['v-sh'] = $wnd.screen.height;
  params['v-sw'] = $wnd.screen.width;
  /* Browser window dimensions */
  params['v-wh'] = $wnd.innerHeight;
  params['v-ww'] = $wnd.innerWidth;
  /* Body element dimensions */
  params['v-bh'] = $wnd.document.body.clientHeight;
  params['v-bw'] = $wnd.document.body.clientWidth;

  /* Current time */
  const date = new Date();
  params['v-curdate'] = date.getTime();

  /* Current timezone offset (including DST shift) */
  const tzo1 = date.getTimezoneOffset();

  /* Compare the current tz offset with the first offset from the end
     of the year that differs --- if less that, we are in DST, otherwise
     we are in normal time */
  let dstDiff = 0;
  let rawTzo = tzo1;
  for (let m = 12; m > 0; m -= 1) {
    date.setUTCMonth(m);
    const tzo2 = date.getTimezoneOffset();
    if (tzo1 !== tzo2) {
      dstDiff = tzo1 > tzo2 ? tzo1 - tzo2 : tzo2 - tzo1;
      rawTzo = tzo1 > tzo2 ? tzo1 : tzo2;
      break;
    }
  }

  /* Time zone offset */
  params['v-tzo'] = tzo1;

  /* DST difference */
  params['v-dstd'] = dstDiff;

  /* Time zone offset without DST */
  params['v-rtzo'] = rawTzo;

  /* DST in effect? */
  params['v-dston'] = tzo1 !== rawTzo;

  /* Time zone id (if available) */
  try {
    params['v-tzid'] = Intl.DateTimeFormat().resolvedOptions().timeZone;
  } catch (err) {
    params['v-tzid'] = '';
  }

  /* Window name */
  if ($wnd.name) {
    params['v-wn'] = $wnd.name;
  }

  /* Detect touch device support */
  let supportsTouch = false;
  try {
    $wnd.document.createEvent('TouchEvent');
    supportsTouch = true;
  } catch (e) {
    /* Chrome and IE10 touch detection */
    supportsTouch = 'ontouchstart' in $wnd || typeof $wnd.navigator.msMaxTouchPoints !== 'undefined';
  }
  params['v-td'] = supportsTouch;

  /* Device Pixel Ratio */
  params['v-pr'] = $wnd.devicePixelRatio;

  if ($wnd.navigator.platform) {
    params['v-np'] = $wnd.navigator.platform;
  }

  /* Color scheme from CSS color-scheme property */
  const colorScheme = getComputedStyle(document.documentElement).colorScheme.trim();
  // "normal" is the default value and means no color scheme is set
  params['v-cs'] = colorScheme && colorScheme !== 'normal' ? colorScheme : '';
  /* Page visibility — initial state of document.hidden / document.hasFocus() */
  params['v-pv'] = currentVisibility();
  /* Fullscreen state — initial state of document.fullscreenEnabled / .fullscreenElement */
  params['v-fs'] = currentFullscreenState();

  /* Screen orientation — initial state of screen.orientation, empty
     when the Screen Orientation API is unavailable. */
  params['v-so'] = currentScreenOrientationType();
  params['v-soa'] = currentScreenOrientationAngle();

  /* Theme name - detect which theme is in use */
  const computedStyle = getComputedStyle(document.documentElement);
  let themeName = '';
  if (computedStyle.getPropertyValue('--vaadin-lumo-theme').trim()) {
    themeName = 'lumo';
  } else if (computedStyle.getPropertyValue('--vaadin-aura-theme').trim()) {
    themeName = 'aura';
  }
  params['v-tn'] = themeName;

  /* Geolocation availability — guarded because tests may reset
     window.Vaadin between runs, removing the namespace that
     Geolocation.ts installs at import time. */
  const geolocation = $wnd.Vaadin.Flow?.geolocation;
  if (geolocation) {
    params['v-ga'] = await geolocation.queryAvailability();
  }

  /* Wake-lock availability — same guard rationale as geolocation. */
  const wakeLock = $wnd.Vaadin.Flow?.wakeLock;
  if (wakeLock) {
    params['v-wla'] = wakeLock.queryAvailability();
  }

  /* Web Share API support */
  params['v-ws'] = isShareSupported();

  /* Stringify each value (they are parsed on the server side) */
  const stringParams: Record<string, string> = {};
  Object.keys(params).forEach((key) => {
    const value = params[key];
    if (typeof value !== 'undefined') {
      stringParams[key] = value.toString();
    }
  });
  return stringParams;
}

$wnd.Vaadin ??= {};
$wnd.Vaadin.Flow ??= {};
// Used by ExtendedClientDetails.refresh() on the server
$wnd.Vaadin.Flow.getBrowserDetailsParameters = collectBrowserDetails;
