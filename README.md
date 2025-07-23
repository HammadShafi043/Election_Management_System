## 🗳️ Election Management System  
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)  
![MySQL](https://img.shields.io/badge/MySQL-00758F?style=for-the-badge&logo=mysql&logoColor=white)  
![Swing](https://img.shields.io/badge/Swing-UI-blue?style=for-the-badge)  
![Client–Server](https://img.shields.io/badge/Client--Server-Architecture-green?style=for-the-badge)  

A GUI-based client-server **Election Management System** developed in Java using `JFrame` for the frontend and `MySQL` for backend data storage. The system allows voters and candidates to register, login, and participate in elections, with secure voting and result display. Admins can manage users, monitor results, and handle system operations.

---

### 📌 Project Scope
The Election Management System aims to streamline the process of conducting digital elections through a secure, user-friendly, and scalable platform. The scope of this project includes:

User Registration & Authentication: Secure registration and login for voters, candidates, and administrators.

Role-Based Access:

Voters can register, log in, and cast their vote (only once).

Candidates can register and appear in voting lists.

Admins can view statistics, manage users, and oversee the entire voting process.

Vote Casting Module: Each authenticated voter can vote for their preferred candidate. Duplicate voting is prevented.

Real-Time Results: Automatically updated vote counts with result display.

Client-Server Communication: All operations are handled through a TCP socket-based client-server model, enhancing separation of concerns.

Database Integration: Persistent storage using MySQL ensures reliability and future scalability.

Network Flexibility: Supports remote and local server deployment with automatic fallback detection.

GUI Frontend: Built with Java Swing for a smooth and interactive user experience.

This project is designed for academic use, small-scale election simulations, or as a foundation for larger, secure e-voting systems.


### 📌 Features

- 🔐 **Role-based authentication** (Admin, Voter, Candidate)  
- 🗳️ **Vote casting** with validation and one-person-one-vote logic  
- 📊 **Real-time result calculation**  
- 🧾 **Voter & candidate registration** via GUI  
- 🧑‍💼 **Admin panel** for managing users, resetting votes, and monitoring activity  
- 🌐 **Server reachability check** (remote → local fallback)  
- 🎨 **Modern UI** using Nimbus Look and Feel  
- 🛠️ Built with standard **Java + JDBC + MySQL**

---

### 🖥️ System Requirements

- Java JDK 24 or later  
- MySQL Server installed and running  
- MySQL Connector/J (JDBC driver)  
- NetBeans or IntelliJ IDEA (optional, for editing)

---

### ⚙️ Server Setup

1. **Create the MySQL database**:
   ```sql
   CREATE DATABASE election_db;
   USE election_db;
   ```

2. **Create required tables** (example):
   ```sql
   CREATE TABLE users (
     id INT AUTO_INCREMENT PRIMARY KEY,
     name VARCHAR(100),
     cnic VARCHAR(20) UNIQUE,
     role ENUM('admin', 'voter', 'candidate'),
     password VARCHAR(100)
   );

   CREATE TABLE votes (
     id INT AUTO_INCREMENT PRIMARY KEY,
     voter_cnic VARCHAR(20),
     candidate_id INT,
     timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
   );
   ```

3. **Add an admin user**:
   ```sql
   INSERT INTO users (name, cnic, role, password) 
   VALUES ('Admin', '11111-1111111-1', 'admin', 'admin123');
   ```

4. **Start the Server**  
   You must run the Java server component (`Server.java`) first. It listens on port `12346`.

---

### 🚀 How to Run

1. Clone or download this repo  
2. Open in your IDE  
3. Configure DB connection (JDBC URL, username, password)  
4. Start the server → `Server.java`  
5. Run the client → `Election_Management_System.java`  
6. App will attempt to connect to server at `your server ip`, fallback to localhost if not reachable  
7. GUI will open (`SignUpForm`) for user login/registration

---

### 🗂️ Project Structure

```
├── election_management_system/
│   ├── Election_Management_System.java   # Main client launcher
│   ├── SignUpForm.java                   # GUI form for registration
│   ├── Server.java                       # Server-side code (TCP listener)
│   └── AdminPanel.java                   # Admin management panel
```

---

