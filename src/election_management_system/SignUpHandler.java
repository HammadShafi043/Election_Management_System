package election_management_system;

import java.security.MessageDigest;

public class SignUpHandler {

    /* called by SignUpForm */
    public static boolean signUp(String name, String cnic, String phone, String password) {
        return new SignUpHandler().signUpInternal(name, cnic, phone, password);
    }

    private boolean signUpInternal(String name, String cnic, String phone, String password) {

        String clean = cnic.replaceAll("-", "");

        /* 1. local validation */
        if (!clean.matches("^[1-7]\\d{12}$"))                                { alert("Invalid CNIC."); return false; }
        if (!phone.matches("^03\\d{9}$"))                                    { alert("Invalid phone."); return false; }
        if (!password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$"))   { alert("Weak password."); return false; }

        /* 2. duplicate CNIC check on server */
        String dup = ServerClient.sendRequest("checkCNIC", clean);
        if ("DUPLICATE".equalsIgnoreCase(dup)) { alert("CNIC already exists."); return false; }
        if (!"OK".equalsIgnoreCase(dup))       { alert("Server error: " + dup); return false; }

        /* 3. constituency lookup */
        String cityCode = clean.substring(0, 5);
        String con = ServerClient.sendRequest("getConstituency", cityCode);
        String[] c = con.split(",", -1);
        if (c.length != 6) { alert("City code not found."); return false; }

        /* 4. hash password + gender */
        String hash;
        try { hash = sha256(password); } catch (Exception e) { alert("Hashing error."); return false; }
        String gender = (Character.getNumericValue(clean.charAt(12)) % 2 == 0) ? "Female" : "Male";

        /* 5. build CSV & send signupUser */
        String csv = String.join(",", name, clean, phone, hash,
                                 c[0], c[1], c[2], c[3], c[4], c[5], gender);

        String reply = ServerClient.sendRequest("signupUser", csv);
        if ("SUCCESS".equalsIgnoreCase(reply)) return true;

        alert("Signup failed: " + reply);
        return false;
    }

    /* -------- helpers -------- */

    private void alert(String msg) { System.out.println(msg); }

    private String sha256(String pw) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] h = md.digest(pw.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : h) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
