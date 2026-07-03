# Deployment Constraints

## Run exactly ONE application instance. Do not scale horizontally.

This backend keeps critical state in the JVM's own memory and on the container's
local disk. Running two or more instances behind a load balancer will silently
break core features. This is a known, accepted constraint for the current
(pilot/single-region) deployment — not something to work around by adding nodes.

### Why (what breaks with 2+ instances)

| Subsystem | Where state lives | Failure with multiple instances |
|-----------|-------------------|----------------------------------|
| WebSocket real-time (`WebSocketConfig` simple broker) | In-JVM heap | A client on node A never receives messages published on node B — order status, courier location, and kitchen tickets silently drop for ~half of users. |
| Rate limiting (`RateLimitService`, bucket4j local buckets) | In-JVM `ConcurrentHashMap` | Each node has its own counters → effective limit is N× intended. OTP/login abuse protection weakens per added node. |
| Image storage (`ImageStorageService`) | Container-local disk `/app/images` | Uploads written on node A return 404 when served from node B. |

### Required deployment shape

- **Exactly one** application container/instance.
- Single host is fine. Persist `/app/images` on a durable volume (already mounted
  in `docker-compose.yml`) so images survive restarts.
- Postgres, Redis, and RabbitMQ may run as their own single instances or managed
  services — the single-instance constraint is about the **app** tier only.
- Put the app behind a reverse proxy / TLS terminator, but with a **single**
  upstream. No round-robin across app replicas.

### What must change before horizontal scaling is safe

(Do NOT attempt these for the current launch — listed for future planning.)

1. Replace the in-memory STOMP broker with a RabbitMQ STOMP relay
   (`enableStompBrokerRelay`) so WebSocket sessions share state across nodes.
2. Move rate-limit buckets to Redis (real distributed bucket4j, not the current
   local map) so limits are global.
3. Move image storage to an object store (S3/GCS) or a shared network volume.

Until all three are done, treat the app tier as **stateful and single-instance.**
