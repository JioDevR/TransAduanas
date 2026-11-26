package pe.sga.security;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class TemporaryCredentialsTest {
    @Test
    void generaCredencialesDistintasCon32BytesAleatorios() {
        var generated = new HashSet<String>();
        for (int i = 0; i < 1000; i++) {
            String credential = TemporaryCredentials.generate();
            assertEquals(32, Base64.getUrlDecoder().decode(credential).length);
            assertTrue(generated.add(credential), "No debe repetirse una credencial");
        }
    }
}
