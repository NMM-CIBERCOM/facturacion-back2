package com.cibercom.facturacion_back.service;

import com.cibercom.facturacion_back.dto.SatValidationRequest;
import com.cibercom.facturacion_back.dto.SatValidationResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SatValidationService {

    /**
     * Simula la validación del SAT para los datos de facturación
     * En un ambiente real, aquí se conectaría con los servicios del SAT
     */
    public SatValidationResponse validarDatosSat(SatValidationRequest request) {
        List<String> errores = new ArrayList<>();
        
        // Validar nombre
        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            errores.add("El nombre es obligatorio");
        } else if (request.getNombre().length() < 3) {
            errores.add("El nombre debe tener al menos 3 caracteres");
        }
        
        // Validar RFC
        if (!validarFormatoRfc(request.getRfc())) {
            errores.add("El RFC no tiene un formato válido");
        }
        
        // Validar código postal
        if (!validarCodigoPostal(request.getCodigoPostal())) {
            errores.add("El código postal debe tener 5 dígitos numéricos");
        }
        
        // Validar régimen fiscal
        if (!validarRegimenFiscal(request.getRegimenFiscal())) {
            errores.add("El régimen fiscal no es válido para persona física");
        }
        
        // Simular validación con el SAT (en ambiente real aquí se haría la consulta)
        boolean datosValidosEnSat = simularConsultaSat(request);
        
        if (!datosValidosEnSat) {
            errores.add("Los datos no coinciden con los registros del SAT");
        }
        
        // Construir respuesta
        boolean esValido = errores.isEmpty();
        
        return SatValidationResponse.builder()
                .valido(esValido)
                .mensaje(esValido ? "Datos válidos según el SAT" : "Datos inválidos")
                .timestamp(LocalDateTime.now())
                .errores(errores)
                .datosValidados(esValido ? construirDatosValidados(request) : null)
                .build();
    }
    
    private boolean validarFormatoRfc(String rfc) {
        if (rfc == null || rfc.trim().isEmpty()) {
            return false;
        }
        
        // Validar formato básico de RFC
        String rfcUpper = rfc.toUpperCase();
        return rfcUpper.matches("^[A-Z&Ñ]{3,4}[0-9]{6}[A-Z0-9]{3}$");
    }
    
    private boolean validarCodigoPostal(String codigoPostal) {
        if (codigoPostal == null || codigoPostal.trim().isEmpty()) {
            return false;
        }
        
        return codigoPostal.matches("^[0-9]{5}$");
    }
    
    private boolean validarRegimenFiscal(String regimenFiscal) {
        if (regimenFiscal == null || regimenFiscal.trim().isEmpty()) {
            return false;
        }
        
        return Arrays.asList(SatValidationRequest.REGIMENES_FISICA).contains(regimenFiscal);
    }
    
    /**
     * Simula la consulta al SAT
     * En ambiente real, aquí se conectaría con los servicios web del SAT
     */
    private boolean simularConsultaSat(SatValidationRequest request) {
        // Simular latencia de red
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simular validación del SAT
        // En un caso real, aquí se validarían los datos contra la base de datos del SAT
        
        // Para este ejemplo, consideramos válidos los datos que pasan las validaciones básicas
        return validarFormatoRfc(request.getRfc()) && 
               validarCodigoPostal(request.getCodigoPostal()) && 
               validarRegimenFiscal(request.getRegimenFiscal());
    }
    
    private SatValidationResponse.DatosValidados construirDatosValidados(SatValidationRequest request) {
        return SatValidationResponse.DatosValidados.builder()
                .nombre(request.getNombre())
                .rfc(request.getRfc().toUpperCase())
                .codigoPostal(request.getCodigoPostal())
                .regimenFiscal(request.getRegimenFiscal())
                .tipoPersona("Persona Física")
                .build();
    }
} 