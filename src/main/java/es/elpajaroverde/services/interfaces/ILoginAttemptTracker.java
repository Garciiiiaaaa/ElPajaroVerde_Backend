package es.elpajaroverde.services.interfaces;

public interface ILoginAttemptTracker {

    void registerFailedAttempt(String nombreUsuario);

    boolean isBlocked(String nombreUsuario);

    void clearAttempts(String nombreUsuario);

    boolean canLogin(String nombreUsuario);
}
