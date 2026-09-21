package es.elpajaroverde.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import es.elpajaroverde.services.interfaces.ILoginAttemptTracker;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptTracker implements ILoginAttemptTracker {

    private final ConcurrentHashMap<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    private final int maxFailedAttempts;
    private final long lockoutMinutes;

    public LoginAttemptTracker(
            @Value("${app.auth.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${app.auth.lockout-minutes:5}") long lockoutMinutes) {
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutMinutes = lockoutMinutes;
    }

    @Override
    public void registerFailedAttempt(String nombreUsuario) {
        attempts.compute(nombreUsuario, (key, existing) -> {
            if (existing == null) {
                LoginAttempt attempt = new LoginAttempt();
                attempt.failedAttempts = 1;
                return attempt;
            }
            existing.failedAttempts++;
            if (existing.failedAttempts >= maxFailedAttempts) {
                existing.lockoutTime = LocalDateTime.now();
            }
            return existing;
        });
    }

    @Override
    public boolean isBlocked(String nombreUsuario) {
        LoginAttempt attempt = attempts.get(nombreUsuario);
        if (attempt == null || attempt.lockoutTime == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(attempt.lockoutTime.plusMinutes(lockoutMinutes));
    }

    @Override
    public boolean canLogin(String nombreUsuario) {
        LoginAttempt attempt = attempts.get(nombreUsuario);
        if (attempt == null) {
            return true;
        }
        if (attempt.lockoutTime == null) {
            return true;
        }
        if (LocalDateTime.now().isAfter(attempt.lockoutTime.plusMinutes(lockoutMinutes))) {
            attempts.remove(nombreUsuario);
            return true;
        }
        return false;
    }

    @Override
    public void clearAttempts(String nombreUsuario) {
        attempts.remove(nombreUsuario);
    }

    static class LoginAttempt {
        int failedAttempts;
        LocalDateTime lockoutTime;
    }
}
