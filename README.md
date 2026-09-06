# footpedia

Football fixtures, reminders, team/player analytics and an AI agent for the
Premier League, La Liga, Serie A and the Bundesliga.

Java 17 · Spring Boot 3.5.16 · Postgres (Supabase) · Redis · Liquibase · Resilience4j

## Three upstream providers now

A third provider, ESPN's unofficial site API, sits alongside the two below.
It's undocumented (no key, no published rate limit, no ToS), so it's used as
a supplement, not a dependency: faster live score/status than
football-data.org's 5-min cycle, plus lineups, match events (goals/cards/
subs) and richer team stats for the live season — none of which
football-data.org's free tier or API-Football's scarce budget cover today.
Verified against a live response before building against it: it does **not**
reliably expose a shot map or clean per-team xG, despite some write-ups
claiming otherwise — this stops at lineups/events/team stats rather than
persisting data that isn't actually there.

FBref (for progressive passes, pressures, xG-chain-style advanced stats) was
considered and **not** built: a live test against its schedule page returned
a Cloudflare bot-challenge page, not HTML, meaning a plain HTTP client (Jsoup
included) cannot get past it — that would require deliberately defeating
Cloudflare's bot protection, which this project won't build tooling for.

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
person to open a player page spends one request; the result is written to
Postgres **permanently**, and everyone after that is served locally. Re-fetching happens
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

The database is [Supabase](https://supabase.com) Postgres (hosted — no local
install, no Docker). Redis still runs natively:

```bash
brew services start redis
```

Create a Supabase project, then from **Project Settings -> Database ->
Connection string** grab the host and password for the direct connection or
session pooler (port 5432 — not the 6543 transaction pooler, which breaks
Hibernate's prepared statement caching).

Copy `.env.example` to `.env` and fill in `DB_HOST`/`DB_PASSWORD` (and the
provider API keys), then run:

```bash
mvn spring-boot:run
```

Liquibase applies the schema against Supabase on first boot — no manual
table creation needed.

## Build plan

**Week 1 — Telegram as the UI**

- [x] Schema, Liquibase migrations, quota guard
- [x] football-data.org client + scheduled fixture/standings ingestion —
      Premier League, La Liga, Serie A, Bundesliga, Champions League
- [x] ESPN client — live score/status speed-up, lineups, match events,
      team stats (see "Three upstream providers" above)
- [x] Redis cache-aside with the TTL table above
- [x] Reminder scheduler — Redis ZSET keyed on kickoff, `ZRANGEBYSCORE` poller
- [ ] Telegram bot: reminders out, questions in
- [ ] Claude agent with a read-only `run_sql` tool

**Later**

- [ ] API-Football lazy loader + staleness policy
- [ ] Historical (API-Football) backfill for La Liga, Serie A, Bundesliga,
      Champions League — currently Premier League only; the 100/day budget
      is shared, so this waits until PL's backfill finishes
- [ ] Analytics rollups: form-adjusted table, xG over/under-performance,
      fixture difficulty runs, home/away splits
- [ ] Reconciliation job — nightly diff against the provider for corrected
      scores, postponements, rescinded cards
- [ ] Circuit breaker + DLQ with replay

**Done, ahead of plan** — [footpedia-fe](https://github.com/SABHYA18/footpedia-fe):
multi-league switching + per-league themes, stadium/crest imagery, lineups +
match timeline + pitch-diagram formations, previous-fixture history, team
detail pages, collapsible Home sections.
