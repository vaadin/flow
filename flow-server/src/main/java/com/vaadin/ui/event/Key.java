package com.vaadin.ui.event;

import java.util.stream.Stream;

/**
 * Enumeration of keyboard keys.
 */
public enum Key {

    /**
     * Key for "{@code Cancel}" key.
     */
    CANCEL("Cancel"),

    /**
     * Key for "{@code Help}" key.
     */
    HELP("Help"),

    /**
     * Key for "{@code Backspace}" key.
     */
    BACKSPACE("Backspace"),

    /**
     * Key for "{@code Tab}" key.
     */
    TAB("Tab"),

    /**
     * Key for "{@code Clear}" key.
     */
    CLEAR("Clear"),

    /**
     * Key for "{@code Enter}" key.
     */
    ENTER("Enter"),

    /**
     * Key for "{@code Shift}" key.
     */
    SHIFT("Shift"),

    /**
     * Key for "{@code Control}" key.
     */
    CONTROL("Control"),

    /**
     * Key for "{@code Alt}" key.
     */
    ALT("Alt"),

    /**
     * Key for "{@code Pause}" key.
     */
    PAUSE("Pause"),

    /**
     * Key for "{@code CapsLock}" key.
     */
    CAPS_LOCK("CapsLock"),

    /**
     * Key for "{@code Escape}" key.
     */
    ESCAPE("Escape"),

    /**
     * Key for "{@code Convert}" key.
     */
    CONVERT("Convert"),

    /**
     * Key for "{@code NonConvert}" key.
     */
    NON_CONVERT("NonConvert"),

    /**
     * Key for "{@code Accept}" key.
     */
    ACCEPT("Accept"),

    /**
     * Key for "{@code ModeChange}" key.
     */
    MODE_CHANGE("ModeChange"),

    /**
     * Key for " " (space) key.
     */
    SPACE(" "),

    /**
     * Key for "{@code PageUp}" key.
     */
    PAGE_UP("PageUp"),

    /**
     * Key for "{@code PageDown}" key.
     */
    PAGE_DOWN("PageDown"),

    /**
     * Key for "{@code End}" key.
     */
    END("End"),

    /**
     * Key for "{@code Home}" key.
     */
    HOME("Home"),

    /**
     * Key for "{@code ArrowLeft}" key.
     */
    ARROW_LEFT("ArrowLeft"),

    /**
     * Key for "{@code ArrowUp}" key.
     */
    ARROW_UP("ArrowUp"),

    /**
     * Key for "{@code ArrowRight}" key.
     */
    ARROW_RIGHT("ArrowRight"),

    /**
     * Key for "{@code ArrowDown}" key.
     */
    ARROW_DOWN("ArrowDown"),

    /**
     * Key for "{@code Select}" key.
     */
    SELECT("Select"),

    /**
     * Key for "{@code Print}" key.
     */
    PRINT("Print"),

    /**
     * Key for "{@code Execute}" key.
     */
    EXECUTE("Execute"),

    /**
     * Key for "{@code PrintScreen}" key.
     */
    PRINT_SCREEN("PrintScreen"),

    /**
     * Key for "{@code Insert}" key.
     */
    INSERT("Insert"),

    /**
     * Key for "{@code Delete}" key.
     */
    DELETE("Delete"),

    /**
     * Key for "{@code 0}" key.
     */
    ZERO("0"),

    /**
     * Key for "{@code 1}" key.
     */
    ONE("1"),

    /**
     * Key for "{@code 2}" key.
     */
    TWO("2"),

    /**
     * Key for "{@code 3}" key.
     */
    THREE("3"),

    /**
     * Key for "{@code 4}" key.
     */
    FOUR("4"),

    /**
     * Key for "{@code 5}" key.
     */
    FIVE("5"),

    /**
     * Key for "{@code 6}" key.
     */
    SIX("6"),

    /**
     * Key for "{@code 7}" key.
     */
    SEVEN("7"),

    /**
     * Key for "{@code 8}" key.
     */
    EIGHT("8"),

    /**
     * Key for "{@code 9}" key.
     */
    NINE("9"),

    /**
     * Key for "{@code )}" key.
     */
    PARENTHESIS_RIGHT(")"),

    /**
     * Key for "{@code !}" key.
     */
    EXCLAMATION_MARK("!"),

    /**
     * Key for "{@code @}" key.
     */
    AT("@"),

    /**
     * Key for "{@code #}" key.
     */
    HASH("#"),

    /**
     * Key for "{@code $}" key.
     */
    DOLLAR("$"),

    /**
     * Key for "{@code %}" key.
     */
    PERCENTAGE("%"),

    /**
     * Key for "{@code ^}" key.
     */
    CARET("^"),

    /**
     * Key for "{@code &}" key.
     */
    AMPERSAND("&"),

    /**
     * Key for "{@code (}" key.
     */
    PARENTHESIS_LEFT("("),

    /**
     * Key for "{@code OS}" key.
     */
    OS("OS"),

    /**
     * Key for "{@code ContextMenu}" key.
     */
    CONTEXT_MENU("ContextMenu"),

    /**
     * Key for "{@code *}" key.
     */
    ASTERISK("*"),

    /**
     * Key for "{@code +}" key.
     */
    PLUS("+"),

    /**
     * Key for "{@code -}" key.
     */
    MINUS("-"),

    /**
     * Key for "{@code .}" key.
     */
    DOT("."),

    /**
     * Key for "{@code /}" key.
     */
    SLASH("/"),

    /**
     * Key for "{@code NumLock}" key.
     */
    NUM_LOCK("NumLock"),

    /**
     * Key for "{@code ScrollLock}" key.
     */
    SCROLL_LOCK("ScrollLock"),

    /**
     * Key for "{@code VolumeMute}" key.
     */
    VOLUME_MUTE("VolumeMute"),

    /**
     * Key for "{@code VolumeDown}" key.
     */
    VOLUME_DOWN("VolumeDown"),

    /**
     * Key for "{@code VolumeUp}" key.
     */
    VOLUME_UP("VolumeUp"),

    /**
     * Key for "{@code ;}" key.
     */
    SEMICOLON(";"),

    /**
     * Key for "{@code =}" key.
     */
    EQUAL("="),

    /**
     * Key for "{@code ,}" key.
     */
    COMMA(","),

    /**
     * Key for "{@code `}" key.
     */
    BACK_TICK("`"),

    /**
     * Key for "{@code [}" key.
     */
    SQUARE_BRACKET_LEFT("["),

    /**
     * Key for "{@code \}" key.
     */
    BACKSLASH("\\"),

    /**
     * Key for "{@code ]}" key.
     */
    SQUARE_BRACKET_RIGHT("]"),

    /**
     * Key for "{@code "}" key.
     */
    QUOTE("\""),

    /**
     * Key for "{@code :}" key.
     */
    COLON(":"),

    /**
     * Key for "{@code <}" key.
     */
    ANGLE_BRACKET_LEFT("<"),

    /**
     * Key for "{@code _}" key.
     */
    UNDERSCORE("_"),

    /**
     * Key for "{@code >}" key.
     */
    ANGLE_BRACKET_RIGHT(">"),

    /**
     * Key for "{@code ?}" key.
     */
    QUESTION_MARK("?"),

    /**
     * Key for "{@code ~}" key.
     */
    TILDE("~"),

    /**
     * Key for "{@code {}" key.
     */
    CURLY_BRACKET_LEFT("{"),

    /**
     * Key for "{@code |}" key.
     */
    PIPE("|"),

    /**
     * Key for "{@code }}" key.
     */
    CURLY_BRACKET_RIGHT("}"),

    /**
     * Key for "{@code Meta}" key.
     */
    META("Meta"),

    /**
     * Key for "{@code AltGraph}" key.
     */
    ALT_GRAPH("AltGraph");

    private final String key;

    Key(String key) {
        this.key = key;
    }

    /**
     * Gets the {@code Key} key value.
     *
     * @return the key value
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the {@code Key} for {@code key}.
     *
     * @param key
     *            the key value
     * @return the {@code Key}
     */
    public static Key of(String key) {
        return Stream.of(values()).filter(k -> k.key == key).findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

}
