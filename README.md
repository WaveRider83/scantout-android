# Scantout Android

<p align="center"><img src="logo.png" alt="Scantout logo" width="220"></p>

**Scantout** — scan OCR simple et rapide : photographie un document, détecte automatiquement les zones de texte, touche pour sélectionner, copie dans le presse-papier. Sans comptes, sans cloud, sans fioritures.

## Fonctionnalités

- 📷 Capture photo plein écran (CameraX, flash, zoom pincement)
- 🔍 Détection automatique des zones de texte (ML Kit + PP-OCR, boîtes orientées)
- ✋ Sélection multi-zones au toucher, copie de la sélection ou de tout le texte
- ✏️ Texte éditable avant copie
- 🗂️ Historique local des captures (miniature + texte OCR), ré-analyse OCR à la demande, suppression
- 📤 Partage via le menu système (texte et/ou image)
- 🔒 100 % local : aucun compte, aucune donnée envoyée

## Installation (beta)

Télécharger `scantout.apk` depuis la dernière [release](../../releases/latest), puis installer (autoriser les sources inconnues). Android 8.0+ (API 26), arm64-v8a.

Signature debug — Android peut afficher « Appli non sécurisée » : c'est normal pour une beta, Valider quand même.

Premier test conseillé : ouvrir l'app et scanner une étiquette réelle pour vérifier la lecture OCR.
