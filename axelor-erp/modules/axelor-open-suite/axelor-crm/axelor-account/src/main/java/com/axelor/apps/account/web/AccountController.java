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

package com.axelor.apps.account.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.account.db.*;
import com.axelor.apps.account.db.repo.*;
import com.axelor.apps.account.exception.IExceptionMessage;
import com.axelor.apps.account.report.IReport;
import com.axelor.apps.account.service.AccountService;
import com.axelor.apps.account.service.app.AppAccountServiceImpl;
import com.axelor.apps.account.translation.ITranslation;
import com.axelor.apps.base.db.Period;
import com.axelor.apps.base.db.repo.PeriodRepository;
import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.repo.RubriquesBudgetaireRepository;
import com.axelor.apps.configuration.db.repo.VersionRubriqueBudgetaireRepository;
import com.axelor.apps.configuration.service.ConvertNomreToLettres;
import com.axelor.apps.configuration.service.MyConfigurationServiceImpl;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.common.ObjectUtils;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class AccountController {

  @Inject AppAccountServiceImpl appAccountService;

  public void DemandeRetraitBudget(ActionRequest request, ActionResponse response) {
    appAccountService.DemandeRetraitBudget(request, response);
  }

  public void showAnnexe(ActionRequest request, ActionResponse response) {
    String choix = request.getContext().get("choixAnnexe").toString();
    if (choix.equals("all")) {
      response.setHidden("annexe", true);
    } else {
      response.setHidden("annexe", false);
    }
  }

  public void TraiterDemandeRetraitBudget(ActionRequest request, ActionResponse response) {
    appAccountService.TraiterDemandeRetraitBudget(request, response);
  }

  public void ImprimerCompteArreteSuite(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String anneestring = (String) request.getContext().get("anneeArreteCompte");
    float annee = Float.parseFloat(anneestring);
    // AnnexeGenerale annexeGenerale = (AnnexeGenerale) request.getContext().get("annexe");
    // Long idAnnexeL = annexeGenerale.getId();
    // float idAnnexe = (float) idAnnexeL;
    float idAnnexe = 1;
    String fileLink =
        ReportFactory.createReport(IReport.ArreteDeCompteSuite, "ArreteDeCompte")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("annexe", idAnnexe)
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("Le fichier d'arrete des comptes").add("html", fileLink).map());
  }

  public void ImprimerCompteArrete(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String anneestring = (String) request.getContext().get("anneeArreteCompte");
    int annee = Integer.valueOf(anneestring);
    int anneePrecedente = annee - 1;
    // Compte compte = (Compte) request.getContext().get("rib");
    // Long idCompte = compte.getId();
    // String ribCompte = compte.getRib();
    Long idCompte = 1L;
    String data = appAccountService.getDataArreteCompt(idCompte, annee);
    String fileLink =
        ReportFactory.createReport(IReport.ArreteCompte, "ArreteDeCompte")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("anneePrecedente", anneePrecedente)
            .addParam("data", data)
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("Le fichier d'arrete des comptes").add("html", fileLink).map());
  }

  public void computeBalance(ActionRequest request, ActionResponse response) {
    try {
      Account account = request.getContext().asType(Account.class);
      if (account.getId() == null) {
        return;
      }
      account = Beans.get(AccountRepository.class).find(account.getId());
      BigDecimal balance = BigDecimal.ZERO;
      if (request.getContext().get("id_groupMouve") == null) {
        balance =
            Beans.get(AccountService.class)
                .computeBalance(account, AccountService.BALANCE_TYPE_DEBIT_BALANCE);
      } else {
        balance =
            Beans.get(AccountService.class)
                .computeBalance_v2(
                    account,
                    AccountService.BALANCE_TYPE_DEBIT_BALANCE,
                    Beans.get(GroupeMoveRepository.class)
                        .find(Long.valueOf((Integer) request.getContext().get("id_groupMouve"))));
      }
      if (balance.compareTo(BigDecimal.ZERO) >= 0) {
        response.setAttr("$balanceBtn", "title", I18n.get(ITranslation.ACCOUNT_DEBIT_BALANCE));
      } else {
        balance = balance.multiply(new BigDecimal(-1));
        response.setAttr("$balanceBtn", "title", I18n.get(ITranslation.ACCOUNT_CREDIT_BALANCE));
      }
      response.setValue("$balanceBtn", balance);
      if (request.getContext().get("id_groupMouve") != null) {
        response.setHidden("$periode", false);
        response.setValue(
            "$periode",
            Beans.get(GroupeMoveRepository.class)
                .find(Long.valueOf((Integer) request.getContext().get("id_groupMouve")))
                .getPeriod());
      } else {
        response.setHidden("$periode", true);
      }

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void checkIfCodeAccountAlreadyExistForCompany(
      ActionRequest request, ActionResponse response) {
    try {
      Account account = request.getContext().asType(Account.class);
      Long accountId = account.getId();
      if (accountId == null) {
        accountId = 0L;
      }
      List<Account> sameAccountList =
          Beans.get(AccountRepository.class)
              .all()
              .filter(
                  "self.company = ?1 AND self.code = ?2 AND self.id != ?3",
                  account.getCompany(),
                  account.getCode(),
                  accountId)
              .fetch();
      if (!ObjectUtils.isEmpty(sameAccountList)) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(IExceptionMessage.ACCOUNT_CODE_ALREADY_IN_USE_FOR_COMPANY),
            account.getCode(),
            account.getCompany().getName());
      }

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void refresh_date(ActionRequest request, ActionResponse response) {
    String date = request.getContext().get("dateRecette").toString();
    if (date.equals("") || date == null) {
      response.setError("date incorrecte");
    }
    String[] tab_mois =
        new String[] {
          "Janvier",
          "Février",
          "Mars",
          "Avril",
          "Mai",
          "Juin",
          "Juillet",
          "Août",
          "Septembre",
          "Octobre",
          "Novembre",
          "Décembre"
        };
    String[] tab = date.split("-");
    response.setValue("anneeRecette", Integer.valueOf(tab[0]));
    response.setValue("moisRecette", tab_mois[Integer.valueOf(tab[1]) - 1]);
  }

  public void imprimerGestionRecette(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String annee = (String) request.getContext().get("anneeRecette");
    String anneeP = (String) request.getContext().get("anneeRecette");
    int a = Integer.parseInt(anneeP) - 1;
    String anneePrecedente = String.valueOf(a);
    String lastMonth = "31/12/" + anneePrecedente.substring(anneePrecedente.length() - 2);
    Compte compte = (Compte) request.getContext().get("rib");
    Long idCompte = compte.getId();
    String ribCompte = compte.getRib();
    int annexe = compte.getAnnexe().getId().intValue();
    String fileLink =
        ReportFactory.createReport(IReport.GestionRecette, "SituationRecette")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("compte", idCompte)
            .addParam("anneePrecedente", anneePrecedente)
            .addParam("compteString", ribCompte)
            .addParam("lastMonth", lastMonth)
            .addParam("annexe", annexe)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Situation Recette").add("html", fileLink).map());
  }

  public void saveLettrage(ActionRequest request, ActionResponse response) {
    BigDecimal debit = BigDecimal.valueOf(0);
    BigDecimal credit = BigDecimal.valueOf(0);
    String[] alphabets = {
      "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
      "T", "U", "V", "X", "Z", "AA", "BB", "CC", "DD", "EE", "FF", "GG", "HH", "II", "JJ", "KK",
      "LL", "MM", "NN", "OO", "PP", "QQ", "RR", "SS", "TT", "UU", "VV", "XX", "ZZ"
    };
    @SuppressWarnings("unchecked")
    List<Integer> idList = (List<Integer>) request.getContext().get("_ids");
    try {
      if (idList != null) {
        for (Integer it : idList) {
          MoveLine moveLine = Beans.get(MoveLineRepository.class).find(it.longValue());
          debit = debit.add(moveLine.getDebit());
          credit = credit.add(moveLine.getCredit());
        }
      }
      if (idList != null) {
        if (debit.equals(credit)) {
          Pointer pointer = Beans.get(PointerRepository.class).find(Integer.valueOf(1).longValue());
          for (Integer it : idList) {
            if (pointer.getCode() != 47) {
              pointer.setCode(pointer.getCode() + 1);
            } else {
              pointer.setCode(0);
            }
            MoveLine moveLine = Beans.get(MoveLineRepository.class).find(it.longValue());
            moveLine.setOrigin(alphabets[pointer.getCode()]);
            appAccountService.saveMoveLine(moveLine);
            appAccountService.savePointer(pointer);
          }
          response.setReload(true);
        }
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void update_compte(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    GestionRecette recette = appAccountService.getRecetteById(id);
    Set<Encaissement> list = recette.getEncaissement();
    for (Encaissement s : list) {
      s.setVersed(true);
      // creer historique budgetaire
      HistoriqueBudgetaire rub =
          appAccountService.addRubrique(
              Integer.parseInt(recette.getAnneeRecette()),
              recette.getDateRecette().getMonthValue(),
              s.getAnnexe().getName(),
              recette.getRubrique().getId(),
              recette.getMontant());
      recette.setHistoriqueRubrique(rub);
      if (recette.getHistorique() == null) {
        // creer historique compte
        HistoriqueCompte h =
            appAccountService.addhitoriqueCompte(
                Integer.parseInt(recette.getAnneeRecette()),
                String.valueOf(recette.getDateRecette().getMonthValue()),
                recette.getRib().getId(),
                recette.getMontant(),
                true,
                s.getAnnexe().getName(),
                -1l);
        recette.setHistorique(h);
        appAccountService.saveRecette(recette);
      } else {
        HistoriqueCompte h =
            appAccountService.addhitoriqueCompte(
                Integer.parseInt(recette.getAnneeRecette()),
                String.valueOf(recette.getDateRecette().getMonthValue()),
                recette.getRib().getId(),
                recette.getMontant(),
                true,
                s.getAnnexe().getName(),
                recette.getHistorique().getId());
      }
    }
  }

  public void affecterSelectEncaissement(ActionRequest request, ActionResponse response) {
    EncaissementSelect m = request.getContext().asType(EncaissementSelect.class);
    response.setValue("annexSelect", m.getEncaissementSelect().getAnnexe());
    response.setValue("montant", m.getEncaissementSelect().getMontant());
    response.setValue("dateEncaisSelect", m.getEncaissementSelect().getDateencaissement());
  }

  public void imprimerArreteCompte(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String anneestring = (String) request.getContext().get("anneeRecette");
    float annee = Float.parseFloat(anneestring);
    AnnexeGenerale annexeGenerale = (AnnexeGenerale) request.getContext().get("annexe");
    Long idAnnexeL = annexeGenerale.getId();
    float idAnnexe = (float) idAnnexeL;
    String fileLink =
        ReportFactory.createReport(IReport.ArreteDeCompte, "ArreteDeCompte")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("annexe", idAnnexe)
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("Le fichier d'arrete des comptes").add("html", fileLink).map());
  }

  public void DeleteRecette(ActionRequest request, ActionResponse response) throws AxelorException {
    Long id = (Long) request.getContext().get("id");
    GestionRecette recette = appAccountService.getRecetteById(id);
    HistoriqueCompte h = recette.getHistorique();
    appAccountService.retraitMontantCompte(h);
    appAccountService.deleteRecette(recette.getId());
    response.setFlash("Montant retirer avec succès");
    response.setReload(true);
  }

  public void getsituation_mensuel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    if (Integer.valueOf(request.getContext().get("mois").toString()) == 0
        || Integer.valueOf(request.getContext().get("annee").toString()) == 0) {
      String ul = "<ul>";
      ul +=
          Integer.valueOf(request.getContext().get("mois").toString()) == 0 ? "<li>Mois</li>" : "";
      ul +=
          Integer.valueOf(request.getContext().get("annee").toString()) == 0
              ? "<li>Année</li>"
              : "";
      ul += "</ul>";
      response.setError("<p>les champs suivants sont vides : </p>" + ul);
      return;
    }
    int mois = Integer.valueOf(request.getContext().get("mois").toString());
    int annee = Integer.valueOf(request.getContext().get("annee").toString());
    String ls1 = "4,5,6,7,10,11,14,15,16,17,18,19,20,21,22,23,24";
    String ls2 =
        "27,28,29,31,34,35,36,38,41,42,43,44,46,48,49,50,53,54,"
            + "55,57,58,59,60,62,63,64,67,68,69,70,71,72,75,76,77,79,80,82,83,84,86,88,89,90,91,92,"
            + "96,97,98,99,100,101,105,107,108,109,111,112,113,115,116,117,118,119,120,121,122,123,124";
    String ls3 = "127,129,130";
    String ls4 =
        "134,135,136,137,139,141,142,143,144,145,146,147,148,152,153,155,156,159,160,161,162,164,165";
    String ls5 = "167,168";
    String ls6 = "172,173";
    String ls7 = "177,178,179,180,181";
    String ls8 = "183,184,185,186,187,188,189,190,191,192,193";
    String ls_terr = "200,201,202,203";
    String ls_contr = "207,208,209,211,215,216";
    String ls_trans = "218,219,220";
    String ls_mobiler = "222,223,224,226,227,228,230,231";
    String ls_particip = "235,236,237,238,239,240,241,242,243";
    String ls_invest = "245,246";
    String ls_arbitage = "248,249";
    String ls_parc = "252,253,254,255,256,257";
    String ls_foire = "259,260,261,262";
    String ls_zi = "264,265";
    String ls_ecole = "272,273,274,275,276,277";
    String ls_center_miss = "279,280,281,282,283";
    String ls_center_chkaf = "285,286,287,288";
    String ls_planDev = "291,292,293,294,295,296,298";
    String data_janv = appAccountService.getSituationMentuelleData(mois, annee, ls1);
    String data_2 = appAccountService.getSituationMentuelleData(mois, annee, ls2);
    String data_3 = appAccountService.getSituationMentuelleData(mois, annee, ls3);
    String data_4 = appAccountService.getSituationMentuelleData(mois, annee, ls4);
    String data_5 = appAccountService.getSituationMentuelleData(mois, annee, ls5);
    String data_6 = appAccountService.getSituationMentuelleData(mois, annee, ls6);
    String data_7 = appAccountService.getSituationMentuelleData(mois, annee, ls7);
    String data_8 = appAccountService.getSituationMentuelleData(mois, annee, ls8);
    List<String> data_tab_1 =
        Arrays.asList(data_janv, data_2, data_3, data_4, data_5, data_6, data_7, data_8);
    BigDecimal data_total = appAccountService.getTotalFromAll(data_tab_1);
    String data_9 = appAccountService.getSituationMentuelleData(mois, annee, ls_terr);
    String data_10 = appAccountService.getSituationMentuelleData(mois, annee, ls_contr);
    String data_11 = appAccountService.getSituationMentuelleData(mois, annee, ls_trans);
    String data_12 = appAccountService.getSituationMentuelleData(mois, annee, ls_mobiler);
    String data_13 = appAccountService.getSituationMentuelleData(mois, annee, ls_particip);
    String data_14 = appAccountService.getSituationMentuelleData(mois, annee, ls_invest);
    String data_15 = appAccountService.getSituationMentuelleData(mois, annee, ls_arbitage);
    String data_16 = appAccountService.getSituationMentuelleData(mois, annee, ls_parc);
    String data_17 = appAccountService.getSituationMentuelleData(mois, annee, ls_foire);
    String data_18 = appAccountService.getSituationMentuelleData(mois, annee, ls_zi);
    String data_19 = appAccountService.getSituationMentuelleData(mois, annee, "267");
    String data_20 = appAccountService.getSituationMentuelleData(mois, annee, ls_ecole);
    String data_21 = appAccountService.getSituationMentuelleData(mois, annee, ls_center_miss);
    String data_22 = appAccountService.getSituationMentuelleData(mois, annee, ls_center_chkaf);
    String data_23 = appAccountService.getSituationMentuelleData(mois, annee, ls_planDev);
    List<String> data_tab_2 =
        Arrays.asList(
            data_9, data_10, data_11, data_12, data_13, data_14, data_15, data_16, data_17, data_18,
            data_19, data_20, data_21, data_22, data_23);
    BigDecimal data_total2 = appAccountService.getTotalFromAll(data_tab_2);
    String[] mois_tab = {
      "Janvier",
      "Février",
      "Mars",
      "Avril",
      "Mai",
      "Juin",
      "Juillet",
      "Août",
      "Septembre",
      "Octobre",
      "Novembre",
      "Décembre"
    };
    String fileLink =
        ReportFactory.createReport(IReport.DepenceMensuel, "Situation_Mensuel")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("data_janv", data_janv)
            .addParam("data_2", data_2)
            .addParam("data_3", data_3)
            .addParam("data_4", data_4)
            .addParam("data_5", data_5)
            .addParam("data_6", data_6)
            .addParam("data_7", data_7)
            .addParam("data_8", data_8)
            .addParam("my_total", data_total)
            .addParam("current_mois", mois_tab[mois - 1])
            .addParam("data_9", data_9)
            .addParam("data_10", data_10)
            .addParam("data_11", data_11)
            .addParam("data_12", data_12)
            .addParam("data_13", data_13)
            .addParam("data_14", data_14)
            .addParam("data_15", data_15)
            .addParam("data_16", data_16)
            .addParam("data_17", data_17)
            .addParam("data_18", data_18)
            .addParam("data_19", data_19)
            .addParam("data_20", data_20)
            .addParam("data_21", data_21)
            .addParam("data_22", data_22)
            .addParam("data_23", data_23)
            .addParam("my_total_end", data_total2)
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("Le fichier d'arrete des comptes").add("html", fileLink).map());
  }

  public void getsituation_anuelle(ActionRequest request, ActionResponse response)
      throws AxelorException {
    if (Integer.valueOf(request.getContext().get("annee").toString()) == 0) {
      String ul = "<ul>";
      ul +=
          Integer.valueOf(request.getContext().get("annee").toString()) == 0
              ? "<li>Année</li>"
              : "";
      ul += "</ul>";
      response.setError("<p>les champs suivants sont vides : </p>" + ul);
      return;
    }
    int annee = Integer.valueOf(request.getContext().get("annee").toString());
    if (annee > LocalDate.now().getYear()) {
      response.setFlash("Merci de saisir une année valide");
      return;
    }
    List<String> ls_ids_1 = new ArrayList<>();
    ls_ids_1.add("4,5,6,7,10,11,14,15,16,17,18,19,20,21,22,23,24");
    ls_ids_1.add(
        "27,28,29,31,34,35,36,38,41,42,43,44,46,49,50,"
            + "53,54,55,57,58,59,60,62,63,64,67,68,69,70,71,72,75,76,"
            + "77,79,80,82,83,84,86,88,89,90,91,92,96,97,98,99,100,101,"
            + "105,107,108,109,111,112,113,115,116,117,118,119,120,121,122,123,124");
    ls_ids_1.add("127,129,130");
    ls_ids_1.add(
        "134,135,136,137,139,141,142,143,144,145,146,147,148,152,153,155,156,159,160,161,162,164,165");
    ls_ids_1.add("167,168");
    ls_ids_1.add("172,173");
    ls_ids_1.add("177,178,179,180,181");
    ls_ids_1.add("183,184,185,186,187,188,189,190,191,192,193");
    List<String> ls_ids_2 = new ArrayList<>();
    ls_ids_2.add("200,201,202,203");
    ls_ids_2.add("207,208,209,211,215,216");
    ls_ids_2.add("218,219,220");
    ls_ids_2.add("222,223,224,226,227,228,230,231");
    ls_ids_2.add("235,236,237,238,239,240,241,242,243");
    ls_ids_2.add("245,246");
    ls_ids_2.add("248,249");
    ls_ids_2.add("252,253,254,255,256,257");
    ls_ids_2.add("259,260,261,262");
    ls_ids_2.add("264,265");
    ls_ids_2.add("267");
    ls_ids_2.add("272,273,274,275,276,277");
    ls_ids_2.add("279,280,281,282,283");
    ls_ids_2.add("285,286,287,288");
    ls_ids_2.add("291,292,293,294,295,296,298");
    ReportSettings file = ReportFactory.createReport(IReport.DepenceAnnuelle, "Situation_Anuelle");
    file.addParam("Locale", ReportSettings.getPrintingLocale(null));
    file.addParam("year", annee);
    appAccountService.appendDatatoFile(annee, ls_ids_1, file);
    appAccountService.appendDataInvestoFile(annee, ls_ids_2, file);
    String fileLink = file.generate().getFileLink();
    response.setView(ActionView.define("Situation annuelle").add("html", fileLink).map());
  }

  public void imprimerGestionTresorerie(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer anneeP = Integer.valueOf(request.getContext().get("anneetresorerie").toString());
    Compte compte = (Compte) request.getContext().get("rib");
    Long idCompte = compte.getId();
    String ribCompte = compte.getRib();
    String data = appAccountService.getDataCompte(idCompte, anneeP);
    String fileLink =
        ReportFactory.createReport(IReport.TRESORERIE, "SituationTresorerie")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("compte", idCompte)
            .addParam("data", data)
            .addParam("compteString", ribCompte)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Situation Tresorerie").add("html", fileLink).map());
  }

  public void imprimersuiviannuelle(ActionRequest request, ActionResponse response)
      throws AxelorException {
    if (request.getContext().get("annee") == null
        || Integer.valueOf(request.getContext().get("annee").toString()) == 0) {
      String ul = "<ul>";
      ul += "<li>Année</li>";
      ul += "</ul>";
      response.setError("<p>les champs suivants sont vides : </p>" + ul);
      return;
    }
    int annee = Integer.valueOf(request.getContext().get("annee").toString());
    String ls_ids_1 = "302,93,303,304,305,306";
    String ls_ids_2 = "2,25,125,131,166,175,182";
    String ls_ids_3 = "307,308,270,309";
    String ls_ids_4 = "198,199,204,299,232,285,244,247,251,300,310,311,312,284,289";
    Compte compte = (Compte) request.getContext().get("compte");
    String x = appAccountService.getsuiviannuelle(annee, ls_ids_1);
    String y = appAccountService.getsuiviannuelleDepenses(annee, ls_ids_2);
    String z = appAccountService.getHistoriqueCompteByYearnow(annee, compte);
    String w = appAccountService.getHistoriqueCompteByYearnow(annee - 1, compte);
    String p = appAccountService.getanne(annee - 1);
    String u = appAccountService.getannebudget(annee - 1);
    String n = appAccountService.getannenow(annee);
    String b = appAccountService.getannenowbuget(annee);
    String h = appAccountService.getannenowbuget(annee - 1);
    String L = appAccountService.getsuiviannuelle1(annee - 1, ls_ids_1);
    String F = appAccountService.getsuiviannuelleDepenses1(annee - 1, ls_ids_2);
    String g = appAccountService.getsuiviannuelleB(annee, ls_ids_3);
    String uu = appAccountService.getsuiviannuelle1last(annee - 1, ls_ids_3);
    String tr = appAccountService.getsuiviannuelleDepenses2(annee, ls_ids_4);
    String data_init = appAccountService.getDataVirementAy(annee - 1, ls_ids_2);
    String data_inite2 = appAccountService.getDataVirementAy(annee - 1, ls_ids_4);
    String trold = appAccountService.getsuiviannuelleDepenses21(annee - 1, ls_ids_4);
    String data_update = appAccountService.getMontantUpdateAy(F, data_init);
    String data_update1 = appAccountService.getMontantUpdateAy(trold, data_inite2);
    String fileLink =
        ReportFactory.createReport(IReport.DepencesuiviAnnuelle, "SuiviAnnuelle")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("data_1", x)
            .addParam("data_2", y)
            .addParam("data_3", z)
            .addParam("data_4", w)
            .addParam("data_5", p)
            .addParam("data_6", L)
            .addParam("data_7", F)
            .addParam("data_8", n)
            .addParam("data_9", b)
            .addParam("data_10", u)
            .addParam("data_11", h)
            .addParam("data_12", g)
            .addParam("data_13", uu)
            .addParam("data_14", tr)
            .addParam("data_15", data_update)
            .addParam("data_16", data_update1)
            .addParam("data_17", trold)
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("Le fichier suivi du budget annuelle").add("html", fileLink).map());
  }

  public void getMontantPrestation(ActionRequest request, ActionResponse response) {
    Encaissement encaissement = request.getContext().asType(Encaissement.class);
    response.setValue("montant", encaissement.getPrestation().getMontant());
  }

  public void getloadDataEncaissement(ActionRequest request, ActionResponse response) {
    EncaissementSelect s = request.getContext().asType(EncaissementSelect.class);
    response.setValue("annexSelect", s.getEncaissementSelect().getAnnexe().getName());
    response.setValue(
        "dateEncaisSelect", s.getEncaissementSelect().getDateencaissement().toString());
    response.setValue("montantSelect", s.getEncaissementSelect().getMontant().toString());
  }

  public void getloadMontantEncaissement(ActionRequest request, ActionResponse response) {
    GestionRecette recette = request.getContext().asType(GestionRecette.class);
    Set<Encaissement> list = recette.getEncaissement();
    BigDecimal som = BigDecimal.ZERO;
    for (Encaissement es : list) {
      som = som.add(es.getMontant());
    }
    if (som.intValue() > 5000) {
      response.setError(
          "Il est important de respecter le montant indiqué et de ne pas le dépasser.");
    } else {
      response.setValue("montant", som);
      response.setValue("montantenlettre", ConvertNomreToLettres.getStringMontant(som));
    }
  }

  public void updateDateLimite(ActionRequest request, ActionResponse response) {
    LocalDate d = LocalDate.now();
    LocalDate d2 = d.withDayOfMonth(d.lengthOfMonth());
    /*d2 = d2.minusDays(3);*/
    response.setValue("dateLimite", d2);
  }

  public void checklistEncaissement(ActionRequest request, ActionResponse response) {
    List<EncaissementSelect> ls =
        (List<EncaissementSelect>) request.getContext().getParent().get("encaissement");
    if (ls != null && ls.size() > 0) {
      String ids = "";
      for (EncaissementSelect tmp : ls) {
        ids += tmp.getEncaissementSelect().getId() + ",";
      }
      ids += "0";
      List<Encaissement> lsEncaissement = appAccountService.getlistEncaissementNotInIds(ids);
      response.setValue("encaissementSelect", lsEncaissement);
    }
  }

  public void tw_print_decisionDeVirement(ActionRequest request, ActionResponse response)
      throws AxelorException {
    long id_virement = (long) request.getContext().get("id");
    String fileLink =
        ReportFactory.createReport(IReport.DecisionVirement, "Decision de virement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id", id_virement)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Code Budget").add("html", fileLink).map());
  }

  public void tw_new_test(ActionRequest request, ActionResponse response) {
    Context ctx = request.getContext().getParent();
    int annee = (int) ctx.get("annee");
    VersionRubriqueBudgetaire v =
        Beans.get(VersionRubriqueBudgetaireRepository.class)
            .all()
            .filter("self.annee=:annee and has_version_final is true")
            .bind("annee", annee)
            .fetchOne();
    if (v != null) {
      response.setAttr("budget_retrait", "readonly", false);
      response.setAttr("budget_ajout", "readonly", false);
      Beans.get(MyConfigurationServiceImpl.class).updateAllTotalRubriqueBudgetaire(annee);
      Long id =
          v.getVersionRubriques().stream()
              .filter(VersionRB::getIs_versionFinale)
              .map(VersionRB::getId)
              .collect(Collectors.toList())
              .get(0);
      response.setAttr("budget_retrait", "domain", "self.id_version=" + id);
      response.setAttr("budget_ajout", "domain", "self.id_version=" + id);
    } else {
      response.setCanClose(true);
      response.setAttr("budget_retrait", "readonly", true);
      response.setAttr("budget_ajout", "readonly", true);
      response.setFlash("Aucune Rubrique disponible dans cette date");
      return;
    }
  }

  public void tw_fun_detect_doc_to_print(ActionRequest request, ActionResponse response)
      throws AxelorException {
    ActionView.ActionViewBuilder viewBuilder =
        ActionView.define("Gestion du version du document")
            .model(RubriquesBudgetaire.class.getName())
            .add("form", "v_doc_budget_form")
            .param("popup", "true")
            .param("forceEdit", "true")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .context("btn_show_in_groupe", request.getContext().get("_signal"))
            .context("anne", request.getContext().get("anne"))
            .context("id_version", request.getContext().get("id_version"));
    response.setView(viewBuilder.map());
  }

  public void tw_v_doc_budget_form_on_new(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String key = (String) request.getContext().get("btn_show_in_groupe");
    response.setValue("$ver_btn", key);
  }

  public void tw_printCodeBudgetPrincipal(ActionRequest request, ActionResponse response)
      throws AxelorException {
    int annee = (int) request.getContext().get("anne");
    String version = request.getContext().get("version").toString();
    String reference = (String) request.getContext().get("reference");
    String fileLink =
        ReportFactory.createReport(IReport.CodeBudgetaireGenerale, "Budget Generale")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("version", version)
            .addParam("reference", reference)
            .generate()
            .getFileLink();
    response.setCanClose(true);
    response.setView(ActionView.define("Budget Generale").add("html", fileLink).map());
  }

  public void tw_printCodeBudgetProdF(ActionRequest request, ActionResponse response)
      throws AxelorException {
    int annee = (int) request.getContext().get("anne");
    String version = request.getContext().get("version").toString();
    String reference = (String) request.getContext().get("reference");
    String fileLink =
        ReportFactory.createReport(IReport.CodeBudgetaireProdF, "Budget Prod F")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("version", version)
            .addParam("reference", reference)
            .generate()
            .getFileLink();
    response.setCanClose(true);
    response.setView(ActionView.define("Budget Generale").add("html", fileLink).map());
  }

  public void tw_printCodeBudgetFONC(ActionRequest request, ActionResponse response)
      throws AxelorException {
    int annee = (int) request.getContext().get("anne");
    String version = request.getContext().get("version").toString();
    String reference = (String) request.getContext().get("reference");
    String fileLink =
        ReportFactory.createReport(IReport.CodeBudgetaireChargesExploitation, "Budget Fonc")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("version", version)
            .addParam("reference", reference)
            .generate()
            .getFileLink();
    response.setCanClose(true);
    response.setView(ActionView.define("Budget Generale").add("html", fileLink).map());
  }

  public void tw_printCodeBudgetFONCDetail(ActionRequest request, ActionResponse response)
      throws AxelorException {
    int annee = (int) request.getContext().get("anne");
    String version = request.getContext().get("version").toString();
    String reference = (String) request.getContext().get("reference");
    String fileLink =
        ReportFactory.createReport(
                IReport.CodeBudgetaireChargesExploitationDetail, "Budget Fonc detail")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("version", version)
            .addParam("reference", reference)
            .generate()
            .getFileLink();
    response.setCanClose(true);
    response.setView(ActionView.define("Budget Generale").add("html", fileLink).map());
  }

  public void tw_printCodeBudgetEquip(ActionRequest request, ActionResponse response)
      throws AxelorException {
    int annee = (int) request.getContext().get("anne");
    Long id_version = (Long) request.getContext().get("id_version");
    String version = request.getContext().get("version").toString();
    String reference = (String) request.getContext().get("reference");
    Beans.get(MyConfigurationServiceImpl.class).update_field_equipe(id_version);
    String fileLink =
        ReportFactory.createReport(IReport.CodeBudgetaireEmploisInvest, "Budget Equip")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("version", version)
            .addParam("reference", reference)
            .generate()
            .getFileLink();
    response.setCanClose(true);
    response.setView(ActionView.define("Budget Generale").add("html", fileLink).map());
  }

  public void tw_printCodeBudgetEquipDetail(ActionRequest request, ActionResponse response)
      throws AxelorException {
    int annee = (int) request.getContext().get("anne");
    Long id_version = (Long) request.getContext().get("id_version");
    String version = request.getContext().get("version").toString();
    String reference = (String) request.getContext().get("reference");
    String fileLink =
        ReportFactory.createReport(IReport.CodeBudgetaireEmploisInvestDetail, "Budget Equip")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("version", version)
            .addParam("reference", reference)
            .generate()
            .getFileLink();
    response.setCanClose(true);
    response.setView(ActionView.define("Budget Generale").add("html", fileLink).map());
  }

  public void tw_ajouterSignataire(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id = (Long) request.getContext().get("id");
    ActionView.ActionViewBuilder actionView =
        ActionView.define("Ajouter un signataire")
            .model(ModificationBudgetList.class.getName())
            .add("form", "form_ajouter_signataire")
            .param("popup", "true")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "true")
            .param("forceEdit", "true")
            .context("_showRecord", id);
    response.setView(actionView.map());
  }

  public void tw_reduireBudget(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    String nouv_etat = (String) request.getContext().get("etat");
    ModificationBudgetList m = Beans.get(ModificationBudgetListRepository.class).find(id);
    if ((m.getEtat() == null || m.getEtat().equals("2")) && nouv_etat.equals("1")) {
      for (Virements v : m.getVirements()) {
        appAccountService.modifierBudgetCascade(v);
        RubriquesBudgetaire retrait =
            Beans.get(RubriquesBudgetaireRepository.class).find(v.getBudget_retrait().getId());
      }
    }
  }

  public void show_traitement_account_lettrage(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    List<Account> ls_account =
        Beans.get(AccountRepository.class)
            .all()
            .filter("self.id in (:ids)")
            .bind(
                "ids",
                Beans.get(MoveLineRepository.class).all().filter("self.move.id in (:ids_long)")
                    .bind(
                        "ids_long",
                        Beans.get(GroupeMoveRepository.class).find(id).getMove_move().stream()
                            .map(Move::getId)
                            .collect(Collectors.toList()))
                    .fetch().stream()
                    .map(
                        moveLine -> {
                          return moveLine.getAccount().getId();
                        })
                    .collect(Collectors.toList()))
            .fetch();

    response.setView(
        ActionView.define(I18n.get("Liste des Comptes"))
            .model(Account.class.getName())
            .add("grid", "account-simple-grid")
            .add("form", "account_simple-form_custom")
            .domain("self.id in (:_id_account)")
            .context(
                "_id_account", ls_account.stream().map(Account::getId).collect(Collectors.toList()))
            .context("id_groupMouve", id)
            .map());
  }

  public void check_all_move_has_content(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    List<Move> m = Beans.get(GroupeMoveRepository.class).find(id).getMove_move();
    List<Move> list_to_delete = new ArrayList<>();
    for (Move tmp : m) {
      if (tmp.getMoveLineList() == null || tmp.getMoveLineList().size() == 0) {
        list_to_delete.add(tmp);
      }
    }
    // :supprimer les move vide.
    appAccountService.deleteListMove(list_to_delete);
  }

  public void tw_load_date_by_periode(ActionRequest request, ActionResponse response) {
    Period p =
        Beans.get(PeriodRepository.class)
            .find(
                (long) ((Integer) ((LinkedHashMap) request.getContext().get("periode")).get("id")));
    response.setValue("dateFrom", p.getFromDate());
    response.setValue("dateTo", p.getToDate());
  }

  public void tw_load_journale_ammortissement(ActionRequest request, ActionResponse response) {
    LocalDate dateAuisition =
        LocalDate.parse(request.getContext().get("acquisitionDate").toString());
    GroupeMove gm =
        Beans.get(GroupeMoveRepository.class)
            .all()
            .filter("self.period.year.name=:year")
            .bind("year", String.valueOf(dateAuisition.getYear()))
            .fetchOne();
    if (gm != null) {
      if (gm.getStatusSelect() != 3) {
        FixedAssetCategory fixedAssetCategory =
            (FixedAssetCategory) request.getContext().get("fixedAssetCategory");
        Long id = (Long) fixedAssetCategory.getId();
        FixedAssetCategory fc = Beans.get(FixedAssetCategoryRepository.class).find(id);
        Integer year = Integer.valueOf(gm.getPeriod().getYear().getName());
        Journal j =
            Beans.get(JournalRepository.class)
                .all()
                .filter("self.id_journal_parent = :id and self.year=:year")
                .bind("id", fc.getJournal().getId())
                .bind("year", year)
                .fetchOne();

        // response.setAttr("journal","domain","self.id="+j!=null?j.getId():0);
        response.setValue("journal", j);
      } else {
        response.setFlash("L'exercice est cloturer impossible d'ajouter une immobilsation");
        return;
      }
    } else {
      response.setFlash("Aucun Exercice ouvert à cette date");
      return;
    }
  }

  public void show_traitement_move_moveline(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    GroupeMove gm = Beans.get(GroupeMoveRepository.class).find(id);
    response.setView(
        ActionView.define(I18n.get("Liste des Comptes"))
            .model(Move.class.getName())
            .add("grid", "ecriture-grid")
            .add("form", "ecriture-form")
            .param("forceEdit", "true")
            .domain("self.id in (:_id_move)")
            .context(
                "_id_move",
                gm.getMove_move().stream().map(Move::getId).collect(Collectors.toList()))
            .context("id_groupMouve", id)
            .map());
  }

  public void selectcondion(ActionRequest request, ActionResponse response) {
    Period p = (Period) request.getContext().get("period");
    p = Beans.get(PeriodRepository.class).find(p.getId());
    List<Journal> j =
        Beans.get(JournalRepository.class)
            .all()
            .filter("self.year=:year")
            .bind("year", p.getYear().getFromDate().getYear())
            .fetch();
    List<Long> mv =
        Beans.get(MoveRepository.class).all().fetch().stream()
            .filter(move -> j.contains(move.getJournal()))
            .map(
                move -> {
                  return move.getJournal().getId();
                })
            .collect(Collectors.toList());
    response.setAttr("journal", "domain", "self.id in (" + Joiner.on(",").join(mv) + ")");
  }
}
