# Guide de configuration SMTP — ChaîneFlux / FluxPro

**Module concerné :** Alertes et escalade (ALR)  
**Composant :** `EmailService` (`JavaMailSender` / Spring Boot Starter Mail)  
**Fichier de config :** `src/main/resources/application.properties`

Ce guide décrit comment connecter le backend au serveur SMTP MINTP pour l’envoi des alertes e-mail (ALR-05) et du récapitulatif quotidien (ALR-08).

---

## 1. Principe

| Environnement | Comportement attendu |
|---------------|----------------------|
| **Dev local** | SMTP configuré ; `fluxpro.alerts.email-redirect-to` pointe vers une boîte de test → tous les e-mails y arrivent |
| **Recette / Prod** | `email-redirect-to` **vide** → e-mails envoyés aux vrais destinataires via SMTP MINTP |

Les secrets (mot de passe SMTP) **ne doivent jamais** être commités dans `application.properties`. Ils passent uniquement par des variables d’environnement (ou un coffre-fort / secrets du déploiement).

---

## 2. Propriétés Spring Boot

```properties
# --- Alertes et escalade (ALR) ---
spring.mail.host=${MINTP_SMTP_HOST:}
spring.mail.port=${MINTP_SMTP_PORT:587}
spring.mail.username=${MINTP_SMTP_USER:}
spring.mail.password=${MINTP_SMTP_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
fluxpro.alerts.from-address=${FLUXPRO_ALERTS_FROM:alertes@mintp.cm}
# Dev : redirection de tous les e-mails (laisser vide en prod)
fluxpro.alerts.email-redirect-to=${FLUXPRO_EMAIL_REDIRECT_TO:}
# URL front pour liens CTA dans les emails
fluxpro.alerts.app-base-url=${FLUXPRO_APP_BASE_URL:http://localhost:3000}
```

### Signification

| Propriété | Variable d’env. | Défaut | Rôle |
|-----------|-----------------|--------|------|
| `spring.mail.host` | `MINTP_SMTP_HOST` | *(vide)* | Nom d’hôte du serveur SMTP |
| `spring.mail.port` | `MINTP_SMTP_PORT` | `587` | Port SMTP (STARTTLS) |
| `spring.mail.username` | `MINTP_SMTP_USER` | *(vide)* | Compte SMTP |
| `spring.mail.password` | `MINTP_SMTP_PASSWORD` | *(vide)* | Mot de passe SMTP |
| `mail.smtp.auth` | — | `true` | Authentification SMTP obligatoire |
| `mail.smtp.starttls.enable` | — | `true` | Chiffrement STARTTLS (port 587) |
| `fluxpro.alerts.from-address` | `FLUXPRO_ALERTS_FROM` | `alertes@mintp.cm` | Adresse expéditeur (`From:`) |
| `fluxpro.alerts.email-redirect-to` | `FLUXPRO_EMAIL_REDIRECT_TO` | *(vide)* | Si renseigné : **tous** les e-mails (alertes + digest) partent vers cette adresse ; le destinataire prévu est indiqué en bandeau HTML |
| `fluxpro.alerts.app-base-url` | `FLUXPRO_APP_BASE_URL` | `http://localhost:3000` | Base URL du front pour les boutons « Ouvrir le dossier » (sans slash final) |

La syntaxe `${NOM_VAR:valeur_par_défaut}` lit d’abord la variable d’environnement ; si elle est absente, la valeur après `:` est utilisée.

---

## 3. Configuration par environnement

### 3.1 Développement local (Windows PowerShell)

Sans SMTP (comportement par défaut — rien à faire) :

```powershell
# Optionnel : forcer explicitement le mode « sans SMTP »
$env:MINTP_SMTP_HOST = ""
```

Avec un serveur de test (MailHog, Mailpit, etc.) :

```powershell
$env:MINTP_SMTP_HOST = "localhost"
$env:MINTP_SMTP_PORT = "1025"
$env:MINTP_SMTP_USER = ""
$env:MINTP_SMTP_PASSWORD = ""
$env:FLUXPRO_ALERTS_FROM = "dev@localhost"
```

> Sur un relay local sans auth, `auth=true` peut provoquer un refus. Dans ce cas, utiliser un outil qui accepte l’auth, ou ajuster temporairement les propriétés SMTP en profil `dev` (ne pas committer de secrets).

### 3.2 Linux / macOS (bash)

```bash
export MINTP_SMTP_HOST="smtp.mintp.cm"
export MINTP_SMTP_PORT="587"
export MINTP_SMTP_USER="compte-smtp"
export MINTP_SMTP_PASSWORD="********"
export FLUXPRO_ALERTS_FROM="alertes@mintp.cm"
```

### 3.3 Production (exemple systemd)

```ini
[Service]
Environment=MINTP_SMTP_HOST=smtp.mintp.cm
Environment=MINTP_SMTP_PORT=587
Environment=MINTP_SMTP_USER=chaine flux-smtp
Environment=MINTP_SMTP_PASSWORD=********
Environment=FLUXPRO_ALERTS_FROM=alertes@mintp.cm
```

Ou via Docker Compose :

```yaml
services:
  flux-pro-backend:
    environment:
      MINTP_SMTP_HOST: smtp.mintp.cm
      MINTP_SMTP_PORT: "587"
      MINTP_SMTP_USER: ${MINTP_SMTP_USER}
      MINTP_SMTP_PASSWORD: ${MINTP_SMTP_PASSWORD}
      FLUXPRO_ALERTS_FROM: alertes@mintp.cm
```

---

## 4. Prérequis côté MINTP / réseau

1. **Compte SMTP** fourni par la DSI / messagerie MINTP (hôte, port, identifiants).
2. **Autorisation d’envoi** depuis l’adresse `FLUXPRO_ALERTS_FROM` (SPF / alias autorisé).
3. **Ouverture réseau** : le serveur d’application doit pouvoir joindre `host:port` (souvent 587/tcp).
4. **STARTTLS** : le certificat du serveur SMTP doit être reconnu par la JVM (CA d’entreprise parfois à importer dans le truststore Java).

---

## 5. Ce qui utilise le SMTP

| Fonction | Service | Déclenchement |
|----------|---------|---------------|
| Alerte individuelle (rappel, retard, escalade…) | `EmailService.send` via `NotificationService.dispatch` | Moteur ALR (`fluxpro.alerts.scheduler.cron`) |
| Digest quotidien des retards (ALR-08) | `EmailService.sendDigest` | Cron digest (`fluxpro.alerts.digest.cron`) |

- Expéditeur : `fluxpro.alerts.from-address`
- Destinataire : e-mail de l’utilisateur (`User.email`) — s’il est vide → erreur `ALERT_RECIPIENT_NO_EMAIL`, statut `FAILED`
- Objet type : `[ChaîneFlux] <libellé type d’alerte> — <référence dossier>`

---

## 6. Comportement en cas d’échec

Un échec SMTP **ne fait pas échouer** toute la transaction du moteur d’alertes :

- canal `EMAIL` → statut `FAILED`, message d’erreur tronqué (≤ 500 car.) stocké sur l’alerte ;
- canal `IN_APP` → reste indépendant (`REQUIRES_NEW`) ;
- le digest loggue un warning et continue avec les autres destinataires.

Vérifier dans les logs :

```text
ALR: envoi EMAIL échoué pour l'alerte <uuid> : ...
ALR: digest échoué pour <email> : ...
```

---

## 7. Checklist de mise en service

- [ ] Obtenir `host`, `port`, `user`, `password` auprès de la messagerie MINTP
- [ ] Définir les 4 variables `MINTP_SMTP_*` sur l’environnement cible
- [ ] Confirmer l’alias / boîte `FLUXPRO_ALERTS_FROM` (défaut `alertes@mintp.cm`)
- [ ] Vérifier la connectivité réseau `host:587` depuis le serveur app
- [ ] Redémarrer le backend
- [ ] Déclencher une alerte de test (ou avancer l’horloge système en mode test) et vérifier :
  - statut `SENT` sur l’alerte `EMAIL`
  - réception dans la boîte du destinataire
- [ ] Contrôler le digest le matin ouvrable suivant (cron `0 30 7 * * MON-FRI`)

---

## 8. Dépannage rapide

| Symptôme | Causes probables |
|----------|------------------|
| Alertes toujours `FAILED` | `MINTP_SMTP_HOST` vide ; mauvais port ; firewall |
| `Authentication failed` | Mauvais user/password ; compte désactivé |
| `Could not convert socket to TLS` | STARTTLS non supporté ; certificat / CA manquant |
| Destinataire non reçu | Spam ; SPF/DKIM ; `From` non autorisé |
| `ALERT_RECIPIENT_NO_EMAIL` | Utilisateur sans adresse e-mail en base |

---

## 9. Références

- Spec fonctionnelle : [`SPEC-ALR.md`](./SPEC-ALR.md) §9.2 (Email), §9.3 (Digest)
- Code : `service/EmailService.java`, `service/EmailTemplateService.java`, `service/NotificationService.java`
- Gabarits HTML : `src/main/resources/templates/email/` (`alert-reminder`, `alert-overdue`, `alert-escalation`, `alert-daily-digest`, `passage-arrival`, `passage-cc`, `alert-generic`)
- Config : `src/main/resources/application.properties` (section ALR)

---

## 10. Gabarits email (`emailTemplateCode`)

Le corps des e-mails est rendu en **HTML** via Thymeleaf. Le fichier utilisé est :

`templates/email/{AlertType.emailTemplateCode}.html`

Si le code est vide ou le fichier absent → `alert-generic.html`.

| Code seed | Usage |
|-----------|--------|
| `alert-reminder` | Rappel avant échéance |
| `alert-overdue` | Dépassement |
| `alert-escalation` | Escalade |
| `alert-daily-digest` | Digest quotidien (tableau) |
| `passage-arrival` | Arrivée sur maillon |
| `passage-cc` | Copie informée |
| `alert-generic` | Repli / type admin sans gabarit |
