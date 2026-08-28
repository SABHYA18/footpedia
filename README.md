# footpedia

Football fixtures, reminders, team/player analytics and an AI agent for the
Premier League, La Liga, Serie A and the Bundesliga.

Java 17 · Spring Boot 3.5.16 · MySQL · Redis · Liquibase · Resilience4j

## The constraint that shapes everything

Two upstream providers, with very different economics:

| Provider | Free limit | Used for |
|---|---|---|
| [football-data.org](https://www.football-data.org/documentation/api) | 10 req/min, **no daily cap** | Competitions, fixtures, standings, scorers — the polling workhorse |
| [API-Football](https://www.api-football.com/pricing) | **100 req/day**, 10/min | Player stats, match detail, lineups, transfers — spent only on demand |

100 requests per day makes a request-through proxy physically impossible. The
whole architecture follows from that:

**Scheduled ingestion** (football-data.org) keeps fixtures and tables warm,
because polling there is effectively free.

**Lazy-load-then-persist** (API-Football) covers the expensive data. The first
person to open a player page spends one request; the result is written to MySQL
**permanently**, and everyone after that is served locally. Re-fetching happens
only when the staleness policy allows (stats older than 7 days *and* the player
has featured since). The daily budget therefore buys *coverage that accumulates*
rather than being re-spent on the same players.

A `reserve-threshold` withholds the tail of the daily budget so a burst of idle
browsing can never starve the evening's live fixtures.

## Design notes worth keeping

- **`raw_payload` is append-only.** Every provider response is stored raw before
  parsing. When the parsing logic turns out wrong, the entire database can be
  re-derived at zero API cost. Without it, one bad migration burns a day's quota.
- **`external_ref` maps provider IDs.** The two providers use different ID spaces
  for the same real-world entity. Internal surrogate IDs are the join key and
  translation happens only at the edges.
- **The quota guard fails closed.** If Redis is unreachable, calls are refused
  rather than sent unmetered — serving stale data beats burning an unrecoverable
  daily budget.
- **Hibernate never owns the schema** (`ddl-auto: validate`). Liquibase does.

## Cache TTLs

Derived from how fast each thing actually changes, not picked round-number-first:

| Data | TTL |
|---|---|
| Live score | 30s |
| Today's fixtures | 5m |
| Fixture list / standings | 1h |
| Player season stats | 12h |
| Transfers | 24h |
| Finished historical fixture | 30d |

## Running locally

Redis and MySQL run natively (no Docker on this machine).

```bash
brew services start redis
brew services start mysql
```

Create the database and a dedicated user:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS footpedia CHARACTER SET utf8mb4; CREATE USER IF NOT EXISTS 'footpedia'@'localhost' IDENTIFIED BY 'CHANGE_ME'; GRANT ALL PRIVILEGES ON footpedia.* TO 'footpedia'@'localhost'; FLUSH PRIVILEGES;"
```

Then copy `.env.example` to `.env`, fill it in, and run:

```bash
mvn spring-boot:run
```

Liquibase applies the schema on first boot.

## Build plan

**Week 1 — Premier League only, Telegram as the UI**

- [x] Schema, Liquibase migrations, quota guard
- [ ] football-data.org client + scheduled fixture/standings ingestion
- [ ] Redis cache-aside with the TTL table above
- [ ] Reminder scheduler — Redis ZSET keyed on kickoff, `ZRANGEBYSCORE` poller
- [ ] Telegram bot: reminders out, questions in
- [ ] Claude agent with a read-only `run_sql` tool

**Later**

- [ ] API-Football lazy loader + staleness policy
- [ ] Remaining three leagues (the schema is already multi-competition)
- [ ] Analytics rollups: form-adjusted table, xG over/under-performance,
      fixture difficulty runs, home/away splits
- [ ] Reconciliation job — nightly diff against the provider for corrected
      scores, postponements, rescinded cards
- [ ] Circuit breaker + DLQ with replay
- [ ] React frontend ([footpedia-fe](https://github.com/SABHYA18/footpedia-fe))
