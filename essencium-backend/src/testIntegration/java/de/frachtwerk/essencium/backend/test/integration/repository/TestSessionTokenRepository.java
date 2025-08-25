package de.frachtwerk.essencium.backend.test.integration.repository;

import de.frachtwerk.essencium.backend.model.SessionToken;
import de.frachtwerk.essencium.backend.repository.SessionTokenRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface TestSessionTokenRepository extends SessionTokenRepository {

  List<SessionToken> findAllByUsername(String username);
}
