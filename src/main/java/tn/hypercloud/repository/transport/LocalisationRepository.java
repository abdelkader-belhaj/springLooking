package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.Localisation;

import java.util.Optional;
public interface LocalisationRepository extends JpaRepository<Localisation, Long> {
    Optional<Localisation> findByAdresse(String adresse);
}