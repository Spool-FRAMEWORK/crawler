# Diseño del contador de errores — qué ve el usuario real de Spool

## Las tres preguntas concretas

### "¿Si la siguiente ejecución no tiene errores, sigue saliendo 300?"

No. Después de reiniciar el proceso, el contador empieza en 0. El panel de Grafana muestra el valor del **proceso activo** — si la nueva ejecución no tiene errores, el panel muestra 0. Los 300 del arranque anterior siguen en el histórico de Prometheus (durante su periodo de retención, típicamente 15 días), pero el panel en modo "last 15 min" solo ve los datos actuales.

### "¿Se acumulan históricamente también los eventos?"

Sí, exactamente igual. `spool_crawler_events_total` y `spool_crawler_errors_total` se comportan igual: ambos se resetean con cada reinicio, ambos persisten en el histórico de Prometheus. Si amplías la ventana temporal de Grafana a "last 6 hours", verías el pico de errores del arranque anterior y el valle a 0 del actual. Si estás en "last 15 min" ves solo lo reciente.

### "¿Se muestran por ejecución reseteando por cada ejecución nueva?"

Sí, por diseño OTel/Prometheus. Cada proceso es una "sesión" de métricas. Reinicio = nueva sesión = contador desde 0. Esto es estándar en todos los sistemas que usan este stack (Spring Boot, Quarkus, etc.).

---

## ¿Refleja bien el uso real de Spool?

**Para el caso de uso principal — alguien que deja Spool corriendo continuamente:**
- El contador sube con cada error a lo largo del tiempo
- Un reinicio (deploy, crash) resetea el contador, pero la tasa de errores (`rate()`) no se ve afectada porque Prometheus detecta el reset automáticamente
- El operador ve exactamente qué está pasando en el proceso vivo

**Para el caso de "lo uso una vez y quiero saber cuántos errores tuve":**
- Arranca, procesa, mira el panel → ve los errores de esa sesión exacta
- Apaga → Prometheus guarda esa historia
- El siguiente arranque parte de 0 → si no hay errores, el panel muestra 0 ✓

**El único caso confuso:**
Si el usuario mira el dashboard durante un reinicio (ventana temporal ancha), vería el contador bajar de repente de 300 a 0. Eso es el reset visible. Para el TFT no es un problema porque las capturas son de una sesión concreta.

---

## Conclusión

El diseño actual es correcto para el uso previsto de Spool. No necesita cambios. El contador refleja la ejecución activa, que es lo que le importa al operador en tiempo real. El histórico está disponible ampliando la ventana temporal de Grafana si se necesita comparar sesiones.
