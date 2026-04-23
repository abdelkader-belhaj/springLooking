package tn.hypercloud.service.transport;

import tn.hypercloud.entity.transport.Localisation;

import java.util.List;
public interface ILocalisationService {
    Localisation addLocalisation(Localisation localisation);
    Localisation updateLocalisation(Localisation localisation);
    void deleteLocalisation(Long id);
    Localisation getLocalisationById(Long id);
    List<Localisation> getAllLocalisations();
}