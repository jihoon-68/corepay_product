package org.example.corepayproductservice.product.application.enums;

import lombok.Getter;

@Getter
public enum ActionType {
    DEDUCT("deduct"),
    RESTORE("restore");

    private final String prefix;
    ActionType(String prefix) { this.prefix = prefix; }
}
