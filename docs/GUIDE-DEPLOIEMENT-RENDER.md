# Guide — Déployer `flux-pro-backend` sur Render

**Projet :** FluxPro — API Spring Boot (`flux-pro-backend`)  
**Cible :** Web Service Render (**Docker**)  
**Stack :** Java 17 · Spring Boot 4.1 · PostgreSQL · S3 · SMTP

> **Important :** Render **ne propose pas de runtime Java natif**.  
> Runtimes natifs : Node.js / Bun, Python, Ruby, Go, Rust, Elixir.  
> Pour Spring Boot, il faut créer un **Web Service Docker** (fichier `Dockerfile` à la racine du repo).

---

## 1. Prérequis

| Élément | Détail |
|--------|--------|
| Repo Git | `flux-pro-backend` (GitHub/GitLab) connecté à Render |
| Runtime Render | **Docker** (pas « Java ») |
| Build image | `Dockerfile` multi-stage (Maven + JRE 17) |
| BDD | **PostgreSQL** (Neon recommandé) |
| Stockage PJ | **S3** (disque Render éphémère → ne pas utiliser `local`) |
| Front | URL publique pour CORS + liens emails |

### Règles projet à respecter

- `spring.jpa.hibernate.ddl-auto=none` → schéma via scripts SQL manuels (`docs/sql/`), pas Hibernate.
- **Ne jamais** activer `ddl-auto=update` / `create` / `create-drop` sur Render.
- Les secrets (JWT, DB, SMTP, AWS) passent par **variables d’environnement**.

---

## 2. Architecture recommandée

```text
[ Front Next.js ] ──HTTPS──► [ Render Web Service Docker : flux-pro-backend ]
                                      │
                         ┌────────────┼────────────┐
                         ▼            ▼            ▼
                     PostgreSQL      AWS S3      SMTP
                     (Neon)       (attachments)  (alertes)
```

---

## 3. Dockerfile (obligatoire)

Le repo contient un `Dockerfile` à la racine :

```dockerfile
# Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -DskipTests clean package

# Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/flux-pro-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
```

Un `.dockerignore` limite le contexte de build (`.git`, `target/`, docs lourdes, etc.).

### Tester l’image en local (optionnel)

```bash
docker build -t flux-pro-backend .
docker run --rm -p 8080:8080 \
  -e PORT=8080 \
  -e SPRING_DATASOURCE_URL=... \
  -e SPRING_DATASOURCE_USERNAME=... \
  -e SPRING_DATASOURCE_PASSWORD=... \
  flux-pro-backend
```

---

## 4. Préparer la base PostgreSQL

### Option A — Neon (recommandée)

1. Créer / utiliser un projet Neon.
2. URL JDBC :

```text
jdbc:postgresql://HOST:5432/DB?sslmode=require
```

3. Exécuter **manuellement** les scripts de `docs/sql/` dans l’ordre chronologique, avant ou juste après le premier déploiement.

### Option B — Render PostgreSQL

1. **New → PostgreSQL**.
2. Noter host / database / user / password (Internal Database URL si le service et la DB sont sur Render).
3. JDBC :

```text
jdbc:postgresql://HOST:5432/DBNAME?sslmode=require
```

4. Appliquer les scripts SQL manuellement.

---

## 5. Créer le Web Service sur Render

1. **New → Web Service**
2. Connecter le repo `flux-pro-backend`
3. Paramètres :

| Champ | Valeur |
|--------|--------|
| Name | `flux-pro-backend` |
| Region | au plus proche (ex. Frankfurt / Oregon) |
| Language / Runtime | **Docker** |
| Branch | `main` (ou branche de prod) |
| Root Directory | `.` (racine du backend, là où se trouve le `Dockerfile`) |
| Dockerfile Path | `./Dockerfile` (défaut) |
| Docker Build Context Directory | `.` |
| Build / Start Command | **laisser vide** (géré par le Dockerfile) |

### Points critiques

- Choisir **Docker**, pas un autre language listé (Java n’apparaît pas).
- Render injecte `$PORT` : le `ENTRYPOINT` du Dockerfile le passe à Spring.
- Ne pas forcer le port `8080` côté Render (variable `PORT` automatique).

### Health Check

Chemins publics (cf. `SecurityConfig`) :

- `/v3/api-docs`
- `/swagger-ui.html`

---

## 6. Variables d’environnement

Dans Render → **Environment**.

### Obligatoires

| Variable | Notes |
|----------|--------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://...:5432/neondb?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | user Postgres |
| `SPRING_DATASOURCE_PASSWORD` | password Postgres |
| `FLUXPRO_JWT_SECRET` | secret **≥ 256 bits**, unique en prod |
| `FLUXPRO_CORS_ALLOWED_ORIGINS` | URL(s) du front, séparées par des virgules |
| `FLUXPRO_APP_BASE_URL` | URL du front **sans** slash final |
| `FLUXPRO_ATTACHMENTS_STORAGE` | `s3` |
| `FLUXPRO_S3_BUCKET` | nom du bucket |
| `FLUXPRO_S3_REGION` | ex. `us-east-1` |
| `AWS_ACCESS_KEY_ID` | clé IAM S3 |
| `AWS_SECRET_ACCESS_KEY` | secret IAM S3 |

### Fortement recommandées

| Variable | Valeur conseillée |
|----------|-------------------|
| `SPRING_JPA_DDL_AUTO` | `none` |
| `FLUXPRO_CLOCK_MODE` | `normal` en prod (pas `test`) |
| `MINTP_SMTP_HOST` | serveur SMTP |
| `MINTP_SMTP_PORT` | `587` |
| `MINTP_SMTP_USER` | compte SMTP |
| `MINTP_SMTP_PASSWORD` | mot de passe SMTP |
| `FLUXPRO_ALERTS_FROM` | adresse expéditeur |
| `FLUXPRO_EMAIL_REDIRECT_TO` | vide en prod réelle, ou boîte de test en pilote |

### Optionnelles (tenant)

| Variable | Exemple |
|----------|---------|
| `FLUXPRO_TENANT_NAME` | `MINTP Cameroun` |
| `FLUXPRO_PRODUCT_NAME` | `FluxPro` |
| `FLUXPRO_TENANT_TIMEZONE` | `Africa/Douala` |
| `FLUXPRO_TENANT_COUNTRY` | `CM` |
| `FLUXPRO_REFERENCE_PREFIX` | `MINTP` |
| `FLUXPRO_TENANT_BADGE` | `Déploiement pilote · MINTP Cameroun` |

---

## 7. Sécurité

1. Surcharger JWT, datasource, SMTP et AWS via l’environnement Render.
2. Idéalement, retirer les secrets en dur de `application.properties`.
3. Régénérer un `FLUXPRO_JWT_SECRET` dédié à la prod.
4. Ne jamais committer `application-secrets.properties`.

---

## 8. Stockage des pièces jointes

Le filesystem Render est **éphémère**.

```text
FLUXPRO_ATTACHMENTS_STORAGE=s3
FLUXPRO_S3_BUCKET=...
FLUXPRO_S3_REGION=...
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
```

Permissions IAM minimales : `PutObject`, `GetObject`, `DeleteObject`.

Voir aussi : `docs/GUIDE-IMPLEMENTATION-STOCKAGE-S3.md` (si présent).

---

## 9. Brancher le frontend

Sur `flux-pro-front` (voir aussi `flux-pro-front/docs/GUIDE-DEPLOIEMENT-RENDER.md`) :

```text
NEXT_PUBLIC_API_URL=https://flux-pro-backend.onrender.com
```

Côté backend (URL du front Render, **sans** slash final) :

```text
FLUXPRO_CORS_ALLOWED_ORIGINS=https://flux-pro-front.onrender.com
FLUXPRO_APP_BASE_URL=https://flux-pro-front.onrender.com
```
---

## 10. Déploiement / redéploiement

1. Commit + push du `Dockerfile` (et éventuellement `.dockerignore`).
2. Render détecte Docker, build l’image, démarre le container.
3. Logs attendus :

```text
Started FluxProBackendApplication
```

4. Vérifier :
   - `GET https://VOTRE-SERVICE.onrender.com/v3/api-docs`
   - `POST /api/auth/login`
   - Swagger : `/swagger-ui.html`

---

## 11. Checklist post-déploiement

- [ ] Runtime Render = **Docker**
- [ ] Scripts SQL appliqués sur Postgres
- [ ] Login OK
- [ ] CORS OK depuis le front
- [ ] Upload / download PJ via S3 OK
- [ ] Emails d’alerte OK (ou redirect de test)
- [ ] `FLUXPRO_CLOCK_MODE=normal`
- [ ] `SPRING_JPA_DDL_AUTO` reste `none`
- [ ] Secret JWT prod distinct du local

---

## 12. Pièges fréquents

| Problème | Cause / correctif |
|----------|-------------------|
| Pas de « Java » dans les runtimes | Normal → choisir **Docker** |
| Build Docker fail | `Dockerfile` absent / mauvais Root Directory |
| Service down / 502 | `PORT` non lu → vérifier `ENTRYPOINT` (`-Dserver.port=${PORT}`) |
| Crash DB au boot | URL/user/password Postgres, `sslmode=require` |
| Erreurs JPA | Schéma non appliqué (`ddl-auto=none`) |
| CORS bloqué | Front absent de `FLUXPRO_CORS_ALLOWED_ORIGINS` |
| PJ perdues | Stockage `local` au lieu de `s3` |
| Cold start long | Plan free : instance endormie après inactivité |
| Build Maven lent / OOM | Plan trop petit ; réduire contexte via `.dockerignore` |

---

## 13. Optionnel — Blueprint `render.yaml`

À la racine du repo :

```yaml
services:
  - type: web
    name: flux-pro-backend
    runtime: docker
    plan: starter
    dockerfilePath: ./Dockerfile
    dockerContext: .
    healthCheckPath: /v3/api-docs
    envVars:
      - key: SPRING_JPA_DDL_AUTO
        value: none
      - key: FLUXPRO_ATTACHMENTS_STORAGE
        value: s3
      - key: FLUXPRO_CLOCK_MODE
        value: normal
      - key: SPRING_DATASOURCE_URL
        sync: false
      - key: SPRING_DATASOURCE_USERNAME
        sync: false
      - key: SPRING_DATASOURCE_PASSWORD
        sync: false
      - key: FLUXPRO_JWT_SECRET
        generateValue: true
      - key: FLUXPRO_CORS_ALLOWED_ORIGINS
        sync: false
      - key: FLUXPRO_APP_BASE_URL
        sync: false
      - key: AWS_ACCESS_KEY_ID
        sync: false
      - key: AWS_SECRET_ACCESS_KEY
        sync: false
```

(`sync: false` = à renseigner dans le dashboard Render)

---

## 14. Références internes

| Fichier / dossier | Rôle |
|-------------------|------|
| `Dockerfile` | Build + runtime Java 17 sur Render |
| `.dockerignore` | Accélère / allège le build Docker |
| `src/main/resources/application.properties` | Config + noms des variables |
| `docs/sql/` | Scripts de schéma et seeds (manuels) |
| `.cursor/rules/database-schema.mdc` | Règle `ddl-auto=none` |
| Front `NEXT_PUBLIC_API_URL` | Pointage vers l’URL Render |
