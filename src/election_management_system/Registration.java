package election_management_system;

import javax.swing.*;
import java.util.Objects;

public class Registration {

    /* =============================================================
       A) ADD CONSTITUENCY
    ============================================================= */
    public static void addConstituency(JTextField cityCodeField,
                                       JTextField provinceField,
                                       JTextField divisionField,
                                       JTextField districtField,
                                       JTextField cityField,
                                       JTextField naSeatField,
                                       JTextField ppSeatField) {

        /* ------- collect + validate ------- */
        String cityCode = cityCodeField.getText().trim().toUpperCase();
        String province = capWords(provinceField.getText());
        String division = capWords(divisionField.getText()) + " Division";
        String district = capWords(districtField.getText()) + " District";
        String city     = capWords(cityField.getText());
        String naSeat   = naSeatField.getText().trim().toUpperCase();
        String ppSeat   = ppSeatField.getText().trim().toUpperCase();

        if (cityCode.isEmpty() || province.isEmpty() || division.isEmpty() ||
            district.isEmpty() || city.isEmpty() || naSeat.isEmpty() || ppSeat.isEmpty()) {
            JOptionPane.showMessageDialog(null, "All fields are required.",
                    "Input error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!cityCode.matches("\\d{5}")) {
            JOptionPane.showMessageDialog(null, "City‑code must be 5 digits.",
                    "Validation error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        /* ------- ask server ------- */
        String csv = String.join(",", cityCode, province, division,
                                   district, city, naSeat, ppSeat);
        String rep = ServerClient.sendRequest("addConstituency", csv);

        if ("SUCCESS".equalsIgnoreCase(rep)) {
            JOptionPane.showMessageDialog(null, "Constituency added!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            clear(cityCodeField, provinceField, divisionField,
                  districtField, cityField, naSeatField, ppSeatField);
        } else {
            JOptionPane.showMessageDialog(null, "Server reply: " + rep,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* =============================================================
       B) REGISTER PARTY
    ============================================================= */
    public static void registerParty(JTextField txtName,
                                     JTextField txtSymbol,
                                     JTextField txtCnic) {

        String partyName = toTitle(txtName.getText().trim());
        String symbol    = toTitle(txtSymbol.getText().trim());
        String cnic      = txtCnic.getText().trim();

        if (partyName.isEmpty() || symbol.isEmpty() || cnic.isEmpty()) {
            JOptionPane.showMessageDialog(null, "All fields are required.",
                    "Input error", JOptionPane.ERROR_MESSAGE); return;
        }

        /* 1. verify CNIC belongs to a User */
        String chk = ServerClient.sendRequest("verifyCNIC", cnic);
        if (!"OK".equalsIgnoreCase(chk)) {
            JOptionPane.showMessageDialog(null, "CNIC not found in Users table.",
                    "Error", JOptionPane.ERROR_MESSAGE); return;
        }

        /* 2. send registerParty command */
        String csv = String.join(",", partyName, symbol, cnic);
        String rep = ServerClient.sendRequest("registerParty", csv);

        if ("SUCCESS".equalsIgnoreCase(rep)) {
            JOptionPane.showMessageDialog(null, "Party registered!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            clear(txtName, txtSymbol, txtCnic);
        } else {
            JOptionPane.showMessageDialog(null, rep,
                    "Server error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* =============================================================
       helpers
    ============================================================= */
    private static void clear(JTextField... f) { for (JTextField t : f) t.setText(""); }

    private static String capWords(String s) {
        StringBuilder sb = new StringBuilder();
        for (String w : s.toLowerCase().trim().split("\\s+")) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0)))
                                .append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
    private static String toTitle(String s) { return capWords(s); }
}
