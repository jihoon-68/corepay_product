package org.example.corepayproductservice.product.application.enums;

import lombok.Getter;

@Getter
public enum CancelReason {

    OUT_OF_STOCK(false),

    PRODUCT_NOT_FOUND(false),

    CUSTOMER_CANCEL(true),

    PAYMENT_FAILED(true);

    private final boolean needStockRestore;

    CancelReason(boolean needStockRestore) {
        this.needStockRestore = needStockRestore;
    }
}
