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

Le texte wolof est dans [`conditions-wolof.json`](conditions-wolof.json) : il sert à la fois pour l'enregistrement
et pour la voix de synthèse. Une version se compose de `commun_debut`, puis `LOCAL` **ou** `EXTERNE`, puis `commun_fin`.

## Option A — Voix de synthèse, sans enregistrement

Le script [`outils/generer_audio_wolof.py`](../../outils/generer_audio_wolof.py) fabrique les deux fichiers avec la voix
wolof **MMS-TTS de Meta**, directement sur votre Mac, puis les active dans l'app :

```bash
cd ~/wiri/wiri-wiri-shop/app-vendeur
python3 -m venv .venv-tts && source .venv-tts/bin/activate
pip install -r outils/requirements-tts.txt     # environ 1 Go (PyTorch)
python outils/generer_audio_wolof.py            # télécharge la voix (environ 150 Mo) la première fois
npx expo start --clear
```

- **Licence CC-BY-NC 4.0 : usage non commercial uniquement.** Parfait pour tester et faire des démonstrations ;
  pour la version commerciale, passez à l'option B.
- **Écoutez les fichiers** (`conditions-wo-local.wav`, `conditions-wo-externe.wav`) avant de les montrer à des vendeurs :
  une voix de synthèse peut mal prononcer certains mots.

## Option B — Enregistrement par une vraie voix (recommandé en production)

1. Application **Dictaphone** : lisez le texte lentement, dans un endroit calme, téléphone à 20 cm.
2. Partagez l'enregistrement vers votre Mac (AirDrop). Le fichier est déjà en `.m4a`.
3. Renommez-le `conditions-wo-local.m4a` ou `conditions-wo-externe.m4a` et placez-le dans ce dossier.
4. Dans `src/audio/conditions.ts`, décommentez la ligne correspondante.
5. Relancez Expo avec `npx expo start --clear`.

Gardez chaque enregistrement court (1 à 2 minutes) : il est inclus dans l'application.
