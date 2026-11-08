package com.crm.identity;

import com.crm.identity.dto.AdminUserRolesRequest;
import com.crm.identity.entity.*;
import com.crm.identity.enums.RoleType;
import com.crm.identity.repository.*;
import com.crm.identity.service.impl.IdentityServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsableSuperAdminPolicyTest {
    final UserRepository users = mock(UserRepository.class);
    final RoleRepository roles = mock(RoleRepository.class);
    final RefreshTokenRepository sessions = mock(RefreshTokenRepository.class);
    final EntityManager em = mock(EntityManager.class);
    final IdentityServiceImpl service = new IdentityServiceImpl(users, roles, mock(PasswordEncoder.class), sessions, em);
    final Map<Long, User> accounts = new HashMap<>();
    User first, second, admin;
    User account(long id, RoleType type) {
        var user = new User(); user.setId(id); user.setUsername("synthetic-" + id); user.setEmail(id + "@example.test");
        user.getRole().add(new Role(type.name(), type)); accounts.put(id,user); return user;
    }
    @BeforeEach void setup() {
        first = account(1, RoleType.SUPER_ADMIN); second = account(2, RoleType.SUPER_ADMIN); admin = account(3, RoleType.ADMIN);
        when(roles.lockByRoleType(RoleType.SUPER_ADMIN)).thenReturn(Optional.of(new Role("SUPER_ADMIN",RoleType.SUPER_ADMIN)));
        when(roles.findByRoleType(any())).thenAnswer(i -> Optional.of(new Role(i.<RoleType>getArgument(0).name(),i.getArgument(0))));
        when(users.lockById(anyLong())).thenAnswer(i -> Optional.ofNullable(accounts.get(i.getArgument(0))));
        when(users.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(users.countUsableSuperAdmins()).thenAnswer(i -> accounts.values().stream().filter(u -> u.isEnabled() && u.isAccountNonLocked()
                && u.getRole().stream().anyMatch(r -> r.getRoleType() == RoleType.SUPER_ADMIN)).count());
    }
    AdminUserRolesRequest employee() { return new AdminUserRolesRequest(Set.of(RoleType.EMPLOYEE)); }
    @Test void disabledOrLockedSecondarySuperAdminDoesNotProtectTheLastUsableOne() {
        second.setEnabled(false);
        assertThrows(IllegalStateException.class, () -> service.updateUserRoles(1L,employee(),first));
        second.setEnabled(true); second.setAccountNonLocked(false);
        assertThrows(IllegalStateException.class, () -> service.updateUserRoles(1L,employee(),first));
        verifyNoInteractions(sessions);
    }
    @Test void autoDeactivationIsAlwaysForbidden() {
        assertThrows(IllegalArgumentException.class, () -> service.updateUserStatus(1L,false,first));
        assertTrue(first.isEnabled()); verifyNoInteractions(sessions);
    }
    @Test void multipleUsableSuperAdminsAllowDeactivationAndRoleRemovalButNeverTheLastOne() {
        service.updateUserStatus(2L,false,first); assertFalse(second.isEnabled());
        assertThrows(IllegalStateException.class, () -> service.updateUserRoles(1L,employee(),first));
        second.setEnabled(true);
        service.updateUserRoles(2L,employee(),first);
        assertThrows(IllegalStateException.class, () -> service.updateUserRoles(1L,employee(),first));
        assertTrue(first.getRole().stream().anyMatch(r -> r.getRoleType() == RoleType.SUPER_ADMIN));
    }
    @Test void ordinaryAdminsCannotManagePrivilegedAccounts() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.updateUserStatus(1L,false,admin));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.updateUserRoles(2L,employee(),admin));
        verifyNoInteractions(sessions);
    }
    @Test void sharedRoleLockPrecedesActorAndTargetReloadAndInvariantCheck() {
        service.updateUserRoles(2L,employee(),first);
        var order = inOrder(roles,users,em,sessions);
        order.verify(roles).lockByRoleType(RoleType.SUPER_ADMIN);
        order.verify(users).lockById(1L); order.verify(em).refresh(first,LockModeType.PESSIMISTIC_WRITE);
        order.verify(users).lockById(2L); order.verify(em).refresh(second,LockModeType.PESSIMISTIC_WRITE);
        order.verify(users).countUsableSuperAdmins(); order.verify(sessions).revokeAllByUser(second);
    }
    @Test void failedSharedLockStopsAllAccountAndSessionMutations() {
        when(roles.lockByRoleType(RoleType.SUPER_ADMIN)).thenThrow(new org.springframework.dao.CannotAcquireLockException("synthetic lock failure"));
        assertThrows(org.springframework.dao.CannotAcquireLockException.class, () -> service.updateUserRoles(2L,employee(),first));
        verify(users,never()).saveAndFlush(any()); verifyNoInteractions(sessions);
    }
    @Test void persistenceFailurePropagatesAndWritesDeclareTransactionalRollback() throws Exception {
        when(users.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("synthetic"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> service.updateUserStatus(2L,false,first));
        assertNotNull(IdentityServiceImpl.class.getMethod("updateUserStatus",Long.class,boolean.class,User.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class));
        assertNotNull(IdentityServiceImpl.class.getMethod("updateUserRoles",Long.class,AdminUserRolesRequest.class,User.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class));
        assertEquals(LockModeType.PESSIMISTIC_WRITE, RoleRepository.class.getMethod("lockByRoleType",RoleType.class)
                .getAnnotation(org.springframework.data.jpa.repository.Lock.class).value());
        // Atomic rollback and actual concurrent waits must be tested later on isolated MySQL.
    }
}
