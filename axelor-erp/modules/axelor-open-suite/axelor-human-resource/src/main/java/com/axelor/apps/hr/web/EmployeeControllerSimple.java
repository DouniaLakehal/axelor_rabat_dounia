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
import com.axelor.apps.configuration.db.*;
import com.axelor.apps.hr.db.*;
import com.axelor.apps.hr.db.repo.*;
import com.axelor.apps.hr.report.IReport;
import com.axelor.apps.hr.service.Etatengagement.EtatEngagmentService;
import com.axelor.apps.hr.service.Etatengagement.ServiceFunction;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.exception.AxelorException;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class EmployeeControllerSimple {
  @Inject EtatEngagmentService appService;
  @Inject ServiceFunction appMethode;
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void imprimerEtatEngagement_simple(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String type =
        request.getContext().get("type_doc") == null
            ? "pdf"
            : request.getContext().get("type_doc").toString();
    String typeEngagement = (String) request.getContext().get("type_engagement");
    appMethode.checkPreRequit(request, response);
    if (response.getData() != null) return;
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    Long id = (Long) request.getContext().get("id");
    Employee emp = appMethode.getEmployerById(id);

    Map<String, Object> dataInfo1 = appService.getDataInfo1(emp, d1, d2);
    EtatEngagement etat = appService.check_changement(emp, d1, d2, dataInfo1);
    if (etat == null) {
      EtatEngagement t = appMethode.getEtatEngagementBefor(emp, d1);
      response.setFlash(
          "Attention la période ne dispose pas d'un etat d'engagement<br>"
              + "Merci de créer un etat d'engagement entre le :"
              + t.getDateFin().plusDays(1)
              + " et "
              + d1);
      return;
    } else {
      appMethode.completeInformation(etat.getId(), type, typeEngagement);
    }
    String fileLink =
        ReportFactory.createReport(IReport.ImprimerEtategagement_simple, "Etat Engagement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("dateDebut", java.sql.Date.valueOf(d1))
            .addParam("dateFin", java.sql.Date.valueOf(d2))
            .addParam("id_etat", etat.getId())
            .addParam("id_emp", emp.getId())
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat Engagement").add("html", fileLink).map());
  }

  public void deleteEtatEngagment(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id = (Long) request.getContext().get("id");
    appService.deleteEtatEngagement(id);
    response.setReload(true);
  }
}
