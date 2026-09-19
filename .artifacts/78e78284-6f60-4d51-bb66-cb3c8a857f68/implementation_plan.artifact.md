# Plan d'implémentation : TimeCraft Multiplatform avec Supabase et MapLibre

Ce plan décrit la transformation de l'application Android simple en une application **Compose Multiplatform (CMP)** ciblant Android et Windows, intégrant **Supabase** pour les données et **MapLibre** pour la cartographie, tout en héritant du style visuel de **Kaptal**.

## Changements proposés

### Configuration du Projet
- **Conversion CMP** : Restructuration du projet en utilisant les dossiers `commonMain`, `androidMain` et `desktopMain`.
- **Dépendances** : Ajout de Supabase (Auth, Postgrest), KMP-Maps (ou intégration native MapLibre), et les bibliothèques de navigation multiplateforme.

### Style et Thème (Inspiré de Kaptal)
- Copie et adaptation du thème (Couleurs `0xFFE8ECEF`, cartes blanches, typographie).
- Utilisation des composants Material 3 personnalisés.

### Fonctionnalités
1. **Authentification (Supabase)** : Écran de connexion basé sur le design de Kaptal.
2. **Calendrier de Travail** : Vue principale pour cocher les jours travaillés.
3. **Suivi des Frais et Clients** : Formulaires de saisie liés aux jours travaillés.
4. **Carte des Trajets (MapLibre)** : Visualisation des trajets sur une carte interactive.

---

## Modifications détaillées

### [Configuration Multiplatform]

#### [MODIFY] [settings.gradle.kts](file:///C:/Users/emeri/AndroidStudioProjects/TimeCraft/settings.gradle.kts)
#### [MODIFY] [libs.versions.toml](file:///C:/Users/emeri/AndroidStudioProjects/TimeCraft/gradle/libs.versions.toml)
#### [NEW] [build.gradle.kts](file:///C:/Users/emeri/AndroidStudioProjects/TimeCraft/app/build.gradle.kts) (Refonte complète pour CMP)

### [Composants Communs (commonMain)]

#### [NEW] [App.kt](file:///C:/Users/emeri/AndroidStudioProjects/TimeCraft/app/src/commonMain/kotlin/com/example/timecraft/App.kt) : Navigation et structure globale.
#### [NEW] [Theme.kt](file:///C:/Users/emeri/AndroidStudioProjects/TimeCraft/app/src/commonMain/kotlin/com/example/timecraft/ui/theme/Theme.kt) : Style hérité de Kaptal.
#### [NEW] [AuthScreen.kt](file:///C:/Users/emeri/AndroidStudioProjects/TimeCraft/app/src/commonMain/kotlin/com/example/timecraft/screens/AuthScreen.kt) : Login Supabase.

---

## Plan de vérification

### Tests Automatisés
- Compilation réussie pour Android (`./gradlew :app:assembleDebug`).
- Compilation réussie pour Desktop (`./gradlew :app:run`).

### Vérification Manuelle
- Lancement de l'application sur un émulateur Android.
- Lancement de l'application sur Windows.
- Test de connexion via Supabase.
- Affichage du calendrier et de la carte.
