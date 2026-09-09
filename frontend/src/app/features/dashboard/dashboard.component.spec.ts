import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [provideZonelessChangeDetection()]
    }).compileComponents();
  });

  it('creates the dashboard', () => {
    const fixture = TestBed.createComponent(DashboardComponent);

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('displays the dashboard title and subtitle', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Dashboard');
    expect(fixture.nativeElement.textContent).toContain('principales informations');
  });

  it('displays the main statistics cards', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Employees');
    expect(fixture.nativeElement.textContent).toContain('Customers');
    expect(fixture.nativeElement.textContent).toContain('Departments');
    expect(fixture.nativeElement.textContent).toContain('Invoices');
    expect(fixture.nativeElement.textContent).toContain('€86.4K');
  });

  it('displays recent activities and summary indicators', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Recent activities');
    expect(fixture.nativeElement.textContent).toContain('New employee profile added');
    expect(fixture.nativeElement.textContent).toContain('Invoices pending');
    expect(fixture.nativeElement.textContent).toContain('Active customers');
  });
});