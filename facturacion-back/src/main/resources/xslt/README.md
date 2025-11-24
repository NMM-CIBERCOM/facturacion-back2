# XSLT para Cadena Original CFDI 4.0

## Importante

Para generar correctamente la cadena original del CFDI 4.0 y evitar el error **CFDI40102**, es necesario usar el XSLT oficial del SAT.

## Cómo obtener el XSLT oficial

1. Visita el sitio oficial del SAT: https://www.sat.gob.mx/cfd/4
2. Busca y descarga el archivo `cadenaoriginal_4_0.xslt`
3. Coloca el archivo en este directorio: `src/main/resources/xslt/cadenaoriginal_4_0.xslt`
4. El código lo detectará automáticamente y lo usará para generar la cadena original

## Alternativa

Si no puedes encontrar el XSLT en el sitio del SAT, también puedes buscarlo en:
- Repositorios oficiales del SAT en GitHub
- Documentación técnica del SAT para CFDI 4.0

## Nota

Sin el XSLT oficial, el sistema usará una normalización manual que puede no coincidir exactamente con lo que el SAT espera, causando el error CFDI40102.

