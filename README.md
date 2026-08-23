# Lightning Cam

Lightning Cam est une PWA expérimentale pour photographier les orages de nuit avec un iPhone 16 Pro. Elle détecte localement les hausses brutales de luminosité, conserve une trame vidéo, demande une photo haute définition à Safari et enregistre un court clip autour de l’éclair.

## Utilisation sur iPhone

1. Ouvrir l’URL GitHub Pages dans Safari, en HTTPS.
2. Autoriser l’accès à la caméra.
3. Poser l’iPhone sur un trépied, branché si possible, avec l’objectif principal `1×` dégagé.
4. Toucher une zone lointaine et contrastée dans l’aperçu pour demander la mise au point.
5. Appuyer sur **Armer la détection** et ne plus déplacer l’iPhone pendant la calibration.
6. Exporter les bonnes captures vers Photos ou Fichiers depuis la galerie.

Pour l’installer, utiliser **Partager → Sur l’écran d’accueil** dans Safari. Si la caméra refuse de démarrer dans l’app installée, ouvrir temporairement l’URL directement dans Safari.

## Ce que Safari permet réellement

L’application demande la caméra arrière principale et la meilleure résolution raisonnable. Elle teste ensuite les capacités exposées par le navigateur. Focus, ISO et exposition ne sont affichés comme disponibles que si la piste vidéo les annonce. Un réglage non exposé reste sous contrôle automatique d’iOS.

La trame JPEG est saisie au moment où le flux vidéo détecte l’éclair. La photo haute définition est déclenchée en parallèle mais peut arriver après un éclair très bref. Le clip est donc essentiel pour évaluer et améliorer la stratégie de capture.

## Confidentialité et fonctionnement hors ligne

- Aucun compte, serveur, téléversement ni outil d’analyse.
- Détection et captures entièrement locales.
- Interface mise en cache après la première visite.
- Médias conservés dans IndexedDB jusqu’à leur export ou suppression.

iOS peut supprimer les données d’un site. La galerie locale n’est pas une sauvegarde : exporte les captures importantes.

## Développement

Prérequis : Node.js 22 et npm.

```bash
npm install
npm run dev
```

La caméra exige HTTPS, sauf sur `localhost`. Pour valider le comportement réel, utiliser la version GitHub Pages sur l’iPhone.

```bash
npm test
npm run check
npm run build
```

Le projet utilise TypeScript, Vite et Vitest. Les tests couvrent le détecteur, le tampon temporel, les adaptateurs caméra/enregistrement, le coordinateur de capture, le stockage et l’enregistrement du service worker.

## Validation terrain

Commencer par une pièce sombre avec un flash bref orienté vers un mur, puis vérifier :

- la calibration et le curseur de sensibilité ;
- les faux positifs causés par les phares ou lampadaires ;
- la netteté de la photo et de la trame vidéo ;
- le niveau de grain ;
- la chauffe et la consommation après 15 à 30 minutes ;
- la continuité après verrouillage/déverrouillage de l’écran.

Le prototype ne remplace pas une app native pour le contrôle manuel de l’ISO, du temps d’exposition et du focus.

## Déploiement

Chaque push sur `main` exécute les tests, le typage et le build, puis publie `dist/` sur GitHub Pages via GitHub Actions.
