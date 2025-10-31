package com.cibercom.facturacion_back.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class TwoFactorAuthUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(TwoFactorAuthUtil.class);
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final int TIME_STEP = 30; // 30 segundos por paso
    private static final int CODE_LENGTH = 6;
    
    /**
     * Genera una clave secreta para TOTP
     */
    public String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[20]; // 160 bits
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
    
    /**
     * Genera un código QR para Google Authenticator
     */
    public String generateQRCodeUrl(String username, String secretKey, String issuer) {
        String encodedSecret = Base32.encode(secretKey.getBytes());
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer, username, encodedSecret, issuer);
    }
    
    /**
     * Verifica un código TOTP
     */
    public boolean verifyCode(String secretKey, String code) {
        try {
            long currentTime = System.currentTimeMillis() / 1000 / TIME_STEP;
            
            // Verificar código actual y códigos de los últimos 2 pasos de tiempo
            for (int i = -2; i <= 2; i++) {
                String expectedCode = generateTOTP(secretKey, currentTime + i);
                if (code.equals(expectedCode)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Error verificando código TOTP: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Genera un código TOTP para un tiempo específico
     */
    private String generateTOTP(String secretKey, long timeStep) {
        try {
            byte[] key = Base64.getDecoder().decode(secretKey);
            byte[] time = longToBytes(timeStep);
            
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, HMAC_SHA1_ALGORITHM);
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(time);
            int offset = hash[hash.length - 1] & 0xf;
            
            int code = ((hash[offset] & 0x7f) << 24) |
                      ((hash[offset + 1] & 0xff) << 16) |
                      ((hash[offset + 2] & 0xff) << 8) |
                      (hash[offset + 3] & 0xff);
            
            code = code % (int) Math.pow(10, CODE_LENGTH);
            return String.format("%0" + CODE_LENGTH + "d", code);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error generando código TOTP: {}", e.getMessage());
            throw new RuntimeException("Error generando código TOTP", e);
        }
    }
    
    /**
     * Convierte un long a array de bytes
     */
    private byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xff);
            value >>= 8;
        }
        return result;
    }
    
    /**
     * Clase interna para codificación Base32
     */
    private static class Base32 {
        private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        
        public static String encode(byte[] data) {
            StringBuilder result = new StringBuilder();
            int buffer = 0;
            int bufferLength = 0;
            
            for (byte b : data) {
                buffer = (buffer << 8) | (b & 0xff);
                bufferLength += 8;
                
                while (bufferLength >= 5) {
                    result.append(ALPHABET.charAt((buffer >> (bufferLength - 5)) & 0x1f));
                    bufferLength -= 5;
                }
            }
            
            if (bufferLength > 0) {
                result.append(ALPHABET.charAt((buffer << (5 - bufferLength)) & 0x1f));
            }
            
            return result.toString();
        }
    }
}
