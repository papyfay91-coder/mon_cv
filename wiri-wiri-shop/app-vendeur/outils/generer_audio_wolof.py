"""
Génère les conditions d'utilisation du micro EN WOLOF, par synthèse vocale, sans enregistrement.

Moteur : MMS-TTS de Meta (modèle « facebook/mms-tts-wol », voix wolof), exécuté sur votre machine.
  - Licence CC-BY-NC 4.0 : usage NON COMMERCIAL (tests, démonstrations). Pour la version
    commerciale, remplacez ces fichiers par un enregistrement fait par une vraie voix.
  - Qualité à vérifier à l'oreille : écoutez les fichiers avant de les activer.

Usage (depuis le dossier app-vendeur) :
    python3 -m venv .venv-tts && source .venv-tts/bin/activate
    pip install -r outils/requirements-tts.txt
    python outils/generer_audio_wolof.py            # génère puis active les fichiers dans l'app
    python outils/generer_audio_wolof.py --sans-activer

Sortie : assets/audio/conditions-wo-local.wav et conditions-wo-externe.wav (WAV 16 kHz, lus par iOS et Android).
"""

import argparse
import json
import re
import wave
from pathlib import Path

import numpy as np

RACINE = Path(__file__).resolve().parent.parent
DOSSIER_AUDIO = RACINE / "assets" / "audio"
TEXTES = DOSSIER_AUDIO / "conditions-wolof.json"
TABLE_AUDIO = RACINE / "src" / "audio" / "conditions.ts"
MODELE = "facebook/mms-tts-wol"
PAUSE_ENTRE_PHRASES = 0.45  # secondes


def phrases(mode: str) -> list[str]:
    textes = json.loads(TEXTES.read_text(encoding="utf-8"))
    return textes["commun_debut"] + textes[mode] + textes["commun_fin"]


class SyntheseMms:
    """Voix wolof de MMS-TTS (téléchargée une seule fois, environ 150 Mo)."""

    def __init__(self):
        import torch
        from transformers import AutoTokenizer, VitsModel

        self.torch = torch
        self.tokenizer = AutoTokenizer.from_pretrained(MODELE)
        self.modele = VitsModel.from_pretrained(MODELE)
        self.frequence = self.modele.config.sampling_rate

    def dire(self, phrase: str) -> np.ndarray:
        self.torch.manual_seed(0)  # même voix à chaque génération
        entree = self.tokenizer(phrase, return_tensors="pt")
        with self.torch.no_grad():
            onde = self.modele(**entree).waveform
        return onde.squeeze().cpu().numpy()


def assembler(synthese, liste: list[str]) -> np.ndarray:
    silence = np.zeros(int(PAUSE_ENTRE_PHRASES * synthese.frequence), dtype=np.float32)
    morceaux = []
    for phrase in liste:
        morceaux += [synthese.dire(phrase).astype(np.float32), silence]
    return np.concatenate(morceaux)


def ecrire_wav(chemin: Path, onde: np.ndarray, frequence: int) -> None:
    onde = onde / max(1e-6, float(np.max(np.abs(onde)))) * 0.9  # volume normalisé
    with wave.open(str(chemin), "wb") as fichier:
        fichier.setnchannels(1)
        fichier.setsampwidth(2)
        fichier.setframerate(frequence)
        fichier.writeframes((onde * 32767).astype("<i2").tobytes())


def activer(fichiers: dict[str, str]) -> None:
    """Active les fichiers générés dans src/audio/conditions.ts (remplace la ligne du mode)."""
    source = TABLE_AUDIO.read_text(encoding="utf-8")
    for mode, nom in fichiers.items():
        source = re.sub(
            rf"^\s*(//\s*)?{mode}: require\('[^']*'\),$",
            f"  {mode}: require('../../assets/audio/{nom}'),",
            source,
            flags=re.MULTILINE,
        )
    TABLE_AUDIO.write_text(source, encoding="utf-8")


def generer(synthese, sans_activer: bool = False) -> dict[str, str]:
    fichiers = {}
    for mode, nom in (("LOCAL", "conditions-wo-local.wav"), ("EXTERNE", "conditions-wo-externe.wav")):
        onde = assembler(synthese, phrases(mode))
        ecrire_wav(DOSSIER_AUDIO / nom, onde, synthese.frequence)
        print(f"{nom} : {len(onde) / synthese.frequence:.0f} s")
        fichiers[mode] = nom
    if not sans_activer:
        activer(fichiers)
        print("Activé dans src/audio/conditions.ts. Relancez Expo avec : npx expo start --clear")
    return fichiers


if __name__ == "__main__":
    arguments = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    arguments.add_argument("--sans-activer", action="store_true", help="générer sans modifier l'application")
    options = arguments.parse_args()
    print(f"Chargement de la voix wolof {MODELE} (licence non commerciale)…")
    generer(SyntheseMms(), options.sans_activer)
