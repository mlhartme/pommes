package net.oneandone.pommes.model;

public enum Match {
    SUBSTRING, PREFIX, SUFFIX, STRING;

    public static Object[] locate(String str) {
        for (int i = 0; i < str.length(); i++) {
            switch (str.charAt(i)) {
                case ':':
                    return new Object[] { SUBSTRING, i };
                case '=':
                    return new Object[] { STRING, i };
                case '^':
                    return new Object[] { PREFIX, i };
                case ',':
                    return new Object[] { SUFFIX, i };
                default:
                    break;
            }
        }
        return null;
    }
}
