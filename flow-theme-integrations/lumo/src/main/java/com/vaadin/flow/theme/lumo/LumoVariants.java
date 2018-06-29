package com.vaadin.flow.theme.lumo;

import com.vaadin.flow.component.HasTheme.ThemeVariant;

/**
 * Contains 'Lumo' theme variants for components.
 */
public class LumoVariants {

    /**
     * Set of variants applicable for 'VerticalLayout' component and 'Lumo'
     * theme.
     */
    public enum VerticalLayoutVariants implements ThemeVariant {
        SPACING_XS("spacing-xs"), SPACING_S("spacing-s"), SPACING(
                "spacing"), SPACING_L("spacing-l"), SPACING_XL("spacing-xl");
        private final String variant;

        VerticalLayoutVariants(java.lang.String variant) {
            this.variant = variant;
        }

        @Override
        public String getVariant() {
            return variant;
        }
    }

    /**
     * Set of variants applicable for 'Button' component and 'Lumo' theme.
     */
    public enum ButtonVariants implements ThemeVariant {
        SMALL("small"), LARGE("large"), TERTIARY("tertiary"), TERTIARY_INLINE(
                "tertiary-inline"), PRIMARY("primary"), SUCCESS(
                        "success"), ERROR(
                                "error"), CONTRAST("contrast"), ICON("icon");
        private final String variant;

        ButtonVariants(java.lang.String variant) {
            this.variant = variant;
        }

        @Override
        public String getVariant() {
            return variant;
        }
    }

    /**
     * Set of variants applicable for 'Grid' component and 'Lumo' theme.
     */
    public enum GridVariants implements ThemeVariant {
        COLUMN_BORDERS("column-borders"), ROW_STRIPES("row-stripes"), COMPACT(
                "compact"), WRAP_CELL_CONTENT("wrap-cell-content");
        private final String variant;

        GridVariants(java.lang.String variant) {
            this.variant = variant;
        }

        @Override
        public String getVariant() {
            return variant;
        }
    }

    /**
     * Set of variants applicable for 'GridTreeToggle' component and 'Lumo'
     * theme.
     */
    public enum GridTreeToggleVariants implements ThemeVariant {
        CONNECTORS("connectors");
        private final String variant;

        GridTreeToggleVariants(java.lang.String variant) {
            this.variant = variant;
        }

        @Override
        public String getVariant() {
            return variant;
        }
    }

    /**
     * Set of variants applicable for 'SplitLayout' component and 'Lumo' theme.
     */
    public enum SplitLayoutVariants implements ThemeVariant {
        SMALL("small"), MINIMAL("minimal");
        private final String variant;

        SplitLayoutVariants(java.lang.String variant) {
            this.variant = variant;
        }

        @Override
        public String getVariant() {
            return variant;
        }
    }

    /**
     * Set of variants applicable for 'TextField' component and 'Lumo' theme.
     */
    public enum TextFieldVariants implements ThemeVariant {
        SMALL("small"), ALIGN_CENTER("align-center"), ALIGN_RIGHT(
                "align-right");
        private final String variant;

        TextFieldVariants(java.lang.String variant) {
            this.variant = variant;
        }

        @Override
        public String getVariant() {
            return variant;
        }
    }

    /**
     * Set of variants applicable for 'HorizontalLayout' component and 'Lumo'
     * theme.
     */
    public enum HorizontalLayoutVariants implements ThemeVariant {
        SPACING_XS("spacing-xs"), SPACING_S("spacing-s"), SPACING(
                "spacing"), SPACING_L("spacing-l"), SPACING_XL("spacing-xl");
        private final String variant;

        HorizontalLayoutVariants(java.lang.String variant) {
            this.variant = variant;
        }

        @Override
        public String getVariant() {
            return variant;
        }
    }

    /**
     * Set of variants applicable for 'Tab' component and 'Lumo' theme.
     */
    public enum TabVariants implements ThemeVariant {
        ICON_ON_TOP("icon-on-top");
        private final String variant;

        TabVariants(java.lang.String variant) {
            this.variant = variant;
        }

        @Override
        public String getVariant() {
            return variant;
        }
    }

    /**
     * Set of variants applicable for 'ProgressBar' component and 'Lumo' theme.
     */
    public enum ProgressBarVariants implements ThemeVariant {
        CONTRAST("contrast"), ERROR("error"), SUCCESS("success");
        private final String variant;

        ProgressBarVariants(java.lang.String variant) {
            this.variant = variant;
        }

        @Override
        public String getVariant() {
            return variant;
        }
    }

    /**
     * Set of variants applicable for 'RadioGroup' component and 'Lumo' theme.
     */
    public enum RadioGroupVariants implements ThemeVariant {
        VERTICAL("vertical");
        private final String variant;

        RadioGroupVariants(java.lang.String variant) {
            this.variant = variant;
        }

        @Override
        public String getVariant() {
            return variant;
        }
    }

    /**
     * Set of variants applicable for 'Tabs' component and 'Lumo' theme.
     */
    public enum TabsVariants implements ThemeVariant {
        SMALL("small"), MINIMAL("minimal"), HIDE_SCROLL_BUTTONS(
                "hide-scroll-buttons"), EQUAL_WIDTH_TABS("equal-width-tabs");
        private final String variant;

        TabsVariants(java.lang.String variant) {
            this.variant = variant;
        }

        @Override
        public String getVariant() {
            return variant;
        }
    }
}
