# Maven Package Uploader

Le programme va copier les pom et jar dans le dossier de votre choix et va vous donner la liste des commandes à exécuter
pour les upload dans la console.

Ne gère pas les .jar avec classifiers

## Getting started

Java 22 (en preview pour le compilateur)

## Lancement

Les paramètres attendu sont les suivants

- Premier paramètre: Dossier que vous voulez updater (exemple: C:/Users/bob/.m2/repository/com)
- Deuxième paramètre: Dosser racine du repository (exemple: C:/Users/bob/.m2/repository)
- Troisième paramètre: Dossier cible (a créer) où copier les jar et pom avant de les upload (exemple: C:/Users/bob/.m2/repository_to_upload)
- Quatrième paramètre: Repository id identifié dans settings.xml de maven (exemple: gitlab-maven)
- Cinquième paramètre: Url package registry (exemple: https://mon_gitlab/api/v4/projects/979/packages/maven)

```


La méthode pour faire l'upload directement depuis le programme ne marche pas pour l'instant. (Problème d'exécution de la
commande dans le context donné)