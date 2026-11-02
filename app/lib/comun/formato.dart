import 'package:decimal/decimal.dart';
import 'package:intl/intl.dart';

/// La Paz está en UTC-4 todo el año, sin horario de verano (RNF19).
const Duration _desfaseLaPaz = Duration(hours: -4);

final NumberFormat _dosDecimales = NumberFormat('#,##0.00', 'en_US');

/// Monto en bolivianos con 2 decimales: «Bs 120.00» (RNF19).
String bolivianos(Decimal monto) => 'Bs ${_dosDecimales.format(monto.toDouble())}';

/// Convierte un monto del JSON a [Decimal] sin perder exactitud: el texto más corto de un número JSON
/// representa exactamente el valor decimal que mandó el servidor.
Decimal decimalDe(Object? valor) => Decimal.parse(valor.toString());

DateTime aLaPaz(DateTime instante) => instante.toUtc().add(_desfaseLaPaz);

/// Fecha en formato dd/mm/aaaa, en hora de La Paz.
String fecha(DateTime instante) => DateFormat('dd/MM/yyyy').format(aLaPaz(instante));

/// Fecha y hora: «02/11/2026 10:00».
String fechaHora(DateTime instante) => DateFormat('dd/MM/yyyy HH:mm').format(aLaPaz(instante));

/// Un día y hora elegidos en La Paz, como instante con su zona, para mandarlo a la API.
String instanteLaPaz(DateTime diaYHora) {
  final texto = DateFormat("yyyy-MM-dd'T'HH:mm:00").format(diaYHora);
  return '$texto-04:00';
}
