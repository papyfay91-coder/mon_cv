# IA locale et gratuite (Voice-to-Store sans OpenAI)

Tout tourne sur votre machine : aucune clé API, aucun abonnement, et la voix des vendeurs ne quitte pas le serveur.

| Étape | Outil | Port |
|---|---|---|
| Transcription de la note vocale | Whisper open source (`serveur_whisper.py`, ce dossier) | 9000 |
| Extraction nom / prix / taille | [Ollama](https://ollama.com) avec un modèle libre (Qwen 2.5 par défaut) | 11434 |

Le backend les appelle avec le même format que l'API d'OpenAI : on passe de l'un à l'autre par la seule configuration.

## Installation sur Mac (une seule fois)

**1. Ollama et le modèle d'extraction**
```bash
brew install ollama          # ou télécharger l'application sur ollama.com
ollama serve                 # laisser ce terminal ouvert (inutile si l'application Ollama est lancée)
ollama pull qwen2.5:7b       # dans un autre terminal, environ 4,7 Go
```
Mac avec 8 Go de mémoire : prenez plutôt `ollama pull qwen2.5:3b` (environ 2 Go) et lancez le backend avec `LLM_MODEL=qwen2.5:3b`.

**2. Le serveur Whisper**
```bash
cd ~/wiri/wiri-wiri-shop/ia-locale
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn serveur_whisper:app --port 9000
```
Le modèle `small` (environ 500 Mo) se télécharge au premier appel, puis reste en cache.
Pour une meilleure qualité mais plus lent : `WHISPER_MODELE=medium uvicorn serveur_whisper:app --port 9000`.

**3. Vérifier**
```bash
curl http://localhost:9000/health        # {"status":"UP","modele":"small"}
curl http://localhost:11434/api/tags     # liste les modèles Ollama installés
```

## Lancer le backend en mode IA locale
Dans le terminal du backend, ajoutez simplement le profil `ia-locale` :
```bash
export SPRING_PROFILES_ACTIVE=ia-locale
mvn spring-boot:run
```
Aucune clé n'est nécessaire. L'écran de consentement de l'app indique alors automatiquement au vendeur
que sa voix est traitée sur vos serveurs, sans service extérieur (réglage `wiriwiri.ia.hebergement: LOCAL` du profil). La première analyse vocale est plus lente (chargement des modèles en mémoire), les suivantes vont plus vite.

## À savoir
- **Wolof** : Whisper ne prend pas officiellement en charge le wolof (`wo`). Le serveur passe alors en détection automatique ;
  la transcription sera approximative. Le vendeur vérifie toujours la fiche avant de publier.
- **Prix en dërëm** (1 dërëm = 5 FCFA) : le prompt ne l'explique pas au modèle, vérifiez les prix proposés.
- **Production** : il faut un serveur assez puissant (idéalement avec carte graphique) pour faire tourner Whisper et Ollama.
- **Tests** : `pip install pytest httpx && python -m pytest` (modèle simulé, aucun téléchargement).
