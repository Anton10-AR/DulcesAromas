import 'package:decimal/decimal.dart';
import 'package:dulces_aromas/comun/formato.dart';
import 'package:flutter_test/flutter_test.dart';

/// RNF19: bolivianos con 2 decimales, fechas dd/mm/aaaa en hora de La Paz.
void main() {
  test('los montos se muestran en bolivianos con 2 decimales', () {
    expect(bolivianos(Decimal.parse('120')), 'Bs 120.00');
    expect(bolivianos(Decimal.parse('1234.5')), 'Bs 1,234.50');
    expect(bolivianos(Decimal.parse('0.1')), 'Bs 0.10');
  });

  test('un monto del JSON se convierte a decimal sin perder exactitud', () {
    expect(decimalDe(395.5), Decimal.parse('395.5'));
    expect(decimalDe(0.1) + decimalDe(0.2), Decimal.parse('0.3'));
    expect(decimalDe(120), Decimal.parse('120'));
  });

  test('las fechas se muestran en hora de La Paz', () {
    // 03:30 UTC del 3 de noviembre son las 23:30 del 2 de noviembre en La Paz.
    final instante = DateTime.utc(2026, 11, 3, 3, 30);
    expect(fecha(instante), '02/11/2026');
    expect(fechaHora(instante), '02/11/2026 23:30');
  });

  test('un horario elegido se manda a la API con la zona de La Paz', () {
    expect(instanteLaPaz(DateTime(2026, 11, 3, 10, 0)), '2026-11-03T10:00:00-04:00');
  });
}
