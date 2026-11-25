# Frontend Angular

Le frontend utilise Angular standalone et PrimeNG (thème Aura adapté aux couleurs CDG). Les appels `/api`
sont envoyés au backend Spring Boot sur `http://localhost:8080`.

## Développement

1. Démarrer PostgreSQL et Spring Boot depuis `backend`.
2. Depuis ce dossier, installer les dépendances puis démarrer Angular :

```powershell
npm ci
npm start
```

Ouvrir ensuite `http://localhost:4200`.

## Tableau de bord administrateur

Les graphiques comparent l'avancement réel à l'avancement attendu et présentent
les budgets consommés. Les heures affichées proviennent des feuilles validées,
regroupées par semaine de saisie sur les huit dernières semaines, semaine
courante comprise. Le bouton « Actualiser » recharge ces données.

## Saisie du temps

L'administrateur peut définir pour chaque freelance un seuil d'alerte entre
0,01 et 10 h/j (8 h par défaut, y compris pour les profils existants). Lors
d'une saisie, le total de toutes les tâches de la journée déclenche un
avertissement au-delà du seuil et est refusé au-delà de 10 h. Les données
historiques ne sont pas modifiées.

Chaque projet indique ses heures prévues et son nombre de tâches prévues. À la
création d'une tâche, son quota proposé correspond aux heures du projet divisées
par ce nombre, arrondi à 0,01 h ; il peut être modifié. Les projets existants
doivent être configurés pour bénéficier de cette proposition. Pour les anciennes
tâches, la charge existante en jours est convertie à raison de 8 h par jour ;
une charge nulle doit être corrigée avant toute nouvelle saisie.

Toutes les heures saisies pour une tâche comptent dans son quota, quel que soit
le statut de la feuille (y compris brouillon et rejetée). Une tâche au quota
n'est plus proposée pour une nouvelle saisie et tout dépassement est refusé.
La suppression d'une ligne libère les heures correspondantes.

Le générateur DEMO calcule les charges et l'avancement à partir des heures
validées, puis ajuste les budgets aux montants des PV pour que les trois indicateurs soient cohérents.
Il ne crée pas de journées dépassant 10 h. Ses exemples d'analyse vérifient les
alertes de budget, de retard et de validation.

## Build intégré à Spring Boot

Le backend utilise Spring Boot 4 et nécessite un JDK 21 (configuré dans
`JAVA_HOME`).

```powershell
npm run build
cd ..\backend
.\mvnw.cmd package
```

Le build Angular est copié dans les ressources du JAR Spring Boot. Les routes
API `/api/**` restent celles du backend existant.
