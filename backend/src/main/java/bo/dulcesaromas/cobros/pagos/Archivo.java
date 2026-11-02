package bo.dulcesaromas.cobros.pagos;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Bytes de un archivo con su tipo de contenido.
 */
@Embeddable
record Archivo(@Column(name = "tipo_contenido") String tipoContenido, @Column(name = "datos") byte[] datos) {
}
