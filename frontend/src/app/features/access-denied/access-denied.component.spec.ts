import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AccessDeniedComponent } from './access-denied.component';

describe('AccessDeniedComponent', () => {
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    await TestBed.configureTestingModule({
      imports: [AccessDeniedComponent],
      providers: [
        provideZonelessChangeDetection(),
        { provide: Router, useValue: router }
      ]
    }).compileComponents();
  });

  it('creates the access denied page', () => {
    const fixture = TestBed.createComponent(AccessDeniedComponent);

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('displays the access denied message', () => {
    const fixture = TestBed.createComponent(AccessDeniedComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Accès refusé');
    expect(fixture.nativeElement.textContent).toContain('Vous n\'avez pas les autorisations nécessaires');
  });

  it('navigates to the dashboard', () => {
    const fixture = TestBed.createComponent(AccessDeniedComponent);
    fixture.detectChanges();

    fixture.nativeElement.querySelector('button').click();

    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});