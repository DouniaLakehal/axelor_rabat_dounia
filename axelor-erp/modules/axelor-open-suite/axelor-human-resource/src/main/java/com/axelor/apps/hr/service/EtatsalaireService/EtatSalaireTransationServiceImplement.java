package com.axelor.apps.hr.service.EtatsalaireService;

import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.repo.*;
import com.axelor.apps.configuration.service.RunSqlRequestForMe;
import com.axelor.apps.hr.db.EtatSalaireTransaction;
import com.axelor.apps.hr.db.EtatSalaireTransactionDetail;
import com.axelor.apps.hr.db.repo.EtatSalaireTransactionDetailRepository;
import com.axelor.apps.hr.db.repo.EtatSalaireTransactionRepository;
import com.axelor.apps.hr.service.EmployeeAdvanceService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EtatSalaireTransationServiceImplement implements EtatSalaireTransationService {
  @Inject EmployeeAdvanceService employeeAdvanceService;
  @Inject CompteRepository compteRepository;
  @Inject RubriqueBudgetaireGeneraleRepository rubriqueBudgetaireGeneraleRepository;
  @Inject BudgetParRubriqueRepository budgetParRubriqueRepository;
  @Inject EtatSalaireTransactionRepository etatSalaireTransactionRepository;
  @Inject EtatSalaireTransactionDetailRepository etatSalaireTransactionDetailRepository;
  @Inject HistoriqueCompteRepository historiqueCompteRepository;
  @Inject HistoriqueBudgetaireRepository historiqueBudgetaireRepository;

  public static final String[] arrayCode = {
    "617111", "617112", "617131", "617133", "617135", "617136", "617422", "61743"
  };

  @Override
  public boolean checkMontantAlltransaction(int annee, int mois, Long id_compte) {
    BigDecimal totalGenerale = employeeAdvanceService.getTotalGenerale(mois, annee);
    Compte c = compteRepository.find(id_compte);
    boolean weHaveArgent = false;
    if (c.getMontant().compareTo(totalGenerale) >= 0) {
      int nbr_true = 0;
      for (String code : arrayCode) {
        if (!weHaveArgent) {
          BigDecimal total_617111 = getTotalMontantByCode(mois, annee, code);
          nbr_true =
              checkRubriqueBudgetaireHaveArgent(code, total_617111, annee)
                  ? (nbr_true + 1)
                  : nbr_true;
        } else {
          return weHaveArgent;
        }
      }
      weHaveArgent = arrayCode.length == nbr_true;
    }
    return weHaveArgent;
  }

  private BigDecimal getTotalMontantByCode(int mois, int annee, String code) {
    BigDecimal res = BigDecimal.ZERO;
    String req = "";
    switch (code) {
      case "617111":
        req =
            "SELECT sum(salaire_net617111) + sum(rcar_rg)+sum(rcarrcomp) + sum(recore) + sum(amo) + sum(mutuelle_mgpapsm)+sum(mutuelle_mgpap_ccd) + sum(mutuelle_omfam_sm)+sum(mutuelle_omfam_caad) + sum(ir) + sum(pret_axa_credit) AS tot from hr_etat_salaire where titulaire=true and is_disposition=false and mois = "
                + mois
                + " and annee = "
                + annee
                + ";";
        break;
      case "617112":
        req =
            "SELECT sum(salaire_net617111) + sum(amo) + sum(mutuelle_mgpap_ccd) +sum(mutuelle_mgpapsm) + sum(ir) from hr_etat_salaire where titulaire=false and is_disposition=false and mois = "
                + mois
                + " and annee = "
                + annee
                + ";";
        break;
      case "617131":
        req =
            "SELECT sum(indemnite_logement_net617131) + sum(rcar_comp617131) + sum(ir617131) from hr_etat_salaire where is_disposition=false and mois = "
                + mois
                + " and annee = "
                + annee
                + ";";
        break;
      case "617133":
        req =
            "select sum(indemnit_fonction_net) from hr_etat_salaire where annee = "
                + annee
                + " and mois = "
                + mois
                + ";";
        break;
      case "617135":
        req =
            "select sum(indemnit_voiture_net) from hr_etat_salaire where  mois = "
                + mois
                + " and annee = "
                + annee
                + ";";
        break;
      case "617136":
        req =
            "select sum(indemnit_represent_net) from hr_etat_salaire where mois = "
                + mois
                + " and annee = "
                + annee
                + ";";
        break;
      case "617422":
        req =
            "select sum(rcar_rg)*2 + sum(rcarrcomp) + sum(rcar_comp617131) from axelor.public.hr_etat_salaire where mois = "
                + mois
                + " and annee = "
                + annee
                + ";";
        break;
      case "61743":
        req =
            "select sum(amo) from axelor.public.hr_etat_salaire where mois = "
                + mois
                + " and annee = "
                + annee
                + ";";
        break;
      default:
        req = "select 0 as total;";
        break;
    }
    res = RunSqlRequestForMe.runSqlRequest_Bigdecimal(req);
    return res == null ? BigDecimal.ZERO : res;
  }

  private boolean checkRubriqueBudgetaireHaveArgent(
      String code, BigDecimal total_617111, int annee) {
    boolean res = false;
    RubriqueBudgetaireGenerale rub =
        rubriqueBudgetaireGeneraleRepository.all().filter("self.codeBudg = ?", code).fetchOne();
    if (rub != null) {
      BudgetParRubrique budget =
          budgetParRubriqueRepository
              .all()
              .filter("self.rubriqueBudgetaire = ? and self.year = ?", rub, annee)
              .fetchOne();
      if (budget != null && budget.getMontant() != null) {
        res = budget.getMontant().compareTo(total_617111) >= 0;
      }
    }
    return res;
  }

  // effectuer retrait depuis les rubriques + compte

  @Override
  @Transactional
  public EtatSalaireTransaction doRetraittransaction(int annee, int mois, Long id_compte) {
    List<RubriqueBudgetaireGenerale> ls_rub = new ArrayList<>();
    List<BudgetParRubrique> ls_budget = new ArrayList<>();
    Compte c = compteRepository.find(id_compte);
    for (String tmp : arrayCode) {
      RubriqueBudgetaireGenerale r =
          rubriqueBudgetaireGeneraleRepository.all().filter("self.codeBudg = ?1", tmp).fetchOne();
      ls_rub.add(r);
    }

    for (RubriqueBudgetaireGenerale tmp : ls_rub) {
      BudgetParRubrique b =
          budgetParRubriqueRepository
              .all()
              .filter("self.year = ?1 and self.rubriqueBudgetaire = ?2", annee, tmp)
              .fetchOne();
      ls_budget.add(b);
    }
    List<EtatSalaireTransactionDetail> ls_detail = new ArrayList<>();

    EtatSalaireTransaction t = new EtatSalaireTransaction();
    t.setAnnee(annee);
    t.setMoisInt(mois);
    t.setCompte(c);
    BigDecimal tt = employeeAdvanceService.getTotalGenerale(mois, annee);
    t.setMontantTotal(tt);
    t = etatSalaireTransactionRepository.save(t);

    for (BudgetParRubrique b : ls_budget) {
      EtatSalaireTransactionDetail d = new EtatSalaireTransactionDetail();
      d.setRubriqueBudgetaire(b.getRubriqueBudgetaire());
      BigDecimal vt = getTotalMontantByCode(mois, annee, b.getRubriqueBudgetaire().getCodeBudg());
      d.setMontant(vt);
      d.setEtatSalaireTransaction(t);
      d = etatSalaireTransactionDetailRepository.save(d);
      ls_detail.add(d);
    }
    t.setEtatSalaireTransactionDetail(ls_detail);
    t = etatSalaireTransactionRepository.save(t);
    return t;
  }

  @Override
  @Transactional
  public void reduceFromRubriqueAndCompte(EtatSalaireTransaction t) {
    AnnexeGenerale annex = null;
    annex =
        t.getCreatedBy().getEmployee() == null ? null : t.getCreatedBy().getEmployee().getAnnexe();

    try {
      List<EtatSalaireTransactionDetail> ls = t.getEtatSalaireTransactionDetail();
      for (EtatSalaireTransactionDetail tmp : ls) {
        BudgetParRubrique b =
            budgetParRubriqueRepository
                .all()
                .filter(
                    "self.year = ?1 and self.rubriqueBudgetaire = ?2",
                    t.getAnnee(),
                    tmp.getRubriqueBudgetaire())
                .fetchOne();

        HistoriqueBudgetaire h = new HistoriqueBudgetaire();
        h.setAnnee(t.getAnnee());
        h.setAnnexe(annex);
        h.setRubriqueBudgetaire(
            rubriqueBudgetaireGeneraleRepository.find(tmp.getRubriqueBudgetaire().getId()));
        h.setMois(t.getMoisInt());
        h.setDateEx(LocalDate.now());
        h.setMontant(tmp.getMontant());
        h.setDateExecution(LocalDateTime.now());
        h.setMontantRubrique(b.getMontant().subtract(tmp.getMontant()));
        h.setBudgetParRubrique(b);
        h = historiqueBudgetaireRepository.save(h);
        tmp.setHistoriqueBudgetaire(h);
        etatSalaireTransactionDetailRepository.save(tmp);
        b.setMontant(b.getMontant().subtract(tmp.getMontant()));
        budgetParRubriqueRepository.save(b);
      }

      HistoriqueCompte c = new HistoriqueCompte();
      c.setDescription("Transaction de retaite du Salaire generale du mois " + t.getMoisString());
      c.setDesignation("Salire du mois " + t.getMoisString());
      c.setRib(t.getCompte().getRib());
      c.setCompte(t.getCompte());
      c.setMontant(
          t.getCompte()
              .getMontant()
              .subtract(t.getMontantTotal())
              .setScale(2, RoundingMode.HALF_UP));
      c.setAction(2);
      c.setAnneeHistorique(LocalDate.now().getYear());
      c.setAnnexe(annex);
      c.setDateTransaction(LocalDateTime.now());
      c.setDepense(t.getMontantTotal());
      c.setMoisHistorique(t.getMoisString());
      c = historiqueCompteRepository.save(c);
      t.setHistoriqueCompte(c);
      etatSalaireTransactionRepository.save(t);

      Compte compte = compteRepository.find(t.getCompte().getId());
      BigDecimal mt_compt = compte.getMontant();
      BigDecimal mt_transaction = t.getMontantTotal();
      BigDecimal mt_result = mt_compt.subtract(mt_transaction);
      compte.setMontant(mt_result);
      compteRepository.save(compte);

    } catch (Exception e) {
      Logger logger = LoggerFactory.getLogger(getClass());
      logger.error("Transaction de retrait du montant Non effectuer Error : ", e);
    }
  }

  @Override
  @Transactional
  public void removeTransaction(int mois, int annee) {
    List<EtatSalaireTransaction> ts =
        etatSalaireTransactionRepository
            .all()
            .filter("self.annee = ? and self.moisInt = ?", annee, mois)
            .fetch();
    if (ts != null) {
      try {
        for (EtatSalaireTransaction t : ts) {
          AnnexeGenerale annex =
              t.getCreatedBy().getEmployee() == null
                  ? null
                  : t.getCreatedBy().getEmployee().getAnnexe();
          for (EtatSalaireTransactionDetail tmp : t.getEtatSalaireTransactionDetail()) {
            Long id = tmp.getHistoriqueBudgetaire().getId();
            etatSalaireTransactionDetailRepository.remove(tmp);
            this.rollbackHistoriqueBudgetaire(id);
          }

          Compte compte = compteRepository.find(t.getCompte().getId());
          compte.setMontant(compte.getMontant().add(t.getMontantTotal()));
          compteRepository.save(compte);

          Long id_hcompt = t.getHistoriqueCompte().getId();
          etatSalaireTransactionRepository.remove(t);
          this.rollbackHistoriqueCompte(id_hcompt);
        }
      } catch (Exception e) {
        Logger logger = LoggerFactory.getLogger(getClass());
        logger.error("Error : ", e);
      }
    }
  }

  @Transactional
  private void rollbackHistoriqueBudgetaire(Long id_historiqueBudgetaire) {
    try {
      HistoriqueBudgetaire h_budget = historiqueBudgetaireRepository.find(id_historiqueBudgetaire);
      List<HistoriqueBudgetaire> ls_hist =
          historiqueBudgetaireRepository
              .all()
              .filter(
                  "self.dateExecution>=?1 and self.rubriqueBudgetaire=?2",
                  h_budget.getDateExecution(),
                  h_budget.getRubriqueBudgetaire())
              .fetch();

      BudgetParRubrique budgetParRubrique =
          budgetParRubriqueRepository.find(h_budget.getBudgetParRubrique().getId());
      budgetParRubrique.setMontant(budgetParRubrique.getMontant().add(h_budget.getMontant()));
      budgetParRubriqueRepository.save(budgetParRubrique);

      for (HistoriqueBudgetaire h : ls_hist) {
        h.setMontantRubrique(h.getMontantRubrique().add(h.getMontant()));
        if (h.getId() == h_budget.getId()) h.setMontant(BigDecimal.ZERO);
        historiqueBudgetaireRepository.save(h);
      }
    } catch (Exception e) {
      Logger logger = LoggerFactory.getLogger(getClass());
      logger.error("Transaction Rollback historique BudetparRubrique Non effectuer Error : ", e);
    }
  }

  @Transactional
  private void rollbackHistoriqueCompte(Long id_hcompt) {
    try {
      HistoriqueCompte historiqueCompte = historiqueCompteRepository.find(id_hcompt);
      List<HistoriqueCompte> ls_c =
          historiqueCompteRepository
              .all()
              .filter(
                  "self.dateTransaction>=?1 and self.compte = ?2",
                  historiqueCompte.getDateTransaction(),
                  historiqueCompte.getCompte())
              .fetch();

      for (HistoriqueCompte h : ls_c) {
        h.setMontant(h.getMontant().add(historiqueCompte.getDepense()));
        if (historiqueCompte.getId() == h.getId()) h.setDepense(BigDecimal.ZERO);
        historiqueCompteRepository.save(h);
      }
    } catch (Exception e) {
      Logger logger = LoggerFactory.getLogger(getClass());
      logger.error("Transaction Rollback historique Compte Non effectuer Error : ", e);
    }
  }
}
