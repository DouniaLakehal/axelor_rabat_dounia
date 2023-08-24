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
package com.axelor.apps.talent.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.base.service.PartnerService;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.apps.talent.db.JobApplication;
import com.axelor.apps.talent.db.JobPosition;
import com.axelor.apps.talent.db.repo.JobApplicationRepository;
import com.axelor.apps.talent.db.repo.JobPositionRepository;
import com.axelor.apps.talent.report.IReport;
import com.axelor.apps.talent.service.JobApplicationService;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Singleton;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

@Singleton
public class JobApplicationController {

  public void hire(ActionRequest request, ActionResponse response) {

    JobApplication jobApplication = request.getContext().asType(JobApplication.class);

    jobApplication = Beans.get(JobApplicationRepository.class).find(jobApplication.getId());

    Employee employee = Beans.get(JobApplicationService.class).hire(jobApplication);

    response.setReload(true);

    response.setView(
        ActionView.define(I18n.get("Employee"))
            .model(Employee.class.getName())
            .add("grid", "employee-grid")
            .add("form", "employee-form")
            .param("search-filters", "employee-filters")
            .context("_showRecord", employee.getId())
            .map());
  }

  public void setSocialNetworkUrl(ActionRequest request, ActionResponse response) {

    JobApplication application = request.getContext().asType(JobApplication.class);
    Map<String, String> urlMap =
        Beans.get(PartnerService.class)
            .getSocialNetworkUrl(application.getFirstName(), application.getLastName(), 2);
    response.setAttr("linkedinLabel", "title", urlMap.get("linkedin"));
  }

  public void imprimerRecrutement(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    if (request.getContext().get("dateDebut") == null) {
      response.setError("Le champ <b>Date Début</b> est obligatoire");
      return;
    }
    if (request.getContext().get("dateFin") == null) {
      response.setError("Le champ <b>Date Fin</b> est obligatoire");
      return;
    }

    java.sql.Date dateDebut =
        java.sql.Date.valueOf(request.getContext().get("dateDebut").toString());
    java.sql.Date dateFin = java.sql.Date.valueOf(request.getContext().get("dateFin").toString());

    String report = IReport.RecuretementReport;

    String fileLink =
        ReportFactory.createReport(report, "Liste recrutements")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("dateDebut", dateDebut)
            .addParam("DateFin", dateFin)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Liste de recrutement").add("html", fileLink).map());
  }

  public void tw_getJobPosition(ActionRequest request, ActionResponse response) {
    List<JobPosition> list =
        Beans.get(JobPositionRepository.class).all().filter("self.statusSelect!=0").fetch();
    response.setValue("jobPosition", list);
  }
}
