package com.axelor.apps.hr.service;

import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.repo.*;
import com.axelor.apps.configuration.service.RunSqlRequestForMe;
import com.axelor.apps.configuration.service.ServiceUtil;
import com.axelor.apps.hr.db.*;
import com.axelor.apps.hr.db.repo.*;
import com.axelor.db.JPA;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class EmployeeAdvanceService {
  
  boolean etat = false;
  @Inject
  private EmployeeAdvanceRepository employeeAdvanceRepository;
  @Inject
  private CongeRepository congeRepository;
  @Inject
  private EmployeeAdvanceUsageRepository employeeAdvanceUsageRepository;
  @Inject
  private EmployeeRepository employeeRepository;
  @Inject
  private AugmentationRepository augmentationRepository;
  @Inject
  private GradeRepository gradeRepository;
  @Inject
  private EchelleRepository echelleRepository;
  @Inject
  private EchelonRepository echelonRepository;
  @Inject
  private EmployeeEtatSommeRepository employeeEtatSommeRepository;
  @Inject
  private RCARRepository rcarRepository;
  @Inject
  private RCARCRepository rcarcRepository;
  @Inject
  private MUTUELLERepository mutuelleRepository;
  @Inject
  private GestionSalaireRepository gestionSalaireRepository;
  @Inject
  private HistoriqueResponsabiliteRepository historiqueResponsabiliteRepository;
  @Inject
  private GestionCreditRepository gestionCreditRepository;
  @Inject
  private AosRepository aosRepository;
  @Inject
  private EtatDesRappelSurSalaireRepository etatDesRappelSurSalaireRepository;
  @Inject
  private EtatSalaireRepository etatSalaireRepository;
  @Inject
  private OrdreVirementRepository ordreVirementRepository;
  @Inject
  private RubriqueBudgetaireGeneraleRepository rubriqueBudgetaireGeneraleRepository;
  @Inject
  private JoursFeriesRepository joursFeriesRepository;
  @Inject
  private ResponsabiliteRepository responsabiliteRepository;
  @Inject
  private IRRepository irRepository;
  @Inject
  private ServiceUtil serviceUtil;
  @Inject
  private ResponsabiliteListRepository responsabiliteListRepository;
  @Inject
  private CompteRepository compteRepository;
  @Inject
  private RappelEmployeRepository rappelEmployeRepository;
  @Inject
  private PrimeRepository primeRepository;
  
  @Transactional
  public void fillExpenseWithAdvances(Expense expense) {
    Employee employee =
            Beans.get(EmployeeRepository.class).find(expense.getUser().getEmployee().getId());
    List<EmployeeAdvance> advanceList =
            employeeAdvanceRepository
                    .all()
                    .filter(
                            "self.employee.id = ?1 AND self.remainingAmount > 0 AND self.date < ?2 AND self.statusSelect = ?3 AND self.typeSelect = ?4",
                            employee.getId(),
                            expense.getPeriod().getToDate(),
                            EmployeeAdvanceRepository.STATUS_VALIDATED,
                            EmployeeAdvanceRepository.TYPE_OCCASIONAL)
                    .fetch();
    if (advanceList != null && !advanceList.isEmpty()) {
      BigDecimal currentAmountToRefund =
              expense
                      .getInTaxTotal()
                      .subtract(expense.getPersonalExpenseAmount())
                      .subtract(expense.getWithdrawnCash());
      for (EmployeeAdvance advance : advanceList) {
        if (currentAmountToRefund.signum() == 0) {
          break;
        }
        currentAmountToRefund = withdrawFromAdvance(advance, expense, currentAmountToRefund);
        employeeAdvanceRepository.save(advance);
      }
      expense.setAdvanceAmount(
              expense
                      .getInTaxTotal()
                      .subtract(currentAmountToRefund)
                      .subtract(expense.getPersonalExpenseAmount())
                      .subtract(expense.getWithdrawnCash()));
    }
  }
  
  public BigDecimal withdrawFromAdvance(
          EmployeeAdvance employeeAdvance, Expense expense, BigDecimal maxAmount) {
    if (maxAmount.compareTo(employeeAdvance.getRemainingAmount()) > 0) {
      maxAmount = maxAmount.subtract(employeeAdvance.getRemainingAmount());
      createEmployeeAdvanceUsage(employeeAdvance, expense, employeeAdvance.getRemainingAmount());
      employeeAdvance.setRemainingAmount(BigDecimal.ZERO);
      
    } else {
      createEmployeeAdvanceUsage(employeeAdvance, expense, maxAmount);
      employeeAdvance.setRemainingAmount(employeeAdvance.getRemainingAmount().subtract(maxAmount));
      maxAmount = BigDecimal.ZERO;
    }
    return maxAmount;
  }
  
  @Transactional
  public void createEmployeeAdvanceUsage(
          EmployeeAdvance employeeAdvance, Expense expense, BigDecimal amount) {
    EmployeeAdvanceUsage usage = new EmployeeAdvanceUsage();
    usage.setEmployeeAdvance(employeeAdvance);
    usage.setExpense(expense);
    usage.setUsedAmount(amount);
    employeeAdvanceUsageRepository.save(usage);
  }
  
  @Transactional
  public Augmentation createAugmentation(Augmentation aug) {
    Augmentation aug_tmp = new Augmentation();
    Employee emp = aug.getEmployer();
    emp.setHasEvolution(true);
    employeeRepository.save(emp);
    aug_tmp.setEtat(aug.getEtat());
    aug_tmp.setDateDebut(aug.getDateDebut());
    aug_tmp.setGrade(aug.getGrade());
    aug_tmp.setEchelle(aug.getEchelle());
    aug_tmp.setEchelon(aug.getEchelon());
    aug_tmp.setHasRappel(true);
    aug_tmp.setEmployer(emp);
    Augmentation aug_old =
            Beans.get(AugmentationRepository.class)
                    .all()
                    .filter("self.employer=:emp and self.etat='En cours'")
                    .bind("emp", emp)
                    .fetchOne();
    if (aug_old != null) {
      aug_tmp.setGrade_old(aug_old.getGrade());
      aug_tmp.setEchelle_old(aug_old.getEchelle());
      aug_tmp.setEchelon_old(aug_old.getEchelon());
    }
    if (aug.getEchelle() != null && aug.getEchelon() != null && aug.getEmployer() != null) {
      Echelle echelle = echelleRepository.find(aug.getEchelle().getId());
      Echelon echelon = echelonRepository.find(aug.getEchelon().getId());
      Employee employee = employeeRepository.find(aug.getEmployer().getId());
      if (employee.getEchelle().getId() == echelle.getId()
              && employee.getEchelon().getId() != echelon.getId()) {
        aug_tmp.setAvtRecalss("Avt");
      } else if (employee.getEchelle().getId() != echelle.getId()
              && employee.getEchelon().getId() == echelon.getId()) {
        aug_tmp.setAvtRecalss("Reclassement");
      }
    }
    return augmentationRepository.save(aug_tmp);
  }
  
  @Transactional
  public Augmentation Add_default_augmentation(long emp_id) {
    Augmentation au =
            augmentationRepository.findByEmployee(employeeRepository.find(emp_id), "En attente").get(0);
    au.setEtat("En cours");
    au = augmentationRepository.save(au);
    return au;
  }
  
  @Transactional
  public Employee update_personnel(Employee emp) {
    return employeeRepository.save(emp);
  }
  
  public int getNombreAugmentation(Long employeeId) {
    Employee emp = Beans.get(EmployeeRepository.class).find(employeeId);
    return Beans.get(augmentationRepository.getClass()).findByEmployee(emp, "En cours").size();
  }
  
  public Augmentation getEchelleAtDate(String d1, String d2, Employee emp, String Debut_Fin) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDate date1 = LocalDate.parse(d1, formatter);
    LocalDate date2 = LocalDate.parse(d2, formatter);
    LocalDate returnvalue = date2.minusMonths(1);
    
    Augmentation au = null;
    if (Debut_Fin.equals("Debut")) {
      au =
              Beans.get(AugmentationRepository.class)
                      .all()
                      .filter(
                              "?1 between self.dateDebut and (case when self.dateFin is null then now() else self.dateFin end) and self.employer=?3 Order By self.dateFin ASC ",
                              date1,
                              date2,
                              emp)
                      .fetchOne();
    } else if (Debut_Fin.equals("Fin"))
      au =
              Beans.get(AugmentationRepository.class)
                      .all()
                      .filter(
                              "self.dateDebut <=?1 and ((self.dateFin >=?2 AND self.etat!='En cours') or (self.dateFin is null AND self.etat='En cours')) AND self.employer = ?3 Order By self.dateFin DESC ",
                              date1,
                              date2,
                              emp)
                      .fetchOne();
    return au;
  }
  
  public Augmentation getEchelleAtDate(
          LocalDate date1, LocalDate date2, Employee emp, String Debut_Fin) {
    Augmentation au = null;
    if (Debut_Fin.equals("Debut")) {
      au =
              Beans.get(AugmentationRepository.class)
                      .all()
                      .filter(
                              "self.dateDebut <=?1 and ((self.dateFin >=?2 AND self.etat!='En cours') or (self.dateFin is null AND self.etat='En cours')) AND self.employer = ?3 Order By self.dateFin ASC ",
                              date1,
                              date2,
                              emp)
                      .fetchOne();
    } else if (Debut_Fin.equals("Fin"))
      au =
              Beans.get(AugmentationRepository.class)
                      .all()
                      .filter(
                              "self.dateDebut <=?1 and ((self.dateFin >=?2 AND self.etat!='En cours') or (self.dateFin is null AND self.etat='En cours')) AND self.employer = ?3 Order By self.dateFin DESC ",
                              date1,
                              date2,
                              emp)
                      .fetchOne();
    return au;
  }
  
  public int getNombreNaissance(String d2, Employee emp, String Debut_Fin) {
    return 0;
  }
  
  public int getNombreEnfantAtDate(Employee emp, String date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDate date1 = LocalDate.parse(date, formatter);
    int res = 0;
    res =
            Beans.get(NaissancesRepository.class)
                    .all()
                    .filter("self.employee= ?1 and self.dateNaiss <= ?2", emp, date1)
                    .fetch()
                    .size();
    return res;
  }
  
  public String getLastDayByDate(String date) throws ParseException {
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    String res = date;
    Calendar c = Calendar.getInstance();
    Date convertedDate = dateFormat.parse(date);
    c.setTime(convertedDate);
    int x = c.get(Calendar.MONTH) + 1;
    res =
            c.getActualMaximum(Calendar.DAY_OF_MONTH)
                    + "/"
                    + (x < 10 ? ("0" + x) : x)
                    + "/"
                    + c.get(Calendar.YEAR);
    return res;
  }
  
  public boolean verifierdate(String d2, LocalDate createdOn) throws ParseException {
    boolean res = true; // true existe fault
    LocalDate date = LocalDate.parse(d2);
    int x = date.compareTo(createdOn); // future.compareTo(passe) ==> > 0
    res = x < 0;
    return res;
  }
  
  @Transactional
  public void updateEtatSomme(Long id_somme, BigDecimal montant) {
    EmployeeEtatSomme x = employeeEtatSommeRepository.find(id_somme);
    x.setMontant(montant);
    employeeEtatSommeRepository.save(x);
  }
  
  public String getSitualtionAtDate(String date1, LocalDate marriageDate) {
    String res = " - ";
    if (marriageDate == null || date1 == null) {
      res = "Célibataire";
      return res;
    } else {
      LocalDate date = LocalDate.parse(date1);
      int x = marriageDate.compareTo(date);
      if (x < 0) res = "Marié(e)";
      else res = "Célibataire";
    }
    return res;
  }
  
  public String getSitualtionAtDate(LocalDate date, LocalDate marriageDate) {
    String res = " - ";
    int x = marriageDate.compareTo(date);
    if (x < 0) res = "Marié(e)";
    else res = "Célibataire";
    return res;
  }
  
  public int getNombreEnfantAtDate_21(Employee employee, String date2) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDate date1 = LocalDate.parse(date2, formatter);
    int res = 0;
    res =
            Beans.get(NaissancesRepository.class)
                    .all()
                    .filter(
                            "self.employee= ?1 and self.dateNaiss <= ?2 and ( (DATE_PART('year', AGE(?3, self.dateNaiss))<21) or (self.handicape=true) )",
                            employee,
                            date1,
                            date1)
                    .fetch()
                    .size();
    return res;
  }
  
  public int getNombreEnfantAtDate_21(Employee employee, LocalDate date) {
    int res = 0;
    res =
            Beans.get(NaissancesRepository.class)
                    .all()
                    .filter(
                            "self.employee= ?1 and self.dateNaiss <= ?2 and ( (DATE_PART('year', AGE(?3, self.dateNaiss))<21) or (self.handicape=true) )",
                            employee,
                            date,
                            date)
                    .fetch()
                    .size();
    return res;
  }
  
  public int getNombreEnfantAtDate_27(Employee employee, LocalDate date) {
    int res = 0;
    res =
            Beans.get(NaissancesRepository.class)
                    .all()
                    .filter(
                            "self.employee= ?1 and self.dateNaiss <= ?2 and ( (DATE_PART('year', AGE(?3, self.dateNaiss))<27) or (self.handicape=true) )",
                            employee,
                            date,
                            date)
                    .fetch()
                    .size();
    return res;
  }
  
  public RCARC getComplementRcarByYear(int year) {
    return rcarcRepository.all().filter("self.annee=?1", year).fetchOne();
  }
  
  public MUTUELLE getMutuelleById(long l) {
    return mutuelleRepository.all().filter("self.mutuelle=?1", l).fetchOne();
  }
  
  public String imprimer_EtatEngagement(
          LocalDate dateDebut, LocalDate dateFin, Long id_emp, String rappel) {
    List<String> data = new ArrayList<>();
    Employee employee = employeeRepository.find(id_emp);
    data.add(employee.getContactPartner().getSimpleFullName());
    data.add(employee.getGrade().getName());
    data.add(employee.getResponsabilite() != null ? employee.getResponsabilite().getName() : "");
    boolean dateOrigine = false;
    int nbr_aug = getNombreAugmentation(employee.getId());
    if (nbr_aug == 0) {
      data.add(employee.getEchelle().getName()); // old echelle
      data.add(employee.getEchelle().getName()); // new echelle
      data.add(employee.getEchelon().getName()); // old echelon
      data.add(employee.getEchelon().getName()); // new echelon
      data.add(employee.getEchelon().getIndice()); // old indice
      data.add(employee.getEchelon().getIndice()); // new indice
    } else {
      dateDebut = dateDebut.minusDays(1);
      if (rappel.equals("rappel")) {
        if (isFirstRappel(id_emp)) {
          dateDebut = dateDebut.plusDays(1);
          dateOrigine = true;
        }
      } else if (!employerHasWordAtDate(id_emp, dateDebut)) {
        dateDebut = dateDebut.plusDays(1);
        dateOrigine = true;
      }
      Augmentation a1 = getEchelleAtDate(dateDebut, dateDebut, employee, "Debut");
      if (a1 == null) {
        a1 = getEchelleAtDate(dateDebut.plusDays(1), dateDebut.plusDays(1), employee, "Debut");
      }
      Augmentation a2 = getEchelleAtDate(dateFin, dateFin, employee, "Fin");
      String old_echelle, old_echelon, old_indice;
      String new_echelle, new_echelon, new_indice;
      if (a1.getDateRappel() == null) {
        old_echelle = a1.getEchelle_old().getName(); // old echelle
        old_echelon = a1.getEchelon_old().getName(); // old echelon
        old_indice = a1.getEchelon_old().getIndice(); // old indice
      } else {
        old_echelle = a1.getEchelle().getName(); // old echelle
        old_echelon = a1.getEchelon().getName(); // old echelon
        old_indice = a1.getEchelon().getIndice(); // old indice
      }
      if (a2.getDateRappel() == null) {
        new_echelle = a2.getEchelle_old().getName(); // old echelle
        new_echelon = a2.getEchelon_old().getName(); // old echelon
        new_indice = a2.getEchelon_old().getIndice(); // old indice
      } else {
        new_echelle = a2.getEchelle().getName(); // old echelle
        new_echelon = a2.getEchelon().getName(); // old echelon
        new_indice = a2.getEchelon().getIndice(); // old indice
      }
      data.add(old_echelle); // old echelle
      data.add(new_echelle); // new echelle
      data.add(old_echelon); // old echelon
      data.add(new_echelon); // new echelon
      data.add(old_indice); // old indice
      data.add(new_indice); // new indice
    }
    // mariage
    if (employee.getMaritalStatus() != null
            && !employee.getMaritalStatus().equals("")
            && employee.getMarriageDate() != null) {
      String situation1 = getSitualtionAtDate(dateDebut, employee.getMarriageDate());
      String situation2 = getSitualtionAtDate(dateFin, employee.getMarriageDate());
      if (employee.getContactPartner() != null
              && employee.getContactPartner().getTitleSelect() != null) {
        situation1 =
                (employee.getContactPartner().getTitleSelect() == 2 && situation1.equals("Marié(e)"))
                        ? "Mariée"
                        : "Marié";
        situation2 =
                (employee.getContactPartner().getTitleSelect() == 2 && situation2.equals("Marié(e)"))
                        ? "Mariée"
                        : "Marié";
      }
      data.add(situation1);
      data.add(situation2);
    } else {
      data.add("Célibataire");
      data.add("Célibataire");
    }
    // enfant en charge
    data.add(String.valueOf(getNombreEnfantAtDate_21(employee, dateDebut))); // old
    data.add(String.valueOf(getNombreEnfantAtDate_21(employee, dateFin))); // new
    // date
    if (dateOrigine == false) {
      dateDebut = dateDebut.plusDays(1);
    }
    data.add(dateDebut.toString()); // old
    data.add(dateFin.toString()); // new
    // nbr de mois - nbr de jours
    long nbr = 0;
    long mois = 0, jour = 0;
    if ((dateDebut.getDayOfMonth() != 1 || dateFin.getDayOfMonth() != dateFin.lengthOfMonth())
            && dateDebut.getMonthValue() == dateFin.getMonthValue()) {
      // LE meme mois get difference by day
      nbr = dateDebut.until(dateFin, ChronoUnit.DAYS);
    } else if (dateDebut.getDayOfMonth() != 1
            && dateFin.getDayOfMonth() == dateFin.lengthOfMonth()
            && dateDebut.getMonthValue() != dateFin.getMonthValue()) {
      // mois et jour 2mois et 15 jours 12-9-2021 --> 30-11-2021
      LocalDate date_fin_mois =
              LocalDate.parse(
                      dateDebut.getYear()
                              + "-"
                              + mois_format(dateDebut.getMonthValue())
                              + "-"
                              + dateDebut.lengthOfMonth());
      jour = dateDebut.until(date_fin_mois, ChronoUnit.DAYS) + 1;
      LocalDate date_debut_next = date_fin_mois.plusDays(1l);
      mois = (ChronoUnit.MONTHS.between(date_debut_next, dateFin));
    } else if (dateDebut.getDayOfMonth() == 1
            && dateFin.getDayOfMonth() != dateFin.lengthOfMonth()
            && dateDebut.getMonthValue() != dateFin.getMonthValue()) {
      // mois et jour 2mois et 15 jours 01-9-2021 --> 15-11-2021
      LocalDate date_fin_mois =
              LocalDate.parse(dateDebut.getYear() + "-" + mois_format(dateFin.getMonthValue()) + "-01");
      date_fin_mois = date_fin_mois.minusDays(1l);
      mois = (ChronoUnit.MONTHS.between(dateDebut, date_fin_mois));
      jour = date_fin_mois.plusDays(1l).until(dateFin, ChronoUnit.DAYS) + 1;
    } else if (dateDebut.getDayOfMonth() == 1
            && dateFin.getDayOfMonth() == dateFin.lengthOfMonth()) {
      nbr = (ChronoUnit.MONTHS.between(dateDebut, dateFin));
    }
    if (nbr == 0) data.add(mois + " mois et " + jour + " jours");
    else data.add(String.valueOf(nbr));
    data.add(employee.getCin());
    data.add(employee.getImmatriculationSM());
    String x = StringUtils.join(data, "__");
    return x;
  }
  
  @Transactional
  public String getDataIndemnite(String dateDebut, String dateFin, Long id_emp, String rappel) {
    List<String> data = new ArrayList<>();
    Employee employee = employeeRepository.find(id_emp);
    int nbr_aug = getNombreAugmentation(employee.getId());
    Corps corps_old = null;
    Corps corps_new = null;
    Grade grade_old = null;
    Grade grade_new = null;
    Echelon echelon_old = null;
    Echelon echelon_new = null;
    LocalDate d1_moin1Day = LocalDate.parse(dateDebut);
    LocalDate d1_annee = LocalDate.parse(dateDebut);
    d1_moin1Day = d1_moin1Day.minusDays(1);
    if (nbr_aug == 0) {
      corps_old = corps_new = employee.getCorps();
      grade_old = grade_new = employee.getGrade();
      echelon_old = echelon_new = employee.getEchelon();
    } else {
      Augmentation a1 =
              getEchelleAtDate(d1_moin1Day.toString(), d1_moin1Day.toString(), employee, "Debut");
      if (a1 == null) a1 = getEchelleAtDate(dateDebut, dateDebut, employee, "Debut");
      Augmentation a2 = getEchelleAtDate(dateFin, dateFin, employee, "Fin");
      if (a1.getDateRappel() == null
              || a1.getDateRappel().compareTo(LocalDate.parse(dateDebut)) > 0) {
        corps_old = a1.getGrade_old().getCorps();
        grade_old = a1.getGrade_old();
        echelon_old = a1.getEchelon_old();
      } else {
        corps_old = a1.getGrade().getCorps();
        grade_old = a1.getGrade();
        echelon_old = a1.getEchelon();
      }
      if (a2.getDateRappel() == null
              || a2.getDateRappel().compareTo(LocalDate.parse(dateFin)) > 0) {
        corps_new = a2.getGrade_old().getCorps();
        grade_new = a2.getGrade_old();
        echelon_new = a2.getEchelon_old();
      } else {
        corps_new = a2.getGrade().getCorps();
        grade_new = a2.getGrade();
        echelon_new = a2.getEchelon();
      }
    }
    // traitement de base
    GestionSalaire gs_old =
            gestionSalaireRepository
                    .all()
                    .filter(
                            "self.corps=?1 and self.grade=?2 and self.echelon=?3 and self.indice=?4",
                            corps_old,
                            grade_old,
                            echelon_old.getName(),
                            echelon_old.getIndice())
                    .fetchOne();
    BigDecimal coef = BigDecimal.ZERO;
    ArrayList<BigDecimal> ls_retraite = new ArrayList<BigDecimal>();
    ArrayList<BigDecimal> ls_diff_rappel = new ArrayList<BigDecimal>();
    if ((LocalDate.parse(dateDebut).getDayOfMonth() != 1
            || LocalDate.parse(dateFin).getDayOfMonth() != LocalDate.parse(dateFin).lengthOfMonth())
            && LocalDate.parse(dateDebut).getMonthValue() == LocalDate.parse(dateFin).getMonthValue()) {
      // LE meme mois get difference by day
      coef =
              coef.add(
                      BigDecimal.valueOf(
                              LocalDate.parse(dateDebut).until(LocalDate.parse(dateFin), ChronoUnit.DAYS) + 1));
    }
    long mois = 0, jour = 0;
    LocalDate d1 = LocalDate.parse(dateDebut);
    LocalDate d2 = LocalDate.parse(dateFin);
    if (rappel.equals("rappel")) {
      if (d1.getDayOfMonth() != 1
              && d2.getDayOfMonth() == d2.lengthOfMonth()
              && d1.getMonthValue() != d2.getMonthValue()) {
        // mois et jour 2mois et 15 jours 12-9-2021 --> 30-11-2021
        LocalDate date_fin_mois =
                LocalDate.parse(
                        d1.getYear() + "-" + mois_format(d1.getMonthValue()) + "-" + d1.lengthOfMonth());
        jour = d1.until(date_fin_mois, ChronoUnit.DAYS) + 1;
        LocalDate date_debut_next = date_fin_mois.plusDays(1l);
        mois = (ChronoUnit.MONTHS.between(date_debut_next, d2));
      } else if (d1.getDayOfMonth() == 1
              && d2.getDayOfMonth() != d2.lengthOfMonth()
              && d1.getMonthValue() != d2.getMonthValue()) {
        // mois et jour 2mois et 15 jours 01-9-2021 --> 15-11-2021
        LocalDate date_fin_mois =
                LocalDate.parse(d1.getYear() + "-" + mois_format(d2.getMonthValue()) + "-01");
        date_fin_mois = date_fin_mois.minusDays(1l);
        mois = (ChronoUnit.MONTHS.between(d1, date_fin_mois));
        jour = date_fin_mois.plusDays(1l).until(d2, ChronoUnit.DAYS) + 1;
      } else if (d1.getDayOfMonth() != 1
              && d2.getDayOfMonth() == d2.lengthOfMonth()
              && d1.getMonthValue() == d2.getMonthValue()) {
        mois = 0;
        jour = d1.until(d2, ChronoUnit.DAYS) + 1;
      } else {
        mois = (ChronoUnit.MONTHS.between(d1, d2));
      }
    }
    GestionSalaire gs_new =
            gestionSalaireRepository
                    .all()
                    .filter(
                            "self.corps=?1 "
                                    + "and self.grade=?2 "
                                    + "and self.echelon=?3 "
                                    + "and self.indice=?4",
                            corps_new,
                            grade_new,
                            echelon_new.getName(),
                            echelon_new.getIndice())
                    .fetchOne();
    data.add("Traitement de base mensuel");
    data.add(gs_old.getTraitementDeBase().toString());
    data.add(gs_new.getTraitementDeBase().toString());
    data.add(gs_new.getTraitementDeBase().subtract(gs_old.getTraitementDeBase()).toString());
    ls_retraite.add(gs_new.getTraitementDeBase());
    ls_diff_rappel.add(gs_new.getTraitementDeBase().subtract(gs_old.getTraitementDeBase()));
    data.add("Indemnité de résidence complémentaire");
    BigDecimal pourc = new BigDecimal(1);
    if (employee.getZoneEmployee().getId() == 3) pourc = BigDecimal.valueOf(0.15);
    else if (employee.getZoneEmployee().getId() == 2) pourc = BigDecimal.valueOf(0.05);
    else if (employee.getZoneEmployee().getId() == 1 || employee.getZoneEmployee().getId() == 4)
      pourc = BigDecimal.valueOf(0.0);
    BigDecimal resid_old = gs_old.getTraitementDeBase().multiply(pourc);
    BigDecimal resid_new = gs_new.getTraitementDeBase().multiply(pourc);
    data.add(resid_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(resid_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(resid_new.subtract(resid_old).setScale(2, RoundingMode.HALF_UP).toString());
    ls_retraite.add(resid_new.setScale(2, RoundingMode.HALF_UP));
    ls_diff_rappel.add(resid_new.subtract(resid_old));
    String titre = "Allocation de hiérarchie administrative";
    if (employee.getCorps().getId() == 16)
      titre = "Indemnité Administrative Spéciale"; // administrateur
    else if (employee.getCorps().getId() == 19 || employee.getCorps().getId() == 14)
      titre = "Indemnité de technicité"; // Technicien - Ingenieur
    data.add(titre);
    data.add(gs_old.getIndemniteDeHierarchieAdministrative().toString());
    data.add(gs_new.getIndemniteDeHierarchieAdministrative().toString());
    data.add(
            gs_new
                    .getIndemniteDeHierarchieAdministrative()
                    .subtract(gs_old.getIndemniteDeHierarchieAdministrative())
                    .toString());
    ls_retraite.add(gs_new.getIndemniteDeHierarchieAdministrative());
    ls_diff_rappel.add(
            gs_new
                    .getIndemniteDeHierarchieAdministrative()
                    .subtract(gs_old.getIndemniteDeHierarchieAdministrative()));
    data.add("Indemnité de sujétion");
    data.add(gs_old.getIndemniteDeSujetion().toString());
    data.add(gs_new.getIndemniteDeSujetion().toString());
    data.add(gs_new.getIndemniteDeSujetion().subtract(gs_old.getIndemniteDeSujetion()).toString());
    ls_retraite.add(gs_new.getIndemniteDeSujetion());
    ls_diff_rappel.add(gs_new.getIndemniteDeSujetion().subtract(gs_old.getIndemniteDeSujetion()));
    data.add("Indemnité d'encadrement");
    data.add(gs_old.getIndemniteEncadrement().toString());
    data.add(gs_new.getIndemniteEncadrement().toString());
    data.add(
            gs_new.getIndemniteEncadrement().subtract(gs_new.getIndemniteEncadrement()).toString());
    ls_retraite.add(gs_new.getIndemniteEncadrement());
    ls_diff_rappel.add(gs_new.getIndemniteEncadrement().subtract(gs_new.getIndemniteEncadrement()));
    data.add("Allocations familiales");
    int child_old = getNombreEnfantAtDate_21(employee, d1_moin1Day);
    int child_new = getNombreEnfantAtDate_21(employee, dateFin);
    
    int child_mt_old, child_mt_new;
    child_mt_old = child_mt_new = 0;
    if (child_old < 6) {
      if (child_old < 3) {
        child_mt_old = child_old * 300;
      } else {
        if (d1_annee.getYear() > 2022) {
          child_mt_old = 900 + ((child_old - 3) * 100);
        } else {
          child_mt_old = 900 + ((child_old - 3) * 36);
        }
      }
    } else {
      child_mt_old = 1008;
    }
    if (child_new < 6) {
      if (child_new < 3) {
        child_mt_new = child_new * 300;
      } else {
        if (d1_annee.getYear() > 2022) {
          child_mt_new = 900 + ((child_new - 3) * 100);
        } else {
          child_mt_new = 900 + ((child_new - 3) * 36);
        }
      }
    } else {
      child_mt_new = 1008;
    }
    data.add(String.valueOf(child_mt_old));
    data.add(String.valueOf(child_mt_new));
    data.add(String.valueOf(child_mt_new - child_mt_old));
    ls_retraite.add(BigDecimal.valueOf(child_mt_new));
    ls_diff_rappel.add(BigDecimal.valueOf(child_mt_new).subtract(BigDecimal.valueOf(child_mt_old)));
    // int nbrHp = historiqueResponsabiliteRepository.all().filter("self.employee.id=?1",
    // employee.getId()).fetch().size();
    int nbrHp_2 =
            employee.getResponsabiliteList() != null ? employee.getResponsabiliteList().size() : 0;
    BigDecimal indem_log_old = new BigDecimal(0);
    BigDecimal indem_log_new = new BigDecimal(0);
    BigDecimal indem_foncNet_old = new BigDecimal(0);
    BigDecimal indem_foncNet_new = new BigDecimal(0);
    BigDecimal indem_voitur_old = new BigDecimal(0);
    BigDecimal indem_voitur_new = new BigDecimal(0);
    BigDecimal indem_repres_old = new BigDecimal(0);
    BigDecimal indem_repres_new = new BigDecimal(0);
    if (nbrHp_2 == 0) {
      setreponsabiliteNull(data, ls_retraite, ls_diff_rappel);
    } else {
      ResponsabiliteList oldResp = getResponsabiliteAtDate(employee, d1_moin1Day);
      if (oldResp == null) oldResp = getResponsabiliteAtDate(employee, LocalDate.parse(dateDebut));
      ResponsabiliteList newResp = getResponsabiliteAtDate(employee, LocalDate.parse(dateFin));
      if (oldResp != null
              && newResp != null
              && newResp.getResponsabilite_select().getId()
              != oldResp.getResponsabilite_select().getId()
              && newResp.getDate_debut().getDayOfMonth() != 1) {
        // calcule
        System.out.println("calcule");
        indem_log_old = oldResp.getResponsabilite_select().getIndemnitLogement();
        indem_log_new = calcule_indemnite(1, oldResp, newResp, LocalDate.parse(dateFin));
        
        indem_foncNet_old = oldResp.getResponsabilite_select().getIndemnitFonction();
        indem_foncNet_new = calcule_indemnite(2, oldResp, newResp, LocalDate.parse(dateFin));
        
        indem_voitur_old = oldResp.getResponsabilite_select().getIndemnitVoiture();
        indem_voitur_new = calcule_indemnite(3, oldResp, newResp, LocalDate.parse(dateFin));
        
        indem_repres_old = oldResp.getResponsabilite_select().getIndemnitRepresentation();
        indem_repres_new = calcule_indemnite(4, oldResp, newResp, LocalDate.parse(dateFin));
        
      } else if (oldResp != null || newResp != null) {
        if (oldResp != null) {
          indem_log_old = oldResp.getResponsabilite_select().getIndemnitLogement();
          indem_foncNet_old = oldResp.getResponsabilite_select().getIndemnitFonction();
          indem_voitur_old = oldResp.getResponsabilite_select().getIndemnitVoiture();
          indem_repres_old = oldResp.getResponsabilite_select().getIndemnitRepresentation();
        }
        if (newResp != null) {
          indem_log_new = newResp.getResponsabilite_select().getIndemnitLogement();
          indem_foncNet_new = newResp.getResponsabilite_select().getIndemnitFonction();
          indem_voitur_new = newResp.getResponsabilite_select().getIndemnitVoiture();
          indem_repres_new = newResp.getResponsabilite_select().getIndemnitRepresentation();
        }
      }
      
      // l_resp==null
      
      if (oldResp == null && newResp == null) {
        setreponsabiliteNull(data, ls_retraite, ls_diff_rappel);
      } else {
        data.add("Indemnité de logement");
        data.add(indem_log_old.toString());
        data.add(indem_log_new.toString());
        data.add(indem_log_new.subtract(indem_log_old).toString());
        ls_retraite.add(indem_log_new);
        ls_diff_rappel.add(indem_log_new.subtract(indem_log_old));
        data.add("Indemnité de fonction nette");
        data.add(indem_foncNet_old.toString());
        data.add(indem_foncNet_new.toString());
        data.add(indem_foncNet_new.subtract(indem_foncNet_old).toString());
        ls_retraite.add(indem_foncNet_new);
        ls_diff_rappel.add(indem_foncNet_new.subtract(indem_foncNet_old));
        BigDecimal presentation_old =
                indem_voitur_old.add(indem_foncNet_old).add(indem_repres_old).add(indem_log_old);
        BigDecimal presentation_new =
                indem_voitur_new.add(indem_foncNet_new).add(indem_repres_new).add(indem_log_new);
        data.add("IND.RESPONSABILITE");
        data.add(presentation_old.toString());
        data.add(presentation_new.toString());
        data.add(presentation_new.subtract(presentation_old).toString());
        ls_retraite.add(indem_voitur_new);
        ls_diff_rappel.add(indem_voitur_new.subtract(indem_voitur_old));
        data.add("Indemnité de représentation");
        data.add(indem_repres_old.toString());
        data.add(indem_repres_new.toString());
        data.add(indem_repres_new.subtract(indem_repres_old).toString());
        ls_retraite.add(indem_repres_new);
        ls_diff_rappel.add(indem_repres_new.subtract(indem_repres_old));
      }
    }
    BigDecimal pourc2 = new BigDecimal(0.1);
    BigDecimal resid_n_old = gs_old.getTraitementDeBase().multiply(pourc2);
    BigDecimal resid_n_new = gs_new.getTraitementDeBase().multiply(pourc2);
    
    data.add("SALAIRE BRUT");
    BigDecimal total_brute_old = new BigDecimal(0);
    BigDecimal total_brute_new = new BigDecimal(0);
    total_brute_old =
            total_brute_old
                    .add(gs_old.getTraitementDeBase())
                    .add(resid_old)
                    .add(resid_n_old)
                    .add(gs_old.getIndemniteDeHierarchieAdministrative())
                    .add(gs_old.getIndemniteDeSujetion())
                    .add(gs_old.getIndemniteEncadrement());
    total_brute_new =
            total_brute_new
                    .add(gs_new.getTraitementDeBase())
                    .add(resid_new)
                    .add(resid_n_new)
                    .add(gs_new.getIndemniteDeHierarchieAdministrative())
                    .add(gs_new.getIndemniteDeSujetion())
                    .add(gs_new.getIndemniteEncadrement());
    data.add(
            total_brute_old
                    .add(BigDecimal.valueOf(child_mt_old))
                    .setScale(2, RoundingMode.HALF_UP)
                    .toString());
    data.add(
            total_brute_new
                    .add(BigDecimal.valueOf(child_mt_new))
                    .setScale(2, RoundingMode.HALF_UP)
                    .toString());
    data.add(
            total_brute_new.subtract(total_brute_old).setScale(2, RoundingMode.HALF_UP).toString());
    ls_retraite.add(total_brute_new);
    ls_diff_rappel.add(total_brute_new.subtract(total_brute_old));
    data.add("R.C.A.R.");
    RCAR rcar_old = serviceUtil.getRcarByYear(d1_moin1Day.getYear());
    if (rappel.equals("rappel")) {
      rcar_old = serviceUtil.getRcarByYear(LocalDate.parse(dateDebut).getYear());
    }
    if (rcar_old == null)
      rcar_old = serviceUtil.getRcarByYear(LocalDate.parse(dateDebut).getYear());
    RCAR rcar_new = serviceUtil.getRcarByYear(LocalDate.parse(dateFin).getYear());
    BigDecimal rcar_mt_old = new BigDecimal("0");
    BigDecimal rcar_mt_new = new BigDecimal("0");
    BigDecimal _prcent = new BigDecimal("100");
    // contractuelle == true
    
    if (!employee.getTypePersonnel()) {
      if (total_brute_old
              .multiply(rcar_old.getPors().divide(_prcent))
              .compareTo(rcar_old.getMontant_max())
              >= 0) {
        rcar_mt_old = rcar_old.getMontant_max();
      } else {
        rcar_mt_old = total_brute_old.multiply(rcar_old.getPors().divide(_prcent));
      }
      if (total_brute_new
              .multiply(rcar_new.getPors().divide(_prcent))
              .compareTo(rcar_new.getMontant_max())
              >= 0) {
        rcar_mt_new = rcar_new.getMontant_max();
      } else {
        rcar_mt_new = total_brute_new.multiply(rcar_new.getPors().divide(_prcent));
      }
    }
    
    RCARC comp_rcar_old = getComplementRcarByYear(d1_moin1Day.getYear());
    if (rappel.equals("rappel")) {
      comp_rcar_old = getComplementRcarByYear(LocalDate.parse(dateDebut).getYear());
    }
    if (comp_rcar_old == null)
      comp_rcar_old = getComplementRcarByYear(LocalDate.parse(dateDebut).getYear());
    RCARC comp_rcar_new = getComplementRcarByYear(LocalDate.parse(dateFin).getYear());
    BigDecimal compRcar_mt_old = new BigDecimal(0);
    BigDecimal compRcar_mt_new = new BigDecimal(0);
    
    if (!employee.getTypePersonnel()) {
      if (total_brute_old.compareTo(comp_rcar_old.getMontant()) > 0) {
        compRcar_mt_old =
                total_brute_old
                        .subtract(comp_rcar_old.getMontant())
                        .multiply(comp_rcar_old.getPors().divide(_prcent));
      }
      if (total_brute_new.compareTo(comp_rcar_new.getMontant()) > 0) {
        compRcar_mt_new =
                total_brute_new
                        .subtract(comp_rcar_new.getMontant())
                        .multiply(comp_rcar_new.getPors().divide(_prcent));
      }
    }
    
    BigDecimal rc_old = rcar_mt_old.add(compRcar_mt_old);
    BigDecimal rc_new = rcar_mt_new.add(compRcar_mt_new);
    data.add(rc_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(rc_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(rc_new.setScale(2, RoundingMode.HALF_UP).subtract(rc_old).toString());
    ls_retraite.add(rc_new);
    ls_diff_rappel.add(rc_new.subtract(rc_old));
    data.add("COMPLEMENT RCAR");
    
    data.add(compRcar_mt_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(compRcar_mt_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(
            compRcar_mt_new.subtract(compRcar_mt_old).setScale(2, RoundingMode.HALF_UP).toString());
    ls_retraite.add(compRcar_mt_new);
    ls_diff_rappel.add(compRcar_mt_new.subtract(compRcar_mt_old));
    // credit entre deux date
    BigDecimal credit_mt_old = new BigDecimal(0);
    BigDecimal credit_mt_new = new BigDecimal(0);
    credit_mt_old = getcreditAtdate(employee, d1_moin1Day);
    credit_mt_new = getcreditAtdate(employee, LocalDate.parse(dateFin));
    
    BigDecimal aos_mt_old = new BigDecimal(0);
    BigDecimal aos_mt_new = new BigDecimal(0);
    aos_mt_old = getcreditAosAtdate(employee, d1_moin1Day);
    aos_mt_new = getcreditAosAtdate(employee, LocalDate.parse(dateFin));
    data.add("AMO");
    BigDecimal amo_mt_old = new BigDecimal(0);
    BigDecimal amo_mt_new = new BigDecimal(0);
    MUTUELLE sec = null;
    MUTUELLE ccd = null;
    MUTUELLE amo = null;
    OrganismeMetuelle o = employee.getOrganismeMetuelle2();
    if (o.getId() == 1) {
      sec = getMutuelleById(2L);
      ccd = getMutuelleById(3L);
    } else {
      sec = getMutuelleById(4L);
      ccd = getMutuelleById(5L);
    }
    amo = getMutuelleById(1L);
    BigDecimal total_2_old = total_brute_old.subtract(indem_log_old);
    BigDecimal total_2_new = total_brute_new.subtract(indem_log_new);
    if (total_2_old.multiply(amo.getPors().divide(_prcent)).compareTo(amo.getMontant_max()) > 0) {
      amo_mt_old = amo.getMontant_max();
    } else if (total_2_old.multiply(amo.getPors().divide(_prcent)).compareTo(amo.getMontant_min())
            < 0) {
      amo_mt_old = amo.getMontant_min();
    } else {
      amo_mt_old = total_2_old.multiply(amo.getPors().divide(_prcent));
    }
    if (total_2_new.multiply(amo.getPors().divide(_prcent)).compareTo(amo.getMontant_max()) > 0) {
      amo_mt_new = amo.getMontant_max();
    } else if (total_2_new.multiply(amo.getPors().divide(_prcent)).compareTo(amo.getMontant_min())
            < 0) {
      amo_mt_new = amo.getMontant_min();
    } else {
      amo_mt_new = total_2_new.multiply(amo.getPors().divide(_prcent));
    }
    
    data.add(amo_mt_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(amo_mt_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(amo_mt_new.subtract(amo_mt_old).setScale(2, RoundingMode.HALF_UP).toString());
    ls_retraite.add(amo_mt_new);
    ls_diff_rappel.add(amo_mt_new.subtract(amo_mt_old));
    BigDecimal sec_mt_old = new BigDecimal(0);
    BigDecimal sec_mt_new = new BigDecimal(0);
    if (total_2_old.multiply(sec.getPors().divide(_prcent)).compareTo(sec.getMontant_max()) > 0) {
      // sec_mt_old = sec.getMontant_max().setScale(2,RoundingMode.HALF_UP);;
      sec_mt_old = sec.getMontant_max();
    } else if (total_2_old.multiply(sec.getPors().divide(_prcent)).compareTo(sec.getMontant_min())
            < 0) {
      sec_mt_old = sec.getMontant_min();
    } else {
      if (d1_annee.getYear() <= 2022) {
        sec_mt_old = new BigDecimal(0);
      } else {
        sec_mt_old = total_2_old.multiply(sec.getPors().divide(_prcent));
      }
    }
    if (total_2_new.multiply(sec.getPors().divide(_prcent)).compareTo(sec.getMontant_max()) > 0) {
      // sec_mt_new = sec.getMontant_max().setScale(2,RoundingMode.HALF_UP);;
      sec_mt_new = sec.getMontant_max();
    } else if (total_2_new.multiply(sec.getPors().divide(_prcent)).compareTo(sec.getMontant_min())
            < 0) {
      sec_mt_new = sec.getMontant_min();
    } else {
      if (d1_annee.getYear() <= 2022) {
        sec_mt_old = new BigDecimal(0);
      } else {
        sec_mt_new = total_2_new.multiply(sec.getPors().divide(_prcent));
      }
    }
    
    if (o.getId() == 1) data.add("SECTEUR MUTUALISTE");
    else data.add("MUTUELLE OMFAM SM");
    data.add(sec_mt_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(sec_mt_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(sec_mt_new.subtract(sec_mt_old).setScale(2, RoundingMode.HALF_UP).toString());
    ls_retraite.add(sec_mt_new);
    ls_diff_rappel.add(sec_mt_new.subtract(sec_mt_old));
    BigDecimal ccd_mt_old = new BigDecimal(0);
    BigDecimal ccd_mt_new = new BigDecimal(0);
    if (o.getId() == 1) {
      data.add("CCD");
      if (gs_old
              .getTraitementDeBase()
              .multiply(ccd.getPors().divide(_prcent))
              .compareTo(ccd.getMontant_max())
              > 0) {
        ccd_mt_old = ccd.getMontant_max();
      } else if (gs_old
              .getTraitementDeBase()
              .multiply(ccd.getPors().divide(_prcent))
              .compareTo(ccd.getMontant_min())
              < 0) {
        ccd_mt_old = ccd.getMontant_min();
      } else {
        ccd_mt_old = gs_old.getTraitementDeBase().multiply(ccd.getPors().divide(_prcent));
      }
      if (gs_new
              .getTraitementDeBase()
              .multiply(ccd.getPors().divide(_prcent))
              .compareTo(ccd.getMontant_max())
              > 0) {
        ccd_mt_new = ccd.getMontant_max();
      } else if (gs_new
              .getTraitementDeBase()
              .multiply(ccd.getPors().divide(_prcent))
              .compareTo(ccd.getMontant_min())
              < 0) {
        ccd_mt_new = ccd.getMontant_min();
      } else {
        ccd_mt_new = gs_new.getTraitementDeBase().multiply(ccd.getPors().divide(_prcent));
      }
      
      data.add(ccd_mt_old.setScale(2, RoundingMode.HALF_UP).toString());
      data.add(ccd_mt_new.setScale(2, RoundingMode.HALF_UP).toString());
      data.add(ccd_mt_new.subtract(ccd_mt_old).setScale(2, RoundingMode.HALF_UP).toString());
      ls_retraite.add(ccd_mt_new);
      ls_diff_rappel.add(ccd_mt_new.subtract(ccd_mt_old));
      
    } else {
      data.add("MUTUELLE OMFAM C.A.A.D");
      BigDecimal total_3_old = new BigDecimal(0);
      BigDecimal total_3_new = new BigDecimal(0);
      total_3_old = total_3_old.add(gs_old.getTraitementDeBase()).add(resid_old).add(resid_n_old);
      total_3_new = total_3_new.add(gs_new.getTraitementDeBase()).add(resid_new).add(resid_n_new);
      if (d1_annee.getYear() <= 2022) {
        ccd_mt_old = new BigDecimal(0);
      } else {
        ccd_mt_old = total_3_old.multiply(ccd.getPors().divide(_prcent));
      }
      
      if (d1_annee.getYear() <= 2022) {
        ccd_mt_new = new BigDecimal(0);
      } else {
        ccd_mt_new = total_3_new.multiply(ccd.getPors().divide(_prcent));
      }
      if (employee.getId() == 167) {
        ccd_mt_old = BigDecimal.valueOf(40);
        ccd_mt_new = BigDecimal.valueOf(40);
      } else {
        ccd_mt_old =
                ccd_mt_old.compareTo(ccd.getMontant_max()) > 0 ? ccd.getMontant_max() : ccd_mt_old;
        ccd_mt_new =
                ccd_mt_new.compareTo(ccd.getMontant_max()) > 0 ? ccd.getMontant_max() : ccd_mt_new;
      }
      data.add(ccd_mt_old.setScale(2, RoundingMode.HALF_UP).toString());
      data.add(ccd_mt_new.setScale(2, RoundingMode.HALF_UP).toString());
      data.add(ccd_mt_new.subtract(ccd_mt_old).setScale(2, RoundingMode.HALF_UP).toString());
      ls_retraite.add(ccd_mt_new);
      ls_diff_rappel.add(ccd_mt_new.subtract(ccd_mt_old));
    }
    data.add("I.R.");
    BigDecimal total_x_old = new BigDecimal(0);
    BigDecimal total_x_new = new BigDecimal(0);
    BigDecimal tva = new BigDecimal(20);
    BigDecimal mt1 = new BigDecimal(2500);
    
    if (d1.getYear() > 2022) {
      if (total_brute_old.compareTo(new BigDecimal(6500)) > 0) {
        tva = new BigDecimal(25);
        mt1 = new BigDecimal(2916.66);
      } else {
        tva = new BigDecimal(35);
        mt1 = new BigDecimal(2500);
      }
    }
    total_x_old = total_brute_old.multiply(tva.divide(_prcent));
    total_x_new = total_brute_new.multiply(tva.divide(_prcent));
    if (total_x_old.compareTo(mt1) > 0) {
      total_x_old = mt1;
    }
    if (total_x_new.compareTo(mt1) > 0) {
      total_x_new = mt1;
    }
    
    BigDecimal total_cnss_old = new BigDecimal(0);
    BigDecimal total_cnss_new = new BigDecimal(0);
    BigDecimal tva_cnss = new BigDecimal(4.48);
    BigDecimal mt_cnss = new BigDecimal(6000);
    if (employee.getIs_cnss()) {
      total_cnss_old = total_brute_old.multiply(tva_cnss.divide(_prcent));
      total_cnss_new = total_brute_new.multiply(tva_cnss.divide(_prcent));
      if (total_cnss_old.compareTo(mt_cnss) > 0) {
        total_cnss_old = mt_cnss;
      }
      if (total_cnss_new.compareTo(mt_cnss) > 0) {
        total_cnss_new = mt_cnss;
      }
    }
    
    BigDecimal some1_old =
            total_brute_old
                    .subtract(total_x_old)
                    .subtract(rcar_mt_old)
                    .subtract(compRcar_mt_old)
                    .subtract(amo_mt_old)
                    .subtract(total_cnss_old.setScale(2, RoundingMode.HALF_UP))
                    .subtract(sec_mt_old)
                    .subtract(ccd_mt_old)
                    .subtract(
                            employee.getIsCotisation()
                                    ? employee.getMontantCotisation()
                                    : BigDecimal.valueOf(0));
    BigDecimal some1_new =
            total_brute_new
                    .subtract(total_x_new)
                    .subtract(rcar_mt_new)
                    .subtract(compRcar_mt_new)
                    .subtract(amo_mt_new)
                    .subtract(total_cnss_new.setScale(2, RoundingMode.HALF_UP))
                    .subtract(sec_mt_new)
                    .subtract(
                            employee.getIsCotisation()
                                    ? employee.getMontantCotisation()
                                    : BigDecimal.valueOf(0))
                    .subtract(ccd_mt_new);
    BigDecimal ir_old = new BigDecimal(0);
    BigDecimal ir_new = new BigDecimal(0);
    BigDecimal mariage_old = new BigDecimal(0);
    BigDecimal mariage_new = new BigDecimal(0);
    if (employee.getMaritalStatus() != null && employee.getMaritalStatus().equals("1")) {
      String situation_old =
              getSitualtionAtDate(d1_moin1Day.toString(), employee.getMarriageDate());
      String situation_new = getSitualtionAtDate(dateFin, employee.getMarriageDate());
      mariage_old =
              (situation_old.equals("Marié(e)") && employee.getContactPartner().getTitleSelect() == 1)
                      ? BigDecimal.valueOf(1)
                      : BigDecimal.valueOf(0);
      mariage_new =
              (situation_new.equals("Marié(e)") && employee.getContactPartner().getTitleSelect() == 1)
                      ? BigDecimal.valueOf(1)
                      : BigDecimal.valueOf(0);
    } else {
      String situation_old = "Célibataire";
      String situation_new = "Célibataire";
      mariage_old = BigDecimal.valueOf(0);
      mariage_new = BigDecimal.valueOf(0);
    }
    // return tranche by montant
    IR ir_tmp_old = serviceUtil.getIRByMontnat(some1_old);
    int child_old_n = getNombreEnfantAtDate_27(employee, d1_moin1Day);
    
    // verifier si exonerer
    if (ir_tmp_old == null || ir_tmp_old.getExonerer()) {
      ir_old = BigDecimal.valueOf(0);
    } else {
      // si non exonerer calculer IR
      ir_old =
              (some1_old.multiply(ir_tmp_old.getPors().divide(_prcent)))
                      .subtract(ir_tmp_old.getMontant_red())
                      .subtract(BigDecimal.valueOf(30).multiply(new BigDecimal(child_old_n)))
                      .subtract(BigDecimal.valueOf(30).multiply(mariage_old));
    }
    // return tranche by montant
    IR ir_tmp_new = serviceUtil.getIRByMontnat(some1_new);
    int child_new_n = getNombreEnfantAtDate_27(employee, d2);
    // verifier si exonerer
    if (ir_tmp_new == null || ir_tmp_new.getExonerer()) {
      ir_new = BigDecimal.valueOf(0);
    } else {
      // si non exonerer calculer IR
      ir_new =
              (some1_new.multiply(ir_tmp_new.getPors().divide(_prcent)))
                      .subtract(ir_tmp_new.getMontant_red())
                      .subtract(BigDecimal.valueOf(30).multiply(new BigDecimal(child_new_n)))
                      .subtract(BigDecimal.valueOf(30).multiply(mariage_new));
    }
    
    if (employee.getId() == 133) {
      ir_old = BigDecimal.valueOf(1309);
      ir_new = BigDecimal.valueOf(1309);
    }
    ir_old = BigDecimal.ZERO.compareTo(ir_old) > 0 ? BigDecimal.ZERO : ir_old;
    ir_new = BigDecimal.ZERO.compareTo(ir_new) > 0 ? BigDecimal.ZERO : ir_new;
    BigDecimal ir_diff = ir_new.subtract(ir_old);
    data.add(ir_old.setScale(1, RoundingMode.HALF_UP).toString());
    data.add(ir_new.setScale(1, RoundingMode.HALF_UP).toString());
    data.add(ir_diff.setScale(1, RoundingMode.HALF_UP).toString());
    ls_retraite.add(ir_new);
    ls_diff_rappel.add(ir_diff);
    
    data.add("A.O.S.");
    Aos aos = employeHasAOSAtDate(employee.getId(), d1);
    BigDecimal mnt = BigDecimal.ZERO;
    AOS_config ao =
            Beans.get(AOS_configRepository.class)
                    .all()
                    .filter("self.grade.id=:grade")
                    .bind("grade", employee.getGrade().getId())
                    .fetchOne();
    mnt =
            ((ao != null && employee.getIs_aos()) ? ao.getMontant() : BigDecimal.ZERO)
                    .add(aos != null ? aos.getRemboursement() : BigDecimal.ZERO);
    
    data.add(mnt.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(mnt.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(mnt.setScale(2, RoundingMode.HALF_UP).toString());
    ls_retraite.add(mnt);
    ls_diff_rappel.add(mnt);
    
    BigDecimal tot_ret_old = new BigDecimal(0);
    BigDecimal tot_ret_new = new BigDecimal(0);
    
    tot_ret_old =
            rcar_mt_old
                    .add(compRcar_mt_old)
                    .add(amo_mt_old)
                    .add(sec_mt_old)
                    .add(ccd_mt_old)
                    .add(getcreditAtdate(employee, d1_moin1Day))
                    .add(
                            employee.getIsCotisation()
                                    ? employee.getMontantCotisation()
                                    : BigDecimal.valueOf(0))
                    .add(ir_old)
                    .add(mnt);
    tot_ret_new =
            rcar_mt_new
                    .add(compRcar_mt_new)
                    .add(amo_mt_new)
                    .add(sec_mt_new)
                    .add(getcreditAtdate(employee, LocalDate.parse(dateFin)))
                    .add(
                            employee.getIsCotisation()
                                    ? employee.getMontantCotisation()
                                    : BigDecimal.valueOf(0))
                    .add(ccd_mt_new)
                    .add(ir_new)
                    .add(mnt);
    
    data.add("SALAIRE NET MENSUEL");
    BigDecimal net_ordo_old = new BigDecimal(0);
    BigDecimal net_ordo_new = new BigDecimal(0);
    net_ordo_old =
            total_brute_old
                    .subtract(tot_ret_old)
                    .add(BigDecimal.valueOf(child_mt_old))
                    .add(indem_foncNet_old.setScale(2, RoundingMode.HALF_UP))
                    .add(indem_voitur_old.setScale(2, RoundingMode.HALF_UP))
                    .add(indem_repres_old.setScale(2, RoundingMode.HALF_UP))
                    .add(indem_log_old.setScale(2, RoundingMode.HALF_UP))
                    .subtract(total_cnss_old.setScale(2, RoundingMode.HALF_UP));
    net_ordo_new =
            total_brute_new
                    .subtract(tot_ret_new)
                    .add(BigDecimal.valueOf(child_mt_new))
                    .add(indem_foncNet_new.setScale(2, RoundingMode.HALF_UP))
                    .add(indem_voitur_new.setScale(2, RoundingMode.HALF_UP))
                    .add(indem_repres_new.setScale(2, RoundingMode.HALF_UP))
                    .add(indem_log_new.setScale(2, RoundingMode.HALF_UP))
                    .subtract(total_cnss_new.setScale(2, RoundingMode.HALF_UP));
    
    data.add(net_ordo_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(net_ordo_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(net_ordo_new.subtract(net_ordo_old).setScale(2, RoundingMode.HALF_UP).toString());
    ls_retraite.add(net_ordo_new);
    ls_diff_rappel.add(net_ordo_new.subtract(net_ordo_old));
    
    data.add("RE. CO. RE.");
    BigDecimal cotisation_emp_old = new BigDecimal(0);
    BigDecimal cotisation_emp_new = new BigDecimal(0);
    BigDecimal credit_emp = new BigDecimal(0);
    boolean hasCotisation1 = employeHasCotisationAtDate(id_emp, d1);
    boolean hasCotisation2 = employeHasCotisationAtDate(id_emp, d2);
    if (hasCotisation1)
      cotisation_emp_old = employee.getMontantCotisation().setScale(2, RoundingMode.HALF_UP);
    if (hasCotisation2)
      cotisation_emp_new = employee.getMontantCotisation().setScale(2, RoundingMode.HALF_UP);
    data.add(cotisation_emp_old.toString());
    data.add(cotisation_emp_new.toString());
    data.add(cotisation_emp_new.subtract(cotisation_emp_old).toString());
    ls_retraite.add(cotisation_emp_new);
    ls_diff_rappel.add(cotisation_emp_new.subtract(cotisation_emp_old));
    
    data.add("CREDITS");
    if (employer_has_credit(employee)) {
      data.add(credit_mt_old.setScale(2, RoundingMode.HALF_UP).toString());
      data.add(credit_mt_new.setScale(2, RoundingMode.HALF_UP).toString());
      data.add(credit_mt_new.subtract(credit_mt_old).setScale(2, RoundingMode.HALF_UP).toString());
      ls_retraite.add(credit_mt_new);
      ls_diff_rappel.add(credit_mt_new.subtract(credit_mt_old));
      
    } else {
      data.add("0");
      data.add("0");
      data.add("0");
      ls_retraite.add(BigDecimal.ZERO);
      ls_diff_rappel.add(BigDecimal.ZERO);
    }
    List<BigDecimal> ls_rappel = new ArrayList<BigDecimal>();
    for (BigDecimal tmp : ls_retraite) {
      BigDecimal retrait = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
      if (coef.compareTo(BigDecimal.ZERO) > 0
              && tmp.compareTo(BigDecimal.ZERO) > 0
              && mois == 0
              && jour == 0) {
        BigDecimal dat_ = coef.multiply(tmp);
        retrait =
                dat_.divide(
                        new BigDecimal(LocalDate.parse(dateFin).lengthOfMonth()),
                        2,
                        RoundingMode.HALF_UP); // (nbr*sal)/maxjour
      }
      data.add(retrait.toString());
    }
    
    if ((mois > 0 || jour > 0) && rappel.equals("rappel")) {
      if (getNbrAugmentation(id_emp) == 1) { // rappel pour nouv employer
        for (int i = 0; i < ls_retraite.size(); i++) {
          BigDecimal tmp = ls_retraite.get(i);
          BigDecimal rappel_nbr = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
          BigDecimal dat_1 = tmp.multiply(BigDecimal.valueOf(jour));
          if (d1.getDayOfMonth() != 1) {
            int x_jour = LocalDate.parse(dateFin).lengthOfMonth();
            dat_1 = dat_1.divide(new BigDecimal(x_jour), 2, RoundingMode.HALF_UP);
          }
          rappel_nbr = tmp.multiply(BigDecimal.valueOf(mois)).add(dat_1);
          ls_rappel.add(rappel_nbr);
        }
        
      } else if (getNbrAugmentation(id_emp) > 1) { // rappel ci employee est ancien
        for (int i = 0; i < ls_diff_rappel.size(); i++) {
          BigDecimal tmp = ls_diff_rappel.get(i);
          BigDecimal rappel_nbr = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
          BigDecimal dat_1 = tmp.multiply(BigDecimal.valueOf(jour));
          if (d1.getDayOfMonth() != 1) {
            dat_1 =
                    dat_1.divide(
                            new BigDecimal(LocalDate.parse(dateDebut).lengthOfMonth()),
                            2,
                            RoundingMode.HALF_UP);
          } else {
            dat_1 =
                    dat_1.divide(
                            new BigDecimal(LocalDate.parse(dateFin).lengthOfMonth()),
                            2,
                            RoundingMode.HALF_UP);
          }
          rappel_nbr = tmp.multiply(BigDecimal.valueOf(mois)).add(dat_1);
          ls_rappel.add(rappel_nbr);
        }
      }
      Augmentation aa = getEmployer_has_Augmentation(id_emp);
      EtatDesRappelSurSalaire etat = new EtatDesRappelSurSalaire();
      etat.setCin(employee.getCin());
      etat.setGrade(aa.getGrade());
      etat.setPeriode(
              "Du "
                      + aa.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                      + " au "
                      + d2.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
      etat.setMatriculeInterne(employee.getMatriculationInterne());
      etat.setNomEtPrenom(employee.getContactPartner().getSimpleFullName());
      etat.setImmatriculationCnops(employee.getImmatriculationSM());
      etat.setDate_debut(d1);
      etat.setDate_fin(d2);
      etat.setId_employer(employee.getId());
      if (aa.getAvtRecalss() != null) {
        String avt =
                aa.getAvtRecalss().equals("Avt")
                        ? "Avt d'échelon"
                        : (aa.getAvtRecalss().equals("Reclassement") ? "Reclasst d'échelle" : "");
        etat.setRappleRegularisation(avt);
      }
      etat.setAffiliation_mutuelle(employee.getAffiliationSM());
      saveEtatDesRappelSurSalaire(etat, ls_rappel, employee.getOrganismeMetuelle2().getName());
      
    } else {
      for (BigDecimal tmp : ls_retraite) {
        ls_rappel.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
      }
    }
    for (int i = 0; i < ls_rappel.size(); i++) {
      data.add(ls_rappel.get(i).toString());
    }
    
    data.add("Indemnité de résidence de base");
    BigDecimal tmp1 = null;
    BigDecimal rappel_nbr1 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    if (employee.getZoneEmployee().getId() == 3
            || employee.getZoneEmployee().getId() == 2
            || employee.getZoneEmployee().getId() == 1
            || employee.getZoneEmployee().getId() == 4)
      tmp1 =
              gs_new
                      .getTraitementDeBase()
                      .subtract(gs_old.getTraitementDeBase())
                      .multiply(BigDecimal.valueOf(0.10));
    BigDecimal dat_1 = tmp1.multiply(BigDecimal.valueOf(jour));
    if (d1.getDayOfMonth() != 1) {
      int x_jour = LocalDate.parse(dateFin).lengthOfMonth();
      dat_1 = dat_1.divide(new BigDecimal(x_jour), 2, RoundingMode.HALF_UP);
    }
    rappel_nbr1 = tmp1.multiply(BigDecimal.valueOf(mois)).add(dat_1);
    ls_rappel.add(rappel_nbr1);
    
    data.add(resid_n_old.toString());
    data.add(resid_n_new.toString());
    
    data.add("BRUT MENSUEL");
    boolean hasPrime1 = employeHasPrimeAtDate(id_emp, d1);
    boolean hasPrime2 = employeHasPrimeAtDate(id_emp, d2);
    BigDecimal prime_emp = employee.getMontant_prime();
    data.add(
            total_brute_old
                    .add(new BigDecimal(child_mt_old))
                    .add(indem_foncNet_old)
                    .add(indem_voitur_old)
                    .add(indem_repres_old)
                    .add(indem_log_old)
                    .toString());
    data.add(
            total_brute_new
                    .add(new BigDecimal(child_mt_new))
                    .add(indem_foncNet_new)
                    .add(indem_voitur_new)
                    .add(indem_repres_new)
                    .add(indem_log_new)
                    .toString());
    
    data.add(String.valueOf(rappel_nbr1));
    
    String x = StringUtils.join(data, "__");
    return x;
  }
  
  public Map<String, BigDecimal> getData_By_Date(Employee emp) {
    etat = true;
    Map<String, BigDecimal> map = new HashMap<>();
    LocalDate date_d = emp.getDateProlongation().minusMonths(1);
    LocalDate date_f = emp.getDateProlongation().minusDays(1);
    String data = getDataIndemnite(date_d.toString(), date_f.toString(), emp.getId(), "");
    etat = false;
    String[] table = data.split("__");
    for (int i = 0; i < table.length; i++) {
      String t = table[i];
      if (t.equals("NET A ORDONNANCER")) {
        map.put("net_old", new BigDecimal(table[i + 1]));
        map.put("net_new", new BigDecimal(table[i + 2]));
        return map;
      }
    }
    map.put("net_old", new BigDecimal(0));
    map.put("net_new", new BigDecimal(0));
    return map;
  }
  
  //////////////////// getDataIndemniteRappel pour stocker le rappel f rappelemploye
  // //////////////////////////////////////////////
  @Transactional
  public String getDataIndemniteRappel(
          String dateDebut, String dateFin, Long id_emp, String rappel) {
    
    List<String> data = new ArrayList<>();
    Employee employee = employeeRepository.find(id_emp);
    RappelEmploye ra = new RappelEmploye();
    int nbr_aug = getNombreAugmentation(employee.getId());
    Corps corps_old = null;
    Corps corps_new = null;
    Grade grade_old = null;
    Grade grade_new = null;
    Echelon echelon_old = null;
    Echelon echelon_new = null;
    LocalDate d1_moin1Day = LocalDate.parse(dateDebut);
    d1_moin1Day = d1_moin1Day.minusDays(1);
    if (nbr_aug == 0) {
      corps_old = corps_new = employee.getCorps();
      grade_old = grade_new = employee.getGrade();
      echelon_old = echelon_new = employee.getEchelon();
    } else {
      Augmentation a1 =
              getEchelleAtDate(d1_moin1Day.toString(), d1_moin1Day.toString(), employee, "Debut");
      if (a1 == null) a1 = getEchelleAtDate(dateDebut, dateDebut, employee, "Debut");
      Augmentation a2 = getEchelleAtDate(dateFin, dateFin, employee, "Fin");
      if (a1.getDateRappel() == null
              || a1.getDateRappel().compareTo(LocalDate.parse(dateDebut)) > 0) {
        corps_old = a1.getGrade_old().getCorps();
        grade_old = a1.getGrade_old();
        echelon_old = a1.getEchelon_old();
      } else {
        corps_old = a1.getGrade().getCorps();
        grade_old = a1.getGrade();
        echelon_old = a1.getEchelon();
      }
      if (a2.getDateRappel() == null
              || a2.getDateRappel().compareTo(LocalDate.parse(dateFin)) > 0) {
        corps_new = a2.getGrade_old().getCorps();
        grade_new = a2.getGrade_old();
        echelon_new = a2.getEchelon_old();
      } else {
        corps_new = a2.getGrade().getCorps();
        grade_new = a2.getGrade();
        echelon_new = a2.getEchelon();
      }
    }
    // traitement de base
    GestionSalaire gs_old =
            gestionSalaireRepository
                    .all()
                    .filter(
                            "self.corps=?1 and self.grade=?2 and self.echelon=?3 and self.indice=?4",
                            corps_old,
                            grade_old,
                            echelon_old.getName(),
                            echelon_old.getIndice())
                    .fetchOne();
    BigDecimal coef = BigDecimal.ZERO;
    ArrayList<BigDecimal> ls_retraite = new ArrayList<BigDecimal>();
    ArrayList<BigDecimal> ls_diff_rappel = new ArrayList<BigDecimal>();
    if ((LocalDate.parse(dateDebut).getDayOfMonth() != 1
            || LocalDate.parse(dateFin).getDayOfMonth() != LocalDate.parse(dateFin).lengthOfMonth())
            && LocalDate.parse(dateDebut).getMonthValue() == LocalDate.parse(dateFin).getMonthValue()) {
      // LE meme mois get difference by day
      coef =
              coef.add(
                      BigDecimal.valueOf(
                              LocalDate.parse(dateDebut).until(LocalDate.parse(dateFin), ChronoUnit.DAYS) + 1));
    }
    long mois = 0, jour = 0;
    LocalDate d1 = LocalDate.parse(dateDebut);
    LocalDate d2 = LocalDate.parse(dateFin);
    
    if (rappel.equals("rappel")) {
      if (d1.getDayOfMonth() != 1
              && d2.getDayOfMonth() == d2.lengthOfMonth()
              && d1.getMonthValue() != d2.getMonthValue()) {
        // mois et jour 2mois et 15 jours 12-9-2021 --> 30-11-2021
        LocalDate date_fin_mois =
                LocalDate.parse(
                        d1.getYear() + "-" + mois_format(d1.getMonthValue()) + "-" + d1.lengthOfMonth());
        jour = d1.until(date_fin_mois, ChronoUnit.DAYS) + 1;
        LocalDate date_debut_next = date_fin_mois.plusDays(1l);
        mois = (ChronoUnit.MONTHS.between(date_debut_next, d2));
        
      } else if (d1.getDayOfMonth() == 1
              && d2.getDayOfMonth() != d2.lengthOfMonth()
              && d1.getMonthValue() != d2.getMonthValue()) {
        // mois et jour 2mois et 15 jours 01-9-2021 --> 15-11-2021
        LocalDate date_fin_mois =
                LocalDate.parse(d1.getYear() + "-" + mois_format(d2.getMonthValue()) + "-01");
        date_fin_mois = date_fin_mois.minusDays(1l);
        mois = (ChronoUnit.MONTHS.between(d1, date_fin_mois));
        jour = date_fin_mois.plusDays(1l).until(d2, ChronoUnit.DAYS) + 1;
      } else if (d1.getDayOfMonth() != 1
              && d2.getDayOfMonth() == d2.lengthOfMonth()
              && d1.getMonthValue() == d2.getMonthValue()) {
        mois = 0;
        jour = d1.until(d2, ChronoUnit.DAYS) + 1;
      } else {
        mois = (ChronoUnit.MONTHS.between(d1, d2));
      }
    }
    ra.setNbr_mois((int) mois);
    GestionSalaire gs_new =
            gestionSalaireRepository
                    .all()
                    .filter(
                            "self.corps=?1 "
                                    + "and self.grade=?2 "
                                    + "and self.echelon=?3 "
                                    + "and self.indice=?4",
                            corps_new,
                            grade_new,
                            echelon_new.getName(),
                            echelon_new.getIndice())
                    .fetchOne();
    data.add("Traitement de base mensuel");
    data.add(gs_old.getTraitementDeBase().toString());
    data.add(gs_new.getTraitementDeBase().toString());
    data.add(gs_new.getTraitementDeBase().subtract(gs_old.getTraitementDeBase()).toString());
    ls_retraite.add(gs_new.getTraitementDeBase());
    ls_diff_rappel.add(gs_new.getTraitementDeBase().subtract(gs_old.getTraitementDeBase()));
    ra.setTraitementDebase_rappel(
            gs_new.getTraitementDeBase().subtract(gs_old.getTraitementDeBase()));
    data.add("Indemnité de résidence complémentaire");
    BigDecimal pourc = new BigDecimal(1);
    if (employee.getZoneEmployee().getId() == 3) pourc = BigDecimal.valueOf(0.15);
    else if (employee.getZoneEmployee().getId() == 2) pourc = BigDecimal.valueOf(0.05);
    else if (employee.getZoneEmployee().getId() == 1 || employee.getZoneEmployee().getId() == 4)
      pourc = BigDecimal.valueOf(0.0);
    BigDecimal resid_old = gs_old.getTraitementDeBase().multiply(pourc);
    BigDecimal resid_new = gs_new.getTraitementDeBase().multiply(pourc);
    data.add(resid_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(resid_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(resid_new.subtract(resid_old).setScale(2, RoundingMode.HALF_UP).toString());
    ls_retraite.add(resid_new.setScale(2, RoundingMode.HALF_UP));
    ls_diff_rappel.add(resid_new.subtract(resid_old));
    // ra.setIndemniteDeResidence_rappel(resid_new.subtract(resid_old));
    String titre = "Allocation de hiérarchie administrative";
    if (employee.getCorps().getId() == 16)
      titre = "Indemnité Administrative Spéciale"; // administrateur
    else if (employee.getCorps().getId() == 19 || employee.getCorps().getId() == 14)
      titre = "Indemnité de technicité"; // Technicien - Ingenieur
    data.add(titre);
    data.add(gs_old.getIndemniteDeHierarchieAdministrative().toString());
    data.add(gs_new.getIndemniteDeHierarchieAdministrative().toString());
    data.add(
            gs_new
                    .getIndemniteDeHierarchieAdministrative()
                    .subtract(gs_old.getIndemniteDeHierarchieAdministrative())
                    .toString());
    ls_retraite.add(gs_new.getIndemniteDeHierarchieAdministrative());
    ls_diff_rappel.add(
            gs_new
                    .getIndemniteDeHierarchieAdministrative()
                    .subtract(gs_old.getIndemniteDeHierarchieAdministrative()));
    ra.setIasIndhierIndDeTech_rappel(
            gs_new
                    .getIndemniteDeHierarchieAdministrative()
                    .subtract(gs_old.getIndemniteDeHierarchieAdministrative()));
    data.add("Indemnité de sujétion");
    data.add(gs_old.getIndemniteDeSujetion().toString());
    data.add(gs_new.getIndemniteDeSujetion().toString());
    data.add(gs_new.getIndemniteDeSujetion().subtract(gs_old.getIndemniteDeSujetion()).toString());
    ls_retraite.add(gs_new.getIndemniteDeSujetion());
    ls_diff_rappel.add(gs_new.getIndemniteDeSujetion().subtract(gs_old.getIndemniteDeSujetion()));
    ra.setIndemniteDeSujetion_rappel(
            gs_new.getIndemniteDeSujetion().subtract(gs_old.getIndemniteDeSujetion()));
    data.add("Indemnité d'encadrement");
    data.add(gs_old.getIndemniteEncadrement().toString());
    data.add(gs_new.getIndemniteEncadrement().toString());
    data.add(
            gs_new.getIndemniteEncadrement().subtract(gs_new.getIndemniteEncadrement()).toString());
    ls_retraite.add(gs_new.getIndemniteEncadrement());
    ls_diff_rappel.add(gs_new.getIndemniteEncadrement().subtract(gs_new.getIndemniteEncadrement()));
    ra.setIndemniteDencadrement_rappel(
            gs_new.getIndemniteEncadrement().subtract(gs_new.getIndemniteEncadrement()));
    data.add("Allocations familiales");
    int child_old = getNombreEnfantAtDate_21(employee, d1_moin1Day);
    int child_new = getNombreEnfantAtDate_21(employee, dateFin);
    
    int child_mt_old, child_mt_new;
    child_mt_old = child_mt_new = 0;
    if (child_old < 6) {
      if (child_old < 3) {
        child_mt_old = child_old * 300;
      } else {
        child_mt_old = 900 + ((child_old - 3) * 36);
      }
    } else {
      child_mt_old = 1008;
    }
    if (child_new < 6) {
      if (child_new < 3) {
        child_mt_new = child_new * 300;
      } else {
        child_mt_new = 900 + ((child_new - 3) * 36);
      }
    } else {
      child_mt_new = 1008;
    }
    data.add(String.valueOf(child_mt_old));
    data.add(String.valueOf(child_mt_new));
    data.add(String.valueOf(child_mt_new - child_mt_old));
    ls_retraite.add(BigDecimal.valueOf(child_mt_new));
    ls_diff_rappel.add(BigDecimal.valueOf(child_mt_new).subtract(BigDecimal.valueOf(child_mt_old)));
    int nbrHp_2 =
            employee.getResponsabiliteList() != null ? employee.getResponsabiliteList().size() : 0;
    BigDecimal indem_log_old = new BigDecimal(0);
    BigDecimal indem_log_new = new BigDecimal(0);
    BigDecimal indem_foncNet_old = new BigDecimal(0);
    BigDecimal indem_foncNet_new = new BigDecimal(0);
    BigDecimal indem_voitur_old = new BigDecimal(0);
    BigDecimal indem_voitur_new = new BigDecimal(0);
    BigDecimal indem_repres_old = new BigDecimal(0);
    BigDecimal indem_repres_new = new BigDecimal(0);
    if (nbrHp_2 == 0) {
      setreponsabiliteNull(data, ls_retraite, ls_diff_rappel);
    } else {
      ResponsabiliteList oldResp = getResponsabiliteAtDate(employee, d1_moin1Day);
      if (oldResp == null) oldResp = getResponsabiliteAtDate(employee, LocalDate.parse(dateDebut));
      ResponsabiliteList newResp = getResponsabiliteAtDate(employee, LocalDate.parse(dateFin));
      if (oldResp != null
              && newResp != null
              && newResp.getResponsabilite_select().getId()
              != oldResp.getResponsabilite_select().getId()
              && newResp.getDate_debut().getDayOfMonth() != 1) {
        // calcule
        System.out.println("calcule");
        indem_log_old = oldResp.getResponsabilite_select().getIndemnitLogement();
        indem_log_new = calcule_indemnite(1, oldResp, newResp, LocalDate.parse(dateFin));
        
        indem_foncNet_old = oldResp.getResponsabilite_select().getIndemnitFonction();
        indem_foncNet_new = calcule_indemnite(2, oldResp, newResp, LocalDate.parse(dateFin));
        
        indem_voitur_old = oldResp.getResponsabilite_select().getIndemnitVoiture();
        indem_voitur_new = calcule_indemnite(3, oldResp, newResp, LocalDate.parse(dateFin));
        
        indem_repres_old = oldResp.getResponsabilite_select().getIndemnitRepresentation();
        indem_repres_new = calcule_indemnite(4, oldResp, newResp, LocalDate.parse(dateFin));
        
      } else if (oldResp != null || newResp != null) {
        if (oldResp != null) {
          indem_log_old = oldResp.getResponsabilite_select().getIndemnitLogement();
          indem_foncNet_old = oldResp.getResponsabilite_select().getIndemnitFonction();
          indem_voitur_old = oldResp.getResponsabilite_select().getIndemnitVoiture();
          indem_repres_old = oldResp.getResponsabilite_select().getIndemnitRepresentation();
        }
        if (newResp != null) {
          indem_log_new = newResp.getResponsabilite_select().getIndemnitLogement();
          indem_foncNet_new = newResp.getResponsabilite_select().getIndemnitFonction();
          indem_voitur_new = newResp.getResponsabilite_select().getIndemnitVoiture();
          indem_repres_new = newResp.getResponsabilite_select().getIndemnitRepresentation();
        }
      }
      
      // l_resp==null
      
      if (oldResp == null && newResp == null) {
        setreponsabiliteNull(data, ls_retraite, ls_diff_rappel);
      } else {
        data.add("Indemnité de logement");
        data.add(indem_log_old.toString());
        data.add(indem_log_new.toString());
        data.add(indem_log_new.subtract(indem_log_old).toString());
        ls_retraite.add(indem_log_new);
        ls_diff_rappel.add(indem_log_new.subtract(indem_log_old));
        ra.setIndemniteLogemBrut_rappel(indem_log_new.subtract(indem_log_old));
        data.add("Indemnité de fonction nette");
        data.add(indem_foncNet_old.toString());
        data.add(indem_foncNet_new.toString());
        data.add(indem_foncNet_new.subtract(indem_foncNet_old).toString());
        ls_retraite.add(indem_foncNet_new);
        ls_diff_rappel.add(indem_foncNet_new.subtract(indem_foncNet_old));
        ra.setIndemnitFonctionNet_rappel(indem_foncNet_new.subtract(indem_foncNet_old));
        data.add("Indemnité de voiture nette");
        data.add(indem_voitur_old.toString());
        data.add(indem_voitur_new.toString());
        data.add(indem_voitur_new.subtract(indem_voitur_old).toString());
        ls_retraite.add(indem_voitur_new);
        ls_diff_rappel.add(indem_voitur_new.subtract(indem_voitur_old));
        ra.setIndemnitVoitureNet_rappel(indem_voitur_new.subtract(indem_voitur_old));
        data.add("Indemnité de représentation");
        data.add(indem_repres_old.toString());
        data.add(indem_repres_new.toString());
        data.add(indem_repres_new.subtract(indem_repres_old).toString());
        ls_retraite.add(indem_repres_new);
        ls_diff_rappel.add(indem_repres_new.subtract(indem_repres_old));
        ra.setIndemnitRepresentNet_rappel(indem_repres_new.subtract(indem_repres_old));
      }
    }
    BigDecimal pourc2 = new BigDecimal(0.1);
    BigDecimal resid_n_old = gs_old.getTraitementDeBase().multiply(pourc2);
    BigDecimal resid_n_new = gs_new.getTraitementDeBase().multiply(pourc2);
    
    data.add("TOTAL BRUT (sans allocation familiale)");
    BigDecimal total_brute_old = new BigDecimal(0);
    BigDecimal total_brute_new = new BigDecimal(0);
    total_brute_old =
            total_brute_old
                    .add(gs_old.getTraitementDeBase())
                    .add(resid_old)
                    .add(resid_n_old)
                    .add(gs_old.getIndemniteDeHierarchieAdministrative())
                    .add(gs_old.getIndemniteDeSujetion())
                    .add(gs_old.getIndemniteEncadrement());
    total_brute_new =
            total_brute_new
                    .add(gs_new.getTraitementDeBase())
                    .add(resid_new)
                    .add(resid_n_new)
                    .add(gs_new.getIndemniteDeHierarchieAdministrative())
                    .add(gs_new.getIndemniteDeSujetion())
                    .add(gs_new.getIndemniteEncadrement());
    data.add(total_brute_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(total_brute_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(
            total_brute_new.subtract(total_brute_old).setScale(2, RoundingMode.HALF_UP).toString());
    ls_retraite.add(total_brute_new);
    ls_diff_rappel.add(total_brute_new.subtract(total_brute_old));
    data.add("RCAR");
    RCAR rcar_old = serviceUtil.getRcarByYear(d1_moin1Day.getYear());
    if (rappel.equals("rappel")) {
      rcar_old = serviceUtil.getRcarByYear(LocalDate.parse(dateDebut).getYear());
    }
    if (rcar_old == null)
      rcar_old = serviceUtil.getRcarByYear(LocalDate.parse(dateDebut).getYear());
    RCAR rcar_new = serviceUtil.getRcarByYear(LocalDate.parse(dateFin).getYear());
    BigDecimal rcar_mt_old = new BigDecimal("0");
    BigDecimal rcar_mt_new = new BigDecimal("0");
    BigDecimal _prcent = new BigDecimal("100");
    // contractuelle == true
    if (!employee.getTypePersonnel()) {
      if (total_brute_old
              .multiply(rcar_old.getPors().divide(_prcent))
              .compareTo(rcar_old.getMontant_max())
              >= 0) {
        rcar_mt_old = rcar_old.getMontant_max();
      } else {
        rcar_mt_old = total_brute_old.multiply(rcar_old.getPors().divide(_prcent));
      }
      if (total_brute_new
              .multiply(rcar_new.getPors().divide(_prcent))
              .compareTo(rcar_new.getMontant_max())
              >= 0) {
        rcar_mt_new = rcar_new.getMontant_max();
      } else {
        rcar_mt_new = total_brute_new.multiply(rcar_new.getPors().divide(_prcent));
      }
    }
    
    data.add(rcar_mt_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(rcar_mt_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(rcar_mt_new.setScale(2, RoundingMode.HALF_UP).subtract(rcar_mt_old).toString());
    ls_retraite.add(rcar_mt_new);
    ls_diff_rappel.add(rcar_mt_new.subtract(rcar_mt_old));
    ra.setRcar_rappel(
            rcar_mt_new
                    .setScale(2, RoundingMode.HALF_UP)
                    .subtract(rcar_mt_old.setScale(2, RoundingMode.HALF_UP)));
    data.add("COMPLEMENT RCAR");
    RCARC comp_rcar_old = getComplementRcarByYear(d1_moin1Day.getYear());
    if (rappel.equals("rappel")) {
      comp_rcar_old = getComplementRcarByYear(LocalDate.parse(dateDebut).getYear());
    }
    if (comp_rcar_old == null)
      comp_rcar_old = getComplementRcarByYear(LocalDate.parse(dateDebut).getYear());
    RCARC comp_rcar_new = getComplementRcarByYear(LocalDate.parse(dateFin).getYear());
    BigDecimal compRcar_mt_old = new BigDecimal(0);
    BigDecimal compRcar_mt_new = new BigDecimal(0);
    // contractuelle -> getTypePersonnel== true
    if (!employee.getTypePersonnel()) {
      if (total_brute_old.compareTo(comp_rcar_old.getMontant()) > 0) {
        compRcar_mt_old =
                total_brute_old
                        .subtract(comp_rcar_old.getMontant())
                        .multiply(comp_rcar_old.getPors().divide(_prcent));
      }
      if (total_brute_new.compareTo(comp_rcar_new.getMontant()) > 0) {
        compRcar_mt_new =
                total_brute_new
                        .subtract(comp_rcar_new.getMontant())
                        .multiply(comp_rcar_new.getPors().divide(_prcent));
      }
    }
    
    data.add(compRcar_mt_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(compRcar_mt_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(
            compRcar_mt_new.subtract(compRcar_mt_old).setScale(2, RoundingMode.HALF_UP).toString());
    ls_retraite.add(compRcar_mt_new);
    ls_diff_rappel.add(compRcar_mt_new.subtract(compRcar_mt_old));
    ra.setComp_rappel(
            compRcar_mt_new
                    .setScale(2, RoundingMode.HALF_UP)
                    .subtract(compRcar_mt_old.setScale(2, RoundingMode.HALF_UP)));
    // credit entre deux date
    BigDecimal credit_mt_old = new BigDecimal(0);
    BigDecimal credit_mt_new = new BigDecimal(0);
    credit_mt_old = getcreditAtdate(employee, d1_moin1Day);
    credit_mt_new = getcreditAtdate(employee, LocalDate.parse(dateFin));
    data.add("MUTUELLE ( A.M.O )");
    BigDecimal amo_mt_old = new BigDecimal(0);
    BigDecimal amo_mt_new = new BigDecimal(0);
    MUTUELLE sec = null;
    MUTUELLE ccd = null;
    MUTUELLE amo = null;
    OrganismeMetuelle o = employee.getOrganismeMetuelle2();
    if (o.getId() == 1) {
      sec = getMutuelleById(2L);
      ccd = getMutuelleById(3L);
    } else {
      sec = getMutuelleById(4L);
      ccd = getMutuelleById(5L);
    }
    amo = getMutuelleById(1L);
    BigDecimal total_2_old = total_brute_old.subtract(indem_log_old);
    BigDecimal total_2_new = total_brute_new.subtract(indem_log_new);
    if (total_2_old.multiply(amo.getPors().divide(_prcent)).compareTo(amo.getMontant_max()) > 0) {
      amo_mt_old = amo.getMontant_max();
    } else if (total_2_old.multiply(amo.getPors().divide(_prcent)).compareTo(amo.getMontant_min())
            < 0) {
      amo_mt_old = amo.getMontant_min();
    } else {
      amo_mt_old = total_2_old.multiply(amo.getPors().divide(_prcent));
    }
    if (total_2_new.multiply(amo.getPors().divide(_prcent)).compareTo(amo.getMontant_max()) > 0) {
      amo_mt_new = amo.getMontant_max();
    } else if (total_2_new.multiply(amo.getPors().divide(_prcent)).compareTo(amo.getMontant_min())
            < 0) {
      amo_mt_new = amo.getMontant_min();
    } else {
      amo_mt_new = total_2_new.multiply(amo.getPors().divide(_prcent));
    }
    
    data.add(amo_mt_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(amo_mt_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(amo_mt_new.subtract(amo_mt_old).setScale(2, RoundingMode.HALF_UP).toString());
    ls_retraite.add(amo_mt_new);
    ls_diff_rappel.add(amo_mt_new.subtract(amo_mt_old));
    ra.setAmo_rappel(
            amo_mt_new
                    .setScale(2, RoundingMode.HALF_UP)
                    .subtract(amo_mt_old.setScale(2, RoundingMode.HALF_UP)));
    BigDecimal sec_mt_old = new BigDecimal(0);
    BigDecimal sec_mt_new = new BigDecimal(0);
    if (total_2_old.multiply(sec.getPors().divide(_prcent)).compareTo(sec.getMontant_max()) > 0) {
      sec_mt_old = sec.getMontant_max();
    } else if (total_2_old.multiply(sec.getPors().divide(_prcent)).compareTo(sec.getMontant_min())
            < 0) {
      sec_mt_old = sec.getMontant_min();
    } else {
      sec_mt_old = total_2_old.multiply(sec.getPors().divide(_prcent));
    }
    if (total_2_new.multiply(sec.getPors().divide(_prcent)).compareTo(sec.getMontant_max()) > 0) {
      sec_mt_new = sec.getMontant_max();
    } else if (total_2_new.multiply(sec.getPors().divide(_prcent)).compareTo(sec.getMontant_min())
            < 0) {
      sec_mt_new = sec.getMontant_min();
    } else {
      sec_mt_new = total_2_new.multiply(sec.getPors().divide(_prcent));
    }
    
    if (o.getId() == 1) data.add("MUTUELLE SEC MUTUALISTE");
    else data.add("MUTUELLE OMFAM SM");
    data.add(sec_mt_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(sec_mt_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(sec_mt_new.subtract(sec_mt_old).setScale(2, RoundingMode.HALF_UP).toString());
    ls_retraite.add(sec_mt_new);
    ls_diff_rappel.add(sec_mt_new.subtract(sec_mt_old));
    ra.setMutuelleOmfamSm_rappel(
            sec_mt_new
                    .setScale(2, RoundingMode.HALF_UP)
                    .subtract(sec_mt_old.setScale(2, RoundingMode.HALF_UP)));
    BigDecimal ccd_mt_old = new BigDecimal(0);
    BigDecimal ccd_mt_new = new BigDecimal(0);
    if (o.getId() == 1) {
      data.add("MUTUELLE C.C.D");
      if (gs_old
              .getTraitementDeBase()
              .multiply(ccd.getPors().divide(_prcent))
              .compareTo(ccd.getMontant_max())
              > 0) {
        ccd_mt_old = ccd.getMontant_max();
      } else if (gs_old
              .getTraitementDeBase()
              .multiply(ccd.getPors().divide(_prcent))
              .compareTo(ccd.getMontant_min())
              < 0) {
        ccd_mt_old = ccd.getMontant_min();
      } else {
        ccd_mt_old = gs_old.getTraitementDeBase().multiply(ccd.getPors().divide(_prcent));
      }
      
      if (gs_new
              .getTraitementDeBase()
              .multiply(ccd.getPors().divide(_prcent))
              .compareTo(ccd.getMontant_max())
              > 0) {
        ccd_mt_new = ccd.getMontant_max();
      } else if (gs_new
              .getTraitementDeBase()
              .multiply(ccd.getPors().divide(_prcent))
              .compareTo(ccd.getMontant_min())
              < 0) {
        ccd_mt_new = ccd.getMontant_min();
      } else {
        ccd_mt_new = gs_new.getTraitementDeBase().multiply(ccd.getPors().divide(_prcent));
      }
      
      data.add(ccd_mt_old.setScale(2, RoundingMode.HALF_UP).toString());
      data.add(ccd_mt_new.setScale(2, RoundingMode.HALF_UP).toString());
      data.add(ccd_mt_new.subtract(ccd_mt_old).setScale(2, RoundingMode.HALF_UP).toString());
      ls_retraite.add(ccd_mt_new);
      ls_diff_rappel.add(ccd_mt_new.subtract(ccd_mt_old));
      ra.setMutuelleMgpapCcd_rappel(
              ccd_mt_new
                      .setScale(2, RoundingMode.HALF_UP)
                      .subtract(ccd_mt_old.setScale(2, RoundingMode.HALF_UP)));
      
    } else {
      data.add("MUTUELLE OMFAM C.A.A.D");
      BigDecimal total_3_old = new BigDecimal(0);
      BigDecimal total_3_new = new BigDecimal(0);
      total_3_old = total_3_old.add(gs_old.getTraitementDeBase()).add(resid_old).add(resid_n_old);
      total_3_new = total_3_new.add(gs_new.getTraitementDeBase()).add(resid_new).add(resid_n_new);
      ccd_mt_old = total_3_old.multiply(ccd.getPors().divide(_prcent));
      ccd_mt_new = total_3_new.multiply(ccd.getPors().divide(_prcent));
      if (employee.getId() == 167) {
        ccd_mt_new = BigDecimal.valueOf(40);
        ccd_mt_old = BigDecimal.valueOf(40);
      } else {
        ccd_mt_old =
                ccd_mt_old.compareTo(ccd.getMontant_max()) > 0 ? ccd.getMontant_max() : ccd_mt_old;
        ccd_mt_new =
                ccd_mt_new.compareTo(ccd.getMontant_max()) > 0 ? ccd.getMontant_max() : ccd_mt_new;
      }
      
      data.add(ccd_mt_old.setScale(2, RoundingMode.HALF_UP).toString());
      data.add(ccd_mt_new.setScale(2, RoundingMode.HALF_UP).toString());
      data.add(ccd_mt_new.subtract(ccd_mt_old).setScale(2, RoundingMode.HALF_UP).toString());
      ls_retraite.add(ccd_mt_new);
      ls_diff_rappel.add(ccd_mt_new.subtract(ccd_mt_old));
      ra.setMutuelleOmfamCaad_rappel(
              ccd_mt_new
                      .setScale(2, RoundingMode.HALF_UP)
                      .subtract(ccd_mt_old.setScale(2, RoundingMode.HALF_UP)));
    }
    data.add("IMPOT SUR REVENU");
    BigDecimal total_x_old = new BigDecimal(0);
    BigDecimal total_x_new = new BigDecimal(0);
    BigDecimal tva = new BigDecimal(20);
    BigDecimal mt1 = new BigDecimal(2500);
    total_x_old = total_brute_old.multiply(tva.divide(_prcent));
    total_x_new = total_brute_new.multiply(tva.divide(_prcent));
    if (total_x_old.compareTo(mt1) > 0) {
      total_x_old = mt1;
    }
    if (total_x_new.compareTo(mt1) > 0) {
      total_x_new = mt1;
    }
    BigDecimal some1_old =
            total_brute_old
                    .subtract(total_x_old)
                    .subtract(rcar_mt_old)
                    .subtract(compRcar_mt_old)
                    .subtract(amo_mt_old)
                    .subtract(employee.getMontant_cnss())
                    .subtract(sec_mt_old)
                    .subtract(ccd_mt_old)
                    .subtract(
                            employee.getIsCotisation()
                                    ? employee.getMontantCotisation()
                                    : BigDecimal.valueOf(0));
    BigDecimal some1_new =
            total_brute_new
                    .subtract(total_x_new)
                    .subtract(rcar_mt_new)
                    .subtract(compRcar_mt_new)
                    .subtract(amo_mt_new)
                    .subtract(employee.getMontant_cnss())
                    .subtract(sec_mt_new)
                    .subtract(
                            employee.getIsCotisation()
                                    ? employee.getMontantCotisation()
                                    : BigDecimal.valueOf(0))
                    .subtract(ccd_mt_new);
    BigDecimal ir_old = new BigDecimal(0);
    BigDecimal ir_new = new BigDecimal(0);
    BigDecimal mariage_old = new BigDecimal(0);
    BigDecimal mariage_new = new BigDecimal(0);
    if (employee.getMaritalStatus() != null && employee.getMaritalStatus().equals("1")) {
      String situation_old =
              getSitualtionAtDate(d1_moin1Day.toString(), employee.getMarriageDate());
      String situation_new = getSitualtionAtDate(dateFin, employee.getMarriageDate());
      mariage_old =
              (situation_old.equals("Marié(e)") && employee.getContactPartner().getTitleSelect() == 1)
                      ? BigDecimal.valueOf(1)
                      : BigDecimal.valueOf(0);
      mariage_new =
              (situation_new.equals("Marié(e)") && employee.getContactPartner().getTitleSelect() == 1)
                      ? BigDecimal.valueOf(1)
                      : BigDecimal.valueOf(0);
    } else {
      String situation_old = "Célibataire";
      String situation_new = "Célibataire";
      mariage_old = BigDecimal.valueOf(0);
      mariage_new = BigDecimal.valueOf(0);
    }
    // return tranche by montant
    IR ir_tmp_old = serviceUtil.getIRByMontnat(some1_old);
    int child_old_n = getNombreEnfantAtDate_27(employee, d1_moin1Day);
    
    // verifier si exonerer
    if (ir_tmp_old == null || ir_tmp_old.getExonerer()) {
      ir_old = BigDecimal.valueOf(0);
    } else {
      // si non exonerer calculer IR
      ir_old =
              (some1_old.multiply(ir_tmp_old.getPors().divide(_prcent)))
                      .subtract(ir_tmp_old.getMontant_red())
                      .subtract(BigDecimal.valueOf(30).multiply(new BigDecimal(child_old_n)))
                      .subtract(BigDecimal.valueOf(30).multiply(mariage_old));
    }
    // return tranche by montant
    IR ir_tmp_new = serviceUtil.getIRByMontnat(some1_new);
    int child_new_n = getNombreEnfantAtDate_27(employee, d2);
    // verifier si exonerer
    if (ir_tmp_new == null || ir_tmp_new.getExonerer()) {
      ir_new = BigDecimal.valueOf(0);
    } else {
      // si non exonerer calculer IR
      ir_new =
              (some1_new.multiply(ir_tmp_new.getPors().divide(_prcent)))
                      .subtract(ir_tmp_new.getMontant_red())
                      .subtract(BigDecimal.valueOf(30).multiply(new BigDecimal(child_new_n)))
                      .subtract(BigDecimal.valueOf(30).multiply(mariage_new));
    }
    ir_old = BigDecimal.ZERO.compareTo(ir_old) > 0 ? BigDecimal.ZERO : ir_old;
    ir_new = BigDecimal.ZERO.compareTo(ir_new) > 0 ? BigDecimal.ZERO : ir_new;
    BigDecimal ir_diff = ir_new.subtract(ir_old);
    data.add(ir_old.setScale(0, RoundingMode.HALF_UP).toString());
    data.add(ir_new.setScale(0, RoundingMode.HALF_UP).toString());
    data.add(ir_diff.setScale(0, RoundingMode.HALF_UP).toString());
    ls_retraite.add(ir_new);
    ls_diff_rappel.add(ir_diff);
    ra.setIr_rappel(ir_diff.setScale(0, RoundingMode.HALF_UP));
    data.add("TOT DES RETENUES");
    BigDecimal tot_ret_old = new BigDecimal(0);
    BigDecimal tot_ret_new = new BigDecimal(0);
    BigDecimal css_old = BigDecimal.ZERO;
    BigDecimal css_new = BigDecimal.ZERO;
    css_old = css_old;
    css_new = css_new;
    tot_ret_old =
            rcar_mt_old
                    .add(compRcar_mt_old)
                    .add(amo_mt_old)
                    .add(sec_mt_old)
                    .add(ccd_mt_old)
                    .add(css_old)
                    .add(getcreditAtdate(employee, d1_moin1Day))
                    .add(
                            employee.getIsCotisation()
                                    ? employee.getMontantCotisation()
                                    : BigDecimal.valueOf(0))
                    .add(ir_old);
    tot_ret_new =
            rcar_mt_new
                    .add(compRcar_mt_new)
                    .add(amo_mt_new)
                    .add(sec_mt_new)
                    .add(css_new)
                    .add(getcreditAtdate(employee, LocalDate.parse(dateFin)))
                    .add(
                            employee.getIsCotisation()
                                    ? employee.getMontantCotisation()
                                    : BigDecimal.valueOf(0))
                    .add(ccd_mt_new)
                    .add(ir_new);
    data.add(tot_ret_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(tot_ret_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(
            tot_ret_new
                    .setScale(2, RoundingMode.HALF_UP)
                    .subtract(tot_ret_old.setScale(2, RoundingMode.HALF_UP))
                    .toString());
    ls_retraite.add(tot_ret_new);
    ls_diff_rappel.add(tot_ret_new.subtract(tot_ret_old));
    data.add("NET A ORDONNANCER");
    BigDecimal net_ordo_old = new BigDecimal(0);
    BigDecimal net_ordo_new = new BigDecimal(0);
    net_ordo_old =
            total_brute_old
                    .subtract(tot_ret_old)
                    .subtract(employee.getMontant_regim())
                    .add(BigDecimal.valueOf(child_mt_old))
                    // .add(indem_log_old)
                    .add(indem_foncNet_old.setScale(2, RoundingMode.HALF_UP))
                    .add(indem_voitur_old.setScale(2, RoundingMode.HALF_UP))
                    .add(indem_repres_old.setScale(2, RoundingMode.HALF_UP));
    net_ordo_new =
            total_brute_new
                    .subtract(tot_ret_new)
                    .subtract(employee.getMontant_regim())
                    .add(BigDecimal.valueOf(child_mt_new))
                    .add(indem_foncNet_new.setScale(2, RoundingMode.HALF_UP))
                    .add(indem_voitur_new.setScale(2, RoundingMode.HALF_UP))
                    .add(indem_repres_new.setScale(2, RoundingMode.HALF_UP));
    
    data.add(net_ordo_old.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(net_ordo_new.setScale(2, RoundingMode.HALF_UP).toString());
    data.add(net_ordo_new.subtract(net_ordo_old).setScale(2, RoundingMode.HALF_UP).toString());
    ls_retraite.add(net_ordo_new);
    ls_diff_rappel.add(net_ordo_new.subtract(net_ordo_old));
    ra.setNetAPayer_rappel(
            net_ordo_new
                    .setScale(2, RoundingMode.HALF_UP)
                    .subtract(net_ordo_old.setScale(2, RoundingMode.HALF_UP)));
    data.add("COTISATION RECORE");
    BigDecimal cotisation_emp_old = new BigDecimal(0);
    BigDecimal cotisation_emp_new = new BigDecimal(0);
    BigDecimal credit_emp = new BigDecimal(0);
    boolean hasCotisation1 = employeHasCotisationAtDate(id_emp, d1);
    boolean hasCotisation2 = employeHasCotisationAtDate(id_emp, d2);
    if (hasCotisation1)
      cotisation_emp_old = employee.getMontantCotisation().setScale(2, RoundingMode.HALF_UP);
    if (hasCotisation2)
      cotisation_emp_new = employee.getMontantCotisation().setScale(2, RoundingMode.HALF_UP);
    data.add(cotisation_emp_old.toString());
    data.add(cotisation_emp_new.toString());
    data.add(cotisation_emp_new.subtract(cotisation_emp_old).toString());
    ls_retraite.add(cotisation_emp_new);
    ls_diff_rappel.add(cotisation_emp_new.subtract(cotisation_emp_old));
    data.add("CREDIT");
    if (employer_has_credit(employee)) {
      data.add(credit_mt_old.setScale(2, RoundingMode.HALF_UP).toString());
      data.add(credit_mt_new.setScale(2, RoundingMode.HALF_UP).toString());
      data.add(credit_mt_new.subtract(credit_mt_old).setScale(2, RoundingMode.HALF_UP).toString());
      ls_retraite.add(credit_mt_new);
      ls_diff_rappel.add(credit_mt_new.subtract(credit_mt_old));
      
    } else {
      data.add("0");
      data.add("0");
      data.add("0");
      ls_retraite.add(BigDecimal.ZERO);
      ls_diff_rappel.add(BigDecimal.ZERO);
    }
    List<BigDecimal> ls_rappel = new ArrayList<BigDecimal>();
    for (BigDecimal tmp : ls_retraite) {
      BigDecimal retrait = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
      if (coef.compareTo(BigDecimal.ZERO) > 0
              && tmp.compareTo(BigDecimal.ZERO) > 0
              && mois == 0
              && jour == 0) {
        BigDecimal dat_ = coef.multiply(tmp);
        retrait =
                dat_.divide(
                        new BigDecimal(LocalDate.parse(dateFin).lengthOfMonth()),
                        2,
                        RoundingMode.HALF_UP); // (nbr*sal)/maxjour
      }
      data.add(retrait.toString());
    }
    
    if ((mois > 0 || jour > 0) && rappel.equals("rappel")) {
      if (getNbrAugmentation(id_emp) == 1) { // rappel pour nouv employer
        for (int i = 0; i < ls_retraite.size(); i++) {
          BigDecimal tmp = ls_retraite.get(i);
          BigDecimal rappel_nbr = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
          BigDecimal dat_1 = tmp.multiply(BigDecimal.valueOf(jour));
          if (d1.getDayOfMonth() != 1) {
            int x_jour = LocalDate.parse(dateFin).lengthOfMonth();
            dat_1 = dat_1.divide(new BigDecimal(x_jour), 2, RoundingMode.HALF_UP);
          }
          rappel_nbr = tmp.multiply(BigDecimal.valueOf(mois)).add(dat_1);
          ls_rappel.add(rappel_nbr);
        }
        
      } else if (getNbrAugmentation(id_emp) > 1) { // rappel ci employee est ancien
        for (int i = 0; i < ls_diff_rappel.size(); i++) {
          BigDecimal tmp = ls_diff_rappel.get(i);
          BigDecimal rappel_nbr = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
          BigDecimal dat_1 = tmp.multiply(BigDecimal.valueOf(jour));
          if (d1.getDayOfMonth() != 1) {
            dat_1 =
                    dat_1.divide(
                            new BigDecimal(LocalDate.parse(dateDebut).lengthOfMonth()),
                            2,
                            RoundingMode.HALF_UP);
          } else {
            dat_1 =
                    dat_1.divide(
                            new BigDecimal(LocalDate.parse(dateFin).lengthOfMonth()),
                            2,
                            RoundingMode.HALF_UP);
          }
          rappel_nbr = tmp.multiply(BigDecimal.valueOf(mois)).add(dat_1);
          ls_rappel.add(rappel_nbr);
        }
      }
      Augmentation aa = getEmployer_has_Augmentation(id_emp);
      EtatDesRappelSurSalaire etat = new EtatDesRappelSurSalaire();
      etat.setCin(employee.getCin());
      etat.setGrade(aa.getGrade());
      etat.setPeriode(
              "Du "
                      + aa.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                      + " au "
                      + d2.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
      etat.setMatriculeInterne(employee.getMatriculationInterne());
      etat.setNomEtPrenom(employee.getContactPartner().getSimpleFullName());
      etat.setImmatriculationCnops(employee.getImmatriculationSM());
      etat.setDate_debut(d1);
      etat.setDate_fin(d2);
      etat.setId_employer(employee.getId());
      if (aa.getAvtRecalss() != null) {
        String avt =
                aa.getAvtRecalss().equals("Avt")
                        ? "Avt d'échelon"
                        : (aa.getAvtRecalss().equals("Reclassement") ? "Reclasst d'échelle" : "");
        etat.setRappleRegularisation(avt);
      }
      etat.setAffiliation_mutuelle(employee.getAffiliationSM());
      saveEtatDesRappelSurSalaire(etat, ls_rappel, employee.getOrganismeMetuelle2().getName());
      
    } else {
      for (BigDecimal tmp : ls_retraite) {
        ls_rappel.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
      }
    }
    for (int i = 0; i < ls_rappel.size(); i++) {
      data.add(ls_rappel.get(i).toString());
    }
    
    data.add("Indemnité de résidence de base");
    BigDecimal tmp1 = null;
    BigDecimal rappel_nbr1 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    if (employee.getZoneEmployee().getId() == 3
            || employee.getZoneEmployee().getId() == 2
            || employee.getZoneEmployee().getId() == 1
            || employee.getZoneEmployee().getId() == 4)
      tmp1 =
              gs_new
                      .getTraitementDeBase()
                      .subtract(gs_old.getTraitementDeBase())
                      .multiply(BigDecimal.valueOf(0.10));
    BigDecimal dat_1 = tmp1.multiply(BigDecimal.valueOf(jour));
    if (d1.getDayOfMonth() != 1) {
      int x_jour = LocalDate.parse(dateFin).lengthOfMonth();
      dat_1 = dat_1.divide(new BigDecimal(x_jour), 2, RoundingMode.HALF_UP);
    }
    rappel_nbr1 = tmp1.multiply(BigDecimal.valueOf(mois)).add(dat_1);
    ls_rappel.add(rappel_nbr1);
    
    data.add(resid_n_old.toString());
    data.add(resid_n_new.toString());
    ra.setIndemniteDeResidence_rappel(
            resid_n_new
                    .setScale(2, RoundingMode.HALF_UP)
                    .subtract(resid_n_old.setScale(2, RoundingMode.HALF_UP)));
    
    data.add("TOTAL BRUT ");
    data.add(
            total_brute_old
                    .add(new BigDecimal(child_mt_old))
                    // .add(indem_log_old)
                    .add(indem_foncNet_old)
                    .add(indem_voitur_old)
                    .add(indem_repres_old)
                    .add(indem_log_old)
                    .add(employee.getMontant_prime())
                    .toString());
    data.add(
            total_brute_new
                    .add(new BigDecimal(child_mt_new))
                    // .add(indem_log_new)
                    .add(indem_foncNet_new)
                    .add(indem_voitur_new)
                    .add(indem_repres_new)
                    .add(indem_log_new)
                    .toString());
    
    data.add(String.valueOf(rappel_nbr1));
    ra.setTotalSalairMensuelBrutImposable_rappel(
            (total_brute_new
                    .add(new BigDecimal(child_mt_new))
                    .add(indem_foncNet_new)
                    .add(indem_voitur_new)
                    .add(indem_repres_new))
                    .subtract(
                            total_brute_old
                                    .add(new BigDecimal(child_mt_old))
                                    .add(indem_foncNet_old)
                                    .add(indem_voitur_old)
                                    .add(indem_repres_old)));
    ra.setEmploye(employee);
    ra.setMois((int) mois);
    rappelEmployeRepository.save(ra);
    
    String x = StringUtils.join(data, "__");
    return x;
  }
  
  public BigDecimal calcule_indemnite(
          int typeIndem, ResponsabiliteList oldResp, ResponsabiliteList newResp, LocalDate dateFin) {
    BigDecimal v1, v2;
    BigDecimal indem1, indem2;
    indem1 = indem2 = BigDecimal.ZERO;
    switch (typeIndem) {
      // 1== logement
      case 1:
        indem1 = oldResp.getResponsabilite_select().getIndemnitLogement();
        indem2 = newResp.getResponsabilite_select().getIndemnitLogement();
        break;
      // 2==Fonction
      case 2:
        indem1 = oldResp.getResponsabilite_select().getIndemnitFonction();
        indem2 = newResp.getResponsabilite_select().getIndemnitFonction();
        break;
      case 4: // 3== representation
        indem1 = oldResp.getResponsabilite_select().getIndemnitRepresentation();
        indem2 = newResp.getResponsabilite_select().getIndemnitRepresentation();
        break;
      case 3: // voiture
        indem1 = oldResp.getResponsabilite_select().getIndemnitVoiture();
        indem2 = newResp.getResponsabilite_select().getIndemnitVoiture();
        break;
      default:
        indem1 = BigDecimal.ZERO;
        indem2 = BigDecimal.ZERO;
    }
    
    v1 = v2 = BigDecimal.ZERO;
    int days_v1 = (oldResp.getDate_fin().getDayOfMonth());
    int days_v2 = (dateFin.getDayOfMonth()) - (newResp.getDate_debut().getDayOfMonth());
    v1 =
            BigDecimal.valueOf(days_v1)
                    .multiply(indem1)
                    .divide(
                            BigDecimal.valueOf(
                                    dateFin
                                            .withDayOfMonth(dateFin.getMonth().length(dateFin.isLeapYear()))
                                            .getDayOfMonth()),
                            2,
                            RoundingMode.HALF_UP);
    v2 =
            BigDecimal.valueOf(days_v2)
                    .multiply(indem2)
                    .divide(
                            BigDecimal.valueOf(
                                    dateFin
                                            .withDayOfMonth(dateFin.getMonth().length(dateFin.isLeapYear()))
                                            .getDayOfMonth()),
                            2,
                            RoundingMode.HALF_UP);
    return v1.add(v2);
  }
  
  public ResponsabiliteList getResponsabiliteAtDate(Employee employee, LocalDate finalD1_moin1Day) {
    ResponsabiliteList result = null;
    for (ResponsabiliteList tmp : employee.getResponsabiliteList()) {
      if (tmp.getDate_debut() != null
              && tmp.getDate_fin() != null
              && finalD1_moin1Day.compareTo(tmp.getDate_debut()) >= 0
              && tmp.getDate_fin().compareTo(finalD1_moin1Day) >= 0) {
        result = tmp;
      }
    }
    if (result == null)
      for (ResponsabiliteList tmp : employee.getResponsabiliteList()) {
        if (tmp.getDate_debut() != null
                && tmp.getDate_fin() == null
                && finalD1_moin1Day.compareTo(tmp.getDate_debut()) >= 0) {
          result = tmp;
        }
      }
    return result;
  }
  
  @Transactional
  private void saveEtatDesRappelSurSalaire(
          EtatDesRappelSurSalaire etat, List<BigDecimal> ls_rappel, String mgpap) {
    etat.setBrut(ls_rappel.get(10));
    etat.setRcar(ls_rappel.get(11));
    etat.setC_rcar(ls_rappel.get(12));
    etat.setAmo(ls_rappel.get(13));
    if (mgpap.equals("MGPAP")) {
      etat.setMgpap_sm(ls_rappel.get(14));
      etat.setMgpap_ccd(ls_rappel.get(15));
    } else if (mgpap.equals("OMFAM")) {
      etat.setOmfam_sm(ls_rappel.get(14));
      etat.setOmfam_caad(ls_rappel.get(15));
    }
    etat.setIr(ls_rappel.get(16));
    etat.setTot_retenues(ls_rappel.get(17));
    etat.setNet_payer(ls_rappel.get(18));
    etat.setRecore(ls_rappel.get(19));
    etatDesRappelSurSalaireRepository.save(etat);
  }
  
  public int getNbrAugmentation(Long id_emp) {
    return augmentationRepository
            .all()
            .filter("self.employer.id=?1 and self.etat!='En attente'", id_emp)
            .fetch()
            .size();
  }
  
  private boolean employer_has_credit(Employee employee) {
    boolean res = false;
    List<GestionCredit> ls =
            gestionCreditRepository.all().filter("self.employee=?1", employee).fetch();
    if (ls != null && ls.size() > 0) res = true;
    return res;
  }
  
  private boolean employer_has_aos(Employee employee) {
    boolean res = false;
    List<Aos> ls = aosRepository.all().filter("self.employee=?1", employee.getId()).fetch();
    if (ls != null && ls.size() > 0) res = true;
    return res;
  }
  
  private BigDecimal getcreditAtdate(Employee employee, LocalDate date) {
    BigDecimal res = new BigDecimal(0);
    if (employee.getEmploymentGestionCreditList() != null
            && employee.getEmploymentGestionCreditList().size() > 0) {
      List<GestionCredit> gestionCredit =
              gestionCreditRepository
                      .all()
                      .filter("self.employee=?1 and date_debut<=?2 and date_fin>=?2 ", employee, date)
                      .fetch();
      if (gestionCredit != null && gestionCredit.size() > 0) {
        for (GestionCredit tmp : gestionCredit) {
          res = res.add(tmp.getRemboursement());
        }
      }
    }
    return res;
  }
  
  private BigDecimal getcreditAosAtdate(Employee employee, LocalDate date) {
    BigDecimal res = new BigDecimal(0);
    if (employee.getAos() != null && employee.getAos().size() > 0) {
      List<Aos> aos =
              aosRepository
                      .all()
                      .filter("self.employee=:emp and :dd between self.dateDebut and self.dateFin")
                      .bind("emp", employee)
                      .bind("dd", date)
                      .fetch();
      if (aos != null && aos.size() > 0) {
        for (Aos tmp : aos) {
          res = res.add(tmp.getRemboursement());
        }
      }
    }
    return res;
  }
  
  private void setreponsabiliteNull(
          List<String> data, List<BigDecimal> ls_retraite, List<BigDecimal> ls_rappel) {
    String[] tab = {
            "Indemnité de logement",
            "Indemnité de fonction nette",
            "IND.RESPONSABILITE",
            "Indemnité de représentation"
    };
    for (int i = 0; i < tab.length; i++) {
      data.add(tab[i]);
      data.add("0");
      data.add("0");
      data.add("0");
      ls_retraite.add(BigDecimal.ZERO);
      ls_rappel.add(BigDecimal.ZERO);
    }
  }
  
  public boolean verifier_date_debut(Long id_emp, String date_debut) {
    Augmentation one =
            augmentationRepository
                    .all()
                    .filter("self.employer=?1 and date_debut<=?2", id_emp, LocalDate.parse(date_debut))
                    .order("createdOn")
                    .fetchOne();
    boolean res = one == null;
    return res;
  }
  
  public String getDateDebutAugmentationByEmployer(Long id_emp) {
    String date;
    Augmentation one =
            augmentationRepository
                    .all()
                    .filter("self.employer=?1", id_emp)
                    .order("createdOn")
                    .fetchOne();
    if (one == null) {
      Employee emp = employeeRepository.find(id_emp);
      return emp.getDaterecrutement().toString();
    }
    return one.getDateDebut().getDayOfMonth()
            + "/"
            + ((one.getDateDebut().getMonthValue() > 9)
            ? one.getDateDebut().getMonthValue()
            : ("0" + one.getDateDebut().getMonthValue()))
            + "/"
            + one.getDateDebut().getYear();
  }
  
  public Augmentation getEmployer_has_Augmentation(Long id_emp) {
    Augmentation h =
            augmentationRepository
                    .all()
                    .filter("self.employer=?1 and self.etat=?2 and self.hasRappel=true", id_emp, "En cours")
                    .fetchOne();
    return h;
  }
  
  @Transactional
  public void update_rappel(Long id_emp, LocalDate date_fin) {
    Augmentation h = getEmployer_has_Augmentation(id_emp);
    h.setHasRappel(false);
    h.setDateRappel(date_fin);
    augmentationRepository.save(h);
  }
  
  public int getNbrEtatRappel(LocalDate d1, LocalDate d2) {
    int x =
            etatDesRappelSurSalaireRepository
                    .all()
                    .filter("self.date_debut>=?1 and self.date_fin<=?2", d1, d2)
                    .fetch()
                    .size();
    return x;
  }
  
  public BigDecimal getTotalByField(LocalDate d1, LocalDate d2, String param) {
    BigDecimal x = BigDecimal.ZERO;
    String req =
            "select sum(s."
                    + param
                    + ") from hr_etat_des_rappel_sur_salaire s where s.date_debut>='"
                    + java.sql.Date.valueOf(d1)
                    + "' and s.date_fin<='"
                    + java.sql.Date.valueOf(d2)
                    + "'";
    x = RunSqlRequestForMe.runSqlRequest_Bigdecimal(req);
    return x;
  }
  
  public BigDecimal getTotalByField(LocalDate d1, LocalDate d2, String param, String Field_toShow) {
    BigDecimal x = BigDecimal.ZERO;
    String req =
            "select sum(s."
                    + param
                    + ") from hr_etat_des_rappel_sur_salaire s where "
                    + Field_toShow
                    + "= true and cast(s.created_on as date) >= cast( '"
                    + d1
                    + "' as date)  and cast(s.created_on as date) <= cast('"
                    + d2
                    + "' as date);";
    x = RunSqlRequestForMe.runSqlRequest_Bigdecimal(req);
    x = x == null ? BigDecimal.ZERO : x;
    return x;
  }
  
  public BigDecimal getMontayoub(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=true", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getRcarRg());
    }
    return b;
  }
  
  public BigDecimal getMontayoub1(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=false", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getRcarRg());
    }
    return b;
  }
  
  public BigDecimal getrcarRComp(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=true", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getRcarRComp());
    }
    return b;
  }
  
  public BigDecimal getrcarRComp1(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=false", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getRcarRComp());
    }
    return b;
  }
  
  public BigDecimal getrecore(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=true", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getRecore());
    }
    return b;
  }
  
  public BigDecimal getrecore1(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=false", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getRecore());
    }
    return b;
  }
  
  public BigDecimal getamo(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=true", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getAmo());
    }
    return b;
  }
  
  public BigDecimal getamo1(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=false", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getAmo());
    }
    return b;
  }
  
  public BigDecimal getmutuelleMgpapCcd(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=true", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getMutuelleMgpapCcd());
    }
    return b;
  }
  
  public BigDecimal getmutuelleMgpapCcd1(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=false", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getMutuelleMgpapCcd());
    }
    return b;
  }
  
  public BigDecimal getmutuelleOmfamCaad(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=true", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getMutuelleOmfamCaad());
    }
    return b;
  }
  
  public BigDecimal getmutuelleOmfamCaad1(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=false", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getMutuelleOmfamCaad());
    }
    return b;
  }
  
  public BigDecimal getIR(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=true", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getiR());
    }
    return b;
  }
  
  public BigDecimal getIR1(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=false", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getiR());
    }
    return b;
  }
  
  public BigDecimal getpretAxaCredit(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=true", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getPretAxaCredit());
    }
    return b;
  }
  
  public BigDecimal getpretAxaCredit1(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=false", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getPretAxaCredit());
    }
    return b;
  }
  
  public BigDecimal getcss(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=true", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getCss());
    }
    return b;
  }
  
  public BigDecimal getcss1(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=false", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getCss());
    }
    return b;
  }
  
  public BigDecimal gettotalRetenue(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=true", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getTotalRetenue());
    }
    return b;
  }
  
  public BigDecimal gettotalRetenue1(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=false", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getTotalRetenue());
    }
    return b;
  }
  
  public List<EtatSalaire> getSalairePlafonne(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository.all().filter("self.annee=?1 and self.mois=?2", annee, mois).fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getSalairePlafonne());
    }
    return t;
  }
  
  @Transactional
  public EtatSalaire saveEtatSalaire(EtatSalaire etatSalaire) {
    return etatSalaireRepository.save(etatSalaire);
  }
  
  public BigDecimal getsumNetAPayer(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=true", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getNetAPayer());
    }
    return b;
  }
  
  public BigDecimal getsumNetAPayer1(int mois, int annee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.mois=?2 and self.titulaire=false", annee, mois)
                    .fetch();
    BigDecimal b = BigDecimal.ZERO;
    for (EtatSalaire tmp : t) {
      b.add(tmp.getNetAPayer());
    }
    return b;
  }
  
  public BigDecimal getnetor(BigDecimal mt10, BigDecimal mt9) {
    BigDecimal tab1 = mt9;
    BigDecimal tab2 = mt10;
    BigDecimal x = tab1;
    BigDecimal z = tab2;
    BigDecimal w = tab2.subtract(tab1);
    return w;
  }
  
  public BigDecimal getnetor1(BigDecimal mt10, BigDecimal mt9) {
    BigDecimal tab1 = mt9;
    BigDecimal tab2 = mt10;
    BigDecimal x = tab1;
    BigDecimal z = tab2;
    BigDecimal w = tab2.subtract(tab1);
    return w;
  }
  
  boolean employeHasCotisationAtDate(Long idemp, LocalDate d) {
    boolean res = false;
    Employee emp = employeeRepository.find(idemp);
    if (emp.getIsCotisation()
            && emp.getDateDebutCotisation() != null
            && d.compareTo(emp.getDateDebutCotisation()) >= 0) return true;
    return res;
  }
  
  public Aos employeHasAOSAtDate(Long idemp, LocalDate d1) {
    Aos ls =
            aosRepository
                    .all()
                    .filter(":d1 between self.dateDebut and self.dateFin and self.employee.id=:emp")
                    .bind("d1", d1)
                    .bind("emp", idemp)
                    .fetchOne();
    return ls;
  }
  
  public boolean employeHasProlongationAtDate(Long idemp, LocalDate d) {
    boolean res = false;
    Employee emp = employeeRepository.find(idemp);
    if (emp.getIs_prolongation()
            && emp.getDateProlongation() != null
            && d.compareTo(emp.getDateProlongation()) >= 0) return true;
    return res;
  }
  
  public boolean employeHasPrimeAtDate(Long idemp, LocalDate d) {
    boolean res = false;
    Employee emp = employeeRepository.find(idemp);
    if (emp.getIs_prime() && emp.getDate_prime() != null && d.compareTo(emp.getDate_prime()) >= 0)
      return true;
    return res;
  }
  
  public String getRibMutuelle(int id_mutuelle, int annee) {
    String req =
            "SELECT concat(x.nam_bank,' N° ' ,x.rib ) from configuration_mutuelle x, configuration_lestypes v where x.annee="
                    + annee
                    + " and v.id="
                    + id_mutuelle
                    + " and x.mutuelle=v.id;";
    Object rib = RunSqlRequestForMe.runSqlRequest_Object(req);
    return rib == null ? "" : rib.toString();
  }
  
  public List<EtatDesRappelSurSalaire> getAllEtatBetweenDate(LocalDate d1, LocalDate d2) {
    List<EtatDesRappelSurSalaire> ls =
            etatDesRappelSurSalaireRepository
                    .all()
                    .filter("self.date_debut<=? and self.date_fin>=?", d1, d2)
                    .fetch();
    return ls;
  }
  
  public int getNbrEtatSalaire(Integer mois, Integer annee) {
    int x =
            etatSalaireRepository
                    .all()
                    .filter("self.mois>=?1 and self.annee<=?2", mois, annee)
                    .fetch()
                    .size();
    return x;
  }
  
  @Transactional
  public void Update_etatSow_true(LocalDate d1, LocalDate d2, String champ) {
    String req =
            "UPDATE hr_etat_des_rappel_sur_salaire SET "
                    + champ
                    + "=true where cast( created_on as date ) >= cast( '"
                    + d1
                    + "' as date) and cast( created_on as date) <= cast( '"
                    + d2
                    + "' as date);";
    RunSqlRequestForMe.runSqlRequest(req);
  }
  
  public String getTitre(ActionRequest request) {
    if (request.getContext().get("_signal") != null) {
      return request.getContext().get("_signal").toString().replace("_", " ");
    } else {
      return request.getData().get("_signal").toString().replace("_", " ");
    }
  }
  
  public String get_ids_omfam(String omfam_mgpap) {
    int id = omfam_mgpap.equals("mgpap_sm") ? 1 : 2;
    String req =
            "select hr_employee.id from hr_employee where hr_employee.organisme_metuelle2=" + id;
    String res = "";
    List<Object> tab = RunSqlRequestForMe.runSqlRequest_ObjectList(req);
    for (Object tmp : tab) {
      res += tmp.toString() + ",";
    }
    res = res.substring(0, res.length() - 1);
    return res;
  }
  
  @Transactional
  public void deleteByQuery(String[] strings) {
    for (String req : strings) {
      if (!req.isEmpty()) RunSqlRequestForMe.runSqlRequest(req);
    }
  }
  
  @Transactional
  public void saveOrderVirement(OrdreVirement ordreVirement) {
    ordreVirementRepository.save(ordreVirement);
  }
  
  private String mois_format(int month) {
    String res = month < 10 ? ("0" + month) : String.valueOf(month);
    return res;
  }
  
  public int[] getNombreDayRestByEmp(Long id_emp, LocalDate datecheck) {
    int autorisation = 10, res = 22;
    int ols_autorisation = 0, old_res = 0;
    Employee emp = employeeRepository.find(id_emp);
    List<Conge> ls =
            congeRepository
                    .all()
                    .filter("self.employee.id = ?1 and self.annee= ?2", id_emp, datecheck.getYear())
                    .fetch();
    for (Conge tmp : ls) {
      if (tmp.getTypeGeneraleConge() != null && tmp.getTypeGeneraleConge().equals("conge"))
        res = res - tmp.getDuree();
      else if (tmp.getTypeGeneraleConge() != null
              && tmp.getTypeGeneraleConge().equals("autorisation"))
        autorisation = autorisation - tmp.getDuree();
    }
    LocalDate old = datecheck.minusYears(1);
    if (old.getYear() > emp.getDaterecrutement().getYear()) {
      old_res = 22;
      List<Conge> ls_old =
              congeRepository
                      .all()
                      .filter("self.employee.id = ?1 and self.annee= ?2", id_emp, old.getYear())
                      .fetch();
      for (Conge tmp : ls) {
        if (tmp.getTypeGeneraleConge() != null && tmp.getTypeGeneraleConge().equals("conge"))
          old_res = old_res - tmp.getDuree();
        else if (tmp.getTypeGeneraleConge() != null
                && tmp.getTypeGeneraleConge().equals("autorisation")
                && tmp.getTypeConge().getIsRetranchable10Jours())
          ols_autorisation = ols_autorisation - tmp.getDuree();
      }
    }
    return new int[]{(old_res + res), (ols_autorisation + autorisation)};
  }
  
  public List<JoursFeries> getJourFerierInDate(LocalDate d1, LocalDate d2) {
    List<JoursFeries> ls =
            joursFeriesRepository
                    .all()
                    .filter(
                            " (self.datefin <=:d2 AND self.datefin >=:d1) OR (self.datedebut <=:d2 AND self.datedebut >=:d1) ")
                    .bind("d1", d1)
                    .bind("d2", d2)
                    .fetch();
    return ls;
  }
  
  public int calculateNbrDayOff(LocalDate d1, LocalDate d2, JoursFeries j) {
    int res = 0;
    if (j.getDatedebut().compareTo(d1) > 0) {
      if (j.getDatefin().compareTo(d2) > 0) {
        // date debut = date ferier
        // date limite = datefin conger
        res = ServiceUtil.getnombreJourWork(j.getDatedebut(), d2);
        
      } else {
        // date debut = date ferier
        // date limite = datefin ferier
        res = ServiceUtil.getnombreJourWork(j.getDatedebut(), j.getDatefin());
      }
    } else {
      if (j.getDatefin().compareTo(d2) > 0) {
        // date debut = date conge
        // date limite = datefin conger
        res = ServiceUtil.getnombreJourWork(d1, d2);
      } else {
        // date debut = date conge
        // date limite = datefin ferier
        res = ServiceUtil.getnombreJourWork(d1, j.getDatefin());
      }
    }
    return res;
  }
  
  public int getnbrDayWork(LocalDate d1, LocalDate d2) {
    return ServiceUtil.getnombreJourWork(d1, d2);
  }
  
  public List<Conge> getListCongeByIdEmployer(Long id) {
    return congeRepository.all().filter("self.employee=?", id).fetch();
  }
  
  @Transactional
  public void saveConge(Conge conge) {
    congeRepository.save(conge);
  }
  
  @Transactional
  public void deleteConge(Long id) {
    Conge c = congeRepository.find(id);
    congeRepository.remove(c);
  }
  
  public Conge getCongeById(Long id) {
    return congeRepository.find(id);
  }
  
  @Transactional
  public void addHistoriqueResponsabilite(Long id_employee) {
    Employee emp = employeeRepository.find(id_employee);
    HistoriqueResponsabilite h = new HistoriqueResponsabilite();
    h.setResponsabilite(emp.getResponsabilite());
    h.setEmployee(emp);
    h.setDateDebut(LocalDate.now());
    h.setEtat("En cours");
    historiqueResponsabiliteRepository.save(h);
  }
  
  public boolean checkNeedAddHitorique(Long id_employee) {
    boolean res = false;
    Employee employee = employeeRepository.find(id_employee);
    List<HistoriqueResponsabilite> ls =
            historiqueResponsabiliteRepository.all().filter("self.employee.id=?1", id_employee).fetch();
    if (ls == null || ls.size() == 0) {
      return true;
    } else if (employee.getResponsabilite() != null) {
      Responsabilite r = responsabiliteRepository.find(employee.getResponsabilite().getId());
      Responsabilite tmp =
              responsabiliteRepository.find(ls.get(ls.size() - 1).getResponsabilite().getId());
      if (r.getId() != tmp.getId()) res = true;
    }
    return res;
  }
  
  @Transactional
  public void updateResponsabilite(Long id_employee) {
    Employee employee = employeeRepository.find(id_employee);
    List<HistoriqueResponsabilite> ls =
            historiqueResponsabiliteRepository.all().filter("self.employee=?1", employee).fetch();
    if (ls != null && ls.size() > 0) {
      HistoriqueResponsabilite h =
              historiqueResponsabiliteRepository.find(ls.get(ls.size() - 1).getId());
      h.setEtat("Ancienne");
      h.setDateFin(LocalDate.now());
      historiqueResponsabiliteRepository.save(h);
    }
  }
  
  public boolean isFirstRappel(Long id_employee) {
    List<Augmentation> ls =
            augmentationRepository.all().filter("self.employer=?1", id_employee).fetch();
    return ls.size() == 1;
  }
  
  public boolean employerHasWordAtDate(Long id_employee, LocalDate date) {
    List<Augmentation> ls =
            augmentationRepository
                    .all()
                    .filter("self.employer=?1 and self.dateDebut<=?2", id_employee, date)
                    .fetch();
    return ls != null && ls.size() > 0;
  }
  
  public boolean checkDateDebutIsAfterDateStart(String date_debut, Long id_emp) {
    Employee employee = employeeRepository.find(id_emp);
    LocalDate date = LocalDate.parse(date_debut);
    date = date.minusDays(1);
    return employee.getDaterecrutement().compareTo(date) <= 0;
  }
  
  public Employee getEmployerById(Long id_emp) {
    return employeeRepository.find(id_emp);
  }
  
  public Augmentation getAugmentationByEmployee(Long id_emp) {
    return augmentationRepository.all().filter("self.employer=?1", id_emp).fetchOne();
  }
  
  public Augmentation getAugmentationByEtat(Long id_emp, String etat) {
    return augmentationRepository
            .all()
            .filter("self.employer=?1 and self.etat=?2", id_emp, etat)
            .fetchOne();
  }
  
  @Transactional
  public Augmentation saveAugmentation(Augmentation augmentationEncours) {
    return augmentationRepository.save(augmentationEncours);
  }
  
  @Transactional
  public Employee saveEmployee(Employee emp) {
    return employeeRepository.save(emp);
  }
  
  @Transactional
  public void removeAugmentationByEmployer(Long id) {
    List<Augmentation> ls = augmentationRepository.all().filter("self.employer=?1", id).fetch();
    for (Augmentation tmp : ls) {
      augmentationRepository.remove(tmp);
    }
  }
  
  public BigDecimal getTotalGenerale(Integer mois, Integer annee) {
    BigDecimal total1 =
            RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                    "SELECT sum(salaire_net617111) + sum(rcar_rg)+sum(rcarrcomp) + sum(recore)  + sum(amo) + sum(mutuelle_mgpapsm)+sum(mutuelle_mgpap_ccd) + sum(mutuelle_omfam_sm)+sum(mutuelle_omfam_caad) + sum(ir)  + sum(pret_axa_credit) + sum(indemnit_fonction_net)  + sum(indemnit_voiture_net) + sum(indemnit_represent_net) as total from hr_etat_salaire where titulaire=true and is_disposition=false and mois = "
                            + mois
                            + " and annee = "
                            + annee
                            + ";");
    BigDecimal total2 =
            RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                    "SELECT sum(y.salaire_net617111) + sum(y.amo)+ sum(y.mutuelle_mgpapsm)+sum(y.mutuelle_mgpap_ccd) + sum(y.ir) as total from hr_etat_salaire y where y.titulaire=false and y.is_disposition=false and y.mois = "
                            + mois
                            + " and y.annee = "
                            + annee
                            + ";");
    BigDecimal total3 =
            RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                    "SELECT sum(x.indemnite_logement_net617131) + sum(x.rcar_comp617131) + sum(x.ir617131) + sum(rcar_rg)*2 + sum(rcarrcomp) + sum(rcar_comp617131) + sum(amo) as total from hr_etat_salaire x where x.is_disposition=false and x.mois = "
                            + mois
                            + " and x.annee = "
                            + annee
                            + ";");
    total1 = total1 == null ? BigDecimal.ZERO : total1;
    return total1
            .add(total2 == null ? BigDecimal.ZERO : total2)
            .add(total3 == null ? BigDecimal.ZERO : total3);
  }
  
  public BigDecimal getIrSurLogement(Integer mois, Integer annee) {
    return RunSqlRequestForMe.runSqlRequest_Bigdecimal(
            "select sum(es.indemnite_logement_net617131)+sum(es.ir617131)+sum(es.rcar_comp617131) from hr_etat_salaire es where es.titulaire=true and es.mois="
                    + mois
                    + " and es.annee="
                    + annee
                    + " group by es.annee, es.mois;");
  }
  
  public BigDecimal getTotalAmo(Integer mois, Integer annee) {
    BigDecimal d =
            RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                    "SELECT sum(x.amo) from hr_etat_salaire x where mois = "
                            + mois
                            + " and annee = "
                            + annee
                            + " and is_disposition = false;");
    return d.setScale(2, RoundingMode.HALF_UP);
  }
  
  public BigDecimal getTotalMtTIT(int mois, int annee) {
    BigDecimal mt1 =
            RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                    "SELECT sum(allocationfamil) + sum(total_mensuel_brut617111) as mt1 from hr_etat_salaire where mois = "
                            + mois
                            + " and annee = "
                            + annee
                            + " and titulaire=true and is_disposition=false;");
    BigDecimal mt2 =
            RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                    "SELECT sum(total_retenue) as retenu from hr_etat_salaire where mois = "
                            + mois
                            + " and annee = "
                            + annee
                            + " and titulaire=true and is_disposition=false;");
    return mt1.subtract(mt2).setScale(2, RoundingMode.HALF_UP);
  }
  
  public List<Compte> getallCompte() {
    return compteRepository.all().fetch();
  }
  
  public BigDecimal getMontantOrdreVirementAgent(int page, int mois, int annee) {
    Double mt = 0d;
    List<EtatSalaire> ls =
            etatSalaireRepository
                    .all()
                    .filter(
                            "self.isDisposition=false and self.titulaire=true and mois = ?1 and annee = ?2 ",
                            mois,
                            annee)
                    .order("imatriculation")
                    .fetch();
    if (page == 0)
      mt =
              etatSalaireRepository.all()
                      .filter(
                              "self.isDisposition=false and self.titulaire=true and mois = ?1 and annee = ?2 ",
                              mois,
                              annee)
                      .order("imatriculation").fetch(32).stream()
                      .mapToDouble(s -> s.getNetAPayer().doubleValue())
                      .sum();
    else if (page == 1) {
      mt =
              etatSalaireRepository.all()
                      .filter(
                              "self.isDisposition=false and self.titulaire=true and mois = ?1 and annee = ?2 ",
                              mois,
                              annee)
                      .order("imatriculation").fetch(40, 32).stream()
                      .mapToDouble(s -> s.getNetAPayer().doubleValue())
                      .sum();
      mt +=
              etatSalaireRepository.all()
                      .filter(
                              "self.isDisposition=false and self.titulaire=false  and mois = ?1 and annee = ?2 ",
                              mois,
                              annee)
                      .fetch().stream()
                      .mapToDouble(s -> s.getNetAPayer().doubleValue())
                      .sum();
    }
    return new BigDecimal(mt);
  }
  
  public Augmentation getFirstAugmentation(Long id_employer) {
    return augmentationRepository
            .all()
            .filter("self.employer=:idemp")
            .bind("idemp", id_employer)
            .order("id")
            .fetchOne();
  }
  
  public LocalDate addDayWeekensDayOff(LocalDate d1, LocalDate d2) {
    LocalDate date_result = d2;
    LocalDate date_increment = d1;
    int compteur = 0;
    do {
      compteur++;
      date_increment = d1.plusDays(compteur);
      if (date_increment.getDayOfWeek() != DayOfWeek.SUNDAY
              && date_increment.getDayOfWeek() != DayOfWeek.SATURDAY) {
        JoursFeries j =
                joursFeriesRepository
                        .all()
                        .filter(" (self.datedebut <=:d1 AND self.datefin >=:d1)")
                        .bind("d1", date_increment)
                        .fetchOne();
        if (j != null && d2.compareTo(date_increment) >= 0) {
          d2 = d2.plusDays(1);
          d2 =
                  d2.getDayOfWeek() == DayOfWeek.SATURDAY
                          ? d2.plusDays(2)
                          : (d2.getDayOfWeek() == DayOfWeek.SUNDAY ? d2.plusDays(1) : d2);
        }
      }
      
    } while (d2.compareTo(date_increment) >= 0);
    return d2;
  }
  
  @Transactional
  public void saveGrade(Grade g) {
    gradeRepository.save(g);
  }
  
  @Transactional
  public void deleteCreateemployerAvancement(int i) {
    String req_delete = "delete  from hr_avancement_employer";
    RunSqlRequestForMe.runSqlRequest(req_delete);
    String field = "av.nbr_mois_rapide";
    if (i == 2) {
      field = "av.nbr_mois_moyen";
    } else if (i == 3) {
      field = "av.nbr_mois_long";
    }
    String req_create =
            "INSERT into hr_avancement_employer (hr_avancement,employer) select "
                    + i
                    + " ,emp.id from configuration_echelon ech join configuration_avancement_periode av on ech.name = av.echelon_start join hr_employee emp on emp.echelon=ech.id join configuration_echelle ell on emp.echelle = ell.id join hr_augmentation aug on aug.employer = emp.id where (ell.name != 'HE' and aug.etat = 'En cours' and date_part ('year',(cast(aug.date_debut as date) + cast(concat("
                    + field
                    + " , ' month') as interval))) <= date_part('year', CURRENT_DATE)) or (ell.name = 'HE' and aug.etat = 'En cours' and date_part ('year',(cast(aug.date_debut as date) + cast(concat(3 , ' year') as interval))) <= date_part('year', CURRENT_DATE)) ;";
    RunSqlRequestForMe.runSqlRequest(req_create);
  }
  
  public BigDecimal calcule_indemnite_middelDate(
          int typeIndem, ResponsabiliteList newResp, LocalDate dateFin) {
    BigDecimal indem1 = BigDecimal.ZERO;
    BigDecimal result = BigDecimal.ZERO;
    switch (typeIndem) {
      // 1== logement
      case 1:
        indem1 = newResp.getResponsabilite_select().getIndemnitLogement();
        break;
      // 2==Fonction
      case 2:
        indem1 = newResp.getResponsabilite_select().getIndemnitFonction();
        break;
      case 4: // 3== representation
        indem1 = newResp.getResponsabilite_select().getIndemnitRepresentation();
        break;
      case 3: // voiture
        indem1 = newResp.getResponsabilite_select().getIndemnitVoiture();
        break;
      default:
        indem1 = BigDecimal.ZERO;
    }
    
    int days_v2 = (dateFin.getDayOfMonth()) - (newResp.getDate_debut().getDayOfMonth());
    
    result =
            BigDecimal.valueOf(days_v2)
                    .multiply(indem1)
                    .divide(
                            BigDecimal.valueOf(
                                    dateFin
                                            .withDayOfMonth(dateFin.getMonth().length(dateFin.isLeapYear()))
                                            .getDayOfMonth()),
                            2,
                            RoundingMode.HALF_UP);
    
    return result;
  }
  
  public Object excuteRequette(int mois, int annee, String req) {
    Object etat = null;
    
    javax.persistence.Query query =
            JPA.em().createNativeQuery(req).setParameter(1, mois).setParameter(2, annee);
    etat = query.getSingleResult();
    if (etat == null) {
      etat = new String[]{".", "0", "0"};
    }
    
    return etat;
  }
  
  public BigDecimal salairePlusIndem(int mois, int annee) {
    
    javax.persistence.Query req_net =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.netapayer+es.netapayer_rappel) as net\n"
                                    + "               from hr_etat_salaire es  where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_net = req_net.getSingleResult();
    BigDecimal somme_net = BigDecimal.ZERO;
    if (obj_net != null) {
      somme_net = ((BigDecimal) obj_net);
    }
    
    javax.persistence.Query req_eqdom =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where es.mois=?1 and es.annee=?2 and gc.intituler=2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_eqdom = req_eqdom.getSingleResult();
    float somme_eqdom = (BigDecimal.ZERO).floatValue();
    if (obj_eqdom != null) {
      somme_eqdom = ((BigDecimal) obj_eqdom).floatValue();
    }
    javax.persistence.Query req_salaf =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where es.mois=?1 and es.annee=?2 and gc.intituler=1")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_salaf = req_salaf.getSingleResult();
    float somme_salaf = (BigDecimal.ZERO).floatValue();
    if (obj_salaf != null) {
      somme_salaf = ((BigDecimal) obj_salaf).floatValue();
    }
    javax.persistence.Query req_sgmb =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where es.mois=?1 and es.annee=?2 and gc.intituler=3")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_sgmb = req_sgmb.getSingleResult();
    float somme_sgmb = (BigDecimal.ZERO).floatValue();
    if (obj_sgmb != null) {
      somme_sgmb = ((BigDecimal) obj_sgmb).floatValue();
    }
    
    String req_rcar =
            "select sum(es.rcar_rg+(es.rcar_rappel)) as rcar, sum(rcarrcomp+(comp_rappel)) as complement from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    
    javax.persistence.Query req_logement =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.indemnite_logem_brut) as ir\n"
                                    + "               from hr_etat_salaire es  where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object req_log = req_logement.getSingleResult();
    float etat_log = (BigDecimal.ZERO).floatValue();
    float etat_log_p = (BigDecimal.ZERO).floatValue();
    if (req_log != null) {
      etat_log = ((BigDecimal) req_log).floatValue();
    }
    
    Object etat_rcar = excuteRequette(mois, annee, req_rcar);
    Object[] obj_rcar = (Object[]) etat_rcar;
    BigDecimal rcar_log = BigDecimal.valueOf(etat_log);
    rcar_log = rcar_log.multiply(new BigDecimal(0.03));
    float somme_rcar = ((BigDecimal) obj_rcar[0]).floatValue();
    float somme_rcar_s =
            ((BigDecimal) obj_rcar[0]).floatValue() + ((BigDecimal) obj_rcar[1]).floatValue();
    etat_log_p = (somme_rcar * 2) + ((BigDecimal) obj_rcar[1]).floatValue();
    
    javax.persistence.Query req_amo =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.amo+(es.amo_rappel)) as amo from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_amo = req_amo.getSingleResult();
    float somme_amo = (BigDecimal.ZERO).floatValue();
    if (etat_amo != null) {
      somme_amo = ((BigDecimal) etat_amo).floatValue();
    }
    
    javax.persistence.Query req_sm_omfam =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_omfam_sm+(es.mutuelle_omfam_sm_rappel)) as sm from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_sm_omfam = req_sm_omfam.getSingleResult();
    float somme_sm_omfam = (BigDecimal.ZERO).floatValue();
    if (etat_sm_omfam != null) {
      somme_sm_omfam = ((BigDecimal) etat_sm_omfam).floatValue();
    }
    
    javax.persistence.Query req_caad_omfam =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_omfam_caad+(es.mutuelle_omfam_caad_rappel)) as caad from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_caad_omfam = req_caad_omfam.getSingleResult();
    float somme_caad_omfam = (BigDecimal.ZERO).floatValue();
    if (etat_caad_omfam != null) {
      somme_caad_omfam = ((BigDecimal) etat_caad_omfam).floatValue();
    }
    
    javax.persistence.Query req_sm_r =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpapsm+(es.mutuelle_mgpapsm_rappel)) as sm_r"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "    where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_sm_r = req_sm_r.getSingleResult();
    float somme_sm_r = (BigDecimal.ZERO).floatValue();
    if (etat_sm_r != null) {
      somme_sm_r = ((BigDecimal) etat_sm_r).floatValue();
    }
    
    javax.persistence.Query req_ccd_r =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpap_ccd+(es.mutuelle_mgpap_ccd_rappel)) as ccd_ra"
                                    + " from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + " where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_ccd_r = req_ccd_r.getSingleResult();
    float somme_ccd_r = (BigDecimal.ZERO).floatValue();
    if (etat_ccd_r != null) {
      somme_ccd_r = ((BigDecimal) etat_ccd_r).floatValue();
    }
    
    BigDecimal total1 =
            (somme_net)
                    .add(BigDecimal.valueOf(somme_eqdom))
                    .add(BigDecimal.valueOf(somme_salaf))
                    .add(BigDecimal.valueOf(somme_sgmb))
                    .add(BigDecimal.valueOf(somme_rcar_s))
                    .add(BigDecimal.valueOf(somme_amo))
                    .add(BigDecimal.valueOf(somme_sm_omfam))
                    .add(BigDecimal.valueOf(somme_caad_omfam))
                    .add(BigDecimal.valueOf(somme_ccd_r))
                    .add(BigDecimal.valueOf(somme_sm_r));
    
    return total1.add(BigDecimal.valueOf(etat_log_p)).add(BigDecimal.valueOf(somme_amo));
  }
  
  @Transactional
  public void validerRappel(Augmentation augmentation) {
    
    Long id_emp = augmentation.getEmployer().getId();
    String dateDebut = augmentation.getDateDebut().toString();
    String dateFin = augmentation.getDateRappel().toString();
    String rappel = "rappel";
    getDataIndemniteRappel(dateDebut, dateFin, id_emp, rappel);
  }
  
  public int moisdoz(Integer annee) {
    
    String req =
            "select hr_etat_salaire.mois from hr_etat_salaire where hr_etat_salaire.annee=" + annee;
    int res = 0;
    List<Object> tab = RunSqlRequestForMe.runSqlRequest_ObjectList(req);
    for (Object tmp : tab) {
      if (tmp.equals(12)) {
        res = 1;
      } else {
        res = 2;
      }
    }
    
    return res;
  }
  
  public boolean verifier_etatEncNouv(String data1) {
    boolean res = false;
    String[] table = data1.split("__");
    if (!Objects.equals(table[7], table[8])) {
      res = true;
    }
    return res;
  }
  
  public Map<String, BigDecimal> calcule_prime(int annee, Long employee) {
    List<EtatSalaire> t =
            etatSalaireRepository
                    .all()
                    .filter(
                            "self.annee=?1 and self.employee=?2 order by self.imatriculation", annee, employee)
                    .fetch();
    
    EtatSalaire etat =
            t.stream().max(Comparator.comparing(etatSalaire -> etatSalaire.getMois())).orElse(null);
    Prime p =
            primeRepository
                    .all()
                    .filter("self.annee=?1 and self.employee=?2", annee, employee)
                    .fetchOne();
    BigDecimal b = BigDecimal.ZERO;
    BigDecimal ir = BigDecimal.ZERO;
    Map<String, BigDecimal> map = new HashMap<>();
    
    if (p != null) {
      if (p.getPourcentage().compareTo(BigDecimal.valueOf(0)) == 0) {
        ir = ir.add(etat.getiR()).add(etat.getIr_rappel());
        b =
                b.add(
                                etat.getTotalSalairMensuelBrutImposable() != null
                                        ? etat.getTotalSalairMensuelBrutImposable()
                                        : BigDecimal.valueOf(0))
                        .add(
                                etat.getTotalSalairMensuelBrutImposable_rappel() != null
                                        ? etat.getTotalSalairMensuelBrutImposable_rappel()
                                        : BigDecimal.valueOf(0));
      } else {
        ir =
                ir.add(etat.getiR())
                        .add(etat.getIr_rappel())
                        .multiply(p.getPourcentage())
                        .divide(BigDecimal.valueOf(100));
        
        b =
                b.add(etat.getTotalSalairMensuelBrutImposable())
                        .add(etat.getTotalSalairMensuelBrutImposable_rappel())
                        .multiply(p.getPourcentage())
                        .divide(BigDecimal.valueOf(100));
      }
    }
    map.put("brut", b);
    map.put("ir", ir);
    return map;
  }
  
  @Transactional
  public Map<String, BigDecimal> calculebrutannee(Integer annee, Long id) {
    List<EtatSalaire> list =
            etatSalaireRepository
                    .all()
                    .filter("self.annee=?1 and self.employee=?2 order by self.imatriculation", annee, id)
                    .fetch();
    BigDecimal c = BigDecimal.ZERO;
    BigDecimal tmp = BigDecimal.ZERO;
    
    BigDecimal ir = BigDecimal.valueOf(0);
    BigDecimal tmp2 = BigDecimal.valueOf(0);
    
    BigDecimal slr = BigDecimal.valueOf(0);
    BigDecimal rtn = BigDecimal.valueOf(0);
    
    Map<String, BigDecimal> map = new HashMap<>();
    
    for (EtatSalaire e : list) {
      c =
              c.add(
                      e.getTotalSalairMensuelBrutImposable()
                              .add(e.getTotalSalairMensuelBrutImposable_rappel()));
      ir = ir.add(e.getiR()).add(e.getIr_rappel());
      
      tmp =
              e.getTotalSalairMensuelBrutImposable().add(e.getTotalSalairMensuelBrutImposable_rappel());
      tmp2 = e.getiR().add(e.getIr_rappel());
      
      slr = slr.add(e.getNetAPayer()).add(e.getNetAPayer_rappel());
      
      rtn = rtn.add(e.getTotalRetenue());
    }
    Prime prime =
            Beans.get(PrimeRepository.class)
                    .all()
                    .filter("self.annee=?1 and self.employee=?2", annee, id)
                    .fetchOne();
    prime.setBrut(c.subtract(tmp));
    prime.setNet(slr);
    prime.setRetenue(rtn);
    prime.setIr(ir.subtract(tmp2));
    primeRepository.save(prime);
    map.put("brut", c.subtract(tmp));
    map.put("ir", ir.subtract(tmp));
    
    return map;
  }
}
