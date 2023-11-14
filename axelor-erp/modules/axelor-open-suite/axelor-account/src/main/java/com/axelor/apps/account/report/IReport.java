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
  String JournalFinancier = "JournalFinancier.rptdesign";
  String ACCOUNTING_REPORT_TYPE = "AccountingReportType%s.rptdesign";
  String ACCOUNTING_REPORT_TYPE_JOURNAL =
          "AccountingReportType11journal.rptdesign";
  String ACCOUNTING_REPORT_TYPE1 = "AccountingReportType22.rptdesign";
  String PAYMENT_VOUCHER = "PaymentVoucher.rptdesign";
  String IRRECOVERABLE = "Irrecoverable.rptdesign";
  String INVOICE = "Invoice.rptdesign";
  String SALE_INVOICES_DETAILS = "SaleInvoicesDetails.rptdesign";
  String PURCHASE_INVOICES_DETAILS = "PurchaseInvoicesDetails.rptdesign";
  String ACCOUNT_MOVE = "AccountMove.rptdesign";
  String SUBROGATION_RELEASE = "SubrogationRelease.rptdesign";
  String CHEQUE_DEPOSIT_SLIP = "ChequeDepositSlip.rptdesign";
  String CASH_DEPOSIT_SLIP = "CashDepositSlip.rptdesign";
  String GestionRecette = "SituationRecette.rptdesign";
  String BilanActif = "BilanActif.rptdesign";
  String BilanPassif = "BilanPassif.rptdesign";
  String CPC = "cpc.rptdesign";
  
  // public static final String CompteProduit = "CompteProduits.rptdesign";
  String CompteProduit = "cpc_hors_taxe.rptdesign";
  
  String ArreteDeCompte = "ArreteDeCompte.rptdesign";
  String GestionRegie = "gestionRegies.rptdesign";
  String OrdreRecette = "OrdreDeRecette.rptdesign";
  String TRESORERIE = "Tresorerie.rptdesign";
  String DepenceMensuel = "SituationDeRecetteMensuel.rptdesign";
  //public static final String DepenceAnnuelle = "situationAnnuelle.rptdesign";
  String DepenceAnnuelle = "situation_depenses_annuelle.rptdesign";
  String DepencesuiviAnnuelle = "SuiviAnnuelle.rptdesign";
  // public static final String ArreteCompte = "ArreteDeCompte.rptdesign";
  String ArreteCompte = "ArreteDeCompte_v2.rptdesign";
  String ArreteDeCompteSuite = "ArreteDeCompteSuite.rptdesign";
  String ReleveAffranchissement = "ReleveAffranchissement.rptdesign";
  String EtatEncaissementAnnuel = "EtatMensuelDesRecettesDeLaRegie.rptdesign";
  String EtatEncaissementAnnuelMoyenne =
          "EtatAnnuelDesRecettesDeLaRegie.rptdesign";
  String EtatEncaissementMensuel = "EtatEncaissementMensuel.rptdesign";
  String EtatEncaissementHybdomadaire =
          "EtatEncaissementHybdomadaire.rptdesign";
  String EtatEncaissementJournalier = "EtatEncaissementJournalier.rptdesign";
  String StPrestation = "Statistiquesdesprestation.rptdesign";
  
  String OrdreDeVersement = "OrdreVersement.rptdesign";
  String ReleveDesProduits = "ReleveDesProduits.rptdesign";
  
  // rubriques budgetaires
  
  String DecisionVirement = "DecisionVirement.rptdesign";
  String CodeBudgetaireGenerale = "CodeBudgetaireGenerale.rptdesign";
  String CodeBudgetaireProdF = "CodeBudgetaireProdF.rptdesign";
  String CodeBudgetaireChargesExploitation =
          "CodeBudgetaireChargesExploitation.rptdesign";
  String CodeBudgetaireChargesExploitationDetail =
          "CodeBudgetaireChargesExploitation_detail.rptdesign";
  String CodeBudgetaireEmploisInvest = "CodeBudgetaireEmploisInvest.rptdesign";
  String CodeBudgetaireEmploisInvestDetail =
          "CodeBudgetaireEmploisInvestDetail.rptdesign";
  
  String BilanGenerale = "Bilan.rptdesign";
  String LETTRAGE = "Lettarge.rptdesign";
  String RecetteGenerale = "RecetteGenerale.rptdesign";
  String PRINT_TRESORERIE = "printTresorerie.rptdesign";
}
