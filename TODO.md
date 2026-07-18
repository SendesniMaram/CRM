# TODO - Finalisation CRM (microservices)

## Étape 1 — Analyse
- [x] Lire `pom.xml` et `application.yml` des services : discovery/gateway/identity
- [ ] Mettre en évidence incohérences potentielles Spring Boot / Spring Cloud / Eureka / Gateway

## Étape 2 — Validation du build
- [x] Lancer `mvn test` sur discovery-service
- [x] Lancer `mvn test` sur identity-service
- [x] Lancer `mvn test` sur gateway-service

## Étape 3 — Nettoyage léger (sans changer la fonctionnalité)
- [ ] Supprimer imports inutiles
- [ ] Supprimer commentaires inutiles (sauf `GatewayServiceSmokeTest` déjà commenté)
- [ ] Supprimer fichiers temporaires éventuels (`target/`)

## Étape 4 — README.md professionnel
- [ ] Générer `README.md` avec : présentation, architecture, structure, technologies, description services, ordre de démarrage, URL

## Étape 5 — Audit final
- [ ] Récupérer la liste des fichiers modifiés
- [ ] Résumer incohérences corrigées
- [ ] Confirmer statut prêt pour versionner sur Git

