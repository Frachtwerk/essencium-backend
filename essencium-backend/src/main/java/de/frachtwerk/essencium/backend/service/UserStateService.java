// Datei: src/main/java/de/frachtwerk/essencium/backend/service/UserStateService.java
package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.repository.BaseUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserStateService {

  private final BaseUserRepository baseUserRepository;

  @PersistenceContext private EntityManager entityManager;

  public UserStateService(BaseUserRepository baseUserRepository) {
    this.baseUserRepository = baseUserRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public AbstractBaseUser fetchOriginalUserState(AbstractBaseUser<?> user) {
    entityManager.clear();
    AbstractBaseUser originalUser =
        (AbstractBaseUser) baseUserRepository.findById(user.getId()).orElse(null);

    if (originalUser != null) {
      entityManager.detach(originalUser);
    }
    return originalUser;
  }
}
