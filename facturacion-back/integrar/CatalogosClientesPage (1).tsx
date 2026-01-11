import React, { useState, useEffect } from 'react';
import { Card } from './Card';
import { FormField } from './FormField';
import { SelectField } from './SelectField';
import { CheckboxField } from './CheckboxField';
import { Button } from './Button';
import { RfcAutocomplete } from './RfcAutocomplete';
import { MagnifyingGlassIcon } from './icons/MagnifyingGlassIcon';
import { PencilSquareIcon } from './icons/PencilSquareIcon';
import { TrashIcon } from './icons/TrashIcon';
import { REGIMEN_FISCAL_OPTIONS, USO_CFDI_OPTIONS } from '../constants';
import { clienteCatalogoService, ClienteDatos } from '../services/clienteCatalogoService';
import { codigoPostalService } from '../services/codigoPostalService';

interface ClienteFormData {
  id?: number;
  rfc: string;
  razonSocial: string;
  nombre?: string;
  apellidoPaterno?: string;
  apellidoMaterno?: string;
  tipoPersona: 'fisica' | 'moral';
  esExtranjero: boolean;
  esPolitico: boolean;
  codigoPostal: string;
  pais: string;
  estado: string;
  municipio: string;
  colonia: string;
  calle: string;
  numeroExterior: string;
  numeroInterior: string;
  correoElectronico: string;
  telefono?: string;
  regimenFiscal: string;
  usoCfdi: string;
}

const initialFormData: ClienteFormData = {
  rfc: '',
  razonSocial: '',
  nombre: '',
  apellidoPaterno: '',
  apellidoMaterno: '',
  tipoPersona: 'moral',
  esExtranjero: false,
  esPolitico: false,
  codigoPostal: '',
  pais: 'MEX',
  estado: '',
  municipio: '',
  colonia: '',
  calle: '',
  numeroExterior: '',
  numeroInterior: '',
  correoElectronico: '',
  telefono: '',
  regimenFiscal: REGIMEN_FISCAL_OPTIONS[0].value,
  usoCfdi: USO_CFDI_OPTIONS[0].value,
};

export const CatalogosClientesPage: React.FC = () => {
  const [clientes, setClientes] = useState<ClienteFormData[]>([]);
  const [formData, setFormData] = useState<ClienteFormData>(initialFormData);
  const [modoEdicion, setModoEdicion] = useState(false);
  const [colonias, setColonias] = useState<string[]>([]);
  const [cargandoCP, setCargandoCP] = useState(false);
  const [filtroBusqueda, setFiltroBusqueda] = useState('');
  const [buscando, setBuscando] = useState(false);
  const [paginaActual, setPaginaActual] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalClientes, setTotalClientes] = useState(0);
  const [cargandoClientes, setCargandoClientes] = useState(false);
  const PAGE_SIZE = 5;

  useEffect(() => {
    cargarClientes();
  }, [paginaActual]);

  const cargarClientes = async () => {
    setCargandoClientes(true);
    try {
      const resultado = await clienteCatalogoService.obtenerTodosPaginados(paginaActual, PAGE_SIZE);
      const clientesFormateados: ClienteFormData[] = resultado.clientes.map(c => {
        // Extraer código postal del domicilio fiscal si existe
        let codigoPostal = '';
        if (c.domicilioFiscal) {
          const cpMatch = c.domicilioFiscal.match(/C\.P\.\s*(\d{5})/i) || 
                          c.domicilioFiscal.match(/\b(\d{5})\b/);
          if (cpMatch) {
            codigoPostal = cpMatch[1];
          }
        }
        
        return {
          id: c.idCliente,
          rfc: c.rfc || '',
          razonSocial: c.razonSocial || '',
          nombre: c.nombre,
          apellidoPaterno: c.paterno,
          apellidoMaterno: c.materno,
          tipoPersona: (c.rfc?.length || 0) === 12 ? 'moral' : 'fisica',
          esExtranjero: false,
          esPolitico: false,
          codigoPostal: codigoPostal,
          pais: c.pais || 'MEX',
          estado: '',
          municipio: '',
          colonia: '',
          calle: '',
          numeroExterior: '',
          numeroInterior: '',
          correoElectronico: c.correoElectronico || '',
          telefono: '',
          regimenFiscal: c.regimenFiscal || REGIMEN_FISCAL_OPTIONS[0].value,
          usoCfdi: c.usoCfdi || USO_CFDI_OPTIONS[0].value,
        };
      });
      setClientes(clientesFormateados);
      setTotalPaginas(resultado.totalPages);
      setTotalClientes(resultado.total);
    } catch (error) {
      console.error('Error al cargar clientes:', error);
    } finally {
      setCargandoClientes(false);
    }
  };

  const cambiarPagina = (nuevaPagina: number) => {
    if (nuevaPagina >= 0 && nuevaPagina < totalPaginas) {
      setPaginaActual(nuevaPagina);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    const newValue = type === 'checkbox' ? (e.target as HTMLInputElement).checked : value;

    setFormData(prev => ({ ...prev, [name]: newValue }));

    if (name === 'rfc') {
      detectarTipoPersona(value);
    }
  };

  const handleCheckboxChange = (name: string, checked: boolean) => {
    setFormData(prev => ({ ...prev, [name]: checked }));
  };

  const detectarTipoPersona = (rfc: string) => {
    const rfcUpper = rfc.toUpperCase().trim();
    if (rfcUpper.length === 12) {
      setFormData(prev => ({ ...prev, tipoPersona: 'moral' }));
    } else if (rfcUpper.length === 13) {
      setFormData(prev => ({ ...prev, tipoPersona: 'fisica' }));
    }
  };

  const handleClienteSelect = (cliente: ClienteDatos) => {
    // Extraer código postal del domicilio fiscal si existe
    let codigoPostal = '';
    if (cliente.domicilioFiscal) {
      const cpMatch = cliente.domicilioFiscal.match(/C\.P\.\s*(\d{5})/i) || 
                      cliente.domicilioFiscal.match(/\b(\d{5})\b/);
      if (cpMatch) {
        codigoPostal = cpMatch[1];
      }
    }
    
    setFormData(prev => ({
      ...prev,
      rfc: cliente.rfc,
      razonSocial: cliente.razonSocial || prev.razonSocial,
      nombre: cliente.nombre || prev.nombre,
      apellidoPaterno: cliente.paterno || prev.apellidoPaterno,
      apellidoMaterno: cliente.materno || prev.apellidoMaterno,
      correoElectronico: cliente.correoElectronico || prev.correoElectronico,
      pais: cliente.pais || prev.pais || 'MEX',
      regimenFiscal: cliente.regimenFiscal || prev.regimenFiscal,
      usoCfdi: cliente.usoCfdi || prev.usoCfdi,
      codigoPostal: codigoPostal || prev.codigoPostal,
    }));
    
    // Si hay código postal, cargar sus datos
    if (codigoPostal && codigoPostal.length === 5) {
      cargarDatosCP(codigoPostal);
    }
  };

  const handleRfcNotFound = () => {
    // Si no se encuentra el RFC, permitir continuar con el alta manual
  };

  const cargarDatosCP = async (cp: string) => {
    if (!cp || cp.length !== 5) {
      setColonias([]);
      setFormData(prev => ({ ...prev, estado: '', municipio: '', colonia: '' }));
      return;
    }

    setCargandoCP(true);
    try {
      const data = await codigoPostalService.obtenerDatosCP(cp);
      if (data) {
        setFormData(prev => ({
          ...prev,
          estado: data.estado || '',
          municipio: data.municipio || '',
          colonia: '',
        }));
        setColonias(data.colonias || []);
      } else {
        setColonias([]);
        setFormData(prev => ({ ...prev, estado: '', municipio: '', colonia: '' }));
      }
    } catch (error) {
      console.error('Error al cargar código postal:', error);
      setColonias([]);
    } finally {
      setCargandoCP(false);
    }
  };

  const handleCodigoPostalChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const cp = e.target.value.replace(/\D/g, '').slice(0, 5);
    setFormData(prev => ({ ...prev, codigoPostal: cp }));

    if (cp.length === 5) {
      cargarDatosCP(cp);
    } else {
      setColonias([]);
      setFormData(prev => ({ ...prev, estado: '', municipio: '', colonia: '' }));
    }
  };

  const handleAgregar = async () => {
    if (!formData.rfc || !formData.razonSocial || !formData.codigoPostal || !formData.correoElectronico) {
      alert('Por favor complete los campos obligatorios marcados con *');
      return;
    }

    try {
      const clienteData: any = {
        rfc: formData.rfc,
        razon_social: formData.razonSocial,
        nombre: formData.nombre,
        paterno: formData.apellidoPaterno,
        materno: formData.apellidoMaterno,
        correo_electronico: formData.correoElectronico,
        regimen_fiscal: formData.regimenFiscal,
        uso_cfdi: formData.usoCfdi,
        pais: formData.pais || 'MEX',
        calle: formData.calle,
        numero_exterior: formData.numeroExterior,
        numero_interior: formData.numeroInterior,
        colonia: formData.colonia,
        municipio: formData.municipio,
        estado: formData.estado,
        codigo_postal: formData.codigoPostal,
      };

      if (modoEdicion && formData.id) {
        // Actualizar cliente existente
        const result = await clienteCatalogoService.actualizarCliente(formData.id, clienteData);
        if (result.success) {
          alert('Cliente actualizado exitosamente');
          // Volver a la primera página después de actualizar
          setPaginaActual(0);
          await cargarClientes();
          setModoEdicion(false);
        } else {
          alert(`Error al actualizar cliente: ${result.error}`);
          return;
        }
      } else {
        // Crear nuevo cliente
        const result = await clienteCatalogoService.guardarCliente(clienteData);
        if (result.success) {
          alert('Cliente guardado exitosamente');
          // Volver a la primera página después de crear (donde aparecerá el nuevo)
          setPaginaActual(0);
          await cargarClientes();
        } else {
          alert(`Error al guardar cliente: ${result.error}`);
          return;
        }
      }

      setFormData(initialFormData);
      setColonias([]);
    } catch (error) {
      console.error('Error al guardar cliente:', error);
      alert('Error al guardar cliente. Por favor intente nuevamente.');
    }
  };

  const handleEditar = async (cliente: ClienteFormData) => {
    if (!cliente.id) {
      alert('No se puede editar un cliente sin ID');
      return;
    }
    
    try {
      // Cargar datos completos del cliente desde el backend
      const clienteCompleto = await clienteCatalogoService.obtenerPorId(cliente.id);
      if (!clienteCompleto) {
        alert('No se pudo cargar los datos del cliente');
        return;
      }
      
      // Extraer código postal del domicilio fiscal si existe
      let codigoPostal = '';
      if (clienteCompleto.domicilioFiscal) {
        const cpMatch = clienteCompleto.domicilioFiscal.match(/C\.P\.\s*(\d{5})/i) || 
                        clienteCompleto.domicilioFiscal.match(/\b(\d{5})\b/);
        if (cpMatch) {
          codigoPostal = cpMatch[1];
        }
      }
      
      // Extraer dirección del domicilio fiscal
      let calle = '';
      let numeroExterior = '';
      let numeroInterior = '';
      let colonia = '';
      
      if (clienteCompleto.domicilioFiscal) {
        // Intentar parsear el domicilio fiscal
        const domicilio = clienteCompleto.domicilioFiscal;
        
        // Extraer código postal primero y removerlo del string para evitar confusión
        const cpPattern = /C\.P\.\s*(\d{5})/i;
        const cpMatch = domicilio.match(cpPattern);
        let domicilioSinCP = domicilio;
        if (cpMatch) {
          domicilioSinCP = domicilio.replace(cpPattern, '').trim();
        }
        // También remover códigos postales sueltos de 5 dígitos al final
        domicilioSinCP = domicilioSinCP.replace(/\b\d{5}\b(?=\s*$)/g, '').trim();
        
        // Buscar número interior primero (más específico)
        const numIntMatch = domicilioSinCP.match(/Int\.\s*([^,]+)/i);
        if (numIntMatch) {
          numeroInterior = numIntMatch[1].trim();
          domicilioSinCP = domicilioSinCP.replace(/Int\.\s*[^,]+/i, '').trim();
        }
        
        // Buscar número exterior (evitar capturar el CP que ya removimos)
        // Buscar números de 1-4 dígitos seguidos opcionalmente de letras, pero no al inicio
        // El número exterior generalmente está después del nombre de la calle
        const numExtMatch = domicilioSinCP.match(/\s+(\d{1,4}[A-Z]?)(?:\s|,|$)/);
        if (numExtMatch) {
          numeroExterior = numExtMatch[1];
          // Remover el número exterior del string para extraer la calle
          domicilioSinCP = domicilioSinCP.replace(/\s+\d{1,4}[A-Z]?(?:\s|,|$)/, '').trim();
        }
        
        // Dividir por comas para extraer colonia, municipio, estado
        const partes = domicilioSinCP.split(',').map(p => p.trim()).filter(p => p.length > 0);
        
        if (partes.length > 0) {
          // La primera parte es generalmente la calle
          calle = partes[0];
          
          // Buscar colonia (generalmente la segunda o tercera parte, antes de municipio/estado)
          if (partes.length > 1) {
            // La colonia generalmente está después de la calle
            // Buscar hasta encontrar municipio o estado
            for (let i = 1; i < partes.length; i++) {
              const parte = partes[i];
              // Si parece ser un municipio o estado conocido, detener
              if (parte.match(/^(Veracruz|Coatepec|MEX|México|Ciudad de México)/i)) {
                break;
              }
              // Si no es municipio/estado y tiene más de 2 caracteres, probablemente es colonia
              if (parte.length > 2 && !parte.match(/^\d+$/)) {
                colonia = parte;
                break;
              }
            }
          }
        }
      }
      
      // Cargar datos del código postal si existe antes de establecer el formData
      let estadoCP = '';
      let municipioCP = '';
      let coloniasCP: string[] = [];
      
      if (codigoPostal && codigoPostal.length === 5) {
        try {
          const datosCP = await codigoPostalService.obtenerDatosCP(codigoPostal);
          if (datosCP) {
            estadoCP = datosCP.estado || '';
            municipioCP = datosCP.municipio || '';
            coloniasCP = datosCP.colonias || [];
          }
        } catch (error) {
          console.error('Error al cargar datos del código postal:', error);
        }
      }
      
      setFormData({
        id: clienteCompleto.idCliente,
        rfc: clienteCompleto.rfc || '',
        razonSocial: clienteCompleto.razonSocial || '',
        nombre: clienteCompleto.nombre,
        apellidoPaterno: clienteCompleto.paterno,
        apellidoMaterno: clienteCompleto.materno,
        tipoPersona: (clienteCompleto.rfc?.length || 0) === 12 ? 'moral' : 'fisica',
        esExtranjero: false,
        esPolitico: false,
        codigoPostal: codigoPostal,
        pais: clienteCompleto.pais || 'MEX',
        estado: estadoCP,
        municipio: municipioCP,
        colonia: colonia,
        calle: calle,
        numeroExterior: numeroExterior,
        numeroInterior: numeroInterior,
        correoElectronico: clienteCompleto.correoElectronico || '',
        telefono: '',
        regimenFiscal: clienteCompleto.regimenFiscal || REGIMEN_FISCAL_OPTIONS[0].value,
        usoCfdi: clienteCompleto.usoCfdi || USO_CFDI_OPTIONS[0].value,
      });
      
      // Establecer las colonias disponibles
      if (coloniasCP.length > 0) {
        setColonias(coloniasCP);
      }
      
      setModoEdicion(true);
    } catch (error) {
      console.error('Error al cargar cliente para editar:', error);
      alert('Error al cargar los datos del cliente');
    }
  };

  const handleEliminar = async (id: number) => {
    if (!confirm('¿Está seguro de eliminar este cliente?')) {
      return;
    }
    
    try {
      const result = await clienteCatalogoService.eliminarCliente(id);
      if (result.success) {
        alert('Cliente eliminado exitosamente');
        // Si la página actual queda vacía después de eliminar, ir a la anterior
        if (clientes.length === 1 && paginaActual > 0) {
          setPaginaActual(paginaActual - 1);
        } else {
          await cargarClientes();
        }
      } else {
        alert(`Error al eliminar cliente: ${result.error}`);
      }
    } catch (error) {
      console.error('Error al eliminar cliente:', error);
      alert('Error al eliminar cliente. Por favor intente nuevamente.');
    }
  };

  const handleBuscar = async () => {
    if (!filtroBusqueda.trim()) {
      alert('Ingrese un RFC o razón social para buscar');
      return;
    }

    setBuscando(true);
    try {
      const rfc = filtroBusqueda.trim().toUpperCase();
      const result = await clienteCatalogoService.buscarClientePorRFC(rfc);
      
      if (result.encontrado && result.cliente) {
        const clienteEncontrado: ClienteFormData = {
          id: Date.now(),
          rfc: result.cliente.rfc,
          razonSocial: result.cliente.razonSocial || '',
          nombre: result.cliente.nombre,
          apellidoPaterno: result.cliente.paterno,
          apellidoMaterno: result.cliente.materno,
          tipoPersona: result.cliente.rfc.length === 12 ? 'moral' : 'fisica',
          esExtranjero: false,
          esPolitico: false,
          codigoPostal: '',
          pais: result.cliente.pais || 'MEX',
          estado: '',
          municipio: '',
          colonia: '',
          calle: '',
          numeroExterior: '',
          numeroInterior: '',
          correoElectronico: result.cliente.correoElectronico || '',
          telefono: '',
          regimenFiscal: result.cliente.regimenFiscal || REGIMEN_FISCAL_OPTIONS[0].value,
          usoCfdi: result.cliente.usoCfdi || USO_CFDI_OPTIONS[0].value,
        };
        setClientes([clienteEncontrado]);
      } else {
        alert('No se encontró ningún cliente con ese RFC');
      }
    } catch (error) {
      console.error('Error al buscar cliente:', error);
      alert('Error al buscar cliente. Por favor intente nuevamente.');
    } finally {
      setBuscando(false);
    }
  };

  // Nota: El filtro de búsqueda ahora se maneja en el backend con paginación
  // Si se necesita búsqueda, se debería implementar en el backend también

  return (
    <div className="space-y-6 p-4 sm:p-6">
      {/* Formulario de Cliente */}
      <Card title="Datos Fiscales">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div className="md:col-span-2 lg:col-span-3">
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              RFC *
            </label>
            <RfcAutocomplete
              value={formData.rfc}
              onChange={(rfc) => {
                setFormData(prev => ({ ...prev, rfc }));
                detectarTipoPersona(rfc);
              }}
              onSelect={handleClienteSelect}
              onNotFound={handleRfcNotFound}
              required
            />
          </div>

          <div className="md:col-span-2 lg:col-span-3 flex flex-wrap gap-4">
            <CheckboxField
              name="esExtranjero"
              label="Extranjero"
              checked={formData.esExtranjero}
              onChange={(e) => handleCheckboxChange('esExtranjero', e.target.checked)}
            />
            <CheckboxField
              name="esPolitico"
              label="P. Político"
              checked={formData.esPolitico}
              onChange={(e) => handleCheckboxChange('esPolitico', e.target.checked)}
            />
          </div>

          {formData.tipoPersona === 'moral' ? (
            <div className="md:col-span-2 lg:col-span-3">
              <FormField
                name="razonSocial"
                label="Razón Social *"
                value={formData.razonSocial}
                onChange={handleChange}
                required
              />
            </div>
          ) : (
            <>
              <FormField
                name="nombre"
                label="Nombre *"
                value={formData.nombre || ''}
                onChange={handleChange}
                required={formData.tipoPersona === 'fisica'}
              />
              <FormField
                name="apellidoPaterno"
                label="Apellido Paterno *"
                value={formData.apellidoPaterno || ''}
                onChange={handleChange}
                required={formData.tipoPersona === 'fisica'}
              />
              <FormField
                name="apellidoMaterno"
                label="Apellido Materno"
                value={formData.apellidoMaterno || ''}
                onChange={handleChange}
              />
            </>
          )}

          <FormField
            name="codigoPostal"
            label="Código Postal *"
            value={formData.codigoPostal}
            onChange={handleCodigoPostalChange}
            required
            maxLength={5}
            disabled={cargandoCP}
          />

          <FormField
            name="pais"
            label="País"
            value={formData.pais}
            onChange={handleChange}
            disabled={false}
          />

          <FormField
            name="estado"
            label="Estado"
            value={formData.estado}
            onChange={handleChange}
            disabled={false}
          />

          <FormField
            name="municipio"
            label="Municipio/Delegación"
            value={formData.municipio}
            onChange={handleChange}
            disabled={false}
          />

          <SelectField
            name="colonia"
            label="Colonia *"
            value={formData.colonia}
            onChange={handleChange}
            options={colonias.map(c => ({ value: c, label: c }))}
            required
            disabled={colonias.length === 0 || cargandoCP}
          />

          <FormField
            name="calle"
            label="Calle *"
            value={formData.calle}
            onChange={handleChange}
            required
          />

          <FormField
            name="numeroExterior"
            label="Número exterior"
            value={formData.numeroExterior}
            onChange={handleChange}
            placeholder="Ej: 123, MZ 5, LT 10"
          />

          <FormField
            name="numeroInterior"
            label="Número interior"
            value={formData.numeroInterior}
            onChange={handleChange}
            placeholder="Ej: EDIF A, DEP 101"
          />

          <div className="md:col-span-2 lg:col-span-3">
            <FormField
              name="correoElectronico"
              label="Correo Electrónico *"
              type="email"
              value={formData.correoElectronico}
              onChange={handleChange}
              required
            />
          </div>

          <SelectField
            name="regimenFiscal"
            label="Régimen Fiscal *"
            value={formData.regimenFiscal}
            onChange={handleChange}
            options={REGIMEN_FISCAL_OPTIONS}
            required
          />

          <SelectField
            name="usoCfdi"
            label="Uso Factura *"
            value={formData.usoCfdi}
            onChange={handleChange}
            options={USO_CFDI_OPTIONS}
            required
          />
        </div>

        <div className="text-xs text-gray-500 dark:text-gray-400 mt-4">
          Los campos marcados con: * son obligatorios
        </div>

        <div className="flex flex-col sm:flex-row gap-2 sm:gap-4 mt-6">
          <Button
            type="button"
            onClick={handleAgregar}
            variant="primary"
            className="w-full sm:w-auto"
          >
            {modoEdicion ? 'Modificar' : 'Agregar'}
          </Button>
          {modoEdicion && (
            <Button
              type="button"
              onClick={() => {
                setFormData(initialFormData);
                setModoEdicion(false);
                setColonias([]);
              }}
              variant="neutral"
              className="w-full sm:w-auto"
            >
              Cancelar
            </Button>
          )}
        </div>
      </Card>

      {/* Datos de Contacto */}
      <Card title="Datos de Contacto">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <FormField
            name="nombre"
            label="Nombre"
            value={formData.nombre || ''}
            onChange={handleChange}
          />
          <FormField
            name="apellidoPaterno"
            label="Apellido Paterno"
            value={formData.apellidoPaterno || ''}
            onChange={handleChange}
          />
          <FormField
            name="apellidoMaterno"
            label="Apellido Materno"
            value={formData.apellidoMaterno || ''}
            onChange={handleChange}
          />
          <FormField
            name="telefono"
            label="Teléfono"
            type="tel"
            value={formData.telefono || ''}
            onChange={handleChange}
          />
        </div>
      </Card>

      {/* Búsqueda */}
      <Card title="Buscar Clientes">
        <div className="flex flex-col sm:flex-row gap-2">
          <FormField
            name="filtroBusqueda"
            label="Buscar"
            value={filtroBusqueda}
            onChange={(e) => setFiltroBusqueda(e.target.value)}
            placeholder="Buscar por RFC o razón social..."
            className="flex-1"
          />
          <Button
            type="button"
            onClick={handleBuscar}
            variant="secondary"
            disabled={buscando}
            className="w-full sm:w-auto mt-6 sm:mt-0 flex items-center justify-center gap-2"
          >
            <MagnifyingGlassIcon className="w-5 h-5" />
            <span>{buscando ? 'Buscando...' : 'Buscar'}</span>
          </Button>
        </div>
      </Card>

      {/* Tabla de Clientes */}
      {(clientes.length > 0 || cargandoClientes) && (
        <Card title={`Clientes Registrados (${totalClientes} total)`}>
          {cargandoClientes ? (
            <div className="text-center py-8">
              <div className="text-gray-500 dark:text-gray-400">Cargando clientes...</div>
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                  <thead className="bg-gray-50 dark:bg-gray-700">
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                        RFC
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                        Razón Social / Nombre
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                        Correo
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                        Acciones
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                    {clientes.map((cliente) => (
                      <tr key={cliente.id} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                          {cliente.rfc}
                        </td>
                        <td className="px-4 py-4 text-sm text-gray-900 dark:text-white">
                          {cliente.tipoPersona === 'moral'
                            ? cliente.razonSocial
                            : `${cliente.nombre || ''} ${cliente.apellidoPaterno || ''} ${cliente.apellidoMaterno || ''}`.trim()}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                          {cliente.correoElectronico}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm font-medium">
                          <div className="flex items-center gap-2">
                            <button
                              onClick={() => handleEditar(cliente)}
                              className="text-green-600 hover:text-green-900 dark:text-green-400 dark:hover:text-green-300"
                              title="Editar"
                            >
                              <PencilSquareIcon className="w-5 h-5" />
                            </button>
                            <button
                              onClick={() => handleEliminar(cliente.id!)}
                              className="text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300"
                              title="Eliminar"
                            >
                              <TrashIcon className="w-5 h-5" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              
              {/* Controles de Paginación */}
              {totalPaginas > 1 && (
                <div className="flex items-center justify-between px-4 py-3 border-t border-gray-200 dark:border-gray-700 sm:px-6">
                  <div className="flex flex-1 justify-between sm:hidden">
                    <button
                      onClick={() => cambiarPagina(paginaActual - 1)}
                      disabled={paginaActual === 0}
                      className="relative inline-flex items-center rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      Anterior
                    </button>
                    <button
                      onClick={() => cambiarPagina(paginaActual + 1)}
                      disabled={paginaActual >= totalPaginas - 1}
                      className="relative ml-3 inline-flex items-center rounded-md border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      Siguiente
                    </button>
                  </div>
                  <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
                    <div>
                      <p className="text-sm text-gray-700 dark:text-gray-300">
                        Mostrando <span className="font-medium">{(paginaActual * PAGE_SIZE) + 1}</span> a{' '}
                        <span className="font-medium">
                          {Math.min((paginaActual + 1) * PAGE_SIZE, totalClientes)}
                        </span>{' '}
                        de <span className="font-medium">{totalClientes}</span> clientes
                      </p>
                    </div>
                    <div>
                      <nav className="isolate inline-flex -space-x-px rounded-md shadow-sm" aria-label="Pagination">
                        <button
                          onClick={() => cambiarPagina(paginaActual - 1)}
                          disabled={paginaActual === 0}
                          className="relative inline-flex items-center rounded-l-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 dark:ring-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 focus:z-20 focus:outline-offset-0 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          <span className="sr-only">Anterior</span>
                          <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                            <path fillRule="evenodd" d="M12.79 5.23a.75.75 0 01-.02 1.06L8.832 10l3.938 3.71a.75.75 0 11-1.04 1.08l-4.5-4.25a.75.75 0 010-1.08l4.5-4.25a.75.75 0 011.06.02z" clipRule="evenodd" />
                          </svg>
                        </button>
                        {Array.from({ length: totalPaginas }, (_, i) => i).map((pageNum) => {
                          // Mostrar solo algunas páginas alrededor de la actual
                          if (
                            pageNum === 0 ||
                            pageNum === totalPaginas - 1 ||
                            (pageNum >= paginaActual - 1 && pageNum <= paginaActual + 1)
                          ) {
                            return (
                              <button
                                key={pageNum}
                                onClick={() => cambiarPagina(pageNum)}
                                className={`relative inline-flex items-center px-4 py-2 text-sm font-semibold ${
                                  pageNum === paginaActual
                                    ? 'z-10 bg-primary text-white focus:z-20 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary'
                                    : 'text-gray-900 dark:text-gray-100 ring-1 ring-inset ring-gray-300 dark:ring-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 focus:z-20 focus:outline-offset-0'
                                }`}
                              >
                                {pageNum + 1}
                              </button>
                            );
                          } else if (pageNum === paginaActual - 2 || pageNum === paginaActual + 2) {
                            return (
                              <span key={pageNum} className="relative inline-flex items-center px-4 py-2 text-sm font-semibold text-gray-700 dark:text-gray-300 ring-1 ring-inset ring-gray-300 dark:ring-gray-600">
                                ...
                              </span>
                            );
                          }
                          return null;
                        })}
                        <button
                          onClick={() => cambiarPagina(paginaActual + 1)}
                          disabled={paginaActual >= totalPaginas - 1}
                          className="relative inline-flex items-center rounded-r-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 dark:ring-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700 focus:z-20 focus:outline-offset-0 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          <span className="sr-only">Siguiente</span>
                          <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                            <path fillRule="evenodd" d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z" clipRule="evenodd" />
                          </svg>
                        </button>
                      </nav>
                    </div>
                  </div>
                </div>
              )}
            </>
          )}
        </Card>
      )}
    </div>
  );
};

