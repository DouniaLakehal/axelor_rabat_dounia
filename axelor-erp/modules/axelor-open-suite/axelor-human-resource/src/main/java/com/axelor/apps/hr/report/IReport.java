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
package com.axelor.apps.hr.report;

public interface IReport {

  public static final String EtatSalaire2 = "EtatSalaire2.rptdesign";
  public static final String EtatSalaire4 = "EtatSalaire4.rptdesign";
  public static final String EtatRabat = "EtatRabat.rptdesign";
  public static final String EMPLYOMENT_CONTRACT = "EmploymentContract.rptdesign";
  public static final String EXPENSE = "Expense.rptdesign";
  public static final String TIMESHEET = "Timesheet.rptdesign";
  public static final String EMPLOYEE_BONUS_MANAGEMENT = "EmployeeBonusMgt.rptdesign";
  public static final String EMPLOYEE_ANNUAL_REPORT = "EmployeeAnnualReport.rptdesign";
  public static final String LUNCH_VOUCHER_MGT_MONTHLY = "LunchVoucherMgt_Monthly.rptdesign";
  public static final String LUNCH_VOUCHER_ADVANCE = "LunchVoucherAdvance.rptdesign";
  public static final String EMPLOYEE_PHONEBOOK = "EmployeePhoneBook.rptdesign";
  public static final String EMPLOYEE_TIMESHEET = "EmployeeTimesheet.rptdesign";
  public static final String EMPLOYEE = "Employee.rptdesign";
  public static final String EMPLOYEE_ETAT_ENGAGEMENT = "EtatEngagement.rptdesign";
  public static final String OrdreMission = "OrdreMission.rptdesign";
  public static final String EtatSommeDues = "EtatSommeDues.rptdesign";
  public static final String EtatSommeDuesJour = "EtatSommeDuesJournaliere.rptdesign";
  public static final String OrdredePaiement1 = "OrdredePaiement1.rptdesign";
  public static final String EMPLOYEE_ETAT_ENGAGEMENT_v2 = "EtatEngagementV2.rptdesign";
  public static final String EMPLOYEE_ETAT_ENGAGEMENT_v3 = "EtatEngagementV3.rptdesign";
  public static final String BordereauDemission = "BordereauDemission.rptdesign";
  public static final String OrdredesPaiements = "RCAR.rptdesign";
  public static final String OrdredesPaiementIR = "IR.rptdesign";
  public static final String OrdredesPaiementCSS = "CSS.rptdesign";
  public static final String Mt_Logement = "Mt_Logement.rptdesign";
  public static final String PageDeGarde = "PageDeGarde.rptdesign";

  public static final String etatRappelSurSalaire = "EtatNominatifRegionRappel.rptdesign";
  public static final String EtatRcarRapell = "EtatRcarRappel.rptdesign";
  public static final String OrdreDeVirementRappel = "OrdreDeVirementRappel.rptdesign";
  public static final String CotisationPatronal = "CotisationPatronalRcar.rptdesign";
  public static final String ImpotSurRevenu = "ImpotSurRevenu.rptdesign";
  public static final String OrdrePayementRcar = "OP_Rcar.rptdesign";
  public static final String OrdrePayementRcarPatronal = "OP_Rcar_Patronal.rptdesign";
  public static final String OrdrePayementIR = "OP_IR.rptdesign";
  public static final String OrdreDeVirement = "OrdreDeVirement.rptdesign";

  public static final String OrdreVirementIR = "OV_IR.rptdesign";
  public static final String OrdrePayementCnops = "OP_CNOPS.rptdesign";
  public static final String OrdrePayment_MTTIT = "OrdreMt.rptdesign";
  public static final String OrdrePayment_MTTIT1 = "OrdreMt1.rptdesign";
  public static final String OrdrePayementAMO = "OP_AMO.rptdesign";
  public static final String OrdreVirementRcar = "OV_RCAR.rptdesign";
  public static final String OP_MT_TIT = "OP_MT_TIT.rptdesign";
  public static final String BD_Emission_Rappel = "BordereauEmissionRappel.rptdesign";
  public static final String OP_Omfam = "OP_OMFAM.rptdesign";
  public static final String OP_MGPAP = "OP_MGPAP.rptdesign";
  public static final String OV_CNOPS = "OV_CNOPS.rptdesign";

  public static final String OVRCAR = "OVRCAR.rptdesign";
  public static final String OVCNOPS = "OVCNOPS.rptdesign";
  public static final String EtatRCAR = "EtatRCAR.rptdesign";
  public static final String EtatRcarSal = "EtatRcarSal.rptdesign";
  public static final String EtatIR = "EtatIR.rptdesign";
  public static final String BordereauMensuel = "BordereauMensuel.rptdesign";
  public static final String Vignette = "Vignette.rptdesign";
  public static final String EtatOMFAM = "EtatOMFAM.rptdesign";
  public static final String EtatMGPAP = "EtatMGPAP.rptdesign";
  public static final String EtatAMO = "EtatAMO.rptdesign";
  public static final String OVothers = "OVothers.rptdesign";
  public static final String etatAmoRappel = "Etat_amo_rappel.rptdesign";
  public static final String EtatAXA = "EtatAXA.rptdesign";
  public static final String Etat_MT_MGPAP = "Etat_MT_SMCCD.rptdesign";
  public static final String Etat_MT_OMFAM = "Etat_MT_OMFAM.rptdesign";

  public static final String Decision_FR = "Decision_FR.rptdesign";
  public static final String Decision_AR = "Decision_AR.rptdesign";
  public static final String Decision_exceptionnel = "Decision_exceprionnel.rptdesign";

  public static final String OrdrePaiement = "OrdrePaiement.rptdesign";
  public static final String ReleveBancaire = "ReleveBancaire.rptdesign";

  public static final String ImprimerEtategagement_simple = "EtatEngagementV_simple.rptdesign";

  public static final String Bordereaudenvoi = "Bordereaudenvoi.rptdesign";
  public static final String BordereaudenvoiOMFAM = "BordereaudenvoiOMFAM.rptdesign";
  public static final String BordereaudenvoiMGPAPM = "BordereaudenvoiMGPAPM.rptdesign";
  public static final String BordereaudenvoiCNOPS = "BordereaudenvoiCNOPS.rptdesign";
  public static final String virement_report = "virement_report2.rptdesign";
  public static final String Listeordredevirement = "listeordredevirement.rptdesign";
  public static final String ETATSDESPRELEVEMENTS = "etatdesprelevements.rptdesign";
  public static final String etatdetailverselent = "Etatdetailverselent.rptdesign";
  public static final String Etatdecompteirnowork = "Etatdecompteirnoworks.rptdesign";
  public static final String OrderdePaiement_N52 = "Op_51.rptdesign";
  public static final String AttestationSalaire = "AttestationSalaire.rptdesign";
  public static final String AttestationTravail = "AttestationTravail.rptdesign";
  public static final String ETATSDESPRELEVEMENTSRCAR = "etatdesprelevementsRCAR.rptdesign";
  public static final String DeclarationIR = "DeclarationIR.rptdesign";
}
