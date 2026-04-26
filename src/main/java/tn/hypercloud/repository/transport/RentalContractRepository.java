package tn.hypercloud.repository.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.hypercloud.entity.transport.RentalContract;

public interface RentalContractRepository extends JpaRepository<RentalContract, Long> {
}