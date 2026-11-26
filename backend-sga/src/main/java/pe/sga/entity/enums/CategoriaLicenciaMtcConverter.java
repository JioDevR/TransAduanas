package pe.sga.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CategoriaLicenciaMtcConverter implements AttributeConverter<CategoriaLicenciaMtc, String> {

    @Override
    public String convertToDatabaseColumn(CategoriaLicenciaMtc attribute) {
        return attribute != null ? attribute.getCodigo() : null;
    }

    @Override
    public CategoriaLicenciaMtc convertToEntityAttribute(String dbData) {
        return dbData != null ? CategoriaLicenciaMtc.fromCodigo(dbData) : null;
    }
}
