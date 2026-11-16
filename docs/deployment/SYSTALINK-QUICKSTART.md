# Déploiement rapide de KORA sur Systalink

Ce guide prépare KORA au déploiement depuis Git sans lancer de déploiement, créer de ressource facturable ni toucher à COLIMA. Les valeurs entre chevrons viennent du tableau de bord Systalink et ne sont pas devinées.

## 1. Capacités publiques et points à confirmer

La page [Application Web / Serverless de Systalink](https://systalink.com/fr/serverless) annonce les applications Node.js et Docker, le déploiement automatique depuis Git, les variables d’environnement et secrets, les logs/monitoring et une URL HTTPS. La page publique ne précise pas les champs de configuration du monorepo, le passage des arguments de build, le nom de la variable de port ni les chemins de health check.

La page [Base de données Systalink](https://systalink.com/fr/base-de-donnees) présente un service de base de données, les sauvegardes et le chiffrement des communications. Sa grille tarifaire publique est explicitement intitulée « MySQL » ; **la disponibilité de PostgreSQL, sa version, le format de son URL JDBC et le mode TLS doivent être confirmés dans le tableau de bord avant de créer une ressource**. Ne pas remplacer PostgreSQL par MySQL.

| Sujet | Établi par le dépôt / les pages publiques | À confirmer dans le tableau de bord |
|---|---|---|
| Application Web | Service Node.js/Docker et déploiement Git annoncés publiquement | Branche, répertoire racine, Dockerfile, build args, build/runtime env et compatibilité Next.js |
| API | Docker est annoncé pour les applications Web | Chemin du Dockerfile, port exposé/injecté, health check et URL de service interne |
| Base de données | KORA exige PostgreSQL et migre le schéma avec Flyway | PostgreSQL disponible, version, paramètres TLS, accès privé, quotas et politique de sauvegarde |
| HTTPS | URL HTTPS annoncée pour les applications | URL finale, certificat actif et comportement des proxys/ports |
| Ressources | La page Serverless affiche des offres de 512 Mo à 4 Go de RAM | Ressources attribuées au build et à chaque application ; aucune offre ni dépense n’est choisie ici |
| Restauration | Sauvegardes annoncées pour le service de base de données | Rétention, fréquence, restauration et versions précédentes disponibles dans l’interface |

Le portail [documentation Systalink](https://docs.systalink.com/) est une application Web publique ; les paramètres opérationnels cités ci-dessus n’ont pas pu y être vérifiés. Les options non documentées restent donc des vérifications de tableau de bord, pas des capacités garanties.

## 2. Architecture proposée

Créer séparément :

1. **KORA Web**, une Application Web Next.js construite depuis `frontend/`.
2. **KORA API**, une Application Web Docker construite depuis `backend/`.
3. **KORA PostgreSQL**, une base gérée uniquement si le tableau de bord confirme le moteur PostgreSQL et ses paramètres TLS.

Les Dockerfiles construisent maintenant les applications depuis le dépôt Git : le backend exécute `./gradlew :app:installDist` dans son étape de build, et le frontend exécute `npm ci` puis `npm run build`. Ils gardent les mêmes applications et le même Compose local ; aucun deuxième backend ou frontend n’est introduit.

### Monorepo : vérification nécessaire

Le dépôt contient les Dockerfiles [backend/Dockerfile](../../backend/Dockerfile) et [frontend/Dockerfile](../../frontend/Dockerfile). Le tableau de bord doit permettre soit de définir `backend/` et `frontend/` comme contextes distincts, soit de sélectionner le Dockerfile et le contexte correspondants. Si ces options n’existent pas, ne pas configurer un build depuis la racine en supposant qu’il découvrira le bon sous-dossier : demander la procédure Systalink ou utiliser le repli VPS ci-dessous.

## 3. Créer KORA API

Dans le tableau de bord, créer une Application Web depuis le dépôt Git KORA, sur la branche de déploiement voulue, avec le contexte Docker `backend/` et son `Dockerfile`. Confirmer le chemin/répertoire accepté par l’interface avant de lancer un build.

### Configuration sur Systalink

| Champ | Valeur |
|---|---|
| Contexte / Répertoire racine | `backend/` |
| Chemin du fichier Docker | `Dockerfile` |
| Branche | La branche choisie pour le déploiement |
| Type de déploiement | Docker |

⚠️ **Important** : Le contexte doit être `backend/` (le répertoire contenant le Dockerfile et le code à construire). Le chemin du Dockerfile est alors `Dockerfile` (relatif au contexte).

Le Dockerfile :

- compile avec Java 17 et le Gradle Wrapper 9.9.1 du dépôt ;
- produit la distribution `:app:installDist`, puis l’exécute dans une image JRE non-root ;
- expose le port par défaut 8080 et installe `curl` pour le health check du Compose local.

**Variables d'environnement / Secrets :**

| Clé | Valeur | Type | Notes |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<hôte>:<port>/<base><options-TLS>` | Variable | Fourni par Systalink après création PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `<utilisateur-PostgreSQL>` | Variable | Fourni par Systalink |
| `SPRING_DATASOURCE_PASSWORD` | <mot-de-passe-à-configurer> | Variable d'environnement masquée | Le backend lit cette variable ; ne pas la mettre en fichier secret sans configurer explicitement la lecture de fichier |
| `SESSION_COOKIE_SECURE` | `true` | Variable | Obligatoire en HTTPS |

Le code utilise `PORT` s’il est fourni, puis `SERVER_PORT`, puis 8080. Ne définir `SERVER_PORT` que si le tableau de bord exige un nom de variable différent. L’endpoint de santé existant est `GET /api/health` ; il vérifie aussi la connexion à la base. Si Systalink demande un chemin de health check, utiliser celui-ci et confirmer le port/protocole dans l’interface.

La vérification est réussie uniquement si la réponse est du JSON `{"status":"ok"}`. Une page HTML « Service temporarily unavailable », même servie avec HTTP 200, indique que le proxy ne joint pas l'API prête à répondre ; vérifier l'état du service, ses journaux de démarrage et sa configuration avant de tester l'inscription.

`ddl-auto` est en mode `validate` et Flyway est activé : la migration existante `V1__forum_and_users.sql` est exécutée au démarrage sur une base vide. Vérifier les logs de migration avant la recette.

## 4. Créer KORA Web

Créer une **Application Web**, pas un site statique : KORA utilise un serveur Next.js (`output: "standalone"`) et un proxy applicatif `/api/*`.

### Configuration sur Systalink

Si l'interface accepte le runtime Node natif, utiliser les paramètres suivants :

| Champ | Valeur |
|---|---|
| Contexte / Répertoire racine | `frontend/` |
| Branche | La branche choisie pour le déploiement |
| Runtime | Node.js 22 |
| Commande de build | `npm ci && npm run build` |
| Commande de démarrage | `npm start` |

**Variables d'environnement :**

| Clé | Valeur | Type |
|---|---|---|
| `KORA_API_URL` | `https://<domaine-HTTPS-KORA-API>` (ex: `https://api.kora.org`) | Variable (build) |
| `PORT` | *(fourni automatiquement par Systalink)* | Laissez vide ou auto |

⚠️ **IMPORTANT** : `KORA_API_URL` **doit commencer par `https://` ou `http://`**. Next.js rewrite exige une URL complète, pas seulement le domaine. Les formats ci-dessous sont **INVALIDES** :
- ❌ `api.kora.org`
- ❌ `kora.org/api`

Les formats ci-dessous sont **VALIDES** :
- ✅ `https://api.kora.org`
- ✅ `https://kora.org`
- ✅ `http://localhost:8080` (développement local)

Définir `KORA_API_URL` dans l’environnement de build avec l’URL API HTTPS Systalink, par exemple `https://<hôte-HTTPS-KORA-API>` **remplacée par la valeur affichée dans le tableau de bord**. Next.js compile les rewrites dans la sortie de build ; changer cette variable uniquement au runtime ne suffit pas. `PORT` est lu au démarrage par Next.js et `HOSTNAME` vaut `0.0.0.0`.

Si Systalink construit via Docker, sélectionner `frontend/Dockerfile` et fournir `KORA_API_URL` comme argument de build. L’image échoue volontairement si cette valeur manque. La valeur n’est pas un secret ; c’est l’origine serveur vers laquelle le rewrite Next.js `/api/*` relaie les appels. Ne pas mettre `http://api:8080` dans le build Systalink : cette adresse est réservée au réseau Compose local.

La configuration Next.js conserve les appels du navigateur en same-origin (`/api/...`) et proxifie côté serveur vers l’API ; **CORS n’est donc pas nécessaire** pour ce parcours. Les cookies de session restent associés à l’origine Web. `SESSION_COOKIE_SECURE=true`, `HttpOnly` et `SameSite=Lax` sont les paramètres de production prévus par [application.yml](../../backend/app/src/main/resources/application.yml).

## 5. PostgreSQL géré — Configuration

Systalink propose PostgreSQL géré. Les paramètres de connexion sont :

| Paramètre | Valeur |
|---|---|
| Hôte | `postgres-dbaas-postgres-kora-db-kora-db.systalinkdatabases.com` |
| Port | `33101` |
| Nom de base | `kora_db` |
| Utilisateur | `kora_db_u` |
| Mot de passe | <mot-de-passe-à-configurer> |
| URL PostgreSQL | `postgresql://<utilisateur>:<mot-de-passe>@<hôte>:<port>/<base>` |

**Construction de l'URL JDBC pour Spring Boot :**

La base de données PostgreSQL doit être configurée dans KORA API avec l'URL JDBC au format suivant :

```
jdbc:postgresql://postgres-dbaas-postgres-kora-db-kora-db.systalinkdatabases.com:33101/kora_db?sslmode=require
```

**Variables d'environnement pour KORA API :**

**Variables ordinaires :**

| Clé | Valeur |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres-dbaas-postgres-kora-db-kora-db.systalinkdatabases.com:33101/kora_db?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `kora_db_u` |
| `SESSION_COOKIE_SECURE` | `true` |

La variable de mot de passe doit être ajoutée dans **Variables environnementales** sous la clé `SPRING_DATASOURCE_PASSWORD`. Systalink masque sa valeur dans l'affichage. La section « Dossiers secrets » monte des fichiers dans le conteneur et ne crée pas automatiquement une variable d'environnement.

Après la création de KORA API avec ces variables, vérifier que Flyway a exécuté la migration `V1__forum_and_users.sql` en consultant les logs de déploiement. Vous devriez voir un message comme « migration successfully applied » ou « schema is up to date ». L'endpoint `/api/health` doit répondre avec un code 200 ; sinon, vérifier la connexion, les logs et que la base est accessible depuis l'API.

Tester le parcours complet : inscription, connexion, publication de question, réponse, acceptation de solution et modification d'une question.

## 6. Test local de la connexion PostgreSQL

Avant de déployer KORA API sur Systalink, vous pouvez tester la connexion à la base de données locale (Compose) ou distante. 

**Avec Compose local** (pour vérifier que les migrations et l'API fonctionnent) :

```sh
cd KORA
cp .env.example .env
# Modifier .env si nécessaire pour le mot de passe local
docker compose up -d db
docker compose logs db  # Attendre "database system is ready to accept connections"
docker compose up --build api
docker compose logs api  # Vérifier "Flyway" et "Successfully applied"
curl http://127.0.0.1:8080/api/health
docker compose down
```

**Avec la base de données Systalink** (sans docker, juste la validation du code) :

```powershell
# Windows PowerShell
cd backend
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://postgres-dbaas-postgres-kora-db-kora-db.systalinkdatabases.com:33101/kora_db?sslmode=require'
$env:SPRING_DATASOURCE_USERNAME='kora_db_u'
$env:SPRING_DATASOURCE_PASSWORD='<mot-de-passe-PostgreSQL>'
$env:SESSION_COOKIE_SECURE='true'
.\gradlew.bat :app:installDist
# L'application Java n'est pas lancée ici (pas de tests de runtime)
```

ou en shell Unix :

```sh
cd backend
export SPRING_DATASOURCE_URL='jdbc:postgresql://postgres-dbaas-postgres-kora-db-kora-db.systalinkdatabases.com:33101/kora_db?sslmode=require'
export SPRING_DATASOURCE_USERNAME='kora_db_u'
export SPRING_DATASOURCE_PASSWORD='<mot-de-passe-PostgreSQL>'
export SESSION_COOKIE_SECURE='true'
./gradlew :app:installDist
```

Le build validant que le code compile avec ces variables définies.

## 7. Vérifications avant et après déploiement

Depuis le dépôt, sans Docker (shell Unix) :

```sh
cd backend
./gradlew test :app:installDist

cd ../frontend
npm ci
npm run lint
KORA_API_URL=https://<hôte-HTTPS-KORA-API> npm run build
```

Sous Windows PowerShell :

```powershell
cd backend
.\gradlew.bat test :app:installDist
cd ..\frontend
npm ci
npm run lint
$env:KORA_API_URL='https://<hôte-HTTPS-KORA-API>'
npm run build
```

Après création des applications (sans considérer le build réussi comme une recette fonctionnelle) :

1. Confirmer les deux URL HTTPS et que la Web peut joindre l’API.
2. Vérifier `/api/health` sur l’API et les logs de Flyway.
3. Depuis KORA Web, s’inscrire, se connecter, publier une question, ajouter une réponse, accepter la solution, rechercher par tag et modifier sa propre question.
4. Confirmer la persistance après rechargement et le refus d’une modification par un autre utilisateur.
5. Vérifier `SESSION_COOKIE_SECURE=true` et les cookies dans le navigateur HTTPS.
6. Vérifier les limites mémoire, les logs et le comportement après un redémarrage contrôlé.

Les sessions Spring actuelles sont en mémoire : un redémarrage de l’API déconnecte les membres, mais les comptes et discussions en PostgreSQL persistent. Garder une seule instance API pour la démonstration tant qu’un stockage de session partagé ou une affinité de session n’a pas été confirmé.

La configuration Compose locale conserve ses limites mémoire (API 1 Gio, Web 1 Gio, PostgreSQL 2 Gio). Les limites et ressources de build Systalink se règlent séparément dans son tableau de bord ; mesurer les besoins et obtenir l’accord budgétaire avant de choisir une offre. Aucune construction d’image ou vérification conteneurisée n’est revendiquée dans cette préparation : Docker Desktop était inaccessible.

## 8. Sauvegarde et retour arrière

- Avant un déploiement, relever le commit Git déployé et confirmer une sauvegarde PostgreSQL récente.
- Vérifier dans le tableau de bord si un déploiement antérieur peut être restauré. Si non, redéployer le commit Git précédent (ou faire un revert ciblé) et contrôler `/api/health`.
- Les migrations Flyway sont versionnées et la base utilise `ddl-auto: validate`. Ne pas tenter de « rollbacker » le schéma par une modification manuelle non testée.
- Pour restaurer des données, créer d’abord une base cible de restauration, restaurer une sauvegarde dans cette base, vérifier l’application, puis faire basculer la variable de connexion. Ne pas écraser la base active sans sauvegarde vérifiée et autorisation explicite.
- Vérifier la rétention, l’export et la procédure de restauration Systalink dans le tableau de bord ; ces détails ne sont pas déduits des pages marketing.

## 9. Repli : un VPS Systalink avec Compose et Caddy

À utiliser uniquement si le service Application Web ne peut pas construire ce monorepo, ou si PostgreSQL géré n’est pas disponible, et après choix/autorisation du VPS. Ce repli réutilise les Dockerfiles et le Compose KORA ; il ne crée pas une seconde implémentation.

1. Sur le VPS, installer Docker Compose et Caddy selon les instructions officielles de leurs éditeurs, configurer DNS pour le domaine choisi et n’autoriser en entrée que SSH, 80 et 443.
2. Cloner le dépôt KORA, copier `.env.example` en `.env`, puis remplacer le mot de passe local par un secret fort. Définir :

   ```text
   SESSION_COOKIE_SECURE=true
   KORA_API_URL=http://api:8080
   KORA_WEB_PORT=3000
   ```

   Ici seulement, `http://api:8080` est le nom interne du service Compose. Le port Web est lié à `127.0.0.1`; l’API et PostgreSQL n’ont aucun port hôte publié.
3. Construire et démarrer :

   ```sh
   docker compose up -d --build
   docker compose ps
   docker compose logs --tail=100 api frontend db
   ```

4. Configurer Caddy sur le VPS, en remplaçant le domaine d’exemple :

   ```caddyfile
   kora.example.sn {
       encode zstd gzip
       reverse_proxy 127.0.0.1:3000
   }
   ```

   Caddy obtient et renouvelle le certificat automatiquement lorsque DNS et ports publics 80/443 sont correctement configurés. Ne pas publier 3000, 8080 ou 5432 au public.
5. La base du repli est le PostgreSQL 17 du Compose et son volume nommé. Mettre en place des sauvegardes hors VPS et tester la restauration ; le volume seul n’est pas une sauvegarde.
6. Pour revenir à une version applicative précédente, déployer un commit Git antérieur puis vérifier la santé. Restaurer les données séparément uniquement si nécessaire, avec une sauvegarde confirmée.

Le Compose local a été adapté pour lier le port Web à la boucle locale et convient aussi au proxy Caddy. Ce guide n’a créé aucun VPS, base, abonnement ni déploiement distant.

## Sources

- Systalink, [Application Web / Serverless](https://systalink.com/fr/serverless) et [Base de données](https://systalink.com/fr/base-de-donnees).
- Next.js 16, [rewrites](https://nextjs.org/docs/app/api-reference/config/next-config-js/rewrites), [variables d’environnement](https://nextjs.org/docs/app/guides/environment-variables) et [déploiement](https://nextjs.org/docs/app/getting-started/deploying).
- Caddy, [HTTPS automatique](https://caddyserver.com/docs/automatic-https).
