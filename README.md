# Maven Package Uploader

Le programme va copier les pom et jar dans le dossier de votre choix et va vous donner la liste des commandes à exécuter
pour les upload.

Ne gère pas les .jar avec classifiers

## Getting started

Java 22 (en preview pour le compilateur)

## Lancement

Il faut modifier les valeurs des 3 premières variables dans le code selon votre choix.

```java
var pathRepositoryToExplore = "C:/Users/perso/.m2/repository/com/worldline/edoc";
var pathRepositoryRoot = "C:/Users/perso/.m2/repository";
var pathDirectoryLibToUpload = "C:/Users/perso/.m2/flat_lib";


```

- pathRepositoryToExplore: le dossier les librairies qui vous voulez uploader
- pathRepositoryRoot: le root du dossier repository
- pathDirectoryLibToUpload: le dossier où vont être copié les .jar et .pom. C'est nécessaire car dans le cas des où on
  upload un .jar le .pom va être recréé par la commande maven, il va donc y avoir conflit.

## TODO

La méthode pour faire l'upload directement depuis le programme ne marche pas pour l'instant. (Problème d'exécution de la
commande dans le context donné)