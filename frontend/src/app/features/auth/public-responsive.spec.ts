import { provideZonelessChangeDetection, Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LandingComponent } from '../landing/landing.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';

describe('Public page responsive layouts', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection(), provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
  });
  afterEach(() => { TestBed.inject(HttpTestingController).verify(); sessionStorage.clear(); });

  for (const page of [
    { component: LandingComponent as Type<unknown>, grid: '.hero-inner', name: 'landing' },
    { component: LoginComponent as Type<unknown>, grid: '.auth-shell', name: 'login' },
    { component: RegisterComponent as Type<unknown>, grid: '.auth-shell', name: 'register' }
  ]) {
    for (const width of [375, 768, 1440]) {
      it(`lays out ${page.name} at ${width}px without horizontal overflow`, async () => {
        const fixture = TestBed.createComponent(page.component);
        fixture.detectChanges(); await fixture.whenStable();
        // A same-origin iframe gives CSS media queries a real viewport. No server or API.
        const frame = document.createElement('iframe');
        frame.style.cssText = `position:absolute;left:-10000px;top:0;width:${width}px;height:1000px;border:0;visibility:hidden`;
        document.body.appendChild(frame);
        try {
          const doc = frame.contentDocument!;
          const reset = doc.createElement('style');
          reset.textContent = 'html,body{margin:0;width:100%}'; doc.head.appendChild(reset);
          document.querySelectorAll('style').forEach(style => doc.head.appendChild(style.cloneNode(true)));
          doc.body.appendChild(fixture.nativeElement.cloneNode(true));
          const grid = doc.querySelector<HTMLElement>(page.grid)!;
          const columns = frame.contentWindow!.getComputedStyle(grid).gridTemplateColumns.split(' ');
          expect(columns.length).withContext('responsive column count').toBe(width <= (page.name === 'landing' ? 900 : 700) ? 1 : 2);
          expect(doc.documentElement.scrollWidth).withContext('horizontal page overflow').toBeLessThanOrEqual(width + 1);
        } finally { frame.remove(); }
      });
    }
  }
});
