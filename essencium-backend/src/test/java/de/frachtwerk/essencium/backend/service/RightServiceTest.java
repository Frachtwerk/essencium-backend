package de.frachtwerk.essencium.backend.service;

import static de.frachtwerk.essencium.backend.api.mocking.MockConfig.configure;
import static de.frachtwerk.essencium.backend.api.mocking.MockConfig.givenMocks;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.api.annotations.EssenciumUnitTest;
import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@EssenciumUnitTest
class RightServiceTest {
  @Mock RightRepository rightRepository;
  @Mock RoleService roleService;

  @InjectMocks RightService rightService;

  @Test
  void getAll() {
    givenMocks(configure(rightRepository).returnsOnFindAll(List.of()));

    List<Right> all = rightService.getAll();

    assertNotNull(all);
    assertEquals(0, all.size());
    verify(rightRepository, times(1)).findAll();
    verifyNoMoreInteractions(rightRepository, roleService);
  }

  @Test
  void testGetAll() {
    Page page = mock(Page.class);
    Pageable pageable = mock(Pageable.class);
    when(rightRepository.findAll(pageable)).thenReturn(page);
    when(page.getTotalElements()).thenReturn(42L);

    Page<Right> all = rightService.getAll(pageable);

    assertNotNull(all);
    assertEquals(42, all.getTotalElements());
    verify(rightRepository, times(1)).findAll(pageable);
    verifyNoMoreInteractions(rightRepository, roleService);
  }

  @Test
  void save() {
    givenMocks(configure(rightRepository).returnAlwaysPassedObjectOnSave());

    Right right = new Right();
    rightService.save(right);
    verify(rightRepository, times(1)).save(right);
    verifyNoMoreInteractions(rightRepository, roleService);
  }

  @Test
  void deleteByAuthority() {
    Right right = Right.builder().authority("authority").build();
    Role role = Role.builder().name("Role").description("TestRole").rights(Set.of(right)).build();
    givenMocks(configure(roleService).returnRoleOnGetByRight(right.getAuthority(), role))
        .and(configure(rightRepository).doNothingOnDeleteEntityByAuthority(right.getAuthority()));

    rightService.deleteByAuthority(right.getAuthority());

    verify(roleService, times(1)).getByRight(right.getAuthority());
    verify(roleService, times(1)).patch(anyString(), anyMap());
    verify(rightRepository, times(1)).deleteByAuthority(right.getAuthority());
    verifyNoMoreInteractions(rightRepository, roleService);
  }

  @Test
  void findByAuthoritySuccess() {
    Right right = Right.builder().authority("authority").build();
    givenMocks(configure(rightRepository).returnOnFindByIdFor(right.getAuthority(), right));

    Right byAuthority = rightService.findByAuthority(right.getAuthority());

    assertNotNull(byAuthority);
    assertEquals(right, byAuthority);
    verify(rightRepository, times(1)).findById(right.getAuthority());
    verifyNoMoreInteractions(rightRepository, roleService);
  }

  @Test
  void findByAuthorityNotFound() {
    givenMocks(configure(rightRepository).returnOnFindByIdFor("authority", null));

    assertThrowsExactly(
        ResourceNotFoundException.class, () -> rightService.findByAuthority("authority"));

    verify(rightRepository, times(1)).findById("authority");
    verifyNoMoreInteractions(rightRepository, roleService);
  }
}
