# Guide d’implémentation — Stockage pièces jointes AWS S3

**Module concerné :** DOS (pièces jointes)  
**État actuel :** disque local via `LocalAttachmentStorageService` (`./data/attachments`)  
**Cible :** bucket AWS S3 (compatible API objet ; MinIO possible plus tard avec le même client)  
**Hors périmètre :** GED documentaire, versioning de documents, OCR, recherche plein texte dans les fichiers

Ce guide décrit **comment** basculer FluxPro vers S3 sans changer le contrat API front (`POST/GET/DELETE …/attachments`).

Références produit :

- [SPEC-DOS §9 — Stockage pièces jointes (MinIO)](./SPEC-DOS.md)
- [DESCRIPTION-PRODUIT-ECARTS-TODO — P1 MinIO/S3](./DESCRIPTION-PRODUIT-ECARTS-TODO.md)

---

## 1. Principe

| Couche | Rôle |
|--------|------|
| **PostgreSQL / Neon** | Métadonnées uniquement (`file_attachments`) |
| **AWS S3** | Octets du fichier |
| **API backend** | Validation métier + contrôle d’accès + écriture/lecture objet |

Le schéma de clé objet **reste identique** à l’existant (et à SPEC-DOS) :

```text
{orgCode}/{year}/{fileId}/{uuid}_{safeFilename}
```

Exemple : `SG/2026/a1b2c3d4-…/7f3e…_demande.pdf`

| Champ DB | Valeur locale actuelle | Valeur cible S3 |
|----------|------------------------|-----------------|
| `storage_bucket` | `"local"` | nom du bucket AWS (ex. `fluxpro-attachments-prod`) |
| `storage_key` | chemin relatif sous `./data/attachments` | clé objet S3 (même format) |

---

## 2. État du code aujourd’hui (à connaître avant de coder)

### 2.1 Points d’entrée

| Classe | Usage |
|--------|--------|
| `LocalAttachmentStorageService` | `store` / `loadAsResource` / `delete` |
| `FileAttachmentService` | Upload / download / delete dossiers internes |
| `PortalSubmissionService` | Upload PJ portail (réutilise le même storage, **duplique** la logique d’écriture métadonnées) |
| `FileController` | Stream download via `ResponseEntity<Resource>` |
| Front `downloadFileAttachment` | Fetch blob via API backend (Bearer JWT) — **pas** d’URL directe disque |

### 2.2 Config actuelle

```properties
fluxpro.attachments.storage-path=./data/attachments
spring.servlet.multipart.max-file-size=21MB
spring.servlet.multipart.max-request-size=22MB
```

Limites métier inchangées : **~20 Mo**, MIME PDF / DOCX / XLSX / JPEG / PNG (`FileAttachmentService`).

### 2.3 Problème de conception à corriger en même temps

Aujourd’hui les services injectent **directement** `LocalAttachmentStorageService`.  
Pour S3 (et garder le local en dev), introduire une **interface** :

```text
AttachmentStorageService
  ├── LocalAttachmentStorageService   (dev / fallback)
  └── S3AttachmentStorageService      (staging / prod)
```

Sélection via propriété, ex. `fluxpro.attachments.storage=local|s3`.

---

## 3. Décisions d’architecture (à figer avant sprint)

### 3.1 Mode de téléchargement (recommandation)

| Option | Description | Avantage | Inconvénient |
|--------|-------------|----------|--------------|
| **A — Proxy backend (recommandé phase 1)** | Garder `GET …/attachments/{aid}/download` ; le backend streame depuis S3 | Aucun changement front ; ACL inchangées (JWT + RBAC) | Charge réseau / CPU sur l’API |
| **B — URL pré-signée** | API renvoie une URL S3 temporaire (ex. 15 min) ; le front télécharge directement | Moins de charge API | Changement front + risque fuite d’URL ; CORS S3 à configurer |

**Recommandation FluxPro :** démarrer avec **option A** (contrat API actuel), ajouter l’option B ensuite si volume / perf l’exige (aligné SPEC-DOS §9.3).

### 3.2 Région et bucket

| Environnement | Bucket suggéré | Région |
|---------------|----------------|--------|
| Dev (optionnel) | `fluxpro-attachments-dev` | ex. `us-east-2` (proche Neon actuel) ou région MINTP |
| Staging | `fluxpro-attachments-staging` | idem |
| Prod | `fluxpro-attachments-prod` | région officielle MINTP / AWS |

Un bucket **privé** (Block Public Access ON). Pas d’accès anonyme.

### 3.3 Authentification AWS

| Mode | Quand |
|------|--------|
| **Access key + secret** (IAM user) | Dev local, CI |
| **IAM Role** (EC2 / ECS / EKS / Elastic Beanstalk) | Prod — **préféré** (pas de clés dans les env) |

Politique IAM minimale (exemple) :

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::fluxpro-attachments-prod/*"
    },
    {
      "Effect": "Allow",
      "Action": ["s3:ListBucket"],
      "Resource": "arn:aws:s3:::fluxpro-attachments-prod"
    }
  ]
}
```

---

## 4. Prérequis AWS (ops)

1. Créer le(s) bucket(s) S3.
2. Activer **Block Public Access**.
3. (Recommandé) activer **versioning S3** uniquement pour récupération d’incident ops — **ne pas** exposer de versions métier dans l’UI (hors GED).
4. (Recommandé) **chiffrement** SSE-S3 ou SSE-KMS.
5. (Recommandé) lifecycle : transition Glacier / suppression des objets orphelins si politique définie plus tard.
6. Créer utilisateur IAM ou rôle avec la policy ci-dessus.
7. Noter : `region`, `bucket`, credentials (ou rôle).

CORS : **non nécessaire** pour l’option A (proxy). Nécessaire seulement pour l’option B (téléchargement navigateur → S3).

---

## 5. Plan d’implémentation code (backend)

### Étape 1 — Interface de stockage

Créer par exemple :

```java
public interface AttachmentStorageService {
    String store(Organization organization, UUID fileId, String originalFilename,
                 InputStream content, long contentLength, String contentType) throws IOException;

    Resource loadAsResource(String storageBucket, String storageKey) throws IOException;

    void delete(String storageBucket, String storageKey) throws IOException;

    /** Identifiant logique du backend de stockage (ex. "local", "s3"). */
    String providerId();
}
```

Notes :

- Ajouter `contentLength` + `contentType` : **requis** pour `PutObject` S3 proprement.
- Passer `storageBucket` au load/delete : les lignes historiques auront `local`, les nouvelles le nom du bucket AWS — permet une **période de transition**.

### Étape 2 — Adapter le local

Faire implémenter `AttachmentStorageService` par `LocalAttachmentStorageService` :

- `providerId()` → `"local"`
- `storage_bucket` en DB reste `"local"` (ou ignorer le bucket et utiliser uniquement `storage-path`)
- Signature `store` enrichie : ignorer length/type si non utilisés

### Étape 3 — Implémenter S3

Dépendance Maven (AWS SDK v2) :

```xml
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>s3</artifactId>
</dependency>
```

Avec Spring Boot 4 / BOM : préférer le BOM AWS si disponible, sinon version SDK pinée explicitement.

`S3AttachmentStorageService` :

1. Construire `S3Client` (région + credentials default chain).
2. `store` → `PutObjectRequest` (bucket config, key inchangée, `contentType`).
3. `loadAsResource` → `GetObject` → wrapper `InputStreamResource` (ou fichier temp si besoin de `Resource` réutilisable).
4. `delete` → `DeleteObjectRequest`.
5. `providerId()` → `"s3"` ; bucket = `fluxpro.attachments.s3.bucket`.

Gestion d’erreurs : mapper `NoSuchKeyException` → `FILE_ATTACHMENT_STORAGE_MISSING` (déjà utilisé par `FileAttachmentService`).

### Étape 4 — Configuration conditionnelle

```properties
# local | s3
fluxpro.attachments.storage=${FLUXPRO_ATTACHMENTS_STORAGE:local}

fluxpro.attachments.storage-path=${FLUXPRO_ATTACHMENTS_PATH:./data/attachments}

fluxpro.attachments.s3.bucket=${FLUXPRO_S3_BUCKET:}
fluxpro.attachments.s3.region=${FLUXPRO_S3_REGION:us-east-2}
# Optionnel hors IAM Role (dev) :
# AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY (SDK default chain)
# Ne pas committer de clés dans application.properties
```

Beans Spring :

```java
@Bean
@ConditionalOnProperty(name = "fluxpro.attachments.storage", havingValue = "local", matchIfMissing = true)
AttachmentStorageService localAttachmentStorageService(...) { ... }

@Bean
@ConditionalOnProperty(name = "fluxpro.attachments.storage", havingValue = "s3")
AttachmentStorageService s3AttachmentStorageService(...) { ... }
```

### Étape 5 — Brancher les services métier

| Fichier | Changement |
|---------|------------|
| `FileAttachmentService` | Injecter `AttachmentStorageService` ; `setStorageBucket` = bucket réel (config S3 ou `"local"`) ; `load`/`delete` avec bucket de la ligne DB |
| `PortalSubmissionService` | Même injection ; **idéalement** déléguer l’upload à `FileAttachmentService` pour supprimer la duplication |
| Tests | Mock de l’interface ; test unitaire S3 avec LocalStack (optionnel) ou mock `S3Client` |

### Étape 6 — Download (option A)

Conserver `FileController.downloadAttachment` :

```text
RBAC FILES:READ → GetObject S3 → stream ResponseEntity<Resource>
```

Attention : pour de gros fichiers, préférer un stream direct (`InputStreamResource`) **sans** charger tout en mémoire.

### Étape 7 — (Optionnel) URL pré-signée

Si besoin plus tard :

1. Ajouter `presignGetUrl(bucket, key, Duration.ofMinutes(15))` via `S3Presigner`.
2. Nouvel endpoint ex. `GET …/attachments/{aid}/download-url` → `{ "url": "...", "expiresAt": "..." }`.
3. Adapter le front pour `window.location` / `fetch` sur l’URL signée.
4. Configurer CORS bucket.

Ne pas casser l’endpoint stream existant tant que le front n’est pas migré.

---

## 6. Migration des fichiers déjà stockés en local

Les métadonnées Neon pointent déjà vers `storage_bucket = 'local'` + `storage_key`.

### Procédure recommandée

1. Déployer le code dual-mode (`local` + `s3`) **sans** couper le local.
2. Script one-shot (Python ou Java) :
   - Lire `file_attachments` où `storage_bucket = 'local'`
   - Pour chaque ligne : lire `./data/attachments/{storage_key}`
   - `PutObject` vers le bucket cible **avec la même `storage_key`**
   - UPDATE `storage_bucket = '<bucket-aws>'`
3. Vérifier quelques downloads.
4. Basculer `FLUXPRO_ATTACHMENTS_STORAGE=s3`.
5. Archiver / sauvegarder le dossier local, puis le retirer des serveurs.

### Points d’attention

| Risque | Mitigation |
|--------|------------|
| Fichier manquant sur disque | Log + skip ; ne pas UPDATE la ligne |
| Upload concurrent pendant migration | Fenêtre de maintenance courte, ou double-écriture temporaire |
| Clés avec caractères spéciaux | Réutiliser `sanitizeFilename` déjà en place |
| Coût / bande passante | Migrer hors heures ; multipart si fichiers proches de 20 Mo (rare) |

Script suggéré (emplacement) : `flux-pro-backend/scripts/migrate_attachments_local_to_s3.py` (même esprit que `migrate_mysql_to_neon.py`).

---

## 7. Sécurité

- Bucket **privé** ; pas d’ACL public-read.
- Secrets uniquement via env / IAM Role / Secrets Manager — **jamais** commités (le mot de passe Neon partagé précédemment doit aussi être rotaté si encore exposé).
- Conservé côté app : validation MIME + taille **avant** `PutObject`.
- Path traversal : déjà géré en local (`normalize` + `startsWith`) ; en S3, la clé est construite serveur (ne jamais accepter une clé client brute).
- Antivirus / scan malware : hors MVP ; possible via S3 event → Lambda plus tard.
- Logs : ne pas logger le contenu ; logger `fileId`, `attachmentId`, `storage_key` suffit.

---

## 8. Impacts front

| Scénario | Impact |
|----------|--------|
| Option A (proxy) | **Aucun** changement front |
| Option B (presign) | Adapter `downloadFileAttachment` (+ éventuellement portail) |

Upload multipart reste inchangé (`POST` vers l’API).

---

## 9. Plan de tests

### 9.1 Dev local avec S3 réel (ou LocalStack)

1. Créer bucket de test.
2. Exporter `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `FLUXPRO_S3_BUCKET`, `FLUXPRO_S3_REGION`, `FLUXPRO_ATTACHMENTS_STORAGE=s3`.
3. Upload PJ sur brouillon → vérifier objet dans la console S3.
4. Download → fichier identique.
5. Delete (DRAFT) → objet supprimé + ligne DB absente.
6. Portail : upload pièce requise + soumission.
7. Document de réponse (IN_PROGRESS) : upload `responseDocument=true`.

### 9.2 Non-régression local

Avec `FLUXPRO_ATTACHMENTS_STORAGE=local` (défaut) : suite existante + smoke upload/download.

### 9.3 Critères d’acceptation

- [ ] Upload interne et portail écrivent dans S3
- [ ] `storage_bucket` / `storage_key` cohérents en DB
- [ ] Download RBAC inchangé
- [ ] Suppression DRAFT OK
- [ ] Dev peut encore tourner en `local` sans AWS
- [ ] Aucune clé AWS dans Git
- [ ] Fichiers historiques migrés ou documentés comme exception

---

## 10. Checklist d’implémentation (ordre suggéré)

1. [ ] Créer buckets + IAM (ops)
2. [ ] Introduire `AttachmentStorageService` + adapter local
3. [ ] Ajouter dépendance AWS SDK S3 + `S3AttachmentStorageService`
4. [ ] Propriétés `fluxpro.attachments.storage` / `s3.*` + beans conditionnels
5. [ ] Brancher `FileAttachmentService` + `PortalSubmissionService` (dédupliquer si possible)
6. [ ] Tests unitaires / smoke
7. [ ] Script migration local → S3
8. [ ] Déployer staging (`storage=s3`) + valider
9. [ ] Migrer données staging puis prod
10. [ ] Mettre à jour `DESCRIPTION-PRODUIT-ECARTS-TODO.md` (Pièces jointes → ✅ stockage S3 ; hors versioning GED)
11. [ ] Mettre à jour SPEC-DOS §9 (AWS S3 en plus / à la place de MinIO « seul »)

---

## 11. Estimation indicative

| Lot | Effort indicatif |
|-----|------------------|
| Refactor interface + S3 + config | 0,5–1 j |
| Tests + polish erreurs | 0,5 j |
| Script migration + run staging/prod | 0,5 j |
| Option B presign + CORS + front | +0,5–1 j (optionnel) |

---

## 12. Hors scope explicite (ne pas glisser dans ce chantier)

- Versioning métier / historique de révisions documentaires
- Remplacement d’une GED (Alfresco, SharePoint…)
- Partage public permanent de fichiers
- Changement des MIME / taille max (sauf décision produit séparée)

---

## 13. Commandes utiles (ops)

```bash
# Vérifier un objet
aws s3 ls s3://fluxpro-attachments-prod/SG/2026/ --recursive

# Copie manuelle d’un fichier local
aws s3 cp ./data/attachments/SG/2026/.../file.pdf s3://fluxpro-attachments-prod/SG/2026/.../file.pdf
```

Variables d’environnement backend (prod) :

```bash
export FLUXPRO_ATTACHMENTS_STORAGE=s3
export FLUXPRO_S3_BUCKET=fluxpro-attachments-prod
export FLUXPRO_S3_REGION=us-east-2
# Si pas d’IAM Role :
# export AWS_ACCESS_KEY_ID=...
# export AWS_SECRET_ACCESS_KEY=...
```

---

*Document généré pour guider l’implémentation. Aucun code S3 n’est activé tant que `fluxpro.attachments.storage` reste à `local`.*
