# Projet Etudiant Openclassrooms n°7 – Améliorez votre application avec des systèmes distribués


<img src="/preview.png" alt="Logo de l'application">


<h1 align="center">Tour Guide</h1>


## Description

L'objectif principal du projet est de fournir des recommandations touristiques basées sur la localisation de l'utilisateur. La mission consistait à débuguer certaines parties de code et améliorer les performances grâce au multithreading.


# Technologies

> Java 17  
> Spring Boot 3.X  
> Maven
> JUnit 5


## Installation

1. Clonez le dépôt :
    ```sh
    git clone https://github.com/Xenophee/Tour-Guide.git
    ```

2. Installez les dépendances :
    ```sh
    mvn install:install-file -Dfile=/libs/gpsUtil.jar -DgroupId=gpsUtil -DartifactId=gpsUtil -Dversion=1.0.0 -Dpackaging=jar
    ```

    ```sh
    mvn install:install-file -Dfile=/libs/RewardCentral.jar -DgroupId=rewardCentral -DartifactId=rewardCentral -Dversion=1.0.0 -Dpackaging=jar
    ```

    ```sh
    mvn install:install-file -Dfile=/libs/TripPricer.jar -DgroupId=tripPricer -DartifactId=tripPricer -Dversion=1.0.0 -Dpackaging=jar
    ```

3. Construisez le projet :
    ```sh
    mvn clean install
    ```


4. Exécutez l'application :
    ```sh
    mvn spring-boot:run
    ```

## Exécution des tests

Pour exécuter les tests, utilisez la commande suivante :

```sh
mvn test
```

