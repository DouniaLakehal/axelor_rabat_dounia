package com.axelor.apps.configuration.service;

import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.repo.IRRepository;
import com.axelor.apps.configuration.db.repo.RCARRepository;
import com.axelor.apps.configuration.db.repo.RubriquesBudgetaireRepository;
import com.axelor.apps.configuration.db.repo.VersionRubriqueBudgetaireRepository;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import org.apache.commons.collections.CollectionUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceUtil {
  public static String[] MOIS_ARRABE = new String[]{"يناير", "فبراير", "مارس", "أبريل", "ماي", "يونيو", "يوليوز", "غشت", "شتنبر", "أكتوبر", "نونبر", "دجنبر"};
  public static String[] MOIS_FRANCAIS = new String[]{"Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
  @Inject
  IRRepository irRepository;
  @Inject
  RCARRepository rcarRepository;
  
  public static int getnombreJourWork(LocalDate d1, LocalDate d2) {
    int res = 0;
    LocalDate tmp = d1;
    do {
      if ((tmp.getDayOfWeek() != DayOfWeek.SATURDAY) && (tmp.getDayOfWeek() != DayOfWeek.SUNDAY)) {
        res++;
      }
      tmp = tmp.plusDays(1);
    } while (tmp.compareTo(d2) <= 0);
    return res;
  }

  public IR getIRByMontnat(BigDecimal some1_old) {
    IR tmp = null;
    tmp =
        irRepository
            .all()
            .filter("self.montant_min < ?1 and self.montant_max >= ?1", some1_old)
            .fetchOne();
    return tmp;
  }

  public RCAR getRcarByYear(int year) {
    return rcarRepository.all().filter("self.annee=?1", year).fetchOne();
  }

  public static LocalDate addDaysSkippingWeekends(LocalDate date, int days) {
    LocalDate result = date;
    int addedDays = 0;
    while (addedDays < days) {
      result = result.plusDays(1);
      if (!(result.getDayOfWeek() == DayOfWeek.SATURDAY
          || result.getDayOfWeek() == DayOfWeek.SUNDAY)) {
        ++addedDays;
      }
    }
    return result;
  }

  public static String getIdListString(List<Long> collection) {
    String idString;
    if (CollectionUtils.isEmpty(collection)) {
      idString = "0";
    } else {
      idString = collection.stream().map(l -> l.toString()).collect(Collectors.joining(","));
    }
    return idString;
  }

  public RubriquesBudgetaire getRubriqueBudgetaireGeneraleByAnneeAndCode(int annee, String code)
      throws Exception {
    VersionRubriqueBudgetaire v =
        Beans.get(VersionRubriqueBudgetaireRepository.class)
            .all()
            .filter("self.annee=:annee")
            .bind("annee", annee)
            .fetchOne();
    if (v == null) {
      throw new Exception("Attention aucun budget dans l'annee " + annee);
    }
    VersionRB vr =
        v.getVersionRubriques().stream()
            .filter(VersionRB::getIs_versionFinale)
            .findFirst()
            .orElse(null);
    if (vr == null) {
      throw new Exception("Attention Pas de budget valide pour l'année " + annee);
    }

    RubriquesBudgetaire rb =
        Beans.get(RubriquesBudgetaireRepository.class)
            .all()
            .filter("self.code_budget=:code and id_version=:idversion")
            .bind("code", code)
            .bind("idversion", vr.getId())
            .fetchOne();
    return rb;
  }
}
