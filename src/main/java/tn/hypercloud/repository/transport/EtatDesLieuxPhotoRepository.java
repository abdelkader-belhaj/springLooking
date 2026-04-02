package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.EtatDesLieuxPhoto;

public interface EtatDesLieuxPhotoRepository extends JpaRepository<EtatDesLieuxPhoto, Long> {
}