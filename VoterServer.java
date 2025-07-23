package server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.time.Instant;

/**
 * Socket‑based EMS server.
 *  • Automatic Scheduled → Started → Finished via MySQL EVENTS
 *  • One vote per voter‑seat‑election enforced by Votes.uniq_vote
 */
public class VoterServer {

    /* ────────── CONFIG ────────── */
    private static final int    PORT = 12346;
    private static final String DB   = "jdbc:mysql://localhost:3306/EMS?serverTimezone=Asia/Karachi";
    private static final String USER = "root";
    private static final String PASS = "Abdu.llah04";
    /* ──────────────────────────── */

    public static void main(String[] args) {
        System.out.println("🟢 EMS server listening on port " + PORT);
        try (ServerSocket ss = new ServerSocket(PORT)) {
            while (true) new Thread(() -> serveClient(ss)).start();
        } catch (IOException e) { e.printStackTrace(); }
    }

    /* ========================================================= */
    private static void serveClient(ServerSocket ss) {
        try (Socket sock = ss.accept();
             BufferedReader in  = new BufferedReader(new InputStreamReader(sock.getInputStream()));
             PrintWriter    out = new PrintWriter(sock.getOutputStream(), true);
             Connection cn  = DriverManager.getConnection(DB, USER, PASS))
        {
            String req = in.readLine();
            String cli = sock.getInetAddress().getHostAddress();
            System.out.println("📥 [" + cli + "] " + req);

            if (req == null || req.isBlank()) { out.println("ERROR: empty"); return; }
            String[] parts = req.split(";", 2);
            String cmd  = parts[0].trim();
            String data = parts.length > 1 ? parts[1] : "";

            String res = switch (cmd) {

                /* heartbeat */
                case "ping"                 -> "pong";

                /* ───── USERS ───── */
                case "checkCNIC"            -> checkCNIC(cn, data);
                case "verifyCNIC"           -> checkCNIC(cn, data).equals("DUPLICATE") ? "OK" : "NOT_FOUND";
                case "getConstituency"      -> getConstituency(cn, data);
                case "signupUser"           -> signupUser(cn, data);
                case "login"                -> login(cn, data);
                case "updatePassword"       -> updatePassword(cn, data);
                case "getUserName"          -> getUserName(cn, data);

                /* ───── CANDIDATES / PARTIES ───── */
                case "getParties"           -> getParties(cn);
                case "checkCandidate"       -> checkCandidate(cn, data);
                case "registerCandidate"    -> registerCandidate(cn, data);
                case "getAllCandidates"     -> getAllCandidates(cn);
                case "getCandidatesBySeat"  -> getCandidatesBySeat(cn, data);
                case "getAllParties"        -> getAllParties(cn);
                case "searchPartyByName"    -> searchPartyByName(cn, data);
                case "getPartiesCount" -> String.valueOf(getPartiesCount(cn));
                case "getCandidatesCount" -> String.valueOf(getCandidatesCount(cn));


                /* ───── VOTERS ───── */
                case "getAllVoters"         -> getAllVoters(cn);
                case "searchVoterByCnic"    -> searchVoterByCnic(cn, data);
                case "getVotersNumber" -> String.valueOf(getVotersNumber(cn));


                /* ───── ELECTION WINDOW ───── */
                case "registerElectionTime" -> registerElectionTime(cn, data);
                case "stopElectionTime"     -> stopElectionTime(cn, data);
                case "getElectionStatus"    -> getElectionStatus(cn);

                /* ───── VOTING ───── */
                case "castVote"             -> castVote(cn, data);
                case "getSeatResults"       -> getSeatResults(cn, data);
                case "getPartyResults"     -> getPartyResults(cn, data);
                case "selectElectionStopTime" -> getElectionStopTime(cn);



                /* ───── ADMIN INSERTS ───── */
                case "addConstituency"      -> addConstituency(cn, data);
                case "registerParty"        -> registerParty(cn, data);
                 
                

                default -> "ERROR: unknown cmd";
            };

            out.println(res);
            System.out.println("📤 [" + cli + "] " + res);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /* ========================================================= */
    /* 1)  ELECTION WINDOW                                       */
    /* ========================================================= */

    private static String registerElectionTime(Connection c, String data) {
        String[] p = data.split(",", 3);
        if (p.length != 3) return "ERROR:bad format";
        try {
            Timestamp start = Timestamp.valueOf(p[0].trim());
            Timestamp stop  = Timestamp.valueOf(p[1].trim());
            String by       = p[2].trim();

            Instant now = Instant.now();
            String status = now.isBefore(start.toInstant()) ? "Scheduled"
                          : now.isAfter(stop.toInstant())   ? "Finished"
                          : "Started";

            int id = upsertElectionWindow(c, start, stop, by, status);
            if (id == -1) return "ERROR:DB failure";

            scheduleAutoEvents(c, id, start, stop);
            return "OK: Election window saved (" + status + ").";
        } catch (Exception ex) { return "ERROR:" + ex.getMessage(); }
    }

    private static int upsertElectionWindow(Connection c, Timestamp s, Timestamp e,
                                            String by, String status) throws SQLException {
        String chk = "SELECT ID FROM Election_Time WHERE DATE(StartTime)=?";
        try (PreparedStatement ps = c.prepareStatement(chk)) {
            ps.setDate(1, new java.sql.Date(s.getTime()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                try (PreparedStatement up = c.prepareStatement(
                        "UPDATE Election_Time SET StartTime=?,StopTime=?,RegisteredBy=?,Status=? WHERE ID=?")) {
                    up.setTimestamp(1, s); up.setTimestamp(2, e);
                    up.setString(3, by);   up.setString(4, status);
                    up.setInt(5, id);      up.executeUpdate();
                }
                return id;
            }
        }
        try (PreparedStatement ins = c.prepareStatement(
             "INSERT INTO Election_Time (StartTime,StopTime,RegisteredBy,Status) VALUES (?,?,?,?)",
             Statement.RETURN_GENERATED_KEYS)) {
            ins.setTimestamp(1, s); ins.setTimestamp(2, e);
            ins.setString(3, by);   ins.setString(4, status);
            ins.executeUpdate();
            ResultSet gk = ins.getGeneratedKeys();
            return gk.next() ? gk.getInt(1) : -1;
        }
    }
    private static String getElectionStopTime(Connection c) {
    try (Statement st = c.createStatement();
         ResultSet rs = st.executeQuery("SELECT StopTime FROM Election_Time WHERE Status='Started' ORDER BY ID DESC LIMIT 1")) {
        return rs.next() ? rs.getTimestamp(1).toString() : null;
    } catch (SQLException e) { return "ERROR:" + e.getMessage(); }
}
    
    
private static int getVotersNumber(Connection c) {
    String sql = "SELECT COUNT(*) FROM Users WHERE Status='Voter'";
    try (Statement st = c.createStatement();
         ResultSet rs = st.executeQuery(sql)) {
        return rs.next() ? rs.getInt(1) : 0;
    } catch (SQLException e) {
        e.printStackTrace();
        return 0;
    }
}

private static int getPartiesCount(Connection c) {
    String sql = "SELECT COUNT(*) FROM Party";
    try (Statement st = c.createStatement();
         ResultSet rs = st.executeQuery(sql)) {
        return rs.next() ? rs.getInt(1) : 0;
    } catch (SQLException e) {
        e.printStackTrace();
        return 0;
    }
}

private static int getCandidatesCount(Connection c) {
    String sql = "SELECT COUNT(*) FROM candidates"; // Make sure your table is named exactly this
    try (Statement st = c.createStatement();
         ResultSet rs = st.executeQuery(sql)) {
        return rs.next() ? rs.getInt(1) : 0;
    } catch (SQLException e) {
        e.printStackTrace();
        return 0;
    }
}






    /* two EVENTS per election */
    private static void scheduleAutoEvents(Connection c,int id,Timestamp s,Timestamp e) throws SQLException {
        String evS = "start_election_"  + id;
        String evF = "finish_election_" + id;
        try (Statement st = c.createStatement()) {
            st.execute("DROP EVENT IF EXISTS " + evS);
            st.execute("DROP EVENT IF EXISTS " + evF);
        }
        String sLit = s.toString().substring(0,19);
        String eLit = e.toString().substring(0,19);

        try (Statement st = c.createStatement()) {
            st.execute("CREATE EVENT "+evS+" ON SCHEDULE AT '"+sLit+"' " +
                       "DO UPDATE Election_Time SET Status='Started' " +
                       "WHERE ID="+id+" AND Status='Scheduled'");
            st.execute("CREATE EVENT "+evF+" ON SCHEDULE AT '"+eLit+"' " +
                       "DO UPDATE Election_Time SET Status='Finished', StopTime='"+eLit+"' " +
                       "WHERE ID="+id+" AND Status!='Finished'");
        }
    }

    private static String stopElectionTime(Connection c, String by) {
        String sql = """
            UPDATE Election_Time
               SET Status='Finished', StopTime=NOW(), RegisteredBy=?
             WHERE Status IN ('Scheduled','Started')
             ORDER BY StartTime DESC LIMIT 1
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, by);
            return ps.executeUpdate()==1 ? "OK: Election window stopped."
                                         : "ERROR: No active window.";
        } catch (SQLException e) { return "ERROR:"+e.getMessage(); }
    }

    private static String getElectionStatus(Connection c) {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT Status FROM Election_Time ORDER BY ID DESC LIMIT 1")) {
            return rs.next() ? rs.getString(1) : "NONE";
        } catch (SQLException e) { return "ERROR:"+e.getMessage(); }
    }

    /* ========================================================= */
    /* 2)  VOTING                                                */
    /* ========================================================= */

    /* data = voterCnic,Seat,CandidateCnic */
    private static String castVote(Connection c, String csv) {
        String[] p = csv.split(",",3);
        if (p.length!=3) return "ERROR:bad format";
        String voter = p[0].trim();
        String seat  = p[1].trim().toUpperCase();
        String cand  = p[2].trim();

        try {
            /* current Started election */
            int eid;
            try (Statement st=c.createStatement();
                 ResultSet rs=st.executeQuery(
                     "SELECT ID FROM Election_Time WHERE Status='Started' ORDER BY ID DESC LIMIT 1")) {
                if (!rs.next()) return "ERROR:No active election";
                eid = rs.getInt(1);
            }

            /* candidate belongs to that seat? */
            try (PreparedStatement ps=c.prepareStatement(
                    "SELECT 1 FROM Candidates WHERE CNIC=? AND ConstitutionSeat=?")) {
                ps.setString(1,cand); ps.setString(2,seat);
                if (!ps.executeQuery().next()) return "ERROR:Candidate not on seat";
            }

            /* insert */
            String ins="INSERT INTO Votes (ElectionID,VoterCNIC,Seat,CandidateCNIC) VALUES (?,?,?,?)";
            try (PreparedStatement ps=c.prepareStatement(ins)) {
                ps.setInt(1,eid); ps.setString(2,voter);
                ps.setString(3,seat); ps.setString(4,cand);
                ps.executeUpdate();
                return "OK:Vote cast";
            } catch (SQLException ex) {
                return ex.getMessage().contains("uniq_vote")
                       ? "ERROR:Already voted" : "ERROR:"+ex.getMessage();
            }
        } catch (SQLException e) { return "ERROR:"+e.getMessage(); }
    }

    /* results for one seat of finished elections
       return: candCNIC,candName,party,votes ; … */
    private static String getSeatResults(Connection c,String seatRaw){
        String seat = seatRaw.trim().toUpperCase();
        String sql = """
            SELECT v.CandidateCNIC,c.CandidateName,c.PartyName,COUNT(*) AS Votes
              FROM Votes v
              JOIN Election_Time e ON e.ID=v.ElectionID
              JOIN Candidates    c ON c.CNIC=v.CandidateCNIC
             WHERE v.Seat=? AND e.Status='Finished'
             GROUP BY v.CandidateCNIC,c.CandidateName,c.PartyName
             ORDER BY Votes DESC
            """;
        try (PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,seat);
            ResultSet rs=ps.executeQuery();
            StringBuilder sb=new StringBuilder();
            while(rs.next()){
                if(sb.length()>0) sb.append(';');
                sb.append(rs.getString(1)).append(',')
                  .append(rs.getString(2)).append(',')
                  .append(rs.getString(3)).append(',')
                  .append(rs.getInt(4));
            }
            return sb.length()==0 ? "NO_RESULTS" : sb.toString();
        }catch(SQLException e){ return "ERROR:"+e.getMessage();}
    }
    
    /** --------------------------------------------------------------
 *  getPartyResults  – case‑insensitive vote totals for one party
 *  data  = party name as typed by the client
 *  reply = seat,votes ; seat,votes ; …  |  NO_RESULTS  |  ERROR:…
 * -------------------------------------------------------------- */
private static String getPartyResults(Connection c, String partyRaw) {
    String sql = """
        SELECT v.Seat, COUNT(*) AS Votes
          FROM Votes v
          JOIN Election_Time e ON e.ID = v.ElectionID
          JOIN Candidates    c ON c.CNIC = v.CandidateCNIC
         WHERE e.Status = 'Finished'
           AND LOWER(c.PartyName) = LOWER(?)      -- ← case‑insensitive
         GROUP BY v.Seat
         ORDER BY v.Seat
        """;
    try (PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, partyRaw.trim());
        ResultSet rs = ps.executeQuery();

        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(rs.getString("Seat")).append(',')
              .append(rs.getInt("Votes"));
        }
        return sb.length() == 0 ? "NO_RESULTS" : sb.toString();

    } catch (SQLException e) { return "ERROR:" + e.getMessage(); }
}

    /* ===================================================== */
    /*  COMMON helpers                                       */
    /* ===================================================== */

    private static String checkCNIC(Connection c,String cnic){
        try (PreparedStatement ps=c.prepareStatement("SELECT 1 FROM Users WHERE CNIC=?")) {
            ps.setString(1,cnic);
            return ps.executeQuery().next()?"DUPLICATE":"OK";
        } catch(SQLException e){ return "ERROR:"+e.getMessage(); }
    }

    private static String getConstituency(Connection c,String code){
        String sql = """
            SELECT Province,Division,District,City,NAConstituency,ProvincialConstituency
            FROM Constituencies WHERE CityCode=?
            """;
        try (PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,code);
            ResultSet rs=ps.executeQuery();
            if(!rs.next()) return "NOT_FOUND";
            return String.join(",",
                    rs.getString(1),rs.getString(2),rs.getString(3),
                    rs.getString(4),rs.getString(5),rs.getString(6));
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }
    }

    /* ===============  USERS (signup / login / pw)  =============== */

    private static String signupUser(Connection c,String csv){
        String[] f=csv.split(",",-1);
        if(f.length!=11) return "ERROR:bad field count";
        String sql="""
            INSERT INTO Users (Name,CNIC,PhoneNo,Password,Province,Division,
                               District,City,NAConstituency,ProvincialConstituency,Gender,Status)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,'Voter')
            """;
        try(PreparedStatement ps=c.prepareStatement(sql)){
            for(int i=0;i<11;i++) ps.setString(i+1,f[i]);
            ps.executeUpdate();
            return "SUCCESS";
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }
    }

    private static String login(Connection c,String csv){
        String[] p=csv.split(",",2);
        if(p.length!=2) return "ERROR:bad format";
        String sql="SELECT Status FROM Users WHERE CNIC=? AND Password=?";
        try(PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,p[0]); ps.setString(2,p[1]);
            ResultSet rs=ps.executeQuery();
            return rs.next()?rs.getString(1):"INVALID";
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }
    }

    private static String updatePassword(Connection c,String csv){
        String[] p=csv.split(",",2);
        if(p.length!=2) return "ERROR:bad format";
        String sql="UPDATE Users SET Password=? WHERE CNIC=?";
        try(PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,p[1]); ps.setString(2,p[0]);
            return ps.executeUpdate()==1?"SUCCESS":"ERROR:update failed";
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }
    }

    /* ===============  CANDIDATES  =============== */

    private static String getUserName(Connection c,String cnic){
        try(PreparedStatement ps=c.prepareStatement("SELECT Name FROM Users WHERE CNIC=?")){
            ps.setString(1,cnic);
            ResultSet rs=ps.executeQuery();
            return rs.next()?rs.getString(1):"ERROR:CNIC not found";
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }
    }

    private static String getParties(Connection c){
        StringBuilder sb=new StringBuilder();
        try(Statement st=c.createStatement();
            ResultSet rs=st.executeQuery("SELECT PartyName,Symbol FROM Party")){
            while(rs.next()){
                if(sb.length()>0) sb.append(';');
                sb.append(rs.getString(1)).append('|').append(rs.getString(2));
            }
            return sb.toString();
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }
    }

    private static String checkCandidate(Connection c,String csv){
        String[] p=csv.split(",",3);
        if(p.length!=3) return "ERROR:bad format";
        String sql="SELECT 1 FROM Candidates WHERE ConstitutionType=? AND ConstitutionSeat=? AND PartyName=?";
        try(PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,p[0]); ps.setString(2,p[1]); ps.setString(3,p[2]);
            return ps.executeQuery().next()?"EXISTS":"OK";
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }
    }

    private static String registerCandidate(Connection c,String csv){
        String[] f=csv.split(",",-1);
        if(f.length!=6) return "ERROR:bad field count";
        String sql="INSERT INTO Candidates (CNIC,CandidateName,ConstitutionType,ConstitutionSeat,PartyName,Symbol) VALUES (?,?,?,?,?,?)";
        try(PreparedStatement ps=c.prepareStatement(sql)){
            for(int i=0;i<6;i++) ps.setString(i+1,f[i]);
            ps.executeUpdate();
            return "SUCCESS";
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }
    }

    private static String getAllCandidates(Connection c){
        StringBuilder sb=new StringBuilder();
        String sql="SELECT CNIC,CandidateName,ConstitutionType,ConstitutionSeat,PartyName,Symbol FROM Candidates ORDER BY ConstitutionSeat";
        try(Statement st=c.createStatement(); ResultSet rs=st.executeQuery(sql)){
            while(rs.next()){
                if(sb.length()>0) sb.append(';');
                sb.append(rs.getString(1)).append(',').append(rs.getString(2)).append(',')
                  .append(rs.getString(3)).append(',').append(rs.getString(4)).append(',')
                  .append(rs.getString(5)).append(',').append(rs.getString(6));
            }
            return sb.toString();
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }
    }

    private static String getCandidatesBySeat(Connection c,String seatRaw){
        String seat=seatRaw.trim().toUpperCase();
        if(!seat.matches("^(NA|PP)-\\d+$")) return "ERROR:bad seat format";

        String checkSql = seat.startsWith("NA")?
              "SELECT 1 FROM Constituencies WHERE NAConstituency=?":
              "SELECT 1 FROM Constituencies WHERE ProvincialConstituency=?";
        try(PreparedStatement chk=c.prepareStatement(checkSql)){
            chk.setString(1,seat);
            if(!chk.executeQuery().next()) return "NO_SEAT";
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }

        StringBuilder sb=new StringBuilder();
        String sql="SELECT CNIC,CandidateName,ConstitutionType,ConstitutionSeat,PartyName,Symbol FROM Candidates WHERE ConstitutionSeat=?";
        try(PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,seat);
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                if(sb.length()>0) sb.append(';');
                sb.append(rs.getString(1)).append(',').append(rs.getString(2)).append(',')
                  .append(rs.getString(3)).append(',').append(rs.getString(4)).append(',')
                  .append(rs.getString(5)).append(',').append(rs.getString(6));
            }
            return sb.length()==0?"NO_CANDIDATE":sb.toString();
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }
    }

    /* ===============  PARTIES  =============== */

private static String getAllParties(Connection c) {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT PartyName,Symbol,LeaderCNIC FROM Party ORDER BY PartyName";
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                if (sb.length() > 0) sb.append(';');
                sb.append(rs.getString(1)).append(',')
                  .append(rs.getString(2)).append(',')
                  .append(rs.getString(3));
            }
            return sb.toString();
        } catch (SQLException e) { return "ERROR:" + e.getMessage(); }
    }

    private static String searchPartyByName(Connection c, String txt) {

        String key = "%" + txt.trim().toLowerCase() + "%";
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT PartyName,Symbol,LeaderCNIC FROM Party "
                   + "WHERE LOWER(PartyName) LIKE ? ORDER BY PartyName";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (sb.length() > 0) sb.append(';');
                sb.append(rs.getString(1)).append(',')
                  .append(rs.getString(2)).append(',')
                  .append(rs.getString(3));
            }
            return sb.length() == 0 ? "NO_MATCH" : sb.toString();
        } catch (SQLException e) { return "ERROR:" + e.getMessage(); }
    }


    /* ===============  VOTERS (NEW)  =============== */

/* ===============  VOTERS (updated)  =============== */

/** full voter dump without the Admin record */
private static String getAllVoters(Connection c){
    StringBuilder sb = new StringBuilder();
    String sql = """
        SELECT CNIC,Name,PhoneNo,City,Status
        FROM Users
        WHERE Status <> 'Admin'                 --  🔹 filter Admin out
        ORDER BY Name
        """;
    try (Statement st = c.createStatement();
         ResultSet rs = st.executeQuery(sql)) {

        while (rs.next()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(rs.getString("CNIC")).append(',')
              .append(rs.getString("Name")).append(',')
              .append(rs.getString("PhoneNo")).append(',')
              .append(rs.getString("City")).append(',')
              .append(rs.getString("Status"));
        }
        return sb.toString();
    } catch (SQLException e) { return "ERROR:" + e.getMessage(); }
}

/** exact search by CNIC — also ignores the Admin row */
/** exact search by CNIC — ignores Admin row
    returns: CNIC,Name,Phone,City,Status,NASeat,PPSeat           */
private static String searchVoterByCnic(Connection c, String cnic){
    String sql = """
        SELECT CNIC,Name,PhoneNo,City,Status,
               NAConstituency,ProvincialConstituency      -- 🔹 new fields
        FROM   Users
        WHERE  CNIC = ? AND Status <> 'Admin'
        """;
    try (PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, cnic);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return "NO_MATCH";

        return rs.getString("CNIC")     + ',' +
               rs.getString("Name")     + ',' +
               rs.getString("PhoneNo")  + ',' +
               rs.getString("City")     + ',' +
               rs.getString("Status")   + ',' +
               rs.getString("NAConstituency")            + ',' +   // 🔹
               rs.getString("ProvincialConstituency");               // 🔹
    } catch (SQLException e) { return "ERROR:" + e.getMessage(); }
}



    /* ===============  ADMIN inserts  =============== */

    private static String addConstituency(Connection c,String csv){
        String[] f=csv.split(",",-1);
        if(f.length!=7) return "ERROR:bad field count";
        try(PreparedStatement dup=c.prepareStatement("SELECT 1 FROM Constituencies WHERE CityCode=?")){
            dup.setString(1,f[0]);
            if(dup.executeQuery().next()) return "ERROR:City‑code exists";
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }

        String sql="INSERT INTO Constituencies (CityCode,Province,Division,District,City,NAConstituency,ProvincialConstituency) VALUES (?,?,?,?,?,?,?)";
        try(PreparedStatement ps=c.prepareStatement(sql)){
            for(int i=0;i<7;i++) ps.setString(i+1,f[i]);
            ps.executeUpdate();
            return "SUCCESS";
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }
    }
    
    // 🔹 New method to register election time (after registerParty)

    private static String registerParty(Connection c,String csv){
        String[] f=csv.split(",",-1);
        if(f.length!=3) return "ERROR:bad field count";

        try(PreparedStatement dup=c.prepareStatement(
            "SELECT 1 FROM Party WHERE LOWER(PartyName)=? OR LOWER(Symbol)=? OR LeaderCNIC=?")){
            dup.setString(1,f[0].toLowerCase());
            dup.setString(2,f[1].toLowerCase());
            dup.setString(3,f[2]);
            if(dup.executeQuery().next()) return "ERROR:duplicate";
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }

        String sql="INSERT INTO Party (PartyName,Symbol,LeaderCNIC) VALUES (?,?,?)";
        try(PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,f[0]); ps.setString(2,f[1]); ps.setString(3,f[2]);
            ps.executeUpdate();
            return "SUCCESS";
        }catch(SQLException e){ return "ERROR:"+e.getMessage(); }
    }
}
