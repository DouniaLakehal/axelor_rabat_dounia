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
package com.axelor.apps.purchase.service.app;

import com.axelor.apps.base.db.AppPurchase;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.configuration.db.*;
import com.axelor.apps.purchase.db.*;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.math.BigDecimal;
import java.util.List;

public interface AppPurchaseService extends AppBaseService {

  public AppPurchase getAppPurchase();

  public void generatePurchaseConfigurations();

  public void generateComparaisonPrix(Long id, List<SelectFournisseur> list_f);

  public void updateComparaisonPrix(Long id);

  public void inject_artilrToComparaison(Long id);

  public String updateTotal(Long id, Integer tva);

  public void deletecomparaisonPrix(List<ComparaisonPrix> com);

  public int getNombreDemandeAchat(Integer year);

  public ArticleDetails getArticleDetailById(Long id);

  public int checkstockByDemandeAchat(ArticleDetails dmd);

  public void retraitStock(Long id);

  public ConsultationPrix getConsultationPrixbyId(Long id);

  public List<ArticlesPrix> getArticlePrixByArticleDetailId(Long id);

  public void removeArticlePrixfromComparaison(ArticlesPrix articleprix);

  public void removeArticlePrix(ArticlesPrix articleprix);

  public void removeArticleDetail(ArticleDetails tmp);

  public ComparaisonPrix getComparaisonPrixById(Long id_compPrix);

  public List<ComparaisonPrix> getListActiveComparaisonPrix(Long id_compPrix);

  public int getNombreCommande(int year);

  public CommandeAchat saveCommandeAchat(CommandeAchat c);

  public BigDecimal getBudgetByrubriqueBudgetaire(RubriqueBudgetaireGenerale rub, int year);

  public void retraitMontantCommande(ComparaisonPrix c);

  public void closeComparaisonPrix(
      List<ComparaisonPrix> list, ComparaisonPrix comparaisonPrix, CommandeAchat commandeAchat);

  public void recevoirCommande(Long idcommande);

  public String getNumeroConsultation();

  public void upDateSelectFourn(Long idFournissuer);

  public Fournisseur getFournisseurById(Long idFournisseur);

  public void deleteSelectFournisseurByConsultationId(Long id_consultation);

  public void deleteArticleDetailConsultationId(Long id_articleDetail);

  public void deleteConsultationPrixById(Long id_consultation);

  public List<ComparaisonPrix> getAllComparaisonPrixByConsultationPrixId(Long id);

  public void deletecomparaisonPrixByConsultationPrixId(Long id_consultation);

  public CommandeAchat getCommandeAchatById(Long id);

  public ArticlesPrix saveArticlePrix(ArticlesPrix art);

  public Vehicules getVehiculesById(Long idVehicule);

  public CommandeAchat creatCommandeAchat(ActionRequest request, ActionResponse response);

  public ReceptionCommande addReceptionCommande(Long id_commande, String BLN);

  public OrderPaymentCommande creatOrdrePayementCommande(
      ActionRequest request, ActionResponse response);

  public OrderPaymentCommande getOrdreByCommandeId(Long id_commande);

  public void update_historiquecompte(
      String designation,
      String rib,
      BigDecimal montant,
      String description,
      Integer action,
      AnnexeGenerale annexe,
      Long id_compte);

  public AnnexeGenerale getAnnexeById(Long id);

  public Compte getCompteById(Long id);

  public OrderPaymentCommande getOrdrePayementbyId(Long id);

  public OrdrevirementCommande getordreVirementById(Long id);

  public HistoriqueCompte addHistoriqueCompte(Compte c, OrdrevirementCommande ov);

  public void saveOrdreVirement(OrdrevirementCommande ov);

  public String getsommeTotaleString(Long ov);

  public List<ArticlesPrix> getArticleRestant(Long id_command);

  public BigDecimal getQuantite_reliquat(Long id_command);

  public void updateStock(List<ArticlesRecu> ls, AnnexeGenerale annexe);

  public OffreAppel getOffreById(Long idOffre);

  public String getNumOffre();

  public Statut getStatutById(Long id);

  public Sociecte saveSociete(Sociecte c);

  public boolean commandeHasReliquat(Long id_commandeAchat);

  void commandeComplete(Long id);

  void deleteReceptionCommande(Long id_reception);

  void addReceptionToCommandeAchat(Long id_reception);

  HistoriqueStock getHistoriqueStockByArticleAnnexe(Article article, AnnexeGenerale annexe);

  void saveHistoriqueStock(HistoriqueStock h);

  void saveOffreAppel(Soumissionnaire s);

  Soumissionnaire getSoumissionnaireById(Long id);

  void validerPieceJointe2(Long id);

  void NotvaliderPieceJointe2(Long id, String comment);

  boolean attacherFormulaireTechniqueSoumissionnaire(Long id);

  void affecterListCritereToSoumiss(Long id);

  void supprimerListCritereFromSoumiss(Long id);

  BigDecimal getPointObtenuByid_soum(Long id_soum);

  void affecterMarcheToSoumissionnaire(Long id_soum);

  public void validerReservePieceJointe2(Long id, String comment);

  List<Soumissionnaire> getSoumissionnaireNotValideByIdOffre(Long id_offre);

  Soumissionnaire getSoumissionnaireSuivant(Long id);

  void saveOffreAppel(OffreAppel o);

  void recalerSoumissionnaireGagnant(Long id_soumissionnaireToRecaler);

  void saveMarcheProvisoire(MarcheProvisoire m);

  MarcheProvisoire getMarcheProvisoire(Long id_marcheProvisoire);

  void createMarcheProvisoire(Long id_soum);

  OffreAppel attribuerNumerosMarche(OffreAppel o);

  void setSoumissionnaireHas_Marche(Long id, boolean etat);

  public FactureAchat getFactureById(Long id);

  public void saveFactureAchat(FactureAchat f);

  OrderPaymentCommande createOrdreDePayement(FactureAchat f);

  OrderPaymentCommande updateOrdreDePayement(FactureAchat f);

  public void deleteFactureAchat(Long id);

  EngagementAchat save_EngagementAchat(EngagementAchat engagementAchat);

  void validerProvisoire(Long id);

  public void validerReserveProvisoire(Long id, String comment);

  void NotvaliderProvisoire(Long id, String comment);

  void validerDefinitif(Long id);

  public void validerReserveDefinitif(Long id, String comment);

  void NotvaliderDefinitif(Long id, String comment);

  void updateDateDocument(String limit);

  void saveLivrableP(LivrableP pp);
}
