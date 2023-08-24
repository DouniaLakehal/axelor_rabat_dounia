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

package com.axelor.apps.bankpayment.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.account.db.AccountingReport;
import com.axelor.apps.account.report.IReport;
import com.axelor.apps.bankpayment.service.AccountingReportBankPaymentService;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Singleton;
import java.time.LocalDate;

@Singleton
public class AccountingReportController {

  public void setBankDetailsDomain(ActionRequest request, ActionResponse response) {
    try {
      AccountingReport accountingReport = request.getContext().asType(AccountingReport.class);
      String domain =
          Beans.get(AccountingReportBankPaymentService.class)
              .createDomainForBankDetails(accountingReport);
      // if nothing was found for the domain, we set it at a default value.
      if (domain.equals("")) {
        response.setAttr("bankDetailsSet", "domain", "self.id IN (0)");
      } else {
        response.setAttr("bankDetailsSet", "domain", domain);
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void imprimer_bilan_actif(ActionRequest request, ActionResponse response)
      throws AxelorException {
    if (request.getContext().get("periode") == null) {
      response.setFlash("Merci de choisir une periode");
      return;
    }
    LocalDate dateFrom = (LocalDate) request.getContext().get("dateFrom");
    LocalDate dateTo = (LocalDate) request.getContext().get("dateTo");
    LocalDate anneePrecedent1 = dateFrom.minusYears(1);
    LocalDate anneePrecedent2 = dateTo.minusYears(1);
    String fileLink =
        ReportFactory.createReport(IReport.BilanActif, "BilanActif")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("dateFrom", java.sql.Date.valueOf(dateFrom))
            .addParam("dateTo", java.sql.Date.valueOf(dateTo))
            .addParam("anneePrecedent1", java.sql.Date.valueOf(anneePrecedent1))
            .addParam("anneePrecedent2", java.sql.Date.valueOf(anneePrecedent2))
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Bilan Actif").add("html", fileLink).map());
  }

  public void imprimer_bilan_passif(ActionRequest request, ActionResponse response)
      throws AxelorException {
    if (request.getContext().get("periode") == null) {
      response.setFlash("Merci de choisir une periode");
      return;
    }
    LocalDate dateFrom = (LocalDate) request.getContext().get("dateFrom");
    LocalDate dateTo = (LocalDate) request.getContext().get("dateTo");
    LocalDate anneeprecedent = dateFrom.minusYears(1);
    LocalDate dateannee = LocalDate.parse(anneeprecedent.getYear() + "-01-01");
    LocalDate dateanneeprev = LocalDate.parse(anneeprecedent.getYear() + "-12-31");
    String fileLink =
        ReportFactory.createReport(IReport.BilanPassif, "BilanPassif")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("dateFrom", java.sql.Date.valueOf(dateFrom))
            .addParam("dateTo", java.sql.Date.valueOf(dateTo))
            .addParam("dateannee", java.sql.Date.valueOf(dateannee))
            .addParam("dateanneeprev", java.sql.Date.valueOf(dateanneeprev))
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Bilan Passif").add("html", fileLink).map());
  }

  public void imprimer_cpc(ActionRequest request, ActionResponse response) throws AxelorException {
    if (request.getContext().get("periode") == null) {
      response.setFlash("Merci de choisir une periode");
      return;
    }
    LocalDate dateFrom = (LocalDate) request.getContext().get("dateFrom");
    LocalDate dateTo = (LocalDate) request.getContext().get("dateTo");
    LocalDate anneeprecedent = dateFrom.minusYears(1);
    LocalDate dateannee = LocalDate.parse(anneeprecedent.getYear() + "-01-01");
    LocalDate dateanneeprev = LocalDate.parse(anneeprecedent.getYear() + "-12-31");
    String type_export = "pdf";
    if (request.getContext().get("format") != null) {
      type_export = request.getContext().get("format").toString();
    }
    String fileLink =
        ReportFactory.createReport(IReport.CPC, "C.P.C")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("dateFrom", java.sql.Date.valueOf(dateFrom))
            .addParam("dateTo", java.sql.Date.valueOf(dateTo))
            .addParam("dateannee", java.sql.Date.valueOf(dateannee))
            .addParam("dateanneeprev", java.sql.Date.valueOf(dateanneeprev))
            .addFormat(type_export)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("C.P.C").add("html", fileLink).map());
  }

  public void imprimer_Compte_Produits(ActionRequest request, ActionResponse response)
      throws AxelorException {
    if (request.getContext().get("periode") == null) {
      response.setFlash("Merci de choisir une periode");
      return;
    }
    LocalDate dateFrom = (LocalDate) request.getContext().get("dateFrom");
    LocalDate dateTo = (LocalDate) request.getContext().get("dateTo");
    LocalDate anneeprecedent = dateFrom.minusYears(1);
    LocalDate dateannee = LocalDate.parse(anneeprecedent.getYear() + "-01-01");
    LocalDate dateanneeprev = LocalDate.parse(anneeprecedent.getYear() + "-12-31");
    String type_export = "pdf";
    if (request.getContext().get("format") != null) {
      type_export = request.getContext().get("format").toString();
    }
    String fileLink =
        ReportFactory.createReport(IReport.CompteProduit, "Compte des produit et charges")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("dateDebut", java.sql.Date.valueOf(dateFrom))
            .addParam("dateFin", java.sql.Date.valueOf(dateTo))
            .addParam("anneePrecedent1", java.sql.Date.valueOf(dateannee))
            .addParam("anneePrecedent2", java.sql.Date.valueOf(dateanneeprev))
            .addFormat(type_export)
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("Compte des produit et charges").add("html", fileLink).map());
  }

  public void imprimer_bilan_generale(ActionRequest request, ActionResponse response)
      throws AxelorException {
    if (request.getContext().get("periode") == null) {
      response.setFlash("Merci de choisir une periode");
      return;
    }
    String type_export = "pdf";
    if (request.getContext().get("format") != null) {
      type_export = request.getContext().get("format").toString();
    }
    LocalDate dateFrom = (LocalDate) request.getContext().get("dateFrom");
    LocalDate dateTo = (LocalDate) request.getContext().get("dateTo");
    String fileLink =
        ReportFactory.createReport(IReport.BilanGenerale, "Bilan")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("dateDebut", java.sql.Date.valueOf(dateFrom))
            .addParam("dateFin", java.sql.Date.valueOf(dateTo))
            .addFormat(type_export)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Bilan").add("html", fileLink).map());
  }
}
