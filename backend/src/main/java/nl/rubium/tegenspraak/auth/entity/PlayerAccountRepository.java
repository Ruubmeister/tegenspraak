package nl.rubium.tegenspraak.auth.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerAccountRepository extends JpaRepository<PlayerAccount, UUID> {

    Optional<PlayerAccount> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByRole(PlayerRole role);

    List<PlayerAccount> findAllByOrderByUsernameAsc();
}
