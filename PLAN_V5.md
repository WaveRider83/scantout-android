# PLAN V5 — « Scantout Simple » : scan OCR copiable, sans hiérarchie d'objets

> Décisions Ric (26/11/2026, Discord) : option A — nouveau repo `~/projects/scantout-v5`,
> V4 « mode note » ABANDONNÉE (remplacée par V5). Historique = images + texte OCR,
> avec re-analyse OCR des images déjà capturées.

## Objectif
Application Android minimale : photographier, détecter les zones de texte, toucher une ou
plusieurs zones, **copier le texte** dans le presse-papier. Fiable, rapide, sans gestion
d'objets (pas de SN, pas de parent/enfant, pas de notes, pas de fiches, pas d'export CSV/listing).

## Ce qu'on GARDE de v3 (portage, pas réécriture)
- Caméra CameraX, résolution 1440×1080 4:3 (`ResolutionSelector` dans CameraScreen).
- ML Kit Text Recognition + PP-OCR det ONNX (`poc/PpOcrDet.kt`, `PpOcrDetector.kt`, 2,4 Mo) pour
  les boîtes orientées ; `StaticImageAnalyzer.kt` (analyse à froid sur image, base de la re-analyse).
- `QuadCrop.kt` (crop polygonal), overlay de zones tapables (inspiré `ui/common/BoundingBoxOverlay.kt`).
- Thème/DA : repartir simple (pas le HUD complet), identité légère à définir.

## Ce qu'on JETTE
- Room objet/note/observation, hiérarchie parent/enfant, fusion TMP-, `isMoreReliable`,
  `buildObjectChainFields`, consensus multi-frames, session chips, ROI journal, NFC, réseau,
  Library/listing, export CSV, manual entry, GS1/SerialModelExtractor (sauf si besoin plus tard).

## Périmètre fonctionnel V5
1. **Capture** : viseur plein écran, déclencheur, flash auto simple, zoom pincée.
2. **Analyse** : à froid sur la photo capturée (pas de live lourd) ; ML Kit en premier,
   PP-OCR det pour les boîtes si besoin (à mesurer sur photos réelles).
3. **Écran résultat** : photo + zones encadrées ; tap = sélection (multi) ; boutons
   « Copier la sélection » / « Copier tout » ; texte éditable avant copie.
4. **Historique** : liste des captures (image miniature + date + texte OCR), persistance
   locale légère (Room v1 : table `scans(id, imagePath, createdAt, ocrText, zonesJson)`),
   ouverture d'un scan passé, **re-analyse OCR** à la demande (relance ML Kit/PP-OCR sur le
   fichier image), suppression.
5. **Partage** : bouton Partager (texte, et/ou image) via intent système.

## Hors périmètre
Objets, SN, fiches, notes, export listing, multi-machines, NFC — tout ce qui fait la lourdeur v3/v4.

## Architecture
Repo unique, 2 modules : `:app` (UI Compose, 1-2 écrans + historique) + `:core:ocr`
(caméra, analyse, crop — porté de v3, deps ML Kit + onnxruntime). Pas de Hilt obligatoire
(garder simple, DI manuelle ou Hilt minimal selon préférence au build).

## Lots proposés (séquentiels, review entre chaque)
- V5-1 : squelette app + caméra + capture + sauvegarde image locale.
- V5-2 : pipeline OCR à froid + overlay zones tapables + copier.
- V5-3 : historique (Room) + re-analyse OCR + suppression + partage.
- V5-4 : polish DA minimal + QA terrain (S22/Note10+) + APK.

## Questions ouvertes
- Icône/nom affiché, package (`com.ric.scantoutv5` ?).
- ML Kit seul suffit-il ou PP-OCR det requis ? → tranché par mesure sur photos réelles (lot V5-2).
- Signature APK : debug pour tests, release plus tard.

## État
Repo créé (git init, branche main). Aucun code. V4 abandonnée côté plan — le repo v3 reste
intact en archive ; ne pas y commiter.
