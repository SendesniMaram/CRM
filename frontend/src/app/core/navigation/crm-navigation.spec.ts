import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { CrmNavigation, CRM_NAVIGATION } from './crm-navigation';
import { CrmRole } from '../models/role.model';
import { AuthService } from '../services/auth.service';
import { safeReturnUrl } from '../services/auth-navigation';

describe('shared CRM navigation', () => {
  for(const role of Object.values(CrmRole)) {
    it('uses implemented routes and a consistent action policy for '+role, () => {
      TestBed.configureTestingModule({providers:[provideZonelessChangeDetection(),{provide:AuthService,useValue:{hasAnyRole:(roles:CrmRole[])=>roles.includes(role)}}]});
      const nav=TestBed.inject(CrmNavigation);
      for(const item of nav.items()) {expect(item.roles).toContain(role);expect(safeReturnUrl(item.route)).toBe(item.route);}
      for(const action of nav.actions()) {expect(action.roles).toContain(role);expect(safeReturnUrl(action.route)).toBe(action.route);}
      expect(nav.groups().flatMap(group=>group.items)).toEqual(nav.items());
    });
  }
  it('keeps distinct routes except for the existing HR association fragment', () => {
    const destinations=CRM_NAVIGATION.map(item=>item.route+(item.fragment?'#'+item.fragment:''));expect(new Set(destinations).size).toBe(destinations.length);
  });
});
