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
package com.axelor.apps.hr.service.app;

import com.axelor.apps.base.db.AppExpense;
import com.axelor.apps.base.db.AppLeave;
import com.axelor.apps.base.db.AppTimesheet;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.repo.AppExpenseRepository;
import com.axelor.apps.base.db.repo.AppLeaveRepository;
import com.axelor.apps.base.db.repo.AppTimesheetRepository;
import com.axelor.apps.base.db.repo.CompanyRepository;
import com.axelor.apps.base.service.app.AppBaseServiceImpl;
import com.axelor.apps.configuration.db.HistoriqueRIB;
import com.axelor.apps.configuration.db.IntitulerCredit;
import com.axelor.apps.configuration.db.repo.HistoriqueRIBRepository;
import com.axelor.apps.configuration.db.repo.IntitulerCreditRepository;
import com.axelor.apps.configuration.db.repo.ResponsabiliteRepository;
import com.axelor.apps.configuration.service.ConvertNomreToLettres;
import com.axelor.apps.hr.db.Conge;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.GestionCredit;
import com.axelor.apps.hr.db.HRConfig;
import com.axelor.apps.hr.db.HistoriqueResponsabilite;
import com.axelor.apps.hr.db.RibHistorique;
import com.axelor.apps.hr.db.repo.*;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class AppHumanResourceServiceImpl extends AppBaseServiceImpl
    implements AppHumanResourceService {

  private AppTimesheetRepository appTimesheetRepo;
  private AppLeaveRepository appLeaveRepo;
  private AppExpenseRepository appExpenseRepo;

  @Inject private CompanyRepository companyRepo;
  @Inject private EmployeeEtatSommeRepository employeeEtatSommeRepository;
  @Inject private EmployeeOrdreRepository employeeOrdreRepository;
  @Inject private HRConfigRepository hrConfigRepo;
  @Inject private HistoriqueRIBRepository historiqueRIBRepository;
  @Inject private RibHistoriqueRepository ribHistoriqueRepository;
  @Inject private HistoriqueResponsabiliteRepository historiqueResponsabiliteRepository;
  @Inject private ResponsabiliteRepository responsabiliteRepository;
  @Inject private GestionCreditRepository gestionCreditRepository;
  @Inject private EmployeeRepository employeeRepository;
  @Inject private CongeRepository congeRepository;
  @Inject private IntitulerCreditRepository intitulerCreditRepository;

  @Inject
  public AppHumanResourceServiceImpl(
      AppTimesheetRepository appTimesheetRepo,
      AppLeaveRepository appLeaveRepo,
      AppExpenseRepository appExpense) {
    this.appTimesheetRepo = appTimesheetRepo;
    this.appLeaveRepo = appLeaveRepo;
    this.appExpenseRepo = appExpense;
  }

  @Override
  public AppTimesheet getAppTimesheet() {
    return appTimesheetRepo.all().fetchOne();
  }

  @Override
  public AppLeave getAppLeave() {
    return appLeaveRepo.all().fetchOne();
  }

  @Override
  public AppExpense getAppExpense() {
    return appExpenseRepo.all().fetchOne();
  }

  @Override
  public void getHrmAppSettings(ActionRequest request, ActionResponse response) {

    try {

      Map<String, Object> map = new HashMap<>();

      map.put("hasInvoicingAppEnable", isApp("invoice"));
      map.put("hasLeaveAppEnable", isApp("leave"));
      map.put("hasExpenseAppEnable", isApp("expense"));
      map.put("hasTimesheetAppEnable", isApp("timesheet"));
      map.put("hasProjectAppEnable", isApp("project"));

      response.setData(map);
      response.setTotal(map.size());

    } catch (Exception e) {
      e.printStackTrace();
      response.setException(e);
    }
  }

  @Override
  @Transactional
  public void generateHrConfigurations() {

    List<Company> companies = companyRepo.all().filter("self.hrConfig is null").fetch();

    for (Company company : companies) {
      HRConfig hrConfig = new HRConfig();
      hrConfig.setCompany(company);
      hrConfigRepo.save(hrConfig);
    }
  }

  @Transactional
  public void save_RIB(String rib) {
    HistoriqueRIB historiqueRIB = new HistoriqueRIB();
    historiqueRIB.setHistoriqueRib(rib);
    Beans.get(HistoriqueRIBRepository.class).save(historiqueRIB);
  }

  /*  @Transactional
  public void saveHistRespon(Employee employee, Long id_respo, Long id_employee) {
    List<HistoriqueResponsabilite> RespoHistorique = load_respoHistorique_by_Employee(id_employee);
    int sizeList = RespoHistorique.size();

    Long respoAncien = employee.getResponsabilite().getId();
    Responsabilite resp_employee = responsabiliteRepository.find(id_respo);
    Boolean compare = id_respo.equals(respoAncien);

    if (id_employee != null && !compare && sizeList == 0) {

      HistoriqueResponsabilite respoHistorique1 = new HistoriqueResponsabilite();
      respoHistorique1.setResponsabilite(employee.getResponsabilite());
      respoHistorique1.setDateDebut(employee.getCreatedOn().toLocalDate());
      respoHistorique1.setDateFin(LocalDateTime.now().toLocalDate());
      respoHistorique1.setEtat("Ancienne");
      historiqueResponsabiliteRepository.save(respoHistorique1);

      HistoriqueResponsabilite respoHistorique2 = new HistoriqueResponsabilite();
      respoHistorique2.setResponsabilite(resp_employee);
      respoHistorique2.setEmployee(employee);
      respoHistorique2.setDateDebut(LocalDateTime.now().toLocalDate());
      respoHistorique2.setEtat("En cours");
      historiqueResponsabiliteRepository.save(respoHistorique2);
    }
    if (id_employee != null && !compare && sizeList != 0) {
      HistoriqueResponsabilite respoHistorique1 = historiqueResponsabiliteRepository
              .all()
              .filter("self.employee=?1 and self.dateFin=null",id_employee)
              .fetchOne();
             */
  /* loadOne_respoHistorique_by_Employee(id_employee);*/
  /*
      respoHistorique1.setDateFin(LocalDate.now());
      respoHistorique1.setEtat("Ancienne");
      historiqueResponsabiliteRepository.save(respoHistorique1);

      HistoriqueResponsabilite respoHistorique2 = new HistoriqueResponsabilite();
      respoHistorique2.setResponsabilite(resp_employee);
      respoHistorique2.setEmployee(employee);
      respoHistorique2.setDateDebut(LocalDateTime.now().toLocalDate());
      respoHistorique2.setEtat("En cours");
      historiqueResponsabiliteRepository.save(respoHistorique2);
    }
  }*/

  @Transactional
  public void save_RibHistorique(String rib, Employee employee, Long id_employee, String banque) {
    List<RibHistorique> listRibsHistoriques = load_ribHistorique_by_Employee(id_employee);
    int sizeList = listRibsHistoriques.size();

    String ribAncien = employee.getRib();
    Boolean compare = rib.equals(ribAncien);

    if (id_employee != null && !compare && sizeList == 0) {

      RibHistorique ribHistorique1 = new RibHistorique();
      ribHistorique1.setHistoriqueRib(employee.getRib());
      ribHistorique1.setEmployee(employee);
      ribHistorique1.setDateDebut(employee.getCreatedOn());
      ribHistorique1.setDateFin(LocalDateTime.now());
      ribHistorique1.setBanque(employee.getBanque());
      ribHistoriqueRepository.save(ribHistorique1);

      RibHistorique ribHistorique2 = new RibHistorique();
      ribHistorique2.setHistoriqueRib(rib);
      ribHistorique2.setEmployee(employee);
      ribHistorique2.setBanque(banque);
      ribHistorique2.setDateDebut(LocalDateTime.now());

      ribHistoriqueRepository.save(ribHistorique2);
    }

    if (id_employee != null && !compare && sizeList != 0) {
      RibHistorique ribHistorique1 = loadOne_ribHistorique_by_Employee(id_employee);
      ribHistorique1.setDateFin(LocalDateTime.now());
      ribHistoriqueRepository.save(ribHistorique1);

      RibHistorique ribHistorique2 = new RibHistorique();
      ribHistorique2.setHistoriqueRib(rib);
      ribHistorique2.setEmployee(employee);
      ribHistorique2.setBanque(banque);
      ribHistorique2.setDateDebut(LocalDateTime.now());

      ribHistoriqueRepository.save(ribHistorique2);
    }

    //    if (id_employee != null) {
    //
    //      String ribAncien = employee.getRib();
    //      Boolean compare = rib.equals(ribAncien);
    //      System.out.print("it is work 2");
    //      if (!compare) {
    //        System.out.print("it is work 3");
    //        RibHistorique ribHistorique = new RibHistorique();
    //        ribHistorique.setHistoriqueRib(ribAncien);
    //        System.out.print("it is work 4");
    //        ribHistorique.setEmployee(employee);
    //        System.out.print("it is work 5");
    //        ribHistoriqueRepository.save(ribHistorique);
    //      }
    //    }

  }

  private List<RibHistorique> load_ribHistorique_by_Employee(Long id) {
    return Beans.get(RibHistoriqueRepository.class).all().filter("self.employee=?1", id).fetch();
  }

  private List<HistoriqueResponsabilite> load_respoHistorique_by_Employee(Long id) {
    return Beans.get(HistoriqueResponsabiliteRepository.class)
        .all()
        .filter("self.employee=?1", id)
        .fetch();
  }

  private RibHistorique loadOne_ribHistorique_by_Employee(Long id) {
    return Beans.get(RibHistoriqueRepository.class)
        .all()
        .filter("self.employee=?1 and self.dateFin=null", id)
        .fetchOne();
  }

  private HistoriqueResponsabilite loadOne_respoHistorique_by_Employee(Long id) {
    return Beans.get(HistoriqueResponsabiliteRepository.class)
        .all()
        .filter("self.employee=?1 and self.dateFin=null", id)
        .fetchOne();
  }
  /*
   * @Transactional public EmployeeOrdre creatOrdrePayement(ActionRequest request,
   * ActionResponse response) { Long id_somme =
   * Long.valueOf(request.getContext().get("id").toString());
   *
   * EmployeeOrdre x = new EmployeeOrdre(); x.setRubrique(rubrique);
   * x.setMontant(montant); x.set return employeeOrdreRepository.save(x); }
   */
  /*
   * public String getsommeTotaleString(Long id_ov) { String somme_string = "";
   * BigDecimal somme = new BigDecimal(0); OrdrevirementCommande ov =
   * ordrevirementCommandeRepository.find(id_ov); List<SelectOP> ls =
   * ov.getSelectOP(); for (SelectOP tmp : ls) { BigDecimal x = new
   * BigDecimal(tmp.getSomme() == null ? "0" : tmp.getSomme()); somme =
   * somme.add(x); } somme_string =
   * ConvertNomreToLettres.convert(somme.longValue()).toUpperCase() + " Dirhams";
   * if (somme.compareTo(new BigDecimal(String.valueOf(somme.longValue()))) > 0) {
   * somme_string = somme_string + " ET "; String decimal =
   * somme.remainder(BigDecimal.ONE).movePointRight(somme.scale()).abs().toString(
   * ); somme_string += (ConvertNomreToLettres.convert(Long.valueOf(decimal)) +
   * " CENTIMES").toUpperCase(); } return somme_string; }
   */
  @Transactional
  public void AddEmployeToCredit(ActionRequest request, ActionResponse response) {
    Long id_employee = Long.valueOf(request.getContext().get("id_Employee").toString());
    IntitulerCredit map = (IntitulerCredit) request.getContext().get("intituler");
    Long id_intituler = map.getId();
    String dossier = (String) request.getContext().get("numDossier");
    String client = (String) request.getContext().get("numClient");
    BigDecimal montant =
        BigDecimal.valueOf(Double.parseDouble(request.getContext().get("montant").toString()));
    BigDecimal remboursement =
        BigDecimal.valueOf(
            Double.parseDouble(request.getContext().get("remboursement").toString()));
    String montantLettre = ConvertNomreToLettres.getStringMontant(remboursement);
    LocalDate dateDebut = LocalDate.parse(request.getContext().get("dateDebut").toString());
    LocalDate dateFin = LocalDate.parse(request.getContext().get("dateFin").toString());
    Employee employee = employeeRepository.find(id_employee);
    List<GestionCredit> CreditList = findLastCreditOfEmployee(id_employee);
    //  gestionCreditRepository.findLastCreditOfEmployee(id_employee);
    // new ArrayList<GestionCredit>();

    //    Boolean isInInterval = false;
    //    for (GestionCredit ls : CreditList) {
    //
    //      if ((dateDebut.isAfter(ls.getDateDebut()) && dateDebut.isBefore(ls.getDateFin()))
    //          || (dateFin.isAfter(ls.getDateDebut()) && dateFin.isBefore(ls.getDateFin()))) {
    //        isInInterval = true;
    //        break;
    //      }
    //    }
    //    if (isInInterval) {
    //      response.setFlash("Vous avez déja un crédit dans cette période");
    //    } else {
    GestionCredit credit = new GestionCredit();
    credit.setIntituler(intitulerCreditRepository.find(id_intituler));
    credit.setNumDossier(dossier);
    credit.setNumClient(client);
    credit.setMontant(montant);
    credit.setRemboursement(remboursement);
    credit.setMontantLettre(montantLettre);
    credit.setDateDebut(dateDebut);
    credit.setDateFin(dateFin);
    credit.setEmployee(employee);
    credit.setId_Employee(id_employee.toString());

    gestionCreditRepository.save(credit);

    String id_empl = request.getContext().get("id_Employee").toString();
    response.setView(
        ActionView.define("Liste des crédits")
            .model(GestionCredit.class.getName())
            .add("grid", "GestionCredit-grid")
            .add("form", "GestionCredit-form")
            .domain("self.employee.id = " + id_empl)
            .context("id_Employee", id_empl)
            .map());
    response.setReload(true);
    response.setCanClose(true);
    // }
  }

  @Transactional
  public void UpdateEmployeToCredit(ActionRequest request, ActionResponse response) {
    Long id_Credit = Long.valueOf(request.getContext().get("id").toString());
    Long id_employeeLong = Long.valueOf(request.getContext().get("id_Employee").toString());
    IntitulerCredit map = (IntitulerCredit) request.getContext().get("intituler");
    Long id_intituler = map.getId();
    String dossier = (String) request.getContext().get("numDossier");
    String client = (String) request.getContext().get("numClient");
    BigDecimal montant =
        BigDecimal.valueOf(Double.parseDouble(request.getContext().get("montant").toString()));
    BigDecimal remboursement =
        BigDecimal.valueOf(
            Double.parseDouble(request.getContext().get("remboursement").toString()));
    String montantLettre = ConvertNomreToLettres.getStringMontant(remboursement);
    LocalDate dateDebut = LocalDate.parse(request.getContext().get("dateDebut").toString());
    LocalDate dateFin = LocalDate.parse(request.getContext().get("dateFin").toString());

    List<GestionCredit> creditList = findLastCreditOfEmployee(id_employeeLong);

    creditList.remove(gestionCreditRepository.find(id_Credit));

    GestionCredit credit = gestionCreditRepository.find(id_Credit);
    String id_employee = credit.getEmployee().getId().toString();
    credit.setIntituler(intitulerCreditRepository.find(id_intituler));
    credit.setNumDossier(dossier);
    credit.setNumClient(client);
    credit.setMontant(montant);
    credit.setRemboursement(remboursement);
    credit.setMontantLettre(montantLettre);
    credit.setDateDebut(dateDebut);
    credit.setDateFin(dateFin);
    // credit.setEmployee(employee);

    gestionCreditRepository.save(credit);

    response.setView(
        ActionView.define("Liste des crédits")
            .model(GestionCredit.class.getName())
            .add("grid", "GestionCredit-grid")
            .add("form", "GestionCredit-form")
            .domain("self.employee.id = " + id_employee)
            .context("id_Employee", id_employee)
            .map());
    response.setReload(true);
    response.setCanClose(true);
  }

  private List<GestionCredit> findLastCreditOfEmployee(Long id) {
    return Beans.get(GestionCreditRepository.class)
        .all()
        .filter("self.employee=?1 order by self.id desc", id)
        .fetch();
  }

  private List<Conge> findAllCongeOfEmployee(Long id) {
    return Beans.get(CongeRepository.class)
        .all()
        .filter("self.employee=?1 order by self.id desc", id)
        .fetch();
  }
}
