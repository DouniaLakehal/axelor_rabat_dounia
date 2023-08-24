package com.axelor.apps.account.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.account.report.IReport;
import com.axelor.apps.account.service.app.ComptabiliteServiceImpl;
import com.axelor.apps.configuration.service.ConvertNomreToLettres;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.exception.AxelorException;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.math.BigDecimal;

@Singleton
public class ComptabiliteController {

  @Inject ComptabiliteServiceImpl comptabiliteService;

  public void ImprimerJFInancier(ActionRequest request, ActionResponse response)
      throws AxelorException {

    int annee = Integer.valueOf(request.getContext().get("anneeJoFinancier").toString());

    int anneePrecedente = 0;
    int moisPrecedent = 0;
    int mois = Integer.valueOf(request.getContext().get("moisJoFinancier").toString());
    if (mois == 1) {
      anneePrecedente = annee - 1;
      moisPrecedent = 12;
    } else {
      anneePrecedente = annee;
      moisPrecedent = mois - 1;
    }

    String datePrecedenteString =
        Integer.toString(annee) + "-" + Integer.toString(mois) + "-" + "01";
    String date1 = "";
    String date2 = "";
    String datePreced = "";
    String dateCurrent = "";
    if (moisPrecedent < 10) {
      date1 = "0" + Integer.toString(moisPrecedent) + "/01/" + Integer.toString(anneePrecedente);
      int day1 = comptabiliteService.getTheLastDayofTheMonth(date1);
      datePreced =
          String.valueOf(day1)
              + "/0"
              + Integer.toString(moisPrecedent)
              + "/"
              + Integer.toString(anneePrecedente);
    } else {
      date1 = Integer.toString(moisPrecedent) + "/01/" + Integer.toString(anneePrecedente);
      int day1 = comptabiliteService.getTheLastDayofTheMonth(date1);
      datePreced =
          String.valueOf(day1)
              + "/"
              + Integer.toString(moisPrecedent)
              + "/"
              + Integer.toString(anneePrecedente);
    }

    if (mois < 10) {
      date2 = "0" + Integer.toString(mois) + "/01/" + Integer.toString(annee);
      int day2 = comptabiliteService.getTheLastDayofTheMonth(date2);
      dateCurrent =
          String.valueOf(day2) + "/0" + Integer.toString(mois) + "/" + Integer.toString(annee);
    } else {
      date2 = Integer.toString(mois) + "/01/" + Integer.toString(annee);
      int day2 = comptabiliteService.getTheLastDayofTheMonth(date2);
      dateCurrent =
          String.valueOf(day2) + "/" + Integer.toString(mois) + "/" + Integer.toString(annee);
    }

    Object etat = comptabiliteService.excuteRequetteSommeDepense(mois, annee);
    float sommeDepense = ((BigDecimal) etat).floatValue();
    //        BigDecimal sommeDepenseDecimal= (BigDecimal) etat;

    // BigDecimal bd = new BigDecimal(Float.toString(d));
    int a = 1;
    BigDecimal sommeDepenseDecimal = new BigDecimal(Float.toString(sommeDepense));

    BigDecimal PetitSommeJFinanc =
        comptabiliteService.CalculSommeDepenseDecimal(
            mois, annee, sommeDepenseDecimal, datePrecedenteString);
    BigDecimal sommeAtRecette =
        comptabiliteService.CalculSommeRecetteDecimal(mois, annee, datePrecedenteString);
    BigDecimal totalAnneePrecedente = comptabiliteService.getSommeComptes(anneePrecedente);

    String anneString = request.getContext().get("anneeJoFinancier").toString();
    String moisString = comptabiliteService.ConvertMoisToLettre(mois);
    BigDecimal TotalCreditsJournalF =
        comptabiliteService.TotalCreditsJournalF2(mois, annee, datePrecedenteString);
    BigDecimal TotalDebitsJournalF =
        comptabiliteService.TotalDebitJournalF2(mois, annee, datePrecedenteString);

    BigDecimal TotalHistoriqueBeforeThisMonths =
        comptabiliteService.CalculTotalHistCompte(mois, anneePrecedente, datePrecedenteString);
    BigDecimal totalGeneraleJF =
        TotalHistoriqueBeforeThisMonths.add(TotalCreditsJournalF).subtract(TotalDebitsJournalF);
    String fileLink =
        ReportFactory.createReport(IReport.JournalFinancier, "JournalFinancier")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("anneeP", anneePrecedente)
            .addParam("mois", mois)
            .addParam("moisPrecedent", moisPrecedent)
            .addParam("anneString", anneString)
            .addParam("datePrecedente", java.sql.Date.valueOf(datePrecedenteString))
            .addParam("moisString", moisString)
            .addParam("sommeAtRecette", sommeAtRecette)
            .addParam("PetitSommeJFinanc", PetitSommeJFinanc)
            .addParam("totalAnneePrecedente", totalAnneePrecedente)
            .addParam("TotalCreditsJournalF", TotalCreditsJournalF)
            .addParam("TotalDebitsJournalF", TotalDebitsJournalF)
            .addParam("totalGeneraleJF", totalGeneraleJF)
            .addParam("datePreced", datePreced)
            .addParam("dateCurrent", dateCurrent)
            .generate()
            .getFileLink();

    response.setView(
        ActionView.define("Le fichier de journal financier").add("html", fileLink).map());
  }

  public void genererReleveAffranchAnnuel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    int annee = Integer.valueOf(request.getContext().get("annee").toString());
    BigDecimal total = comptabiliteService.calculTotalAffranch(annee);
    String totalString = ConvertNomreToLettres.getStringMontant(total);
    String fileLink =
        ReportFactory.createReport(IReport.ReleveAffranchissement, "Releve affranchissement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("total", totalString)
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("Le fichier d'afffranchissement").add("html", fileLink).map());
  }

  public void saveFirstAffranchisement(ActionRequest request, ActionResponse response) {
    comptabiliteService.saveFirstAffranchisement(request, response);
  }
}
