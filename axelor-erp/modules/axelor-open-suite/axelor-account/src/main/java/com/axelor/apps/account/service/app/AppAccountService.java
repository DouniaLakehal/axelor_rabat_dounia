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
import com.axelor.apps.base.db.AppAccount;
import com.axelor.apps.base.db.AppBudget;
import com.axelor.apps.base.db.AppInvoice;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.configuration.db.AnnexeGenerale;
import com.axelor.apps.configuration.db.Compte;
import com.axelor.apps.configuration.db.HistoriqueBudgetaire;
import com.axelor.apps.configuration.db.HistoriqueCompte;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.auth.db.User;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AppAccountService extends AppBaseService {
  String getDataArreteCompt(Long idCompte, int annee);
  
  float getSommeRecettePrevuPrecedente(int anneePrecedent);
  
  float getSommeRecetteRealisePrecedente(int anneePrecedent);
  
  Float getDepenseSommeRealise(int annee, long id);
  
  Float getDepensePrvueOfRubrique(Long id, int annee);
  
  float getSommeOfCreditsCCISM(int annee);
  
  Float getRecettePrvueOfRubrique(Long id, int annee);
  
  Float getRecetteSommeRealise(int annee, Long compte, long id);
  
  AppAccount getAppAccount();
  
  AppBudget getAppBudget();
  
  AppInvoice getAppInvoice();
  
  void generateAccountConfigurations();
  
  AnnexeGenerale getAnnexeById(Long id);
  
  User getUserById(Long id);
  
  Compte getCompteById(Long id);
  
  GestionRecette getRecetteById(Long id);
  
  HistoriqueCompte addhitoriqueCompte(
          int annee,
          String mois,
          Long idcompte,
          BigDecimal montant,
          boolean recette,
          String annexe_id,
          Long id_historique);
  
  HistoriqueBudgetaire addRubrique(
          int annee, int mois, String annexe_id, Long rubrique_id, BigDecimal montant);
  
  void saveRecette(GestionRecette recette);
  
  void saveMoveLine(MoveLine moveLine);
  
  void retraitMontantCompte(HistoriqueCompte h);
  
  void deleteRecette(Long id);
  
  String getMontantString(BigDecimal montant);
  
  RegieTest getRegiById(Long id);
  
  void DemandeRetraitBudget(ActionRequest request, ActionResponse response);
  
  void TraiterDemandeRetraitBudget(ActionRequest request, ActionResponse response);
  
  String getSituationMentuelleData(int mois, int year, String ls);
  
  BigDecimal getTotalFromAll(List<String> data_tab_1);
  
  String calcule_somme_allMois(List<String> ls_som);
  
  String getsuiviannuelle(int year, String ls_id_1);
  
  String getDataInitial(int year, String ids);
  
  String getDataVirement(int year, String ids);
  
  String getMontantUpdate(String data_virement, String data_init);
  
  String getMontantReliquat(String data_update, String data_som);
  
  BigDecimal gettotalFromList(String data);
  
  void appendDatatoFile(int annee, List<String> ls_id, ReportSettings file);
  
  void appendDataInvestoFile(int annee, List<String> ls_id, ReportSettings file);
  
  String getsuiviannuelleDepenses(int year, String ls_id_2);
  
  String getHistoriqueCompteByYearnow(int year, Compte compte);
  
  String getanne(int year);
  
  String getsuiviannuelle1(int year, String ls_id_1);
  
  String getsuiviannuelleDepenses1(int year, String ls_id_2);
  
  String getannenow(int year);
  
  String getannenowbuget(int year);
  
  String getannebudget(int year);
  
  String getsuiviannuelleB(int year, String ls_id_3);
  
  String getsuiviannuelle1last(int year, String ls_id_3);
  
  String getsuiviannuelleDepenses2(int year, String ls_ids_4);
  
  String getsuiviannuelleDepenses21(int year, String ls_ids_4);
  
  String getDataVirementAy(int year, String ls_ids_2);
  
  String getMontantUpdateAy(String y, String ls_ids_2);
  
  BigDecimal checkMontant(LocalDate d1, LocalDate d2);
  
  List<Encaissement> getlistEncaissementNotInIds(String ids);
  
  AnnexeGenerale getDefaultAnnexeCentrale();
  
  User getDefaultUserCentrale();
  
  Encaissement getEncaissementById(Long id);
  
  void AnnulerEncaissement(Long id, String commentaire);
  
  void ModifierEncaissement(Long id);
  
  void VerserEncaissement(Long id);
  
  void modifierBudgetCascade(Virements v);
  
  void deleteListMove(List<Move> list_to_delete);
  
  AccountPrintReconcil save_account_print_reconcil(AccountPrintReconcil ac);
  
  void saveEncaissement(Encaissement s);
  
  String getNumeroOrdre();
  
  String getNumeroQuittanceInc();
  
  TresaurerieAnnuel saveTresairerie(TresaurerieAnnuel temp);
  
  void updateAllTresaurerie(Integer year);
  
  Long getId_version_valideByYear(int year, ActionResponse response);
  
  /*public BigDecimal getTotalEncaissement(Long id_annexe);*/
}
