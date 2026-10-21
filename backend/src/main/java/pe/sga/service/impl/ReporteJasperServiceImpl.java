package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.reporte.PuertoReporteItemDTO;
import pe.sga.dto.reporte.SedeReporteItemDTO;
import pe.sga.dto.reporte.UsuarioReporteItemDTO;
import pe.sga.entity.CatalogoPuerto;
import pe.sga.entity.Sede;
import pe.sga.entity.Usuario;
import pe.sga.repository.CatalogoPuertoRepository;
import pe.sga.repository.SedeRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.IReporteJasperService;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteJasperServiceImpl implements IReporteJasperService {

    private final UsuarioRepository usuarioRepository;
    private final SedeRepository sedeRepository;
    private final CatalogoPuertoRepository puertoRepository;

    private static volatile JasperReport cachedUsuariosReport;
    private static volatile JasperReport cachedSedesReport;
    private static volatile JasperReport cachedPuertosReport;

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarUsuariosPdf() {
        List<Usuario> usuarios = usuarioRepository.findAll();

        List<UsuarioReporteItemDTO> items = usuarios.stream()
                .map(u -> UsuarioReporteItemDTO.builder()
                        .idUsuario(u.getIdUsuario())
                        .username(u.getUsername())
                        .nombreCompleto(u.getNombres() + " " + u.getApellidos())
                        .rol(u.getRol() != null ? u.getRol().getNombreRol().name() : "-")
                        .cargoTitulo(u.getCargoTitulo() != null && !u.getCargoTitulo().isBlank()
                                ? u.getCargoTitulo() : "Colaborador")
                        .sedeAgencia(u.getSede() != null ? u.getSede().getNombre() : "Sin Sede")
                        .estadoActivo(Boolean.TRUE.equals(u.getEstadoActivo()) ? "ACTIVO" : "INACTIVO")
                        .build())
                .toList();

        try {
            JasperReport report = getOrCompileUsuariosReport();

            Map<String, Object> params = new HashMap<>();
            params.put("TOTAL_USUARIOS", items.size());
            params.put("FECHA_GENERACION", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(items);
            JasperPrint print = JasperFillManager.fillReport(report, params, dataSource);

            byte[] pdfBytes = JasperExportManager.exportReportToPdf(print);
            return new ByteArrayInputStream(pdfBytes);
        } catch (JRException e) {
            log.error("Error al generar reporte de usuarios en JasperReports: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF de usuarios: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarSedesPdf() {
        List<Sede> sedes = sedeRepository.findAll();

        List<SedeReporteItemDTO> items = sedes.stream()
                .map(s -> SedeReporteItemDTO.builder()
                        .idSede(s.getIdSede())
                        .codigoSede(s.getCodigoSede())
                        .nombre(s.getNombre())
                        .tipo(s.getTipo())
                        .distrito(s.getDistrito())
                        .telefono(s.getTelefono() != null ? s.getTelefono() : "-")
                        .estado(s.getEstado())
                        .build())
                .toList();

        try {
            JasperReport report = getOrCompileSedesReport();

            Map<String, Object> params = new HashMap<>();
            params.put("TOTAL_SEDES", items.size());
            params.put("FECHA_GENERACION", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(items);
            JasperPrint print = JasperFillManager.fillReport(report, params, dataSource);

            byte[] pdfBytes = JasperExportManager.exportReportToPdf(print);
            return new ByteArrayInputStream(pdfBytes);
        } catch (JRException e) {
            log.error("Error al generar reporte de sedes en JasperReports: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF de sedes: " + e.getMessage(), e);
        }
    }

    private JasperReport getOrCompileUsuariosReport() throws JRException {
        if (cachedUsuariosReport == null) {
            synchronized (ReporteJasperServiceImpl.class) {
                if (cachedUsuariosReport == null) {
                    cachedUsuariosReport = JasperCompileManager.compileReport(buildUsuariosReportDesign());
                }
            }
        }
        return cachedUsuariosReport;
    }

    private JasperReport getOrCompileSedesReport() throws JRException {
        if (cachedSedesReport == null) {
            synchronized (ReporteJasperServiceImpl.class) {
                if (cachedSedesReport == null) {
                    cachedSedesReport = JasperCompileManager.compileReport(buildSedesReportDesign());
                }
            }
        }
        return cachedSedesReport;
    }

    private JasperDesign buildUsuariosReportDesign() throws JRException {
        JasperDesign design = new JasperDesign();
        design.setName("ReporteUsuariosSGA");
        design.setPageWidth(595);
        design.setPageHeight(842);
        design.setColumnWidth(555);
        design.setLeftMargin(20);
        design.setRightMargin(20);
        design.setTopMargin(20);
        design.setBottomMargin(20);

        JRDesignParameter paramTotal = new JRDesignParameter();
        paramTotal.setName("TOTAL_USUARIOS");
        paramTotal.setValueClass(Integer.class);
        design.addParameter(paramTotal);

        JRDesignParameter paramFecha = new JRDesignParameter();
        paramFecha.setName("FECHA_GENERACION");
        paramFecha.setValueClass(String.class);
        design.addParameter(paramFecha);

        String[] fieldNames = {"idUsuario", "username", "nombreCompleto", "rol", "cargoTitulo", "sedeAgencia", "estadoActivo"};
        Class<?>[] fieldTypes = {Integer.class, String.class, String.class, String.class, String.class, String.class, String.class};

        for (int i = 0; i < fieldNames.length; i++) {
            JRDesignField field = new JRDesignField();
            field.setName(fieldNames[i]);
            field.setValueClass(fieldTypes[i]);
            design.addField(field);
        }

        // Title Band
        JRDesignBand titleBand = new JRDesignBand();
        titleBand.setHeight(75);

        JRDesignStaticText title = new JRDesignStaticText();
        title.setText("SISTEMA SGA - AUDITORÍA Y SEGURIDAD TI");
        title.setX(0);
        title.setY(10);
        title.setWidth(555);
        title.setHeight(22);
        title.setBold(true);
        title.setFontSize(14f);
        title.setForecolor(new Color(24, 43, 73));
        title.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        titleBand.addElement(title);

        JRDesignStaticText subtitle = new JRDesignStaticText();
        subtitle.setText("Reporte Oficial de Usuarios y Cuentas de Acceso");
        subtitle.setX(0);
        subtitle.setY(33);
        subtitle.setWidth(555);
        subtitle.setHeight(16);
        subtitle.setFontSize(10f);
        subtitle.setForecolor(new Color(100, 116, 139));
        subtitle.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        titleBand.addElement(subtitle);

        JRDesignTextField fechaText = new JRDesignTextField();
        JRDesignExpression fechaExpr = new JRDesignExpression();
        fechaExpr.setText("\"Generado: \" + $P{FECHA_GENERACION} + \" | Total: \" + $P{TOTAL_USUARIOS}");
        fechaText.setExpression(fechaExpr);
        fechaText.setX(0);
        fechaText.setY(54);
        fechaText.setWidth(555);
        fechaText.setHeight(14);
        fechaText.setFontSize(8.5f);
        fechaText.setForecolor(new Color(100, 116, 139));
        fechaText.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        titleBand.addElement(fechaText);

        design.setTitle(titleBand);

        // Header Band
        JRDesignBand headerBand = new JRDesignBand();
        headerBand.setHeight(22);

        int[] colWidths = {35, 75, 120, 75, 105, 90, 55};
        String[] headers = {"ID", "USUARIO", "NOMBRES Y APELLIDOS", "ROL", "CARGO", "SEDE", "ESTADO"};

        int currentX = 0;
        for (int i = 0; i < headers.length; i++) {
            JRDesignStaticText header = new JRDesignStaticText();
            header.setText(headers[i]);
            header.setX(currentX);
            header.setY(0);
            header.setWidth(colWidths[i]);
            header.setHeight(20);
            header.setBold(true);
            header.setFontSize(8f);
            header.setMode(ModeEnum.OPAQUE);
            header.setBackcolor(new Color(24, 43, 73));
            header.setForecolor(Color.WHITE);
            header.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
            headerBand.addElement(header);
            currentX += colWidths[i];
        }
        design.setColumnHeader(headerBand);

        // Detail Band
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(18);

        currentX = 0;
        for (int i = 0; i < fieldNames.length; i++) {
            JRDesignTextField textField = new JRDesignTextField();
            JRDesignExpression expr = new JRDesignExpression();
            expr.setText("$F{" + fieldNames[i] + "}");
            textField.setExpression(expr);
            textField.setX(currentX);
            textField.setY(0);
            textField.setWidth(colWidths[i]);
            textField.setHeight(16);
            textField.setFontSize(8f);
            textField.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
            detailBand.addElement(textField);
            currentX += colWidths[i];
        }
        ((JRDesignSection) design.getDetailSection()).addBand(detailBand);

        // Footer Band
        JRDesignBand footerBand = new JRDesignBand();
        footerBand.setHeight(20);
        JRDesignTextField footerText = new JRDesignTextField();
        JRDesignExpression footerExpr = new JRDesignExpression();
        footerExpr.setText("\"SGA Platform TI Security - Página \" + $V{PAGE_NUMBER}");
        footerText.setExpression(footerExpr);
        footerText.setX(0);
        footerText.setY(4);
        footerText.setWidth(555);
        footerText.setHeight(14);
        footerText.setFontSize(8f);
        footerText.setForecolor(new Color(148, 163, 184));
        footerText.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        footerBand.addElement(footerText);
        design.setPageFooter(footerBand);

        return design;
    }

    private JasperDesign buildSedesReportDesign() throws JRException {
        JasperDesign design = new JasperDesign();
        design.setName("ReporteSedesSGA");
        design.setPageWidth(595);
        design.setPageHeight(842);
        design.setColumnWidth(555);
        design.setLeftMargin(20);
        design.setRightMargin(20);
        design.setTopMargin(20);
        design.setBottomMargin(20);

        JRDesignParameter paramTotal = new JRDesignParameter();
        paramTotal.setName("TOTAL_SEDES");
        paramTotal.setValueClass(Integer.class);
        design.addParameter(paramTotal);

        JRDesignParameter paramFecha = new JRDesignParameter();
        paramFecha.setName("FECHA_GENERACION");
        paramFecha.setValueClass(String.class);
        design.addParameter(paramFecha);

        String[] fieldNames = {"idSede", "codigoSede", "nombre", "tipo", "distrito", "telefono", "estado"};
        Class<?>[] fieldTypes = {Integer.class, String.class, String.class, String.class, String.class, String.class, String.class};

        for (int i = 0; i < fieldNames.length; i++) {
            JRDesignField field = new JRDesignField();
            field.setName(fieldNames[i]);
            field.setValueClass(fieldTypes[i]);
            design.addField(field);
        }

        // Title Band
        JRDesignBand titleBand = new JRDesignBand();
        titleBand.setHeight(75);

        JRDesignStaticText title = new JRDesignStaticText();
        title.setText("SISTEMA SGA - INFRAESTRUCTURA OPERATIVA");
        title.setX(0);
        title.setY(10);
        title.setWidth(555);
        title.setHeight(22);
        title.setBold(true);
        title.setFontSize(14f);
        title.setForecolor(new Color(24, 43, 73));
        title.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        titleBand.addElement(title);

        JRDesignStaticText subtitle = new JRDesignStaticText();
        subtitle.setText("Reporte Oficial de Sedes y Agencias");
        subtitle.setX(0);
        subtitle.setY(33);
        subtitle.setWidth(555);
        subtitle.setHeight(16);
        subtitle.setFontSize(10f);
        subtitle.setForecolor(new Color(100, 116, 139));
        subtitle.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        titleBand.addElement(subtitle);

        JRDesignTextField fechaText = new JRDesignTextField();
        JRDesignExpression fechaExpr = new JRDesignExpression();
        fechaExpr.setText("\"Generado: \" + $P{FECHA_GENERACION} + \" | Total Sedes: \" + $P{TOTAL_SEDES}");
        fechaText.setExpression(fechaExpr);
        fechaText.setX(0);
        fechaText.setY(54);
        fechaText.setWidth(555);
        fechaText.setHeight(14);
        fechaText.setFontSize(8.5f);
        fechaText.setForecolor(new Color(100, 116, 139));
        fechaText.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        titleBand.addElement(fechaText);

        design.setTitle(titleBand);

        // Header Band
        JRDesignBand headerBand = new JRDesignBand();
        headerBand.setHeight(22);

        int[] colWidths = {35, 80, 140, 80, 90, 70, 60};
        String[] headers = {"ID", "CÓDIGO", "NOMBRE DE SEDE", "TIPO", "DISTRITO", "TELÉFONO", "ESTADO"};

        int currentX = 0;
        for (int i = 0; i < headers.length; i++) {
            JRDesignStaticText header = new JRDesignStaticText();
            header.setText(headers[i]);
            header.setX(currentX);
            header.setY(0);
            header.setWidth(colWidths[i]);
            header.setHeight(20);
            header.setBold(true);
            header.setFontSize(8f);
            header.setMode(ModeEnum.OPAQUE);
            header.setBackcolor(new Color(24, 43, 73));
            header.setForecolor(Color.WHITE);
            header.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
            headerBand.addElement(header);
            currentX += colWidths[i];
        }
        design.setColumnHeader(headerBand);

        // Detail Band
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(18);

        currentX = 0;
        for (int i = 0; i < fieldNames.length; i++) {
            JRDesignTextField textField = new JRDesignTextField();
            JRDesignExpression expr = new JRDesignExpression();
            expr.setText("$F{" + fieldNames[i] + "}");
            textField.setExpression(expr);
            textField.setX(currentX);
            textField.setY(0);
            textField.setWidth(colWidths[i]);
            textField.setHeight(16);
            textField.setFontSize(8f);
            textField.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
            detailBand.addElement(textField);
            currentX += colWidths[i];
        }
        ((JRDesignSection) design.getDetailSection()).addBand(detailBand);

        // Footer Band
        JRDesignBand footerBand = new JRDesignBand();
        footerBand.setHeight(20);
        JRDesignTextField footerText = new JRDesignTextField();
        JRDesignExpression footerExpr = new JRDesignExpression();
        footerExpr.setText("\"SGA Platform Infraestructura - Página \" + $V{PAGE_NUMBER}");
        footerText.setExpression(footerExpr);
        footerText.setX(0);
        footerText.setY(4);
        footerText.setWidth(555);
        footerText.setHeight(14);
        footerText.setFontSize(8f);
        footerText.setForecolor(new Color(148, 163, 184));
        footerText.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        footerBand.addElement(footerText);
        design.setPageFooter(footerBand);

        return design;
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarPuertosPdf() {
        List<CatalogoPuerto> puertos = puertoRepository.findAll();

        List<PuertoReporteItemDTO> items = puertos.stream()
                .map(p -> PuertoReporteItemDTO.builder()
                        .idPuerto(p.getIdPuerto())
                        .codigoPuerto(p.getCodigoPuerto())
                        .nombre(p.getNombre())
                        .pais(p.getPais())
                        .tipo(p.getTipo())
                        .estado(p.getEstado())
                        .build())
                .toList();

        try {
            JasperReport report = getOrCompilePuertosReport();

            Map<String, Object> params = new HashMap<>();
            params.put("TOTAL_PUERTOS", items.size());
            params.put("FECHA_GENERACION", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(items);
            JasperPrint print = JasperFillManager.fillReport(report, params, dataSource);

            byte[] pdfBytes = JasperExportManager.exportReportToPdf(print);
            return new ByteArrayInputStream(pdfBytes);
        } catch (JRException e) {
            log.error("Error al generar reporte de puertos en JasperReports: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF de puertos: " + e.getMessage(), e);
        }
    }

    private JasperReport getOrCompilePuertosReport() throws JRException {
        if (cachedPuertosReport == null) {
            synchronized (ReporteJasperServiceImpl.class) {
                if (cachedPuertosReport == null) {
                    cachedPuertosReport = JasperCompileManager.compileReport(buildPuertosReportDesign());
                }
            }
        }
        return cachedPuertosReport;
    }

    private JasperDesign buildPuertosReportDesign() throws JRException {
        JasperDesign design = new JasperDesign();
        design.setName("ReportePuertosSGA");
        design.setPageWidth(595);
        design.setPageHeight(842);
        design.setColumnWidth(555);
        design.setLeftMargin(20);
        design.setRightMargin(20);
        design.setTopMargin(25);
        design.setBottomMargin(25);

        JRDesignParameter paramFecha = new JRDesignParameter();
        paramFecha.setName("FECHA_GENERACION");
        paramFecha.setValueClass(String.class);
        design.addParameter(paramFecha);

        JRDesignParameter paramTotal = new JRDesignParameter();
        paramTotal.setName("TOTAL_PUERTOS");
        paramTotal.setValueClass(Integer.class);
        design.addParameter(paramTotal);

        String[] fieldNames = {"idPuerto", "codigoPuerto", "nombre", "pais", "tipo", "estado"};
        Class<?>[] fieldTypes = {Integer.class, String.class, String.class, String.class, String.class, String.class};
        for (int i = 0; i < fieldNames.length; i++) {
            JRDesignField field = new JRDesignField();
            field.setName(fieldNames[i]);
            field.setValueClass(fieldTypes[i]);
            design.addField(field);
        }

        // Title Band
        JRDesignBand titleBand = new JRDesignBand();
        titleBand.setHeight(65);

        JRDesignStaticText title = new JRDesignStaticText();
        title.setText("SISTEMA DE GESTIÓN ADUANERA (SGA)");
        title.setX(0);
        title.setY(0);
        title.setWidth(555);
        title.setHeight(18);
        title.setBold(true);
        title.setFontSize(14f);
        title.setForecolor(new Color(24, 43, 73));
        title.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
        titleBand.addElement(title);

        JRDesignStaticText subtitle = new JRDesignStaticText();
        subtitle.setText("Catálogo Maestro de Puertos y Terminales Aduaneros");
        subtitle.setX(0);
        subtitle.setY(20);
        subtitle.setWidth(555);
        subtitle.setHeight(14);
        subtitle.setFontSize(10f);
        subtitle.setForecolor(new Color(100, 116, 139));
        subtitle.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
        titleBand.addElement(subtitle);

        JRDesignTextField fechaText = new JRDesignTextField();
        JRDesignExpression fechaExpr = new JRDesignExpression();
        fechaExpr.setText("\"Generado: \" + $P{FECHA_GENERACION} + \" | Total Puertos: \" + $P{TOTAL_PUERTOS}");
        fechaText.setExpression(fechaExpr);
        fechaText.setX(0);
        fechaText.setY(40);
        fechaText.setWidth(555);
        fechaText.setHeight(14);
        fechaText.setFontSize(8f);
        fechaText.setForecolor(new Color(71, 85, 105));
        fechaText.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        titleBand.addElement(fechaText);

        design.setTitle(titleBand);

        // Header Band
        JRDesignBand headerBand = new JRDesignBand();
        headerBand.setHeight(22);

        int[] colWidths = {40, 90, 200, 85, 75, 65};
        String[] headers = {"ID", "CÓDIGO", "NOMBRE DEL PUERTO", "PAÍS", "TIPO", "ESTADO"};

        int currentX = 0;
        for (int i = 0; i < headers.length; i++) {
            JRDesignStaticText header = new JRDesignStaticText();
            header.setText(headers[i]);
            header.setX(currentX);
            header.setY(0);
            header.setWidth(colWidths[i]);
            header.setHeight(20);
            header.setBold(true);
            header.setFontSize(8f);
            header.setMode(ModeEnum.OPAQUE);
            header.setBackcolor(new Color(24, 43, 73));
            header.setForecolor(Color.WHITE);
            header.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
            headerBand.addElement(header);
            currentX += colWidths[i];
        }
        design.setColumnHeader(headerBand);

        // Detail Band
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(18);

        currentX = 0;
        for (int i = 0; i < fieldNames.length; i++) {
            JRDesignTextField textField = new JRDesignTextField();
            JRDesignExpression expr = new JRDesignExpression();
            expr.setText("$F{" + fieldNames[i] + "}");
            textField.setExpression(expr);
            textField.setX(currentX);
            textField.setY(0);
            textField.setWidth(colWidths[i]);
            textField.setHeight(16);
            textField.setFontSize(8f);
            textField.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
            detailBand.addElement(textField);
            currentX += colWidths[i];
        }
        ((JRDesignSection) design.getDetailSection()).addBand(detailBand);

        // Footer Band
        JRDesignBand footerBand = new JRDesignBand();
        footerBand.setHeight(20);
        JRDesignTextField footerText = new JRDesignTextField();
        JRDesignExpression footerExpr = new JRDesignExpression();
        footerExpr.setText("\"SGA Catálogo Maestro de Puertos - Página \" + $V{PAGE_NUMBER}");
        footerText.setExpression(footerExpr);
        footerText.setX(0);
        footerText.setY(4);
        footerText.setWidth(555);
        footerText.setHeight(14);
        footerText.setFontSize(8f);
        footerText.setForecolor(new Color(148, 163, 184));
        footerText.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        footerBand.addElement(footerText);
        design.setPageFooter(footerBand);

        return design;
    }
}
