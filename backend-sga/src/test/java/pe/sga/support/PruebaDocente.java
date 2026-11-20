package pe.sga.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Tag("docente15")
@ExtendWith(EvidenciaDocenteExtension.class)
public @interface PruebaDocente {
    String codigo();
    String tipo();
    String herramientas();
    String objetivo();
}
