package com.axelor.apps.hr.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.configuration.db.AvancementPeriode;
import com.axelor.apps.configuration.db.Compte;
import com.axelor.apps.configuration.db.Echelon;
import com.axelor.apps.configuration.db.Grade;
import com.axelor.apps.configuration.db.repo.AvancementPeriodeRepository;
import com.axelor.apps.configuration.db.repo.EchelonRepository;
import com.axelor.apps.hr.db.Augmentation;
import com.axelor.apps.hr.db.Conge;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.report.IReport;
import com.axelor.apps.hr.service.EmployeeAdvanceService;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.LocalDate;
import java.util.List;

@Singleton
public class CustomController {

  @Inject EmployeeAdvanceService appServices;

  public void tw_test_show_hide_compteInformationDetail(
      ActionRequest request, ActionResponse response) {
    String data = request.getContext().get("saveTransaction").toString();
    List<Compte> cpt = appServices.getallCompte();
    // response.setValue("compte", cpt);
    if (data.equals("oui")) {
      response.setHidden("compte", false);
    } else {
      response.setHidden("compte", true);
    }
  }

  public void updateDateRecrutement(ActionRequest request, ActionResponse response) {
    if (request.getContext().get("id") == null
        || request.getContext().get("daterecrutement") == null) return;
    Long id_employer = (Long) request.getContext().get("id");
    LocalDate date = (LocalDate) request.getContext().get("daterecrutement");
    Augmentation augmentation = appServices.getFirstAugmentation(id_employer);
    int compare = augmentation.getDateDebut().compareTo(date);
    if (augmentation != null && compare != 0) {
      augmentation.setDateDebut(date);
      appServices.saveAugmentation(augmentation);
    }
  }

  public void printDecision(ActionRequest request, ActionResponse response) throws AxelorException {
    if (request.getContext().get("id") == null) return;
    Long id = (Long) request.getContext().get("id");
    Conge c = appServices.getCongeById(id);
    Employee emp = appServices.getEmployerById(c.getEmployee().getId());
    String nomEmp =
        emp.getContactPartner().getTitleSelect() == 2
            ? ("Mme " + emp.getContactPartner().getSimpleFullName())
            : ("Mr " + emp.getContactPartner().getSimpleFullName());
    String fileLink =
        ReportFactory.createReport(IReport.Decision_FR, "Décision_FR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id", id)
            .addParam("cin", emp.getCin() + ", " + emp.getGrade().getName())
            .addParam("nomEmp", nomEmp)
            .addParam("rest", c.getNbrDayRest())
            .addParam("duree", c.getDuree())
            .addParam("dateDebut", java.sql.Date.valueOf(c.getDateDebut()))
            .addParam("dateFin", java.sql.Date.valueOf(c.getDateFin()))
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Décision_FR").add("html", fileLink).map());
  }

  public void printDecision_ar(ActionRequest request, ActionResponse response)
      throws AxelorException {
    if (request.getContext().get("id") == null) return;
    Long id = (Long) request.getContext().get("id");
    Conge c = appServices.getCongeById(id);
    Employee emp = appServices.getEmployerById(c.getEmployee().getId());
    String nomEmp =
        emp.getContactPartner().getTitleSelect() == 2
            ? ("ة " + emp.getNonprenom_ar())
            : (" " + emp.getNonprenom_ar());
    String fileLink =
        ReportFactory.createReport(IReport.Decision_AR, "Décision_AR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id", id)
            .addParam("cin", emp.getCin() + ", " + emp.getGrade().getName())
            .addParam("nomEmp", nomEmp)
            .addParam("rest", c.getNbrDayRest())
            .addParam("duree", c.getDuree())
            .addParam("d1", java.sql.Date.valueOf(c.getDateDebut()))
            .addParam("d2", java.sql.Date.valueOf(c.getDateFin()))
            .addParam("d_creation", java.sql.Date.valueOf(c.getCreatedOn().toLocalDate()))
            .addParam("grade_ar", c.getGrade().getNom_ar())
            .addParam("occasion", c.getMonasaba())
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Décision_AR").add("html", fileLink).map());
  }

  public void printExecption(ActionRequest request, ActionResponse response)
      throws AxelorException {
    if (request.getContext().get("id") == null) return;
    Long id = (Long) request.getContext().get("id");
    Conge c = appServices.getCongeById(id);
    Employee emp = appServices.getEmployerById(c.getEmployee().getId());
    String nomEmp =
        emp.getContactPartner().getTitleSelect() == 2
            ? ("سيدة " + emp.getNonprenom_ar())
            : ("سيد " + emp.getNonprenom_ar());
    String fileLink =
        ReportFactory.createReport(IReport.Decision_exceptionnel, "Decision_exceptionnel")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id", id)
            .addParam("nomEmp", nomEmp)
            .addParam("occasion", c.getMonasaba())
            .addParam("rest", c.getNbrDayRest())
            .addParam("duree", String.format("%02d", c.getDuree()))
            .addParam("d1", java.sql.Date.valueOf(c.getDateDebut()))
            .addParam("d2", java.sql.Date.valueOf(c.getDateFin()))
            .addParam("d_create", java.sql.Date.valueOf(c.getCreatedOn().toLocalDate()))
            .addParam("grade_ar", c.getGrade().getNom_ar())
            .addParam("occasion", c.getMonasaba())
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Decision_exceptionnel").add("html", fileLink).map());
  }

  public void diableFiledDateFin(ActionRequest request, ActionResponse response) {
    if (request.getContext().get("typeGeneraleConge") == null) return;
    String choix = (String) request.getContext().get("typeGeneraleConge");
    if (choix.equals("autorisation")) response.setReadonly("dateFin", true);
    else response.setReadonly("dateFin", false);
  }

  public void printDecision_ar_nameSave(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id = (Long) request.getContext().get("id");
    Employee emp = appServices.getEmployerById(id);
    emp.setNonprenom_ar(request.getContext().get("nonprenom_ar").toString());
    emp = appServices.saveEmployee(emp);
    Grade g = (Grade) request.getContext().get("grade");
    g.setNom_ar(request.getContext().get("grade_ar").toString());
    appServices.saveGrade(g);

    if (request.getContext().get("_id") == null) return;
    Long id_c = Long.valueOf((Integer) request.getContext().get("_id"));
    Conge c = appServices.getCongeById(id_c);
    String nomEmp =
        emp.getContactPartner().getTitleSelect() == 2
            ? ("ة " + emp.getNonprenom_ar())
            : (" " + emp.getNonprenom_ar());

    String fileLink =
        ReportFactory.createReport(IReport.Decision_AR, "Décision_AR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id", id)
            .addParam("cin", emp.getCin() + ", " + emp.getGrade().getName())
            .addParam("nomEmp", nomEmp)
            .addParam("rest", c.getNbrDayRest())
            .addParam("duree", c.getDuree())
            .addParam("d1", java.sql.Date.valueOf(c.getDateDebut()))
            .addParam("d2", java.sql.Date.valueOf(c.getDateFin()))
            .addParam("d_creation", java.sql.Date.valueOf(c.getCreatedOn().toLocalDate()))
            .addParam("grade_ar", c.getGrade().getNom_ar())
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Décision_AR").add("html", fileLink).map());
  }

  public void deleteCreateListEmployerRapide(ActionRequest request, ActionResponse response) {
    appServices.deleteCreateemployerAvancement(1);
  }

  public void deleteCreateListEmployerMoyen(ActionRequest request, ActionResponse response) {
    appServices.deleteCreateemployerAvancement(2);
  }

  public void deleteCreateListEmployerLong(ActionRequest request, ActionResponse response) {
    appServices.deleteCreateemployerAvancement(3);
  }

  public void loadDataconfigurationAuglenatation(ActionRequest request, ActionResponse response) {
    if (request.getContext().get("_id") != null) {
      Long id = Long.valueOf((Integer) request.getContext().get("_id"));
      String type = request.getContext().get("type_aug").toString();
      Employee emp = appServices.getEmployerById(id);
      Augmentation aug = appServices.getAugmentationByEtat(emp.getId(), "En cours");
      AvancementPeriode avp =
          Beans.get(AvancementPeriodeRepository.class)
              .all()
              .filter("self.echelon_start =:echelon")
              .bind("echelon", emp.getEchelon().getName())
              .fetchOne();
      LocalDate date_send = LocalDate.now();
      List<Echelon> ls_echelon =
          Beans.get(EchelonRepository.class)
              .all()
              .filter("self.echelle =:echelle")
              .bind("echelle", emp.getEchelle())
              .fetch();
      Echelon echelon =
          ls_echelon.stream()
              .filter(echelon1 -> echelon1.getName().equals(avp.getEchelon_end()))
              .findFirst()
              .orElse(null);

      if (emp.getEchelle().getName().equals("HE")) {
        date_send = aug.getDateDebut().plusYears(3);
      } else {
        if (type.equals("rapide")) {
          date_send = aug.getDateDebut().plusMonths(avp.getNbr_mois_rapide());

        } else if (type.equals("moyen")) {
          date_send = aug.getDateDebut().plusMonths(avp.getNbr_mois_moyen());
        } else {
          date_send = aug.getDateDebut().plusMonths(avp.getNbr_mois_long());
        }
      }

      response.setValue("employer", emp);
      response.setValue("grade", emp.getGrade());
      response.setValue("echelle", emp.getEchelle());
      response.setValue("echelon", echelon);
      response.setValue("dateDebut", date_send);
      response.setValue("avtRecalss", "Avt");
      response.setValue("etat", "En attente");
      response.setValue("hasRappel", true);
    }
  }
}
