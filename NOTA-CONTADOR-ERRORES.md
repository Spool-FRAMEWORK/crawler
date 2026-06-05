# Comportamiento del contador de errores en reinicios

## Lo que ocurre

Sí, es exactamente eso. El contador `spool_crawler_errors_total` vive **en memoria** dentro del proceso Java. Cuando el proceso muere y se vuelve a levantar, el contador empieza desde 0. Prometheus no sabe nada de la historia anterior — solo ve la serie desde el último arranque.

Secuencia que explica lo que ves:

```
Arranque 1 → 300 errores → contador = 300
Proceso reiniciado
Arranque 2 → 300 errores nuevos → contador = 300 (no 600)
```

El panel de Grafana muestra el valor actual del proceso vivo, así que parece que "no sube" cuando en realidad empezó desde 0 otra vez.

## ¿Es un bug? No, es el contrato de los contadores OTel/Prometheus

Prometheus lo sabe: cuando detecta que un contador baja (300 → 0), marca un **reset** internamente. Las funciones `rate()` e `increase()` tienen en cuenta los resets y calculan correctamente aun después de reinicios. El valor bruto (`spool_crawler_errors_total`) en cambio muestra simplemente el estado del proceso actual.

Esto afecta a **todos** los contadores de cualquier sistema Prometheus/OTel. Es el comportamiento estándar y esperado.

## Consecuencias para el dashboard

| Query | Comportamiento tras reinicio |
|---|---|
| `spool_crawler_errors_total` (raw) | Empieza desde 0, pierde historia |
| `increase(errors[$__rate_interval])` | Correcto, detecta el reset automáticamente |
| `rate(errors[1m])` | Correcto, ídem |

## Mi opinión

El panel de "total acumulado" tiene sentido para ver la foto en un momento puntual, pero es frágil en producción porque cualquier reinicio lo resetea. Para el TFT está bien: arrancas, ejecutas el escenario, sacas la captura, y el contador refleja exactamente esa sesión.

Si en el futuro quisieras acumulado persistente entre reinicios, la solución estándar es una **recording rule** en Prometheus que agrega el `increase()` en un gauge externo, pero eso es overkill para lo que necesitas ahora.

## Conclusión

Lo que describes es correcto y esperado. El contador no "no funciona" — funciona perfectamente dentro del ciclo de vida del proceso. Si el panel sigue en 0 tras reinicio + nuevos errores, el problema sería otro (OTel no inicializado, etc.), pero si arranca en 0 y sube con cada error, todo está bien.
