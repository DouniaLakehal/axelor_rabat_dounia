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
package com.axelor.apps.account.report;

public interface IReport {
  public static final String JournalFinancier = "JournalFinancier.rptdesign";
  public static final String ACCOUNTING_REPORT_TYPE = "AccountingReportType%s.rptdesign";
  public static final String ACCOUNTING_REPORT_TYPE1 = "AccountingReportType22.rptdesign";
  public static final String PAYMENT_VOUCHER = "PaymentVoucher.rptdesign";
  public static final String IRRECOVERABLE = "Irrecoverable.rptdesign";
  public static final String INVOICE = "Invoice.rptdesign";
  public static final String SALE_INVOICES_DETAILS = "SaleInvoicesDetails.rptdesign";
  public static final String PURCHASE_INVOICES_DETAILS = "PurchaseInvoicesDetails.rptdesign";
  public static final String ACCOUNT_MOVE = "AccountMove.rptdesign";
  public static final String SUBROGATION_RELEASE = "SubrogationRelease.rptdesign";
  public static final String CHEQUE_DEPOSIT_SLIP = "ChequeDepositSlip.rptdesign";
  public static final String CASH_DEPOSIT_SLIP = "CashDepositSlip.rptdesign";
  public static final String GestionRecette = "SituationRecette.rptdesign";
  public static final String BilanActif = "BilanActif.rptdesign";
  public static final String BilanPassif = "BilanPassif.rptdesign";
  public static final String CPC = "cpc.rptdesign";

  // public static final String CompteProduit = "CompteProduits.rptdesign";
  public static final String CompteProduit = "cpc_hors_taxe.rptdesign";

  public static final String ArreteDeCompte = "ArreteDeCompte.rptdesign";
  public static final String GestionRegie = "gestionRegies.rptdesign";
  public static final String OrdreRecette = "OrdreDeRecette.rptdesign";
  public static final String TRESORERIE = "Tresorerie.rptdesign";
  public static final String DepenceMensuel = "SituationDeRecetteMensuel.rptdesign";
  public static final String DepenceAnnuelle = "situationAnnuelle.rptdesign";
  public static final String DepencesuiviAnnuelle = "SuiviAnnuelle.rptdesign";
  public static final String ArreteCompte = "ArreteDeCompte.rptdesign";
  public static final String ArreteDeCompteSuite = "ArreteDeCompteSuite.rptdesign";
  public static final String ReleveAffranchissement = "ReleveAffranchissement.rptdesign";
  public static final String EtatEncaissementAnnuel = "EtatMensuelDesRecettesDeLaRegie.rptdesign";
  public static final String EtatEncaissementAnnuelMoyenne =
      "EtatAnnuelDesRecettesDeLaRegie.rptdesign";
  public static final String EtatEncaissementMensuel = "EtatEncaissementMensuel.rptdesign";
  public static final String EtatEncaissementHybdomadaire =
      "EtatEncaissementHybdomadaire.rptdesign";
  public static final String EtatEncaissementJournalier = "EtatEncaissementJournalier.rptdesign";
  public static final String StPrestation = "Statistiquesdesprestation.rptdesign";

  public static final String OrdreDeVersement = "OrdreVersement.rptdesign";
  public static final String ReleveDesProduits = "ReleveDesProduits.rptdesign";

  // rubriques budgetaires

  public static final String DecisionVirement = "DecisionVirement.rptdesign";
  public static final String CodeBudgetaireGenerale = "CodeBudgetaireGenerale.rptdesign";
  String CodeBudgetaireProdF = "CodeBudgetaireProdF.rptdesign";
  public static final String CodeBudgetaireChargesExploitation =
      "CodeBudgetaireChargesExploitation.rptdesign";
  public static final String CodeBudgetaireChargesExploitationDetail =
      "CodeBudgetaireChargesExploitation_detail.rptdesign";
  public static final String CodeBudgetaireEmploisInvest = "CodeBudgetaireEmploisInvest.rptdesign";
  public static final String CodeBudgetaireEmploisInvestDetail =
      "CodeBudgetaireEmploisInvestDetail.rptdesign";

  public static final String BilanGenerale = "Bilan.rptdesign";
}
