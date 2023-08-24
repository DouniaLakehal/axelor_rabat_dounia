package com.axelor.apps.hr.web.expense;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.configuration.service.ConvertNomreToLettres;
import com.axelor.apps.configuration.service.MyConfigurationService;
import com.axelor.apps.configuration.service.RunSqlRequestForMe;
import com.axelor.apps.hr.service.EmployeeAdvanceService;
import com.axelor.apps.hr.service.EtatsalaireService.EtatSalaireServiceImpl;
import com.axelor.apps.hr.service.app.AppHumanResourceService;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.math.BigDecimal;

@Singleton
public class AchrafController {

  @Inject EtatSalaireServiceImpl etatSalaireServiceImpl;
  @Inject MyConfigurationService appConfService;
  @Inject AppHumanResourceService appHumanResourceService;
  @Inject EmployeeAdvanceService employeeAdvanceService;

  @Inject EmployeeAdvanceService appservice;

  private final String[] tableau_mois_francais = {
    "JANVIER",
    "FEVRIER",
    "MARS",
    "AVRIL",
    "MAI",
    "JUIN",
    "JUILLET",
    "AOUT",
    "SEPTEMBRE",
    "OCTOBRE",
    "NOVEMBRE",
    "DECEMBRE"
  };

  public Object excuteRequette(int mois, int annee, String req) {
    Object etat = null;

    javax.persistence.Query query =
        JPA.em().createNativeQuery(req).setParameter(1, mois).setParameter(2, annee);
    etat = query.getSingleResult();
    if (etat == null) {
      etat = new String[] {".", "0", "0"};
    }

    return etat;
  }

  public void ordredevirement(ActionRequest request, ActionResponse response)
      throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");

    BigDecimal m = employeeAdvanceService.salairePlusIndem(mois, annee);
    String mt_string = ConvertNomreToLettres.getStringMontant(m);

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.virement_report, "Ordre de virement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("mois", mois)
            .addParam("montantString", mt_string)
            .addParam("indem_salair", m)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement").add("html", fileLink).map());
  }

  public void ordredepaiementN52(ActionRequest request, ActionResponse response)
      throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");

    ///////////////////////////////// CNOPS AMO///////////////////////
    String req_amo =
        "select sum(es.amo) as amo from hr_etat_salaire es where es.mois="
            + mois
            + " and es.annee="
            + annee
            + "";
    BigDecimal etat_amo = RunSqlRequestForMe.runSqlRequest_Bigdecimal(req_amo);
    String montant_amo = ConvertNomreToLettres.getStringMontant(etat_amo);

    ///////////////////////////////// RCAR///////////////////////

    javax.persistence.Query req_logement =
        JPA.em()
            .createNativeQuery(
                "select sum(es.indemnite_logem_brut) as ir\n"
                    + "               from hr_etat_salaire es  where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object req_log = req_logement.getSingleResult();
    float etat_log = (BigDecimal.ZERO).floatValue();
    if (req_log != null) {
      etat_log = ((BigDecimal) req_log).floatValue();
    }

    String req_rcar =
        "select sum(es.rcar_rg) as salaires,\n"
            + "                 sum(es.rcarrcomp) as logement\n"
            + "               from hr_etat_salaire es  where es.mois=?1 and es.annee=?2";
    Object etat_rcar = excuteRequette(mois, annee, req_rcar);
    Object[] obj_rcar = (Object[]) etat_rcar;
    float rcar_log = etat_log;
    rcar_log = (float) (rcar_log * 0.03);
    float somme_rcar =
        ((BigDecimal) obj_rcar[0]).floatValue() + ((BigDecimal) obj_rcar[1]).floatValue();
    float rcar_sal = ((BigDecimal) obj_rcar[0]).floatValue() * 2;
    float compl_rcar = ((BigDecimal) obj_rcar[1]).floatValue();
    float rcar = rcar_sal + compl_rcar;
    float total_rcar = rcar - rcar_log;
    double number_rcar = (Math.round(total_rcar * 100)) / 100.0;
    String montant_rcar = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_rcar));

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrderdePaiement_N52, "Ordre de paiement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("mois", mois)
            .addParam("sommeAMO", etat_amo)
            .addParam("Montant_AMO", montant_amo)
            .addParam("rcar_log", rcar_log)
            .addParam("net", rcar)
            .addParam("total_rcar", total_rcar)
            .addParam("montant_rcar", montant_rcar)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de paiement").add("html", fileLink).map());
  }
}
