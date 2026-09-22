package dev.drytis.pdfform.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All user-editable values of Form GSTR-3B, keyed by field name.
 * Numeric table cells default to "0.00"; dash cells to "-".
 */
public class Gstr3bData {

    private final Map<String, String> values = new LinkedHashMap<>();

    public static final String[] META_FIELDS = {
            "year", "period", "gstin", "legalName", "tradeName", "arn", "arnDate"
    };

    // table 3.1 rows a-e, columns: taxableValue, igst, cgst, sgst, cess
    public static final String[] T31_ROWS = {"a", "b", "c", "d", "e"};
    // table 3.1.1 rows i, ii
    public static final String[] T311_ROWS = {"i", "ii"};
    // table 3.2 rows: unregistered, composition, uin — columns taxableValue, igst
    public static final String[] T32_ROWS = {"unreg", "comp", "uin"};
    // table 4: sections A(1-3), B(1-2), C, D(1-2) x igst,cgst,sgst,cess
    public static final String[] T4_ROWS = {
            "a1", "a2", "a3", "a4", "a5",
            "b1", "b2", "c", "d", "d1", "d2"
    };
    // table 5 rows: composition/exempt, nonGST — columns inter, intra
    public static final String[] T5_ROWS = {"exempt", "nonGst"};
    // table 5.1 rows: interestComputed(-), interestPaid, lateFee — igst,cgst,sgst,cess
    public static final String[] T51_ROWS = {"interestComputed", "interestPaid", "lateFee"};
    // table 6.1 sections A, B rows igst,cgst,sgst,cess x payable,adjustment,net, itcI,itcC,itcS,itcCess, cash, interestCash, lateFeeCash
    public static final String[] T61_ROWS = {"aIgst", "aCgst", "aSgst", "aCess", "bIgst", "bCgst", "bSgst", "bCess"};
    // breakup of previous period: igst,cgst,sgst,cess (period label = period + year)
    public static final String[] BREAKUP_COLS = {"igst", "cgst", "sgst", "cess"};
    // verification
    public static final String[] VERIFY_FIELDS = {"verifyDate", "signatoryName", "designation"};

    public Gstr3bData() {
        defaults();
    }

    public void defaults() {
        put("year", "2026-27");
        put("period", "August");
        put("gstin", "");
        put("legalName", "");
        put("tradeName", "");
        put("arn", "");
        put("arnDate", "");
        for (String r : T31_ROWS) for (String c : new String[]{"val", "igst", "cgst", "sgst", "cess"}) put("t31_" + r + "_" + c, "0.00");
        for (String r : T311_ROWS) for (String c : new String[]{"val", "igst", "cgst", "sgst", "cess"}) put("t311_" + r + "_" + c, "0.00");
        for (String r : T32_ROWS) for (String c : new String[]{"val", "igst"}) put("t32_" + r + "_" + c, "0.00");
        for (String r : T4_ROWS) for (String c : new String[]{"igst", "cgst", "sgst", "cess"}) put("t4_" + r + "_" + c, "0.00");
        for (String r : T5_ROWS) for (String c : new String[]{"inter", "intra"}) put("t5_" + r + "_" + c, "0.00");
        for (String c : new String[]{"igst", "cgst", "sgst", "cess"}) put("t51_interestComputed_" + c, "-");
        for (String r : T51_ROWS) if (!r.equals("interestComputed")) for (String c : new String[]{"igst", "cgst", "sgst", "cess"}) put("t51_" + r + "_" + c, "0.00");
        for (String r : T61_ROWS) for (String c : new String[]{"payable", "adj", "net", "itcI", "itcC", "itcS", "itcCess", "cash", "intCash", "lfCash"}) put("t61_" + r + "_" + c, "0.00");
        for (String c : BREAKUP_COLS) put("breakup_" + c, "0.00");
        put("verifyDate", "");
        put("signatoryName", "");
        put("designation", "");
    }

    /** Store a value; numbers are normalized to plain decimal format. */
    public void put(String key, String value) {
        if (value == null) value = "";
        values.put(key, value.trim());
    }

    public String get(String key) {
        return values.getOrDefault(key, "");
    }

    /** Numeric getter — returns "0.00" for blank/invalid. */
    public String num(String key) {
        String v = get(key);
        if (v.isEmpty() || v.equals("-")) return v.isEmpty() ? "0.00" : "-";
        try {
            return String.format(java.util.Locale.US, "%.2f", Double.parseDouble(v));
        } catch (NumberFormatException e) {
            return "0.00";
        }
    }

    public Map<String, String> all() {
        return values;
    }

    /** Getter for Thymeleaf/Spring form binding: th:field="*{values['key']}". */
    public Map<String, String> getValues() {
        return values;
    }
}
