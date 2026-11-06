package net.phoenix.core.integration.conflux.client;

final class CodeInput {

    private CodeInput() {}

    static boolean isCodeChar(char ch) {
        return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'k' && ch <= 'o') || ch == 'r';
    }

    static boolean isHex6(String v, int from) {
        if (from < 0 || from + 6 > v.length()) return false;
        for (int i = from; i < from + 6; i++) {
            char ch = v.charAt(i);
            if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'))) return false;
        }
        return true;
    }

    static String convertPasted(String text) {
        return text.replaceAll("&#([0-9A-Fa-f]{6})", "{#$1}").replaceAll("&([0-9a-fk-or])", "§$1");
    }
}
