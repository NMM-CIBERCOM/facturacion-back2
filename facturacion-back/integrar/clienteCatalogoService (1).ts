import { apiUrl } from './api';

export interface ClienteDatos {
  idCliente?: number;
  rfc: string;
  razonSocial?: string;
  nombre?: string;
  paterno?: string;
  materno?: string;
  correoElectronico?: string;
  pais?: string;
  domicilioFiscal?: string;
  regimenFiscal?: string;
  registroTributario?: string;
  usoCfdi?: string;
}

export interface ClienteResponse {
  encontrado: boolean;
  cliente?: ClienteDatos | null;
}

class ClienteCatalogoService {
  public buildRfc(iniciales: string, fecha: string, homoclave: string): string {
    return `${(iniciales || '').toUpperCase()}${(fecha || '').toUpperCase()}${(homoclave || '').toUpperCase()}`.trim();
  }

  public async buscarClientePorRFC(rfc: string): Promise<ClienteResponse> {
    const normalized = (rfc || '').toUpperCase();
    if (!normalized) {
      return { encontrado: false, cliente: null };
    }
    try {
      const resp = await fetch(apiUrl(`/catalogo-clientes/${encodeURIComponent(normalized)}`));
      const data = await resp.json().catch(() => null);
      if (!data) {
        return { encontrado: false, cliente: null };
      }
      const cliente: ClienteDatos | undefined = data?.cliente ? {
        rfc: data.cliente.rfc,
        razonSocial: data.cliente.razonSocial,
        nombre: data.cliente.nombre,
        paterno: data.cliente.paterno,
        materno: data.cliente.materno,
        correoElectronico: data.cliente.correoElectronico,
        pais: data.cliente.pais,
        domicilioFiscal: data.cliente.domicilioFiscal,
        regimenFiscal: data.cliente.regimenFiscal,
        registroTributario: data.cliente.registroTributario,
        usoCfdi: data.cliente.usoCfdi,
      } : undefined;
      return { encontrado: Boolean(data?.encontrado && cliente), cliente: cliente ?? null };
    } catch (e) {
      console.error('Error consultando cliente catálogo por RFC', e);
      return { encontrado: false, cliente: null };
    }
  }

  /**
   * Busca clientes por RFC parcial (para autocompletado)
   * Usa el endpoint GET /api/catalogo-clientes/buscar?rfc=...&limit=10
   */
  public async buscarClientesPorRFCParcial(rfc: string, limit: number = 10): Promise<ClienteDatos[]> {
    const normalized = (rfc || '').toUpperCase().trim();
    if (!normalized || normalized.length < 3) {
      return [];
    }
    try {
      const resp = await fetch(apiUrl(`/catalogo-clientes/buscar?rfc=${encodeURIComponent(normalized)}&limit=${limit}`));
      if (resp.ok) {
        const data = await resp.json();
        if (data.clientes && Array.isArray(data.clientes)) {
          return data.clientes.map((c: any) => ({
            rfc: c.rfc || '',
            razonSocial: c.razonSocial,
            nombre: c.nombre,
            paterno: c.paterno,
            materno: c.materno,
            correoElectronico: c.correoElectronico,
            pais: c.pais,
            domicilioFiscal: c.domicilioFiscal,
            regimenFiscal: c.regimenFiscal,
            registroTributario: c.registroTributario,
            usoCfdi: c.usoCfdi,
          }));
        }
      }
      return [];
    } catch (e) {
      console.error('Error buscando clientes por RFC parcial', e);
      return [];
    }
  }

  /**
   * Guarda un nuevo cliente en el catálogo
   * Usa el endpoint POST /api/catalogo-clientes
   */
  public async guardarCliente(cliente: any): Promise<{ success: boolean; idCliente?: number; error?: string }> {
    try {
      const resp = await fetch(apiUrl('/catalogo-clientes'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(cliente),
      });
      
      const data = await resp.json();
      
      if (resp.ok && !data.error) {
        return {
          success: true,
          idCliente: data.idCliente,
        };
      } else {
        return {
          success: false,
          error: data.error || 'Error al guardar cliente',
        };
      }
    } catch (e) {
      console.error('Error guardando cliente', e);
      return {
        success: false,
        error: e instanceof Error ? e.message : 'Error desconocido',
      };
    }
  }

  /**
   * Obtiene todos los clientes
   * Usa el endpoint GET /api/catalogo-clientes
   */
  public async obtenerTodos(): Promise<ClienteDatos[]> {
    try {
      const resp = await fetch(apiUrl('/catalogo-clientes'));
      if (resp.ok) {
        const data = await resp.json();
        if (data.clientes && Array.isArray(data.clientes)) {
          return data.clientes.map((c: any) => ({
            idCliente: c.idCliente,
            rfc: c.rfc || '',
            razonSocial: c.razonSocial,
            nombre: c.nombre,
            paterno: c.paterno,
            materno: c.materno,
            correoElectronico: c.correoElectronico,
            pais: c.pais,
            domicilioFiscal: c.domicilioFiscal,
            regimenFiscal: c.regimenFiscal,
            registroTributario: c.registroTributario,
            usoCfdi: c.usoCfdi,
          }));
        }
      }
      return [];
    } catch (e) {
      console.error('Error obteniendo todos los clientes', e);
      return [];
    }
  }

  /**
   * Obtiene clientes paginados
   * Usa el endpoint GET /api/catalogo-clientes?page={page}&size={size}
   */
  public async obtenerTodosPaginados(page: number = 0, size: number = 5): Promise<{
    clientes: ClienteDatos[];
    total: number;
    page: number;
    size: number;
    totalPages: number;
    hasNext: boolean;
    hasPrevious: boolean;
  }> {
    try {
      const resp = await fetch(apiUrl(`/catalogo-clientes?page=${page}&size=${size}`));
      if (resp.ok) {
        const data = await resp.json();
        return {
          clientes: (data.clientes || []).map((c: any) => ({
            idCliente: c.idCliente,
            rfc: c.rfc || '',
            razonSocial: c.razonSocial,
            nombre: c.nombre,
            paterno: c.paterno,
            materno: c.materno,
            correoElectronico: c.correoElectronico,
            pais: c.pais,
            domicilioFiscal: c.domicilioFiscal,
            regimenFiscal: c.regimenFiscal,
            registroTributario: c.registroTributario,
            usoCfdi: c.usoCfdi,
          })),
          total: data.total || 0,
          page: data.page || 0,
          size: data.size || 5,
          totalPages: data.totalPages || 0,
          hasNext: data.hasNext || false,
          hasPrevious: data.hasPrevious || false,
        };
      }
      return {
        clientes: [],
        total: 0,
        page: 0,
        size: 5,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
      };
    } catch (e) {
      console.error('Error obteniendo clientes paginados', e);
      return {
        clientes: [],
        total: 0,
        page: 0,
        size: 5,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
      };
    }
  }

  /**
   * Obtiene un cliente por ID
   * Usa el endpoint GET /api/catalogo-clientes/id/{id}
   */
  public async obtenerPorId(id: number): Promise<ClienteDatos | null> {
    try {
      const resp = await fetch(apiUrl(`/catalogo-clientes/id/${id}`));
      if (resp.ok) {
        const data = await resp.json();
        if (data.cliente) {
          const c = data.cliente;
          return {
            idCliente: c.idCliente,
            rfc: c.rfc || '',
            razonSocial: c.razonSocial,
            nombre: c.nombre,
            paterno: c.paterno,
            materno: c.materno,
            correoElectronico: c.correoElectronico,
            pais: c.pais,
            domicilioFiscal: c.domicilioFiscal,
            regimenFiscal: c.regimenFiscal,
            registroTributario: c.registroTributario,
            usoCfdi: c.usoCfdi,
          };
        }
      }
      return null;
    } catch (e) {
      console.error('Error obteniendo cliente por ID', e);
      return null;
    }
  }

  /**
   * Actualiza un cliente existente
   * Usa el endpoint PUT /api/catalogo-clientes/{id}
   */
  public async actualizarCliente(id: number, cliente: any): Promise<{ success: boolean; error?: string }> {
    try {
      const resp = await fetch(apiUrl(`/catalogo-clientes/${id}`), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(cliente),
      });
      
      const data = await resp.json();
      
      if (resp.ok && !data.error) {
        return { success: true };
      } else {
        return {
          success: false,
          error: data.error || 'Error al actualizar cliente',
        };
      }
    } catch (e) {
      console.error('Error actualizando cliente', e);
      return {
        success: false,
        error: e instanceof Error ? e.message : 'Error desconocido',
      };
    }
  }

  /**
   * Elimina un cliente
   * Usa el endpoint DELETE /api/catalogo-clientes/{id}
   */
  public async eliminarCliente(id: number): Promise<{ success: boolean; error?: string }> {
    try {
      const resp = await fetch(apiUrl(`/catalogo-clientes/${id}`), {
        method: 'DELETE',
      });
      
      const data = await resp.json();
      
      if (resp.ok && !data.error) {
        return { success: true };
      } else {
        return {
          success: false,
          error: data.error || 'Error al eliminar cliente',
        };
      }
    } catch (e) {
      console.error('Error eliminando cliente', e);
      return {
        success: false,
        error: e instanceof Error ? e.message : 'Error desconocido',
      };
    }
  }
}

export const clienteCatalogoService = new ClienteCatalogoService();