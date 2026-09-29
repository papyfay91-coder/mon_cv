# Wiri-Wiri Shop

SaaS e-commerce **100 % visuel et vocal (wolof)** pour les commerçants du secteur informel au Sénégal.
Le vendeur crée une fiche produit en quelques secondes : 📸 une photo + 🎙️ une phrase en wolof → l'IA pré-remplit
nom, prix et taille → le vendeur vérifie et publie. Ses clients commandent sur une vitrine web légère et paient
directement le vendeur par Wave ou Orange Money.

```
wiri-wiri-shop/
├── backend/        API Spring Boot 3 (Java 17) · PostgreSQL · Flyway
├── vitrine-web/    Vitrine client Next.js 15 (rendu serveur + cache 60 s, ~105 ko de JS)
├── app-vendeur/    Application vendeur React Native (Expo SDK 57)
├── docker-compose.yml, Caddyfile   Déploiement (PostgreSQL + API + vitrine + HTTPS TLS 1.3)
```

## Démarrage rapide

### Backend

```bash
cd backend
export DB_URL=jdbc:postgresql://localhost:5432/wiriwiri DB_USER=wiriwiri DB_PASSWORD=...
export JWT_SECRET=$(openssl rand -base64 32) AES_KEY=$(openssl rand -base64 32) OTP_PEPPER=$(openssl rand -base64 32)
export OPENAI_API_KEY=sk-...        # facultatif : sans clé, l'analyse vocale répond 503
mvn spring-boot:run                  # le schéma est créé par Flyway
mvn test                             # 22 tests (H2 en mode PostgreSQL)
```

En développement, les SMS OTP sont écrits dans les journaux (`EnvoiSmsConsole`). **En production**, il faut fournir
un bean `EnvoiSms` branché sur un opérateur SMS et définir `wiriwiri.sms.fournisseur` à une autre valeur que `console`.

### Vitrine web

```bash
cd vitrine-web && npm install
NEXT_PUBLIC_API_URL=http://localhost:8080 npm run dev     # http://localhost:3000/boutique/<uuid>
```

### Application vendeur

```bash
cd app-vendeur && npm install
EXPO_PUBLIC_API_URL=http://<ip-du-pc>:8080 npx expo start
```

### Production

```bash
cp .env.example .env   # renseigner domaines et secrets
docker compose up -d --build
```

## API

| Méthode | Route | Accès | Rôle |
|---|---|---|---|
| POST | `/api/auth/otp` | public | Envoie un code OTP par SMS (réponse identique que le numéro existe ou non) |
| POST | `/api/auth/verification` | public | Vérifie le code → JWT. À la 1ʳᵉ connexion, `nomVendeur` est requis (`NOM_REQUIS`) |
| GET | `/api/vitrine/boutiques/{id}?page=` | public | Vitrine : produits actifs, paginés par 24 |
| GET | `/api/vitrine/produits/{id}` | public | Fiche produit |
| POST | `/api/vitrine/commandes` | public | Commande client → liens WhatsApp / Wave / Orange Money |
| GET | `/fichiers/{uuid}.{jpg,png,webp}` | public | Images (cache immuable 1 an) |
| GET · PUT | `/api/vendeur/boutique` | JWT | Profil, nom, lien de paiement Wave |
| POST · DELETE | `/api/vendeur/consentement` | JWT | Donner / retirer le consentement vocal |
| DELETE | `/api/vendeur/compte` | JWT | Droit à l'oubli : purge définitive |
| POST | `/api/vendeur/produits/analyse-vocale` | JWT + consentement | Note vocale `.wav`/`.m4a` → fiche pré-remplie |
| POST | `/api/vendeur/produits` | JWT | Publication (multipart : `image`, `nomProduit`, `prix`, `taille`) |
| GET · PATCH · DELETE | `/api/vendeur/produits[/{id}]` | JWT | Gestion des produits |
| GET | `/api/vendeur/commandes` | JWT | Commandes reçues (numéros déchiffrés) |

Les erreurs suivent le format `ProblemDetail` (RFC 9457) avec un champ `code` stable (`CODE_INVALIDE`,
`CONSENTEMENT_REQUIS`, `IA_INDISPONIBLE`, `TROP_DE_REQUETES`…). Aucune trace technique n'est renvoyée.

## Correspondance avec le cahier des charges

| Exigence | Mise en œuvre |
|---|---|
| Couches Controller / Service / Repository / Entity / DTO | Paquets `controller`, `service`, `repository`, `entity`, `dto` ; clients IA isolés dans `ia` |
| JWT stateless HMAC-SHA256 | `JwtService` (jjwt, HS256, 30 min, sujet = UUID boutique, sans donnée personnelle) |
| Connexion OTP SMS sans mot de passe | `AuthService` : 6 chiffres `SecureRandom`, 5 min, condensat HMAC poivré, comparaison en temps constant, 5 essais max, 60 s entre deux envois |
| `SecurityFilterChain` restrictive | `SecurityConfig` : `denyAll` par défaut, seules les routes du tableau ci-dessus sont publiques |
| TLS 1.3 | `Caddyfile` (`protocols tls1.3`) ou profil Spring `tls` (`enabled-protocols: TLSv1.3`) ; HSTS activé |
| AES-256 au repos | `ChiffrementAes` (AES-256-**GCM**, IV aléatoire) via le convertisseur JPA sur `commandes.telephone_client` |
| Injections SQL | Spring Data JPA / JPQL paramétré uniquement |
| XSS / CSRF | CSRF désactivé (stateless) ; nettoyage des saisies côté React **et** côté API ; CSP stricte sur l'API et la vitrine ; liens de paiement filtrés par liste blanche |
| Bucket4j | `LimitationDebitFilter` : 120 req/min/IP globalement, 5 req/min/IP sur `/api/auth/**` |
| Consentement explicite (CDP) | Écran `Consentement` avant toute demande d'accès au micro ; `date_consentement` horodatée ; l'API refuse l'analyse vocale sans consentement |
| Droit à l'oubli | `DELETE /api/vendeur/compte` : commandes, produits, OTP, boutique, puis fichiers images après validation de la transaction |
| Finalité des audios | L'audio reste **en mémoire** le temps de la requête (`file-size-threshold`), il n'est jamais écrit sur disque ni en base |
| Non-détention de fonds (BCEAO) | Aucun flux financier : liens `wa.me`, lien Wave du vendeur avec montant, USSD Orange Money `#144#` |
| Schéma PostgreSQL | `V1__schema_initial.sql` : UUID générés côté serveur, `CHECK (prix > 0)`, `ON DELETE CASCADE`, index `(boutique_id, actif)` |
| Voice-to-Store | `IngestionVocaleService` → `WhisperTranscripteur` (`language=wo`) → `LlmExtracteur` (prompt du §6.1, mode JSON) → validation stricte des types |

## Écarts assumés par rapport au cahier des charges

- **Commande publique en POST** : le cahier des charges ne rend publiques que les requêtes `GET`, mais un client
  sans compte doit pouvoir commander. Seule `POST /api/vitrine/commandes` est ouverte, soumise à la limitation de débit.
- **`commandes.telephone_client` en `VARCHAR(255)`** : le chiffré AES-GCM en Base64 dépasse 20 caractères.
- **Ajouts au schéma** : `produits.taille` (extraite par l'IA), `boutiques.date_consentement` (preuve du
  consentement), `boutiques.lien_wave`, et une table `codes_otp`.
- **`produits.audio_url` reste toujours `NULL`** : parmi les deux options du §4.1, c'est la suppression après
  traitement qui a été retenue.

## Points d'attention avant la mise en production

1. **Le wolof et Whisper** : le wolof ne fait pas partie des langues officiellement prises en charge par Whisper.
   L'API peut refuser `language=wo` ; le client relance alors automatiquement en détection automatique. La qualité
   de transcription est à mesurer sur de vrais enregistrements de vendeurs. La validation finale par le vendeur
   reste le garde-fou.
2. **Prix en « dërëm »** : à Dakar, les prix se disent souvent en dërëm (1 dërëm = 5 FCFA : « ñaari junni » peut
   signifier 10 000 FCFA). Le prompt du §6.1, repris tel quel, ne le précise pas au LLM. Il faudrait ajouter
   cette règle au prompt (`ExtracteurLlm.PROMPT_SYSTEME`) et la tester.
3. **Transfert de données hors du Sénégal (CDP)** : la voix du vendeur est envoyée à OpenAI (hors Sénégal). La loi
   n° 2008-12 encadre ces transferts : il faut vérifier les formalités auprès de la CDP. Le texte de consentement le
   mentionne déjà.
4. **SMS** : il faut brancher un vrai fournisseur (par exemple l'API SMS d'Orange) à la place de l'implémentation
   console.
5. **Montée en charge** : les compteurs Bucket4j sont en mémoire, donc propres à chaque instance. Avec plusieurs
   instances, il faut passer par Bucket4j avec Redis. Le stockage des images est local (volume Docker) ;
   `StockageService` peut être remplacé par un stockage objet.
6. **Numéro du vendeur en clair** : il sert d'identifiant de connexion (index unique) et de destinataire WhatsApp.
   Pour le chiffrer, il faudrait un index aveugle (HMAC), par exemple.
7. **Métadonnées EXIF** : l'application recadre la photo (ré-encodage), mais le serveur ne supprime pas lui-même
   les métadonnées des images reçues.
