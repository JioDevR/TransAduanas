import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class RotationMaintenanceProbe {
    public static void main(String[] args) throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(args[0]);
        Logger logger = LoggerFactory.getLogger("pe.sga.maintenance.PM06");
        int count = Integer.parseInt(args[2]);
        MDC.put("requestId", args[1]);
        try {
            String payload = "X".repeat(384);
            for (int i=1; i<=count; i++) {
                if (i % 2 == 0) {
                    logger.warn("maintenance_test={} seq={} kind=WARN_CONTROL_SIN_FALLO payload={}",args[1],i,payload);
                } else {
                    logger.info("maintenance_test={} seq={} kind=INFO_CONTROL payload={}",args[1],i,payload);
                }
                if (i % 25 == 0) Thread.sleep(10);
            }
        } finally {
            MDC.clear();
            context.stop();
        }
        long errors=context.getStatusManager().getCopyOfStatusList().stream().filter(s->s.getLevel()==Status.ERROR).count();
        System.out.println("Eventos="+count+"; errores_internos_Logback="+errors);
        if (errors>0) {
            context.getStatusManager().getCopyOfStatusList().stream().filter(s->s.getLevel()==Status.ERROR).forEach(System.err::println);
            System.exit(2);
        }
    }
}
