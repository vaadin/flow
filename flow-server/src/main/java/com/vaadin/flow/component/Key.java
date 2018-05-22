/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.component;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * An interface to represent keyboard keys.
 * <p>
 * See
 * https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent/key/Key_Values
 */
@FunctionalInterface
public interface Key extends Serializable {

    /**
     * The user agent wasn't able to map the event's virtual keycode to a
     * specific key value. This can happen due to hardware or software
     * constraints, or because of constraints around the platform on which the
     * user agent is running.
     */
    Key UNIDENTIFIED = Key.of("Unidentified");

    /**
     * The <code>Alt</code> (Alternative) key.
     */
    Key ALT = Key.of("Alt");

    /**
     * The <code>AltGr</code> or <code>AltGraph</code> (Alternate Graphics) key.
     * Enables the ISO Level 3 shift modifier (where <code>Shift</code> is the
     * level 2 modifier).
     */
    Key ALT_GRAPH = Key.of("AltGraph");

    /**
     * The <code>Caps Lock</code> key. Toggles the capital character lock on and
     * off for subsequent input.
     */
    Key CAPS_LOCK = Key.of("CapsLock");

    /**
     * The <code>Control</code>, <code>Ctrl</code>, or <code>Ctl</code> key.
     * Allows typing control characters.
     */
    Key CONTROL = Key.of("Control");

    /**
     * The <code>Fn</code> (Function modifier) key. Used to allow generating
     * function key (<code>F1</code>-<code>F15</code>, for instance) characters
     * on keyboards without a dedicated function key area. Often handled in
     * hardware so that events aren't generated for this key.
     */
    Key FN = Key.of("Fn");

    /**
     * The <code>FnLock</code> or <code>F-Lock</code> (Function Lock) key.
     * Toggles the function key mode described by "Fn" on and off. Often handled
     * in hardware so that events aren't generated for this key.
     */
    Key FN_LOCK = Key.of("FnLock");

    /**
     * The <code>Hyper</code> key.
     */
    Key HYPER = Key.of("Hyper");

    /**
     * The <code>Meta</code> key. Allows issuing special command inputs. This is
     * the <code>Windows</code> logo key, or the <code>Command</code> or
     * <code>⌘</code> key on Mac keyboards.
     */
    Key META = Key.of("Meta");

    /**
     * The <code>NumLock</code> (Number Lock) key. Toggles the numeric keypad
     * between number entry some other mode (often directional arrows).
     */
    Key NUM_LOCK = Key.of("NumLock");

    /**
     * The <code>Scroll Lock</code> key. Toggles beteen scrolling and cursor
     * movement modes.
     */
    Key SCROLL_LOCK = Key.of("ScrollLock");

    /**
     * The <code>Shift</code> key. Modifies keystrokes to allow typing upper (or
     * other) case letters, and to support typing punctuation and other special
     * characters.
     */
    Key SHIFT = Key.of("Shift");

    /**
     * The <code>Super</code> key.
     */
    Key SUPER = Key.of("Super");

    /**
     * The <code>Symbol</code> modifier key (found on certain virtual
     * keyboards).
     */
    Key SYMBOL = Key.of("Symbol");

    /**
     * The <code>Symbol Lock</code> key.
     */
    Key SYMBOL_LOCK = Key.of("SymbolLock");

    /**
     * The <code>Enter</code> or <code>↵</code> key (sometimes labeled
     * <code>Return</code>).
     */
    Key ENTER = Key.of("Enter");

    /**
     * The Horizontal Tab key, <code>Tab</code>.
     */
    Key TAB = Key.of("Tab");

    /**
     * The space key, <code>Space Bar</code>.
     */
    Key SPACE = Key.of(" ");

    /**
     * The down arrow key.
     */
    Key ARROW_DOWN = Key.of("ArrowDown");

    /**
     * The left arrow key.
     */
    Key ARROW_LEFT = Key.of("ArrowLeft");

    /**
     * The right arrow key.
     */
    Key ARROW_RIGHT = Key.of("ArrowRight");

    /**
     * The up arrow key.
     */
    Key ARROW_UP = Key.of("ArrowUp");

    /**
     * The <code>End</code> key. Moves to the end of content.
     */
    Key END = Key.of("End");

    /**
     * The <code>Home</code> key. Moves to the start of content.
     */
    Key HOME = Key.of("Home");

    /**
     * The <code>Page Down</code> (or <code>PgDn</code>) key. Scrolls down or
     * displays the next page of content.
     */
    Key PAGE_DOWN = Key.of("PageDown");

    /**
     * The <code>Page Up</code> (or <code>PgUp</code>) key. Scrolls up or
     * displays the previous page of content.
     */
    Key PAGE_UP = Key.of("PageUp");

    /**
     * The <code>Backspace</code> key. This key is labeled <code>Delete</code>
     * on Mac keyboards.
     */
    Key BACKSPACE = Key.of("Backspace");

    /**
     * The <code>Clear</code> key. Removes the currently selected input.
     */
    Key CLEAR = Key.of("Clear");

    /**
     * The <code>Copy</code> key (on certain extended keyboards).
     */
    Key COPY = Key.of("Copy");

    /**
     * The Cursor Select key, <code>CrSel</code>.
     */
    Key CR_SEL = Key.of("CrSel");

    /**
     * The <code>Cut</code> key (on certain extended keyboards).
     */
    Key CUT = Key.of("Cut");

    /**
     * The Delete key, <code>Del</code>.
     */
    Key DELETE = Key.of("Delete", "Del");

    /**
     * Erase to End of Field. Deletes all characters from the current cursor
     * position to the end of the current field.
     */
    Key ERASE_EOF = Key.of("EraseEof");

    /**
     * The <code>ExSel</code> (Extend Selection) key.
     */
    Key EX_SEL = Key.of("ExSel");

    /**
     * The Insert key, <code>Ins</code>. Toggles&nbsp; between inserting and
     * overwriting text.
     */
    Key INSERT = Key.of("Insert");

    /**
     * Paste from the clipboard.
     */
    Key PASTE = Key.of("Paste");

    /**
     * Redo the last action.
     */
    Key REDO = Key.of("Redo");

    /**
     * Undo the last action.
     */
    Key UNDO = Key.of("Undo");

    /**
     * The <code>Accept</code>, <code>Commit</code>, or <code>OK</code> key or
     * button. Accepts the currently selected option or input method sequence
     * conversion.
     */
    Key ACCEPT = Key.of("Accept");

    /**
     * The <code>Again</code> key. Redoes or repeats a previous action.
     */
    Key AGAIN = Key.of("Again");

    /**
     * The <code>Attn</code> (Attention) key.
     */
    Key ATTN = Key.of("Attn");

    /**
     * The <code>Cancel</code> key.
     */
    Key CANCEL = Key.of("Cancel");

    /**
     * Shows the context menu. Typically found between the <code>Windows</code>
     * (or <code>OS</code>) key and the <code>Control</code> key on the right
     * side of the keyboard.
     */
    Key CONTEXT_MENU = Key.of("ContextMenu");

    /**
     * The <code>Esc</code> (Escape) key. Typically used as an exit, cancel, or
     * "escape this operation" button. Historically, the Escape character was
     * used to signal the start of a special control sequence of characters
     * called an "escape sequence."
     */
    Key ESCAPE = Key.of("Escape", "Esc");

    /**
     * The <code>Execute</code> key.
     */
    Key EXECUTE = Key.of("Execute");

    /**
     * The <code>Find</code> key. Opens an interface (typically a dialog box)
     * for performing a find/search operation.
     */
    Key FIND = Key.of("Find");

    /**
     * The <code>Finish</code> key.
     */
    Key FINISH = Key.of("Finish");

    /**
     * The <code>Help</code> key. Opens or toggles the display of help
     * information.
     */
    Key HELP = Key.of("Help");

    /**
     * The <code>Pause</code> key. Pauses the current application or state, if
     * applicable. Note: This shouldn't be confused with the
     * <code>"MediaPause"</code> key value, which is used for media controllers,
     * rather than to control applications and processes.
     *
     */
    Key PAUSE = Key.of("Pause");

    /**
     * The <code>Play</code> key. Resumes a previously paused application, if
     * applicable. Note: This shouldn't be confused with the
     * <code>"MediaPlay"</code> key value, which is used for media controllers,
     * rather than to control applications and processes.
     *
     */
    Key PLAY = Key.of("Play");

    /**
     * The <code>Props</code> (Properties) key.
     */
    Key PROPS = Key.of("Props");

    /**
     * The <code>Select</code> key.
     */
    Key SELECT = Key.of("Select");

    /**
     * The <code>ZoomIn</code> key.
     */
    Key ZOOM_IN = Key.of("ZoomIn");

    /**
     * The <code>ZoomOut</code> key.
     */
    Key ZOOM_OUT = Key.of("ZoomOut");

    /**
     * The Brightness Down key. Typically used to reduce the brightness of the
     * display.
     */
    Key BRIGHTNESS_DOWN = Key.of("BrightnessDown");

    /**
     * The Brightness Up key. Typically increases the brightness of the display.
     */
    Key BRIGHTNESS_UP = Key.of("BrightnessUp");

    /**
     * The <code>Eject</code> key. Ejects removable media (or toggles an optical
     * storage device tray open and closed).
     */
    Key EJECT = Key.of("Eject");

    /**
     * The <code>LogOff</code> key.
     */
    Key LOG_OFF = Key.of("LogOff");

    /**
     * The <code>Power</code> button or key, to toggle power on and off.
     * <p>
     * Note: Not all systems pass this key through to to the user agent.
     */
    Key POWER = Key.of("Power");

    /**
     * The <code>PowerOff</code> or <code>PowerDown</code> key. Shuts off the
     * system.
     */
    Key POWER_OFF = Key.of("PowerOff");

    /**
     * The <code>PrintScreen</code> or <code>PrtScr</code> key. Sometimes
     * <code>SnapShot</code>. Captures the screen and prints it or saves it to
     * disk.
     */
    Key PRINT_SCREEN = Key.of("PrintScreen");

    /**
     * The <code>Hibernate</code> key. This saves the state of the computer to
     * disk and then shuts down; the computer can be returned to its previous
     * state by restoring the saved state information.
     */
    Key HIBERNATE = Key.of("Hibernate");

    /**
     * The <code>Standby</code> key; also known as <code>Suspend</code> or
     * <code>Sleep</code>. This turns off the display and puts the computer in a
     * low power consumption mode, without completely powering off.
     */
    Key STANDBY = Key.of("Standby");

    /**
     * The <code>WakeUp</code> key; used to wake the computer from the
     * hibernation or standby modes.
     */
    Key WAKE_UP = Key.of("WakeUp");

    /**
     * The <code>All Candidates</code> key, which starts multi-candidate mode,
     * in which multiple candidates are displayed for the ongoing input.
     */
    Key ALL_CANDIDATES = Key.of("AllCandidates");

    /**
     * The <code>Alphanumeric</code> key.
     */
    Key ALPHANUMERIC = Key.of("Alphanumeric");

    /**
     * The <code>Code Input</code> key, which enables code input mode, which
     * lets the user enter characters by typing their code points (their Unicode
     * character numbers, typically).
     */
    Key CODE_INPUT = Key.of("CodeInput");

    /**
     * The <code>Compose</code> key.
     */
    Key COMPOSE = Key.of("Compose");

    /**
     * The <code>Convert</code> key, which instructs the IME to convert the
     * current input method sequence into the resulting character.
     */
    Key CONVERT = Key.of("Convert");

    /**
     * A dead "combining" key; that is, a key which is used in tandem with other
     * keys to generate accented and other modified characters.
     */
    Key DEAD = Key.of("Dead");

    /**
     * The <code>Final</code> (Final Mode) key is used on some Asian keyboards
     * to enter final mode when using IMEs.
     */
    Key FINAL_MODE = Key.of("FinalMode");

    /**
     * Switches to the first character group on an
     * <a href="https://en.wikipedia.org/wiki/ISO/IEC_9995">ISO/IEC 9995
     * keyboard</a>. Each key may have multiple groups of characters, each in
     * its own column. Pressing this key instructs the device to interpret
     * keypresses as coming from the first column on subsequent keystrokes.
     */
    Key GROUP_FIRST = Key.of("GroupFirst");

    /**
     * Switches to the last character group on an
     * <a href="https://en.wikipedia.org/wiki/ISO/IEC_9995">ISO/IEC 9995
     * keyboard</a>.
     */
    Key GROUP_LAST = Key.of("GroupLast");

    /**
     * Switches to the next character group on an
     * <a href="https://en.wikipedia.org/wiki/ISO/IEC_9995">ISO/IEC 9995
     * keyboard</a>.
     */
    Key GROUP_NEXT = Key.of("GroupNext");

    /**
     * Switches to the previous character group on an
     * <a href="https://en.wikipedia.org/wiki/ISO/IEC_9995">ISO/IEC 9995
     * keyboard</a>.
     */
    Key GROUP_PREVIOUS = Key.of("GroupPrevious");

    /**
     * The Mode Change key. Toggles or cycles among input modes of IMEs.
     */
    Key MODE_CHANGE = Key.of("ModeChange");

    /**
     * The Next Candidate function key. Selects the next possible match for the
     * ongoing input.
     */
    Key NEXT_CANDIDATE = Key.of("NextCandidate");

    /**
     * The <code>NonConvert</code> ("Don't convert") key. This accepts the
     * current input method sequence without running conversion when using an
     * IME.
     */
    Key NON_CONVERT = Key.of("NonConvert");

    /**
     * The Previous Candidate key. Selects the previous possible match for the
     * ongoing input.
     */
    Key PREVIOUS_CANDIDATE = Key.of("PreviousCandidate");

    /**
     * The <code>Process</code> key. Instructs the IME to process the
     * conversion.
     */
    Key PROCESS = Key.of("Process");

    /**
     * The Single Candidate key. Enables single candidate mode (as opposed to
     * multi-candidate mode); in this mode, only one candidate is displayed at a
     * time.
     */
    Key SINGLE_CANDIDATE = Key.of("SingleCandidate");

    /**
     * The <code>Hangul</code> (Korean character set) mode key, which toggles
     * between Hangul and English entry modes.
     */
    Key HANGUL_MODE = Key.of("HangulMode");

    /**
     * Selects the Hanja mode, for converting Hangul characters to the more
     * specific Hanja characters.
     */
    Key HANJA_MODE = Key.of("HanjaMode");

    /**
     * Selects the Junja mode, in which Korean is represented using single-byte
     * Latin characters.
     */
    Key JUNJA_MODE = Key.of("JunjaMode");

    /**
     * The <code>Eisu</code> key. This key's purpose is defined by the IME, but
     * may be used to close the IME.
     */
    Key EISU = Key.of("Eisu");

    /**
     * The <code>Hankaku</code> (half-width characters) key.
     */
    Key HANKAKU = Key.of("Hankaku");

    /**
     * The <code>Hiragana</code> key; selects Kana characters mode.
     */
    Key HIRAGANA = Key.of("Hiragana");

    /**
     * Toggles between the Hiragana and Katakana writing systems.
     */
    Key HIRAGANA_KATAKANA = Key.of("HiraganaKatakana");

    /**
     * The <code>Kana Mode</code> (Kana Lock) key.
     */
    Key KANA_MODE = Key.of("KanaMode");

    /**
     * The <code>Kanji Mode</code> key. Enables entering Japanese text using the
     * ideographic characters of Chinese origin.
     */
    Key KANJI_MODE = Key.of("KanjiMode");

    /**
     * The <code>Katakana</code> key.
     */
    Key KATAKANA = Key.of("Katakana");

    /**
     * The <code>Romaji</code> key; selects the Roman character set.
     */
    Key ROMAJI = Key.of("Romaji");

    /**
     * The <code>Zenkaku</code> (full width) characters key.
     */
    Key ZENKAKU = Key.of("Zenkaku");

    /**
     * The <code>Zenkaku/Hankaku</code> (full width/half width) toggle key.
     */
    Key ZENKAKU_HANAKU = Key.of("ZenkakuHanaku");

    /**
     * The first general-purpose function key, <code>F1</code>.
     */
    Key F1 = Key.of("F1");

    /**
     * The <code>F2</code> key.
     */
    Key F2 = Key.of("F2");

    /**
     * The <code>F3</code> key.
     */
    Key F3 = Key.of("F3");

    /**
     * The <code>F4</code> key.
     */
    Key F4 = Key.of("F4");

    /**
     * The <code>F5</code> key.
     */
    Key F5 = Key.of("F5");

    /**
     * The <code>F6</code> key.
     */
    Key F6 = Key.of("F6");

    /**
     * The <code>F7</code> key.
     */
    Key F7 = Key.of("F7");

    /**
     * The <code>F8</code> key.
     */
    Key F8 = Key.of("F8");

    /**
     * The <code>F9</code> key.
     */
    Key F9 = Key.of("F9");

    /**
     * The <code>F10</code> key.
     */
    Key F10 = Key.of("F10");

    /**
     * The <code>F11</code> key.
     */
    Key F11 = Key.of("F11");

    /**
     * The <code>F12</code> key.
     */
    Key F12 = Key.of("F12");

    /**
     * The <code>F13</code> key.
     */
    Key F13 = Key.of("F13");

    /**
     * The <code>F14</code> key.
     */
    Key F14 = Key.of("F14");

    /**
     * The <code>F15</code> key.
     */
    Key F15 = Key.of("F15");

    /**
     * The <code>F16</code> key.
     */
    Key F16 = Key.of("F16");

    /**
     * The <code>F17</code> key.
     */
    Key F17 = Key.of("F17");

    /**
     * The <code>F18</code> key.
     */
    Key F18 = Key.of("F18");

    /**
     * The <code>F19</code> key.
     */
    Key F19 = Key.of("F19");

    /**
     * The <code>F20</code> key.
     */
    Key F20 = Key.of("F20");

    /**
     * The first general-purpose virtual function key.
     */
    Key SOFT1 = Key.of("Soft1");

    /**
     * The second general-purpose virtual function key.
     */
    Key SOFT2 = Key.of("Soft2");

    /**
     * The third general-purpose virtual function key.
     */
    Key SOFT3 = Key.of("Soft3");

    /**
     * The fourth general-purpose virtual function key.
     */
    Key SOFT4 = Key.of("Soft4");

    /**
     * Presents a list of recently-used applications which lets the user change
     * apps quickly.
     */
    Key APP_SWITCH = Key.of("AppSwitch");

    /**
     * The <code>Call</code> key; dials the number which has been entered.
     */
    Key CALL = Key.of("Call");

    /**
     * The <code>Camera</code> key; activates the camera.
     */
    Key CAMERA = Key.of("Camera");

    /**
     * The <code>Focus</code> key; focuses the camera.
     */
    Key CAMERA_FOCUS = Key.of("CameraFocus");

    /**
     * The <code>End Call</code> or <code>Hang Up</code> button.
     */
    Key END_CALL = Key.of("EndCall");

    /**
     * The <code>Back</code> button.
     */
    Key GO_BACK = Key.of("GoBack");

    /**
     * The <code>Home</code> button, which takes the user to the phone's main
     * screen (usually an application launcher).
     */
    Key GO_HOME = Key.of("GoHome");

    /**
     * The <code>Headset Hook</code> key. This is typically actually a button on
     * the headset which is used to hang up calls and play or pause media.
     */
    Key HEADSET_HOOK = Key.of("HeadsetHook");

    /**
     * The <code>Redial</code> button, which redials the last-called number.
     */
    Key LAST_NUMBER_REDIAL = Key.of("LastNumberRedial");

    /**
     * The <code>Notification</code> key.
     */
    Key NOTIFICATION = Key.of("Notification");

    /**
     * A button which cycles among the notification modes: silent, vibrate,
     * ring, and so forth.
     */
    Key MANNER_MODE = Key.of("MannerMode");

    /**
     * The <code>Voice Dial</code> key. Initiates voice dialing.
     */
    Key VOICE_DIAL = Key.of("VoiceDial");

    /**
     * Switches to the previous channel.
     */
    Key CHANNEL_DOWN = Key.of("ChannelDown");

    /**
     * Switches to the next channel.
     */
    Key CHANNEL_UP = Key.of("ChannelUp");

    /**
     * Starts, continues, or increases the speed of fast forwarding the media.
     */
    Key MEDIA_FAST_FORWARD = Key.of("MediaFastForward");

    /**
     * Pauses the currently playing media. Some older applications use simply
     * "Pause" but this is not correct.
     */
    Key MEDIA_PAUSE = Key.of("MediaPause");

    /**
     * Starts or continues playing media at normal speed, if not already doing
     * so. Has no effect otherwise.
     */
    Key MEDIA_PLAY = Key.of("MediaPlay");

    /**
     * Toggles between playing and pausing the current media.
     */
    Key MEDIA_PLAY_PAUSE = Key.of("MediaPlayPause");

    /**
     * Starts or resumes recording media.
     */
    Key MEDIA_RECORD = Key.of("MediaRecord");

    /**
     * Starts, continues, or increases the speed of rewinding the media.
     */
    Key MEDIA_REWIND = Key.of("MediaRewind");

    /**
     * Stops the current media activity (such as playing, recording, pausing,
     * forwarding, or rewinding). Has no effect if the media is currently
     * stopped already.
     */
    Key MEDIA_STOP = Key.of("MediaStop");

    /**
     * Seeks to the next media or program track.
     */
    Key MEDIA_TRACK_NEXT = Key.of("MediaTrackNext");

    /**
     * Seeks to the previous media or program track.
     */
    Key MEDIA_TRACK_PREVIOUS = Key.of("MediaTrackPrevious");

    /**
     * Adjusts audio balance toward the left.
     */
    Key AUDIO_BALANCE_LEFT = Key.of("AudioBalanceLeft");

    /**
     * Adjusts audio balance twoard the right.
     */
    Key AUDIO_BALANCE_RIGHT = Key.of("AudioBalanceRight");

    /**
     * Decreases the amount of bass.
     */
    Key AUDIO_BASS_DOWN = Key.of("AudioBassDown");

    /**
     * Reduces bass boosting or cycles downward through bass boost modes or
     * states.
     */
    Key AUDIO_BASS_BOOST_DOWN = Key.of("AudioBassBoostDown");

    /**
     * Toggles bass boosting on and off.
     */
    Key AUDIO_BASS_BOOST_TOGGLE = Key.of("AudioBassBoostToggle");

    /**
     * Increases the amount of bass boosting, or cycles upward through a set of
     * bass boost modes or states.
     */
    Key AUDIO_BASS_BOOST_UP = Key.of("AudioBassBoostUp");

    /**
     * Increases the amount of bass.
     */
    Key AUDIO_BASS_UP = Key.of("AudioBassUp");

    /**
     * Adjusts the audio fader toward the front.
     */
    Key AUDIO_FADER_FRONT = Key.of("AudioFaderFront");

    /**
     * Adjusts the audio fader toward the rear.
     */
    Key AUDIO_FADER_REAR = Key.of("AudioFaderRear");

    /**
     * Selects the next available surround sound mode.
     */
    Key AUDIO_SURROUND_MODE_NEXT = Key.of("AudioSurroundModeNext");

    /**
     * Decreases the amount of treble.
     */
    Key AUDIO_TREBLE_DOWN = Key.of("AudioTrebleDown");

    /**
     * Increases the amount of treble.
     */
    Key AUDIO_TREBLE_UP = Key.of("AudioTrebleUp");

    /**
     * Decreases the audio volume.
     */
    Key AUDIO_VOLUME_DOWN = Key.of("AudioVolumeDown");

    /**
     * Mutes the audio.
     */
    Key AUDIO_VOLUME_MUTE = Key.of("AudioVolumeMute");

    /**
     * Increases the audio volume.
     */
    Key AUDIO_VOLUME_UP = Key.of("AudioVolumeUp");

    /**
     * Toggles the microphone on and off.
     */
    Key MICROPHONE_TOGGLE = Key.of("MicrophoneToggle");

    /**
     * Decreases the microphone's input volume.
     */
    Key MICROPHONE_VOLUME_DOWN = Key.of("MicrophoneVolumeDown");

    /**
     * Mutes the microphone input.
     */
    Key MICROPHONE_VOLUME_MUTE = Key.of("MicrophoneVolumeMute");

    /**
     * Increases the microphone's input volume.
     */
    Key MICROPHONE_VOLUME_UP = Key.of("MicrophoneVolumeUp");

    /**
     * Switches into TV viewing mode.
     */
    Key TV = Key.of("TV");

    /**
     * Toggles 3D TV mode on and off.
     */
    Key TV_3D_MODE = Key.of("TV3DMode");

    /**
     * Toggles between antenna and cable inputs.
     */
    Key TV_ANTENNA_CABLE = Key.of("TVAntennaCable");

    /**
     * Toggles audio description mode on and off.
     */
    Key TV_AUDIO_DESCRIPTION = Key.of("TVAudioDescription");

    /**
     * Decreases the audio description's mixing volume; reduces the volume of
     * the audio descriptions relative to the program sound.
     */
    Key TV_AUDIO_DESCRIPTION_MIX_DOWN = Key.of("TVAudioDescriptionMixDown");

    /**
     * Increases the audio description's mixing volume; increases the volume of
     * the audio descriptions relative to the program sound.
     */
    Key TV_AUDIO_DESCRIPTION_MIX_UP = Key.of("TVAudioDescriptionMixUp");

    /**
     * Displays or hides the media contents available for playback (this may be
     * a channel guide showing the currently airing programs, or a list of media
     * files to play).
     */
    Key TV_CONTENTS_MENU = Key.of("TVContentsMenu");

    /**
     * Displays or hides the TV's data service menu.
     */
    Key TV_DATA_SERVICE = Key.of("TVDataService");

    /**
     * Cycles the input mode on an external TV.
     */
    Key TV_INPUT = Key.of("TVInput");

    /**
     * Switches to the input "Component 1."
     */
    Key TV_INPUT_COMPONENT1 = Key.of("TVInputComponent1");

    /**
     * Switches to the input "Component 2."
     */
    Key TV_INPUT_COMPONENT2 = Key.of("TVInputComponent2");

    /**
     * Switches to the input "Composite 1."
     */
    Key TV_INPUT_COMPOSITE1 = Key.of("TVInputComposite1");

    /**
     * Switches to the input "Composite 2."
     */
    Key TV_INPUT_COMPOSITE2 = Key.of("TVInputComposite2");

    /**
     * Switches to the input "HDMI 1."
     */
    Key TV_INPUT_HDMI1 = Key.of("TVInputHDMI1");

    /**
     * Switches to the input "HDMI 2."
     */
    Key TV_INPUT_HDMI2 = Key.of("TVInputHDMI2");

    /**
     * Switches to the input "HDMI 3."
     */
    Key TV_INPUT_HDMI3 = Key.of("TVInputHDMI3");

    /**
     * Switches to the input "HDMI 4."
     */
    Key TV_INPUT_HDMI4 = Key.of("TVInputHDMI4");

    /**
     * Switches to the input "VGA 1."
     */
    Key TV_INPUT_VGA1 = Key.of("TVInputVGA1");

    /**
     * The Media Context menu key.
     */
    Key TV_MEDIA_CONTEXT = Key.of("TVMediaContext");

    /**
     * Toggle the TV's network connection on and off.
     */
    Key TV_NETWORK = Key.of("TVNetwork");

    /**
     * Put the TV into number entry mode.
     */
    Key TV_NUMBER_ENTRY = Key.of("TVNumberEntry");

    /**
     * The device's power button.
     */
    Key TV_POWER = Key.of("TVPower");

    /**
     * Radio button.
     */
    Key TV_RADIO_SERVICE = Key.of("TVRadioService");

    /**
     * Satellite button.
     */
    Key TV_SATELLITE = Key.of("TVSatellite");

    /**
     * Broadcast Satellite button.
     */
    Key TV_SATELLITE_BS = Key.of("TVSatelliteBS");

    /**
     * Communication Satellite button.
     */
    Key TV_SATELLITE_CS = Key.of("TVSatelliteCS");

    /**
     * Toggles among available satellites.
     */
    Key TV_SATELLITE_TOGGLE = Key.of("TVSatelliteToggle");

    /**
     * Selects analog terrestrial television service (analog cable or antenna
     * reception).
     */
    Key TV_TERRESTRIAL_ANALOG = Key.of("TVTerrestrialAnalog");

    /**
     * Selects digital terrestrial television service (digital cable or antenna
     * reception).
     */
    Key TV_TERRESTRIAL_DIGITAL = Key.of("TVTerrestrialDigital");

    /**
     * Timer programming button.
     */
    Key TV_TIMER = Key.of("TVTimer");

    /**
     * Changes the input mode on an external audio/video receiver (AVR) unit.
     */
    Key AVR_INPUT = Key.of("AVRInput");

    /**
     * Toggles the power on an external AVR unit.
     */
    Key AVR_POWER = Key.of("AVRPower");

    /**
     * General-purpose media function key, color-coded red; this has index 0
     * among the colored keys.
     */
    Key COLOR_F0_RED = Key.of("ColorF0Red");

    /**
     * General-purpose media funciton key, color-coded green; this has index 1
     * among the colored keys.
     */
    Key COLOR_F1_GREEN = Key.of("ColorF1Green");

    /**
     * General-purpose media funciton key, color-coded yellow; this has index 2
     * among the colored keys.
     */
    Key COLOR_F2_YELLOW = Key.of("ColorF2Yellow");

    /**
     * General-purpose media funciton key, color-coded blue; this has index 3
     * among the colored keys.
     */
    Key COLOR_F3_BLUE = Key.of("ColorF3Blue");

    /**
     * General-purpose media funciton key, color-coded grey; this has index 4
     * among the colored keys.
     */
    Key COLOR_F4_GREY = Key.of("ColorF4Grey");

    /**
     * General-purpose media funciton key, color-coded brown; this has index 5
     * among the colored keys.
     */
    Key COLOR_F5_BROWN = Key.of("ColorF5Brown");

    /**
     * Toggles closed captioning on and off.
     */
    Key CLOSED_CAPTION_TOGGLE = Key.of("ClosedCaptionToggle");

    /**
     * Adjusts the brightness of the device by toggling between two brightness
     * levels <em>or</em> by cycling among multiple brightness levels.
     */
    Key DIMMER = Key.of("Dimmer");

    /**
     * Cycles among video sources.
     */
    Key DISPLAY_SWAP = Key.of("DisplaySwap");

    /**
     * Switches the input source to the Digital Video Recorder (DVR).
     */
    Key DVR = Key.of("DVR");

    /**
     * The Exit button, which exits the curreent application or menu.
     */
    Key EXIT = Key.of("Exit");

    /**
     * Clears the program or content stored in the first favorites list slot.
     */
    Key FAVORITE_CLEAR0 = Key.of("FavoriteClear0");

    /**
     * Clears the program or content stored in the second favorites list slot.
     */
    Key FAVORITE_CLEAR1 = Key.of("FavoriteClear1");

    /**
     * Clears the program or content stored in the third favorites list slot.
     */
    Key FAVORITE_CLEAR2 = Key.of("FavoriteClear2");

    /**
     * Clears the program or content stored in the fourth favorites list slot.
     */
    Key FAVORITE_CLEAR3 = Key.of("FavoriteClear3");

    /**
     * Selects (recalls) the program or content stored in the first favorites
     * list slot.
     */
    Key FAVORITE_RECALL0 = Key.of("FavoriteRecall0");

    /**
     * Selects (recalls) the program or content stored in the second favorites
     * list slot.
     */
    Key FAVORITE_RECALL1 = Key.of("FavoriteRecall1");

    /**
     * Selects (recalls) the program or content stored in the third favorites
     * list slot.
     */
    Key FAVORITE_RECALL2 = Key.of("FavoriteRecall2");

    /**
     * Selects (recalls) the program or content stored in the fourth favorites
     * list slot.
     */
    Key FAVORITE_RECALL3 = Key.of("FavoriteRecall3");

    /**
     * Stores the current program or content into the first favorites list slot.
     */
    Key FAVORITE_STORE0 = Key.of("FavoriteStore0");

    /**
     * Stores the current program or content into the second favorites list
     * slot.
     */
    Key FAVORITE_STORE1 = Key.of("FavoriteStore1");

    /**
     * Stores the current program or content into the third favorites list slot.
     */
    Key FAVORITE_STORE2 = Key.of("FavoriteStore2");

    /**
     * Stores the current program or content into the fourth favorites list
     * slot.
     */
    Key FAVORITE_STORE3 = Key.of("FavoriteStore3");

    /**
     * Toggles the display of the program or content guide.
     */
    Key GUIDE = Key.of("Guide");

    /**
     * If the guide is currently displayed, this button tells the guide to
     * display the next day's content.
     */
    Key GUIDE_NEXT_DAY = Key.of("GuideNextDay");

    /**
     * If the guide is currently displayed, this button tells the guide to
     * display the previous day's content.
     */
    Key GUIDE_PREVIOUS_DAY = Key.of("GuidePreviousDay");

    /**
     * Toggles the display of information about the currently selected content,
     * program, or media.
     */
    Key INFO = Key.of("Info");

    /**
     * Tells the device to perform an instant replay (typically some form of
     * jumping back a short amount of time then playing it again, possibly but
     * not usually in slow motion).
     */
    Key INSTANT_REPLAY = Key.of("InstantReplay");

    /**
     * Opens content linked to the current program, if available and possible.
     */
    Key LINK = Key.of("Link");

    /**
     * Lists the current program.
     */
    Key LIST_PROGRAM = Key.of("ListProgram");

    /**
     * Toggles a display listing currently available live content or programs.
     */
    Key LIVE_CONTENT = Key.of("LiveContent");

    /**
     * Locks or unlocks the currently selected content or pgoram.
     */
    Key LOCK = Key.of("Lock");

    /**
     * Presents a list of media applications, such as photo viewers, audio and
     * video players, and games.
     */
    Key MEDIA_APPS = Key.of("MediaApps");

    /**
     * The Audio Track key.
     */
    Key MEDIA_AUDIO_TRACK = Key.of("MediaAudioTrack");

    /**
     * Jumps back to the last-viewed content, program, or other media.
     */
    Key MEDIA_LAST = Key.of("MediaLast");

    /**
     * Skips backward to the previous content or program.
     */
    Key MEDIA_SKIP_BACKWARD = Key.of("MediaSkipBackward");

    /**
     * Skips forward to the next content or program.
     */
    Key MEDIA_SKIP_FORWARD = Key.of("MediaSkipForward");

    /**
     * Steps backward to the previous content or program.
     */
    Key MEDIA_STEP_BACKWARD = Key.of("MediaStepBackward");

    /**
     * Steps forward to the next content or program.
     */
    Key MEDIA_STEP_FORWARD = Key.of("MediaStepForward");

    /**
     * Top Menu button; opens the media's main menu, such as on a DVD or Blu-Ray
     * disc.
     */
    Key MEDIA_TOP_MENU = Key.of("MediaTopMenu");

    /**
     * Navigates into a submenu or option.
     */
    Key NAVIGATE_IN = Key.of("NavigateIn");

    /**
     * Navigates to the next item.
     */
    Key NAVIGATE_NEXT = Key.of("NavigateNext");

    /**
     * Navigates out of the current screen or menu.
     */
    Key NAVIGATE_OUT = Key.of("NavigateOut");

    /**
     * Navigates to the previous item.
     */
    Key NAVIGATE_PREVIOUS = Key.of("NavigatePrevious");

    /**
     * Cycles to the next channel in the favorites list.
     */
    Key NEXT_FAVORITE_CHANNEL = Key.of("NextFavoriteChannel");

    /**
     * Cycles to the next saved user profile, if this feature is supported and
     * multiple profiles exist.
     */
    Key NEXT_USER_PROFILE = Key.of("NextUserProfile");

    /**
     * Opens the user interface for selecting on demand content or programs to
     * watch.
     */
    Key ON_DEMAND = Key.of("OnDemand");

    /**
     * Starts the process of pairing the remote with a device to be controlled.
     */
    Key PAIRING = Key.of("Pairing");

    /**
     * A button to move the picture-in-picture view downward.
     */
    Key PIP_DOWN = Key.of("PinPDown");

    /**
     * A button to control moving the picture-in-picture view.
     */
    Key PIP_MOVE = Key.of("PinPMove");

    /**
     * Toggles display of th epicture-in-picture view on and off.
     */
    Key PIP_TOGGLE = Key.of("PinPToggle");

    /**
     * A button to move the picture-in-picture view upward.
     */
    Key PIP_UP = Key.of("PinPUp");

    /**
     * Decreases the media playback rate.
     */
    Key PLAY_SPEED_DOWN = Key.of("PlaySpeedDown");

    /**
     * Returns the media playback rate to normal.
     */
    Key PLAY_SPEED_RESET = Key.of("PlaySpeedReset");

    /**
     * Increases the media playback rate.
     */
    Key PLAY_SPEED_UP = Key.of("PlaySpeedUp");

    /**
     * Toggles random media (also known as "shuffle mode") on and off.
     */
    Key RANDOM_TOGGLE = Key.of("RandomToggle");

    /**
     * A code sent when the remote control's battery is low. This doesn't
     * actually correspond to a physical key at all.
     */
    Key RC_LOW_BATTERY = Key.of("RcLowBattery");

    /**
     * Cycles among the available media recording speeds.
     */
    Key RECORD_SPEED_NEXT = Key.of("RecordSpeedNext");

    /**
     * Toggles radio frequency (RF) input bypass mode on and off. RF bypass mode
     * passes RF input directly to the RF output without any processing or
     * filtering.
     */
    Key RF_BYPASS = Key.of("RfBypass");

    /**
     * Toggles the channel scan mode on and off; this is a mode which flips
     * through channels automatically until the user stops the scan.
     */
    Key SCAN_CHANNELS_TOGGLE = Key.of("ScanChannelsToggle");

    /**
     * Cycles through the available screen display modes.
     */
    Key SCREEN_MODE_NEXT = Key.of("ScreenModeNext");

    /**
     * Toggles display of the device's settings screen on and off.
     */
    Key SETTINGS = Key.of("Settings");

    /**
     * Toggles split screen display mode on and off.
     */
    Key SPLIT_SCREEN_TOGGLE = Key.of("SplitScreenToggle");

    /**
     * Cycles among input modes on an external set-top box (STB).
     */
    Key STB_INPUT = Key.of("STBInput");

    /**
     * Toggles on and off an external STB.
     */
    Key STB_POWER = Key.of("STBPower");

    /**
     * Toggles the display of subtitles on and off if they're available.
     */
    Key SUBTITLE = Key.of("Subtitle");

    /**
     * Toggles display of
     * <a href="https://en.wikipedia.org/wiki/teletext">teletext</a>, if
     * available.
     */
    Key TELETEXT = Key.of("Teletext");

    /**
     * Cycles through the available video modes.
     */
    Key VIDEO_MODE_NEXT = Key.of("VideoModeNext");

    /**
     * Causes the device to identify itself in some fashion, such as by flashing
     * a light, briefly changing the brightness of indicator lights, or emitting
     * a tone.
     */
    Key WINK = Key.of("Wink");

    /**
     * Toggles between full-screen and scaled content display, or otherwise
     * change the magnification level.
     */
    Key ZOOM_TOGGLE = Key.of("ZoomToggle");

    /**
     * Presents a list of possible corrections for a word which was incorrectly
     * identified.
     */
    Key SPEECH_CORRECTION_LIST = Key.of("SpeechCorrectionList");

    /**
     * Toggles between dictation mode and command/control mode. This lets the
     * speech engine know whether to interpret spoken words as input text or as
     * commands.
     */
    Key SPEECH_INPUT_TOGGLE = Key.of("SpeechInputToggle");

    /**
     * Closes the current document or message. Must not exit the application.
     */
    Key CLOSE = Key.of("Close");

    /**
     * Creates a new document or message.
     */
    Key NEW = Key.of("New");

    /**
     * Opens an existing document or message.
     */
    Key OPEN = Key.of("Open");

    /**
     * Prints the current document or message.
     */
    Key PRINT = Key.of("Print");

    /**
     * Saves the current document or message.
     */
    Key SAVE = Key.of("Save");

    /**
     * Starts spell checking the current document.
     */
    Key SPELL_CHECK = Key.of("SpellCheck");

    /**
     * Opens the user interface to forward a message.
     */
    Key MAIL_FORWARD = Key.of("MailForward");

    /**
     * Opens the user interface to reply to a message.
     */
    Key MAIL_REPLY = Key.of("MailReply");

    /**
     * Sends the current message.
     */
    Key MAIL_SEND = Key.of("MailSend");

    /**
     * The <code>Calculator</code> key. This is often used as a generic
     * application launcher key (<code>APPCOMMAND_LAUNCH_APP2</code>).
     */
    Key LAUNCH_CALCULATOR = Key.of("LaunchCalculator");

    /**
     * The <code>Calendar</code> key.
     */
    Key LAUNCH_CALENDAR = Key.of("LaunchCalendar");

    /**
     * The <code>Contacts</code> key.
     */
    Key LAUNCH_CONTACTS = Key.of("LaunchContacts");

    /**
     * The <code>Mail</code> key.
     */
    Key LAUNCH_MAIL = Key.of("LaunchMail");

    /**
     * The <code>Media Player</code> key.
     */
    Key LAUNCH_MEDIA_PLAYER = Key.of("LaunchMediaPlayer");

    /**
     * The <code>Music Player</code> key.
     */
    Key LAUNCH_MUSIC_PLAYER = Key.of("LaunchMusicPlayer");

    /**
     * The <code>My Computer</code> key on Windows keyboards. This is often used
     * as a generic application launcher key
     * (<code>APPCOMMAND_LAUNCH_APP1</code>).
     */
    Key LAUNCH_MY_COMPUTER = Key.of("LaunchMyComputer");

    /**
     * The <code>Phone</code> key, to open the phone dialer application if one
     * is present.
     */
    Key LAUNCH_PHONE = Key.of("LaunchPhone");

    /**
     * The <code>Screen Saver</code> key.
     */
    Key LAUNCH_SCREEN_SAVER = Key.of("LaunchScreenSaver");

    /**
     * The <code>Spreadsheet</code> key.
     */
    Key LAUNCH_SPREADSHEET = Key.of("LaunchSpreadsheet");

    /**
     * The <code>Web Browser</code> key.
     */
    Key LAUNCH_WEB_BROWSER = Key.of("LaunchWebBrowser");

    /**
     * The <code>WebCam</code> key. Opens the webcam application.
     */
    Key LAUNCH_WEB_CAM = Key.of("LaunchWebCam");

    /**
     * The <code>Word Processor</code> key. This may be an icon of a specific
     * word processor application, or a generic document icon.
     */
    Key LAUNCH_WORD_PROCESSOR = Key.of("LaunchWordProcessor");

    /**
     * The first generic application launcher button.
     */
    Key LAUNCH_APPLICATION1 = Key.of("LaunchApplication1");

    /**
     * The second generic application launcher button.
     */
    Key LAUNCH_APPLICATION2 = Key.of("LaunchApplication2");

    /**
     * The third generic application launcher button.
     */
    Key LAUNCH_APPLICATION3 = Key.of("LaunchApplication3");

    /**
     * The fourth generic application launcher button.
     */
    Key LAUNCH_APPLICATION4 = Key.of("LaunchApplication4");

    /**
     * The fifth generic application launcher button.
     */
    Key LAUNCH_APPLICATION5 = Key.of("LaunchApplication5");

    /**
     * The sixth generic application launcher button.
     */
    Key LAUNCH_APPLICATION6 = Key.of("LaunchApplication6");

    /**
     * The seventh generic application launcher button.
     */
    Key LAUNCH_APPLICATION7 = Key.of("LaunchApplication7");

    /**
     * The eighth generic application launcher button.
     */
    Key LAUNCH_APPLICATION8 = Key.of("LaunchApplication8");

    /**
     * The ninth generic application launcher button.
     */
    Key LAUNCH_APPLICATION9 = Key.of("LaunchApplication9");

    /**
     * The 10th generic application launcher button.
     */
    Key LAUNCH_APPLICATION10 = Key.of("LaunchApplication10");

    /**
     * The 11th generic application launcher button.
     */
    Key LAUNCH_APPLICATION11 = Key.of("LaunchApplication11");

    /**
     * The 12th generic application launcher button.
     */
    Key LAUNCH_APPLICATION12 = Key.of("LaunchApplication12");

    /**
     * The 13th generic application launcher button.
     */
    Key LAUNCH_APPLICATION13 = Key.of("LaunchApplication13");

    /**
     * The 14th generic application launcher button.
     */
    Key LAUNCH_APPLICATION14 = Key.of("LaunchApplication14");

    /**
     * The 15th generic application launcher button.
     */
    Key LAUNCH_APPLICATION15 = Key.of("LaunchApplication15");

    /**
     * The 16th generic application launcher button.
     */
    Key LAUNCH_APPLICATION16 = Key.of("LaunchApplication16");

    /**
     * Navigates to the previous content or page in the current Web view's
     * history.
     */
    Key BROWSER_BACK = Key.of("BrowserBack");

    /**
     * Opens the user's list of bookmarks/favorites.
     */
    Key BROWSER_FAVORITES = Key.of("BrowserFavorites");

    /**
     * Navigates to the next content or page in the current Web view's history.
     */
    Key BROWSER_FORWARD = Key.of("BrowserForward");

    /**
     * Navigates to the user's preferred home page.
     */
    Key BROWSER_HOME = Key.of("BrowserHome");

    /**
     * Refreshes the current page or contentl.
     */
    Key BROWSER_REFRESH = Key.of("BrowserRefresh");

    /**
     * Activates the user's preferred search engine or the search interface
     * within their browser.
     */
    Key BROWSER_SEARCH = Key.of("BrowserSearch");

    /**
     * Stops loading the currently displayed Web view or content.
     */
    Key BROWSER_STOP = Key.of("BrowserStop");

    /**
     * The decimal point key (typically <code>.</code> or <code>,</code>
     * depending on the region. In newer browsers, this value to simply be the
     * character generated by the decimal key (one of those two characters).
     */
    Key DECIMAL = Key.of("Decimal");

    /**
     * The <code>11</code> key found on certain media numeric keypads.
     */
    Key KEY11 = Key.of("Key11");

    /**
     * The <code>12</code> key found on certain media numeric keypads.
     */
    Key KEY12 = Key.of("Key12");

    /**
     * The numeric keypad's multiplication key, <code>*</code>.
     */
    Key MULTIPLY = Key.of("Multiply");

    /**
     * The numeric keypad's addition key, <code>+</code>.
     */
    Key ADD = Key.of("Add");

    /**
     * The numeric keypad's division key, /.
     */
    Key DIVIDE = Key.of("Divide");

    /**
     * The numeric keypad's subtraction key, -.
     */
    Key SUBTRACT = Key.of("Subtract");

    /**
     * The numeric keypad's places separator character (in the United States,
     * this is a comma, but elsewhere it is frequently a period).
     */
    Key SEPARATOR = Key.of("Separator");

    /**
     * Returns a {@link Key} instance representing the {@code keys} codes.
     *
     * @param keys
     *            the codes representing the key
     * @return the {@link Key} instance
     */
    static Key of(String... keys) {
        return () -> Arrays.asList(keys);
    }

    /**
     * Returns the list of codes representing the key, which should be the same
     * as the <code>key</code> property in the JavaScript
     * <code>KeyboardEvent</code>.
     * <p>
     * See https://developer.mozilla.org/en-US/docs/Web/API/KeyboardEvent/key
     *
     * @return the list of codes representing the key
     */
    List<String> getKeys();

    /**
     * Checks if the {@code key} code represents this {@link Key}.
     *
     * @param key
     *            the key code
     * @return true, if this {@link Key} matches the code
     */
    default boolean matches(String key) {
        return getKeys().contains(key);
    }

}
