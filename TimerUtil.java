// TimerUtil.java
package election_management_system;

import javax.swing.*;
import java.util.Date;
import java.util.TimerTask;

public class TimerUtil {
    private javax.swing.Timer swingTimer;

    public TimerUtil(JLabel label) {
        swingTimer = new javax.swing.Timer(1000, e -> {
            long remaining = stopTime.getTime() - System.currentTimeMillis();
            if (remaining <= 0) {
                label.setText("⏹️ Voting time is over!");
                swingTimer.stop();
            } else {
                long seconds = (remaining / 1000) % 60;
                long minutes = (remaining / (1000 * 60)) % 60;
                long hours   = (remaining / (1000 * 60 * 60));
                label.setText(String.format("⏳ Voting ends in %02d:%02d:%02d", hours, minutes, seconds));
            }
        });
    }

    private Date stopTime;

    public void startCountdown(Date stopTime) {
        this.stopTime = stopTime;
        swingTimer.start();
    }
}
