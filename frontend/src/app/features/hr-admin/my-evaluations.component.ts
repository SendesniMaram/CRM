import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

interface Evaluation { id: number; periodStart: string; periodEnd: string; criteria: string | null; comments: string | null; status: string; }
interface Page<T> { content: T[]; totalPages: number; number: number; }

@Component({ selector: 'app-my-evaluations', standalone: true, imports: [DatePipe], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<main class="container py-4"><h1>Mes évaluations</h1>
    @if (loading()) { <p role="status">Chargement des évaluations…</p> }
    @if (error()) { <p role="alert" class="text-danger">{{ error() }}</p> }
    @if (!loading() && !error() && evaluations().length===0) { <p>Aucune évaluation finalisée n’est disponible.</p> }
    @if (evaluations().length) { <ul class="list-group">@for (item of evaluations(); track item.id) {
      <li class="list-group-item"><button class="btn btn-link p-0" (click)="select(item)">Période du {{ item.periodStart | date:'mediumDate' }} au {{ item.periodEnd | date:'mediumDate' }}</button></li>
    }</ul> }
    @if (selected(); as item) { <section class="card mt-3"><div class="card-body"><h2>Détail de l’évaluation</h2>
      <p><strong>Critères</strong></p><p>{{ item.criteria || 'Aucun critère renseigné.' }}</p>
      <p><strong>Commentaires</strong></p><p>{{ item.comments || 'Aucun commentaire.' }}</p></div></section> }
    @if (pages()>1) { <div class="d-flex gap-2 mt-3"><button class="btn btn-outline-primary" [disabled]="page()===0 || loading()" (click)="load(page()-1)">Précédent</button><span>Page {{ page()+1 }} / {{ pages() }}</span><button class="btn btn-outline-primary" [disabled]="page()+1>=pages() || loading()" (click)="load(page()+1)">Suivant</button></div> }
  </main>` })
export class MyEvaluationsComponent {
  private readonly http=inject(HttpClient); private readonly url='http://localhost:8080/hr/api/evaluations/mine';
  protected readonly evaluations=signal<Evaluation[]>([]); protected readonly selected=signal<Evaluation|null>(null);
  protected readonly loading=signal(false); protected readonly error=signal(''); protected readonly page=signal(0); protected readonly pages=signal(0);
  constructor(){this.load(0);}
  protected load(page:number){this.loading.set(true);this.error.set('');this.selected.set(null);this.http.get<Page<Evaluation>>(this.url,{params:{page,size:10}}).subscribe({
    next:r=>{this.evaluations.set(r.content);this.page.set(r.number);this.pages.set(r.totalPages);this.loading.set(false);},
    error:(e:HttpErrorResponse)=>{this.loading.set(false);this.evaluations.set([]);this.error.set(e.status===401?'Votre session a expiré.':e.status===403?'Cette page est réservée aux employés.':e.status===404?'Aucune association employé n’est configurée pour ce compte.':'Impossible de charger vos évaluations. Réessayez plus tard.');}
  });}
  protected select(item:Evaluation){this.selected.set(item);}
}
