package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.DemandeCourse;
import tn.hypercloud.entity.user.User;

import java.math.BigDecimal;
import java.util.List;

public interface ITransportationBookingService {


    DemandeCourse createBookingRequest(DemandeCourse demandeCourse);


    DemandeCourse startMatching(Long demandeId);


    List<DemandeCourse> getBookingsByClient(User client);


    BigDecimal calculateEstimatedPrice(DemandeCourse demande);
}