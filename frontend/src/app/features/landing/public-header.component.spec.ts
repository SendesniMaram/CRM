import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PublicHeaderComponent } from './public-header.component';

describe('Public header mobile navigation', () => {
  beforeEach(() => TestBed.configureTestingModule({
    imports: [PublicHeaderComponent],
    providers: [provideZonelessChangeDetection(), provideRouter([])]
  }));

  it('closes the menu without cancelling the anchor navigation', () => {
    const fixture = TestBed.createComponent(PublicHeaderComponent);
    fixture.detectChanges();
    const menu = fixture.nativeElement.querySelector('details') as HTMLDetailsElement;
    menu.open = true;
    let cancelledBeforeBrowserNavigation: boolean | undefined;
    fixture.nativeElement.addEventListener('click', (event: MouseEvent) => {
      cancelledBeforeBrowserNavigation = event.defaultPrevented;
      event.preventDefault(); // Keep this unit test on the Karma page.
    });
    menu.querySelector('a')!.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
    expect(cancelledBeforeBrowserNavigation).toBeFalse();
    expect(menu.open).toBeFalse();
  });

  it('closes the menu with Escape and restores focus to its trigger', () => {
    const fixture = TestBed.createComponent(PublicHeaderComponent);
    fixture.detectChanges();
    const menu = fixture.nativeElement.querySelector('details') as HTMLDetailsElement;
    menu.open = true;
    const focus = spyOn(menu.querySelector('summary')!, 'focus');
    menu.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    expect(menu.open).toBeFalse();
    expect(focus).toHaveBeenCalled();
  });
});
