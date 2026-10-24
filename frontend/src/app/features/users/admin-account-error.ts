/** Display only fixed messages; dependency bodies may contain private diagnostics. */
export function adminAccountError(error: { status?: number } | null | undefined, operation: 'account' | 'association'): string {
  switch (error?.status) {
    case 400: return operation === 'account' ? 'Vérifiez les champs obligatoires et leur format.'
      : 'Référence invalide ou association déjà enregistrée. Vérifiez les comptes et les fiches sélectionnés.';
    case 409: return operation === 'account' ? 'Ce nom d’utilisateur ou cet e-mail existe déjà. Consultez la liste avant de réessayer.'
      : 'Réaffectation refusée : employé déjà associé ou historique de congés existant. Rechargez les associations.';
    case 401: return 'Authentification invalide. Reconnectez-vous si nécessaire.';
    case 403: return 'Vous ne disposez pas des autorisations nécessaires.';
    case 404: return 'Le compte, la fiche employé ou l’association n’existe plus. Rechargez les données.';
    case 502: return 'La réponse d’un service est invalide. Rechargez les données avant de réessayer.';
    case 503: return 'Un service est temporairement indisponible. Réessayez plus tard.';
    default: return 'La demande n’a pas pu être confirmée. Rechargez les données avant de réessayer.';
  }
}
