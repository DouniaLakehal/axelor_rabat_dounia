package com.axelor.apps.hr.web.expense;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.configuration.service.ConvertNomreToLettres;
import com.axelor.apps.configuration.service.MyConfigurationService;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Singleton
public class AyoubController {

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

  public void bordereaudenvoi(ActionRequest request, ActionResponse response)
      throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.Bordereaudenvoi, "RCAR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .generate()
            .getFileLink();
    response.setView(ActionView.define("RCAR").add("html", fileLink).map());
  }

  public void bordereaudenvoi_MGPAPM(ActionRequest request, ActionResponse response)
      throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.BordereaudenvoiMGPAPM, "MGPAPM")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MGPAPM").add("html", fileLink).map());
  }

  public void bordereaudenvoi_OMFAM(ActionRequest request, ActionResponse response)
      throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.BordereaudenvoiOMFAM, "OMFAM")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .generate()
            .getFileLink();
    response.setView(ActionView.define("OMFAM").add("html", fileLink).map());
  }

  public void bordereaudenvoi_CNOPS(ActionRequest request, ActionResponse response)
      throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.BordereaudenvoiCNOPS, "CNOPS")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .generate()
            .getFileLink();
    response.setView(ActionView.define("CNOPS").add("html", fileLink).map());
  }

  public void listeordredevirement(ActionRequest request, ActionResponse response)
      throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.Listeordredevirement, "liste")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .generate()
            .getFileLink();
    response.setView(ActionView.define("liste").add("html", fileLink).map());
  }

  public void etatprelevement(ActionRequest request, ActionResponse response)
      throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.ETATSDESPRELEVEMENTS, "ETAT DE PRELEVEMENT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .generate()
            .getFileLink();
    response.setView(ActionView.define("ETAT DE PRELEVEMENT").add("html", fileLink).map());
  }

  public void Etatdetailverselent(ActionRequest request, ActionResponse response)
      throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    javax.persistence.Query somme_Assiette =
        JPA.em()
            .createNativeQuery(
                "SELECT sum(es.total_salair_mensuel_brut_imposable-es.indemnite_logem_brut-es.indemnit_fonction_net-es.indemnit_voiture_net-es.indemnit_represent_net-es.allocationfamil) as Assiette"
                    + " from hr_etat_salaire  as es inner join hr_employee as e on es.employee = e.id where es.mois=?1 and es.annee=?2 and e.regem=2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat_salaf = somme_Assiette.getSingleResult();
    float somme_salaf = (BigDecimal.ZERO).floatValue();
    if (etat_salaf != null) {
      somme_salaf = ((BigDecimal) etat_salaf).floatValue();
    }
    double number_salaf = somme_salaf;
    String montant_salaf = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_salaf));
    javax.persistence.Query somme_Assiette1 =
        JPA.em()
            .createNativeQuery(
                "SELECT sum(es.total_salair_mensuel_brut_imposable-es.indemnite_logem_brut-es.indemnit_fonction_net-es.indemnit_voiture_net-es.indemnit_represent_net-es.allocationfamil) as Assiette"
                    + " from hr_etat_salaire  as es inner join hr_employee as e on es.employee = e.id where es.mois=?1 and es.annee=?2 and e.regem=1")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat_salaf1 = somme_Assiette1.getSingleResult();
    float somme_salaf1 = (BigDecimal.ZERO).floatValue();
    if (etat_salaf1 != null) {
      somme_salaf1 = ((BigDecimal) etat_salaf1).floatValue();
    }
    double number_salaf1 = somme_salaf1;
    String montant_salaf1 =
        ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_salaf1));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.etatdetailverselent, "ETAT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("lettre_montant", montant_salaf)
            .addParam("lettre_montant1", montant_salaf1)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("ETAT").add("html", fileLink).map());
  }

  //    public void Imprimer_DECOMPTE(ActionRequest request, ActionResponse response)
  //            throws AxelorException {
  //
  //         if (request.getContext().get("mois") == null) {
  //            response.setError("Le champ <b>Mois</b> est obligatoire");
  //            return;
  //        }
  //        if (request.getContext().get("annee") == null) {
  //            response.setError("Le champ <b>Année</b> est obligatoire");
  //            return;
  //        }
  //        Integer mois = (Integer) request.getContext().get("mois");
  //        Integer annee = (Integer) request.getContext().get("annee");
  //
  //        Integer id = (Integer) request.getContext().get("_id");
  //
  //
  //
  //        String fileLink =
  //                ReportFactory.createReport(com.axelor.apps.hr.report.IReport.etatdedecompteir,
  // "ETAT DE DECOMPTE IR")
  //                        .addParam("Locale", ReportSettings.getPrintingLocale(null))
  //                        .addParam("annee", annee)
  //                        .addParam("mois", mois)
  ////                        .addParam("id", id)
  //                        .generate()
  //                        .getFileLink();
  //        response.setView(ActionView.define("ETAT DE DECOMPTE IR").add("html", fileLink).map());
  //    }

  public void Etatdecompteir(ActionRequest request, ActionResponse response)
      throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    Integer id = (Integer) request.getContext().get("_id");

    int nbr = employeeAdvanceService.moisdoz(annee);
    if (nbr == 1) {
      javax.persistence.Query information_agent =
          JPA.em()
              .createNativeQuery(
                  "select  sum(es.indemnit_fonction_net)+sum(es.indemnit_voiture_net)+sum(es.indemnit_represent_net)+"
                      + "sum(es.rcar_rg)+sum(es.rcarrcomp)+sum(mutuelle_mgpapsm)+sum(mutuelle_omfam_sm)+sum(mutuelle_mgpap_ccd)+sum(mutuelle_omfam_caad) "
                      + "as dedu   from hr_etat_salaire as es inner join hr_employee as e on es.employee =e.id where es.annee=?1 and e.id=?2")
              .setParameter(1, annee)
              .setParameter(2, id);

      Object etat_salaf = information_agent.getSingleResult();
      float somme_salaf = (BigDecimal.ZERO).floatValue();
      if (etat_salaf != null) {
        somme_salaf = ((BigDecimal) etat_salaf).floatValue();
      }
      javax.persistence.Query brut_agent =
          JPA.em()
              .createNativeQuery(
                  "select  sum(es.traitement_debase)+sum(es.indemnite_de_residence)+sum(es.ias_indhier_ind_de_tech)+sum(es.indemnite_de_sujetion)+sum(es.indemnite_dencadrement)+sum(es.indemnite_logem_brut) as brut    from hr_etat_salaire as es inner join hr_employee as e on es.employee =e.id where es.annee=?1 and e.id=?2")
              .setParameter(1, annee)
              .setParameter(2, id);

      Object etat_salaf1 = brut_agent.getSingleResult();
      float somme_salaf1 = (BigDecimal.ZERO).floatValue();
      if (etat_salaf1 != null) {
        somme_salaf1 = ((BigDecimal) etat_salaf1).floatValue();
      }

      javax.persistence.Query net =
          JPA.em()
              .createNativeQuery(
                  "select sum(es.netapayer)  from hr_employee as e inner join configuration_grade as g on e.grade = g.id    inner join  hr_etat_salaire as es on e.id = es.employee inner join configuration_echelle as ec on e.echelle = ec.id  inner join configuration_echelon as ech on e.echelon = ech.id   inner join configuration_gestion_salaire as cs on e.echelle = cs.id    where   es.annee=?1 and e.id=?2")
              .setParameter(1, annee)
              .setParameter(2, id);

      Object etat_salaf2 = net.getSingleResult();
      float somme_salaf2 = (BigDecimal.ZERO).floatValue();
      if (etat_salaf2 != null) {
        somme_salaf2 = ((BigDecimal) etat_salaf2).floatValue();
      }

      javax.persistence.Query ir =
          JPA.em()
              .createNativeQuery(
                  "select  sum(es.ir) as brut  from hr_etat_salaire as es inner join hr_employee as e on es.employee =e.id where es.annee=?1 and e.id=?2")
              .setParameter(1, annee)
              .setParameter(2, id);

      Object etat_salaf3 = ir.getSingleResult();
      float somme_salaf3 = (BigDecimal.ZERO).floatValue();
      if (etat_salaf3 != null) {
        somme_salaf3 = ((BigDecimal) etat_salaf3).floatValue();
      }

      javax.persistence.Query irmois =
          JPA.em()
              .createNativeQuery(
                  "select  sum(es.ir) as brut  from hr_etat_salaire as es inner join hr_employee as e on es.employee =e.id where es.annee=?1 and e.id=?2 and es.mois=?3")
              .setParameter(1, annee)
              .setParameter(2, id)
              .setParameter(3, mois);

      Object etat_salaf4 = irmois.getSingleResult();
      float somme_salaf4 = (BigDecimal.ZERO).floatValue();
      if (etat_salaf4 != null) {
        somme_salaf4 = ((BigDecimal) etat_salaf4).floatValue();
      }

      response.setReadonly("annee", true);
      response.setHidden("showDECOMPTE", false);
      LocalDate today = LocalDate.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
      String daynow = today.format(formatter);
      String fileLink =
          ReportFactory.createReport(com.axelor.apps.hr.report.IReport.Etatdecompteirnowork, "ETAT")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .addParam("annee", annee)
              .addParam("mois", mois)
              .addParam("id", id)
              .addParam("daynow", daynow)
              .addParam("somme_salaf", somme_salaf)
              .addParam("somme_salaf1", somme_salaf1)
              .addParam("somme_salaf2", somme_salaf2)
              .addParam("somme_salaf3", somme_salaf3)
              .addParam("somme_salaf4", somme_salaf4)
              .addParam("moisString", tableau_mois_francais[mois - 1])
              .generate()
              .getFileLink();
      response.setView(ActionView.define("ETAT").add("html", fileLink).map());
    } else {

      response.setReadonly("annee", false);
      response.setHidden("showDECOMPTE", true);
      response.setFlash("Veuillez imprimer l'état du salaire pour le mois de décembre");
    }
  }
}
