# Sprint 2A — Lot 2A.1 : contrat d'annuaire multi-rôles

> Archive documentaire : les constats, chiffres, anciens commits et autorisations ci-dessous décrivent la campagne datée du rapport. Ils ne constituent pas un état courant ni un plan à réappliquer sur `publication-clean`. Toute opération réelle nécessite une nouvelle vérification et l’autorisation adaptée.

Date : 19 octobre 2026, Africa/Tunis. Branche `microservices`.

## 1. Périmètre

Contrat de consultation Identity et consommateurs d'annuaire HR uniquement. Références consultées : feuille de route des espaces par rôle, audit fonctionnel, rapport Sprint 1B lot F, modèles/DTO/mapper Identity, associations HR, clients HTTP et configurations de sécurité existantes.

Aucun début du lot 2A.2 : pas de page Angular, pas de nouvelle API administrative, pas de création/attribution libre de rôles et pas d'association automatique. Aucun changement de schéma, MySQL réel, Docker, .env, secret, déploiement, commit ou push.

## 2. Fichiers modifiés et créés

Production modifiée :

- `microservices/identity-service/src/main/java/com/crm/identity/dto/UserResponse.java` : roles et accountNonLocked ajoutés au DTO d'annuaire.
- `microservices/identity-service/src/main/java/com/crm/identity/service/impl/IdentityServiceImpl.java` : projection complète, déterministe et dédupliquée ; stabilisation de roleType.
- `microservices/hr-service/src/main/java/com/crm/hr/controller/LeaveAssociationController.java` : éligibilité d'après roles et état ; tableaux d'options vérifiés.

Production créée :

- `microservices/hr-service/src/main/java/com/crm/hr/client/DirectoryAccount.java` : validation du contrat et calcul d'éligibilité partagé entre options, création et réaffectation.

Tests modifiés :

- `microservices/identity-service/src/test/java/com/crm/identity/AdminEmployeeCreationTest.java` : matrice de lecture annuaire pour les quatre rôles.
- `microservices/hr-service/src/test/java/com/crm/hr/LeaveSecurityIntegrationTest.java` : fixtures d'annuaire conformes au nouveau contrat, assertions de propriété conservées.
- `microservices/hr-service/src/test/java/com/crm/hr/LeaveAssociationReassignmentTest.java` : fixtures et tests multi-rôles, état, protocole, erreurs et absence d'écriture.

Tests créés :

- `microservices/identity-service/src/test/java/com/crm/identity/service/impl/DirectoryRolesTest.java`.
- `microservices/hr-service/src/test/java/com/crm/hr/client/DirectoryAccountTest.java`.

Les configurations Spring Security, entités, repositories, clients HTTP, POM et common-security ne sont pas modifiés. Les modifications antérieures de ces fichiers/modules ne sont pas attribuées à ce lot.

Contrôle final SHA-256 : sur les 177 fichiers déjà modifiés/non suivis au début, 175 restent identiques ; seuls les deux tests HR d'association/propriété cités ci-dessus sont étendus avec conservation de leurs scénarios existants. Les autres éditions portent sur quatre fichiers auparavant propres ; trois fichiers Java et ce rapport sont créés. Aucun fichier supprimé.

## 3. Contrat JSON avant/après

Les GET `/api/users` (tableau) et `/api/users/{id}` (objet) utilisent le même DTO. Exemple synthétique avec ADMIN et EMPLOYEE :

Avant, choix du rôle dépendant de l'itération du Set :

```json
{
  "id": 42,
  "firstName": "Test",
  "lastName": "Directory",
  "username": "directory-user",
  "email": "directory@example.test",
  "phone": null,
  "enabled": true,
  "roleType": "ADMIN",
  "createdAt": null
}
```

Après :

```json
{
  "id": 42,
  "firstName": "Test",
  "lastName": "Directory",
  "username": "directory-user",
  "email": "directory@example.test",
  "phone": null,
  "enabled": true,
  "roleType": "ADMIN",
  "roles": ["ADMIN", "EMPLOYEE"],
  "accountNonLocked": true,
  "createdAt": null
}
```

Ordre explicite des rôles : **SUPER_ADMIN, ADMIN, EMPLOYEE, CLIENT**. La projection sélectionne chaque type réellement présent une seule fois, sans dépendre de l'ordre JPA/HashSet ou de l'ordinal de l'enum. Elle ne modifie pas la collection persistée. roles est une liste JSON, sans préfixe ROLE_. Les comptes sans rôle ont roles=[] et roleType=null.

`roleType` reste un champ chaîne ou null pour rétrocompatibilité structurelle ; pour un compte mono-rôle, sa valeur est inchangée. Pour un compte multi-rôle, sa valeur devient le premier rôle de l'ordre documenté : elle peut donc différer de l'ancien choix arbitraire. Les nouveaux consommateurs doivent utiliser roles. L'ordre d'apparition des propriétés JSON n'est pas un contrat.

`enabled` existait ; `accountNonLocked` est ajouté afin que HR puisse refuser aussi les références de comptes verrouillés sans nouvel appel administratif ni colonne. Aucun mot de passe, refresh token ou secret n'est ajouté au DTO. LoginResponse, RefreshTokenResponse, émission JWT et contrats Login/Refresh ne sont pas modifiés.

## 4. Comportement HR

Options, création et réaffectation utilisent le même prédicat : membre EMPLOYEE dans roles **et** enabled=true **et** accountNonLocked=true. Le roleType historique n'est plus consulté. HR ne transforme pas ces rôles d'annuaire en autorités de l'appelant : les permissions restent issues de son authentification JWT.

- ADMIN+EMPLOYEE, quel que soit l'ordre reçu : référence éligible ; aucune promotion de l'appelant.
- CLIENT ou roles=[] : non éligible ; exclu des options, association/réaffectation refusée avec le 400 métier existant.
- Compte disabled/locked : même refus ; une association existante n'est ni effacée ni réaffectée.
- roles absent/non tableau, rôle null/non textuel/inconnu, état absent/non booléen ou objet de compte invalide : 502 générique, sans fallback roleType.
- Collections d'options Identity ou Employee non tableaux : 502 plutôt qu'une liste vide trompeuse.
- Refus distant 401/403 : propagation existante conservée ; panne/indisponibilité 503 conservée. Le client HTTP existant et ses autres correspondances de statut ne changent pas.

Les doublons de rôles reçus par HR n'augmentent aucun privilège ; le prédicat d'appartenance reste identique. Identity n'en émet pas. Le DTO d'annuaire ne constitue pas une nouvelle autorisation de lecture publique.

Les vérifications d'ID, username, existence des références, unicité et congés empêchant la réaffectation restent en place. Les propriétés des demandes/PDF et les transactions existantes ne changent pas. La relecture a constaté que le contrôleur n'utilisait auparavant pas les flags du compte cible pour son éligibilité ; ce lot rend ces refus explicites et testés, sans changer le RBAC de l'appelant.

## 5. Tests ajoutés et validations

Identity : ordre et déduplication sur deux ordres d'entrée, mono-EMPLOYEE, compte sans EMPLOYEE, compte sans rôle, flags actuels, JSON legacy et absence de credentials, collection persistée inchangée, annuaire accessible seulement ADMIN/SUPER_ADMIN.

HR : EMPLOYEE seul/multi-rôles/ordres différents/doublons ; sans EMPLOYEE ; disabled/locked ; pas de fallback legacy ; types et rôles malformés ; options non tableaux ; création et réaffectation multi-rôles ; absence d'écriture sur refus 401/403/503 Identity ou Employee. Les suites complètes reprennent les droits, la propriété, les PDF, conflits/historique, la validation centralisée Sprint 1B et les contrats d'authentification.

Commandes réellement exécutées, depuis la racine :

```powershell
mvn -B "-Dmaven.repo.local=C:\Users\ASUS\.m2\repository" -f microservices/identity-service/pom.xml "-Dtest=DirectoryRolesTest,AdminEmployeeCreationTest,UidIssuanceIntegrationTest,SessionSecurityIntegrationTest" test
mvn -B "-Dmaven.repo.local=C:\Users\ASUS\.m2\repository" -f microservices/hr-service/pom.xml "-Dtest=DirectoryAccountTest,LeaveAssociationReassignmentTest,LeaveSecurityIntegrationTest,LeaveDirectoryClientTest,CentralizedJwtSecurityTest" test
mvn -B "-Dmaven.repo.local=C:\Users\ASUS\.m2\repository" -f microservices/identity-service/pom.xml test
mvn -B "-Dmaven.repo.local=C:\Users\ASUS\.m2\repository" -f microservices/hr-service/pom.xml test
```

Exécutions séquentielles pour contenir la mémoire, cache Maven local, H2 et dépendances HTTP simulées. Journaux ciblés `target/sprint2a-lot1-targeted.log`, puis complets `target/sprint2a-lot1-regression.log` de chaque module. Les chiffres des campagnes ciblées et complètes ne s'additionnent pas.

| Campagne | Tests | Échecs | Erreurs | Ignorés | Résultat |
|---|---:|---:|---:|---:|---|
| Identity ciblé | 49 | 0 | 0 | 0 | BUILD SUCCESS |
| HR ciblé initial | 102 | 0 | 0 | 0 | BUILD SUCCESS |
| Identity complet final | 115 | 0 | 0 | 0 | BUILD SUCCESS |
| HR complet final | 217 | 0 | 0 | 0 | BUILD SUCCESS |
| **Total final distinct des deux modules** | **332** | **0** | **0** | **0** | **Succès** |

La campagne ciblée HR précède six cas supplémentaires et la vérification complète de création multi-rôle, tous inclus dans la régression finale. Les deux suites finales ajoutent 38 cas par rapport aux 105 Identity + 189 HR du lot F : 10 Identity et 28 HR. DirectoryRolesTest compte 6 cas ; DirectoryAccountTest 17 ; la matrice de permissions Identity ajoute 4 cas et les scénarios d'association HR 11. Les campagnes ciblées ne s'ajoutent pas aux 332 résultats finaux.

Aucune erreur de compilation ni aucun échec de test rencontré dans les campagnes exécutées. Les avertissements de configuration de test/H2 et LF/CRLF ne sont pas masqués ; aucun paramètre de sécurité n'est relâché pour obtenir ces résultats.

`git diff --check` sur les fichiers suivis concernés réussit, avec avertissements LF/CRLF. Aucun test Angular n'est lancé : aucun fichier Angular ne change. Aucune régression des sept autres consommateurs n'est requise par un changement de common-security, car cette bibliothèque ne change pas ; leur futur E2E reste distinct.

## 6. Risques et limites

Identity doit être mis à jour avant HR lors d'une future livraison approuvée : HR exige roles et accountNonLocked. Un ancien Identity produit désormais un 502 sur ces opérations d'annuaire, jamais un accès permis par fallback. Les anciens consommateurs ignorant les champs additionnels peuvent continuer à utiliser roleType, avec la stabilisation multi-rôle indiquée ci-dessus.

Le contrat accepte les quatre types actuellement connus ; un futur type exige une évolution coordonnée de la validation HR. L'annuaire reste non paginé. L'association reste locale aux congés et indexée par username ; aucune association globale User–Employee/Customer n'est créée.

Les états sont lus à l'instant de consultation de l'annuaire, sans atomicité distribuée avec la création de liaison HR. Ce contrôle ne remplace pas la validation JWT à chaque requête ni une future commande atomique statut/révocation. Les données et scénarios de test sont synthétiques ; H2/MockMvc et doubles HTTP ne prouvent pas Docker, Eureka ou MySQL réel.

L'E2E réel Sprint 1B reste en attente de capacité dédiée. Aucun verdict de conformité globale ou de livraison n'est déduit de ce lot.

## 7. Prérequis du lot 2A.2

Validation utilisateur de ce lot et du contrat enrichi ; accord sur l'utilisation de roles par les nouveaux modèles Angular ; prévoir les erreurs 400 métier, 401/403, 502 protocole et 503 disponibilité ; utiliser des IDs explicites pour les liaisons ; conserver les contraintes de réaffectation et les comptes séparés des fiches Employee. La création de page utilisateurs/association ne commence pas dans ce lot.

**VERDICT FINAL : PASS pour le lot 2A.1 et ses validations isolées. E2E réel et déploiement non validés. Arrêt après ce lot dans l'attente de validation utilisateur.**
