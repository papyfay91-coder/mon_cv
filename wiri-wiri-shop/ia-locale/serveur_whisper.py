"""
Serveur de transcription Whisper 100 % local et gratuit pour Wiri-Wiri Shop.

Expose la même route que l'API d'OpenAI (POST /v1/audio/transcriptions) :
le backend Spring Boot l'appelle sans aucune modification de code.

- Moteur : faster-whisper (Whisper open source, optimisé CPU / Apple Silicon).
- L'audio reste en mémoire : il n'est jamais écrit sur disque (finalité CDP).
- Le wolof ne fait pas partie des langues officielles de Whisper : si la langue
  demandée n'est pas reconnue, la transcription se fait en détection automatique.

Lancement :  uvicorn serveur_whisper:app --port 9000
Variables :  WHISPER_MODELE (tiny, base, small, medium, large-v3 ; défaut : small)
"""

import io
import logging
import os
import threading

from fastapi import FastAPI, File, Form, HTTPException, UploadFile

journal = logging.getLogger("whisper-local")
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

TAILLE_MAX = 10 * 1024 * 1024  # identique à la limite du backend
NOM_MODELE = os.environ.get("WHISPER_MODELE", "small")

app = FastAPI(title="Wiri-Wiri Whisper local")
_verrou = threading.Lock()
_modele = None


def modele():
    """Charge le modèle au premier appel (téléchargé une seule fois, puis gardé en cache)."""
    global _modele
    with _verrou:
        if _modele is None:
            from faster_whisper import WhisperModel

            journal.info("Chargement du modèle Whisper « %s »…", NOM_MODELE)
            _modele = WhisperModel(NOM_MODELE, device="auto", compute_type="int8")
            journal.info("Modèle prêt.")
        return _modele


def langue_supportee(code: str | None) -> str | None:
    if not code:
        return None
    try:
        from faster_whisper.tokenizer import _LANGUAGE_CODES
    except ImportError:  # structure interne changée : on laisse Whisper décider
        return None
    return code if code in _LANGUAGE_CODES else None


@app.get("/health")
def sante():
    return {"status": "UP", "modele": NOM_MODELE}


@app.post("/v1/audio/transcriptions")
async def transcrire(
    file: UploadFile = File(...),
    model: str = Form(None),  # ignoré : le modèle est choisi au lancement du serveur
    language: str = Form(None),
    prompt: str = Form(None),
    temperature: float = Form(0.0),
    response_format: str = Form("json"),
):
    audio = await file.read()
    if not audio:
        raise HTTPException(status_code=400, detail="Fichier audio vide.")
    if len(audio) > TAILLE_MAX:
        raise HTTPException(status_code=413, detail="Fichier audio trop volumineux.")

    langue = langue_supportee(language)
    if language and not langue:
        journal.info("Langue « %s » non prise en charge par Whisper : détection automatique.", language)

    try:
        segments, info = modele().transcribe(
            io.BytesIO(audio),
            language=langue,
            initial_prompt=prompt,
            temperature=temperature,
            vad_filter=True,
        )
        texte = " ".join(s.text.strip() for s in segments).strip()
    except Exception as erreur:  # audio illisible, format inconnu…
        journal.warning("Transcription impossible : %s", erreur)
        raise HTTPException(status_code=400, detail="Audio illisible.") from erreur
    finally:
        del audio  # rien n'est conservé

    journal.info("Transcription terminée (langue détectée : %s, %d caractères).", info.language, len(texte))
    return {"text": texte, "language": info.language}
