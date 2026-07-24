# Roadmap d'implémentation — Portail de soumission (PORTAL)

**Projet :** FluxPro  
**Module :** Portail de soumission  
**Source :** [SPEC-PORTAIL-SOUMISSION-DETAILLEE.md](./SPEC-PORTAIL-SOUMISSION-DETAILLEE.md)  
**Version roadmap :** 0.2  
**Date :** 24 juillet 2026  
**Effort indicatif MVP :** 6–9 semaines (1 backend + 1 frontend familiers de DOS / CHN / ALR)

---

## 1. Objectif

Livrer un MVP permettant à un **demandeur non-utilisateur métier** de :

1. disposer d'un compte portail (interne créé par admin, ou externe selon §14 de la spec) ;
2. s'authentifier sur un realm dédié (et **activer** son compte interne à la 1ʳᵉ connexion) ;
3. accéder à son **espace** : catalogue des demandes disponibles pour son audience ;
4. remplir un formulaire + pièces jointes et soumettre ;
5. suivre l'avancement en lecture seule (et recevoir des emails aux étapes clés).

Deux variantes : **portail interne** (congés, absences…) et **portail externe** (usagers, partenaires) — même moteur, audiences isolées.

Le circuit interne (passages, délais, alertes, dashboard) reste inchangé.

---

## 2. Hypothèses retenues (spec §14)

| Sujet | Décision | Impact |
|---|---|---|
| Inscription **interne** | **Figée** — création admin + mdp provisoire + activation (changement mdp) à la 1ʳᵉ connexion | P0, P1, P3, P5 |
| Remise mdp provisoire | **Figée** — affichage **one-shot** UI admin ± **email** optionnel ; non relisible ensuite | P3, P5, P6 |
| Variantes de portail | `INTERNAL` / `EXTERNAL` (`portalUserType` + `portalAudience`) | P1, P2, P4 |
| Auth **externe** | **Figée** — **OTP email** à l'inscription et à chaque connexion | P3, P4 |
| Visibilité 1er maillon | **Option B** — soumission hors circuit | P0 → P2 |
| Emplacement `formSchema` | Sur `FileType` | P1, P5 |
| Suivi demandeur | Page lecture seule + emails aux étapes clés | P4, P6 |
| Pilote MVP | **≥1 type interne** (congé) ; **externe optionnel** | P0, P7 |

> **Gate P0 :** décisions d'auth + périmètre pilote **closes** ; rester à trancher : paramètres OTP (TTL) et `active` à la création interne.

---

## 3. Vue d'ensemble des phases

```
P0 Cadrage (OTP params / wireframes)
    │
    ▼
P1 Fondations données & sécurité
    │
    ├──────────────┐
    ▼              ▼
P2 Moteur soumission   P3 Auth portail (+ activation interne)
    │              │
    ├──────┬───────┘
    ▼      ▼
P5 Admin  P4 Front demandeur (interne / externe)
    │      │
    │      ▼
    │     P6 Suivi & notifications
    │      │
    └──┬───┘
       ▼
     P7 Durcissement & pilote
```

| Phase | Titre | Durée | Dépend de |
|---|---|---|---|
| **P0** | Décisions & cadrage | 2–3 j | — |
| **P1** | Fondations données & sécurité | 1–1,5 sem | P0 |
| **P2** | Moteur de soumission backend | 1,5–2 sem | P1 |
| **P3** | Authentification portail | 1–1,5 sem | P1 (// P2) |
| **P4** | Front portail demandeur | 1,5–2 sem | P2 + P3 |
| **P5** | Admin métier — config + comptes internes | 1–1,5 sem | P2 + P3 |
| **P6** | Suivi & notifications | 0,5–1 sem | P4 |
| **P7** | Durcissement & pilote | 1 sem | P5 + P6 |

---

## 4. Détail des phases

### P0 — Décisions & cadrage

**Objectif :** clôturer les points encore ouverts ; l'inscription interne est déjà actée.

**Livrables**

- [x] Inscription interne = création admin + mdp provisoire + activation 1ʳᵉ connexion
- [x] Canal de remise du mdp provisoire = one-shot UI admin ± email
- [x] Auth externe = **OTP email** (chaque connexion)
- [x] Option B confirmée (soumission hors circuit)
- [x] `formSchema` sur `FileType` + `portalAudience`
- [x] Suivi = page lecture seule + emails clés
- [x] Type(s) pilote MVP = **≥1 interne (congé)** ; **externe optionnel**
- [ ] Wireframes : login/activation interne, espace, admin création compte

**Exit criteria**

- Note de décisions à jour (spec §14)
- Schéma formulaire exemple pour le type pilote **Demande de congé** (§8.1)

---

### P1 — Fondations données & sécurité

**Objectif :** schéma SQL, audiences, assouplir les FKs `User`, isoler le realm `/api/portal/**`.

**Livrables**

- [ ] Table `portal_users` (+ script dans `docs/sql/`) incluant `mustChangePassword`, `passwordChangedAt`, `createdByAdminUserId`
- [ ] Colonnes `FileType` : `portalEnabled`, `portalAudience`, `chainTemplateId`, `defaultFirstStepResponsibleUserId`, `formSchema`, `requiredAttachmentKeys`
- [ ] `FileEntity.createdBy` nullable ou créateur technique + `portalUserId`
- [ ] `FileAttachment.uploader` adapté (portail / technique)
- [ ] Security filter distinct JWT métier vs token portail

**Exit criteria**

- Scripts SQL appliqués en local
- Endpoints métier inchangés / smoke tests OK
- Appel `/api/portal/**` refusé sans token portail

---

### P2 — Moteur de soumission backend

**Objectif :** validation → création dossier → PJ → `submit` → init chaîne auto + filtre audience (PORTAL-01…04, 11, 12).

**Livrables**

- [ ] `FormSchemaValidator` (types v1)
- [ ] `PortalSubmissionService` avec contrôle `portalUserType` ↔ `portalAudience`
- [ ] `GET /api/portal/form-types` (filtré audience) et `/schema`
- [ ] `POST /api/portal/submissions` (+ attachments)
- [ ] `GET /api/portal/submissions` et `/{ref}`
- [ ] Admin `PUT /api/admin/file-types/{code}/portal` (incl. audience) et `/form-schema`

**Exit criteria**

- Soumission E2E : `DRAFT` → `IN_PROGRESS` + référence + stage 1 actif
- Rejet si audience incompatible ou `formData` / PJ invalides
- PORTAL-05 : aucun endpoint passage exposé au portail

---

### P3 — Authentification portail

**Objectif :** realm auth dédié ; **activation interne** ; **OTP externe**.  
**Parallélisable avec P2** dès que P1 est clos.

**Livrables**

- [ ] `POST /api/portal/auth/login` (email + mdp — interne)
- [ ] `POST /api/portal/auth/activate` — mdp provisoire → nouveau mdp ; `mustChangePassword = false`
- [ ] Challenge `PASSWORD_CHANGE_REQUIRED` si 1ʳᵉ connexion / reset (PORTAL-14, PORTAL-15)
- [ ] `POST /api/portal/auth/register` (externe) + `otp/request` + `otp/verify`
- [ ] OTP à chaque login externe ; `emailVerifiedAt` à la 1ʳᵉ vérif ; pas de soumission si non vérifié
- [ ] JWT portail : `portalUserId`, `portalUserType`, `mustChangePassword`
- [ ] Rate limiting login / OTP / activate
- [ ] Isolation : un `PortalUser` ne voit que ses soumissions

**Exit criteria**

- Compte interne créé avec provisoire : login → force activation → puis accès espace
- Tant que `mustChangePassword` : refus catalogue / soumission / suivi
- Login portail ne donne aucun accès `/api/admin` ni passages
- Reset admin réarme correctement `mustChangePassword`

**Risques**

- Fuite du mdp provisoire (canal de remise)
- Confusion tokens métier vs portail côté front

---

### P4 — Front portail demandeur

**Objectif :** UI espace demandeur, variantes interne / externe.

**Livrables**

- [ ] Routes `/portal/internal` et `/portal/external` (ou équivalent) hors shell métier
- [ ] Écran login interne + **écran activation** (changement mdp obligatoire)
- [ ] Login / OTP externe
- [ ] Espace : catalogue (filtré), formulaire dynamique, upload PJ, confirmation + référence
- [ ] Mes demandes + détail suivi lecture seule
- [ ] Auth store portail séparé de `RequireAuth` métier

**Exit criteria**

- Parcours complet employé : 1ʳᵉ connexion → activation → soumission → suivi
- Aucun appel API métier depuis le portail

---

### P5 — Admin métier — config portail + comptes internes

**Objectif :** configurer les types **et** créer / gérer les comptes portail internes.

**Livrables**

- [ ] Écran config `FileType` : `portalEnabled`, `portalAudience`, chaîne, responsable défaut
- [ ] Éditeur schéma formulaire v1 (pas drag & drop)
- [ ] **CRUD comptes portail internes** : créer (mdp provisoire **one-shot** UI + option « envoyer par email »), modifier, activer/désactiver
- [ ] **Reset mot de passe** (nouveau provisoire one-shot ± email + `mustChangePassword = true`)
- [ ] Retrait portail (`portalEnabled = false`) sans supprimer dossiers (PORTAL-10)
- [ ] APIs : `POST/PATCH/GET /api/admin/portal-users`, `POST .../reset-password`

**Exit criteria**

- Admin crée un employé → celui-ci s'active et soumet sans autre intervention IT
- Admin active un 2ᵉ type (audience correcte) sans déploiement backend

---

### P6 — Suivi & notifications

**Objectif :** boucler le retour demandeur.

**Livrables**

- [ ] Vue suivi : statut, maillon courant, référence
- [ ] Emails : soumission reçue, étape clé, clôture
- [ ] (Optionnel) email de bienvenue interne sans mdp en clair, ou rappel « compte créé — contactez admin »

**Exit criteria**

- Demandeur reçoit email à soumission + clôture
- Contenu suivi limité au contrat lecture seule

---

### P7 — Durcissement & pilote

**Objectif :** sécuriser, tester, livrer le pilote **Demande de congé** (portail interne). Le portail externe peut être livré en code mais n'est **pas** requis pour le Go/No-Go MVP.

**Livrables**

- [ ] Tests : création admin, activation, isolation audience, soumission, PORTAL-01…15
- [ ] Scan / whitelist MIME PJ, quotas
- [ ] Runbook : SQL, SMTP, création comptes internes, activation type congé
- [ ] Pilote RH — **Demande de congé** (`portalAudience = INTERNAL`)
- [ ] Portail externe : présent ou reporté post-MVP (hors critère Go)
- [ ] Mesures : activation OK, taux soumission, tickets support

**Exit criteria**

- Go/No-Go pilote validé
- Zéro accès croisé métier ↔ portail / interne ↔ externe
- Spec → statut « implémenté MVP »

---

## 5. Planning indicatif

| Tranche | Chemin critique | En parallèle | Résultat |
|---|---|---|---|
| Sem 0 | P0 cadrage OTP / wireframes | Wireframes activation / admin one-shot mdp | Décisions auth closes |
| Sem 1–2 | P1 fondations SQL / sécu | Contrats OpenAPI | Realm + tables prêts |
| Sem 2–4 | P2 moteur soumission | P3 auth + **activation** | API E2E + login interne |
| Sem 4–6 | P4 front espace | P5 admin config **+ comptes** | Parcours UI complet |
| Sem 6–8 | P6 suivi / emails | Tests sécu audience | Retour demandeur |
| Sem 8–9 | P7 pilote interne | Runbook + correctifs | MVP production limitée |

---

## 6. Écarts code actuel → phases (spec §13)

| Écart | État actuel | Phase |
|---|---|---|
| `PortalUser` + auth | Absent | P1 + P3 |
| Variantes interne / externe | Absent | P1 + P2 + P4 |
| Création admin + activation mdp | Absent | P3 + P5 |
| Formulaire par type | `metadata` libre | P1 + P2 |
| `createdBy` / uploader | `User` obligatoire | P1 |
| Init chaîne auto | Manuel | P2 |
| Front espace demandeur | Absent | P4 |
| Suivi demandeur | Absent | P4 + P6 |
| Admin config + comptes portail | Absent | P5 |

---

## 7. MVP vs hors périmètre

### Inclus MVP

- Deux audiences (`INTERNAL` / `EXTERNAL`)
- Comptes internes : **création admin + mdp provisoire + activation 1ʳᵉ connexion**
- Auth externe : **OTP** à chaque connexion
- `formSchema` + `portalAudience` sur `FileType`
- Soumission auto Option B + init chaîne
- Espace demandeur (catalogue / soumettre / suivre)
- Admin : config types + CRUD comptes internes
- Emails clés + pilote **Demande de congé** (interne)
- Portail externe (OTP) : **optionnel** pour le Go MVP
- Règles PORTAL-01 à PORTAL-15

### Hors MVP / hors périmètre

- Obligation de livrer un type / parcours externe pour le Go/No-Go
- Self-register employés internes
- Option A — maillon `PORTAL_SUBMISSION` visible
- Éditeur drag & drop de formulaires
- SSO / LDAP portail
- Signature électronique du demandeur
- GED versioning / plan de classement

---

## 8. API cible (rappel)

### Auth & activation

| Méthode | Endpoint | Phase |
|---|---|---|
| `POST` | `/api/portal/auth/login` | P3 |
| `POST` | `/api/portal/auth/activate` | P3 |
| `POST` | `/api/portal/auth/register` | P3 |
| `POST` | `/api/portal/auth/otp/request` | P3 |
| `POST` | `/api/portal/auth/otp/verify` | P3 |

### Espace demandeur

| Méthode | Endpoint | Phase |
|---|---|---|
| `GET` | `/api/portal/form-types` | P2 / P4 |
| `GET` | `/api/portal/form-types/{code}/schema` | P2 |
| `POST` | `/api/portal/submissions` | P2 |
| `POST` | `/api/portal/submissions/{id}/attachments` | P2 |
| `GET` | `/api/portal/submissions` | P2 / P4 |
| `GET` | `/api/portal/submissions/{ref}` | P2 / P6 |

### Admin métier

| Méthode | Endpoint | Phase |
|---|---|---|
| `PUT` | `/api/admin/file-types/{code}/portal` | P2 / P5 |
| `PUT` | `/api/admin/file-types/{code}/form-schema` | P2 / P5 |
| `POST` | `/api/admin/portal-users` | P3 / P5 |
| `PATCH` | `/api/admin/portal-users/{id}` | P5 |
| `POST` | `/api/admin/portal-users/{id}/reset-password` | P3 / P5 |
| `GET` | `/api/admin/portal-users` | P5 |

---

## 9. Règles de gestion à couvrir en tests

| ID | Règle | Phase |
|---|---|---|
| PORTAL-01…10 | Voir spec §10 | P2 / P4 / P5 / P7 |
| PORTAL-11 | Interne → types `INTERNAL` / `BOTH` | P2 |
| PORTAL-12 | Externe → types `EXTERNAL` / `BOTH` | P2 |
| PORTAL-13 | Pas de self-register interne | P3 / P5 |
| PORTAL-14 | Création / reset → `mustChangePassword` + activation | P3 |
| PORTAL-15 | Tant que non activé : pas catalogue / soumission / suivi | P3 / P4 |

---

## 10. Historique

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-24 | Équipe produit / technique | Création roadmap d'implémentation MVP PORTAL |
| 2026-07-24 | Équipe produit / technique | v0.2 — inscription interne admin + mdp provisoire + activation ; audiences ; APIs admin portal-users ; PORTAL-11…15 |
| 2026-07-24 | Équipe produit / technique | Remise mdp provisoire figée : one-shot UI admin ± email |
| 2026-07-24 | Équipe produit / technique | Auth externe figée : OTP email (chaque connexion) |
| 2026-07-24 | Équipe produit / technique | Pilote MVP figé : ≥1 type interne (congé) ; externe optionnel |
