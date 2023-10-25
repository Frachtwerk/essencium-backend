package de.frachtwerk.essencium.backend.repository;

import de.frachtwerk.essencium.backend.model.ApiTokenUser;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApiTokenUserRepository
    extends JpaRepository<ApiTokenUser, UUID>, JpaSpecificationExecutor<ApiTokenUser> {
  List<ApiTokenUser> findByUser(String user);

  boolean existsByUserAndDescription(String username, String description);
}
