package election_management_system;

import javax.swing.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;

public final class ElectionTimeRegistrar {

    private ElectionTimeRegistrar() {}

    /* ─────────────────────────────────────────────────────────── */
    /* 1)  START / UPDATE                                          */
    /* ─────────────────────────────────────────────────────────── */
    public static void register(JTextField startField,
                                JTextField stopField,
                                String     registeredBy) {

        DateTimeFormatter humanFmt = DateTimeFormatter.ofPattern("h:mm a");
        LocalTime start, stop;
        try {
            start = LocalTime.parse(startField.getText().trim().toUpperCase(), humanFmt);
            stop  = LocalTime.parse(stopField .getText().trim().toUpperCase(), humanFmt);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(startField,
                    "Time must look like 09:00 am or 5:30 PM",
                    "Format Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!stop.isAfter(start)) {
            JOptionPane.showMessageDialog(startField,
                    "Stop‑time must be *after* start‑time.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate today   = LocalDate.now();
        LocalDateTime sDT = today.atTime(start);
        LocalDateTime eDT = today.atTime(stop);

        if (eDT.isBefore(LocalDateTime.now())) {
            /* window already gone → pick one of next 5 days */
            String[] nextFive = new String[5];
            for (int i = 0; i < 5; i++) nextFive[i] = today.plusDays(i + 1).toString();

            String chosen = (String) JOptionPane.showInputDialog(
                    startField,
                    "Today’s window is already past.\nSelect another date:",
                    "Select date",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    nextFive,
                    nextFive[0]);
            if (chosen == null) return; // cancelled
            LocalDate newDay = LocalDate.parse(chosen);
            sDT = newDay.atTime(start);
            eDT = newDay.atTime(stop);
        }

        DateTimeFormatter sqlFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String payload = sDT.format(sqlFmt) + ',' + eDT.format(sqlFmt) + ',' + registeredBy;

        String reply = ServerClient.sendRequest("registerElectionTime", payload);

        if (reply.startsWith("OK")) {
            JOptionPane.showMessageDialog(startField,
                    reply.replace("OK: ", "") + "\n" + sDT + "  →  " + eDT,
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(startField, reply,
                    "Server Response", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ─────────────────────────────────────────────────────────── */
    /* 2)  FORCE‑FINISH                                           */
    /* ─────────────────────────────────────────────────────────── */
    public static void stop(String registeredBy) {
        int ans = JOptionPane.showConfirmDialog(null,
                "Are you sure you want to STOP the current election window?",
                "Confirm Stop", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;

        String reply = ServerClient.sendRequest("stopElectionTime", registeredBy);
        if (reply.startsWith("OK")) {
            JOptionPane.showMessageDialog(null, reply.replace("OK: ", ""),
                    "Election Finished", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, reply,
                    "Server Response", JOptionPane.ERROR_MESSAGE);
        }
    }
    
 public static void handleVote(JTable cast_T,
                                  String voterCnic,
                                  java.awt.Component parent) {

        /* 1) election status */
        String status = ServerClient.sendRequest("getElectionStatus", "").trim();
        if (status.startsWith("ERROR")) { error(parent,status); return; }

        switch (status.toLowerCase()) {
            case "scheduled" -> { info(parent,"⏳  Election not started yet."); return; }
            case "finished"  -> { info(parent,"🛑  Voting has finished.");     return; }
            case "started"   -> { /* proceed */ }
            default          -> { info(parent,"No active election window.");   return; }
        }

        /* 2) locate columns dynamically */
        DefaultTableModel m = (DefaultTableModel) cast_T.getModel();
        int seatCol = findColumn(m,"Seat");
        int cnicCol = findColumn(m,"CNIC");
        if (seatCol < 0 || cnicCol < 0) {
            error(parent,"Table missing \"Seat\" or \"CNIC\" column.");
            return;
        }

        /* detect available seat types */
        boolean hasNA=false, hasPP=false;
        for (int r=0;r<m.getRowCount();r++){
            String seat=m.getValueAt(r,seatCol).toString().toUpperCase();
            if (seat.startsWith("NA")) hasNA=true; else hasPP=true;
        }
        if (!hasNA && !hasPP) { info(parent,"No candidates available."); return; }

        /* 3) validate selection */
        int[] selRows = cast_T.getSelectedRows();
        Integer naRow=null, ppRow=null;
        for (int row: selRows){
            String seat=m.getValueAt(row,seatCol).toString().toUpperCase();
            if (seat.startsWith("NA")){
                if (naRow!=null){ warn(parent,"Select only ONE NA candidate."); return; }
                naRow=row;
            } else {
                if (ppRow!=null){ warn(parent,"Select only ONE PP candidate."); return; }
                ppRow=row;
            }
        }
        if (hasNA && hasPP && (naRow==null||ppRow==null)){
            warn(parent,"Select ONE NA candidate and ONE PP candidate."); return;
        }
        if (hasNA && !hasPP && naRow==null){ warn(parent,"Select ONE NA candidate."); return; }
        if (hasPP && !hasNA && ppRow==null){ warn(parent,"Select ONE PP candidate."); return; }

        /* 4) send vote(s) */
        List<String> errors = new ArrayList<>();

        if (naRow!=null){
            String seat = m.getValueAt(naRow,seatCol).toString();
            String cand = m.getValueAt(naRow,cnicCol).toString();
            String rep  = ServerClient.sendRequest("castVote",
                          voterCnic + "," + seat + "," + cand);
            if (!rep.startsWith("OK")) errors.add("NA → "+rep);
        }
        if (ppRow!=null){
            String seat = m.getValueAt(ppRow,seatCol).toString();
            String cand = m.getValueAt(ppRow,cnicCol).toString();
            String rep  = ServerClient.sendRequest("castVote",
                          voterCnic + "," + seat + "," + cand);
            if (!rep.startsWith("OK")) errors.add("PP → "+rep);
        }

        /* 5) feedback */
        if (errors.isEmpty()) {
            info(parent,"✅  Your vote has been recorded.\nThank you!");
            cast_T.clearSelection();
        } else {
            error(parent,String.join("\n", errors));
        }
    }

    /* ============================================================ *
     *  SHARED RENDER / DIALOG HELPERS
     * ============================================================ */

    private static void renderTable(JTable table, String resp, String[] cols) {
        if (resp.startsWith("ERROR")) { error(table,resp); return; }

        DefaultTableModel model = new DefaultTableModel(cols,0);
        if (!resp.isBlank()) {
            for (String row : resp.split(";"))
                model.addRow(row.split(",", cols.length));
        }
        table.setModel(model);
    }

    private static int findColumn(DefaultTableModel m, String header) {
        for (int i=0;i<m.getColumnCount();i++)
            if (header.equalsIgnoreCase(m.getColumnName(i))) return i;
        return -1;            // not found
    }

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