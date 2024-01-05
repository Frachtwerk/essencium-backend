package de.frachtwerk.essencium.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.model.TestLongUser;
import de.frachtwerk.essencium.backend.model.exception.NotAllowedException;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import de.frachtwerk.essencium.backend.repository.RoleRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {
  @Mock RoleRepository roleRepository;
  @Mock RightRepository rightRepository;
  @Mock LongUserService userService;

  @InjectMocks RoleService roleService;

  @BeforeEach
  void setUp() {
    roleService.setUserService(userService);
  }

  @Test
  void getAll() {
    when(roleRepository.findAll()).thenReturn(List.of());
    roleService.getAll();
    verify(roleRepository, times(1)).findAll();
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoInteractions(userService);
  }

  @Test
  void testGetAll() {
    when(roleRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
    roleService.getAll(Pageable.unpaged());
    verify(roleRepository, times(1)).findAll(any(Pageable.class));
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoInteractions(userService);
  }

  @Test
  void getByName() {
    when(roleRepository.findByName(anyString())).thenReturn(mock(Role.class));
    roleService.getByName("RoleName");
    verify(roleRepository, times(1)).findByName("RoleName");
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoInteractions(userService);
  }

  @Test
  void saveNew() {
    Role mockedRole = mock(Role.class);
    when(roleRepository.save(any(Role.class))).thenReturn(mockedRole);
    when(roleRepository.findById(anyString())).thenReturn(Optional.empty());
    when(mockedRole.getName()).thenReturn("RoleName");
    roleService.save(mockedRole);
    verify(roleRepository, times(1)).save(any(Role.class));
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoInteractions(userService);
  }

  @Test
  void saveUpdate() {
    Role mockedRole = mock(Role.class);
    when(roleRepository.save(any(Role.class))).thenReturn(mockedRole);
    when(roleRepository.findById(anyString())).thenReturn(Optional.of(mockedRole));
    when(mockedRole.getName()).thenReturn("RoleName");
    when(mockedRole.isProtected()).thenReturn(false);
    roleService.save(mockedRole);
    verify(roleRepository, times(1)).save(any(Role.class));
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoInteractions(userService);
  }

  @Test
  void delete() {
    Role mockedRole = mock(Role.class);
    doNothing().when(roleRepository).delete(any(Role.class));
    when(roleRepository.findById(anyString())).thenReturn(Optional.of(mockedRole));
    when(userService.loadUsersByRole(anyString())).thenReturn(List.of());
    when(mockedRole.getName()).thenReturn("RoleName");
    when(mockedRole.isProtected()).thenReturn(false);
    roleService.delete(mockedRole);
    verify(roleRepository, times(1)).delete(any(Role.class));
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoMoreInteractions(userService);
  }

  @Test
  void getById() {
    when(roleRepository.findByName(anyString())).thenReturn(mock(Role.class));
    roleService.getById("RoleName");
    verify(roleRepository, times(1)).findByName("RoleName");
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoInteractions(userService);
  }

  @Test
  void create() {
    Role mockedRole = mock(Role.class);
    when(roleRepository.save(any(Role.class))).thenReturn(mockedRole);
    when(roleRepository.findById(anyString())).thenReturn(Optional.empty());
    when(mockedRole.getName()).thenReturn("RoleName");
    roleService.create(mockedRole);
    verify(roleRepository, times(1)).save(any(Role.class));
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoInteractions(userService);
  }

  @Test
  void update() {
    Role mockedRole = mock(Role.class);
    when(roleRepository.existsById(anyString())).thenReturn(true);
    when(roleRepository.findById(anyString())).thenReturn(Optional.of(mockedRole));
    when(mockedRole.getName()).thenReturn("RoleName");
    when(mockedRole.isProtected()).thenReturn(false);
    when(roleRepository.save(any(Role.class))).thenReturn(mockedRole);
    roleService.update("RoleName", mockedRole);
    verify(roleRepository, times(1)).save(any(Role.class));
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoInteractions(userService);
  }

  @Test
  void patch() {
    Role mockedRole = mock(Role.class);

    Map<String, Object> map =
        Map.of("description", "Description", "isProtected", true, "isDefaultRole", true);

    when(roleRepository.save(any(Role.class))).thenReturn(mockedRole);
    when(roleRepository.findById(anyString())).thenReturn(Optional.of(mockedRole));
    when(roleRepository.findByIsDefaultRoleIsTrue()).thenReturn(Optional.empty());
    when(mockedRole.isProtected()).thenReturn(false);

    roleService.patch("RoleName", map);

    verify(roleRepository, times(1)).save(any(Role.class));
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoInteractions(userService);

    verify(mockedRole, times(1)).setDescription("Description");
    verify(mockedRole, times(1)).setProtected(true);
    verify(mockedRole, times(1)).setDefaultRole(true);
    verifyNoMoreInteractions(mockedRole);
  }

  @Test
  void patchName() {
    Role mockedRole = mock(Role.class);

    Map<String, Object> map = Map.of("name", "newRole");

    when(roleRepository.findById(anyString())).thenReturn(Optional.of(mockedRole));
    when(mockedRole.isProtected()).thenReturn(false);

    assertThrows(ResourceUpdateException.class, () -> roleService.patch("RoleName", map));

    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoMoreInteractions(mockedRole);
    verifyNoInteractions(userService);
  }

  @Test
  void patchRightsAsString() {
    Role mockedRole = mock(Role.class);

    Map<String, Object> map = Map.of("rights", Set.of("Right1", "Right2"));

    when(rightRepository.findByAuthority(anyString()))
        .thenAnswer(
            invocation -> {
              String authority = invocation.getArgument(0);
              return Right.builder().authority(authority).description(authority).build();
            });
    when(roleRepository.save(any(Role.class))).thenReturn(mockedRole);
    when(roleRepository.findById(anyString())).thenReturn(Optional.of(mockedRole));

    when(mockedRole.isProtected()).thenReturn(false);

    roleService.patch("RoleName", map);

    verify(roleRepository, times(1)).save(any(Role.class));
    verifyNoMoreInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);

    verify(mockedRole, times(1)).setRights(anySet());

    verifyNoMoreInteractions(mockedRole);
    verifyNoInteractions(userService);
  }

  @Test
  void patchRightsAsRight() {
    Role mockedRole = mock(Role.class);

    Map<String, Object> map =
        Map.of(
            "rights",
            Set.of(
                Right.builder().authority("Right1").description("Right1").build(),
                Right.builder().authority("Right2").description("Right2").build()));

    when(rightRepository.findByAuthority(anyString()))
        .thenAnswer(
            invocation -> {
              String authority = invocation.getArgument(0);
              return Right.builder().authority(authority).description(authority).build();
            });

    when(roleRepository.save(any(Role.class))).thenReturn(mockedRole);
    when(roleRepository.findById(anyString())).thenReturn(Optional.of(mockedRole));
    when(mockedRole.isProtected()).thenReturn(false);

    roleService.patch("RoleName", map);

    verify(roleRepository, times(1)).save(any(Role.class));
    verifyNoMoreInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);

    verify(mockedRole, times(1)).setRights(anySet());
    verifyNoMoreInteractions(mockedRole);
    verifyNoInteractions(userService);
  }

  @Test
  void deleteById() {
    Role mocked = mock(Role.class);
    when(mocked.getName()).thenReturn("RoleName");
    doNothing().when(roleRepository).delete(any(Role.class));
    when(roleRepository.findById(anyString())).thenReturn(Optional.of(mocked));
    when(userService.loadUsersByRole(anyString())).thenReturn(List.of());
    roleService.deleteById("RoleName");
    verify(roleRepository, times(1)).delete(any(Role.class));
    verify(roleRepository, times(1)).findById("RoleName");
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoMoreInteractions(userService);
  }

  @Test
  void deleteByIdFail() {
    Role mocked = mock(Role.class);
    when(mocked.getName()).thenReturn("RoleName");
    when(roleRepository.findById(anyString())).thenReturn(Optional.of(mocked));
    when(userService.loadUsersByRole(anyString())).thenReturn(List.of(mock(TestLongUser.class)));
    assertThrows(NotAllowedException.class, () -> roleService.deleteById("RoleName"));
    verify(roleRepository, times(1)).findById("RoleName");
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoMoreInteractions(userService);
  }

  @Test
  void getRole() {
    when(roleRepository.findByName(anyString())).thenReturn(mock(Role.class));
    roleService.getRole("RoleName");
    verify(roleRepository, times(1)).findByName("RoleName");
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoInteractions(userService);
  }

  @Test
  void getDefaultRole() {
    when(roleRepository.findByIsDefaultRoleIsTrue()).thenReturn(Optional.of(mock(Role.class)));
    roleService.getDefaultRole();
    verify(roleRepository, times(1)).findByIsDefaultRoleIsTrue();
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoInteractions(userService);
  }

  @Test
  void getDefaultRoleNull() {
    when(roleRepository.findByIsDefaultRoleIsTrue()).thenReturn(Optional.empty());
    Role defaultRole = roleService.getDefaultRole();
    assertNull(defaultRole);
    verify(roleRepository, times(1)).findByIsDefaultRoleIsTrue();
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoInteractions(userService);
  }

  @Test
  void getByRight() {
    when(roleRepository.findAllByRights_Authority(anyString())).thenReturn(List.of());
    roleService.getByRight("RightName");
    verify(roleRepository, times(1)).findAllByRights_Authority("RightName");
    verifyNoInteractions(rightRepository);
    verifyNoMoreInteractions(roleRepository);
    verifyNoInteractions(userService);
  }
}
