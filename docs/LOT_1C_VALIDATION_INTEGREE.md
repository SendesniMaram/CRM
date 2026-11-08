# Lot 1C — Protocole de validation intégrée différée

Ce protocole est préparé, **non exécuté**. Utiliser une pile isolée, bornée en mémoire, avec données fictives et volumes distincts. Un démarrage Flyway peut appliquer les migrations : il ne fait pas partie des opérations réalisées pendant ce lot.

## Préparer le canal HTTPS HR → Employee

La cause statique identifiée est l'absence d'activation et de matériel cryptographique dans le Compose courant. Les composants existants sont désactivés par défaut et renvoient une indisponibilité ; aucun certificat défectueux en environnement réel n'a été observé pendant ce lot.

L'overlay optionnel [docker-compose.personal-spaces.yml](../docker-compose.personal-spaces.yml) reprend les propriétés déjà utilisées dans `scripts/e2e/demo-model.mjs`. Il ne remplace pas le fichier Compose principal et ne publie pas le port 8443 sur l'hôte. Il doit être combiné avec la configuration de validation isolée lors d'une phase ultérieure autorisée. Il ne définit pas à lui seul l'isolation des bases, les sauvegardes ou les limites mémoire.

Définir `CRM_PROFILE_MATERIAL_DIR` vers un répertoire externe absolu contenant :

| Fichier externe | Exigence |
|---|---|
| `employee-tls.p12` | PKCS12 RSA, alias `employee`, certificat valide avec SAN DNS `employee-service` et sa chaîne approuvée |
| `employee-tls-password` | Mot de passe du PKCS12 dans un fichier lisible uniquement par les opérateurs/services autorisés |
| `employee-tls.pem` | Certificat X.509 approuvé ou autorité de confiance correspondante, sans clé privée |
| `hr-signing-private.pem` | Clé RSA dédiée d'au moins 2048 bits, PEM PKCS#8 ; uniquement accessible à HR |
| `hr-signing-public.pem` | Clé publique correspondante ; uniquement nécessaire à Employee |

Tous les montages sont en lecture seule. Le `kid` `profile` doit correspondre des deux côtés. Le JWT de service est distinct du JWT utilisateur, limité à 30 secondes, à l'employé et à la permission de lecture du résumé. Aucun secret n'a été généré ou lu pendant le lot. Si une autre couche définit déjà `SPRING_APPLICATION_JSON`, fusionner explicitement ses propriétés avant utilisation : cette variable n'est pas fusionnée automatiquement entre overlays.

Vérifier ultérieurement la résolution DNS, la chaîne de confiance, le SAN, les droits des fichiers et la synchronisation des horloges. Un certificat expiré, inconnu ou pour un autre hôte doit échouer. Les redirections ne doivent pas transférer le credential ; les API internes doivent rester inaccessibles via Gateway et le port HTTP public. Ne jamais ajouter `trust-all`, désactiver le hostname verifier ou utiliser HTTP à la place de HTTPS.

## Migration documentaire

Livrer ensemble le modèle HR et `V20261101_02__employee_document_visibility.sql`. Sur une copie sauvegardée, vérifier Flyway et la validation JPA, installation neuve et upgrade. La colonne `employee_visible` est obligatoire et vaut faux par défaut. Vérifier que tous les documents historiques restent privés, que leurs métadonnées/fichiers restent intacts et que l'administrateur peut partager puis retirer un document. Aucune migration historique n'est à modifier.

## Scénarios à rejouer via le vrai Gateway

Préparer deux comptes EMPLOYEE E1/E2 et deux CLIENT C1/C2, actifs, non verrouillés, avec associations distinctes ; un compte sans association ; un compte désactivé ; un ADMIN et un SUPER_ADMIN. Les connexions doivent produire les claims `uid` et `sid` du Lot 1B.

| Cas | Résultat attendu |
|---|---|
| Association employé/client valide, puis répétition identique | Création, puis réponse idempotente ; une seule association persistée |
| Association vers une entité absente, compte inactif/verrouillé ou mauvais rôle | Refus selon contrat ; aucune écriture partielle |
| Deux créations simultanées pour le même UID avec deux clients différents | Une réussite et un conflit 409 ; aucun remplacement du gagnant |
| Deux UID simultanés pour le même client/employé | Une association persistée ; conflit sur l'autre |
| Annuaire Identity/Employee indisponible ou réponse invalide | 503/502 selon contrat, sans association persistée ni diagnostic technique public |
| E1 ouvre `/profile`, `/leaves`, `/my-payroll`, `/my-documents` | Résumé de E1, congés propres, bulletins publiés propres, seuls documents propres et partagés |
| E1 soumet un congé puis admin décide | Demande persistée et statut visible au rechargement ; refus d'accès/modification par E2 |
| E1 cible un bulletin ou un document de E2 | Refus ; aucun octet ni métadonnée privée renvoyé |
| Document de E1 privé ou partage retiré | Absent de la liste ; téléchargement personnel 404 |
| Liste/téléchargement sans association | Erreur explicite ; aucune recherche par username/e-mail ou ID fourni par Angular |
| Query `employeeId`, `customerId` ou `userId` sur les nouvelles API personnelles | 400 ; aucun changement de périmètre |
| C1 ouvre profil, factures, détail, paiements, PDF et dashboard | Uniquement ses données ; statistiques conformes au contrat Lot 1A, montants textuels conservés pour les paiements |
| C1 cible une facture/paiements/PDF de C2 | 404 ; aucune lecture des paiements de C2 |
| Aucune facture, aucun paiement ou aucun document partagé | États vides distincts d'une indisponibilité |
| Logout, révocation de rôle ou désactivation pendant une lecture | Ancien token refusé après commit ; interface nettoyée, aucune réponse tardive ne rétablit les données |
| Fichier absent, trop grand, lien symbolique ou référence hors stockage | Téléchargement refusé sans chemin physique dans la réponse |

Enregistrer les réponses HTTP sans token ni corps personnel dans les journaux de validation. Vérifier `Cache-Control: no-store`, téléchargement en pièce jointe, contrôle MIME et l'absence de clé de stockage/notes internes dans les DTO personnels.

Les tests unitaires de conflits et les lectures séquentielles ne démontrent pas les contraintes MySQL effectives, l'attente des verrous, les rollbacks ou les effets d'un cache JPA. Vérifier ces points avec des transactions concurrentes sur la copie isolée ; inclure rollback et reprise après panne. Les suites d'intégration existantes Customer/EmployeeAssociation/EmployeeProfile/PersonalInvoice/PersonalPayroll restent à exécuter dans cette phase, après vérification de leurs profils et de leurs cibles de base.
