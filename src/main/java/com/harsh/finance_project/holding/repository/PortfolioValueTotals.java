package com.harsh.finance_project.holding.repository;

import java.math.BigDecimal;

public interface PortfolioValueTotals {
    BigDecimal getTotalInvestedValue();

    BigDecimal getTotalCurrentValue();
}
