# Conditions d'utilisation du micro, en wolof

L'écran de consentement propose un bouton **« Écouter en wolof »**. Il faut deux enregistrements,
car le texte change selon l'endroit où la voix est traitée :

| Fichier | Quand il est lu |
|---|---|
| `conditions-wo-local.m4a` | IA locale (profil `ia-locale`) : la voix reste sur vos serveurs |
| `conditions-wo-externe.m4a` | OpenAI : la voix part sur des serveurs hors du Sénégal |

## ⚠️ À faire relire avant d'enregistrer

Le texte wolof ci-dessous est une **proposition de traduction** du texte français affiché à l'écran.
C'est un texte de consentement (loi n° 2008-12, CDP) : faites-le **relire et corriger par une personne
dont le wolof est la langue maternelle**, et gardez le même sens que le texte français, point par point.

## Texte à lire

**Début (les deux versions)**
> Salaam aleekum ! Ci Wiri-Wiri Shop, mën nga wax sa njaay ci wolof, te nu bind ko ci sa plaas.
> Déglul bu baax lii, ndax sa baat la.

**1. Un seul usage (les deux versions)**
> Mikro bi, dañu koy jëfandikoo rekk ngir bind xët u sa njaay : tur wi, njëg ji ak taille bi.

**2a. Version LOCALE (`conditions-wo-local.m4a`)**
> Sa baat, sunu bopp lañu koy bind, ci sunu serwëer yi. Duñu ko yónnee kenn ci biti.

**2b. Version OPENAI (`conditions-wo-externe.m4a`)**
> Sa baat, OpenAI moo koy bind, ci ay serwëer yu nekk ci biti Senegaal.

**3. Jamais conservée (les deux versions)**
> Bu xët wi paree, dañuy far sa baat. Duñu ko denc.

**4. Vous gardez la main (les deux versions)**
> Yaw miy xool xët wu nekk balaa ngay siiwal. Te mën nga dindi sa ndigël saa su la neexee, ci « Paramètres ».

**Fin (les deux versions)**
> Su nga nangoo, bësal « J'accepte ». Su nga nanguwul, mën nga bind loxo, te dara du ci wàññiku.

## Enregistrer (iPhone)

1. Application **Dictaphone** : lisez le texte lentement, dans un endroit calme, téléphone à 20 cm.
2. Partagez l'enregistrement vers votre Mac (AirDrop). Le fichier est déjà en `.m4a`.
3. Renommez-le `conditions-wo-local.m4a` ou `conditions-wo-externe.m4a` et placez-le dans ce dossier.
4. Dans `src/audio/conditions.ts`, décommentez la ligne correspondante.
5. Relancez Expo avec `npx expo start --clear`.

Gardez chaque enregistrement court (1 à 2 minutes) : il est inclus dans l'application.
