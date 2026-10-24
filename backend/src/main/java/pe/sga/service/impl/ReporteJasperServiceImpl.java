package pe.sga.service.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import pe.sga.dto.reporte.UsuarioReporteItemDTO;
import pe.sga.entity.CatalogoMercancia;
import pe.sga.entity.CatalogoPuerto;
import pe.sga.entity.CatalogoTipoCarga;
import pe.sga.entity.Sede;
import pe.sga.entity.Usuario;
import pe.sga.repository.CatalogoMercanciaRepository;
import pe.sga.repository.CatalogoPuertoRepository;
import pe.sga.repository.CatalogoTipoCargaRepository;
import pe.sga.repository.SedeRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.IReporteJasperService;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteJasperServiceImpl implements IReporteJasperService {

    private final UsuarioRepository usuarioRepository;
    private final SedeRepository sedeRepository;
    private final CatalogoPuertoRepository puertoRepository;
    private final CatalogoTipoCargaRepository tipoCargaRepository;
    private final CatalogoMercanciaRepository mercanciaRepository;
    private final SpringTemplateEngine templateEngine;

    private static volatile JasperReport cachedUsuariosReport;

    // =========================================================================
    // 01. USUARIOS & SEGURIDAD TI (JASPER ENGINE LEGACY)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarUsuariosPdf() {
        List<Usuario> usuarios = usuarioRepository.findAll();

        List<UsuarioReporteItemDTO> items = usuarios.stream()
                .map(u -> UsuarioReporteItemDTO.builder()
                        .idUsuario(u.getIdUsuario())
                        .username(u.getUsername())
                        .nombreCompleto(u.getNombres() + " " + u.getApellidos())
                        .rol(u.getRol() != null ? u.getRol().name() : "-")
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

    // =========================================================================
    // 02. SEDES Y AGENCIAS TERRITORIALES (CORPORATE HTML / OPENHTMLTOPDF)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarSedesPdf() {
        List<Sede> sedes = sedeRepository.findAll();

        long operativas = sedes.stream()
                .filter(s -> "ACTIVO".equalsIgnoreCase(s.getEstado()) || "OPERATIVO".equalsIgnoreCase(s.getEstado()))
                .count();
        long inactivas = sedes.size() - operativas;
        long totalDistritos = sedes.stream()
                .map(Sede::getDistrito)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        Map<String, Object> variables = new HashMap<>();
        variables.put("items", sedes);
        variables.put("totalSedes", sedes.size());
        variables.put("totalOperativas", operativas);
        variables.put("totalInactivas", inactivas);
        variables.put("totalDistritos", totalDistritos > 0 ? totalDistritos : 1);
        variables.put("fechaGeneracion", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        return renderHtmlToPdf("reports/reporte-sedes", variables);
    }

    // =========================================================================
    // 03. PUERTOS Y TERMINALES ADUANEROS (CORPORATE HTML / OPENHTMLTOPDF)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarPuertosPdf() {
        List<CatalogoPuerto> puertos = puertoRepository.findAll();

        long maritimos = puertos.stream()
                .filter(p -> p.getTipo() != null && p.getTipo().toUpperCase().contains("MARIT"))
                .count();
        long activos = puertos.stream()
                .filter(p -> "ACTIVO".equalsIgnoreCase(p.getEstado()))
                .count();
        long otros = puertos.size() - maritimos;

        Map<String, Object> variables = new HashMap<>();
        variables.put("items", puertos);
        variables.put("totalPuertos", puertos.size());
        variables.put("totalMaritimos", maritimos);
        variables.put("totalOtros", otros);
        variables.put("totalActivos", activos);
        variables.put("fechaGeneracion", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        return renderHtmlToPdf("reports/reporte-puertos", variables);
    }

    // =========================================================================
    // 04. TIPOS DE CARGA & CONTENEDOR (CORPORATE HTML / OPENHTMLTOPDF)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarTiposCargaPdf() {
        List<CatalogoTipoCarga> tipos = tipoCargaRepository.findAll();

        long activos = tipos.stream()
                .filter(t -> "ACTIVO".equalsIgnoreCase(t.getEstado()))
                .count();
        long fcl = tipos.stream()
                .filter(t -> (t.getCodigo() != null && t.getCodigo().toUpperCase().contains("CONT"))
                        || (t.getNombre() != null && t.getNombre().toUpperCase().contains("CONTENEDOR")))
                .count();
        long lcl = tipos.size() - fcl;

        Map<String, Object> variables = new HashMap<>();
        variables.put("items", tipos);
        variables.put("totalTipos", tipos.size());
        variables.put("totalActivos", activos);
        variables.put("totalFcl", fcl);
        variables.put("totalLcl", lcl);
        variables.put("fechaGeneracion", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        return renderHtmlToPdf("reports/reporte-tipos-carga", variables);
    }

    // =========================================================================
    // 05. MERCANCÍAS & SUBPARTIDAS ARANCELARIAS (CORPORATE HTML / OPENHTMLTOPDF)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarMercanciasPdf() {
        List<CatalogoMercancia> mercancias = mercanciaRepository.findAll();

        long vuce = mercancias.stream()
                .filter(m -> Boolean.TRUE.equals(m.getRequierePermiso()))
                .count();
        long libre = mercancias.size() - vuce;
        long activos = mercancias.stream()
                .filter(m -> "ACTIVO".equalsIgnoreCase(m.getEstado()))
                .count();

        Map<String, Object> variables = new HashMap<>();
        variables.put("items", mercancias);
        variables.put("totalMercancias", mercancias.size());
        variables.put("totalVuce", vuce);
        variables.put("totalLibre", libre);
        variables.put("totalActivos", activos);
        variables.put("fechaGeneracion", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        return renderHtmlToPdf("reports/reporte-mercancias", variables);
    }

    // =========================================================================
    // MOTOR DE RENDERIZADO HTML CORPORATIVO A PDF
    // =========================================================================
    private ByteArrayInputStream renderHtmlToPdf(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String html = templateEngine.process(templateName, context);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();

            return new ByteArrayInputStream(os.toByteArray());
        } catch (Exception e) {
            log.error("Error al renderizar PDF corporativo HTML con plantilla {}: {}", templateName, e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF corporativo (" + templateName + "): " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES JASPER (LEGACY USUARIOS)
    // =========================================================================
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
}
