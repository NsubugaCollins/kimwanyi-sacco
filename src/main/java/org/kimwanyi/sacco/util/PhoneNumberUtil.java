package org.kimwanyi.sacco.util;


import org.kimwanyi.sacco.exception.ValidationException;


public final class PhoneNumberUtil {


    private PhoneNumberUtil() {

    }


    /**
     * Converts Ugandan phone numbers
     *
     * 0772123456
     * 256772123456
     * +256772123456
     *
     * into:
     *
     * +256772123456
     */
    public static String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;

        }


        // Remove spaces, -, brackets
        phone = phone
                .trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");



        // Already correct format
        if (phone.startsWith("+256")) {
            return validate(phone);

        }

        // Starts with 256
        if (phone.startsWith("256")) {
            phone = "+" + phone;
            return validate(phone);
        }

        // Local Ugandan format
        if (phone.startsWith("0")) {
            phone =
                    "+256" + phone.substring(1);
            return validate(phone);
        }
        throw new ValidationException(
                "Invalid Ugandan phone number format"
        );

    }

    private static String validate(String phone) {


        /*
          Uganda mobile numbers:

          +2567XXXXXXXX

          +256
          +
          7
          +
          8 digits
        */

        if (!phone.matches("^\\+2567[0-9]{8}$")) {
            throw new ValidationException("Invalid phone number");
        }
        return phone;
    }
}