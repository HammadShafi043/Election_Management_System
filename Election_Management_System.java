// ----------- File: Election_Management_System.java -----------
package election_management_system;

import javax.swing.*;
import java.io.IOException;
import java.net.*;

public class Election_Management_System {
    public static final String SERVER_IP   = "192.168.1.20";   // ✅ real IP
    public static final int    SERVER_PORT = 12346;

    public static void main(String[] args) { 

        /* ------------------------------------------------------------------
         * 1)  PICK THE *SAME* LOOK‑AND‑FEEL YOU SEE IN NETBEANS’ PREVIEW
         * ------------------------------------------------------------------ */
        setNimbusLookAndFeel();          //  <-- call this BEFORE *any* Swing code

        /* ------------------------------------------------------------------
         * 2)  CHECK SERVER REACHABILITY
         * ------------------------------------------------------------------ */
        if (!checkServerConnection(SERVER_IP.trim(), SERVER_PORT)) {
            System.out.println("✗ Remote server not reachable, trying localhost…");
            if (!checkServerConnection("127.0.0.1", SERVER_PORT)) {
                JOptionPane.showMessageDialog(null,
                        "✗ Unable to connect to server (remote or local). " +
                        "Make sure the server is running.",
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        System.out.println("✔ Server reachable, launching client…");

        /* ------------------------------------------------------------------
         * 3)  SHOW THE APP
         * ------------------------------------------------------------------ */
        SwingUtilities.invokeLater(() -> {
            SignUpForm form = new SignUpForm();
            form.setLocationRelativeTo(null);
            form.setVisible(true);
        });
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper to force the Nimbus Look‑and‑Feel                              */
    /* ---------------------------------------------------------------------- */
    private static void setNimbusLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;                             // success!
                }
            }
            // Nimbus missing? fall back to the platform default:
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();                       // keep going with Metal
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Simple reachability test                                               */
    /* ---------------------------------------------------------------------- */
    private static boolean checkServerConnection(String serverIP, int serverPort) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverIP, serverPort), 2000);
            return true;
        } catch (IOException e) {
            System.out.println("✗ Failed to connect to server at "
                               + serverIP + ":" + serverPort);
            return false;
        }
    }
}
