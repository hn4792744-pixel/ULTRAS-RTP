package com.ultras.rtp.utils;

public final class Text {
    private Text() {
    }

    /** 3 -> 𝟹 (mathematical monospace digits). */
    public static String mono(int number) {
        String s = Integer.toString(Math.max(0, number));
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.appendCodePoint(0x1D7F6 + (c - '0'));
        }
        return sb.toString();
    }
}
