package com.bestbymanager.app.utilities;

import com.google.zxing.oned.UPCEReader;
public final class BarcodeUtil {

    private BarcodeUtil() { }

    /**
     * Canonicalises any retail barcode to a 13-digit GTIN.
     * Accepts:
     *   • 12-digit UPC-A
     *   • 13-digit EAN-13 (already canonical)
     *   • 8-digit EAN-8
     *   • 8-digit UPC-E (as returned by ZXing)
     *   • 7-digit UPC-E (missing check digit)
     *   • 6-digit UPC-E (legacy APIs)
     *
     * @throws IllegalArgumentException if the input is not 6, 7, 8, 12 or 13 digits
     */
    public static String toCanonical(String raw) {
        String digits = raw.replaceAll("\\D", "");

        switch (digits.length()) {
            case 12:                                      // UPC-A ➜ GTIN-13
                return '0' + digits;

            case 13:                                      // already GTIN-13
                return digits;

            case 8:                                       // EAN-8 **or** UPC-E
                return (digits.charAt(0) <= '1')          // system digit 0 or 1 ⇒ UPC-E
                        ? upcEToEan13(digits)
                        : ean8ToEan13(digits);

            case 7:                                       // 7-digit UPC-E
                return upcEToEan13(digits);

            case 6:                                       // 6-digit UPC-E (rare)
                return upcEToEan13(digits);

            default:
                throw new IllegalArgumentException(
                        "Barcode must be 6, 7, 8, 12, or 13 digits: " + raw);
        }
    }

    /** Expands an EAN-8 to 13 digits per GS1 padding rules. */
    private static String ean8ToEan13(String ean8) {
        String base12 = "00000" + ean8.substring(0, 7);   // pad + first 7
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = base12.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;              // EAN weighting
        }
        int check = (10 - (sum % 10)) % 10;
        return base12 + check;
    }

    /** Expands UPC-E (6 or 8 digits) to UPC-A, then adds leading 0 for GTIN-13. */
    private static String upcEToEan13(String upcE) {
        if (!upcE.matches("\\d{6,8}")) {          // 6, 7, or 8 numeric digits only
            throw new IllegalArgumentException("Invalid UPC-E: " + upcE);
        }
        // 6-digit ⇒ assume number-system 0, still missing check digit
        if (upcE.length() == 6) {
            upcE = '0' + upcE;                 // now 7 digits
        }

        // 7-digit ⇒ derive the check digit
        if (upcE.length() == 7) {
            upcE += calcUpcECheck(upcE);       // now 8 digits
        }

        // at this point we always have 8 digits
        String upcA = UPCEReader.convertUPCEtoUPCA(upcE); // 12-digit UPC-A
        return '0' + upcA;                                 // 13-digit GTIN
    }

    /** Calculate UPC-E check digit via temporary UPC-A expansion. */
    private static int calcUpcECheck(String upcE7) {
        // Expand to UPC-A with dummy check digit so ZXing accepts it
        String upcA11 = UPCEReader.convertUPCEtoUPCA(upcE7 + '0').substring(0, 11);

        // Standard mod-10 calculation
        int sum = 0;
        for (int i = 0; i < 11; i++) {
            int d = upcA11.charAt(i) - '0';
            sum += (i % 2 == 0) ? d * 3 : d;
        }
        return (10 - (sum % 10)) % 10;
    }

    /**
     * User-friendly display version:
     *   • drop five leading 0s from EAN-8-derived codes
     *   • drop one leading 0 from UPC-A-derived codes
     */
    public static String displayCode(String canonical) {
        if (canonical == null) return "";
        if (canonical.length() == 13 && canonical.startsWith("0")) { return canonical.substring(1); }
        return canonical;
    }
}
