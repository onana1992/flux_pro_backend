# Rapport d'implémentation — RBAC dynamique (utilisateurs / rôles / permissions)

**Projet :** FluxPro  
**Date :** 1er juillet 2026  
**Référence :** modèle core_banking_backend + core_bankink_dashboard  
**Statut :** Implémenté — **script SQL à exécuter manuellement**

---

## 1. Objectif

Aligner FluxPro sur le modèle RBAC du projet core banking :

```
User ──(user_roles)──► Role ──(role_permissions)──► Permission
```

- Permissions nommées `RESOURCE:ACTION` (ex. `USERS:READ`)
- JWT et profil `/api/users/me` exposent `roles[]` et `permissions[]`
- Garde API via `@RequiresPermission` (AOP, comme core banking)
- UI admin : rôles, permissions, navigation filtrée par permission

---

## 2. Prérequis déploiement

Exécuter **manuellement** sur MySQL avant redémarrage :

```
docs/sql/2026-07-02_rbac_roles_permissions.sql
```

Au démarrage, `RbacDataInitializer` :

1. Crée les permissions FluxPro
2. Crée les rôles système (alignés sur `UserRole`)
3. Remplit `user_roles` pour les utilisateurs existants

---

## 3. Backend — livrables

### 3.1 Schéma

| Table | Rôle |
|-------|------|
| `roles` | Rôles (système + personnalisés) |
| `permissions` | Permissions `RESOURCE:ACTION` |
| `user_roles` | Affectation utilisateur → rôles |
| `role_permissions` | Affectation rôle → permissions |

Le champ `users.role` (enum) est **conservé** pour le périmètre organisationnel (`OrganizationScopeService`).

### 3.2 API

| Méthode | Route | Permission |
|---------|-------|------------|
| GET | `/api/admin/roles` | `ROLES:READ` |
| GET | `/api/admin/roles/{id}` | `ROLES:READ` |
| POST | `/api/admin/roles` | `ROLES:CREATE` |
| PUT | `/api/admin/roles/{id}` | `ROLES:UPDATE` |
| DELETE | `/api/admin/roles/{id}` | `ROLES:DELETE` |
| POST | `/api/admin/roles/{id}/permissions` | `ROLES:UPDATE` |
| DELETE | `/api/admin/roles/{id}/permissions/{permissionId}` | `ROLES:UPDATE` |
| GET | `/api/admin/permissions` | `PERMISSIONS:READ` |
| POST/PUT/DELETE | `/api/admin/permissions` | `PERMISSIONS:*` |
| POST | `/api/users/{id}/roles` | `USERS:UPDATE` |
| DELETE | `/api/users/{id}/roles/{roleId}` | `USERS:UPDATE` |

Les endpoints users/orgs/types existants utilisent désormais `@RequiresPermission` au lieu de `@PreAuthorize(hasRole(...))`.

### 3.3 Sécurité

| Composant | Fichier |
|-----------|---------|
| Annotation | `security/RequiresPermission.java` |
| Aspect AOP | `security/RbacValidationAspect.java` |
| Résolution authorities | `security/RbacAuthorityService.java` |
| Constantes | `security/RbacPermissions.java` |
| Seed | `config/RbacDataInitializer.java` |

JWT : claims `roles` et `permissions` ajoutés.

### 3.4 Matrice initiale (rôles système)

| Rôle | Permissions clés |
|------|------------------|
| `SUPER_ADMIN` | Toutes |
| `BUSINESS_ADMIN` | USERS*, ORGANIZATIONS*, ORGANIZATION_TYPES*, ROLES:READ, PERMISSIONS:READ |
| `DIRECTOR`, `SERVICE_HEAD`, `REGIONAL_DIRECTOR` | USERS:READ, ORGANIZATIONS:READ |
| `AGENT`, `SUPPORT` | ORGANIZATIONS:READ |
| `READER` | ORGANIZATIONS:READ, USERS:READ |

---

## 4. Frontend — livrables

| Élément | Chemin |
|---------|--------|
| Types `Role`, `Permission` | `src/lib/types.ts` |
| API clients | `src/lib/api.ts` |
| Helpers `hasPermission`, etc. | `src/lib/auth-storage.ts` |
| Page rôles | `src/app/admin/roles/page.tsx` |
| Page permissions | `src/app/admin/permissions/page.tsx` |
| Navigation | `AppShell` — entrées Rôles / Permissions |
| Garde routes | `RequireAuth` — prop `permission` |

Le profil utilisateur inclut `roles[]` et `permissions[]` après reconnexion.

---

## 5. Tests

- `mvn compile test -Dtest=OrganizationScopeServiceTest` : OK
- Tests d'intégration RBAC : à ajouter (403 sans permission, seed SUPER_ADMIN)

---

## 6. Écarts / suite

| Sujet | Statut |
|-------|--------|
| Page détail rôle (`/admin/roles/[id]`) | À compléter (édition permissions) |
| Affectation rôles sur fiche utilisateur | API prête, UI à enrichir |
| Révocation JWT après changement permissions | Re-login requis (comme core banking) |
| ORBAC / périmètre dossiers | Hors scope — RBAC classique conservé |

---

## 7. Vérification manuelle

1. Exécuter le script SQL
2. Redémarrer le backend
3. Se connecter en `SUPER_ADMIN`
4. Vérifier `/api/users/me` → `permissions` non vide
5. Ouvrir `/admin/roles` et `/admin/permissions`
6. Tester 403 pour un agent sans `ROLES:READ`
