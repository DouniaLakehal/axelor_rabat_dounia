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
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.repo.RubriqueBudgetaireGeneraleRepository;
import com.axelor.apps.configuration.service.ConvertNomreToLettres;
import com.axelor.apps.configuration.service.MyConfigurationService;
import com.axelor.apps.configuration.service.RunSqlRequestForMe;
import com.axelor.apps.configuration.service.ServiceUtil;
import com.axelor.apps.hr.db.*;
import com.axelor.apps.hr.db.repo.*;
import com.axelor.apps.hr.report.IReport;
import com.axelor.apps.hr.service.EmployeeAdvanceService;
import com.axelor.apps.hr.service.app.AppHumanResourceServiceImpl;
import com.axelor.apps.hr.service.employee.EmployeeService;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.CallMethod;
import com.axelor.meta.MetaStore;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.meta.schema.views.Selection;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wslite.json.JSONException;
import wslite.json.JSONObject;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class EmployeeController {

  private AugmentationRepository augmentationRepository;
  @Inject private GestionCreditRepository gestionCreditRepository;
  @Inject private EmployeeRepository employeeRepository;
  @Inject private ServiceUtil serviceUtil;
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final RubriqueBudgetaireGenerale RubriqueBudgetaireGenerale = null;

  @Inject private EmployeeAdvanceService appservice;
  @Inject private MyConfigurationService appConfigService;

  public void showAnnualReport(ActionRequest request, ActionResponse response)
      throws JSONException, NumberFormatException, AxelorException {
    String employeeId = request.getContext().get("_id").toString();
    String year = request.getContext().get("year").toString();
    int yearId = new JSONObject(year).getInt("id");
    String yearName = new JSONObject(year).getString("name");
    User user = AuthUtils.getUser();
    String name =
        I18n.get("Annual expenses report") + " :  " + user.getFullName() + " (" + yearName + ")";
    String fileLink =
        ReportFactory.createReport(IReport.EMPLOYEE_ANNUAL_REPORT, name)
            .addParam("EmployeeId", Long.valueOf(employeeId))
            .addParam(
                "Timezone",
                getTimezone(
                    Beans.get(EmployeeRepository.class).find(Long.valueOf(employeeId)).getUser()))
            .addParam("YearId", yearId)
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .toAttach(Beans.get(EmployeeRepository.class).find(Long.valueOf(employeeId)))
            .generate()
            .getFileLink();
    response.setView(ActionView.define(name).add("html", fileLink).map());
    response.setCanClose(true);
  }

  public void setEmployeeSocialNetworkUrl(ActionRequest request, ActionResponse response) {
    Employee employee = request.getContext().asType(Employee.class);
    if (employee.getContactPartner() != null) {
      Map<String, String> urlMap =
          Beans.get(EmployeeService.class)
              .getSocialNetworkUrl(
                  employee.getContactPartner().getName(),
                  employee.getContactPartner().getFirstName());
      response.setAttr("contactPartner.facebookLabel", "title", urlMap.get("facebook"));
      response.setAttr("contactPartner.twitterLabel", "title", urlMap.get("twitter"));
      response.setAttr("contactPartner.linkedinLabel", "title", urlMap.get("linkedin"));
      response.setAttr("contactPartner.youtubeLabel", "title", urlMap.get("youtube"));
    }
  }

  public void setContactSocialNetworkUrl(ActionRequest request, ActionResponse response) {
    Partner partnerContact = request.getContext().asType(Partner.class);
    Map<String, String> urlMap =
        Beans.get(EmployeeService.class)
            .getSocialNetworkUrl(partnerContact.getName(), partnerContact.getFirstName());
    response.setAttr("facebookLabel", "title", urlMap.get("facebook"));
    response.setAttr("twitterLabel", "title", urlMap.get("twitter"));
    response.setAttr("linkedinLabel", "title", urlMap.get("linkedin"));
    response.setAttr("youtubeLabel", "title", urlMap.get("youtube"));
  }

  public void printEmployeePhonebook(ActionRequest request, ActionResponse response)
      throws AxelorException {
    User user = AuthUtils.getUser();
    String name = I18n.get("Employee PhoneBook");
    String fileLink =
        ReportFactory.createReport(IReport.EMPLOYEE_PHONEBOOK, name + "-${date}")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("UserId", user.getId())
            .addParam("Timezone", getTimezone(user))
            .generate()
            .getFileLink();
    LOG.debug("Printing " + name);
    response.setView(ActionView.define(name).add("html", fileLink).map());
  }

  public void printEmployeeReport(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Employee employee = request.getContext().asType(Employee.class);
    String name = I18n.get("Employee");
    String fileLink =
        ReportFactory.createReport(IReport.EMPLOYEE, name + "-${date}")
            .addParam("EmployeeId", employee.getId())
            .addParam("Timezone", getTimezone(employee.getUser()))
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .generate()
            .getFileLink();
    LOG.debug("Printing " + name);
    response.setView(ActionView.define(name).add("html", fileLink).map());
  }

  private String getTimezone(User user) {
    if (user == null || user.getActiveCompany() == null) {
      return null;
    }
    return user.getActiveCompany().getTimezone();
  }

  public void generateNewDPAE(ActionRequest request, ActionResponse response) {
    Employee employee = request.getContext().asType(Employee.class);
    employee = Beans.get(EmployeeRepository.class).find(employee.getId());
    try {
      Long dpaeId = Beans.get(EmployeeService.class).generateNewDPAE(employee);
      ActionViewBuilder builder =
          ActionView.define(I18n.get("DPAE"))
              .model(DPAE.class.getName())
              .add("grid", "dpae-grid")
              .add("form", "dpae-form")
              .param("search-filters", "dpae-filters")
              .context("_showRecord", dpaeId);
      response.setView(builder.map());
    } catch (AxelorException e) {
      TraceBackService.trace(response, e);
    }
    response.setReload(true);
  }

  public void modifierDate(ActionRequest request, ActionResponse response) {
    Augmentation augmentation = request.getContext().asType(Augmentation.class);
    int nbr_attente =
        Beans.get(AugmentationRepository.class)
            .findByEmployee(augmentation.getEmployer(), "En attente")
            .size();
    if (nbr_attente != 0
        || appservice.getEmployer_has_Augmentation(augmentation.getEmployer().getId()) != null) {
      if (nbr_attente != 0)
        response.setAlert("le Personnel dispose d'une Augmentation <b>En attente</b>");
      else response.setAlert("le Personnel dispose d'un <b>Rappel En attente</b>");

    } else {
      // create une augmentation
      Beans.get(EmployeeAdvanceService.class).createAugmentation(augmentation);
      response.setReload(true);
      response.setPending("close");
      ActionViewBuilder builder =
          ActionView.define("Liste des augmentations")
              .model(Augmentation.class.getName())
              .add("grid", "config.augmentation-grid")
              .add("form", "config.augmentation-form");
      response.setView(builder.map());
      response.setSignal("refresh-tab", true);
    }
  }

  public void upgrade_Personnel(ActionRequest request, ActionResponse response) {
    Long id_emp = Long.valueOf((Integer) request.getContext().get("_id"));
    Employee emp = appservice.getEmployerById(id_emp);
    try {
      Augmentation augmentationEnAttente = appservice.getAugmentationByEtat(id_emp, "En attente");
      Augmentation augmentationEncours = appservice.getAugmentationByEtat(id_emp, "En cours");
      augmentationEncours.setEtat("Ancien");
      augmentationEncours.setDateFin(augmentationEnAttente.getDateDebut().minusDays(1));
      augmentationEncours = appservice.saveAugmentation(augmentationEncours);

      augmentationEnAttente.setEtat("En cours");
      // augmentationEnAttente.setDateRappel(LocalDate.now());
      augmentationEnAttente = appservice.saveAugmentation(augmentationEnAttente);
      emp.setEchelon(augmentationEnAttente.getEchelon());
      emp.setEchelle(augmentationEnAttente.getEchelle());
      emp.setGrade(augmentationEnAttente.getGrade());
      emp.setHasEvolution(false);
      // enregistrement d'un employer
      emp = appservice.update_personnel(emp);

      Beans.get(EmployeeAdvanceService.class).validerRappel(augmentationEnAttente);

    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    response.setReload(true);
  }

  @CallMethod
  public void printEtatEngagement(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    long employeeId = (int) request.getContext().get("_id");
    Employee employee = Beans.get(EmployeeRepository.class).find(employeeId);
    long old_echelle,
        new_echelle,
        old_echelon,
        new_echelon,
        old_grade,
        new_grade,
        old_corps,
        new_corps;
    int old_children, new_children, old_children_21, new_children_21;
    old_echelle =
            new_echelle =
                    old_echelon = new_echelon = old_grade = new_grade = old_corps = new_corps = 0L;
    old_children = new_children = old_children_21 = new_children_21 = 0;
    String situation = " - ";
    boolean hasresponsabilite = employee.getResponsabilite() != null;
    String d1, d2, situation_old, situation_new;
    d1 = d2 = situation_old = situation_new = "";
    boolean error_date = true;
    boolean error_empty = request.getContext().get("dateDebut") == null
            || request.getContext().get("dateFin") == null;
    if (!error_empty) {
      String date1 = request.getContext().get("dateDebut").toString();
      String date2 = request.getContext().get("dateFin").toString();
      error_date =
              Beans.get(EmployeeAdvanceService.class)
                      .verifierdate(date1, employee.getDaterecrutement());
      new_children = Beans.get(EmployeeAdvanceService.class).getNombreEnfantAtDate(employee, date2);
      old_children = Beans.get(EmployeeAdvanceService.class).getNombreEnfantAtDate(employee, date1);
      new_children_21 =
              Beans.get(EmployeeAdvanceService.class).getNombreEnfantAtDate_21(employee, date2);
      old_children_21 =
              Beans.get(EmployeeAdvanceService.class).getNombreEnfantAtDate_21(employee, date1);
      if (employee.getMaritalStatus() != null
          && !employee.getMaritalStatus().equals("")
          && employee.getMarriageDate() != null) {
        situation_old = appservice.getSitualtionAtDate(date1, employee.getMarriageDate());
        situation_new = appservice.getSitualtionAtDate(date2, employee.getMarriageDate());
        situation = employee.getMaritalStatus();
        Selection.Option item = MetaStore.getSelectionItem("hr.employee.marital.status", situation);
        situation = I18n.get(item.getTitle());
      } else {
        situation_new = situation_old = "Célibataire";
      }
      int nbr_aug = Beans.get(EmployeeAdvanceService.class).getNombreAugmentation(employeeId);
      if (nbr_aug == 0 && !error_date) {
        old_corps = new_corps = employee.getCorps().getId();
        old_grade = new_grade = employee.getGrade().getId();
        old_echelle = new_echelle = employee.getEchelle().getId().intValue();
        old_echelon = new_echelon = employee.getEchelon().getId().intValue();

      } else if (nbr_aug > 0 && !error_date) {
        Augmentation a1 =
            Beans.get(EmployeeAdvanceService.class)
                .getEchelleAtDate(date1, date2, employee, "Debut");
        Augmentation a2 =
            Beans.get(EmployeeAdvanceService.class).getEchelleAtDate(date1, date2, employee, "Fin");
        if (a1 != null && !error_date) {
          old_corps = a1.getGrade().getCorps().getId();
          old_grade = a1.getGrade().getId();
          old_echelle = a1.getEchelle().getId();
          old_echelon = a1.getEchelon().getId();
        }
        if (a2 != null && !error_date) {
          new_corps = a2.getGrade().getCorps().getId();
          new_grade = a2.getGrade().getId();
          new_echelon = a2.getEchelon().getId();
          new_echelle = a2.getEchelle().getId();
        }
      }
      LocalDate date_debut = LocalDate.parse(date1);
      LocalDate date_fin = LocalDate.parse(date2);
      RCAR c1 = serviceUtil.getRcarByYear(date_debut.getYear());
      BigDecimal rcarMontnat1 = new BigDecimal(0);
      BigDecimal rcarpourcentage1 = new BigDecimal(0);
      if (c1 != null) {
        rcarMontnat1 = rcarMontnat1.add(c1.getMontant_max());
        rcarpourcentage1 = rcarpourcentage1.add(c1.getPors());
      } else {
        response.setFlash("Rcar non disponible dans l'année : " + date_debut.getYear());
        return;
      }
      RCAR c2 = serviceUtil.getRcarByYear(date_fin.getYear());
      BigDecimal rcarMontnat2 = new BigDecimal(0);
      BigDecimal rcarpourcentage2 = new BigDecimal(0);
      if (c2 != null) {
        rcarMontnat2 = rcarMontnat2.add(c2.getMontant_max());
        rcarpourcentage2 = rcarpourcentage2.add(c2.getPors());
      } else {
        response.setFlash("Rcar non disponible dans l'année : " + date_fin.getYear());
        return;
      }
      RCARC comp_old = appservice.getComplementRcarByYear(date_debut.getYear());
      RCARC comp_new = appservice.getComplementRcarByYear(date_fin.getYear());
      BigDecimal compMontant = new BigDecimal(0);
      BigDecimal compPoucentage = new BigDecimal(0);
      if (comp_old != null) {
        compMontant = comp_old.getMontant();
        compPoucentage = comp_old.getPors();
      } else {
        response.setFlash("complement Rcar non disponible dans l'année : " + date_debut.getYear());
        return;
      }
      BigDecimal compMontant_new = new BigDecimal(0);
      BigDecimal compPoucentage_new = new BigDecimal(0);
      if (comp_new != null) {
        compMontant_new = comp_new.getMontant();
        compPoucentage_new = comp_new.getPors();
      } else {
        response.setFlash("complement Rcar non disponible dans l'année : " + date_fin.getYear());
        return;
      }
      OrganismeMetuelle o = employee.getOrganismeMetuelle2();
      MUTUELLE sec = null;
      MUTUELLE ccd = null;
      MUTUELLE amo = null;
      if (o.getId() == 1) {
        sec = appservice.getMutuelleById(2L);
        ccd = appservice.getMutuelleById(3L);
      } else {
        sec = appservice.getMutuelleById(4L);
        ccd = appservice.getMutuelleById(5L);
      }
      amo = appservice.getMutuelleById(1L);
      BigDecimal amo_min = new BigDecimal(0);
      BigDecimal amo_max = new BigDecimal(0);
      BigDecimal amo_purc = new BigDecimal(0);
      if (amo != null) {
        amo_min = amo.getMontant_min();
        amo_max = amo.getMontant_max();
        amo_purc = amo.getPors();
      } else {
        response.setFlash("complement Rcar non disponible dans l'année : ");
        return;
      }
      BigDecimal sec_min = new BigDecimal(0);
      BigDecimal sec_max = new BigDecimal(0);
      BigDecimal sec_purc = new BigDecimal(0);
      if (sec != null) {
        sec_min = sec.getMontant_min();
        sec_max = sec.getMontant_max();
        sec_purc = sec.getPors();
      } else {
        response.setFlash("complement Rcar non disponible dans l'année : ");
        return;
      }
      BigDecimal ccd_min = new BigDecimal(0);
      BigDecimal ccd_max = new BigDecimal(0);
      BigDecimal ccd_purc = new BigDecimal(0);
      if (ccd != null) {
        ccd_min = ccd.getMontant_min();
        ccd_max = ccd.getMontant_max();
        ccd_purc = ccd.getPors();
      } else {
        response.setFlash("complement Rcar non disponible dans l'année : ");
        return;
      }
      if (!error_date) {
        String fileLink =
            ReportFactory.createReport(
                    IReport.EMPLOYEE_ETAT_ENGAGEMENT, "EtatEngagement" + "-${date}")
                .addParam("Locale", ReportSettings.getPrintingLocale(null))
                .addParam("employeeId", employeeId)
                .addParam("newIdechelle", new_echelle)
                .addParam("oldIdechelle", old_echelle)
                .addParam("newIdechelon", new_echelon)
                .addParam("oldIdechelon", old_echelon)
                .addParam("newEnfant", new_children)
                .addParam("oldEnfant", old_children)
                .addParam("situation", situation)
                .addParam("dateStart", java.sql.Date.valueOf(LocalDate.parse(date1)))
                .addParam("dateEnd", java.sql.Date.valueOf(LocalDate.parse(date2)))
                .addParam("old_corps", old_corps)
                .addParam("new_corps", new_corps)
                .addParam("new_grade", new_grade)
                .addParam("old_grade", old_grade)
                .addParam("annee_courante", LocalDate.now().getYear())
                .addParam("hasresponsabilite", hasresponsabilite)
                .addParam("rcarMontantMax1", rcarMontnat1)
                .addParam("rcarpourcentage1", rcarpourcentage1)
                .addParam("rcarMontantMax2", rcarMontnat2)
                .addParam("rcarpourcentage2", rcarpourcentage2)
                .addParam("empMutuelle", o.getId())
                .addParam("amo_min", amo_min)
                .addParam("amo_max", amo_max)
                .addParam("amo_purc", amo_purc)
                .addParam("sec_min", sec_min)
                .addParam("sec_max", sec_max)
                .addParam("sec_purc", sec_purc)
                .addParam("ccd_min", ccd_min)
                .addParam("ccd_max", ccd_max)
                .addParam("ccd_purc", ccd_purc)
                .addParam("compMontant_new", compMontant_new)
                .addParam("compPoucentage_new", compPoucentage_new)
                .addParam("compMontant", compMontant)
                .addParam("compPoucentage", compPoucentage)
                .addParam("situation_old", situation_old)
                .addParam("situation_new", situation_new)
                .addParam("old_children_21", old_children_21)
                .addParam("new_children_21", new_children_21)
                .addParam("zone_id", employee.getZoneEmployee().getId())
                .addParam(
                    "nombreMois",
                    1 + (ChronoUnit.MONTHS.between(LocalDate.parse(date1), LocalDate.parse(date2))))
                .generate()
                .getFileLink();
        LOG.debug("Printing " + "EtatEngagement");
        response.setView(ActionView.define("Etat Engagement").add("html", fileLink).map());
      } else {
        response.setFlash("La date saisie est invalide");
      }

    } else {
      response.setFlash("Un ou plusieurs champs sont vides");
    } /*end emptyField*/
  } /*end methode*/

  public long nbrJour(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    String date_debut = request.getContext().get("date_debut").toString();
    String date_fin = request.getContext().get("date_fin").toString();
    String heure_depart = request.getContext().get("heure_depart").toString();
    String heure_arrivee = request.getContext().get("heure_arrivee").toString();
    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    Date date = formatter.parse(date_debut);
    Date date2 = formatter.parse(date_fin);
    long difference_In_Time = date2.getTime() - date.getTime();
    long difference_In_Days = ((difference_In_Time / (1000 * 60 * 60 * 24)) % 365) + 1;
    String[] h1 = heure_depart.split(":");
    String[] h2 = heure_arrivee.split(":");
    long nbrRepas = difference_In_Days * 3;
    if (Integer.parseInt(h1[0]) >= 11.30) {
      nbrRepas = nbrRepas - 1;
    }
    if (Integer.parseInt(h1[0]) >= 18) {
      nbrRepas = nbrRepas - 1;
    }
    if (Integer.parseInt(h1[0]) >= 0.0) {
      nbrRepas = nbrRepas - 1;
    }
    if (Integer.parseInt(h2[0]) <= 14) {
      nbrRepas = nbrRepas - 1;
    }
    if (Integer.parseInt(h2[0]) <= 21) {
      nbrRepas = nbrRepas - 1;
    }
    if (Integer.parseInt(h2[0]) <= 5) {
      nbrRepas = nbrRepas - 1;
    }
    response.setValue("nbrJour", nbrRepas);
    Long idmission = Long.valueOf(request.getContext().get("id").toString());
    return nbrRepas;
  }

  public void theGrade(ActionRequest request, ActionResponse response) {
    EmployeeMissions employee = request.getContext().asType(EmployeeMissions.class);
    response.setValue("grade", employee.getEmployee().getGrade());
  }

  public void imprimerOrdreMission(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Long id_mission = Long.valueOf(request.getContext().get("id").toString());
    // appPurchaseService.commandeAchat(id_compPrix);
    System.out.println(id_mission);
    EmployeeMissions employeeMission = Beans.get(EmployeeMissionsRepository.class).find(id_mission);
    String date_debut = request.getContext().get("date_debut").toString();
    String date_fin = request.getContext().get("date_fin").toString();
    String heure_depart = request.getContext().get("heure_depart").toString();
    String heure_arrivee = request.getContext().get("heure_arrivee").toString();
    String ordre = employeeMission.getOrdre();
    String employee = employeeMission.getEmployee().getName();
    String grade = employeeMission.getGrade().getName();
    String ville_arrivee = employeeMission.getVille_arrivee().getName();
    String ville_depart = employeeMission.getVille_depart().getName();
    String dateOrdre = request.getContext().get("dateOrdre").toString();
    String fileLink =
        ReportFactory.createReport(IReport.OrdreMission, "OrdreMission")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("employee", employee)
            .addParam("grade", grade)
            .addParam("ordre", ordre)
            .addParam("ville_arrivee", ville_arrivee)
            .addParam("ville_depart", ville_depart)
            .addParam("date_debut", date_debut)
            .addParam("date_fin", date_fin)
            .addParam("heure_depart", heure_depart)
            .addParam("heure_arrivee", heure_arrivee)
            .addParam("dateOrdre", dateOrdre)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de mission").add("html", fileLink).map());
  }

  public void imprimerEtatSomme(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Employee map = (Employee) request.getContext().get("emplo");
    Long idEmployee = map.getId();
    String mois1 = request.getContext().get("mois1").toString();
    String mois2 = request.getContext().get("mois2").toString();
    List<EmployeeMissions> ls =
        Beans.get(EmployeeMissionsRepository.class)
            .all()
            .filter("self.employee=?1", idEmployee)
            .fetch();
    if (ls.size() > 0) {
      java.math.BigDecimal somme = new java.math.BigDecimal(0);
      for (EmployeeMissions tmp : ls) {
        if (tmp.getIndemnite() != null) {
          java.math.BigDecimal taux = tmp.getIndemnite().getTaux();
          java.math.BigDecimal montant = tmp.getMontant_carburant();
          somme = somme.add(montant.multiply(taux)).setScale(2, RoundingMode.HALF_UP);
        }
      }
      Long id = (Long) request.getContext().get("id");
      Beans.get(EmployeeAdvanceService.class).updateEtatSomme(id, somme);
      long tt = somme.longValue();
      String lettre_montant = ConvertNomreToLettres.getStringMontant(somme);
      String fileLink =
          ReportFactory.createReport(IReport.EtatSommeDues, "EtatSommeDues")
              .addParam("employer_dial_mission", idEmployee)
              .addParam("le_premier", java.sql.Date.valueOf(LocalDate.parse(mois1)))
              .addParam("le_dernier", java.sql.Date.valueOf(LocalDate.parse(mois2)))
              .addParam("lettre_montant", lettre_montant)
              .generate()
              .getFileLink();
      response.setView(ActionView.define("Etat des sommes").add("html", fileLink).map());
    } else {
      response.setFlash("Aucune Mission disponible");
    }
  }

  public void imprimerEtatSomme2(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Employee map = (Employee) request.getContext().get("emplo");
    Long idEmployee = map.getId();
    String mois1 = request.getContext().get("mois1").toString();
    String mois2 = request.getContext().get("mois2").toString();
    List<EmployeeMissions> ls =
        Beans.get(EmployeeMissionsRepository.class)
            .all()
            .filter("self.employee=?1", idEmployee)
            .fetch();
    if (ls.size() > 0) {
      java.math.BigDecimal somme = new java.math.BigDecimal(0);
      for (EmployeeMissions tmp : ls) {
        java.math.BigDecimal taux = tmp.getGroupe().getIndemniteInterne1();
        java.math.BigDecimal montant = new BigDecimal(tmp.getNbrJour());
        somme = somme.add(montant.multiply(taux)).setScale(2, RoundingMode.HALF_UP);
      }
      Long id = (Long) request.getContext().get("id");
      Beans.get(EmployeeAdvanceService.class).updateEtatSomme(id, somme);
      String lettre_m = ConvertNomreToLettres.getStringMontant(somme);
      String fileLink =
          ReportFactory.createReport(IReport.EtatSommeDuesJour, "EtatSommeDues")
              .addParam("employer_dial_mission", idEmployee)
              .addParam("le_premier", java.sql.Date.valueOf(LocalDate.parse(mois1)))
              .addParam("le_dernier", java.sql.Date.valueOf(LocalDate.parse(mois2)))
              .addParam("lettre_m", lettre_m)
              .generate()
              .getFileLink();
      response.setView(ActionView.define("Etat des sommes").add("html", fileLink).map());
    } else {
      response.setFlash("Aucune Mission disponible");
    }
  }

  public void imprimerOrdrePaiement(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    RubriqueBudgetaireGenerale rubrique =
        (RubriqueBudgetaireGenerale) request.getContext().get("rubrique");
    Integer id_etatSomme = (Integer) request.getContext().get("_id");
    Long id_etat = id_etatSomme.longValue();
    Long id = rubrique.getId();
    RubriqueBudgetaireGenerale r = Beans.get(RubriqueBudgetaireGeneraleRepository.class).find(id);
    EmployeeEtatSomme etat_somme = Beans.get(EmployeeEtatSommeRepository.class).find(id_etat);
    String code_budget = r.getCodeBudg();
    String rub_name = r.getName();
    String somme = etat_somme.getMontant().toString();
    String name_employer = etat_somme.getEmplo().getName();
    String grade_name = etat_somme.getEmplo().getGrade().getName();
    String rib = etat_somme.getEmplo().getRib();
    BigDecimal let = new BigDecimal(somme);
    Long let2 = let.longValue();
    String lettre_m = ConvertNomreToLettres.getStringMontant(let);
    String fileLink =
        ReportFactory.createReport(IReport.OrdredePaiement1, "OrdredePaiement1")
            .addParam("employer", name_employer)
            .addParam("rub", rub_name)
            .addParam("som", somme)
            .addParam("code", code_budget)
            .addParam("grade", grade_name)
            .addParam("rib", rib)
            .addParam("lettre_m", lettre_m)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de paiement").add("html", fileLink).map());
  }

  public void verifier_rib_employee(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Map<String, Object> _maps = request.getData();
    Map<String, Object> maps = (Map<String, Object>) _maps.get("context");
    String rib = (String) maps.get("rib");
    Beans.get(AppHumanResourceServiceImpl.class).save_RIB(rib);
  }

  public void setDate(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Long id_employee = (Long) request.getContext().get("id");
    LocalDate date_p = (LocalDate) request.getContext().get("dateProlongation");

    String sql =
        "select netapayer, ir, mois , annee from hr_etat_salaire\n"
            + "where employee = "
            + id_employee
            + "\n"
            + "and cast(concat(01,'/',"
            + date_p.minusMonths(1).getMonthValue()
            + ",'/',"
            + date_p.minusMonths(1).getYear()
            + ") as date) >= cast(concat(01,'/',mois,'/',annee) as date)\n"
            + "order by cast(concat(01,'/',mois,'/',annee) as date) desc limit 1";

    int nb_etat =
        Beans.get(EtatSalaireRepository.class).all().fetch().stream()
            .filter(
                etatSalaire -> {
                  LocalDate dd =
                      LocalDate.parse(
                          String.format("%02d-%02d-01", date_p.getYear(), date_p.getMonthValue()));
                  LocalDate d2 =
                      LocalDate.parse(
                          String.format(
                              "%02d-%02d-01", etatSalaire.getAnnee(), etatSalaire.getMois()));
                  return dd.compareTo(d2) >= 0;
                })
            .collect(Collectors.toList())
            .size();
    if (nb_etat > 0) {
      Object o = RunSqlRequestForMe.runSqlRequest_Object(sql);
      if (o != null) {
        Object[] s = (Object[]) o;
        response.setValue("net", s[0]);
        response.setValue("irsalaire", s[1]);
      } else {
        response.setFlash("Aucun salaire net trouvé");
        response.setValue("dateProlongation", null);
      }
    } else {
      response.setFlash("Aucun salaire net trouvé");
      response.setValue("dateProlongation", null);
    }
  }

  // @Transactional
  public void addToHistoriqueRib(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Long id_employee = (Long) request.getContext().get("id");
    String banque = request.getContext().get("banque").toString();
    if (id_employee != null) {
      String rib = request.getContext().get("rib").toString();
      Employee employee = Beans.get(EmployeeRepository.class).find(id_employee);
      Beans.get(AppHumanResourceServiceImpl.class)
          .save_RibHistorique(rib, employee, id_employee, banque);
    }
  }

  public void addHistoriqueResponsabilite(ActionRequest request, ActionResponse response) {
    Long id_employee = (Long) request.getContext().get("id");

    if (appservice.checkNeedAddHitorique(id_employee)
        && request.getContext().get("responsabilite") != null) {
      // appservice.updateResponsabilite(id_employee);
      // appservice.addHistoriqueResponsabilite(id_employee);
    }
  }

  // nouv_etat_engagement
  public void Imprimer_EtatEngagement(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String rappel = "normal";
    Integer id = (Integer) request.getContext().get("_id");
    Long id_emp = Long.valueOf(id);
    if (request.getContext().get("dateDebut") == null) {
      response.setError("Le champ <b>Date Début</b> est obligatoire");
      return;
    }
    if (request.getContext().get("dateFin") == null) {
      response.setError("Le champ <b>Date Fin</b> est obligatoire");
      return;
    }
    if (request.getContext().get("type_doc") == null) {
      response.setError("Le <b>Type de Document</b> est obligatoire");
      return;
    }
    if (request.getContext().get("rappel") != null) {
      rappel = request.getContext().get("rappel").toString();
    }
    String date_debut = request.getContext().get("dateDebut").toString();
    String date_fin = request.getContext().get("dateFin").toString();
    String type_doc = request.getContext().get("type_doc").toString();
    if (appservice.verifier_date_debut(id_emp, date_debut)) {
      String date = appservice.getDateDebutAugmentationByEmployer(id_emp);
      String msg = "<p>La Date début est non Valide</p><p>Date debut minimum : " + date + "</p>";
      response.setFlash(msg);
      return;
    }
    LocalDate dateDebut = LocalDate.parse(date_debut);
    LocalDate dateFin = LocalDate.parse(date_fin);
    String dateDay = "Non";
    if ((dateDebut.getDayOfMonth() != 1 || dateFin.getDayOfMonth() != dateFin.lengthOfMonth())
        && dateDebut.getMonthValue() == dateFin.getMonthValue()) {
      // LE meme mois get difference by day
      dateDay = "Oui";
    }
    String data1 = appservice.imprimer_EtatEngagement(dateDebut, dateFin, id_emp, rappel);
    String type_etat = request.getContext().get("type_engagement").toString();
    boolean cv = appservice.verifier_etatEncNouv(data1);
    if (!cv) {
      type_etat = "normal";
    }
    String data_indem = appservice.getDataIndemnite(date_debut, date_fin, id_emp, rappel);
    String isFirstRappel = "non";
    if (rappel.equals("rappel")) {
      appservice.update_rappel(id_emp, dateFin);
      isFirstRappel = appservice.isFirstRappel(id_emp) ? "oui" : "non";
    }
    isFirstRappel =
        (isFirstRappel.equals("oui")
                || !appservice.checkDateDebutIsAfterDateStart(date_debut, id_emp))
            ? "oui"
            : "non";

    /*String report = IReport.EMPLOYEE_ETAT_ENGAGEMENT_v2;
    if (request.getContext().get("type_engagement") != null
        && request.getContext().get("type_engagement").toString().equals("normal"))*/
    String report = IReport.EMPLOYEE_ETAT_ENGAGEMENT_v3;

    String fileLink =
        ReportFactory.createReport(report, "EtatEngagementV2" + "-${date}")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("listdata", data1)
            .addParam("dataIndem", data_indem)
            .addParam("dateDay", dateDay)
            .addParam("rappel", rappel)
            .addParam("isFirstRappel", isFirstRappel)
            .addParam("type_etat", type_etat)
            .addFormat(type_doc)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat Engagement").add("html", fileLink).map());
  }

  public void EnregistrerCreditEmployee(ActionRequest request, ActionResponse response) {
    Object obj = request.getContext();
    //  Beans.get(AppHumanResourceServiceImpl.class).saveCreditEmployee(request,response);
  }

  public void AjouterNouveauCredit(ActionRequest request, ActionResponse response) {
    String id = request.getContext().get("id").toString();
    response.setView(
        ActionView.define("Nouveau credit")
            .model(GestionCredit.class.getName())
            .add("grid", "GestionCredit-grid")
            .add("form", "GestionCredit-form")
            .domain("self.employee.id = " + id)
            .context("id_Employee", id)
            .map());
  }

  public void NewCredit(ActionRequest request, ActionResponse response) {
    String test = request.getContext().get("id_Employee").toString();
    response.setValue("id_Employee", test);
  }

  /*public void getloadMontantCredit(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    GestionCredit credit = gestionCreditRepository.find(id);
    String lettre = ConvertNomreToLettres.getStringMontant(credit.getRemboursement());
    credit.setMontantLettre(lettre);
    gestionCreditRepository.save(credit);
    response.setReload(true);
  }*/

  public void NewConge(ActionRequest request, ActionResponse response) {
    String test = request.getContext().get("id_Employee").toString();
    response.setValue("id_Employee", test);
    if (test == null || test.equals("")) return;
    int[] nbr = appservice.getNombreDayRestByEmp(Long.valueOf(test), LocalDate.now());
    response.setValue("nbrDayMax_conge", BigDecimal.valueOf(nbr[0]));
    response.setValue("nbrDayMax_autorisation", BigDecimal.valueOf(nbr[1]));
  }

  public void AddEmployeToCredit(ActionRequest request, ActionResponse response) {
    Beans.get(AppHumanResourceServiceImpl.class).AddEmployeToCredit(request, response);
  }

  public void UpdateEmployeToCredit(ActionRequest request, ActionResponse response) {
    Beans.get(AppHumanResourceServiceImpl.class).UpdateEmployeToCredit(request, response);
  }

  public void AjouterNouveauConge(ActionRequest request, ActionResponse response) {
    String id = request.getContext().get("id").toString();
    response.setView(
        ActionView.define("Nouveau congé")
            .model(Conge.class.getName())
            .add("grid", "conge-grid")
            .add("form", "conge-form")
            .param("show-confirm", "false")
            .param("forceEdit", "true")
            .domain("self.employee.id = " + id)
            .context("id_Employee", id)
            .map());
  }

  public void AddEmployeToConge(ActionRequest request, ActionResponse response) {
    Long id = 0l;
    if (request.getContext().get("id") != null) {
      id = (Long) request.getContext().get("id");
    }
    Long id_employee = Long.valueOf(request.getContext().get("id_Employee").toString());
    TypeConge typeConge = (TypeConge) request.getContext().get("typeConge");
    BigDecimal max_conge = new BigDecimal(request.getContext().get("nbrDayMax_conge").toString());
    BigDecimal max_autorisation =
        new BigDecimal(request.getContext().get("nbrDayMax_autorisation").toString());
    Integer duree = (Integer) request.getContext().get("duree");
    LocalDate dateDebut = LocalDate.parse(request.getContext().get("dateDebut").toString());
    LocalDate dateFin = LocalDate.parse(request.getContext().get("dateFin").toString());
    String nature = (String) request.getContext().get("typeGeneraleConge");
    BigDecimal max = nature.equals("conge") ? max_conge : max_autorisation;
    String ocasion =
        request.getContext().get("occasion") != null
            ? request.getContext().get("occasion").toString()
            : "";
    String monasaba =
        request.getContext().get("monasaba") != null
            ? request.getContext().get("monasaba").toString()
            : "";

    Employee employee = employeeRepository.find(id_employee);
    List<Conge> listconge = appservice.getListCongeByIdEmployer(id_employee);

    if (id == 0
        && !(typeConge.getTypeCongeGenerale().equals("autorisation")
            && !typeConge.getIsRetranchable10Jours())) {
      if ((appservice
                  .getNombreDayRestByEmp(id_employee, LocalDate.now())[
                  typeConge.getTypeCongeGenerale().equals("conge") ? 0 : 1]
              - duree)
          < 0) {
        response.setFlash(
            "Il vous reste "
                + (max.intValue() - duree)
                + " jours, Veuillez saisir une durée valable");
        return;
      }
    } else if (!(typeConge.getTypeCongeGenerale().equals("autorisation")
        && !typeConge.getIsRetranchable10Jours())) {
      Conge c = appservice.getCongeById(id);
      if (((appservice
                      .getNombreDayRestByEmp(id_employee, LocalDate.now())[
                      typeConge.getTypeCongeGenerale().equals("conge") ? 0 : 1]
                  + c.getDuree())
              - duree)
          < 0) {
        response.setFlash(
            "Il vous reste "
                + (max.intValue() - duree)
                + " jours, Veuillez saisir une durée valable");
        return;
      }
    }

    if (request.getContext().get("monasaba") == null && nature.equals("autorisation")) {
      response.setFlash("Le champs occasion Arabe est obligatoire");
      return;
    }

    Boolean isInInterval = false;
    for (Conge ls : listconge) {
      Boolean test1 =
          dateDebut.compareTo(ls.getDateDebut()) >= 0 && ls.getDateFin().compareTo(dateDebut) >= 0;
      Boolean test2 =
          dateFin.compareTo(ls.getDateDebut()) >= 0 && ls.getDateFin().compareTo(dateFin) >= 0;
      if (isInInterval == false && (test1 || test2) && ls.getId() != id) {
        isInInterval = true;
      }
    }

    if (isInInterval) {
      response.setFlash("Cet employee a deja un congé dans cette periode");
      return;
    } else {
      Conge conge = null;
      conge = id == 0 ? new Conge() : appservice.getCongeById(id);
      conge.setTypeConge(typeConge);
      conge.setDuree(duree.intValue());
      conge.setGrade(employee.getGrade());
      conge.setDateDebut(dateDebut);
      conge.setOccasion(ocasion);
      conge.setMonasaba(monasaba);
      conge.setDateFin(dateFin);
      conge.setNbrDayRest(id == 0 ? max.subtract(BigDecimal.valueOf(duree)) : max);
      conge.setTypeGeneraleConge(nature);
      conge.setAnnee(LocalDate.now().getYear());
      conge.setEmployee(employee);
      conge.setId_Employee(id_employee.toString());
      appservice.saveConge(conge);

      /*response.setView(
              ActionView.define("Liste des conges")
                      .model(Conge.class.getName())
                      .add("grid", "conge-grid")
                      .add("form", "conge-form")
                      .param("forceEdit", "true")
                      .domain("self.employee.id = " + id_employee)
                      .context("id_Employee", id_employee)
                      .map());

      response.setCanClose(true);*/
    }
  }

  public void UpdateEmployeToConge(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    appservice.deleteConge(id);
    AddEmployeToConge(request, response);
  }

  public void verifierRappel(ActionRequest request, ActionResponse response) {
    Long id_emp;
    String date_debut = "dateDebut";
    String date_fin = "dateFin";
    if (request.getContext().get("_id") != null) {
      id_emp = Long.valueOf(request.getContext().get("_id").toString());
    } else {
      Map<String, Object> emp = (Map<String, Object>) request.getContext().get("employer");
      if (emp == null) return;
      id_emp = Long.valueOf(emp.get("id").toString());
      date_debut = "date_debut";
      date_fin = "date_fin";
    }
    if (appservice.getEmployer_has_Augmentation(id_emp) != null) {
      if (appservice.getNbrAugmentation(id_emp) == 1) {
        response.setReadonly("rappel", true);
        response.setValue("rappel", "rappel");
        LocalDate d_fin =
            LocalDate.parse(
                LocalDate.now().getYear()
                    + "-"
                    + ((LocalDate.now().getMonthValue() < 10)
                        ? "0" + LocalDate.now().getMonthValue()
                        : LocalDate.now().getMonthValue())
                    + "-"
                    + LocalDate.now().lengthOfMonth());
        LocalDate d_debut = appservice.getEmployer_has_Augmentation(id_emp).getDateDebut();
        response.setValue(date_debut, d_debut);
        response.setValue(date_fin, d_fin);
        response.setReadonly(date_debut, true);
      } else {
        response.setReadonly("rappel", false);
        response.setValue("rappel", "normal");
        response.setValue(date_debut, null);
        response.setValue(date_fin, null);
        response.setReadonly(date_debut, false);
      }
      response.setHidden("rappel", false);
    } else {
      response.setValue("rappel", "normal");
      response.setHidden("rappel", true);
      response.setReadonly("rappel", false);
      response.setValue(date_debut, null);
      response.setValue(date_fin, null);
      response.setReadonly(date_debut, false);
    }
  }

  public void OnchangeReppel(ActionRequest request, ActionResponse response) {
    String s = request.getContext().get("rappel").toString();
    Long id_emp = Long.valueOf(request.getContext().get("_id").toString());
    if (s.equals("rappel")) {
      LocalDate d_fin =
          LocalDate.parse(
              LocalDate.now().getYear()
                  + "-"
                  + ((LocalDate.now().getMonthValue() < 10)
                      ? "0" + LocalDate.now().getMonthValue()
                      : LocalDate.now().getMonthValue())
                  + "-"
                  + LocalDate.now().lengthOfMonth());
      LocalDate d_debut = appservice.getEmployer_has_Augmentation(id_emp).getDateDebut();
      response.setValue("dateDebut", d_debut);
      response.setValue("dateFin", d_fin);
      response.setReadonly("dateDebut", true);

    } else {
      response.setValue("dateDebut", null);
      response.setValue("dateFin", null);
      response.setReadonly("dateDebut", false);
    }
  }

  public void imprimerEtatEngagement(ActionRequest request, ActionResponse response)
      throws AxelorException {
    HashMap<String, Object> emp = (HashMap<String, Object>) request.getContext().get("employer");
    request.getContext().put("_id", emp.get("id"));
    LocalDate dateDebut = LocalDate.parse(request.getContext().get("date_debut").toString());
    request.getContext().put("dateDebut", dateDebut);
    LocalDate dateFin = LocalDate.parse(request.getContext().get("date_fin").toString());
    request.getContext().put("dateFin", dateFin);
    String type_doc = request.getContext().get("type_doc").toString();
    request.getContext().put("type_doc", type_doc);
    String type_engagement = request.getContext().get("type_engagement").toString();
    request.getContext().put("type_engagement", type_engagement);
    Imprimer_EtatEngagement(request, response);
  }

  public void verifier_liste_rappel(ActionRequest request, ActionResponse response) {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    int nbr = appservice.getNbrEtatRappel(d1, d2);
    if (nbr > 0) {
      response.setReadonly("date_debut", true);
      response.setReadonly("date_fin", true);
      response.setHidden("btn_1", false);
    } else {
      response.setReadonly("date_debut", false);
      response.setReadonly("date_fin", false);
      response.setHidden("btn_1", true);
      response.setFlash("Aucun Rappel dans cette periode");
    }
  }

  private void imprimer_OP_Generale(
      ActionResponse response,
      String reportName,
      LocalDate d1,
      LocalDate d2,
      String champs,
      String titre)
      throws AxelorException {
    BigDecimal x = appservice.getTotalByField(d1, d2, champs);
    String sm = ConvertNomreToLettres.getStringMontant(x);
    String fileLink =
        ReportFactory.createReport(reportName, titre + "-${date}")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("d1", java.sql.Date.valueOf(d1))
            .addParam("d2", java.sql.Date.valueOf(d2))
            .addParam("som", x)
            .addParam("tot_tot", sm)
            .generate()
            .getFileLink();
    response.setView(ActionView.define(titre).add("html", fileLink).map());
  }

  private void imprimer_OP_Generale(
      ActionResponse response,
      String reportName,
      LocalDate d1,
      LocalDate d2,
      String titre,
      String[] champs)
      throws AxelorException {
    BigDecimal x = BigDecimal.ZERO;
    for (int i = 0; i < champs.length; i++) {
      x = x.add(appservice.getTotalByField(d1, d2, champs[i]).setScale(2, RoundingMode.HALF_UP));
    }
    String sm = ConvertNomreToLettres.getStringMontant(x);
    String fileLink =
        ReportFactory.createReport(reportName, titre + "-${date}")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("d1", java.sql.Date.valueOf(d1))
            .addParam("d2", java.sql.Date.valueOf(d2))
            .addParam("som", x)
            .addParam("tot_tot", sm)
            .generate()
            .getFileLink();
    response.setView(ActionView.define(titre).add("html", fileLink).map());
  }

  // hatim add methode
  // @Update_etatSow_true(...) changer etat a true
  public void imprimer_Etat_Grid(
      ActionResponse response,
      LocalDate d1,
      LocalDate d2,
      String champs,
      String titre,
      String nameGrid,
      String toShowField)
      throws AxelorException {
    // appservice.Update_etatSow_true(d1,d2,toShowField);
    ActionViewBuilder act = ActionView.define(titre);
    act.add("grid", nameGrid + "_grid")
        .model(EtatDesRappelSurSalaire.class.getName())
        .add("form", "etat_salaire_emp_form")
        .param("popup", "true")
        .param("popup-save", "false")
        .param("show-confirm", "false")
        .param("search-filters", "false")
        .param("forceEdit", "true")
        .context("date_debut", d1)
        .context("date_fin", d2)
        .context("champs", champs)
        .context("toShowField", toShowField);
    if (champs.equals("omfam_sm")) {
      String ids_omfam = appservice.get_ids_omfam("omfam_sm");
      act.domain(
          "self.date_debut>='"
              + d1
              + "' and self.date_fin<='"
              + d2
              + "' and self.id_employer in ("
              + ids_omfam
              + ")");
    } else if (champs.equals("mgpap_sm")) {
      String ids_omfam = appservice.get_ids_omfam("omfam_sm");
      act.domain(
          "self.date_debut>='"
              + d1
              + "' and self.date_fin<='"
              + d2
              + "' and self.id_employer not in ("
              + ids_omfam
              + ")");
    } else {
      act.domain("self.date_debut>='" + d1 + "' and self.date_fin<='" + d2 + "'");
    }
    response.setView(act.map());

    /* response.setView(
    ActionView.define(titre)
            .model(EtatDesRappelSurSalaire.class.getName())
            .add("grid", nameGrid+"_grid")
            //.add("form",nameGrid+"_form")
            .add("form","etat_salaire_emp_form")
            .param("popup","true")
            .param("popup-save", "false")
            .param("show-confirm", "false")
            .param("search-filters", "false")
            .param("forceEdit", "true")
            .context("date_debut",d1)
            .context("date_fin",d2)
            .context("champs",champs)
            .context("toShowField",toShowField)
            .map());*/
  }

  private void imprimer_doc_Generale(
      ActionResponse response,
      String reportName,
      LocalDate d1,
      LocalDate d2,
      String champs,
      String toShowField,
      String titre)
      throws AxelorException {
    BigDecimal x = appservice.getTotalByField(d1, d2, champs, toShowField);
    String sm = ConvertNomreToLettres.getStringMontant(x);
    String fileLink =
        ReportFactory.createReport(reportName, titre + "-${date}")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("d1", java.sql.Date.valueOf(d1))
            .addParam("d2", java.sql.Date.valueOf(d2))
            .addParam("som", x)
            .addParam("tot_tot", sm)
            .generate()
            .getFileLink();
    response.setView(ActionView.define(titre).add("html", fileLink).map());
  }

  private void imprimer_doc_Generale(
      ActionResponse response,
      String reportName,
      LocalDate d1,
      LocalDate d2,
      String toShowField,
      String titre,
      String[] champs)
      throws AxelorException {
    BigDecimal x = BigDecimal.ZERO;
    for (int i = 0; i < champs.length; i++) {
      String ch = champs[i];
      x =
          x.add(
              appservice
                  .getTotalByField(d1, d2, ch, toShowField)
                  .setScale(2, RoundingMode.HALF_UP));
    }
    String sm = ConvertNomreToLettres.getStringMontant(x);
    String fileLink =
        ReportFactory.createReport(reportName, titre + "-${date}")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("d1", java.sql.Date.valueOf(d1))
            .addParam("d2", java.sql.Date.valueOf(d2))
            .addParam("som", x)
            .addParam("tot_tot", sm)
            .generate()
            .getFileLink();
    response.setView(ActionView.define(titre).add("html", fileLink).map());
  }

  private void imprimer_doc_Generale(
      ActionResponse response,
      String reportName,
      LocalDate d1,
      LocalDate d2,
      String titre,
      Map<String, String[]> map)
      throws AxelorException {
    BigDecimal x = BigDecimal.ZERO;
    for (Map.Entry<String, String[]> tmp : map.entrySet()) {
      String[] champs = tmp.getValue();
      for (int i = 0; i < champs.length; i++) {
        String ch = champs[i];
        x =
            x.add(
                appservice
                    .getTotalByField(d1, d2, ch, tmp.getKey())
                    .setScale(2, RoundingMode.HALF_UP));
      }
    }
    String sm = ConvertNomreToLettres.getStringMontant(x);
    String fileLink =
        ReportFactory.createReport(reportName, titre + "-${date}")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("d1", java.sql.Date.valueOf(d1))
            .addParam("d2", java.sql.Date.valueOf(d2))
            .addParam("som", x)
            .addParam("tot_tot", sm)
            .generate()
            .getFileLink();
    response.setView(ActionView.define(titre).add("html", fileLink).map());
  }

  public void load_grid_emp(ActionRequest request, ActionResponse response) throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    imprimer_Etat_Grid(
        response, d1, d2, "net_payer", "Etat Personnel", "etat_salaire_emp", "show_etat_emp");
  }

  public void load_grid_Rcar(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    imprimer_Etat_Grid(response, d1, d2, "rcar", "Etat RCAR", "etat_rcar", "show_rcar");
  }

  public void load_grid_IR(ActionRequest request, ActionResponse response) throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    imprimer_Etat_Grid(response, d1, d2, "ir", "Impot sur revenu", "etat_ir", "show_ir");
  }

  public void load_grid_AMO(ActionRequest request, ActionResponse response) throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    imprimer_Etat_Grid(
        response, d1, d2, "amo", "Assurance Maladie Obligatoire", "etat_amo", "show_amo");
  }

  public void imprimer_Etat_rcar(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String champs = request.getContext().get("champs").toString();
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    imprimer_doc_Generale(response, IReport.EtatRcarRapell, d1, d2, champs, toShowField, titre);
  }

  public void imprimer_OP_RCAR(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    Map<String, String[]> map = new HashMap<>();
    map.put(toShowField, new String[] {"rcar", "c_rcar"});
    imprimer_doc_Generale(
        response,
        IReport.OrdrePayementRcar,
        d1,
        d2,
        toShowField,
        titre,
        new String[] {"rcar", "c_rcar"});
  }

  public void imprimer_OV_Rcar(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    imprimer_doc_Generale(
        response,
        IReport.OrdreVirementRcar,
        d1,
        d2,
        toShowField,
        titre,
        new String[] {"rcar", "rcar", "c_rcar", "rcar"});
  }

  public void imprimer_EtatDesRappelSurSalaire(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String champs = request.getContext().get("champs").toString();
    String toShowField = request.getContext().get("toShowField").toString();
    String title = appservice.getTitre(request);
    imprimer_doc_Generale(
        response, IReport.etatRappelSurSalaire, d1, d2, champs, toShowField, title);
  }

  public void imprimer_Ordre_de_virement(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String champs = request.getContext().get("champs").toString();
    String toShowField = request.getContext().get("toShowField").toString();
    String title = appservice.getTitre(request);
    imprimer_doc_Generale(
        response, IReport.OrdreDeVirementRappel, d1, d2, champs, toShowField, title);
  }

  public void imprimer_cotisation_patronal(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String champs = request.getContext().get("champs").toString();
    String toShowField = request.getContext().get("toShowField").toString();
    imprimer_doc_Generale(
        response,
        IReport.CotisationPatronal,
        d1,
        d2,
        toShowField,
        "Etat Rcar Patronal",
        new String[] {"rcar", "c_rcar"});
  }

  public void imprimer_OP_RCAR_Patronal(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    Map<String, String[]> map = new HashMap<>();
    map.put(toShowField, new String[] {"rcar", "rcar", "c_rcar"});

    imprimer_doc_Generale(
        response,
        IReport.OrdrePayementRcarPatronal,
        d1,
        d2,
        toShowField,
        titre,
        new String[] {"rcar", "rcar", "c_rcar"});
  }

  public void imprimer_OP_MT_TIT(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    String champs = request.getContext().get("champs").toString();
    Map<String, String[]> map = new HashMap<>();
    map.put(toShowField, new String[] {champs});

    imprimer_doc_Generale(response, IReport.OP_MT_TIT, d1, d2, champs, toShowField, titre);
  }

  public void imprimer_impotSurRevenuRappel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    String champs = request.getContext().get("champs").toString();
    imprimer_doc_Generale(response, IReport.ImpotSurRevenu, d1, d2, champs, toShowField, titre);
  }

  public void imprimer_OP_IR(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    String champs = request.getContext().get("champs").toString();
    Map<String, String[]> map = new HashMap<>();
    map.put(toShowField, new String[] {champs});

    imprimer_doc_Generale(response, IReport.OrdrePayementIR, d1, d2, champs, toShowField, titre);
  }

  public void imprimer_OV_IR(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    String champs = request.getContext().get("champs").toString();
    imprimer_doc_Generale(response, IReport.OrdreVirementIR, d1, d2, champs, toShowField, titre);
  }

  public void imprimer_OP_AMO(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    Map<String, String[]> map = new HashMap<>();
    map.put(toShowField, new String[] {"amo"});

    imprimer_doc_Generale(response, IReport.OrdrePayementAMO, d1, d2, "amo", toShowField, titre);
  }

  public void imprimer_Etat_AMO(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    imprimer_doc_Generale(
        response, IReport.etatAmoRappel, d1, d2, toShowField, titre, new String[] {"amo", "amo"});
  }

  public void imprimer_OP_CNOPS(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    Map<String, String[]> map = new HashMap<>();
    map.put(toShowField, new String[] {"amo"});

    imprimer_doc_Generale(response, IReport.OrdrePayementCnops, d1, d2, "amo", toShowField, titre);
  }

  public void imprimer_OP_MGPAP(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    Map<String, String[]> map = new HashMap<>();
    map.put(toShowField, new String[] {"mgpap_sm", "mgpap_ccd"});
    imprimer_doc_Generale(
        response,
        IReport.OP_MGPAP,
        d1,
        d2,
        toShowField,
        titre,
        new String[] {"mgpap_sm", "mgpap_ccd"});
  }

  public void imprimer_ETAT_MGPAP(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    imprimer_doc_Generale(
        response,
        IReport.Etat_MT_MGPAP,
        d1,
        d2,
        toShowField,
        titre,
        new String[] {"mgpap_sm", "mgpap_ccd"});
  }

  public void imprimer_Etat_OMFAM(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    imprimer_doc_Generale(
        response,
        IReport.Etat_MT_OMFAM,
        d1,
        d2,
        toShowField,
        titre,
        new String[] {"omfam_sm", "omfam_caad"});
  }

  public void load_grid_MGPAP(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    imprimer_Etat_Grid(
        response, d1, d2, "mgpap_sm", "MUTUELLE MGPAP (SM + CCD)", "etat_mgpap", "show_mgpap");
  }

  public void load_grid_OMFAM(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    imprimer_Etat_Grid(
        response, d1, d2, "omfam_sm", "MUTUELLE OMFAM (SM + CCD)", "etat_mgpap", "show_mgpap");
  }

  public void imprimer_OP_OMFAM(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String toShowField = request.getContext().get("toShowField").toString();
    String titre = appservice.getTitre(request);
    Map<String, String[]> map = new HashMap<>();
    map.put(toShowField, new String[] {"omfam_sm", "omfam_caad"});
    imprimer_doc_Generale(
        response,
        IReport.OP_Omfam,
        d1,
        d2,
        toShowField,
        titre,
        new String[] {"omfam_sm", "omfam_caad"});
  }

  public void imprimer_OV_CNOPS(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    String titre = appservice.getTitre(request);
    // Map<toshow,Array champs>
    Map<String, String[]> map = new HashMap<>();
    map.put(
        "show_etat_emp", new String[] {"amo", "mgpap_sm", "mgpap_ccd", "omfam_sm", "omfam_caad"});
    map.put("show_mgpap", new String[] {"amo"});
    imprimer_doc_Generale(response, IReport.OV_CNOPS, d1, d2, titre, map);
  }

  public void imprimer_BD_Emission(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    LocalDate d2 = LocalDate.parse(request.getContext().get("date_fin").toString());
    Map<String, String[]> map = new HashMap<>();
    map.put(
        "show_etat_emp",
        new String[] {
          "net_payer",
          "rcar",
          "c_rcar",
          "amo",
          "mgpap_sm",
          "mgpap_ccd",
          "omfam_sm",
          "omfam_caad",
          "ir"
        });
    map.put("show_rcar", new String[] {"rcar", "rcar", "c_rcar"});
    map.put("show_amo", new String[] {"amo"});
    imprimer_doc_Generale(
        response, IReport.BD_Emission_Rappel, d1, d2, "Bordereau d'Emission", map);
  }

  public void get_duree_congee(ActionRequest request, ActionResponse response)
      throws AxelorException {
    LocalDate d1 = null;
    LocalDate d2 = null;
    if (request.getContext().get("typeGeneraleConge") != null) {
      String nature = request.getContext().get("typeGeneraleConge").toString();
      if (nature.equals("conge")) {
        if (request.getContext().get("dateDebut") == null
            || request.getContext().get("dateFin") == null) return;
        d1 = (LocalDate) request.getContext().get("dateDebut");
        d2 = (LocalDate) request.getContext().get("dateFin");

        int duree = appservice.getnbrDayWork(d1, d2);
        List<JoursFeries> ls = appservice.getJourFerierInDate(d1, d2);
        if (ls != null && ls.size() > 0) {
          for (JoursFeries j : ls) {
            duree -= appservice.calculateNbrDayOff(d1, d2, j);
          }
        }
        response.setValue("duree", duree);

      } else if (nature.equals("autorisation")
          && request.getContext().get("dateDebut") != null
          && request.getContext().get("typeConge") != null) {
        TypeConge t = (TypeConge) request.getContext().get("typeConge");
        int nbr = t.getNbrJoursAnnee() - 1;
        d1 = (LocalDate) request.getContext().get("dateDebut");
        d2 = ServiceUtil.addDaysSkippingWeekends(d1, nbr);
        d2 = appservice.addDayWeekensDayOff(d1, d2);
        response.setValue("dateFin", d2);
        response.setValue("duree", t.getNbrJoursAnnee());
      }
    }
  }
  // hatim end methodes

  public void showCnssAos(ActionRequest request, ActionResponse response) {
    String choix = request.getContext().get("annexe").toString();
    if (choix.contains("Beni Mellal") || choix.contains("khenifra") || choix.contains("kheribga")) {
      response.setHidden("is_aos", true);
      response.setHidden("montant_aos", true);
      response.setHidden("is_cnss", false);
      response.setHidden("montant_cnss", false);
    } else {
      response.setHidden("is_cnss", true);
      response.setHidden("montant_cnss", true);
      response.setHidden("is_aos", false);
      response.setHidden("montant_aos", false);
    }
  }

  public void Imprimer_AttestationSalaire(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer id = (Integer) request.getContext().get("_id");
    Long id_emp = Long.valueOf(id);
    if (request.getContext().get("dateDebut") == null) {
      response.setError("Le champ <b>Date Début</b> est obligatoire");
      return;
    }
    if (request.getContext().get("dateFin") == null) {
      response.setError("Le champ <b>Date Fin</b> est obligatoire");
      return;
    }
    if (request.getContext().get("type_doc") == null) {
      response.setError("Le <b>Type de Document</b> est obligatoire");
      return;
    }

    String date_debut = request.getContext().get("dateDebut").toString();
    String date_fin = request.getContext().get("dateFin").toString();

    LocalDate dateDebut = LocalDate.parse(date_debut);
    LocalDate dateFin = LocalDate.parse(date_fin);

    EtatSalaire rs =
        Beans.get(EtatSalaireRepository.class)
            .all()
            .filter("self.mois=:mois and self.annee=:annee")
            .bind("mois", dateFin.getMonthValue())
            .bind("annee", dateFin.getYear())
            .fetchOne();
    if (rs == null) {
      response.setFlash("Aucune état de salaire dans la date choisi");
      return;
    }
    String type_doc = request.getContext().get("type_doc").toString();

    String fileLink =
        ReportFactory.createReport(IReport.AttestationSalaire, "Attestation de salaire")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("dateDebut", java.sql.Date.valueOf(dateDebut))
            .addParam("dateFin", java.sql.Date.valueOf(dateFin))
            .addParam("id_employee", id)
            .addFormat(type_doc)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Attestation de salaire").add("html", fileLink).map());
  }
  
  public void imprimerAttestationDeTravail(ActionRequest request, ActionResponse response)
          throws AxelorException {
    Long id = (Long) request.getContext().get("id");
    String fileLink =
            ReportFactory.createReport(IReport.AttestationTravail, "Attestation de travail")
                    .addParam("Locale", ReportSettings.getPrintingLocale(null))
                    .addParam("id_employee", id)
                    .generate()
                    .getFileLink();
    response.setView(ActionView.define("Attestation de salaire").add("html", fileLink).map());
  }
}
