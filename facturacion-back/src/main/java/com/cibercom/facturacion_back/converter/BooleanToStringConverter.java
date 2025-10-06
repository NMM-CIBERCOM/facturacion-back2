package com.cibercom.facturacion_back.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertidor para mapear valores Boolean a String compatibles con Oracle.
 * Escritura: '1'/'0'. Lectura tolerante: 'S'/'N', 'Y'/'N', '1'/'0', 'TRUE'/'FALSE'.
 */
@Converter
public class BooleanToStringConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        if (attribute == null) {
            return "0"; // persistimos como '0' por defecto
        }
        return attribute ? "1" : "0"; // usar 1/0 evita conflictos con CHECKs existentes
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return false;
        }
        String v = dbData.trim();
        if ("S".equalsIgnoreCase(v) || "Y".equalsIgnoreCase(v) || "TRUE".equalsIgnoreCase(v) || "1".equals(v)) {
            return true;
        }
        return false;
    }
}