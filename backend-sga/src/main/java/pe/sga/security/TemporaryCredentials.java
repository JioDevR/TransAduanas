package pe.sga.security;

import java.security.SecureRandom;
import java.util.Base64;

/** Credenciales impredecibles que nunca se registran ni se incluyen en notificaciones. */
public final class TemporaryCredentials {
    private static final SecureRandom RANDOM = new SecureRandom();
    private TemporaryCredentials() { }

    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
