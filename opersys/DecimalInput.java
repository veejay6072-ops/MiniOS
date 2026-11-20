package opersys;

import java.math.BigDecimal;

public final class DecimalInput {
    private DecimalInput() {
    }

    public static double parseDecimal(String text, String what) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(what + " is empty.");
        }
        try {
            double value = Double.parseDouble(text.trim());
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException(what + " must be a valid number.");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(what + " must be a number.");
        }
    }

    public static String format(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
