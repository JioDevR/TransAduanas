package pe.sga.support;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/** Imprime metadatos y resultado real de JUnit, sin sustituir las aserciones. */
public class EvidenciaDocenteExtension implements BeforeTestExecutionCallback, TestWatcher {
    private void escribir(ExtensionContext context, String etapa, String texto) {
        PruebaDocente caso = context.getRequiredTestMethod().getAnnotation(PruebaDocente.class);
        System.out.printf("[%s] %s | %s%n", caso.codigo(), etapa, texto);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        PruebaDocente caso = context.getRequiredTestMethod().getAnnotation(PruebaDocente.class);
        escribir(context, "TIPO", caso.tipo());
        escribir(context, "HERRAMIENTAS", caso.herramientas());
        escribir(context, "OBJETIVO", caso.objetivo());
        escribir(context, "METODO", context.getRequiredTestClass().getSimpleName()
                + "#" + context.getRequiredTestMethod().getName());
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        escribir(context, "RESULTADO", "APROBADO: todas las aserciones del metodo terminaron correctamente.");
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        escribir(context, "RESULTADO", "NO APROBADO: " + cause.getClass().getSimpleName()
                + ": " + cause.getMessage());
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        escribir(context, "RESULTADO", "INTERRUMPIDO: " + cause.getMessage());
    }
}
