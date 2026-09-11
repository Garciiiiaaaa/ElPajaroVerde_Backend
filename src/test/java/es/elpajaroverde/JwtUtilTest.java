package es.elpajaroverde;

import es.elpajaroverde.security.JwtUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil("", 8);

    @Test
    void generateToken_and_extractUsername_returnsCorrectUser() {
        String token = jwtUtil.generateToken("admin1");
        String username = jwtUtil.extractUsername(token);
        assertEquals("admin1", username);
    }

    @Test
    void validateToken_withValidToken_returnsTrue() {
        String token = jwtUtil.generateToken("admin1");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() {
        JwtUtil shortLivedJwt = new JwtUtil("", 0);
        String token = shortLivedJwt.generateToken("admin1");
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        assertFalse(shortLivedJwt.validateToken(token));
    }

    @Test
    void validateToken_withTamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("admin1");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    void rotateKey_invalidatesOldTokens() {
        String token = jwtUtil.generateToken("admin1");
        assertTrue(jwtUtil.validateToken(token));
        jwtUtil.rotateKey();
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void rotateKey_newTokensAreValid() {
        jwtUtil.generateToken("admin1");
        jwtUtil.rotateKey();
        String newToken = jwtUtil.generateToken("admin1");
        assertTrue(jwtUtil.validateToken(newToken));
    }

    @Test
    void isTokenExpired_withValidToken_returnsFalse() {
        String token = jwtUtil.generateToken("admin1");
        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void isTokenExpired_withExpiredToken_returnsTrue() {
        JwtUtil shortLivedJwt = new JwtUtil("", 0);
        String token = shortLivedJwt.generateToken("admin1");
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        assertTrue(shortLivedJwt.isTokenExpired(token));
    }
}
