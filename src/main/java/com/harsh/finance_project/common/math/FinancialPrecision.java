package com.harsh.finance_project.common.math;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FinancialPrecision {
    public static final int QUANTITY_SCALE = 4;
    public static final int PRICE_SCALE = 4;
    public static final int MONEY_SCALE = 4;

    private FinancialPrecision() {
    }

    public static BigDecimal quantity(BigDecimal value) {
        return value.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal price(BigDecimal value) {
        return value.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal settlementAmount(BigDecimal quantity, BigDecimal price) {
        return quantity(quantity).multiply(price(price)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
