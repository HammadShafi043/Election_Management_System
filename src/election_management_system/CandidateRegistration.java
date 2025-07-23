package election_management_system;

import javax.swing.*;
import java.util.*;

/**
 * Static helper – call from your “Register Candidate” JButton.
 *
 * txtCNIC : JTextField with CNIC
 * cmbType : JComboBox with values "NA" or "PP"
 */
public class CandidateRegistration {

    public static void registerCandidate(JTextField txtCNIC,
                                         JComboBox<String> cmbType) {

        /* -------- basic validation -------- */
        String raw = txtCNIC.getText().trim();
        String cnic = raw.replaceAll("[^0-9]", "");
        if (cnic.length() != 13) {
            JOptionPane.showMessageDialog(null, "CNIC must be 13 digits",
                    "Input error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String seatType = Objects.toString(cmbType.getSelectedItem(), "");
        if (!seatType.equalsIgnoreCase("NA") && !seatType.equalsIgnoreCase("PP")) {
            JOptionPane.showMessageDialog(null, "Select NA or PP",
                    "Input error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        /* -------- 1. verify CNIC exists -------- */
        String check = ServerClient.sendRequest("verifyCNIC", cnic);
        if (!"OK".equalsIgnoreCase(check)) {
            JOptionPane.showMessageDialog(null, "CNIC not found in Users table.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        /* -------- 2. get user name (needed for candidate row) -------- */
        String name = ServerClient.sendRequest("getUserName", cnic);
        if (name.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(null, name,
                    "Server error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        /* -------- 3. constituency seat for that CNIC -------- */
        String cityCode = cnic.substring(0, 5);
        String conResp = ServerClient.sendRequest("getConstituency", cityCode);
        String[] con = conResp.split(",", -1);              // 6 fields
        if (con.length != 6) {
            JOptionPane.showMessageDialog(null, "Constituency not found for city code.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String seat = seatType.equalsIgnoreCase("NA") ? con[4] : con[5];

        /* -------- 4. get party list -------- */
        String partyCSV = ServerClient.sendRequest("getParties", "");
        if (partyCSV.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(null, partyCSV,
                    "Server error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // server sends Party|Symbol;Party|Symbol;...
        String[] pairs = partyCSV.split(";");
        if (pairs.length == 0 || pairs[0].isBlank()) {
            JOptionPane.showMessageDialog(null, "No parties available.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<String> partyNames  = new ArrayList<>();
        List<String> partySymbols = new ArrayList<>();
        for (String pr : pairs) {
            String[] ps = pr.split("\\|", 2);
            partyNames.add(ps[0]);
            partySymbols.add(ps.length > 1 ? ps[1] : "");
        }

        String selParty = (String) JOptionPane.showInputDialog(null,
                "Select Party", "Party selection",
                JOptionPane.PLAIN_MESSAGE, null,
                partyNames.toArray(), partyNames.get(0));
        if (selParty == null) return;
        int idx = partyNames.indexOf(selParty);
        String selSymbol = partySymbols.get(idx);

        /* -------- 5. check seat already taken by that party -------- */
        String chk = ServerClient.sendRequest("checkCandidate",
                seatType + "," + seat + "," + selParty);
        if ("EXISTS".equalsIgnoreCase(chk)) {
            JOptionPane.showMessageDialog(null,
                    "This party already has a candidate for that seat.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!"OK".equalsIgnoreCase(chk)) {   // server error
            JOptionPane.showMessageDialog(null, chk,
                    "Server error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        /* -------- 6. register candidate -------- */
        String csv = String.join(",", cnic, name, seatType, seat, selParty, selSymbol);
        String rep = ServerClient.sendRequest("registerCandidate", csv);

        if ("SUCCESS".equalsIgnoreCase(rep))
            JOptionPane.showMessageDialog(null, "Candidate registered!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        else
            JOptionPane.showMessageDialog(null, "Failed: " + rep,
                    "Error", JOptionPane.ERROR_MESSAGE);
    }
}
