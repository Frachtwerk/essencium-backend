package de.frachtwerk.essencium.backend.repository;

import de.frachtwerk.essencium.backend.model.ApiTokenUser;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiTokenUserRepository extends JpaRepository<ApiTokenUser, UUID> {
  List<ApiTokenUser> findByUsername(String username);
}
