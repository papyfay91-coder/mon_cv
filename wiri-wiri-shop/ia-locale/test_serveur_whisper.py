"""Tests du serveur local, avec un modèle simulé (aucun téléchargement)."""

from types import SimpleNamespace

from fastapi.testclient import TestClient

import serveur_whisper


class FauxModele:
    def __init__(self):
        self.appels = []

    def transcribe(self, audio, **options):
        self.appels.append(options)
        segments = [SimpleNamespace(text=" Robe bu bees, "), SimpleNamespace(text="fukki junni ")]
        return iter(segments), SimpleNamespace(language=options.get("language") or "fr")


def client(faux):
    serveur_whisper._modele = faux
    return TestClient(serveur_whisper.app)


def test_meme_format_que_openai():
    faux = FauxModele()
    reponse = client(faux).post(
        "/v1/audio/transcriptions",
        files={"file": ("note.m4a", b"\x00\x00\x00\x20ftypM4A ", "audio/mp4")},
        data={"model": "whisper-1", "language": "fr", "prompt": "Dakar", "response_format": "json"},
    )
    assert reponse.status_code == 200
    assert reponse.json()["text"] == "Robe bu bees, fukki junni"
    assert faux.appels[0]["language"] == "fr"
    assert faux.appels[0]["initial_prompt"] == "Dakar"


def test_wolof_non_supporte_passe_en_detection_automatique():
    faux = FauxModele()
    reponse = client(faux).post(
        "/v1/audio/transcriptions",
        files={"file": ("note.wav", b"RIFF\x00\x00\x00\x00WAVE", "audio/wav")},
        data={"language": "wo"},
    )
    assert reponse.status_code == 200
    assert faux.appels[0]["language"] is None


def test_audio_vide_refuse():
    reponse = client(FauxModele()).post(
        "/v1/audio/transcriptions", files={"file": ("vide.wav", b"", "audio/wav")}
    )
    assert reponse.status_code == 400


def test_sante():
    etat = client(FauxModele()).get("/health").json()
    assert etat["status"] == "UP" and etat["pret"] is True
