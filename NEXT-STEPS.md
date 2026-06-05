# Pasos a seguir para levantar y verificar las métricas

## 1. Compilar core (tiene el OTELConfig modificado)

```
cd D:\Projects\SpoolFramework\core
mvn install -DskipTests
```

## 2. Compilar el crawler

```
cd D:\Projects\SpoolFramework\Crawler
mvn package -DskipTests
```

## 3. Levantar el stack de infraestructura

```
cd D:\Projects\SpoolFramework\Crawler\.docker
docker compose up -d
```

Servicios que estarán activos:
| Servicio | Puerto | Para qué |
|---|---|---|
| Postgres | 5432 | Inbox del crawler |
| Kafka | 9092 | Event bus |
| Tempo | 4317 / 4318 / 3200 | Trazas distribuidas |
| Loki | 3100 | Logs |
| OTel Collector | 4319 (gRPC) / 4320 (HTTP) / 8889 (scrape) | Recibe métricas de la app |
| Prometheus | 9090 | Scraping de métricas |
| Grafana | 3000 | Visualización |

## 4. Ejecutar la aplicación crawler

Arrancar la aplicación Java normalmente (desde el IDE o con `java -jar`).
Las variables de entorno relevantes tienen defaults correctos:
- `OTEL_METRICS_ENDPOINT` → `http://localhost:4320/v1/metrics` (OTel Collector)
- `OTEL_TRACES_ENDPOINT` → `http://localhost:4318/v1/traces` (Tempo)
- `OTEL_LOGS_ENDPOINT` → `http://localhost:3100/otlp/v1/logs` (Loki)

## 5. Verificar que Prometheus recibe las métricas

Abrir http://localhost:9090 → pestaña "Status > Targets".
Debe aparecer el job `spool` con estado `UP`.

Si no aparece en los primeros 30 segundos, revisar que el OTel Collector está corriendo
y que la app ha enviado al menos una métrica (esperar un ciclo de poll, ~10 s).

## 6. Consultar métricas en Grafana

Abrir http://localhost:3000 → Explore → datasource **Prometheus**.

### Latencia p95 del crawler (resultado en segundos)
```promql
histogram_quantile(0.95,
  sum(rate(spool_crawler_latency_gemini_market_bucket[1m])) by (le)
)
```
Seleccionar unidad del panel: **Time → seconds (s)**. Grafana mostrará "82ms", "150ms", etc.

### Tasa de eventos capturados
```promql
rate(spool_crawler_events_total_gemini_market[1m])
```
Unidad del panel: **Misc → events/s** o simplemente **short**.

### Tasa de errores
```promql
rate(spool_crawler_errors_total_gemini_market[1m])
```

### Tamaño de payload (p90, en bytes)
```promql
histogram_quantile(0.90,
  sum(rate(spool_crawler_payload_size_gemini_market_bucket[1m])) by (le)
)
```
Unidad del panel: **Data → bytes (IEC)**.

> Si tienes varios crawlers (gemini-market, gemini-market-v2), sustituye el sourceId
> en el nombre de la métrica o usa una regex: `spool_crawler_latency_.*_bucket`.

## Notas

- **Prometheus convención de tiempo**: los valores de latencia están en **segundos** en
  Prometheus. El timer de la app convierte internamente ms → s antes de emitir. Los
  buckets configurados son `[0.001 … 10.0]` s, lo que cubre de 1 ms a 10 s.
- **Nombres de métricas**: con `add_metric_suffixes: false` en el OTel Collector, los
  nombres se normalizan tal cual (puntos → guiones bajos). No se añaden sufijos extra.
- **Mimir eliminado**: ya no es necesario. Prometheus es suficiente para la demo del TFT
  y tiene soporte maduro en Grafana 11.
