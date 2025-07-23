package election_management_system;

import java.io.*;
import java.net.*;

/**
 * One-liner socket helper.
 * Returns the first server reply line,
 * or "ERROR:<description>" if connection fails.
 */
public class ServerClient {

    private static final String SERVER_IP   = Election_Management_System.SERVER_IP;
    private static final int    SERVER_PORT = Election_Management_System.SERVER_PORT;

    public static String sendRequest(String cmd, String data) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), 2000);

            PrintWriter  out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            out.println(cmd + ";" + (data == null ? "" : data));
            return in.readLine();

        } catch (IOException e) {
            /* include exception class for clarity */
            return "ERROR:" + e.getClass().getSimpleName() + " – " + e.getMessage();
        }
    }
    
    
        public static int getVotersNumber() {
        try (
            Socket sock = new Socket(SERVER_IP, SERVER_PORT);
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
        ) {
            out.println("getVotersNumber");
            String res = in.readLine();
            return Integer.parseInt(res);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
        
    public static int getPartiesCount() {
    try (
        Socket sock = new Socket(SERVER_IP, SERVER_PORT);
        PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
    ) {
        out.println("getPartiesCount");
        String res = in.readLine();
        return Integer.parseInt(res);
    } catch (Exception e) {
        e.printStackTrace();
        return 0;
    }
}

public static int getCandidatesCount() {
    try (
        Socket sock = new Socket(SERVER_IP, SERVER_PORT);
        PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
    ) {
        out.println("getCandidatesCount");
        String res = in.readLine();
        return Integer.parseInt(res);
    } catch (Exception e) {
        e.printStackTrace();
        return 0;
    }
}


    public static boolean ping() { return "pong".equalsIgnoreCase(sendRequest("ping", "")); }
}
