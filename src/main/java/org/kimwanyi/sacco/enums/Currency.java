package org.kimwanyi.sacco.enums;

public enum Currency {
    UGX("UGX", "USh", "Ugandan Shilling"),
    USD("USD", "$", "US Dollar"),
    KES("KES", "KSh", "Kenyan Shilling");

    private final String code;
    private final String symbol;
    private final String displayName;

    Currency(String code, String symbol, String displayName) {
        this.code = code;
        this.symbol = symbol;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDisplayName() {
        return displayName;
    }
}
