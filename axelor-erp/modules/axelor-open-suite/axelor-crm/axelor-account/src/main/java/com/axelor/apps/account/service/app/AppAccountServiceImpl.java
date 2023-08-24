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
package com.axelor.apps.account.service.app;

import com.axelor.apps.account.db.*;
import com.axelor.apps.account.db.repo.*;
import com.axelor.apps.base.db.AppAccount;
import com.axelor.apps.base.db.AppBudget;
import com.axelor.apps.base.db.AppInvoice;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.repo.AppAccountRepository;
import com.axelor.apps.base.db.repo.AppBudgetRepository;
import com.axelor.apps.base.db.repo.AppInvoiceRepository;
import com.axelor.apps.base.db.repo.CompanyRepository;
import com.axelor.apps.base.service.app.AppBaseServiceImpl;
import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.repo.*;
import com.axelor.apps.configuration.service.ConvertNomreToLettres;
import com.axelor.apps.configuration.service.RunSqlRequestForMe;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

@Singleton
public class AppAccountServiceImpl extends AppBaseServiceImpl implements AppAccountService {

  @Inject private AppAccountRepository appAccountRepo;

  @Inject private AppBudgetRepository appBudgetRepo;

  @Inject private AppInvoiceRepository appInvoiceRepo;
  @Inject private HistoriqueBudgetaireRepository historiqueBudgetaireRepository;
  @Inject private AccountConfigRepository accountConfigRepo;
  @Inject private RegieTestRepository regieTestRepository;
  @Inject private CompanyRepository companyRepo;
  @Inject private GestionRecetteRepository gestionRecetteRepository;
  @Inject private HistoriqueCompteRepository historiqueCompteRepository;
  @Inject private RubriqueBudgetaireGeneraleRepository rubriqueBudgetaireGeneraleRepository;
  @Inject private ModificationBudgetRepository modificationBudgetRepository;
  @Inject private BudgetParRubriqueRepository budgetParRubriqueRepository;
  @Inject private MoveLineRepository moveLineRepository;
  @Inject private PointerRepository pointerRepository;
  @Inject private MoveLineManagementRepository movelineManagementRepository;

  @Inject private AnnexeGeneraleRepository annexeGeneraleRepository;
  @Inject private UserRepository userRepository;

  @Inject private CompteRepository compteRepository;

  @Inject private SituationRecetteRepository situationRecetteRepository;
  @Inject private PrevisionRecetteRepository previsionRecetteRepository;
  @Inject private EncaissementRepository encaissementRepository;
  @Inject private EncaissementSelectRepository encaissementSelectRepository;
  @Inject private RubriquesBudgetaireRepository rubriquesBudgetaireRepository;
  @Inject private MoveRepository moveRepository;

  @Override
  public AppAccount getAppAccount() {
    return appAccountRepo.all().fetchOne();
  }

  public String getDataArreteCompt(Long idCompte, int annee) {
    int anneePrecedent = annee - 1;
    List<String> data = new ArrayList<>();
    // annee precedente

    Float sommeRecettePrevuePrecedente = getSommeRecettePrevuPrecedente(anneePrecedent);
    Float sommeRecetteRealisePrecedente = getSommeRecetteRealisePrecedente(anneePrecedent);
    Float diffRecettePrecedente = sommeRecetteRealisePrecedente - sommeRecettePrevuePrecedente;

    // recettes prevues et realisées
    Float recetteP1 = getRecettePrvueOfRubrique(301L, annee);
    Float recetteP2 = getRecettePrvueOfRubrique(302L, annee);
    Float recetteP3 = getRecettePrvueOfRubrique(303L, annee);
    Float recetteP4 = getRecettePrvueOfRubrique(304L, annee);
    Float recetteP5 = getRecettePrvueOfRubrique(305L, annee);
    Float recetteP6 = getRecettePrvueOfRubrique(306L, annee);
    Float recetteP7 = getRecettePrvueOfRubrique(307L, annee);
    Float recetteP8 = getRecettePrvueOfRubrique(308L, annee);
    Float recetteP9 = getRecettePrvueOfRubrique(309L, annee);

    Float totalRecatteP =
        recetteP1
            + recetteP2
            + recetteP3
            + recetteP4
            + recetteP5
            + recetteP6
            + recetteP7
            + recetteP8
            + recetteP9
            + sommeRecettePrevuePrecedente;

    Float recetteR1 = getRecetteSommeRealise(annee, idCompte, 301L);
    Float recetteR2 = getRecetteSommeRealise(annee, idCompte, 302L);
    Float recetteR3 = getRecetteSommeRealise(annee, idCompte, 303L);
    Float recetteR4 = getRecetteSommeRealise(annee, idCompte, 304L);
    Float recetteR5 = getRecetteSommeRealise(annee, idCompte, 305L);
    Float recetteR6 = getRecetteSommeRealise(annee, idCompte, 306L);
    Float recetteR7 = getRecetteSommeRealise(annee, idCompte, 307L);
    Float recetteR9 = getRecetteSommeRealise(annee, idCompte, 308L);
    Float recetteR8 = getRecetteSommeRealise(annee, idCompte, 309L);

    Float totalRecatteR =
        recetteR1
            + recetteR2
            + recetteR3
            + recetteR4
            + recetteR5
            + recetteR6
            + recetteR7
            + recetteR8
            + recetteR9
            + sommeRecetteRealisePrecedente;

    Float diffRec1 = recetteR1 - recetteP1;
    Float diffRec2 = recetteR2 - recetteP2;
    Float diffRec3 = recetteR3 - recetteP3;
    Float diffRec4 = recetteR4 - recetteP4;
    Float diffRec5 = recetteR5 - recetteP5;
    Float diffRec6 = recetteR6 - recetteP6;
    Float diffRec7 = recetteR7 - recetteP7;
    Float diffRec8 = recetteR8 - recetteP8;
    Float diffRec9 = recetteR9 - recetteP9;

    Float TotaldiffRec =
        diffRec1
            + diffRec2
            + diffRec3
            + diffRec4
            + diffRec5
            + diffRec6
            + diffRec7
            + diffRec8
            + diffRec9
            + diffRecettePrecedente;

    // annee precedente
    data.add(sommeRecettePrevuePrecedente.toString()); // 0
    data.add(sommeRecetteRealisePrecedente.toString()); // 1
    data.add(diffRecettePrecedente.toString()); // 2

    // annee presente Recettes
    data.add(recetteP1.toString()); // 3
    data.add(recetteP2.toString()); // 4
    data.add(recetteP3.toString()); // 5
    data.add(recetteP4.toString()); // 6
    data.add(recetteP5.toString()); // 7
    data.add(recetteP6.toString()); // 8
    data.add(recetteP7.toString()); // 9
    data.add(recetteP8.toString()); // 10
    data.add(recetteP9.toString()); // 11

    data.add(totalRecatteP.toString()); // 12

    data.add(recetteR1.toString()); // 13
    data.add(recetteR2.toString()); // 14
    data.add(recetteR3.toString()); // 15
    data.add(recetteR4.toString()); // 16
    data.add(recetteR5.toString()); // 17
    data.add(recetteR6.toString()); // 18
    data.add(recetteR7.toString()); // 19
    data.add(recetteR8.toString()); // 20
    data.add(recetteR9.toString()); // 21

    data.add(totalRecatteR.toString()); // 22

    data.add(diffRec1.toString()); // 23
    data.add(diffRec2.toString()); // 24
    data.add(diffRec3.toString()); // 25
    data.add(diffRec4.toString()); // 26
    data.add(diffRec5.toString()); // 27
    data.add(diffRec6.toString()); // 28
    data.add(diffRec7.toString()); // 29
    data.add(diffRec8.toString()); // 30
    data.add(diffRec9.toString()); // 31

    data.add(TotaldiffRec.toString()); // 32

    // annee presente Depense prevue et realisé
    Float depenseP1 = getDepensePrvueOfRubrique(2L, annee);
    Float depenseP2 = getDepensePrvueOfRubrique(25L, annee);
    Float depenseP3 = getDepensePrvueOfRubrique(125L, annee);
    Float depenseP4 = getDepensePrvueOfRubrique(131L, annee);
    Float depenseP5 = getDepensePrvueOfRubrique(166L, annee);
    Float depenseP6 = getDepensePrvueOfRubrique(175L, annee);
    Float depenseP7 = getDepensePrvueOfRubrique(306L, annee);

    Float depenseP8 = getDepensePrvueOfRubrique(199L, annee);
    Float depenseP9 = getDepensePrvueOfRubrique(204L, annee);
    Float depenseP10 = getDepensePrvueOfRubrique(221L, annee);
    Float depenseP11 = getDepensePrvueOfRubrique(233L, annee);
    Float depenseP12 = getDepensePrvueOfRubrique(251L, annee);
    Float depenseP13 = getDepensePrvueOfRubrique(261L, annee);
    Float depenseP14 = getDepensePrvueOfRubrique(263L, annee);
    Float depenseP15 = getDepensePrvueOfRubrique(228L, annee);

    Float totalDepenseP1 =
        depenseP1 + depenseP2 + depenseP3 + depenseP4 + depenseP5 + depenseP6 + depenseP7;
    Float totalDepenseP2 =
        depenseP8
            + depenseP9
            + depenseP10
            + depenseP11
            + depenseP12
            + depenseP13
            + depenseP14
            + depenseP15;
    Float SommeOfCreditsCCISM = getSommeOfCreditsCCISM(annee);
    Float totalDepenseP =
        depenseP1
            + depenseP2
            + depenseP3
            + depenseP4
            + depenseP5
            + depenseP6
            + depenseP7
            + depenseP8
            + depenseP9
            + depenseP10
            + depenseP11
            + depenseP12
            + depenseP13
            + depenseP14
            + depenseP15
            + SommeOfCreditsCCISM;

    Float depenseR1 = getDepenseSommeRealise(annee, 2L);
    Float depenseR2 = getDepenseSommeRealise(annee, 25L);
    Float depenseR3 = getDepenseSommeRealise(annee, 126L);
    Float depenseR4 = getDepenseSommeRealise(annee, 612L);
    Float depenseR5 = getDepenseSommeRealise(annee, 166L);
    Float depenseR6 = getDepenseSommeRealise(annee, 175L);
    Float depenseR7 = getDepenseSommeRealise(annee, 306L);

    Float totalDepenseR1 =
        depenseR1 + depenseR2 + depenseR3 + depenseR4 + depenseR5 + depenseR6 + depenseR7;

    Float depenseR8 = getDepenseSommeRealise(annee, 199L);
    Float depenseR9 = getDepenseSommeRealise(annee, 204L);
    Float depenseR10 = getDepenseSommeRealise(annee, 221L);
    Float depenseR11 = getDepenseSommeRealise(annee, 233L);
    Float depenseR12 = getDepenseSommeRealise(annee, 251L);
    Float depenseR13 = getDepenseSommeRealise(annee, 261L);
    Float depenseR14 = getDepenseSommeRealise(annee, 263L);
    Float depenseR15 = getDepenseSommeRealise(annee, 228L);

    Float totalDepenseR2 =
        depenseR8
            + depenseR9
            + depenseR10
            + depenseR11
            + depenseR12
            + depenseR13
            + depenseR14
            + depenseR15;
    Float totalDepenseR =
        depenseR1
            + depenseR2
            + depenseR3
            + depenseR4
            + depenseR5
            + depenseR6
            + depenseR7
            + depenseR8
            + depenseR9
            + depenseR10
            + depenseR11
            + depenseR12
            + depenseR13
            + depenseR14
            + depenseR15;

    Float diffD1 = depenseP1 - depenseR1;
    Float diffD2 = depenseP2 - depenseR2;
    Float diffD3 = depenseP3 - depenseR3;
    Float diffD4 = depenseP4 - depenseR4;
    Float diffD5 = depenseP5 - depenseR5;
    Float diffD6 = depenseP6 - depenseR6;
    Float diffD7 = depenseP7 - depenseR7;

    Float diffDtotal1 = totalDepenseP1 - totalDepenseR1;

    Float diffD8 = depenseP8 - depenseR8;
    Float diffD9 = depenseP9 - depenseR9;
    Float diffD10 = depenseP10 - depenseR10;
    Float diffD11 = depenseP11 - depenseR11;
    Float diffD12 = depenseP12 - depenseR12;
    Float diffD13 = depenseP13 - depenseR13;
    Float diffD14 = depenseP14 - depenseR14;
    Float diffD15 = depenseP15 - depenseR15;

    Float diffDtotal2 = totalDepenseP2 - totalDepenseR2;
    Float totalDiffD =
        diffD1
            + diffD2
            + diffD3
            + diffD4
            + diffD5
            + diffD6
            + diffD7
            + diffD8
            + diffD9
            + diffD10
            + diffD11
            + diffD12
            + diffD13
            + diffD14
            + diffD15
            + getSommeOfCreditsCCISM(annee);

    data.add(depenseP1.toString()); // 33
    data.add(depenseP2.toString()); // 34
    data.add(depenseP3.toString()); // 35
    data.add(depenseP4.toString()); // 36
    data.add(depenseP5.toString()); // 37
    data.add(depenseP6.toString()); // 38
    data.add(depenseP7.toString()); // 39

    data.add(totalDepenseP1.toString()); // 40

    data.add(depenseP8.toString()); // 41
    data.add(depenseP9.toString()); // 42
    data.add(depenseP10.toString()); // 43
    data.add(depenseP11.toString()); // 44
    data.add(depenseP12.toString()); // 45
    data.add(depenseP13.toString()); // 46
    data.add(depenseP14.toString()); // 47
    data.add(depenseP15.toString()); // 48

    data.add(totalDepenseP2.toString()); // 49
    data.add(totalDepenseP.toString()); // 50

    data.add(depenseR1.toString()); // 51
    data.add(depenseR2.toString()); // 52
    data.add(depenseR3.toString()); // 53
    data.add(depenseR4.toString()); // 54
    data.add(depenseR5.toString()); // 55
    data.add(depenseR6.toString()); // 56
    data.add(depenseR7.toString()); // 57

    data.add(totalDepenseR1.toString()); // 58

    data.add(depenseR8.toString()); // 59
    data.add(depenseR9.toString()); // 60
    data.add(depenseR10.toString()); // 61
    data.add(depenseR11.toString()); // 62
    data.add(depenseR12.toString()); // 63
    data.add(depenseR13.toString()); // 64
    data.add(depenseR14.toString()); // 65
    data.add(depenseR15.toString()); // 66

    data.add(totalDepenseR2.toString()); // 67
    data.add(totalDepenseR.toString()); // 68

    data.add(diffD1.toString()); // 69
    data.add(diffD2.toString()); // 70
    data.add(diffD3.toString()); // 71
    data.add(diffD4.toString()); // 72
    data.add(diffD5.toString()); // 73
    data.add(diffD6.toString()); // 74
    data.add(diffD7.toString()); // 75

    data.add(diffDtotal1.toString()); // 76

    data.add(diffD8.toString()); // 77
    data.add(diffD9.toString()); // 78
    data.add(diffD10.toString()); // 79
    data.add(diffD11.toString()); // 80
    data.add(diffD12.toString()); // 81
    data.add(diffD13.toString()); // 82
    data.add(diffD14.toString()); // 83
    data.add(diffD15.toString()); // 84

    data.add(diffDtotal2.toString()); // 85
    data.add(totalDiffD.toString()); // 86

    Float totalArreteCompte = totalRecatteR - totalDepenseR;
    data.add(totalArreteCompte.toString()); // 87

    data.forEach((n) -> System.out.println(n));

    String x = StringUtils.join(data, "__");
    return x;
  }

  public float getSommeRecettePrevuPrecedente(int anneePrecedent) {
    List<PrevisionRecette> recettePrecedentList =
        Beans.get(PrevisionRecetteRepository.class)
            .all()
            .filter("self.year=:annee")
            .bind("annee", anneePrecedent)
            .fetch();
    float somme = 0;
    for (PrevisionRecette lrbl : recettePrecedentList) {
      somme = somme + lrbl.getMontant().floatValue();
    }
    return somme;
  }

  public float getSommeRecetteRealisePrecedente(int anneePrecedent) {
    List<GestionRecette> recettePrecedentList =
        Beans.get(GestionRecetteRepository.class)
            .all()
            .filter("self.anneeRecette=:annee")
            .bind("annee", anneePrecedent)
            .fetch();
    float somme = 0;
    for (GestionRecette lrbl : recettePrecedentList) {
      somme = somme + lrbl.getMontant().floatValue();
    }
    return somme;
  }

  public Float getRecetteSommeRealise(int annee, Long compte, long id) {
    List<GestionRecette> recetteList =
        Beans.get(GestionRecetteRepository.class)
            .all()
            // .filter("self.anneeRecette=:annee AND self.rib=:compte AND self.rubrique=:id")
            .filter("self.anneeRecette=:annee AND self.rubrique=:id")
            .bind("annee", annee)
            // .bind("compte", compte)
            .bind("id", id)
            .fetch();

    float somme = 0;

    for (GestionRecette c : recetteList) {
      somme += c.getMontant().floatValue();
    }
    return somme;
  }

  public Float getRecettePrvueOfRubrique(Long id, int annee) {
    PrevisionRecette rec =
        Beans.get(PrevisionRecetteRepository.class)
            .all()
            .filter("self.rubriqueBudgetaire=:id and self.year=:annee")
            .bind("id", id)
            .bind("annee", annee)
            .fetchOne();
    float b = 0;
    // rec.getMontant().compareTo(new BigDecimal(0))
    if (rec != null) {
      b = rec.getMontant().floatValue();
    }

    return b;
  }

  public Float getDepenseSommeRealise(int annee, long id) {
    List<HistoriqueBudgetaire> depenseList =
        Beans.get(HistoriqueBudgetaireRepository.class)
            .all()
            .filter("self.annee=:annee AND self.rubriqueBudgetaire =:id")
            .bind("annee", annee)
            .bind("id", id)
            .fetch();

    float somme = 0;

    for (HistoriqueBudgetaire c : depenseList) {
      somme += c.getMontant().floatValue();
    }
    return somme;
  }

  public Float getDepensePrvueOfRubrique(Long id, int annee) {
    HistoriqueBudgetaire rec =
        Beans.get(HistoriqueBudgetaireRepository.class)
            .all()
            .filter("self.rubriqueBudgetaire=:id AND  self.annee=:annee order by self.id asc")
            .bind("id", id)
            .bind("annee", annee)
            .fetchOne();
    float b = 0;
    if (rec != null) {
      b = rec.getMontantRubrique().floatValue();
    }
    return b;
  }

  public float getSommeOfCreditsCCISM(int annee) {
    List<CreditAchatCCIS> listCredis =
        Beans.get(CreditAchatCCISRepository.class)
            .all()
            .filter("self.annee=:annee")
            .bind("annee", annee)
            .fetch();
    float somme = 0;
    for (CreditAchatCCIS c : listCredis) {
      somme = somme + c.getMontant().floatValue();
    }
    return somme;
  }

  @Override
  public AppBudget getAppBudget() {
    return appBudgetRepo.all().fetchOne();
  }

  @Override
  public AppInvoice getAppInvoice() {
    return appInvoiceRepo.all().fetchOne();
  }

  @Transactional
  @Override
  public void generateAccountConfigurations() {

    List<Company> companies = companyRepo.all().filter("self.accountConfig is null").fetch();

    for (Company company : companies) {
      AccountConfig config = new AccountConfig();
      config.setCompany(company);
      accountConfigRepo.save(config);
    }
  }

  @Transactional
  public void saveMoveLine(MoveLine moveLine) {
    moveLineRepository.save(moveLine);
  }

  @Transactional
  public void savePointer(Pointer pointer) {
    pointerRepository.save(pointer);
  }

  @Override
  public AnnexeGenerale getAnnexeById(Long id) {
    return annexeGeneraleRepository.find(id);
  }

  @Override
  public User getUserById(Long id) {
    return userRepository.find(id);
  }

  @Override
  public Compte getCompteById(Long id) {
    return compteRepository.find(id);
  }

  public GestionRecette getRecetteById(Long id) {
    return gestionRecetteRepository.find(id);
  }

  public EncaissementSelect getEncaissementSelectById(Long id) {
    return encaissementSelectRepository.find(id);
  }

  @Transactional
  public HistoriqueCompte addhitoriqueCompte(
      int annee,
      String mois,
      Long idcompte,
      BigDecimal montant,
      boolean recette,
      String annexe_id,
      Long id_historique) {

    HistoriqueCompte h = null;
    if (id_historique < 0) {
      Compte cpt = compteRepository.find(idcompte);
      BigDecimal newMontant = cpt.getMontant().add(montant);
      cpt.setMontant(newMontant);
      cpt = compteRepository.save(cpt);

      h = new HistoriqueCompte();
      h.setMontant(cpt.getMontant());
      if (recette) h.setAction(1); // Crédit = Ajouter montant
      else h.setAction(2); // Débit = retirer montant
      h.setCompte(cpt);
      h.setRib(cpt.getRib());
      h.setDesignation(cpt.getDesignation());
      h.setDescription("Ajout Recette");
      h.setAnnexe(annexeGeneraleRepository.findByName(annexe_id));
      h.setDateTransaction(LocalDateTime.now());
      String m = (Integer.parseInt(mois) < 10) ? ("0" + mois) : mois;
      h.setMoisHistorique(m);
      h.setAnneeHistorique(annee);
      if (recette) h.setRecette(montant);
      else h.setDepense(montant);

      h = historiqueCompteRepository.save(h);

    } else if (id_historique > 0) {
      h = historiqueCompteRepository.find(id_historique);

      BigDecimal retirer = new BigDecimal(0);
      Compte c = compteRepository.find(idcompte);
      if (recette) retirer = h.getRecette();
      else retirer = h.getDepense();

      BigDecimal newMantant = c.getMontant().subtract(retirer);
      newMantant = newMantant.add(montant);
      c.setMontant(newMantant);
      compteRepository.save(c);

      List<HistoriqueCompte> ls =
          historiqueCompteRepository
              .all()
              .filter("self.dateTransaction >= ?1", h.getDateTransaction())
              .fetch();

      for (HistoriqueCompte tmp : ls) {
        if (recette) {

          BigDecimal mont = tmp.getMontant().subtract(retirer);
          mont = mont.add(montant);
          tmp.setMontant(mont);
          h.setRib(c.getRib());
          h.setDesignation(c.getDesignation());
          h.setDescription("Edit Recette");
          h.setAnnexe(annexeGeneraleRepository.findByName(annexe_id));
          h.setDateTransaction(LocalDateTime.now());
          String m = (Integer.parseInt(mois) < 10) ? ("0" + mois) : mois;
          h.setMoisHistorique(m);
          h.setAnneeHistorique(annee);
          historiqueCompteRepository.save(tmp);
        }
      }

      if (recette) {
        h.setRecette(montant);
      }
    }

    return h;
  }

  @Transactional
  public void saveRecette(GestionRecette recette) {
    gestionRecetteRepository.save(recette);
  }

  @Override
  @Transactional
  public void retraitMontantCompte(HistoriqueCompte h) {
    Compte compte = h.getCompte();
    BigDecimal montant = new BigDecimal(0);
    if (h.getRecette() != null) {
      montant = h.getRecette();
    } else {
      montant = h.getDepense();
    }

    BigDecimal mt_compt = compte.getMontant();
    mt_compt = mt_compt.subtract(montant);
    compte.setMontant(mt_compt);

    compteRepository.save(compte);

    List<HistoriqueCompte> ls =
        historiqueCompteRepository
            .all()
            .filter("self.dateTransaction >= ?1", h.getDateTransaction())
            .fetch();

    for (HistoriqueCompte tmp : ls) {
      BigDecimal mont = tmp.getMontant().subtract(montant);
      tmp.setMontant(mont);
      historiqueCompteRepository.save(tmp);
    }
  }

  @Transactional
  @Override
  public void deleteRecette(Long id) {
    GestionRecette tmp = gestionRecetteRepository.find(id);
    gestionRecetteRepository.remove(tmp);
  }

  @Override
  public String getMontantString(BigDecimal montant) {
    return ConvertNomreToLettres.getStringMontant(montant);
  }

  @Override
  public RegieTest getRegiById(Long id) {
    return regieTestRepository.find(id);
  }

  public String getDataCompte(Long compte, Integer annee) {
    List<String> data = new ArrayList<>();
    float soldeTotal = 0;
    float totalRecette = 0;
    float totalDepense = 0;

    //// data last Year
    int a = annee - 1;
    String anneePrecedente = String.valueOf(a);
    List<HistoriqueCompte> hct = getHistoriqueCompteByYearPrecedent(a, compte);
    String lastMonth = "SOLDE AU 31/12/" + anneePrecedente.substring(anneePrecedente.length() - 2);

    float TotalEncaissement = 0;
    float Solde = 0;
    if (hct.size() > 0) {
      HistoriqueCompte hc = hct.get(hct.size() - 1);
      Solde = hc.getMontant().floatValue();
      data.add(lastMonth);
      data.add(String.valueOf(hc.getMontant()));
      data.add(String.valueOf(0));
      data.add(String.valueOf(TotalEncaissement));
      data.add(String.valueOf(0));
      data.add(String.valueOf(Solde));

      /* totalRecette += hc.getRecette().floatValue();
      totalDepense += hc.getDepense().floatValue();*/
      soldeTotal += Solde;
    } else {
      data.add(lastMonth);
      data.add(String.valueOf(0));
      data.add(String.valueOf(0));
      data.add(String.valueOf(0));
      data.add(String.valueOf(0));
      data.add(String.valueOf(0));
    }

    // Data Janvier

    data.add("Janvier");
    data.add(String.valueOf(Solde));

    float sommeRecetteJ = getSommeRecetteByMois(annee, compte, "01");
    float sommeDepenseJ = getSommeDepenseByMois(annee, compte, "01");
    TotalEncaissement = Solde + sommeRecetteJ;
    Solde = TotalEncaissement - sommeDepenseJ;

    data.add(String.valueOf(sommeRecetteJ));
    data.add(String.valueOf(TotalEncaissement));
    data.add(String.valueOf(sommeDepenseJ));
    data.add(String.valueOf(Solde));

    totalRecette += sommeRecetteJ;
    totalDepense += sommeDepenseJ;
    soldeTotal += Solde;

    // Data Février

    data.add("Février");
    data.add(String.valueOf(Solde));

    float sommeRecetteF = getSommeRecetteByMois(annee, compte, "02");
    float sommeDepenseF = getSommeDepenseByMois(annee, compte, "02");
    TotalEncaissement = Solde + sommeRecetteF;
    Solde = TotalEncaissement - sommeDepenseF;

    data.add(String.valueOf(sommeRecetteF));
    data.add(String.valueOf(TotalEncaissement));
    data.add(String.valueOf(sommeDepenseF));
    data.add(String.valueOf(Solde));

    totalRecette += sommeRecetteF;
    totalDepense += sommeDepenseF;
    soldeTotal += Solde;

    // Data Mars

    data.add("Mars");
    data.add(String.valueOf(Solde));

    float sommeRecetteM = getSommeRecetteByMois(annee, compte, "03");
    float sommeDepenseM = getSommeDepenseByMois(annee, compte, "03");
    TotalEncaissement = Solde + sommeRecetteM;
    Solde = TotalEncaissement - sommeDepenseM;

    data.add(String.valueOf(sommeRecetteM));
    data.add(String.valueOf(TotalEncaissement));
    data.add(String.valueOf(sommeDepenseM));
    data.add(String.valueOf(Solde));

    totalRecette += sommeRecetteM;
    totalDepense += sommeDepenseM;
    soldeTotal += Solde;

    // Data Avril

    data.add("Avril");
    data.add(String.valueOf(Solde));

    float sommeRecetteAvril = getSommeRecetteByMois(annee, compte, "04");
    float sommeDepenseAvril = getSommeDepenseByMois(annee, compte, "04");
    TotalEncaissement = Solde + sommeRecetteAvril;
    Solde = TotalEncaissement - sommeDepenseAvril;

    data.add(String.valueOf(sommeRecetteAvril));
    data.add(String.valueOf(TotalEncaissement));
    data.add(String.valueOf(sommeDepenseAvril));
    data.add(String.valueOf(Solde));

    totalRecette += sommeRecetteAvril;
    totalDepense += sommeDepenseAvril;
    soldeTotal += Solde;

    // Data Mai

    data.add("Mai");
    data.add(String.valueOf(Solde));

    float sommeRecetteMai = getSommeRecetteByMois(annee, compte, "05");
    float sommeDepenseMai = getSommeDepenseByMois(annee, compte, "05");
    TotalEncaissement = Solde + sommeRecetteMai;
    Solde = TotalEncaissement - sommeDepenseMai;

    data.add(String.valueOf(sommeRecetteMai));
    data.add(String.valueOf(TotalEncaissement));
    data.add(String.valueOf(sommeDepenseMai));
    data.add(String.valueOf(Solde));

    totalRecette += sommeRecetteMai;
    totalDepense += sommeDepenseMai;
    soldeTotal += Solde;

    // Data Juin

    data.add("Juin");
    data.add(String.valueOf(Solde));

    float sommeRecetteJuin = getSommeRecetteByMois(annee, compte, "06");
    float sommeDepenseJuin = getSommeDepenseByMois(annee, compte, "06");
    TotalEncaissement = Solde + sommeRecetteJuin;
    Solde = TotalEncaissement - sommeDepenseJuin;

    data.add(String.valueOf(sommeRecetteJuin));
    data.add(String.valueOf(TotalEncaissement));
    data.add(String.valueOf(sommeDepenseJuin));
    data.add(String.valueOf(Solde));

    totalRecette += sommeRecetteJuin;
    totalDepense += sommeDepenseJuin;
    soldeTotal += Solde;

    // Data Juillet

    data.add("Juillet");
    data.add(String.valueOf(Solde));

    float sommeRecetteJuillet = getSommeRecetteByMois(annee, compte, "07");
    float sommeDepenseJuillet = getSommeDepenseByMois(annee, compte, "07");
    TotalEncaissement = Solde + sommeRecetteJuillet;
    Solde = TotalEncaissement - sommeDepenseJuillet;

    data.add(String.valueOf(sommeRecetteJuillet));
    data.add(String.valueOf(TotalEncaissement));
    data.add(String.valueOf(sommeDepenseJuillet));
    data.add(String.valueOf(Solde));

    totalRecette += sommeRecetteJuillet;
    totalDepense += sommeDepenseJuillet;
    soldeTotal += Solde;
    // Data Aout

    data.add("Aout");
    data.add(String.valueOf(Solde));

    float sommeRecetteAout = getSommeRecetteByMois(annee, compte, "08");
    float sommeDepenseAout = getSommeDepenseByMois(annee, compte, "08");
    TotalEncaissement = Solde + sommeRecetteAout;
    Solde = TotalEncaissement - sommeDepenseAout;

    data.add(String.valueOf(sommeRecetteAout));
    data.add(String.valueOf(TotalEncaissement));
    data.add(String.valueOf(sommeDepenseAout));
    data.add(String.valueOf(Solde));

    totalRecette += sommeRecetteAout;
    totalDepense += sommeDepenseAout;
    soldeTotal += Solde;

    // Data September

    data.add("Septembre");
    data.add(String.valueOf(Solde));

    float sommeRecetteSept = getSommeRecetteByMois(annee, compte, "09");
    float sommeDepenseSept = getSommeDepenseByMois(annee, compte, "09");
    TotalEncaissement = Solde + sommeRecetteSept;
    Solde = TotalEncaissement - sommeDepenseSept;

    data.add(String.valueOf(sommeRecetteSept));
    data.add(String.valueOf(TotalEncaissement));
    data.add(String.valueOf(sommeDepenseSept));
    data.add(String.valueOf(Solde));

    totalRecette += sommeRecetteSept;
    totalDepense += sommeDepenseSept;
    soldeTotal += Solde;

    // Data Octobre

    data.add("Octobre");
    data.add(String.valueOf(Solde));

    float sommeRecetteOct = getSommeRecetteByMois(annee, compte, "10");
    float sommeDepenseOct = getSommeDepenseByMois(annee, compte, "10");
    TotalEncaissement = Solde + sommeRecetteOct;
    Solde = TotalEncaissement - sommeDepenseOct;

    data.add(String.valueOf(sommeRecetteOct));
    data.add(String.valueOf(TotalEncaissement));
    data.add(String.valueOf(sommeDepenseOct));
    data.add(String.valueOf(Solde));

    totalRecette += sommeRecetteOct;
    totalDepense += sommeDepenseOct;
    soldeTotal += Solde;

    // Data Novembre

    data.add("Novembre");
    data.add(String.valueOf(Solde));

    float sommeRecetteNov = getSommeRecetteByMois(annee, compte, "11");
    float sommeDepenseNov = getSommeDepenseByMois(annee, compte, "11");
    TotalEncaissement = Solde + sommeRecetteNov;
    Solde = TotalEncaissement - sommeDepenseNov;

    data.add(String.valueOf(sommeRecetteNov));
    data.add(String.valueOf(TotalEncaissement));
    data.add(String.valueOf(sommeDepenseNov));
    data.add(String.valueOf(Solde));

    totalRecette += sommeRecetteNov;
    totalDepense += sommeDepenseNov;
    soldeTotal += Solde;

    // Data Decembre

    data.add("Decembre");
    data.add(String.valueOf(Solde));

    float sommeRecetteDec = getSommeRecetteByMois(annee, compte, "12");
    float sommeDepenseDec = getSommeDepenseByMois(annee, compte, "12");
    TotalEncaissement = Solde + sommeRecetteDec;
    Solde = TotalEncaissement - sommeDepenseDec;

    data.add(String.valueOf(sommeRecetteDec));
    data.add(String.valueOf(TotalEncaissement));
    data.add(String.valueOf(sommeDepenseDec));
    data.add(String.valueOf(Solde));

    totalRecette += sommeRecetteDec;
    totalDepense += sommeDepenseDec;
    soldeTotal += Solde;

    data.add(String.valueOf(totalRecette));
    data.add(String.valueOf(totalDepense));
    data.add(String.valueOf(soldeTotal));

    String x = StringUtils.join(data, "__");
    return x;
  }

  public List<HistoriqueCompte> getHistoriqueCompteByYearPrecedent(int a, Long compte) {
    return Beans.get(HistoriqueCompteRepository.class)
        .all()
        .filter("self.anneeHistorique=:a AND compte=:compte")
        .bind("a", a)
        .bind("compte", compte)
        .fetch();
  }

  public Float getSommeRecetteByMois(int a, Long compte, String mois) {
    List<HistoriqueCompte> hct =
        Beans.get(HistoriqueCompteRepository.class)
            .all()
            .filter("self.anneeHistorique=:a AND compte=:compte AND moisHistorique=:mois")
            .bind("a", a)
            .bind("compte", compte)
            .bind("mois", mois)
            .fetch();

    float somme = 0;

    for (HistoriqueCompte c : hct) {
      somme += c.getRecette().floatValue();
    }
    return somme;
  }

  public Float getSommeDepenseByMois(int a, Long compte, String mois) {
    List<HistoriqueCompte> hct =
        Beans.get(HistoriqueCompteRepository.class)
            .all()
            .filter("self.anneeHistorique=:a AND compte=:compte AND moisHistorique=:mois")
            .bind("a", a)
            .bind("compte", compte)
            .bind("mois", mois)
            .fetch();

    float somme = 0;

    for (HistoriqueCompte c : hct) {
      somme += c.getDepense().floatValue();
    }
    return somme;
  }

  public String getSituationMentuelleData(int mois, int year, String ls) {
    List<String> data = new ArrayList<>();
    BigDecimal total = new BigDecimal(0);

    List<RubriqueBudgetaireGenerale> ls_rub =
        rubriqueBudgetaireGeneraleRepository
            .all()
            .filter("self.id in (" + ls + ")")
            .order("id")
            .fetch();

    for (RubriqueBudgetaireGenerale tmp : ls_rub) {
      List<HistoriqueBudgetaire> ls_h =
          historiqueBudgetaireRepository
              .all()
              .filter(
                  "self.rubriqueBudgetaire=?1 and self.mois=?2 and self.annee=?3", tmp, mois, year)
              .fetch();
      BigDecimal somme = new BigDecimal(0);
      for (HistoriqueBudgetaire tmp_h : ls_h) {
        somme = somme.add(tmp_h.getMontant());
      }
      data.add(somme.setScale(2, RoundingMode.HALF_UP).toString());
      total = total.add(somme);
    }
    data.add(total.setScale(2, RoundingMode.HALF_UP).toString());

    String res = StringUtils.join(data, "__");
    return res;
  }

  public List<String> getSituationAnnuelleData(int year, List<String> ls1, List<String> ls2) {
    List<String> res = new ArrayList<>();
    for (int i = 1; i <= 12; i++) {}

    return res;
  }

  public BigDecimal getTotalFromAll(List<String> data_tab_1) {
    BigDecimal res = new BigDecimal(0);
    for (String tmp : data_tab_1) {
      String[] n = tmp.split("__");
      res = res.add(new BigDecimal(n[n.length - 1]));
    }
    return res.setScale(2, RoundingMode.HALF_UP);
  }

  @Transactional
  public void DemandeRetraitBudget(ActionRequest request, ActionResponse response) {
    BudgetParRubrique budgetRetrait = (BudgetParRubrique) request.getContext().get("budgetRetrait");
    BigDecimal montant =
        BigDecimal.valueOf(Double.parseDouble(request.getContext().get("ajout").toString()));
    BudgetParRubrique budgetAjout = (BudgetParRubrique) request.getContext().get("budgetAjout");

    BigDecimal montantRetrait =
        BigDecimal.valueOf(Double.parseDouble(budgetRetrait.getMontant().toString()));
    if (montant.compareTo(montantRetrait) > 0) {
      response.setFlash(
          "Le montant saisi est superieur au budget de la rubrique choisi, le budget restant de la rubrique est: "
              + montantRetrait
              + " Veuillez choisir un montant inferieur au budget");
    } else if (request.getContext().get("id") == null) {
      ModificationBudget modificationBudget = new ModificationBudget();
      modificationBudget.setAjout(montant);
      modificationBudget.setBudgetRetrait(budgetRetrait);
      modificationBudget.setBudgetAjout(budgetAjout);
      modificationBudget.setDateDemande(LocalDate.now());
      modificationBudgetRepository.save(modificationBudget);
      response.setView(
          ActionView.define("Modification du budget par rubrique")
              .model(ModificationBudget.class.getName())
              .add("grid", "ModificationBudget-grid")
              .add("form", "ModificationBudget-form")
              .map());
      response.setReload(true);
      response.setCanClose(true);
    } else {
      Long id = (Long) request.getContext().get("id");
      ModificationBudget modificationBudget = modificationBudgetRepository.find(id);
      modificationBudget.setAjout(montant);
      modificationBudget.setBudgetRetrait(budgetRetrait);
      modificationBudget.setBudgetAjout(budgetAjout);
      modificationBudget.setDateDemande(LocalDate.now());
      modificationBudgetRepository.save(modificationBudget);
      response.setView(
          ActionView.define("Modification du budget par rubrique")
              .model(ModificationBudget.class.getName())
              .add("grid", "ModificationBudget-grid")
              .add("form", "ModificationBudget-form")
              .map());
      response.setReload(true);
      response.setCanClose(true);
    }
  }

  public String calcule_somme_allMois(List<String> ls_som) {
    String res = "";
    List<BigDecimal> ls = new ArrayList<>();

    for (int i = 0; i < 12; i++) {
      String s = ls_som.get(i);
      String[] tab = s.split("__");
      for (int y = 0; y < tab.length; y++) {
        BigDecimal x = new BigDecimal(tab[y]);
        if (i == 0) {
          ls.add(x);
        } else {
          BigDecimal xx = x.add(ls.get(y));
          ls.set(y, xx);
        }
      }
    }

    res = StringUtils.join(ls, "__");
    return res;
  }

  @Transactional
  public void TraiterDemandeRetraitBudget(ActionRequest request, ActionResponse response) {
    Long id_Demande = (Long) request.getContext().get("id");
    int typeAction = Integer.valueOf(request.getContext().get("type").toString());
    String commentaire = request.getContext().get("commentaire").toString();
    ModificationBudget demandeModifBudget = modificationBudgetRepository.find(id_Demande);

    if (typeAction == 1) {
      demandeModifBudget.setType("1");
      demandeModifBudget.setCommentaire(commentaire);
      modificationBudgetRepository.save(demandeModifBudget);

      BudgetParRubrique budgetParRubriqueRetrait = demandeModifBudget.getBudgetRetrait();
      BudgetParRubrique budgetParRubriqueAjout = demandeModifBudget.getBudgetAjout();

      BigDecimal montantMoins =
          (demandeModifBudget.getBudgetRetrait().getMontant())
              .subtract(demandeModifBudget.getAjout());
      BigDecimal montantPlus =
          (demandeModifBudget.getBudgetAjout().getMontant()).add(demandeModifBudget.getAjout());
      budgetParRubriqueRetrait.setMontant(montantMoins);
      budgetParRubriqueRepository.save(budgetParRubriqueRetrait);
      budgetParRubriqueAjout.setMontant(montantPlus);
      budgetParRubriqueRepository.save(budgetParRubriqueAjout);

    } else {
      demandeModifBudget.setType("2");
      demandeModifBudget.setCommentaire(commentaire);
      modificationBudgetRepository.save(demandeModifBudget);
    }
    response.setView(
        ActionView.define("Demande de retrait")
            .model(ModificationBudget.class.getName())
            .add("grid", "Traitamant_ModificationBudget-grid")
            .add("form", "Traitement_ModificationBudget-form")
            .map());
    response.setReload(true);
    response.setCanClose(true);
  }

  @Override
  public String getDataInitial(int year, String ids) {
    String res = "";
    List<BigDecimal> ls = new ArrayList<>();
    List<RubriqueBudgetaireGenerale> rubriqueBudgetaireGenerale =
        rubriqueBudgetaireGeneraleRepository.all().filter("self.id in(" + ids + ")").fetch();
    for (RubriqueBudgetaireGenerale tmp : rubriqueBudgetaireGenerale) {
      HistoriqueBudgetaire b =
          historiqueBudgetaireRepository
              .all()
              .filter("self.annee=?1 and self.rubriqueBudgetaire=?2", year, tmp)
              .order("id")
              .fetchOne();
      if (b != null) {
        ls.add(b.getMontantRubrique().setScale(2, RoundingMode.HALF_UP));
      } else {
        ls.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
      }
    }
    BigDecimal som = ls.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    ls.add(som);
    res = StringUtils.join(ls, "__");
    return res;
  }

  @Override
  public String getDataVirement(int year, String ids) {
    List<BigDecimal> data = new ArrayList<>();
    List<RubriqueBudgetaireGenerale> rubriqueBudgetaireGenerale =
        rubriqueBudgetaireGeneraleRepository.all().filter("self.id in(" + ids + ")").fetch();
    for (RubriqueBudgetaireGenerale tmp : rubriqueBudgetaireGenerale) {
      BudgetParRubrique bpr =
          budgetParRubriqueRepository
              .all()
              .filter("self.rubriqueBudgetaire=?1 and year=?2", tmp, year)
              .order("-id")
              .fetchOne();
      BigDecimal sum = BigDecimal.ZERO;
      if (bpr != null) {
        List<ModificationBudget> ls =
            modificationBudgetRepository
                .all()
                .filter(
                    "self.type='1' and self.year=?1 and (self.budgetRetrait=?2 or self.budgetAjout=?2)",
                    year,
                    bpr)
                .fetch();
        for (ModificationBudget tmp2 : ls) {
          if (tmp2.getBudgetAjout().getId() == bpr.getId()) sum = sum.add(tmp2.getAjout());
          else if (tmp2.getBudgetRetrait().getId() == bpr.getId())
            sum = sum.subtract(tmp2.getAjout());
        }
      }
      data.add(sum.setScale(2, RoundingMode.HALF_UP));
    }
    BigDecimal tt = data.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    data.add(tt);
    String res = StringUtils.join(data, "__");
    return res;
  }

  @Override
  public String getMontantUpdate(String data_virement, String data_init) {
    List<BigDecimal> ls = new ArrayList<>();
    String[] tab1 = data_virement.split("__");
    String[] tab2 = data_init.split("__");
    for (int i = 0; i < tab1.length; i++) {
      BigDecimal x = new BigDecimal(tab1[i]);
      BigDecimal y = new BigDecimal(tab2[i]);
      ls.add(x.add(y));
    }
    return StringUtils.join(ls, "__");
  }

  @Override
  public String getMontantReliquat(String data_init_update, String data_som_dep) {
    String tab1[] = data_init_update.split("__");
    String tab2[] = data_som_dep.split("__");
    List<BigDecimal> ls = new ArrayList<>();
    for (int i = 0; i < tab1.length; i++) {
      BigDecimal x = new BigDecimal(tab1[i]);
      BigDecimal y = new BigDecimal(tab2[i]);
      ls.add(x.subtract(y));
    }
    return StringUtils.join(ls, "__");
  }

  public String getsuiviannuelle(int year, String ls1) {
    List<BigDecimal> data = new ArrayList<>();
    BigDecimal total = new BigDecimal(0);
    String res = "";

    List<RubriqueBudgetaireGenerale> ls_rub =
        rubriqueBudgetaireGeneraleRepository.all().filter("self.id in (" + ls1 + ")").fetch();
    BigDecimal somme = new BigDecimal(0);
    for (RubriqueBudgetaireGenerale tmp : ls_rub) {
      HistoriqueBudgetaire ls_h =
          historiqueBudgetaireRepository
              .all()
              .filter("self.rubriqueBudgetaire=?1 and self.annee=?2", tmp, year)
              .fetchOne();

      if (ls_h != null) {
        data.add(ls_h.getMontantRubrique());
        somme = somme.add(ls_h.getMontantRubrique());
      } else {
        data.add(BigDecimal.ZERO);
      }
    }
    data.add(somme);
    res = StringUtils.join(data, "__");
    return res;
  }

  public String getsuiviannuelle1(int year, String ls1) {
    List<BigDecimal> data = new ArrayList<>();
    BigDecimal total = new BigDecimal(0);
    String res = "";

    List<RubriqueBudgetaireGenerale> ls_rub =
        rubriqueBudgetaireGeneraleRepository
            .all()
            .filter("self.id in (" + ls1 + ")")
            .order("codeBudg")
            .fetch();
    BigDecimal somme = new BigDecimal(0);
    for (RubriqueBudgetaireGenerale tmp : ls_rub) {
      HistoriqueBudgetaire ls_h =
          historiqueBudgetaireRepository
              .all()
              .filter("self.rubriqueBudgetaire=?1 and self.annee=?2", tmp, year)
              .fetchOne();

      if (ls_h != null) {
        data.add(ls_h.getMontantRubrique());
        somme = somme.add(ls_h.getMontantRubrique());

      } else {
        data.add(BigDecimal.ZERO);
      }
    }
    data.add(somme);
    res = StringUtils.join(data, "__");
    return res;
  }

  public String getsuiviannuelleDepenses(int year, String ls2) {
    List<BigDecimal> data = new ArrayList<>();
    BigDecimal total = new BigDecimal(0);
    String res = "";

    List<RubriqueBudgetaireGenerale> ls_rub =
        rubriqueBudgetaireGeneraleRepository
            .all()
            .filter("self.id in (" + ls2 + ")")
            .order("codeBudg")
            .fetch();
    BigDecimal somme = new BigDecimal(0);
    for (RubriqueBudgetaireGenerale tmp : ls_rub) {
      HistoriqueBudgetaire ls_h =
          historiqueBudgetaireRepository
              .all()
              .filter("self.rubriqueBudgetaire=?1 and self.annee=?2", tmp, year)
              .fetchOne();

      if (ls_h != null) {
        data.add(ls_h.getMontantRubrique());
        somme = somme.add(ls_h.getMontantRubrique());
      } else {
        data.add(BigDecimal.ZERO);
      }
    }
    data.add(somme);
    res = StringUtils.join(data, "__");
    return res;
  }

  public String getsuiviannuelleDepenses1(int year, String ls2) {
    List<BigDecimal> data = new ArrayList<>();
    BigDecimal total = new BigDecimal(0);
    String res = "";

    List<RubriqueBudgetaireGenerale> ls_rub =
        rubriqueBudgetaireGeneraleRepository
            .all()
            .filter("self.id in (" + ls2 + ")")
            .order("codeBudg")
            .fetch();
    BigDecimal somme = new BigDecimal(0);
    for (RubriqueBudgetaireGenerale tmp : ls_rub) {
      HistoriqueBudgetaire ls_h =
          historiqueBudgetaireRepository
              .all()
              .filter("self.rubriqueBudgetaire=?1 and self.annee=?2", tmp, year)
              .fetchOne();

      if (ls_h != null) {
        data.add(ls_h.getMontantRubrique());
        somme = somme.add(ls_h.getMontantRubrique());
      } else {
        data.add(BigDecimal.ZERO);
      }
    }
    data.add(somme);
    res = StringUtils.join(data, "__");
    return res;
  }

  public String getHistoriqueCompteByYearnow(int year, Compte compte) {
    String res = "";
    BigDecimal somme = new BigDecimal(0);
    if (compte != null) {

      List<HistoriqueCompte> ls_h =
          Beans.get(HistoriqueCompteRepository.class)
              .all()
              .filter("self.id=:compte AND anneeHistorique=:year")
              .bind("compte", compte)
              .bind("year", year)
              .fetch();

      for (HistoriqueCompte c : ls_h) {
        if (ls_h != null) {
          somme = c.getMontant();
        }
      }
    } else {
      List<HistoriqueCompte> ls_h =
          Beans.get(HistoriqueCompteRepository.class)
              .all()
              .filter("self.anneeHistorique=:year")
              .bind("year", year)
              .fetch();

      for (HistoriqueCompte c : ls_h) {
        if (ls_h != null) {
          somme = somme.add(c.getMontant());
        }
      }
    }
    res = somme.toString();
    return res;
  }

  public String getanne(int year) {
    String res = "";
    String lastMonth = "Disponible à la trésorerie au 31/12/" + year;

    res = lastMonth;

    return res;
  }

  public String getannenow(int year) {
    String res = "";
    String lastMonth = "BUDGET AU TITRE DE L'ANNEE " + year;

    res = lastMonth;

    return res;
  }

  public String getannenowbuget(int year) {
    String res = "";
    String lastMonth = "budget" + year;

    res = lastMonth;

    return res;
  }

  public String getannebudget(int year) {
    String res = "";
    String lastMonth = "budget" + year + "modifie";

    res = lastMonth;

    return res;
  }

  public String getsuiviannuelleB(int year, String ls3) {
    List<BigDecimal> data = new ArrayList<>();
    BigDecimal total = new BigDecimal(0);
    String res = "";

    List<RubriqueBudgetaireGenerale> ls_rub =
        rubriqueBudgetaireGeneraleRepository
            .all()
            .filter("self.id in (" + ls3 + ")")
            .order("codeBudg")
            .fetch();
    BigDecimal somme = new BigDecimal(0);
    for (RubriqueBudgetaireGenerale tmp : ls_rub) {
      HistoriqueBudgetaire ls_h =
          historiqueBudgetaireRepository
              .all()
              .filter("self.rubriqueBudgetaire=?1 and self.annee=?2", tmp, year)
              .fetchOne();

      if (ls_h != null) {
        data.add(ls_h.getMontantRubrique());
        somme = somme.add(ls_h.getMontantRubrique());
      } else {
        data.add(BigDecimal.ZERO);
      }
    }
    data.add(somme);
    res = StringUtils.join(data, "__");
    return res;
  }

  public String getsuiviannuelle1last(int year, String ls3) {
    List<BigDecimal> data = new ArrayList<>();
    BigDecimal total = new BigDecimal(0);
    String res = "";

    List<RubriqueBudgetaireGenerale> ls_rub =
        rubriqueBudgetaireGeneraleRepository
            .all()
            .filter("self.id in (" + ls3 + ")")
            .order("codeBudg")
            .fetch();
    BigDecimal somme = new BigDecimal(0);
    for (RubriqueBudgetaireGenerale tmp : ls_rub) {
      HistoriqueBudgetaire ls_h =
          historiqueBudgetaireRepository
              .all()
              .filter("self.rubriqueBudgetaire=?1 and self.annee=?2", tmp, year)
              .fetchOne();

      if (ls_h != null) {
        data.add(ls_h.getMontantRubrique());
        somme = somme.add(ls_h.getMontantRubrique());
      } else {
        data.add(BigDecimal.ZERO);
      }
    }
    data.add(somme);
    res = StringUtils.join(data, "__");
    return res;
  }

  public String getsuiviannuelleDepenses2(int year, String ls4) {
    List<BigDecimal> data = new ArrayList<>();
    BigDecimal total = new BigDecimal(0);
    String res = "";

    List<RubriqueBudgetaireGenerale> ls_rub =
        rubriqueBudgetaireGeneraleRepository
            .all()
            .filter("self.id in (" + ls4 + ")")
            .order("codeBudg")
            .fetch();
    BigDecimal somme = new BigDecimal(0);
    for (RubriqueBudgetaireGenerale tmp : ls_rub) {
      HistoriqueBudgetaire ls_h =
          historiqueBudgetaireRepository
              .all()
              .filter("self.rubriqueBudgetaire=?1 and self.annee=?2", tmp, year)
              .fetchOne();
      if (ls_h != null) {
        data.add(ls_h.getMontantRubrique());
        somme = somme.add(ls_h.getMontantRubrique());

      } else {

        data.add(BigDecimal.ZERO);
      }
    }
    data.add(somme);
    res = StringUtils.join(data, "__");
    return res;
  }

  public String getsuiviannuelleDepenses21(int year, String ls4) {
    List<BigDecimal> data = new ArrayList<>();
    BigDecimal total = new BigDecimal(0);
    String res = "";

    List<RubriqueBudgetaireGenerale> ls_rub =
        rubriqueBudgetaireGeneraleRepository
            .all()
            .filter("self.id in (" + ls4 + ")")
            .order("codeBudg")
            .fetch();
    BigDecimal somme = new BigDecimal(0);
    for (RubriqueBudgetaireGenerale tmp : ls_rub) {
      HistoriqueBudgetaire ls_h =
          historiqueBudgetaireRepository
              .all()
              .filter("self.rubriqueBudgetaire=?1 and self.annee=?2", tmp, year)
              .fetchOne();
      if (ls_h != null) {
        data.add(ls_h.getMontantRubrique());
        somme = somme.add(ls_h.getMontantRubrique());
      } else {
        data.add(BigDecimal.ZERO);
      }
    }
    data.add(somme);
    res = StringUtils.join(data, "__");
    return res;
  }

  public BigDecimal gettotalFromList(String data) {
    BigDecimal res = BigDecimal.ONE;
    String[] tab = data.split("__");
    if (tab.length > 0) res = new BigDecimal(tab[tab.length - 1]);
    return res.setScale(2, RoundingMode.HALF_UP);
  }

  public String getDataVirementAy(int year, String ids) {
    List<BigDecimal> data = new ArrayList<>();
    List<RubriqueBudgetaireGenerale> rubriqueBudgetaireGenerale =
        rubriqueBudgetaireGeneraleRepository.all().filter("self.id in(" + ids + ")").fetch();
    for (RubriqueBudgetaireGenerale tmp : rubriqueBudgetaireGenerale) {
      BudgetParRubrique bpr =
          budgetParRubriqueRepository
              .all()
              .filter("self.rubriqueBudgetaire=?1 and year=?2", tmp, year)
              .order("-id")
              .fetchOne();
      BigDecimal sum = BigDecimal.ZERO;
      if (bpr != null) {
        List<ModificationBudget> ls =
            modificationBudgetRepository
                .all()
                .filter(
                    "self.type='1' and self.year=?1 and (self.budgetRetrait=?2 or self.budgetAjout=?2)",
                    year,
                    bpr)
                .fetch();
        for (ModificationBudget tmp2 : ls) {
          if (tmp2.getBudgetAjout().getId() == bpr.getId()) sum = sum.add(tmp2.getAjout());
          else if (tmp2.getBudgetRetrait().getId() == bpr.getId())
            sum = sum.subtract(tmp2.getAjout());
        }
      }
      data.add(sum.setScale(2, RoundingMode.HALF_UP));
    }
    BigDecimal tt = data.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    data.add(tt);
    String res = StringUtils.join(data, "__");
    return res;
  }

  public String getMontantUpdateAy(String data_virement, String data_init) {
    List<BigDecimal> ls = new ArrayList<>();
    String[] tab1 = data_virement.split("__");
    String[] tab2 = data_init.split("__");
    for (int i = 0; i < tab1.length; i++) {
      BigDecimal x = new BigDecimal(tab1[i]);
      BigDecimal z = new BigDecimal(tab2[i]);
      ls.add(x.add(z));
    }
    return StringUtils.join(ls, "__");
  }

  @Override
  public void appendDatatoFile(int annee, List<String> ls_ids_1, ReportSettings file) {
    String tot_ls1 = "";
    List<BigDecimal> ls_tot_ls = new ArrayList<>();
    for (int y = 0; y < ls_ids_1.size(); y++) {
      List<String> ls_som = new ArrayList<>();
      String tmp = ls_ids_1.get(y);
      String data_init = getDataInitial(annee, tmp);
      file.addParam("data_init_" + (y + 1), data_init);
      if (y == 0) ls_tot_ls.add(gettotalFromList(data_init));
      else ls_tot_ls.set(0, ls_tot_ls.get(0).add(gettotalFromList(data_init)));

      String data_virement = getDataVirement(annee, tmp);
      file.addParam("data_virement_" + (y + 1), data_virement);
      if (y == 0) ls_tot_ls.add(gettotalFromList(data_virement));
      else ls_tot_ls.set(1, ls_tot_ls.get(1).add(gettotalFromList(data_virement)));

      String data_update = getMontantUpdate(data_virement, data_init);
      file.addParam("data_update_" + (y + 1), data_update);
      if (y == 0) ls_tot_ls.add(gettotalFromList(data_update));
      else ls_tot_ls.set(2, ls_tot_ls.get(2).add(gettotalFromList(data_update)));

      for (int i = 1; i <= 12; i++) {
        String data = getSituationMentuelleData(i, annee, tmp);
        file.addParam(i + "_data_" + (y + 1), data);
        ls_som.add(data);
        if (y == 0) ls_tot_ls.add(gettotalFromList(data));
        else ls_tot_ls.set(2 + (i), ls_tot_ls.get(2 + i).add(gettotalFromList(data)));
      }

      String data_som = calcule_somme_allMois(ls_som);
      file.addParam("data_som_" + (y + 1), data_som);
      if (y == 0) ls_tot_ls.add(gettotalFromList(data_som));
      else ls_tot_ls.set(15, ls_tot_ls.get(15).add(gettotalFromList(data_som)));

      String data_reliquat = getMontantReliquat(data_update, data_som);
      file.addParam("data_reliquat_" + (y + 1), data_reliquat);
      if (y == 0) ls_tot_ls.add(gettotalFromList(data_som));
      else ls_tot_ls.set(16, ls_tot_ls.get(16).add(gettotalFromList(data_som)));
    }
    file.addParam("data_totalEx1", StringUtils.join(ls_tot_ls, "__"));
  }

  @Override
  public void appendDataInvestoFile(int annee, List<String> ls_ids_1, ReportSettings file) {
    String tot_ls1 = "";
    List<BigDecimal> ls_tot_ls = new ArrayList<>();
    for (int y = 0; y < ls_ids_1.size(); y++) {
      List<String> ls_som = new ArrayList<>();
      String tmp = ls_ids_1.get(y);
      String data_init = getDataInitial(annee, tmp);
      file.addParam("invt_data_init_" + (y + 1), data_init);
      if (y == 0) ls_tot_ls.add(gettotalFromList(data_init));
      else ls_tot_ls.set(0, ls_tot_ls.get(0).add(gettotalFromList(data_init)));

      String data_virement = getDataVirement(annee, tmp);
      file.addParam("invt_data_virement_" + (y + 1), data_virement);
      if (y == 0) ls_tot_ls.add(gettotalFromList(data_virement));
      else ls_tot_ls.set(1, ls_tot_ls.get(1).add(gettotalFromList(data_virement)));

      String data_update = getMontantUpdate(data_virement, data_init);
      file.addParam("invt_data_update_" + (y + 1), data_update);
      if (y == 0) ls_tot_ls.add(gettotalFromList(data_update));
      else ls_tot_ls.set(2, ls_tot_ls.get(2).add(gettotalFromList(data_update)));

      for (int i = 1; i <= 12; i++) {
        String data = getSituationMentuelleData(i, annee, tmp);
        file.addParam(i + "_invt_data_" + (y + 1), data);
        ls_som.add(data);
        if (y == 0) ls_tot_ls.add(gettotalFromList(data));
        else ls_tot_ls.set(2 + (i), ls_tot_ls.get(2 + i).add(gettotalFromList(data)));
      }

      String data_som = calcule_somme_allMois(ls_som);
      file.addParam("invt_data_som_" + (y + 1), data_som);
      if (y == 0) ls_tot_ls.add(gettotalFromList(data_som));
      else ls_tot_ls.set(15, ls_tot_ls.get(15).add(gettotalFromList(data_som)));

      String data_reliquat = getMontantReliquat(data_update, data_som);
      file.addParam("invt_data_reliquat_" + (y + 1), data_reliquat);
      if (y == 0) ls_tot_ls.add(gettotalFromList(data_reliquat));
      else ls_tot_ls.set(16, ls_tot_ls.get(16).add(gettotalFromList(data_reliquat)));
    }
    file.addParam("data_totalEx2", StringUtils.join(ls_tot_ls, "__"));
  }

  public BigDecimal checkMontant(LocalDate d1, LocalDate d2) {
    BigDecimal res =
        RunSqlRequestForMe.runSqlRequest_Bigdecimal(
            "select sum(montant) from account_encaissement where dateencaissement>='"
                + d1
                + "' and dateencaissement < '"
                + d2
                + "'");
    return res;
  }

  public Encaissement getEncaissementById(Long id) {
    return encaissementRepository.find(id);
  }

  @javax.transaction.Transactional
  public void setVersed(boolean b, Long id) {
    Encaissement r = encaissementRepository.find(id);
    r.setVersed(b);
    encaissementRepository.save(r);
  }

  @Override
  public List<Encaissement> getlistEncaissementNotInIds(String ids) {
    String req = "self.id not in (" + ids + ")";
    return encaissementRepository.all().filter(req).fetch();
  }

  @Transactional
  public HistoriqueBudgetaire addRubrique(
      int annee, int mois, String annexe_id, Long rubrique_id, BigDecimal montant) {

    RubriqueBudgetaireGenerale r = rubriqueBudgetaireGeneraleRepository.find(rubrique_id);
    BudgetParRubrique b =
        budgetParRubriqueRepository
            .all()
            .filter("self.year=?1 and self.rubriqueBudgetaire=?2", annee, r)
            .fetchOne();
    HistoriqueBudgetaire historiqueBudgetaire = new HistoriqueBudgetaire();
    historiqueBudgetaire.setAnnee(annee);
    historiqueBudgetaire.setMois(mois);
    historiqueBudgetaire.setAnnexe(annexeGeneraleRepository.findByName(annexe_id));
    historiqueBudgetaire.setDateEx(LocalDate.now());
    historiqueBudgetaire.setRubriqueBudgetaire(r);
    if (b == null) {
      historiqueBudgetaire.setMontant(montant);
      historiqueBudgetaire.setMontantRubrique(montant);
      historiqueBudgetaire = historiqueBudgetaireRepository.save(historiqueBudgetaire);

      BudgetParRubrique budget = new BudgetParRubrique();
      budget.setName(r.getName());
      budget.setRubriqueBudgetaire(r);
      budget.setMontant(montant);
      budget.setYear(annee);
      budget.setHistoriqueBudgetaire(historiqueBudgetaire);
      budgetParRubriqueRepository.save(budget);
    } else {

      BigDecimal m = b.getMontant().add(montant);
      b.setMontant(m);
      historiqueBudgetaire.setMontant(montant);
      historiqueBudgetaire.setMontantRubrique(m);
      historiqueBudgetaire = historiqueBudgetaireRepository.save(historiqueBudgetaire);
      b.setHistoriqueBudgetaire(historiqueBudgetaire);
      budgetParRubriqueRepository.save(b);
    }

    return historiqueBudgetaire;
  }

  @Override
  public AnnexeGenerale getDefaultAnnexeCentrale() {
    AnnexeGenerale a1 =
        annexeGeneraleRepository.all().filter("self.typeCentral=?1", true).fetchOne();
    if (a1 != null) return a1;

    AnnexeGenerale a2 =
        annexeGeneraleRepository.all().filter("self.name like '%Beni Mellal%'").fetchOne();
    return a2;
  }

  @Override
  public User getDefaultUserCentrale() {
    User a1 = userRepository.all().filter("self.allencaissement=?1", true).fetchOne();
    return a1;
  }

  /*  @Override
  public BigDecimal getTotalEncaissement(Long id_annexe){
    AnnexeGenerale annexe = annexeGeneraleRepository.find(id_annexe);
    List<Encaissement> encaissements = encaissementRepository.all().filter("self.versed=?1 and self.annexe=?2 ",false, annexe).fetch();
    BigDecimal r = BigDecimal.ZERO;
    if(encaissements==null)
      return r;
    for(Encaissement en:encaissements){
      r=r.add(en.getMontant());
    }
    return r;
  }*/

  @Override
  @Transactional
  public void AnnulerEncaissement(Long id, String commentaire) {
    Encaissement ec = getEncaissementById(id);
    ec.setEtatEncaissement(true);
    ec.setCommentaire(commentaire);
    encaissementRepository.save(ec);
  }

  @Override
  @Transactional
  public void modifierBudgetCascade(Virements v) {
    // retrait en cacsade
    RubriquesBudgetaire retrait = rubriquesBudgetaireRepository.find(v.getBudget_retrait().getId());
    retraitEnCascadeMontantBudget(retrait, v.getMontant());
    ajoutEnCascadeMontantBudget(v.getBudget_ajout(), v.getMontant());
  }

  @Transactional
  private void retraitEnCascadeMontantBudget(RubriquesBudgetaire retrait, BigDecimal montant) {
    RubriquesBudgetaire b = rubriquesBudgetaireRepository.find(retrait.getId());
    b.setMontant_budget(b.getMontant_budget().subtract(montant));
    rubriquesBudgetaireRepository.save(b);
    if (b.getBudgetParent() != null) {
      modifierBudgetcascadeParent(b.getBudgetParent(), montant, "reduce");
    }
  }

  @Transactional
  private void ajoutEnCascadeMontantBudget(RubriquesBudgetaire retrait, BigDecimal montant) {
    RubriquesBudgetaire b = rubriquesBudgetaireRepository.find(retrait.getId());
    b.setMontant_budget(b.getMontant_budget().add(montant));
    rubriquesBudgetaireRepository.save(b);
    if (b.getBudgetParent() != null) {
      modifierBudgetcascadeParent(b.getBudgetParent(), montant, "add");
    }
  }

  @Transactional
  private void modifierBudgetcascadeParent(
      RubriquesBudgetaire budget, BigDecimal montant, String operation) {
    RubriquesBudgetaire b = rubriquesBudgetaireRepository.find(budget.getId());
    if (operation.equals("add")) {
      b.setMontant_total_children(b.getMontant_total_children().add(montant));
    } else {
      b.setMontant_total_children(b.getMontant_total_children().subtract(montant));
    }
    rubriquesBudgetaireRepository.save(b);
    if (b.getBudgetParent() != null) {
      modifierBudgetcascadeParent(b.getBudgetParent(), montant, operation);
    }
  }

  @Override
  @Transactional
  public void deleteListMove(List<Move> list_to_delete) {
    String req = "";
    for (Move tmp : list_to_delete) {
      req += "delete from account_groupe_move_move_move where move_move = " + tmp.getId() + ";";
      req += "delete from account_move where id = " + tmp.getId() + ";";
      RunSqlRequestForMe.runSqlRequest(req);
    }
  }
}
