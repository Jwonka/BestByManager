package com.example.bestbymanager;

import com.example.bestbymanager.utilities.BarcodeUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class BarcodeUtilTest {

    @Test
    public void toCanonical_upcA_returnsExpectedGtin13() {
        String canonical = BarcodeUtil.toCanonical("036000291452");
        assertEquals("0036000291452", canonical);
    }

    @Test
    public void displayCode_upcACanonical_returnsUpcA() {
        String canonical = "0036000291452";
        assertEquals("036000291452", BarcodeUtil.displayCode(canonical));
    }

    @Test
    public void toCanonical_ean8_returnsExpectedGtin13() {
        String canonical = BarcodeUtil.toCanonical("55123457");
        assertEquals("0000055123457", canonical);
    }

    @Test
    public void displayCode_ean8Canonical_returnsEan8() {
        String canonical = "0000055123457";
        assertEquals("55123457", BarcodeUtil.displayCode(canonical));
    }

    @Test
    public void toCanonical_upcE8_returnsExpectedGtin13() {
        String canonical = BarcodeUtil.toCanonical("04252614");
        assertEquals("0042100005264", canonical);
    }

    @Test
    public void toCanonical_upcE6_calculatesCheckDigit() {
        String canonical = BarcodeUtil.toCanonical("425261");
        assertEquals("0042100005264", canonical);
    }

    @Test
    public void displayCode_upcECanonical_returnsUpcA() {
        String canonical = "0042100005264";
        assertEquals("042100005264", BarcodeUtil.displayCode(canonical));
    }

    @Test
    public void toCanonical_invalidLength_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BarcodeUtil.toCanonical("12345"));
        assertTrue(exception.getMessage().contains("Barcode must be 6, 7, 8, 12, or 13 digits"));
    }

    @Test
    public void toCanonical_noDigits_throws() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BarcodeUtil.toCanonical("abc"));
        assertTrue(exception.getMessage().contains("Barcode must be 6, 7, 8, 12, or 13 digits"));
    }

    @Test
    public void displayCode_nonCanonical_returnsInput() {
        assertEquals("123", BarcodeUtil.displayCode("123"));
    }
}
