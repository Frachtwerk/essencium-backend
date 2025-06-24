package de.frachtwerk.essencium.backend.security.permission;

import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.service.RoleService;
import java.io.Serializable;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class RolePermissionEvaluator implements EntityPermissionEvaluator {

  private static final Set<Class<?>> SUPPORTED = Set.of(Role.class);

  private final RoleService roleService;

  @Autowired
  public RolePermissionEvaluator(RoleService roleService) {
    this.roleService = roleService;
  }

  /**
   * permission check where the *object itself* is already loaded (e.g. <code>
   * @PreAuthorize("hasPermission(#role,'update')")</code>)
   */
  @Override
  public boolean hasPermission(Authentication auth, Object target, Object permission) {

    if (!(target instanceof Role role)) {
      return false;
    }
    return switch (String.valueOf(permission)) {
      case "create" -> has(auth, BasicApplicationRight.ROLE_CREATE);
      case "read" -> has(auth, BasicApplicationRight.ROLE_READ);
      case "update" -> has(auth, BasicApplicationRight.ROLE_UPDATE) && !role.isProtected();
      case "delete" -> has(auth, BasicApplicationRight.ROLE_DELETE) && !role.isProtected();
      default -> false;
    };
  }

  /**
   * permission check where only <code>id</code> and <code>type</code> are known (e.g. REST
   * endpoints using ids in the path)
   */
  @Override
  public boolean hasPermission(
      Authentication auth, Serializable id, String type, Object permission) {

    if (!Role.class.getSimpleName().equals(type)) {
      return false;
    }

    Role role = roleService.getByName(String.valueOf(id));
    return hasPermission(auth, role, permission); // delegate to the other method
  }

  /* ------------------------------------------------------------------ */
  /* helper                                                             */
  /* ------------------------------------------------------------------ */

  private static boolean has(Authentication auth, BasicApplicationRight right) {
    return auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals(right.getAuthority()));
  }

  /* ------------------------------------------------------------------ */
  /* supports-methods required by the Delegator                         */
  /* ------------------------------------------------------------------ */

  @Override
  public boolean supports(String type) {
    return Role.class.getSimpleName().equals(type);
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return SUPPORTED.contains(clazz);
  }
}
