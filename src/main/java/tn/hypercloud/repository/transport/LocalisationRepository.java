package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.hypercloud.entity.transport.Localisation;

@Repository
public interface LocalisationRepository extends JpaRepository<Localisation, Long> {
}
