package org.example.corepayproductservice.product.application;

public enum ActionType {
    DEDUCT("deduct"),
    RESTORE("restore");

    private final String prefix;
    ActionType(String prefix) { this.prefix = prefix; }
    public String getPrefix() { return prefix; }
}
