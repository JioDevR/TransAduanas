import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class RotationProbe {
    public static void main(String[] args) throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(args[0]);
        int count = Integer.parseInt(args[2]);
        Logger logger = LoggerFactory.getLogger("pe.sga.monitoring.RotationProbe");
        MDC.put("requestId", "03030303030303030303030303030303");
        try {
            String payload = "X".repeat(384);
            for (int i = 1; i <= count; i++) {
                logger.info("monitor_test={} seq={} payload={}", args[1], i, payload);
                if (i % 25 == 0) Thread.sleep(10);
            }
        } finally {
            MDC.clear();
            context.stop();
        }
        long errors = context.getStatusManager().getCopyOfStatusList().stream()
                .filter(s -> s.getLevel() == Status.ERROR).count();
        System.out.println("Eventos emitidos=" + count + "; errores de Logback=" + errors);
        if (errors > 0) {
            context.getStatusManager().getCopyOfStatusList().stream()
                    .filter(s -> s.getLevel() == Status.ERROR)
                    .forEach(s -> System.err.println(s.toString()));
            System.exit(2);
        }
    }
}
