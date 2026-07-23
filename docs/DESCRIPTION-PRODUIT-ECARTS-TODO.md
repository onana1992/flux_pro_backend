# FluxPro — Description produit, écarts et TODO

**Projet :** FluxPro — Gestion Électronique des Dossiers et suivi par chaîne de passation  
**Date :** 22 juillet 2026  
**Statut :** Document de cadrage produit (aligné sur le code actuel + cible)  
**Références :** [Cahier des charges MINTP](./CAHIER-DES-CHARGES-CHAINEFLUX-MINTP%20(1).md) · [Roadmap](./ROADMAP-IMPLEMENTATION-CHAINEFLUX.md) · [SPEC-DOS](./SPEC-DOS.md) · [SPEC-CHN](./SPEC-CHN.md) · [SPEC-ALR](./SPEC-ALR.md) · [Décision templates vs BPM](./DECISION-TEMPLATES-PARALLELES-VS-BPM.md) · [Guide onboarding type + template + alertes](./GUIDE-ADMIN-ONBOARDING-TYPE-TEMPLATE-ALERTES.md)

---

## 1. Clarification terminologique

| Terme | Signification dans FluxPro |
|-------|----------------------------|
| **GED** | **Gestion Électronique des Dossiers** (cycle de vie du dossier administratif) |
| **Pas GED documentaire** | Pas une bibliothèque de documents autonome (plan de classement, versioning doc, records management) |
| **Chaîne / template** | Circuit de passation configurable — **substitut officiel d’un BPM** ([décision](./DECISION-TEMPLATES-PARALLELES-VS-BPM.md)) ; étapes séquentielles ou parallèles (même `stepOrder`, join AND) |

---

## 2. Description produit (cible / communication)

### FluxPro

**Plateforme de Gestion Électronique des Dossiers et de suivi des circuits de passation**

FluxPro permet aux administrations et organisations de **créer, suivre et piloter des dossiers administratifs** de bout en bout. Chaque dossier suit un **circuit de traitement configurable** (chaîne de passation) : responsable identifié, délais mesurés, alertes en cas de retard, historique complet des transmissions.

Les pièces jointes accompagnent le dossier ; le produit se concentre sur le **cycle de vie du dossier**, pas sur une bibliothèque documentaire autonome.

### Vision

Devenir la plateforme de référence pour la **gestion électronique des dossiers administratifs** en Afrique.

### Mission

Remplacer registres papier, cahiers de transmission et tableurs par un système où chaque dossier est **numérique, localisable, responsable nommé et traçable**.

### Valeur ajoutée

- Dématérialisation du suivi des dossiers
- Circuits de passation configurables par type de dossier
- Responsable actuel toujours visible
- Gestion des délais (SLA) en jours / heures ouvrés
- Alertes et escalades automatiques
- Suivi en temps réel de chaque dossier
- Traçabilité complète des actions et transmissions
- Réduction des délais de traitement
- Tableaux de bord pour piloter charge et conformité
- Sécurisation des accès (organigramme, rôles, permissions)

### Pour qui

- Ministères et administrations centrales
- Collectivités territoriales
- Directions et délégations régionales
- Entreprises publiques à circuits hiérarchiques formalisés

*(Déploiement de référence : pilote MINTP Cameroun — courriers, marchés simplifiés, autorisations de travaux.)*

### Exemples de dossiers gérés

- Courriers et correspondances officielles
- Dossiers de marchés publics (circuits simplifiés)
- Autorisations et demandes administratives
- Plaintes et réclamations à circuit fixe
- Tout dossier qui circule avec visas et délais

### Fonctionnalités clés

- Création et cycle de vie du dossier (brouillon → en cours → clôturé / archivé)
- Types de dossiers et numérotation
- **Modèles de chaîne configurables** (étapes, rôles, délais, étapes optionnelles ou parallèles)
- Transmission, retour, suspension, réaffectation
- Pièces jointes liées au dossier (support du dossier, pas le cœur métier)
- Notifications in-app et email
- Tableau de bord et exports
- Organigramme, utilisateurs, RBAC, audit
- Intérim / suppléant (délégation d’action et d’alertes)
- API REST

### Ce que FluxPro est / n’est pas

| Est | N’est pas |
|-----|-----------|
| Gestion électronique **des dossiers** | GED **documentaire** (bibliothèque, plan de classement, versioning document) |
| Suivi de **chaînes de passation** | Moteur **BPM** générique (BPMN, Camunda, etc.) |
| Pilotage des **délais et responsabilités** | Signature électronique (hors cœur actuel) |

---

## 3. Positionnement réel du code (aujourd’hui)

FluxPro est aujourd’hui une application de **suivi de dossiers par chaîne hiérarchique** (souvent nommée ChaîneFlux dans les specs), avec :

- Auth JWT + RBAC + organigramme
- Dossiers + pièces jointes (stockage local)
- Templates de chaîne + passages (transmit / return / suspend / reassign)
- SLA ouvrés (calendrier métier admin) + alertes in-app / email + digest
- Dashboard opérationnel + export CSV
- Recherche dossiers (liste + command bar header)
- Paramètres tenant lite (timezone, préfixe référence, branding email / produit)
- Horloge de test pour démontrer les alertes
- Intérim / suppléant (`User.substitute`) : délégation d’autorité + alertes (sans période d’absence)

**Cas pilote :** MINTP Cameroun (DAG / DIER / DRTP) ; timezone / pays configurables via `tenant_settings`.

---

## 4. Écarts — description vs implémentation

Légende : ✅ aligné · ⚠️ partiel · ❌ absent / hors périmètre volontaire

### 4.1 Vision, mission, positionnement

| Élément description | État | Commentaire |
|---------------------|------|-------------|
| Gestion électronique des dossiers | ✅ | Cœur du produit (`FileEntity`, cycle de vie, passages) |
| Suivi des circuits de passation | ✅ | Templates + `FilePassage` |
| Plateforme multi-secteurs Afrique | ⚠️ | Seeds **MINTP** ; **tenant lite** (`tenant_settings` : nom produit, timezone, préfixe, email) — pas encore multi-org / white-label complet |
| Remplacer papier / cahiers / Excel | ✅ | Objectif CDC et MVP couverts sur le suivi |

### 4.2 Valeur ajoutée

| Élément | État | Écart |
|---------|------|-------|
| Dématérialisation du suivi des dossiers | ✅ | Métadonnées + circuit numériques |
| Circuits configurables | ✅ | Admin templates de chaîne |
| Responsable actuel visible | ✅ | Possessionnaire / maillon actif |
| SLA jours/heures ouvrés | ✅ | `DelaiService` + calendrier |
| Alertes et escalades | ✅ | Règles par template, in-app + email |
| Suivi temps réel | ✅ | UI circuit + notifications |
| Traçabilité des transmissions | ✅ | Historique passages ; audit admin/login |
| Tableaux de bord | ⚠️ | Widgets + CSV ; pas de rapports PDF / BI avancée |
| Sécurisation accès | ✅ | JWT, RBAC, scope org |

### 4.3 Fonctionnalités clés

| Fonctionnalité | État | Écart |
|----------------|------|-------|
| Cycle de vie dossier | ✅ | DRAFT → IN_PROGRESS → CLOSED / ARCHIVED / CANCELLED… |
| Types + numérotation | ✅ | `FileType`, séquences org |
| Modèles de chaîne | ✅ | CRUD + étapes optionnelles / parallèles (même `stepOrder`) |
| Transmission / retour / suspend / réaffecter | ✅ | `PassageService` |
| Pièces jointes | ⚠️ | Upload/download local ; pas MinIO, pas versioning (volontairement hors GED documentaire) |
| Notifications in-app + email | ✅ | SMS non livré (Phase 2 CDC) |
| Digest retards | ✅ | Email digest paramétrable |
| Dashboard + exports | ⚠️ | CSV oui ; PDF non |
| Organigramme / users / RBAC / audit | ✅ | Complet pour le MVP ; suppléant en édition utilisateur |
| API REST | ✅ | API métier interne ; pas de connecteurs externes (LDAP, e-gov…) |
| Recherche | ✅ | **Déjà fait** : filtres liste `/files` (texte, type, statut, priorité) + recherche globale header (`HeaderSearch`) ; API sur **objet**, **numéro**, **expéditeur** |
| Calendrier ouvrés | ✅ | CRUD admin `/admin/settings` + seed fériés ; utilisé par `DelaiService` |
| Paramètres organisation | ✅ | Onglet Paramètres + `GET /api/public/tenant-config` |
| Archivage | ⚠️ | Statut `ARCHIVED` ; pas de politique de rétention / records management |

### 4.4 Hors périmètre volontaire (ne pas promettre)

| Sujet | Décision |
|-------|----------|
| Moteur BPM générique | **Refusé** — remplacé officiellement par templates + étapes parallèles ([décision](./DECISION-TEMPLATES-PARALLELES-VS-BPM.md)) |
| GED documentaire complète | **Hors cœur** — PJ au service du dossier seulement |
| Signature électronique | Non implémentée |
| SMS | Stub / Phase 2 |
| LDAP / SSO / 2FA | Non implémentés (Should CDC Phase 2) |
| Santé, banques, RH métier dédiés | Pas de modules sectoriels ; réutilisation via nouveaux types + templates |

### 4.5 Écarts techniques connus (CDC Must / Should partiels)

| Item CDC / produit | État | Note |
|--------------------|------|------|
| Export PDF dashboard / fiche circulation | ❌ | Dashboard refuse PDF ; pas de lib PDF |
| Stockage objet MinIO / S3 | ❌ | Commentaire config seulement ; disque `./data/attachments` |
| Canal SMS | ❌ | `AlertChannel` = IN_APP + EMAIL |
| Intérim / suppléant (`User.substitute`) | ✅ | Délégation active tant que `substitute_id` est renseigné (pas de constat d’absence ni période). API + UI édition (org → utilisateur filtré) ; autorité passation, listes / dashboard, accès dossier, alertes via `SubstituteService` |
| Hold externe dossier (`externalHold*`) | ✅ | Suspend / resume CHN-PASS ; `ON_HOLD` + ALR-07 ; UI circuit |
| Saut de maillon (CHN-10) | ⚠️ | Should CDC ; à confirmer produit |
| Copie informée CC (CHN-09) | ✅ | Table `file_passage_cc` ; notifs IN_APP+EMAIL à l'arrivée (responsable `PASSAGE_ARRIVAL` + CC `PASSAGE_CC`) ; UI **liaison du circuit** : CC optionnel par maillon |
| Notification arrivée maillon | ✅ | **Déjà fait** : à l'init / transmission / retour / réaffectation — responsable (+ suppléant) et CC |
| Recherche full-text DOS-08 | ✅ | **Déjà fait** : `search` API + filtres `/files` + header — **objet**, **numéro**, **expéditeur** |
| Header search front (⌘K) | ✅ | **Déjà fait** (branchée) : `HeaderSearch` → `searchFiles` ; debounce 300 ms, preview 8, Entrée → `/files?search=`, clic → fiche |
| Filtres dossiers (liste `/files`) | ✅ | **Déjà fait** (branchés) : recherche texte + type + statut + priorité → `GET /api/files` |
| Calendrier métier admin | ✅ | CRUD + RBAC `BUSINESS_CALENDAR:*` |
| Tenant settings (white-label lite) | ✅ | Table `tenant_settings` ; admin + config publique |
| Rôles destinataires digest | ✅ | CRUD admin ; seed `DIRECTOR` |
| « Remember me » login | ✅ | **Déjà fait** : coché → `localStorage` (+ email mémorisé) ; décoché → `sessionStorage` (fin à la fermeture d’onglet) |

---

## 5. TODO

Priorisation indicative : **P0** = pilote / crédibilité · **P1** = post-pilote proche · **P2** = nice-to-have / Phase 2 CDC.

### 5.1 Produit & communication — P0

- [ ] Adopter cette description comme source officielle (site, pitch, README)
- [ ] Remplacer toute promesse « BPM configurable » / « GED documentaire » par **chaînes de passation** + **gestion électronique des dossiers**
- [ ] Aligner branding front (FluxPro / ChaîneFlux / MINTP) sur un seul positionnement

### 5.2 Compléter le cœur « Gestion électronique des dossiers » — P0 / P1

- [x] **P0** — Filtres dossiers (`/files`) : **déjà fait** — texte + type + statut + priorité → `searchFiles` / `GET /api/files`
- [x] **P0** — Recherche globale header : **déjà fait** (branchée) — objet, numéro, expéditeur (`HeaderSearch` → `/files?search=`)
- [x] **P1** — Command bar header : **déjà fait** — debounce + preview résultats + navigation fiche
- [ ] **P0** — Finaliser / documenter l’archivage (qui peut archiver, effets sur alertes et visibilité)
- [x] **P1** — Tenant lite / paramètres organisation (timezone, préfixe, branding email)
- [x] **P1** — Calendrier ouvrés admin + rôles digest alertes (Paramètres)
- [x] **P1** — Hold externe « en attente pièce » (ALR-07 / champs `externalHold*`) : API + UI + pause alertes
- [x] **P1** — Intérim / suppléant (ORG-04) : brancher `substitute` sur assignation et alertes
  - Délégation (titulaire reste `responsibleUser`) ; suppléant actif peut transmettre / retourner / suspendre / reprendre / réassigner / commenter
  - Visibilité dossiers + widget « Mon activité » + `assertCanAccessFile`
  - Alertes : destinataire effectif = suppléant actif (`SubstituteService.effectiveRecipient`)
  - UI : champ uniquement en **modification** (select organisation + select utilisateur filtré) ; pas à la création
  - Limite connue : pas de flag / période d’absence — retirer le suppléant pour désactiver l’intérim
- [x] **P2** — Copie informée CC (CHN-09) + notif d'arrivée responsable : **déjà fait**
- [ ] **P1** — Saut de maillon exceptionnel (CHN-10) si validé métier

### 5.3 Chaînes de passation (pas de BPM) — P1

- [x] **P1** — Documenter officiellement que les **templates + stages parallèles** remplacent un moteur BPM → [DECISION-TEMPLATES-PARALLELES-VS-BPM.md](./DECISION-TEMPLATES-PARALLELES-VS-BPM.md)
- [x] **P1** — Guide admin : créer un type de dossier + template + règles d’alerte (onboarding hors MINTP) → [GUIDE-ADMIN-ONBOARDING-TYPE-TEMPLATE-ALERTES.md](./GUIDE-ADMIN-ONBOARDING-TYPE-TEMPLATE-ALERTES.md)
- [ ] Revue UX circuit (lisibilité multi-maillons parallèles, retours)

### 5.4 Alertes & notifications — P1 / P2

- [x] **P1** — Vrais gabarits email (`emailTemplateCode`) au lieu du corps construit en dur
- [ ] **P1** — Journal audit `ALERT_SENT` si exigé SPEC-AUD
- [ ] **P2** — Canal SMS (opérateur local)
- [ ] **P2** — Préférences de notification utilisateur (UI)

### 5.5 Dashboard & reporting — P1

- [ ] Export PDF (synthèses direction / SG) — SPEC-DSH
- [ ] Fiche de circulation PDF du dossier (si demandé métier)
- [ ] Rapport mensuel automatisé (job) — Could CDC

### 5.6 Infrastructure & intégrations — P1 / P2

- [ ] **P1** — Stockage pièces jointes MinIO/S3 (fiabilité, backup) — sans basculer en GED documentaire
- [ ] **P2** — LDAP / AD MINTP
- [ ] **P2** — 2FA
- [ ] **P2** — Connecteurs externes (messagerie, GED tierce) si besoin d’interop

### 5.7 Qualité & dette — P0 / P1

- [ ] **P0** — Mettre à jour les en-têtes SPEC (`non implémenté`) obsolètes vs code réel
- [ ] **P1** — Nettoyer jobs dépréciés (`AlertSchedulerJob` / `AlertDigestJob` shells) si encore présents
- [ ] **P1** — Tests de non-régression passation parallèle + alertes avec horloge système
- [x] **P2** — « Remember me » login : **déjà fait** — `localStorage` vs `sessionStorage` + email prérempli

### 5.8 Hors scope (ne pas traiter comme TODO cœur)

- [ ] ~~Moteur BPM (Camunda / Flowable / BPMN)~~ — **refusé** tant que les templates suffisent
- [ ] ~~GED documentaire (versioning, plan de classement, full records)~~ — **hors cœur** ; PJ au service du dossier uniquement
- [ ] ~~Modules métier santé / banque / RH dédiés~~ — nouveaux **types + templates**, pas de verticales séparées

---

## 6. Synthèse

| Question | Réponse |
|----------|---------|
| FluxPro est-il une GED ? | **Oui** — Gestion Électronique des **Dossiers** |
| Faut-il un BPM ? | **Non** — templates + étapes parallèles (join AND) ; [décision officielle](./DECISION-TEMPLATES-PARALLELES-VS-BPM.md) |
| Écart principal description ancienne vs code | Ancienne description = plateforme GED/BPM multi-secteurs ; code = passation + SLA MINTP |
| Priorité prochaine | Archivage clair, reporting PDF, MinIO pour PJ |

---

## 7. Historique

| Date | Auteur | Changement |
|------|--------|------------|
| 2026-07-16 | Équipe produit / cadrage | Création : description alignée, écarts, TODO ; GED = dossiers ; BPM écarté |
| 2026-07-22 | Équipe produit / tech | Recherche header P0/P1 ✅ ; tenant lite ✅ ; calendrier + digest roles ✅ ; hold externe ✅ ; priorités recalées |
| 2026-07-22 | Équipe produit / tech | ORG-04 intérim/suppléant ✅ : `SubstituteService`, autorité + listes + alertes ; UI édition org→user ; pas de constat d’absence |
| 2026-07-22 | Équipe produit / tech | Recherche globale P0 clarifiée **déjà fait** : objet + numéro + expéditeur (API + header + `/files`) |
| 2026-07-22 | Équipe produit / tech | Filtres dossiers + header search explicitement **déjà faits / branchés** (plus d’écart « non branché ») |
| 2026-07-22 | Équipe produit / tech | « Remember me » login ✅ : session persistante (`localStorage`) vs session d’onglet (`sessionStorage`) |
| 2026-07-22 | Équipe produit / tech | CHN-09 CC + notifs d'arrivée maillon ✅ (`PASSAGE_ARRIVAL` / `PASSAGE_CC`, `file_passage_cc`) |
| 2026-07-22 | Équipe produit / tech | Décision officielle templates + étapes parallèles vs BPM ✅ → [DECISION-TEMPLATES-PARALLELES-VS-BPM.md](./DECISION-TEMPLATES-PARALLELES-VS-BPM.md) |
| 2026-07-22 | Équipe produit / tech | Guide admin onboarding hors MINTP ✅ : type → template → alertes → [GUIDE-ADMIN-ONBOARDING-TYPE-TEMPLATE-ALERTES.md](./GUIDE-ADMIN-ONBOARDING-TYPE-TEMPLATE-ALERTES.md) |
| 2026-07-23 | Équipe produit / tech | Gabarits email HTML (`emailTemplateCode` + Thymeleaf) ✅ — `templates/email/*` |
