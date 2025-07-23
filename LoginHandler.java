package election_management_system;

import java.security.MessageDigest;
import javax.swing.*;

public class LoginHandler {

    /* ---------- LOGIN ---------- */
    public static boolean loginFunction(String cnic, String password) {
        try {
            String clean = cnic.replaceAll("-", "");
            String hash  = sha256(password);

            String reply = ServerClient.sendRequest("login", clean + "," + hash);

            switch (reply.toUpperCase()) {
                case "ADMIN"  -> { new AdminForm().setVisible(true); return true; }
                case "VOTER"  -> { new VoterForm().setVisible(true); return true; }
                case "INVALID" -> JOptionPane.showMessageDialog(null,
                        "Invalid CNIC or Password", "Login failed", JOptionPane.ERROR_MESSAGE);
                default        -> JOptionPane.showMessageDialog(null,
                        "Server error: " + reply, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        return false;
    }

    /* ---------- FORGOT‑PASSWORD ---------- */
    public static void forgotPassword(JFrame parent) {
        String cnic = JOptionPane.showInputDialog(parent,
                "Enter your CNIC (without dashes):", "Forgot Password",
                JOptionPane.QUESTION_MESSAGE);

        if (cnic == null || cnic.trim().isEmpty()) return;
        cnic = cnic.trim();

        /* confirm CNIC exists */
        if (!"OK".equalsIgnoreCase(ServerClient.sendRequest("verifyCNIC", cnic))) {
            JOptionPane.showMessageDialog(parent, "CNIC not found!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        /* ask for new pw twice */
        JPasswordField np = new JPasswordField();
        JPasswordField cp = new JPasswordField();
        Object[] msg = { "New Password:", np, "Confirm Password:", cp };
        if (JOptionPane.showConfirmDialog(parent, msg,
                "Reset Password", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        String newPw = new String(np.getPassword());
        String conPw = new String(cp.getPassword());

        if (!newPw.equals(conPw)) {
            JOptionPane.showMessageDialog(parent, "Passwords do not match!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!validatePassword(newPw)) {
            JOptionPane.showMessageDialog(parent,
                    "Password must be ≥8 chars and include upper, lower and digit.",
                    "Weak password", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String rep = ServerClient.sendRequest("updatePassword",
                    cnic + "," + sha256(newPw));
            JOptionPane.showMessageDialog(parent,
                    "SUCCESS".equalsIgnoreCase(rep) ? "Password updated!"
                                                     : "Failed: " + rep);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    /* ---------- helpers ---------- */
    private static boolean validatePassword(String pw) {
        return pw.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");
    }
    private static String sha256(String pw) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] h = md.digest(pw.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : h) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
