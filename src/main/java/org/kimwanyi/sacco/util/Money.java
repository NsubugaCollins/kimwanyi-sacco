package org.kimwanyi.sacco.util;

import org.kimwanyi.sacco.enums.Currency;
import org.kimwanyi.sacco.exception.ValidationException;

import java.math.BigDecimal;

public final class Money {

    public static final Money ZERO = new Money(BigDecimal.ZERO, FinancialConstants.DEFAULT_CURRENCY);

    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount) {
        this(amount, FinancialConstants.DEFAULT_CURRENCY);
    }

    public Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new ValidationException("Monetary amount cannot be null.");
        }
        if (currency == null) {
            throw new ValidationException("Monetary currency cannot be null.");
        }
        this.amount = amount.setScale(FinancialConstants.DECIMAL_SCALE, FinancialConstants.ROUNDING_MODE);
        this.currency = currency;
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Money add(Money other) {
        checkSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        checkSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        if (factor == null) {
            throw new ValidationException("Multiplication factor cannot be null.");
        }
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public Money multiply(long factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    public Money divide(BigDecimal divisor) {
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("Division by zero or null is not allowed.");
        }
        return new Money(this.amount.divide(divisor, FinancialConstants.DECIMAL_SCALE, FinancialConstants.ROUNDING_MODE), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        checkSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        checkSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Money other) {
        checkSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    private void checkSameCurrency(Money other) {
        if (other == null) {
            throw new ValidationException("Cannot perform money operation on null.");
        }
        if (this.currency != other.currency) {
            throw new ValidationException(String.format(
                    "Currency mismatch: Cannot combine %s with %s", this.currency, other.currency
            ));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && currency == money.currency;
    }

    @Override
    public int hashCode() {
        return 31 * amount.stripTrailingZeros().hashCode() + currency.hashCode();
    }

    @Override
    public String toString() {
        return currency.getSymbol() + " " + amount.toPlainString();
    }
}
