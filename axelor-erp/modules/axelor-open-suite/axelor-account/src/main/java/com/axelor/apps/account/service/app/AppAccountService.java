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
import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.HistoriqueCompte;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.auth.db.User;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AppAccountService extends AppBaseService {
  public String getDataArreteCompt(Long idCompte, int annee);

  public float getSommeRecettePrevuPrecedente(int anneePrecedent);

  public float getSommeRecetteRealisePrecedente(int anneePrecedent);

  public Float getDepenseSommeRealise(int annee, long id);

  public Float getDepensePrvueOfRubrique(Long id, int annee);

  public float getSommeOfCreditsCCISM(int annee);

  public Float getRecettePrvueOfRubrique(Long id, int annee);

  public Float getRecetteSommeRealise(int annee, Long compte, long id);

  public AppAccount getAppAccount();

  public AppBudget getAppBudget();

  public AppInvoice getAppInvoice();

  public void generateAccountConfigurations();

  public AnnexeGenerale getAnnexeById(Long id);

  User getUserById(Long id);

  public Compte getCompteById(Long id);

  public GestionRecette getRecetteById(Long id);

  public HistoriqueCompte addhitoriqueCompte(
      int annee,
      String mois,
      Long idcompte,
      BigDecimal montant,
      boolean recette,
      String annexe_id,
      Long id_historique);

  public HistoriqueBudgetaire addRubrique(
      int annee, int mois, String annexe_id, Long rubrique_id, BigDecimal montant);

  public void saveRecette(GestionRecette recette);

  public void saveMoveLine(MoveLine moveLine);

  public void retraitMontantCompte(HistoriqueCompte h);

  public void deleteRecette(Long id);

  public String getMontantString(BigDecimal montant);

  public RegieTest getRegiById(Long id);

  public void DemandeRetraitBudget(ActionRequest request, ActionResponse response);

  public void TraiterDemandeRetraitBudget(ActionRequest request, ActionResponse response);

  public String getSituationMentuelleData(int mois, int year, String ls);

  public BigDecimal getTotalFromAll(List<String> data_tab_1);

  public String calcule_somme_allMois(List<String> ls_som);

  public String getsuiviannuelle(int year, String ls_id_1);

  public String getDataInitial(int year, String ids);

  public String getDataVirement(int year, String ids);

  public String getMontantUpdate(String data_virement, String data_init);

  public String getMontantReliquat(String data_update, String data_som);

  public BigDecimal gettotalFromList(String data);

  public void appendDatatoFile(int annee, List<String> ls_id, ReportSettings file);

  public void appendDataInvestoFile(int annee, List<String> ls_id, ReportSettings file);

  public String getsuiviannuelleDepenses(int year, String ls_id_2);

  public String getHistoriqueCompteByYearnow(int year, Compte compte);

  public String getanne(int year);

  public String getsuiviannuelle1(int year, String ls_id_1);

  public String getsuiviannuelleDepenses1(int year, String ls_id_2);

  public String getannenow(int year);

  public String getannenowbuget(int year);

  public String getannebudget(int year);

  public String getsuiviannuelleB(int year, String ls_id_3);

  public String getsuiviannuelle1last(int year, String ls_id_3);

  public String getsuiviannuelleDepenses2(int year, String ls_ids_4);

  public String getsuiviannuelleDepenses21(int year, String ls_ids_4);

  public String getDataVirementAy(int year, String ls_ids_2);

  public String getMontantUpdateAy(String y, String ls_ids_2);

  public BigDecimal checkMontant(LocalDate d1, LocalDate d2);

  List<Encaissement> getlistEncaissementNotInIds(String ids);

  AnnexeGenerale getDefaultAnnexeCentrale();

  User getDefaultUserCentrale();

  public Encaissement getEncaissementById(Long id);

  void AnnulerEncaissement(Long id, String commentaire);

  void ModifierEncaissement(Long id);

  void VerserEncaissement(Long id);

  void modifierBudgetCascade(Virements v);

  void deleteListMove(List<Move> list_to_delete);

  public AccountPrintReconcil save_account_print_reconcil(AccountPrintReconcil ac);

  void saveEncaissement(Encaissement s);

  public String getNumeroOrdre();

  public String getNumeroQuittanceInc();

  TresaurerieAnnuel saveTresairerie(TresaurerieAnnuel temp);

  void updateAllTresaurerie(Integer year);

  /*public BigDecimal getTotalEncaissement(Long id_annexe);*/
}
