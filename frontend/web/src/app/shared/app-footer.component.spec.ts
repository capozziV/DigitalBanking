import { TestBed } from '@angular/core/testing';
import { AppFooterComponent } from './app-footer.component';

describe('AppFooterComponent', () => {
  it('shows the copyright line', async () => {
    const fixture = TestBed.createComponent(AppFooterComponent);
    await fixture.whenStable();
    const footer = (fixture.nativeElement as HTMLElement).querySelector('footer');
    expect(footer?.textContent?.trim()).toBe('© 2026 capzz.labs');
  });
});
