/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.cibercom.facturacion_back.dao;

import com.cibercom.facturacion_back.dto.FacturaFrontendRequest;

public interface FacturaDAO {
    void guardarFactura(FacturaFrontendRequest factura);
}
