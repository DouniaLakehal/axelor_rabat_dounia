/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2021 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.hr.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.configuration.db.Compte;
import com.axelor.apps.hr.db.EtatSalaire;
import com.axelor.apps.hr.db.EtatSalaireTransaction;
import com.axelor.apps.hr.report.IReport;
import com.axelor.apps.hr.service.EtatsalaireService.EtatSalaireServiceImpl;
import com.axelor.apps.hr.service.EtatsalaireService.EtatSalaireTransationService;
import com.axelor.apps.purchase.db.EtatSalaire_ops;
import com.axelor.apps.purchase.db.repo.EtatSalaire_opsRepository;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class EtatSalaireController {
  @Inject EtatSalaireServiceImpl etatSalaireServiceImpl;
  @Inject EtatSalaireTransationService appService;

  public void genererEtatSalaire(ActionRequest request, ActionResponse response)
      throws AxelorException {

    int annee = Integer.valueOf(request.getContext().get("annee").toString());
    int mois = Integer.valueOf(request.getContext().get("mois").toString());
    String retraitSalaire_transact = "non";
    Compte c = null;
    Boolean test = false;
    if (request.getContext().get("saveTransaction") != null) {
      retraitSalaire_transact = request.getContext().get("saveTransaction").toString();
      if (retraitSalaire_transact.equals("oui") && request.getContext().get("compte") != null) {
        c = (Compte) request.getContext().get("compte");
        test = appService.checkMontantAlltransaction(annee, mois, c.getId());
      } else if (retraitSalaire_transact.equals("oui")
          && request.getContext().get("compte") == null) {
        response.setFlash("Le compte est obligatoire");
        return;
      }
    }

    String type = "pdf";
    if (request.getContext().get("type_file") != null) {
      type = request.getContext().get("type_file").toString();
      if (type.isEmpty()) {
        response.setFlash("Le type du document est obligatoire");
        return;
      }
    }
    List<EtatSalaire> ls = new ArrayList<>();

    String moisString = etatSalaireServiceImpl.ConvertMoisToLettre(mois);

    if (etatSalaireServiceImpl.getNumberOfEtatSalaire(annee, mois) != 0) {
      EtatSalaire_ops etatSalaire_ops =
          Beans.get(EtatSalaire_opsRepository.class)
              .all()
              .filter("self.mois=:mois and self.year=:year")
              .bind("mois", mois)
              .bind("year", annee)
              .fetchOne();
      if (etatSalaire_ops == null) {
        etatSalaireServiceImpl.supprimerTousEtatSalaire(annee, mois);

      } else {
        ls = new ArrayList<>(etatSalaire_ops.getEtatSalaireList());
      }
      if (request.getContext().get("saveTransaction") != null
          && request.getContext().get("saveTransaction").toString().equals("oui")) {
        if (test) {
          appService.removeTransaction(mois, annee);
        }
      }
    } else ls = etatSalaireServiceImpl.AddDataToEtatSalaire(annee, mois);

    if (request.getContext().get("saveTransaction") != null
        && request.getContext().get("saveTransaction").toString().equals("oui")) {
      // check all transaction ok?
      if (test) {
        // effectuer transaction
        EtatSalaireTransaction t = appService.doRetraittransaction(annee, mois, c.getId());
        appService.reduceFromRubriqueAndCompte(t);
      } else {
        String msg =
            "Les montants dans les rubriques ou le compte "
                + c.getDesignation()
                + " ne sont pas suffisants";
        response.setFlash(msg);
        return;
      }
    }
    String fileLink = "";
    if (type.equals("xls")) {
      fileLink =
          ReportFactory.createReport(IReport.EtatSalaire2, "Etat des salaires")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .addParam("annee", annee)
              .addParam("mois", mois)
              .addParam("moisString", moisString)
              .addFormat(type)
              .generate()
              .getFileLink();
    } else {
      fileLink =
          ReportFactory.createReport(IReport.EtatRabat, "Etat des salaires")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .addParam("annee", annee)
              .addParam("mois", mois)
              .addParam("moisString", moisString)
              .addFormat(type)
              .generate()
              .getFileLink();
    }
    response.setView(ActionView.define("Etat des salaires").add("html", fileLink).map());
  }
}
