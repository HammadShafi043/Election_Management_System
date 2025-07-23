 -- Create the EMS database
CREATE DATABASE IF NOT EXISTS EMS;
USE EMS;

-- Create the Users table with all necessary fields
CREATE TABLE IF NOT EXISTS Users (
    UserID INT PRIMARY KEY AUTO_INCREMENT,
    Name VARCHAR(100) NOT NULL,
    CNIC VARCHAR(15) NOT NULL UNIQUE,
    PhoneNo VARCHAR(15) NOT NULL,
    Password CHAR(64) NOT NULL,
    Province VARCHAR(50) NOT NULL,
    Division VARCHAR(50) NOT NULL,
    District VARCHAR(50) NOT NULL,
    City VARCHAR(50) NOT NULL,
    NAConstituency VARCHAR(50) NOT NULL,
    ProvincialConstituency VARCHAR(50) NOT NULL,
    Gender ENUM('Male', 'Female') NOT NULL,
    Status ENUM('Voter') NOT NULL DEFAULT 'Voter',
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS Constituencies (
    CityCode VARCHAR(10) PRIMARY KEY,
    Province VARCHAR(100),
    Division VARCHAR(100),
    District VARCHAR(100),
    City VARCHAR(100),
    NAConstituency VARCHAR(20),
    ProvincialConstituency VARCHAR(20)
);


CREATE TABLE IF NOT EXISTS Party (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    PartyName VARCHAR(100),
    Symbol VARCHAR(100),
    LeaderCNIC VARCHAR(15),
    UNIQUE (PartyName),
    UNIQUE (Symbol)
);

CREATE TABLE IF NOT EXISTS Candidates (
    ID                 INT AUTO_INCREMENT PRIMARY KEY,
    CNIC               VARCHAR(13)  NOT NULL,
    CandidateName      VARCHAR(100) NOT NULL,
    ConstitutionType   VARCHAR(2)   NOT NULL,   -- 'NA' or 'PP'
    ConstitutionSeat   VARCHAR(10)  NOT NULL,   -- e.g. NA‑101 / PP‑102
    PartyName          VARCHAR(100) NOT NULL,
    Symbol             VARCHAR(50)  NOT NULL,

    -- one candidate per Party per Seat:
    UNIQUE KEY uniq_party_seat (ConstitutionType, ConstitutionSeat, PartyName),

    -- (optional but common) reference back to Party table
    FOREIGN KEY (PartyName) REFERENCES Party (PartyName),

    -- (optional) reference back to Users table
    FOREIGN KEY (CNIC)      REFERENCES Users (CNIC)
);

CREATE TABLE IF NOT Exists Election_Time (
    ID           INT AUTO_INCREMENT PRIMARY KEY,
    StartTime    DATETIME      NOT NULL,
    StopTime     DATETIME      NOT NULL,
    RegisteredBy VARCHAR(100)  NOT NULL,
    Status ENUM('Scheduled','Started','Finished')
           NOT NULL
           DEFAULT 'Scheduled',
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_start_date (StartTime)
);


/* === Votes table =========================================== */
CREATE TABLE IF NOT EXISTS Votes (
    ID            INT AUTO_INCREMENT PRIMARY KEY,
    ElectionID    INT         NOT NULL,
    VoterCNIC     VARCHAR(15) NOT NULL,
    Seat          VARCHAR(10) NOT NULL,
    CandidateCNIC VARCHAR(13) NOT NULL,
    CastAt        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_vote (ElectionID, VoterCNIC, Seat),

    FOREIGN KEY (ElectionID)    REFERENCES Election_Time(ID) ON DELETE CASCADE,
    FOREIGN KEY (VoterCNIC)     REFERENCES Users(CNIC),
    FOREIGN KEY (CandidateCNIC) REFERENCES Candidates(CNIC)
);

/* === helper index (version‑neutral) ======================== */
-- see block above
SET @ix :=
  (SELECT COUNT(*)
     FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'Votes'
      AND INDEX_NAME   = 'idx_votes_seat');
SET @sql :=
  IF(@ix = 0,
     'ALTER TABLE Votes ADD INDEX idx_votes_seat (Seat)',
     'SELECT \"idx_votes_seat already exists\"');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

/* === SeatResults view ====================================== */
DROP VIEW IF EXISTS SeatResults;
CREATE VIEW SeatResults AS
SELECT e.ID            AS ElectionID,
       e.StartTime,
       v.Seat,
       v.CandidateCNIC,
       c.CandidateName,
       c.PartyName,
       COUNT(*)         AS Votes
FROM   Votes v
JOIN   Election_Time e  ON e.ID = v.ElectionID
JOIN   Candidates    c  ON c.CNIC = v.CandidateCNIC
WHERE  e.Status = 'Finished'
GROUP  BY e.ID, v.Seat, v.CandidateCNIC
ORDER  BY e.ID, v.Seat, Votes DESC;













-- ALTER TABLE Users MODIFY COLUMN Status VARCHAR(20);

-- Lahore District (Lahore Division)
-- INSERT INTO Constituencies VALUES ('35202', 'Punjab', 'Lahore Division', 'Lahore District', 'Lahore', 'NA-101', 'PP-101');
-- INSERT INTO Constituencies VALUES ('35201', 'Punjab', 'Lahore Division', 'Lahore District', 'Kot Lakhpat', 'NA-102', 'PP-102');
-- INSERT INTO Constituencies VALUES ('35203', 'Punjab', 'Lahore Division', 'Lahore District', 'Raiwind', 'NA-103', 'PP-103');
-- INSERT INTO Constituencies VALUES ('35204', 'Punjab', 'Lahore Division', 'Lahore District', 'Shalimar', 'NA-104', 'PP-104');
-- INSERT INTO Constituencies VALUES ('35205', 'Punjab', 'Lahore Division', 'Lahore District', 'Model Town', 'NA-105', 'PP-105');

-- Kasur District (Lahore Division)
-- INSERT INTO Constituencies VALUES ('35101', 'Punjab', 'Lahore Division', 'Kasur District', 'Kasur', 'NA-106', 'PP-106');
-- INSERT INTO Constituencies VALUES ('35102', 'Punjab', 'Lahore Division', 'Kasur District', 'Pattoki', 'NA-107', 'PP-107');
-- INSERT INTO Constituencies VALUES ('35103', 'Punjab', 'Lahore Division', 'Kasur District', 'Chunian', 'NA-108', 'PP-108');
-- INSERT INTO Constituencies VALUES ('35104', 'Punjab', 'Lahore Division', 'Kasur District', 'Kot Radha Kishan', 'NA-109', 'PP-109');
-- INSERT INTO Constituencies VALUES ('35105', 'Punjab', 'Lahore Division', 'Kasur District', 'Mustafabad', 'NA-110', 'PP-110');

-- Sialkot District (Gujranwala Division)
-- INSERT INTO Constituencies VALUES ('34101', 'Punjab', 'Gujranwala Division', 'Sialkot District', 'Sialkot', 'NA-111', 'PP-111');
-- INSERT INTO Constituencies VALUES ('34102', 'Punjab', 'Gujranwala Division', 'Sialkot District', 'Daska', 'NA-112', 'PP-112');
-- INSERT INTO Constituencies VALUES ('34103', 'Punjab', 'Gujranwala Division', 'Sialkot District', 'Sambrial', 'NA-113', 'PP-113');
-- INSERT INTO Constituencies VALUES ('34104', 'Punjab', 'Gujranwala Division', 'Sialkot District', 'Pasrur', 'NA-114', 'PP-114');
-- INSERT INTO Constituencies VALUES ('34105', 'Punjab', 'Gujranwala Division', 'Sialkot District', 'Uggoki', 'NA-115', 'PP-115');


-- INSERT INTO Users (Name, CNIC, PhoneNo, Password, Province, Division, District, City, NAConstituency, ProvincialConstituency, Gender, Status)
-- VALUES ('Admin', '0000000000000', '03000000000', SHA2('admin123456789', 256), 'Punjab', 'Admin Division', 'Admin District', 'Admin City', 'NA-000', 'PP-000', 'Male', 'Admin');


-- select * from Users;
-- select * from Party;
--  select * from Constituencies ;
--  select * from candidates;
-- select * from Election_Time;


-- SHOW EVENTS FROM EMS;          -- you should see start_* and finish_* events
-- SELECT * FROM Election_Time;   -- watch Status flip Scheduled → Started → Finished




-- SET GLOBAL event_scheduler = ON;
-- GRANT EVENT ON EMS.* TO 'root'@'localhost';
-- truncate table votes;
-- select * from votes;
--  select * from Users;
-- SELECT COUNT(*) FROM Candidate-- ;
SHOW TABLES;


