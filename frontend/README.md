# Frontend Angular

Le frontend utilise Angular standalone et Angular Material. Les appels `/api`
sont envoyés au backend Spring Boot sur `http://localhost:8080`.

## Développement

1. Démarrer PostgreSQL et Spring Boot depuis `backend`.
2. Depuis ce dossier, installer les dépendances puis démarrer Angular :

```powershell
npm ci
npm start
```

Ouvrir ensuite `http://localhost:4200`.

## Build intégré à Spring Boot

```powershell
npm run build
cd ..\backend
.\mvnw.cmd package
```

Le build Angular est copié dans les ressources du JAR Spring Boot. Les routes
API `/api/**` restent celles du backend existant.
