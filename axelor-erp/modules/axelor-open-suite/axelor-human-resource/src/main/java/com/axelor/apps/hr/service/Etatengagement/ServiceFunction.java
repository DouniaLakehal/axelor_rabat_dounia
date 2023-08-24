package com.axelor.apps.hr.service.Etatengagement;

import com.axelor.apps.configuration.db.Echelon;
import com.axelor.apps.configuration.db.repo.EchelonRepository;
import com.axelor.apps.hr.db.Augmentation;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.EtatEngagement;
import com.axelor.apps.hr.db.repo.*;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class ServiceFunction {

  @Inject EmployeeRepository employeeRepository;

  @Inject AugmentationRepository augmentationRepository;

  @Inject NaissancesRepository naissancesRepository;

  @Inject EtatEngagementRepository etatEngagementRepository;

  @Inject EchelonRepository echelonRepository;

  private final String[] keys = new String[] {"echelon", "enfant", "sitFamille"};

  protected Echelon getEchelon_at_Date(Long id, LocalDate tmp) {
    Echelon resultat = null;
    Augmentation au =
        augmentationRepository
            .all()
            .filter("self.employer.id=:id and :d1 between self.dateDebut and self.dateFin")
            .bind("id", id)
            .bind("d1", tmp)
            .fetchOne();
    if (au == null)
      au =
          augmentationRepository
              .all()
              .filter("self.employer.id=:id and self.dateFin is null and :d1 > self.dateDebut")
              .bind("id", id)
              .bind("d1", tmp)
              .fetchOne();
    resultat = au != null ? au.getEchelon() : resultat;
    return resultat;
  }

  public Employee getEmployerById(Long id) {
    return employeeRepository.find(id);
  }

  public void checkPreRequit(ActionRequest request, ActionResponse response) {
    if (request.getContext().get("date_debut") == null) {
      response.setFlash("Date de début est obligatoire !");
      return;
    }
    if (request.getContext().get("date_fin") == null) {
      response.setFlash("Date de fin est obligatoire !");
      return;
    }
    if (request.getContext().get("id") == null) {
      response.setFlash("Le personnel est obligatoire !");
      return;
    }

    if (request.getContext().get("type_engagement") == null) {
      response.setFlash("Le champs de type d'engagment est obligatoire");
      return;
    }
    Long id = (Long) request.getContext().get("id");
    Employee emp = getEmployerById(id);
    LocalDate d1 = LocalDate.parse(request.getContext().get("date_debut").toString());
    if (emp.getDaterecrutement() == null || emp.getDaterecrutement().compareTo(d1) > 0) {
      response.setFlash(
          "la date de début est incorrecte<br>Le personnel à été recuter le : "
              + emp.getDaterecrutement());
      return;
    }
  }

  protected int enfant_21_max(Employee employee, LocalDate date) {
    int res = 0;
    res =
        naissancesRepository
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

  protected String mariage_situation(Employee emp, LocalDate date) {
    if (emp.getMarriageDate() != null
        && emp.getMaritalStatus().equals("1")
        && emp.getMarriageDate().compareTo(date) >= 0) {
      return emp.getContactPartner().getTitleSelect() == 2 ? "Mariée" : "Marié";
    }
    return "Célibataire";
  }

  protected String getDuration_betweenTo_date(LocalDate d1, LocalDate d2) {
    boolean d1_first_day = d1.getDayOfMonth() == 1;
    boolean d2_last_day = d2 == d2.withDayOfMonth(d2.getMonth().length(d2.isLeapYear()));
    String s = "";
    int mois = 0;
    // du début du mois jusqu'a la fin du mois.
    if (d1_first_day && d2_last_day) {
      s = date_convert(d1, d2);
    }

    if (!d1_first_day && d2_last_day) {
      LocalDate last_d1 = d1.withDayOfMonth(d1.getMonth().length(d1.isLeapYear()));
      s = (ChronoUnit.DAYS.between(d1, last_d1) + 1) + " jours ";
      if (d2.compareTo(last_d1) != 0) {
        s = date_convert(last_d1.plusDays(1), d2) + s;
      }
    }

    if (d1_first_day && !d2_last_day) {
      LocalDate first_d2 = d2.withDayOfMonth(1);
      s = (ChronoUnit.DAYS.between(first_d2, d2) + 1) + " jours ";
      if (d1.compareTo(first_d2) != 0) s = date_convert(d1, first_d2.minusDays(1)) + s;
    }

    if (!d1_first_day && !d2_last_day) {
      if (d1.getMonthValue() != d2.getMonthValue() || d1.getYear() != d2.getYear()) {
        LocalDate last_d1 = d1.withDayOfMonth(d1.getMonth().length(d1.isLeapYear()));
        LocalDate first_d2 = d2.withDayOfMonth(1);
        int tmp1 = (int) (ChronoUnit.DAYS.between(d1, last_d1) + 1);
        int tmp2 = (int) (ChronoUnit.DAYS.between(first_d2, d2) + 1);
        s = tmp1 + tmp2 + " jours ";
        if (last_d1.plusDays(1).compareTo(first_d2) != 0) {
          s = date_convert(last_d1.plusDays(1), first_d2.minusDays(1)) + s;
        }
      } else {
        s = (ChronoUnit.DAYS.between(d1, d2) + 1) + " jours ";
      }
    }

    return s;
  }

  private String date_convert(LocalDate d1, LocalDate d2) {
    int mois = (int) (ChronoUnit.MONTHS.between(d1, d2) + 1);
    if (mois >= 12) return (ChronoUnit.YEARS.between(d1, d2)) + " year " + mois % 12 + " mois ";
    else return mois + " mois";
  }

  public EtatEngagement getEtatengagementByemployer(Long id, LocalDate d1, LocalDate d2) {
    return etatEngagementRepository
        .all()
        .filter("self.employee=:id and self.dateDebut=:d1 and self.dateFin=:d2")
        .bind("id", id)
        .bind("d1", d1)
        .bind("d2", d2)
        .fetchOne();
  }

  @Transactional
  protected EtatEngagement saveEtatEngagment(
      Map<String, Object> data, Employee emp, LocalDate d1, LocalDate d2) {
    EtatEngagement etat = new EtatEngagement();

    etat.setEmployee(employeeRepository.find(emp.getId()));

    etat.setEchelon_old(echelonRepository.find((Long) data.get("echelon_old")));
    etat.setEchelon_new(echelonRepository.find((Long) data.get("echelon_new")));

    etat.setDateDebut(d1);
    etat.setDateFin(d2);

    etat.setEnfant_old((Integer) data.get("enfant_old"));
    etat.setEnfant_new((Integer) data.get("enfant_new"));

    etat.setSit_famille_old((String) data.get("sitFamille_old"));
    etat.setSit_famille_new((String) data.get("sitFamille_new"));

    etat.setDuree((String) data.get("duration"));

    etat = etatEngagementRepository.save(etat);
    List<EtatEngagement> ls = emp.getEtatEngagement();
    ls.add(etat);
    emp.setEtatEngagement(ls);
    employeeRepository.save(emp);
    return etat;
  }

  protected boolean checkEquals(Map<String, Object> map) {
    boolean equlz = true;
    for (String key : keys) {
      if (equlz && !map.get(key + "_old").equals(map.get(key + "_new"))) equlz = false;
    }
    return equlz;
  }

  @Transactional
  protected Long extentionEtatEngagement(EtatEngagement tmp, LocalDate d2) {
    EtatEngagement etat = etatEngagementRepository.find(tmp.getId());
    etat.setDateFin(d2);
    etat = etatEngagementRepository.save(etat);
    return etat.getId();
  }

  public EtatEngagement getEtatEngagementBefor(Employee emp, LocalDate d1) {
    EtatEngagement res =
        etatEngagementRepository
            .all()
            .filter("self.dateDebut>=:date and self.employee=:emp")
            .bind("date", d1)
            .bind("emp", emp)
            .order("-dateDebut")
            .fetchOne();
    return res;
  }

  protected boolean check_liaison(EtatEngagement tmp, Map<String, Object> data) {

    boolean a = tmp.getEchelon_new().equals(echelonRepository.find((Long) data.get("echelon_old")));
    boolean b = tmp.getEnfant_new().equals((Integer) data.get("enfant_old"));
    boolean c = tmp.getSit_famille_new().equals((String) data.get("sitFamille_old"));

    return a && b && c;
  }

  @Transactional
  public void completeInformation(Long id_etat, String type, String typeEngagement) {
    EtatEngagement etat = etatEngagementRepository.find(id_etat);
    etat.setNatureDoc(type);
    etat.setNatureEtatEngagement(typeEngagement);
    etatEngagementRepository.save(etat);
  }
}
