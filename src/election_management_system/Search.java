package election_management_system;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

public class Search {

    /* ---------------------- CANDIDATES ---------------------- */
    public static void searchAllCandidates(JTable table) {
        renderTable(table,
                ServerClient.sendRequest("getAllCandidates", ""),
                new String[]{"CNIC","Name","Type","Seat","Party","Symbol"});
    }

    public static void searchCandidatesBySeat(JTextField txtSeat, JTable table) {
        String seat = txtSeat.getText().trim().toUpperCase();
        if (!seat.matches("^(NA|PP)-\\d+$")) {
            warn(table, "Seat must look like NA‑123 or PP‑456.");
            return;
        }

        String resp = ServerClient.sendRequest("getCandidatesBySeat", seat);

        switch (resp) {
            case "NO_SEAT" -> { info(table, "No such constituency seat exists."); return; }
            case "NO_CANDIDATE" -> { info(table, "No candidate registered for this seat."); return; }
        }

        renderTable(table, resp,
                new String[]{"CNIC","Name","Type","Seat","Party","Symbol"});
    }

    /* ---------------------- PARTIES ---------------------- */
    public static void searchAllParties(JTable table) {
        renderTable(table,
                ServerClient.sendRequest("getAllParties", ""),
                new String[]{"Party","Symbol","Leader CNIC"});
    }

    public static void searchPartyByName(JTextField txt, JTable table) {
        String key = txt.getText().trim();
        if (key.isEmpty()) {
            info(table, "Enter party name or symbol to search.");
            return;
        }

        String resp = ServerClient.sendRequest("searchPartyByName", key);

        if ("NO_MATCH".equals(resp)) {
            info(table, "No party matches your search.");
            return;
        }

        renderTable(table, resp, new String[]{"Party","Symbol","Leader CNIC"});
    }

    /* ---------------------- VOTERS ---------------------- */
    public static void searchAllVoters(JTable table) {
        renderTable(table,
                ServerClient.sendRequest("getAllVoters", ""),
                new String[]{"CNIC","Name","Phone","City","Status"});
    }

    public static void searchVoterByCnic(JTextField txt, JTable table) {
        String cnic = txt.getText().trim().replaceAll("[^0-9]", "");
        if (!cnic.matches("\\d{13}")) {
            warn(table, "Enter a 13‑digit CNIC (without dashes).");
            return;
        }

        String resp = ServerClient.sendRequest("searchVoterByCnic", cnic);
        if ("NO_MATCH".equals(resp)) {
            info(table, "No voter with this CNIC.");
            return;
        }

        renderTable(table, resp, new String[]{"CNIC","Name","Phone","City","Status"});
    }

    /* ------------------ Helpers ------------------ */
    private static void renderTable(JTable table, String resp, String[] cols) {
        if (resp.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(table, resp,
                    "Server error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultTableModel model = new DefaultTableModel(cols, 0);

        if (!resp.isBlank()) {
            for (String row : resp.split(";"))
                model.addRow(row.split(",", cols.length));
        }
        table.setModel(model);
    }

    private static void info(JTable t, String msg) {
        JOptionPane.showMessageDialog(t, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void warn(JTable t, String msg) {
        JOptionPane.showMessageDialog(t, msg, "Warning", JOptionPane.WARNING_MESSAGE);
    }

/* ------------------------------------------------------------
 *  Remove ALL ListSelectionListeners from the table’s model
 * ------------------------------------------------------------ */
private static void resetSelectionListeners(JTable t) {
    javax.swing.DefaultListSelectionModel sel =
        (javax.swing.DefaultListSelectionModel) t.getSelectionModel();

    for (javax.swing.event.ListSelectionListener l : sel.getListSelectionListeners()) {
        sel.removeListSelectionListener(l);
    }
}


    /* ---------------- Confirm Voter & Show Candidates ---------------- */
/* ------------------------------------------------------------
 * Confirm voter & load NA + PP candidates into one JTable
 * ------------------------------------------------------------ */
public static void confirmVoterRegistration(JTextField txtCnic,
                                            JTable      table,
                                            java.awt.Component parent) {

    /* 1️⃣  Same CNIC pattern as before */
    String cnic = txtCnic.getText().trim().replaceAll("[^0-9]", "");
    if (!cnic.matches("\\d{13}")) {
        JOptionPane.showMessageDialog(parent,
                "Enter a 13‑digit CNIC (without dashes).",
                "Format error", JOptionPane.WARNING_MESSAGE);
        return;
    }

    /* 2️⃣  Ask the server */
    String resp = ServerClient.sendRequest("searchVoterByCnic", cnic);

    if ("NO_MATCH".equals(resp)) {
        JOptionPane.showMessageDialog(parent,
                "❌ This CNIC is not registered as a voter.",
                "Not found", JOptionPane.INFORMATION_MESSAGE);
        return;
    }
    if (resp.startsWith("ERROR")) {
        JOptionPane.showMessageDialog(parent, resp,
                "Server error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    /* 3️⃣  Parse the 7‑field record */
    String[] user = resp.split(",", -1);
    if (user.length < 7) {                             // expecting 7 fields
        JOptionPane.showMessageDialog(parent,
                "Malformed voter record (expected 7 fields, got "
                        + user.length + ").",
                "Data error", JOptionPane.ERROR_MESSAGE);
        return;
    }
    String naSeat = user[5].trim();    // NA‑xxx
    String ppSeat = user[6].trim();    // PP‑xxx

    /* 4️⃣  Fetch candidates for NA & PP */
    String[] headers = {"CNIC","Name","Type","Seat","Party","Symbol"};
    StringBuilder combined = new StringBuilder();
    boolean hasCand = false;

    for (String seat : new String[]{naSeat, ppSeat}) {
        if (seat.isEmpty()) continue;
        String cands = ServerClient.sendRequest("getCandidatesBySeat", seat);
        if (!cands.equals("NO_SEAT") && !cands.equals("NO_CANDIDATE")) {
            combined.append(cands).append(";");
            hasCand = true;
        }
    }

    if (!hasCand) {
        JOptionPane.showMessageDialog(parent,
                "No candidate is available in your constituency.",
                "No candidates", JOptionPane.INFORMATION_MESSAGE);
        table.setModel(new DefaultTableModel(headers, 0));
        return;
    }

    /* 5️⃣  Render to table */
    renderTable(table, combined.toString(), headers);

    JOptionPane.showMessageDialog(parent,
            "✅ Voter is registered.\nYou may now cast your vote.",
            "Success", JOptionPane.INFORMATION_MESSAGE);
}

}
