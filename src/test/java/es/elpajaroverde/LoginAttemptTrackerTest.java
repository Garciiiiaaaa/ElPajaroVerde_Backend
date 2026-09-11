package es.elpajaroverde;

import es.elpajaroverde.services.LoginAttemptTracker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptTrackerTest {

    private final LoginAttemptTracker tracker = new LoginAttemptTracker(5, 5);

    @Test
    void fourFailures_notBlocked() {
        for (int i = 0; i < 4; i++) {
            tracker.registerFailedAttempt("admin");
        }
        assertFalse(tracker.isBlocked("admin"));
    }

    @Test
    void fiveFailures_blocked() {
        for (int i = 0; i < 5; i++) {
            tracker.registerFailedAttempt("admin");
        }
        assertTrue(tracker.isBlocked("admin"));
    }

    @Test
    void blocked_andMinutesNotPassed_cannotLogin() {
        for (int i = 0; i < 5; i++) {
            tracker.registerFailedAttempt("admin");
        }
        assertFalse(tracker.canLogin("admin"));
    }

    @Test
    void blocked_andMinutesPassed_canLogin() {
        LoginAttemptTracker shortTracker = new LoginAttemptTracker(5, 0);
        for (int i = 0; i < 5; i++) {
            shortTracker.registerFailedAttempt("admin");
        }
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        assertTrue(shortTracker.canLogin("admin"));
    }

    @Test
    void clearAttempts_removesAllData() {
        for (int i = 0; i < 3; i++) {
            tracker.registerFailedAttempt("admin");
        }
        tracker.clearAttempts("admin");
        assertFalse(tracker.isBlocked("admin"));
        assertTrue(tracker.canLogin("admin"));
    }

    @Test
    void differentUsers_areTrackedIndependently() {
        for (int i = 0; i < 5; i++) {
            tracker.registerFailedAttempt("admin1");
        }
        assertFalse(tracker.isBlocked("admin2"));
    }
}
