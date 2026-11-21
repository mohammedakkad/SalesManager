package com.trader.qa;

/**
 * The app prints numbers through one formatter with Arabic-Indic digits (U+0660..U+0669),
 * Arabic decimal separator U+066B and grouping separator U+066C. Expected values in these
 * tests are written in ASCII for readability and converted here.
 */
final class ArabicDigits {
    private ArabicDigits() {}

    /** "12,345.67" -> "١٢٬٣٤٥٫٦٧" */
    static String ar(String ascii) {
        StringBuilder out = new StringBuilder(ascii.length());
        for (char c : ascii.toCharArray()) {
            if (c >= '0' && c <= '9') out.append((char) ('٠' + (c - '0')));
            else if (c == '.') out.append('٫');
            else if (c == ',') out.append('٬');
            else out.append(c);
        }
        return out.toString();
    }

    /** Removes bidi isolate/mark controls that may survive text extraction. */
    static String stripBidiControls(String text) {
        return text.replaceAll("[\\u2066-\\u2069\\u200E\\u200F]", "");
    }

    /** Character class body for digits/separators in either system, for regex look-arounds. */
    static final String NUMBER_CHARS = "0-9.\\u0660-\\u0669\\u066B\\u066C";
}
