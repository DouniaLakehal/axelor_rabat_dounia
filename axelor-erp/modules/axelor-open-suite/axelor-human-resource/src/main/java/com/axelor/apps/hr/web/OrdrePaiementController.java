package com.axelor.apps.hr.web;

import com.axelor.apps.hr.db.EtatSalaire;
import com.axelor.apps.hr.db.repo.EtatSalaireRepository;
import com.axelor.apps.hr.service.PaymentServices;
import com.axelor.apps.purchase.db.EtatSalaire_ops;
import com.axelor.apps.purchase.db.OrderPaymentCommande;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.util.HashSet;
import java.util.List;
import javax.inject.Inject;

public class OrdrePaiementController {

  @Inject PaymentServices appservices;

  public void genererOrdrePayement(ActionRequest request, ActionResponse response)
      throws Exception {
    Integer year = (Integer) request.getContext().get("year");
    Integer mois = (Integer) request.getContext().get("mois");

    List<EtatSalaire> etatSalaire =
        Beans.get(EtatSalaireRepository.class)
            .all()
            .filter("self.annee=:year and self.mois =:mois")
            .bind("year", year)
            .bind("mois", mois)
            .fetch();

    if (etatSalaire == null) {
      response.setFlash("Aucune etat de salaire trouvée");
      return;
    }

    EtatSalaire_ops etatSalaire_ops = new EtatSalaire_ops();
    etatSalaire_ops.setMois(mois);
    etatSalaire_ops.setYear(year);
    etatSalaire_ops.setEtatSalaireList(new HashSet<>(etatSalaire));
    etatSalaire_ops = appservices.saveEtatSalaireOP(etatSalaire_ops);
    appservices.createOrderPaymentRH(etatSalaire_ops);

    OrderPaymentCommande o = new OrderPaymentCommande();

    System.out.println(o);
  }
}
