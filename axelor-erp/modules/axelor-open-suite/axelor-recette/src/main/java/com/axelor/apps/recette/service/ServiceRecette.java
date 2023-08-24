package com.axelor.apps.recette.service;

import com.axelor.apps.recette.db.InfoAttestation;
import com.axelor.apps.recette.db.InfoOrigin;
import com.axelor.apps.recette.db.Ressortissant;
import com.axelor.apps.recette.db.repo.InfoAttestationRepository;
import com.axelor.apps.recette.db.repo.InfoOriginRepository;
import com.axelor.apps.recette.db.repo.RessortissantRepository;
import com.axelor.apps.recette.report.IReport;
import com.google.inject.Inject;
import javax.transaction.Transactional;

public class ServiceRecette {
  @Inject private RessortissantRepository ressortissantRepository;
  @Inject private InfoAttestationRepository infoAttestationRepository;
  @Inject private InfoOriginRepository infoOriginRepository;

  public Ressortissant getRessortissantById(Long id) {
    return ressortissantRepository.find(id);
  }

  public String getReportByInfoAttestation(Long id, String lang) {
    String repport = "";
    InfoAttestation m = infoAttestationRepository.find(id);
    if (m.getTypeDelivrance()) {
      switch (lang) {
        case "arabe":
          repport =
              m.getRessortissant().getTypePersonne()
                  ? IReport.Attestation_Morale_arabic
                  : IReport.Attestation_Physique_arabic;
          break;
        case "français":
          repport =
              m.getRessortissant().getTypePersonne()
                  ? IReport.Attestation_Morale_fr
                  : IReport.Attestation_Physique_fr;
          break;
        case "anglais":
          repport =
              m.getRessortissant().getTypePersonne()
                  ? IReport.Attestation_Morale_en
                  : IReport.Attestation_Physique_en;
          break;
        default:
          break;
      }
    } else {
      switch (lang) {
        case "arabe":
          repport =
              m.getRessortissant().getTypePersonne()
                  ? IReport.Attestation_morale_taxe_ar
                  : IReport.Attestation_Physique_taxe_ar;
          break;
        case "français":
          repport =
              m.getRessortissant().getTypePersonne()
                  ? IReport.Attestation_morale_taxe_fr
                  : IReport.Attestation_Physique_taxe_fr;
          break;
        case "anglais":
          repport =
              m.getRessortissant().getTypePersonne()
                  ? IReport.Attestation_morale_taxe_en
                  : IReport.Attestation_Physique_taxe_en;
          break;
        default:
          break;
      }
    }

    return repport;
  }

  @Transactional
  public void setHasAttestation(boolean b, Long id) {
    Ressortissant r = ressortissantRepository.find(id);
    r.setHas_attestation(b);
    ressortissantRepository.save(r);
  }

  @Transactional
  public void setHasorigin(boolean b, Long id) {
    Ressortissant r = ressortissantRepository.find(id);
    r.setHas_origin(b);
    ressortissantRepository.save(r);
  }

  public InfoAttestation getInfoAttestationByRessortissant(Long id) {
    InfoAttestation info =
        infoAttestationRepository.all().filter("self.ressortissant=?1", id).fetchOne();
    return info;
  }

  public InfoOrigin getInfoOriginByRessortissant(Long id) {
    InfoOrigin infoO = infoOriginRepository.all().filter("self.ressortissant=?1", id).fetchOne();
    return infoO;
  }

  public InfoAttestation getInfoAttestationById(Long id) {
    return infoAttestationRepository.find(id);
  }
}
