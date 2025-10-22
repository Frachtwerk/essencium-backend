package de.frachtwerk.essencium.backend.repository;

import de.frachtwerk.essencium.backend.model.ApiToken;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiTokenRepository extends BaseRepository<ApiToken, UUID> {
  List<ApiToken> findAllByLinkedUser(String linkedUser);
}
