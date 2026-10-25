import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
@Component({ selector: 'app-landing-faq', standalone: true, imports: [MatIconModule], templateUrl: './landing-faq.component.html', styleUrl: './landing-faq.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class LandingFaqComponent {
  protected readonly questions = [
    { title: 'À qui s’adresse cet espace CRM ?', text: 'Aux équipes qui souhaitent réunir leur organisation, leurs ressources humaines et leur suivi client. Les accès diffèrent selon le rôle : administrateur, employé ou client.' },
    { title: 'Tous les utilisateurs voient-ils les mêmes informations ?', text: 'Non. Les modules et les données accessibles dépendent des autorisations du compte. Un employé retrouve notamment ses propres demandes de congé ; les fonctions de gestion restent réservées aux rôles autorisés.' },
    { title: 'Puis-je créer un compte ?', text: 'Oui, l’inscription existante permet de créer un compte client. Elle ne donne pas de droits d’administration ni d’accès automatique aux modules internes. Pour un accès employé, rapprochez-vous de votre administrateur.' },
    { title: 'L’aperçu présente-t-il les données de mon entreprise ?', text: 'Non. L’aperçu de cette page utilise uniquement des données fictives. Vos informations réelles sont disponibles après connexion, dans la limite des permissions de votre compte.' }
  ];
}
