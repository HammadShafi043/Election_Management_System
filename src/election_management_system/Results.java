/* ────────────────────────────────────────────────────────────────
 *  Results.java   –  seat‑level vote results for EMS client
 * ──────────────────────────────────────────────────────────────── */
package election_management_system;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Static helpers for displaying final vote tallies.
 *
 * Usage from your “Show Results” button:
 *
 *    Results.showSeatResults(C_N, R_T, btnShowResults);
 *
 * Where
 *   • C_N  – JTextField holding the seat label (e.g. NA‑101, PP‑110)
 *   • R_T  – JTable that will display the results
 *   • parent – any Swing component for centering dialogs
 */
public final class Results {

    private Results() {}          // static‑only class

    /* =============================================================
     *  Show results for a single constituency seat
     * ============================================================= */
    public static void showSeatResults(JTextField C_N,
                                       JTable     R_T,
                                       java.awt.Component parent) {

        /* 1️⃣  validate seat format -------------------------------- */
        String seat = C_N.getText().trim().toUpperCase();
        if (!seat.matches("^(NA|PP)-\\d+$")) {
            warn(parent,"Seat must look like NA‑123 or PP‑456.");
            return;
        }

        /* 2️⃣  ask the server -------------------------------------- */
        String resp = ServerClient.sendRequest("getSeatResults", seat);

        if (resp.startsWith("ERROR")) { error(parent,resp); return; }
        if ("NO_RESULTS".equals(resp)) {
            info(parent,"No results yet for "+seat+
                       " (election not finished or no votes cast).");
            R_T.setModel(new DefaultTableModel(
                    new String[]{"Candidate CNIC","Candidate","Party","Votes"}, 0));
            return;
        }

        /* 3️⃣  build table + TOTAL row ----------------------------- */
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Candidate CNIC","Candidate","Party","Votes"}, 0);

        int total = 0;
        for (String row : resp.split(";")) {               // candCNIC,candName,party,votes
            String[] parts = row.split(",", 4);
            total += Integer.parseInt(parts[3]);
            model.addRow(parts);
        }
        model.addRow(new Object[]{"","","",""});
        model.addRow(new Object[]{"—","TOTAL VOTES","—", total});

        R_T.setModel(model);
    }
    
    /* ======================================================================
 *  PARTY RESULTS  (calls getPartyResults on the server)
 * ====================================================================== */
public static void showPartyResults(JTextField P_N,
                                    JTable     R_T,
                                    java.awt.Component parent) {

    String party = P_N.getText().trim();
    if (party.isEmpty()) {
        JOptionPane.showMessageDialog(parent,
                "Enter a party name.", "Input required",
                JOptionPane.WARNING_MESSAGE);
        return;
    }

    String resp = ServerClient.sendRequest("getPartyResults", party);

    if (resp.startsWith("ERROR")) {
        JOptionPane.showMessageDialog(parent, resp,
                "Server error", JOptionPane.ERROR_MESSAGE);
        return;
    }
    if ("NO_RESULTS".equals(resp)) {
        JOptionPane.showMessageDialog(parent,
                "No results yet for \"" + party + "\".",
                "No data", JOptionPane.INFORMATION_MESSAGE);
        R_T.setModel(new DefaultTableModel(
                new String[]{"Seat","Votes"}, 0));
        return;
    }

    DefaultTableModel model = new DefaultTableModel(
            new String[]{"Seat","Votes"}, 0);

    int total = 0;
    for (String row : resp.split(";")) {       // seat,votes
        String[] p = row.split(",", 2);
        total += Integer.parseInt(p[1]);
        model.addRow(p);
    }
    model.addRow(new Object[]{"",""});
    model.addRow(new Object[]{"TOTAL VOTES", total});

    R_T.setModel(model);
}


    /* =============================================================
     *  Small dialog helpers
     * ============================================================= */
    private static void info (java.awt.Component p,String msg) {
        JOptionPane.showMessageDialog(p,msg,"Info",JOptionPane.INFORMATION_MESSAGE);
    }
    private static void warn (java.awt.Component p,String msg) {
        JOptionPane.showMessageDialog(p,msg,"Warning",JOptionPane.WARNING_MESSAGE);
    }
    private static void error(java.awt.Component p,String msg) {
        JOptionPane.showMessageDialog(p,msg,"Error",JOptionPane.ERROR_MESSAGE);
    }
}
