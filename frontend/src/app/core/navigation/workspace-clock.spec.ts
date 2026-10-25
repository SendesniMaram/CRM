import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { WorkspaceClock, accountInitials, workspaceGreeting } from './workspace-clock';

describe('workspace presentation', () => {
  it('uses local time for greetings without inventing identity data', () => {
    expect(workspaceGreeting(new Date(2026, 9, 16, 9))).toBe('Bonjour');
    expect(workspaceGreeting(new Date(2026, 9, 16, 18))).toBe('Bonsoir');
    expect(workspaceGreeting(new Date(2026, 9, 16, 3))).toBe('Bonsoir');
    expect(accountInitials('Élodie Ben Ali')).toBe('ÉB');
    expect(accountInitials('unit-user')).toBe('UU');
    expect(accountInitials('')).toBe('?');
  });
  it('refreshes the date and clears its timer when the application is destroyed', () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
    jasmine.clock().install();
    try {
      jasmine.clock().mockDate(new Date(2026, 11, 31, 23, 59, 30));
      const clock = TestBed.inject(WorkspaceClock);
      expect(clock.now().getFullYear()).toBe(2026);
      jasmine.clock().tick(60_000);
      expect(clock.now().getFullYear()).toBe(2027);
      const value = clock.now();
      TestBed.resetTestingModule();
      jasmine.clock().tick(60_000);
      expect(clock.now()).toBe(value);
    } finally { jasmine.clock().uninstall(); }
  });
});
