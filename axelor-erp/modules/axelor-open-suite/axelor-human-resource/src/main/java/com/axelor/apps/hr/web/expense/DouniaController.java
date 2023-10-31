package com.axelor.apps.hr.web.expense;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.configuration.db.repo.IntitulerCreditRepository;
import com.axelor.apps.configuration.db.repo.MUTUELLERepository;
import com.axelor.apps.configuration.service.ConvertNomreToLettres;
import com.axelor.apps.configuration.service.MyConfigurationService;
import com.axelor.apps.configuration.service.RunSqlRequestForMe;
import com.axelor.apps.hr.db.OrdreVirement;
import com.axelor.apps.hr.db.repo.OrdreVirementRepository;
import com.axelor.apps.hr.report.IReport;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Transactional;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Singleton
public class DouniaController {
  
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
  private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Inject
  EtatSalaireServiceImpl etatSalaireServiceImpl;
  @Inject
  MyConfigurationService appConfService;
  @Inject
  AppHumanResourceService appHumanResourceService;
  @Inject
  EmployeeAdvanceService employeeAdvanceService;
  @Inject
  MUTUELLERepository mutuelleRepository;
  @Inject
  IntitulerCreditRepository intitulerCreditRepository;
  @Inject
  OrdreVirementRepository ordreVirementRepository;
  @Inject
  EmployeeAdvanceService appservice;
  
  public Object excuteRequette(int mois, int annee, String req) {
    Object etat = null;
    
    javax.persistence.Query query =
            JPA.em().createNativeQuery(req).setParameter(1, mois).setParameter(2, annee);
    etat = query.getSingleResult();
    if (etat == null) {
      etat = new String[]{".", "0", "0"};
    }
    
    return etat;
  }
  
  public void SaveOrdreVirement(
          String benificaire,
          String objet,
          String pieces,
          BigDecimal montant,
          Integer annee,
          Integer mois) {
    OrdreVirement ordreVirement = new OrdreVirement();
    ordreVirement.setDatreOrdre(LocalDate.now());
    ordreVirement.setBeneficiaireName(benificaire);
    ordreVirement.setMontant(montant);
    ordreVirement.setObjetVirement(objet);
    ordreVirement.setPieceProduiAppui(pieces);
    ordreVirement.setAnnee(annee);
    ordreVirement.setMois(mois);
    appservice.saveOrderVirement(ordreVirement);
  }
  
  @Transactional
  public void BordereauEmission(ActionRequest request, ActionResponse response)
          throws AxelorException {
    
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    List<OrdreVirement> ordreVirements =
            ordreVirementRepository
                    .all()
                    .filter("self.annee=:annee and self.mois=:mois")
                    .bind("annee", annee)
                    .bind("mois", mois)
                    .fetch();
    
    javax.persistence.Query req_pl =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.salaire_plafonne) from hr_etat_salaire es left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_pl = req_pl.getSingleResult();
    
    BigDecimal somme_pl = BigDecimal.ZERO;
    if (obj_pl != null) {
      somme_pl = (BigDecimal) obj_pl;
    }
    
    /////////////////////////////// ***************** NET
    // *****************///////////////////////////////

    /*if(ordreVirements.size()!=0){
      String req = "";
      for (OrdreVirement tmp : ordreVirements) {
        req = "delete from hr_ordre_virement where id = " + tmp.getId() + ";";
        appservice.deleteByQuery(new String[] {req});
      }
    }*/
    
    javax.persistence.Query req_net =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.netapayer+es.netapayer_rappel) as net\n"
                                    + "               from hr_etat_salaire es  where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_net = req_net.getSingleResult();
    BigDecimal somme_net = BigDecimal.ZERO;
    if (obj_net != null) {
      somme_net = ((BigDecimal) obj_net);
      SaveOrdreVirement(
              "NET",
              "Salaires du mois : " + mois + "/" + String.valueOf(annee).substring(2) + " + Indemnités",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              somme_net,
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** EQDOM
    // *****************///////////////////////////////
    
    javax.persistence.Query req_eqdom =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where es.mois=?1 and es.annee=?2 and gc.intituler=2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_eqdom = req_eqdom.getSingleResult();
    float somme_eqdom = (BigDecimal.ZERO).floatValue();
    if (obj_eqdom != null) {
      somme_eqdom = ((BigDecimal) obj_eqdom).floatValue();
      
      SaveOrdreVirement(
              "EQDOM",
              "Précomptes sur salaires mois : "
                      + mois
                      + "/"
                      + String.valueOf(annee).substring(2)
                      + " * EQDOM *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_eqdom),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** AXA credit
    // *****************///////////////////////////////
    
    javax.persistence.Query req_salaf =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where es.mois=?1 and es.annee=?2 and gc.intituler=1")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_salaf = req_salaf.getSingleResult();
    float somme_salaf = (BigDecimal.ZERO).floatValue();
    if (obj_salaf != null) {
      somme_salaf = ((BigDecimal) obj_salaf).floatValue();
    }
    
    /////////////////////////////// ***************** CIH
    // *****************///////////////////////////////
    
    javax.persistence.Query req_sgmb =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where es.mois=?1 and es.annee=?2 and gc.intituler=3")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_sgmb = req_sgmb.getSingleResult();
    float somme_sgmb = (BigDecimal.ZERO).floatValue();
    if (obj_sgmb != null) {
      somme_sgmb = ((BigDecimal) obj_sgmb).floatValue();
    }
    
    /////////////////////////////// ***************** BCP
    // *****************///////////////////////////////
    
    javax.persistence.Query req_bcp =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where es.mois=?1 and es.annee=?2 and gc.intituler=5")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_bcp = req_bcp.getSingleResult();
    float somme_bcp = (BigDecimal.ZERO).floatValue();
    if (obj_bcp != null) {
      somme_bcp = ((BigDecimal) obj_bcp).floatValue();
    }
    
    /////////////////////////////// ***************** RCAR
    // *****************///////////////////////////////
    String req_rcar =
            "select sum(es.rcar_rg+(es.rcar_rappel)) as rcar, sum(rcarrcomp+(comp_rappel)) as complement from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    
    javax.persistence.Query req_logement =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.indemnite_logem_brut) as ir\n"
                                    + "               from hr_etat_salaire es  where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object req_log = req_logement.getSingleResult();
    float etat_log = (BigDecimal.ZERO).floatValue();
    float etat_log_p = (BigDecimal.ZERO).floatValue();
    if (req_log != null) {
      etat_log = ((BigDecimal) req_log).floatValue();
    }
    
    Object etat_rcar = excuteRequette(mois, annee, req_rcar);
    Object[] obj_rcar = (Object[]) etat_rcar;
    BigDecimal rcar_log = BigDecimal.valueOf(etat_log);
    rcar_log = rcar_log.multiply(new BigDecimal(0.03));
    float somme_rcar = ((BigDecimal) obj_rcar[0]).floatValue();
    float somme_rcar_s =
            ((BigDecimal) obj_rcar[0]).floatValue() + ((BigDecimal) obj_rcar[1]).floatValue();
    etat_log_p = (somme_rcar * 2) + ((BigDecimal) obj_rcar[1]).floatValue();
    
    SaveOrdreVirement(
            "R.C.A.R",
            "Cotisations salariales et Patronales du mois : "
                    + mois
                    + "/"
                    + String.valueOf(annee).substring(2)
                    + " * RCAR *",
            "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
            BigDecimal.valueOf(somme_rcar_s + etat_log_p),
            annee,
            mois);
    
    /////////////////////////////// ***************** CNSS
    // *****************///////////////////////////////
    javax.persistence.Query req_cnss =
            JPA.em()
                    .createNativeQuery(
                            "select sum(he.montant_cnss+(es.cnss_rappel)) as cnss from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_cnss = req_cnss.getSingleResult();
    float somme_cnss = (BigDecimal.ZERO).floatValue();
    if (etat_cnss != null) {
      somme_cnss = ((BigDecimal) etat_cnss).floatValue();
    }
    
    /////////////////////////////// ***************** AMO
    // *****************///////////////////////////////
    
    javax.persistence.Query req_amo =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.amo+(es.amo_rappel)) as amo from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_amo = req_amo.getSingleResult();
    float somme_amo = (BigDecimal.ZERO).floatValue();
    if (etat_amo != null) {
      somme_amo = ((BigDecimal) etat_amo).floatValue();
      SaveOrdreVirement(
              "CNOPS",
              "Cotisations salariales et Patronales du mois : "
                      + mois
                      + "/"
                      + String.valueOf(annee).substring(2)
                      + " * CNOPS *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_amo),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** OMFAM SM
    // *****************///////////////////////////////
    
    javax.persistence.Query req_sm_omfam =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_omfam_sm+(es.mutuelle_omfam_sm_rappel)) as sm from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_sm_omfam = req_sm_omfam.getSingleResult();
    float somme_sm_omfam = (BigDecimal.ZERO).floatValue();
    if (etat_sm_omfam != null) {
      somme_sm_omfam = ((BigDecimal) etat_sm_omfam).floatValue();
      SaveOrdreVirement(
              "OMFAM SM",
              "Secteur mutualiste du mois : "
                      + mois
                      + "/"
                      + String.valueOf(annee).substring(2)
                      + " * Mutuelle OMFAM *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_sm_omfam),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** OMFAM CAAD
    // *****************///////////////////////////////
    
    javax.persistence.Query req_caad_omfam =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_omfam_caad+(es.mutuelle_omfam_caad_rappel)) as caad from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_caad_omfam = req_caad_omfam.getSingleResult();
    float somme_caad_omfam = (BigDecimal.ZERO).floatValue();
    if (etat_caad_omfam != null) {
      somme_caad_omfam = ((BigDecimal) etat_caad_omfam).floatValue();
      SaveOrdreVirement(
              "OMFAM CAAD",
              "CAAD du mois : "
                      + mois
                      + "/"
                      + String.valueOf(annee).substring(2)
                      + " Mutuelle * OMFAM *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_sm_omfam),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** MGPAP SM KHENIFRA
    // *****************///////////////////////////////
    javax.persistence.Query req_sm_r =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpapsm+(es.mutuelle_mgpapsm_rappel)) as sm_r"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "     left join configuration_annexe_generale ag on he.annexe = ag.id"
                                    + "    where es.mois=?1 and es.annee=?2 and ag.id=311")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_sm_r = req_sm_r.getSingleResult();
    float somme_sm_r = (BigDecimal.ZERO).floatValue();
    if (etat_sm_r != null) {
      somme_sm_r = ((BigDecimal) etat_sm_r).floatValue();
      SaveOrdreVirement(
              "MGPAP SM 311",
              "Secteur mutualiste du mois : "
                      + mois
                      + "/"
                      + String.valueOf(annee).substring(2)
                      + " * MGPAP *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_sm_r),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** MGPAP SM KHERIBGA
    // *****************///////////////////////////////
    javax.persistence.Query req_sm_g =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpapsm+(es.mutuelle_mgpapsm_rappel)) as sm_ra"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "     left join configuration_annexe_generale ag on he.annexe = ag.id"
                                    + "    where es.mois=?1 and es.annee=?2 and ag.id=312")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_sm_g = req_sm_g.getSingleResult();
    float somme_sm_g = (BigDecimal.ZERO).floatValue();
    if (etat_sm_g != null) {
      somme_sm_g = ((BigDecimal) etat_sm_g).floatValue();
      SaveOrdreVirement(
              "MGPAP SM 312",
              "Secteur mutualiste du mois : "
                      + mois
                      + "/"
                      + String.valueOf(annee).substring(2)
                      + " * MGPAP *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_sm_g),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** MGPAP CCD KHENIFRA
    // *****************///////////////////////////////
    javax.persistence.Query req_ccd_r =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpap_ccd+(es.mutuelle_mgpap_ccd_rappel)) as ccd_ra"
                                    + " from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + " left join configuration_annexe_generale ag on he.annexe = ag.id"
                                    + " where ag.id=311 and es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_ccd_r = req_ccd_r.getSingleResult();
    float somme_ccd_r = (BigDecimal.ZERO).floatValue();
    if (etat_ccd_r != null) {
      somme_ccd_r = ((BigDecimal) etat_ccd_r).floatValue();
      SaveOrdreVirement(
              "CCD SM 311",
              "CCD du mois : " + mois + "/" + String.valueOf(annee).substring(2) + " * MGPAP *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_ccd_r),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** MGPAP CCD KHERIBGA
    // *****************///////////////////////////////
    javax.persistence.Query req_ccd_g =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpap_ccd+(es.mutuelle_mgpap_ccd_rappel)) as ccd_ra"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "     left join configuration_annexe_generale ag on he.annexe = ag.id"
                                    + "    where es.mois=?1 and es.annee=?2 and ag.id=312")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_ccd_g = req_ccd_g.getSingleResult();
    float somme_ccd_g = (BigDecimal.ZERO).floatValue();
    if (etat_ccd_g != null) {
      somme_ccd_g = ((BigDecimal) etat_ccd_g).floatValue();
      SaveOrdreVirement(
              "CCD SM 312",
              "CCD du mois : " + mois + "/" + String.valueOf(annee).substring(2) + " * MGPAP *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_ccd_g),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** IR
    // *****************///////////////////////////////
    
    javax.persistence.Query req_ir =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.ir+(es.ir_rappel)) as ir from hr_etat_salaire es\n"
                                    + "        left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_ir = req_ir.getSingleResult();
    float somme_ir = (BigDecimal.ZERO).floatValue();
    if (obj_ir != null) {
      somme_ir = ((BigDecimal) obj_ir).floatValue();
      SaveOrdreVirement(
              "IR",
              "IR mois " + mois + "/" + String.valueOf(annee).substring(2),
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_eqdom),
              annee,
              mois);
    }
    
    BigDecimal total1 =
            somme_pl
                    .add(somme_net)
                    .add(BigDecimal.valueOf(somme_eqdom))
                    .add(BigDecimal.valueOf(somme_salaf))
                    .add(BigDecimal.valueOf(somme_sgmb))
                    .add(BigDecimal.valueOf(somme_bcp))
                    .add(BigDecimal.valueOf(somme_ir))
                    .add(BigDecimal.valueOf(somme_rcar_s))
                    .add(BigDecimal.valueOf(somme_amo))
                    .add(BigDecimal.valueOf(somme_sm_omfam))
                    .add(BigDecimal.valueOf(somme_caad_omfam))
                    .add(BigDecimal.valueOf(somme_ccd_g))
                    .add(BigDecimal.valueOf(somme_ccd_r))
                    .add(BigDecimal.valueOf(somme_sm_g))
                    .add(BigDecimal.valueOf(somme_sm_r));
    
    BigDecimal total2 =
            total1.add(BigDecimal.valueOf(etat_log_p)).add(BigDecimal.valueOf(somme_amo));
    
    String fileLink =
            ReportFactory.createReport(
                            com.axelor.apps.hr.report.IReport.BordereauDemission, "Bordereau d'emission")
                    .addParam("Locale", ReportSettings.getPrintingLocale(null))
                    .addParam("annee", annee)
                    .addParam("mois", mois)
                    .addParam("moisString", tableau_mois_francais[mois - 1])
                    .addParam("somme_net", somme_net)
                    .addParam("somme_pl", somme_pl)
                    .addParam("somme_eqdom", somme_eqdom)
                    .addParam("somme_salaf", somme_salaf)
                    .addParam("somme_sgmb", somme_sgmb)
                    .addParam("somme_bcp", somme_bcp)
                    .addParam("somme_ir", somme_ir)
                    .addParam("somme_rcar", somme_rcar)
                    .addParam("somme_rcar_s", somme_rcar_s)
                    .addParam("somme_cnss", somme_cnss)
                    .addParam("somme_amo", somme_amo)
                    .addParam("somme_sm_omfam", somme_sm_omfam)
                    .addParam("somme_caad_omfam", somme_caad_omfam)
                    .addParam("somme_ccd_r", somme_ccd_r)
                    .addParam("somme_sm_r", somme_sm_r)
                    .addParam("somme_sm_g", somme_sm_g)
                    .addParam("somme_ccd_g", somme_ccd_g)
                    .addParam("etat_log_p", etat_log_p)
                    .addParam("total1", total1)
                    .addParam("total2", total2)
                    .addParam("Lieu", "Emis à Béni Mellal, le")
                    .generate()
                    .getFileLink();
    response.setView(
            ActionView.define("Bordereau d'emission du personnel de la CCIS")
                    .add("html", fileLink)
                    .map());
  }
  
  public void OrdrePaiement(ActionRequest request, ActionResponse response) throws AxelorException {
    
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    /////////////////////////////// ***************** indemnite
    // *****************///////////////////////////////
    
    String req_ind =
            "select  sum(es.indemnit_fonction_net) as fonction,\n"
                    + "       sum(es.indemnit_voiture_net) as voiture,\n"
                    + "       sum(es.indemnit_represent_net) as represent,\n"
                    + "       sum(es.netapayer+(es.netapayer_rappel)) as salaires\n"
                    + "from hr_etat_salaire es  where es.mois=?1 and es.annee=?2";
    Object etat_ind = excuteRequette(mois, annee, req_ind);
    Object[] obj_ind = (Object[]) etat_ind;

    /*BigDecimal x = (BigDecimal) obj_ind[0];

    x =
            x.subtract(
                    ((x.subtract((x.multiply(new BigDecimal(0.03))))).multiply(new BigDecimal(0.38)))
                            .add(x.multiply(new BigDecimal(0.03))));*/
    
    float somme_ind = ((BigDecimal) obj_ind[3]).floatValue();
    BigDecimal somme_ind2 = ((BigDecimal) obj_ind[3]);
    float somme_net =
            ((BigDecimal) obj_ind[0]).floatValue()
                    + ((BigDecimal) obj_ind[1]).floatValue()
                    + ((BigDecimal) obj_ind[2]).floatValue();
    double number_ind = (Math.round(somme_ind * 100)) / 100.0;
    String montant_ind = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_ind));
    
    float net = somme_ind + somme_net;
    
    /////////////////////////////// ***************** IR
    // *****************///////////////////////////////
    
    String req_ir =
            "select sum(es.ir+(es.ir_rappel)) as ir,\n"
                    + "               sum(es.ir617131) as irInlogement\n"
                    + "               from hr_etat_salaire es  where es.mois=?1 and es.annee=?2";
    
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
    
    Object etat_ir = excuteRequette(mois, annee, req_ir);
    Object[] obj_ir = (Object[]) etat_ir;
    
    BigDecimal somme_ir =
            BigDecimal.valueOf(
                    ((BigDecimal) obj_ir[0]).floatValue() + ((BigDecimal) obj_ir[1]).floatValue());
    BigDecimal round_ir = somme_ir;
    String montant_ir = ConvertNomreToLettres.getStringMontant(round_ir);
    
    BigDecimal ir = BigDecimal.valueOf(etat_log);
    
    ir = ((ir.subtract((ir.multiply(new BigDecimal(0.03))))).multiply(new BigDecimal(0.38)));
    BigDecimal sum_ir = somme_ir.subtract(ir);
    /////////////////////////////// ***************** RCAR
    // *****************///////////////////////////////
    
    String req_rcar =
            "select sum(es.rcar_rg+(es.rcar_rappel)) as salaires,\n"
                    + "                 sum(es.rcarrcomp+(es.comp_rappel)) as logement\n"
                    + "               from hr_etat_salaire es  where es.mois=?1 and es.annee=?2";
    Object etat_rcar = excuteRequette(mois, annee, req_rcar);
    Object[] obj_rcar = (Object[]) etat_rcar;
    BigDecimal rcar_log = BigDecimal.valueOf(etat_log);
    rcar_log = rcar_log.multiply(new BigDecimal(0.03));
    float somme_rcar =
            ((BigDecimal) obj_rcar[0]).floatValue() + ((BigDecimal) obj_rcar[1]).floatValue();
    float total_rcar =
            ((BigDecimal) obj_rcar[0]).floatValue()
                    + ((BigDecimal) obj_rcar[1]).floatValue()
                    - rcar_log.floatValue();
    double number_rcar = (Math.round(somme_rcar * 100)) / 100.0;
    String montant_rcar = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_rcar));
    
    /////////////////////////////// ***************** AMO
    // *****************///////////////////////////////
    
    String req_amo =
            "select sum(es.amo+(es.amo_rappel)) as amo from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat_amo = excuteRequette(mois, annee, req_amo);
    float somme_amo = ((BigDecimal) etat_amo).floatValue();
    double number_amo = (Math.round(somme_amo * 100)) / 100.0;
    String montant_amo = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_amo));
    
    /////////////////////////////// ***************** MGPAP CCD KHENIFRA
    // *****************///////////////////////////////
    javax.persistence.Query req_ccd_r =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpap_ccd+(es.mutuelle_mgpap_ccd_rappel)) as ccd_ra"
                                    + " from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + " where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_ccd_r = req_ccd_r.getSingleResult();
    float somme_ccd_r = (BigDecimal.ZERO).floatValue();
    if (etat_ccd_r != null) {
      somme_ccd_r = ((BigDecimal) etat_ccd_r).floatValue();
    }
    double number_ccd_r = (Math.round(somme_ccd_r * 100)) / 100.0;
    String montant_ccd_r = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_ccd_r));
    
    /////////////////////////////// ***************** MGPAP SM KHENIFRA
    // *****************///////////////////////////////
    javax.persistence.Query req_sm_r =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpapsm+(es.mutuelle_mgpapsm_rappel)) as sm_r"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "    where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_sm_r = req_sm_r.getSingleResult();
    float somme_sm_r = (BigDecimal.ZERO).floatValue();
    if (etat_sm_r != null) {
      somme_sm_r = ((BigDecimal) etat_sm_r).floatValue();
    }
    double number_sm_r = (Math.round(somme_sm_r * 100)) / 100.0;
    String montant_sm_r = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_sm_r));
    
    /////////////////////////////// ***************** MGPAP SM KHERIBGA
    // *****************///////////////////////////////
    javax.persistence.Query req_sm_g =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpapsm+(es.mutuelle_mgpapsm_rappel)) as sm_ra"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "     left join configuration_annexe_generale ag on he.annexe = ag.id"
                                    + "    where es.mois=?1 and es.annee=?2 and ag.id=312")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_sm_g = req_sm_g.getSingleResult();
    float somme_sm_g = (BigDecimal.ZERO).floatValue();
    if (etat_sm_g != null) {
      somme_sm_g = ((BigDecimal) etat_sm_g).floatValue();
    }
    double number_sm_g = (Math.round(somme_sm_g * 100)) / 100.0;
    String montant_sm_g = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_sm_g));
    
    /////////////////////////////// ***************** MGPAP CCD KHERIBGA
    // *****************///////////////////////////////
    javax.persistence.Query req_ccd_g =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpap_ccd+(es.mutuelle_mgpap_ccd_rappel)) as ccd_ra"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "    where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_ccd_g = req_ccd_g.getSingleResult();
    float somme_ccd_g = (BigDecimal.ZERO).floatValue();
    if (etat_ccd_g != null) {
      somme_ccd_g = ((BigDecimal) etat_ccd_g).floatValue();
    }
    double number_ccd_g = (Math.round(somme_ccd_g * 100)) / 100.0;
    String montant_ccd_g = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_ccd_g));
    
    /////////////////////////////// ***************** OMFAM SM
    // *****************///////////////////////////////
    
    String req_sm_omfam =
            "select sum(es.mutuelle_omfam_sm+(es.mutuelle_omfam_sm_rappel)) as sm from hr_etat_salaire es where es.mois="
                    + mois
                    + " and es.annee="
                    + annee
                    + "";
    BigDecimal etat_sm_omfam = RunSqlRequestForMe.runSqlRequest_Bigdecimal(req_sm_omfam);
    
    String montant_sm_omfam = ConvertNomreToLettres.getStringMontant(etat_sm_omfam);
    
    /////////////////////////////// ***************** OMFAM CAAD
    // *****************///////////////////////////////
    
    String req_caad_omfam =
            "select sum(es.mutuelle_omfam_caad+(es.mutuelle_omfam_caad_rappel)) as caad from hr_etat_salaire es where es.mois="
                    + mois
                    + " and es.annee="
                    + annee
                    + "";
    BigDecimal etat_caad_omfam = RunSqlRequestForMe.runSqlRequest_Bigdecimal(req_caad_omfam);
    
    String montant_caad_omfam = ConvertNomreToLettres.getStringMontant(etat_caad_omfam);
    
    /////////////////////////////// ***************** EQDOM
    // *****************///////////////////////////////
    javax.persistence.Query req_eqdom =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as eqdom from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where gc.intituler=2 and es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_eqdom = req_eqdom.getSingleResult();
    float somme_eqdom = (BigDecimal.ZERO).floatValue();
    if (etat_eqdom != null) {
      somme_eqdom = ((BigDecimal) etat_eqdom).floatValue();
    }
    double number_eqdom = (Math.round(somme_eqdom * 100)) / 100.0;
    String montant_eqdom = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_eqdom));
    
    /////////////////////////////// ***************** WAFA SALAF
    // *****************///////////////////////////////
    javax.persistence.Query req_salaf =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where gc.intituler=1 and es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_salaf = req_salaf.getSingleResult();
    float somme_salaf = (BigDecimal.ZERO).floatValue();
    if (etat_salaf != null) {
      somme_salaf = ((BigDecimal) etat_salaf).floatValue();
    }
    double number_salaf = (Math.round(somme_salaf * 100)) / 100.0;
    String montant_salaf = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_salaf));
    
    /////////////////////////////// ***************** CIH
    // *****************///////////////////////////////
    javax.persistence.Query req_cih =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where gc.intituler=3 and es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_cih = req_cih.getSingleResult();
    float somme_cih = (BigDecimal.ZERO).floatValue();
    if (etat_salaf != null) {
      somme_cih = ((BigDecimal) etat_cih).floatValue();
    }
    double number_cih = (Math.round(somme_cih * 100)) / 100.0;
    String montant_cih = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_cih));
    
    /////////////////////////////// ***************** AOS
    // *****************///////////////////////////////
    javax.persistence.Query req_cnss =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.aos) as aos"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "    where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_cnss = req_cnss.getSingleResult();
    float somme_cnss = (BigDecimal.ZERO).floatValue();
    if (etat_cnss != null) {
      somme_cnss = ((BigDecimal) etat_cnss).floatValue();
    }
    double number_cnss = (Math.round(somme_cnss * 100)) / 100.0;
    String montant_cnss = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_cnss));
    
    String fileLink =
            ReportFactory.createReport(
                            com.axelor.apps.hr.report.IReport.OrdrePaiement, "Personnel de la CCIS")
                    .addParam("Locale", ReportSettings.getPrintingLocale(null))
                    .addParam("annee", annee)
                    .addParam("mois", mois)
                    .addParam("moisString", tableau_mois_francais[mois - 1])
                    .addParam("Total_ind", somme_ind2)
                    .addParam("Total_ind_net", net)
                    .addParam("Total_ir", somme_ir)
                    .addParam("ir_log", ir)
                    .addParam("sum_ir", sum_ir)
                    .addParam("Total_rcar", total_rcar)
                    .addParam("fonction", obj_ind[0])
                    .addParam("voiture", obj_ind[1])
                    .addParam("represent", obj_ind[2])
                    .addParam("salaires", obj_ind[3])
                    .addParam("ir_sal", obj_ir[0])
                    .addParam("irInlogement", obj_ir[1])
                    .addParam("rcar_sal", somme_rcar)
                    .addParam("rcarInlogement", rcar_log)
                    .addParam("amo_sal", somme_amo)
                    .addParam("ccd_sal_r", somme_ccd_r)
                    .addParam("sm_sal_r", somme_sm_r)
                    .addParam("sm_sal_g", somme_sm_g)
                    .addParam("ccd_sal_g", somme_ccd_g)
                    .addParam("sm_sal_omfam", etat_sm_omfam)
                    .addParam("caad_sal_omfam", etat_caad_omfam)
                    .addParam("sal_eqdom", somme_eqdom)
                    .addParam("sal_salaf", somme_salaf)
                    .addParam("somme_cnss", somme_cnss)
                    .addParam("MontantInd", montant_ind)
                    .addParam("MontantIR", montant_ir)
                    .addParam("MontantRCAR", montant_rcar)
                    .addParam("MontantAMO", montant_amo)
                    .addParam("MontantCCD_R", montant_ccd_r)
                    .addParam("MontantSM_R", montant_sm_r)
                    .addParam("MontantSM_G", montant_sm_g)
                    .addParam("MontantCCD_G", montant_ccd_g)
                    .addParam("MontantSM_OMFAM", montant_sm_omfam)
                    .addParam("MontantCAAD_OMFAM", montant_caad_omfam)
                    .addParam("MontantEQDOM", montant_eqdom)
                    .addParam("MontantSALAF", montant_salaf)
                    .addParam("montant_cnss", montant_cnss)
                    .addParam("logement", BigDecimal.valueOf(0))
                    .addParam("RoundIR", round_ir)
                    .addParam("Lieu", "Emis à Béni Mellal, le")
                    .generate()
                    .getFileLink();
    response.setView(
            ActionView.define("Ordre de paiement du personnel de la CCIS").add("html", fileLink).map());
  }
  
  public void ReleveBancaire(ActionRequest request, ActionResponse response)
          throws AxelorException {
    
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    
    /////////////////////////////// ***************** OMFAM SM
    // *****************///////////////////////////////
    
    String req_sm_omfam =
            "select sum(es.mutuelle_omfam_sm) as sm from hr_etat_salaire es where es.mois="
                    + mois
                    + " and es.annee="
                    + annee
                    + "";
    BigDecimal etat_sm_omfam = RunSqlRequestForMe.runSqlRequest_Bigdecimal(req_sm_omfam);
    
    String montant_sm_omfam = ConvertNomreToLettres.getStringMontant(etat_sm_omfam);
    
    /////////////////////////////// ***************** OMFAM CAAD
    // *****************///////////////////////////////
    
    String req_caad_omfam =
            "select sum(es.mutuelle_omfam_caad) as caad from hr_etat_salaire es where es.mois="
                    + mois
                    + " and es.annee="
                    + annee
                    + "";
    BigDecimal etat_caad_omfam = RunSqlRequestForMe.runSqlRequest_Bigdecimal(req_caad_omfam);
    
    String montant_caad_omfam = ConvertNomreToLettres.getStringMontant(etat_caad_omfam);
    
    /////////////////////////////// ***************** AMO
    // *****************///////////////////////////////
    
    String req_amo =
            "select sum(es.amo) as amo from hr_etat_salaire es where es.mois="
                    + mois
                    + " and es.annee="
                    + annee
                    + "";
    BigDecimal etat_amo = RunSqlRequestForMe.runSqlRequest_Bigdecimal(req_amo);
    float somme_amo = etat_amo.floatValue();
    float total_amo = etat_amo.floatValue() + etat_amo.floatValue();
    
    String montant_amo = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(total_amo));
    
    /////////////////////////////// ***************** RCAR
    // *****************///////////////////////////////
    
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
    BigDecimal rcar_log = BigDecimal.valueOf(etat_log);
    rcar_log = rcar_log.multiply(new BigDecimal(0.03));
    float somme_rcar =
            ((BigDecimal) obj_rcar[0]).floatValue() + ((BigDecimal) obj_rcar[1]).floatValue();
    float rcar = ((BigDecimal) obj_rcar[0]).floatValue();
    float rcar_sal = ((BigDecimal) obj_rcar[0]).floatValue() * 2;
    float compl_rcar = ((BigDecimal) obj_rcar[1]).floatValue();
    float total_rcar = somme_rcar + rcar_sal + rcar + compl_rcar + rcar_sal + compl_rcar;
    double number_rcar = (Math.round(somme_rcar * 100)) / 100.0;
    String montant_rcar = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(total_rcar));
    
    /////////////////////////////// ***************** CNSS
    // *****************///////////////////////////////
    javax.persistence.Query req_cnss =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.aos) as cnss from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_cnss = req_cnss.getSingleResult();
    float somme_cnss = (BigDecimal.ZERO).floatValue();
    if (etat_cnss != null) {
      somme_cnss = ((BigDecimal) etat_cnss).floatValue();
    }
    double number_cnss = (Math.round(somme_cnss * 100)) / 100.0;
    String montant_cnss = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_cnss));
    
    /////////////////////////////// ***************** EQDOM
    // *****************///////////////////////////////
    javax.persistence.Query req_eqdom =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as eqdom from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where gc.intituler=2 and es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_eqdom = req_eqdom.getSingleResult();
    float somme_eqdom = (BigDecimal.ZERO).floatValue();
    if (etat_eqdom != null) {
      somme_eqdom = ((BigDecimal) etat_eqdom).floatValue();
    }
    double number_eqdom = (Math.round(somme_eqdom * 100)) / 100.0;
    String montant_eqdom = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_eqdom));
    
    /////////////////////////////// ***************** WAFA SALAF
    // *****************///////////////////////////////
    javax.persistence.Query req_salaf =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where gc.intituler=1 and es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_salaf = req_salaf.getSingleResult();
    float somme_salaf = (BigDecimal.ZERO).floatValue();
    if (etat_salaf != null) {
      somme_salaf = ((BigDecimal) etat_salaf).floatValue();
    }
    double number_salaf = (Math.round(somme_salaf * 100)) / 100.0;
    String montant_salaf = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_salaf));
    
    /////////////////////////////// ***************** MGPAP khnifra sm
    // *****************///////////////////////////////
    
    javax.persistence.Query req_sm_r =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpapsm) as sm_r"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "    where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_sm_r = req_sm_r.getSingleResult();
    float somme_sm_r = (BigDecimal.ZERO).floatValue();
    if (etat_sm_r != null) {
      somme_sm_r = ((BigDecimal) etat_sm_r).floatValue();
    }
    double number_sm_r = (Math.round(somme_sm_r * 100)) / 100.0;
    String montant_sm_r = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_sm_r));
    
    /////////////////////////////// ***************** MGPAP  Khouribga sm
    // *****************///////////////////////////////
    
    javax.persistence.Query req_sm_kho =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpapsm) as sm_r"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "    where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_sm_kh = req_sm_kho.getSingleResult();
    float somme_sm_kh = (BigDecimal.ZERO).floatValue();
    if (etat_sm_kh != null) {
      somme_sm_kh = ((BigDecimal) etat_sm_kh).floatValue();
    }
    double number_sm_kh = (Math.round(somme_sm_kh * 100)) / 100.0;
    String montant_sm_kh = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_sm_kh));
    
    /////////////////////////////// ***************** MGPAP CCD KHENIFRA
    // *****************///////////////////////////////
    javax.persistence.Query req_ccd_r =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpap_ccd) as ccd_ra"
                                    + " from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + " where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_ccd_r = req_ccd_r.getSingleResult();
    float somme_ccd_r = (BigDecimal.ZERO).floatValue();
    if (etat_ccd_r != null) {
      somme_ccd_r = ((BigDecimal) etat_ccd_r).floatValue();
    }
    double number_ccd_r = (Math.round(somme_ccd_r * 100)) / 100.0;
    String montant_ccd_r = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_ccd_r));
    
    /////////////////////////////// ***************** MGPAP CCD KHERIBGA
    // *****************///////////////////////////////
    javax.persistence.Query req_ccd_g =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpap_ccd) as ccd_ra"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "     left join configuration_annexe_generale ag on he.annexe = ag.id"
                                    + "    where es.mois=?1 and es.annee=?2 and ag.id=312")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_ccd_g = req_ccd_g.getSingleResult();
    float somme_ccd_g = (BigDecimal.ZERO).floatValue();
    if (etat_ccd_g != null) {
      somme_ccd_g = ((BigDecimal) etat_ccd_g).floatValue();
    }
    double number_ccd_g = (Math.round(somme_ccd_g * 100)) / 100.0;
    String montant_ccd_g = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_ccd_g));
    
    javax.persistence.Query somme_Assiette1 =
            JPA.em()
                    .createNativeQuery(
                            "SELECT sum(es.amo+es.amo) as somam from hr_etat_salaire  as es inner join hr_employee as e on es.employee = e.id\n"
                                    + "where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_c1 = somme_Assiette1.getSingleResult();
    float somme_c1 = (BigDecimal.ZERO).floatValue();
    if (etat_c1 != null) {
      somme_c1 = ((BigDecimal) etat_c1).floatValue();
    }
    float releve = somme_c1;
    double number_c1 = somme_c1;
    String montant_c1 = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_c1));
    
    javax.persistence.Query rel_eqdom =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as eqdom from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where gc.intituler=2 and es.mois=?1 and es.annee=?2 and he.id in(132,146)")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_eqdom_r = rel_eqdom.getSingleResult();
    float somme_eqdom_r = (BigDecimal.ZERO).floatValue();
    if (etat_eqdom_r != null) {
      somme_eqdom_r = ((BigDecimal) etat_eqdom_r).floatValue();
    }
    double number_eqdom_r = (Math.round(somme_eqdom_r * 100)) / 100.0;
    String montant_eqdom_r =
            ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_eqdom_r));
    
    /////////////////// exterieur /////////////
    
    javax.persistence.Query rel_exterieur =
            JPA.em()
                    .createNativeQuery(
                            "select sum(cast(LEFT(indemnite_journaliere,  -2) as decimal))+sum(cast(LEFT(frais_indemnite,  -2) as decimal))\n"
                                    + "    as house_no from hr_employee_missions where status_select=3 and type_mission=false and date_part('month', updated_on)=?1 and\n"
                                    + "date_part('year', updated_on)=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_exterieur = rel_exterieur.getSingleResult();
    float somme_exterieur = (BigDecimal.ZERO).floatValue();
    if (etat_exterieur != null) {
      somme_exterieur = ((BigDecimal) etat_exterieur).floatValue();
    }
    double number_exterieur = (Math.round(somme_exterieur * 100)) / 100.0;
    String montant_exterieur =
            ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_exterieur));
    
    /////////////////// interieur /////////////
    
    javax.persistence.Query rel_interieur =
            JPA.em()
                    .createNativeQuery(
                            "select case when sum(cast(LEFT(frais_indemnite,  -2) as decimal)) is not null then sum(cast(LEFT(frais_indemnite,  -2) as decimal)) else 0.00 end\n"
                                    + "    as house_no from hr_employee_missions where status_select=3 and type_mission=true and date_part('month', updated_on)=?1 and\n"
                                    + "date_part('year', updated_on)=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_interieur = rel_interieur.getSingleResult();
    float somme_interieur = (BigDecimal.ZERO).floatValue();
    if (etat_interieur != null) {
      somme_interieur = ((BigDecimal) etat_interieur).floatValue();
    }
    double number_interieur = (Math.round(somme_interieur * 100)) / 100.0;
    String montant_interieur =
            ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number_interieur));
    
    String fileLink =
            ReportFactory.createReport(IReport.ReleveBancaire, "Releve bancaire")
                    .addParam("Locale", ReportSettings.getPrintingLocale(null))
                    .addParam("annee", annee)
                    .addParam("mois", mois)
                    .addParam("moisString", tableau_mois_francais[mois - 1])
                    .addParam("releve", releve)
                    .addParam("montant_c1", montant_c1)
                    .addParam("sm_sal_omfam", etat_sm_omfam)
                    .addParam("caad_sal_omfam", etat_caad_omfam)
                    .addParam("somme_amo", somme_amo)
                    .addParam("total_amo", total_amo)
                    .addParam("somme_eqdom", somme_eqdom)
                    .addParam("somme_salaf", somme_salaf)
                    .addParam("somme_sm_r", somme_sm_r)
                    .addParam("somme_sm_kh", somme_sm_kh)
                    .addParam("somme_ccd_r", somme_ccd_r)
                    .addParam("somme_ccd_g", somme_ccd_g)
                    .addParam("montant_ccd_r", montant_ccd_r)
                    .addParam("montant_ccd_g", montant_ccd_g)
                    .addParam("MontantSM_OMFAM", montant_sm_omfam)
                    .addParam("MontantCAAD_OMFAM", montant_caad_omfam)
                    .addParam("somme_cnss", somme_cnss)
                    .addParam("montant_cnss", montant_cnss)
                    .addParam("Total_rcar", total_rcar)
                    .addParam("montant_rcar", montant_rcar)
                    .addParam("rcar_log", rcar_log)
                    .addParam("rcar_r", rcar)
                    .addParam("compl_rcar", compl_rcar)
                    .addParam("rcar_sal", rcar_sal)
                    .addParam("montant_amo", montant_amo)
                    .addParam("montant_sm_kh", montant_sm_kh)
                    .addParam("montant_eqdom", montant_eqdom)
                    .addParam("montant_salaf", montant_salaf)
                    .addParam("montant_sm_r", montant_sm_r)
                    .addParam("number_exterieur", number_exterieur)
                    .addParam("montant_exterieur", montant_exterieur)
                    .addParam("number_interieur", number_interieur)
                    .addParam("montant_interieur", montant_interieur)
                    .addParam("somme_eqdom_r", somme_eqdom_r)
                    .addParam("montant_eqdom_r", montant_eqdom_r)
                    .addParam("Arrete", "Arrêté le présent relevé bancaire à la somme de :")
                    .generate()
                    .getFileLink();
    response.setView(ActionView.define("Releve bancaire").add("html", fileLink).map());
  }
  
  @Transactional
  public void pageDeGarde(ActionRequest request, ActionResponse response) throws AxelorException {
    
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    List<OrdreVirement> ordreVirements =
            ordreVirementRepository
                    .all()
                    .filter("self.annee=:annee and self.mois=:mois")
                    .bind("annee", annee)
                    .bind("mois", mois)
                    .fetch();
    
    javax.persistence.Query req_pl =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.salaire_plafonne) from hr_etat_salaire es left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_pl = req_pl.getSingleResult();
    
    BigDecimal somme_pl = BigDecimal.ZERO;
    if (obj_pl != null) {
      somme_pl = (BigDecimal) obj_pl;
    }
    
    /////////////////////////////// ***************** NET
    // *****************///////////////////////////////

    /*if(ordreVirements.size()!=0){
      String req = "";
      for (OrdreVirement tmp : ordreVirements) {
        req = "delete from hr_ordre_virement where id = " + tmp.getId() + ";";
        appservice.deleteByQuery(new String[] {req});
      }
    }*/
    
    javax.persistence.Query req_net =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.netapayer+es.netapayer_rappel) as net\n"
                                    + "               from hr_etat_salaire es  where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_net = req_net.getSingleResult();
    BigDecimal somme_net = BigDecimal.ZERO;
    if (obj_net != null) {
      somme_net = ((BigDecimal) obj_net);
      SaveOrdreVirement(
              "NET",
              "Salaires du mois : " + mois + "/" + String.valueOf(annee).substring(2) + " + Indemnités",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              somme_net,
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** EQDOM
    // *****************///////////////////////////////
    
    javax.persistence.Query req_eqdom =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where es.mois=?1 and es.annee=?2 and gc.intituler=3")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_eqdom = req_eqdom.getSingleResult();
    float somme_eqdom = (BigDecimal.ZERO).floatValue();
    if (obj_eqdom != null) {
      somme_eqdom = ((BigDecimal) obj_eqdom).floatValue();
      
      SaveOrdreVirement(
              "EQDOM",
              "Précomptes sur salaires mois : "
                      + mois
                      + "/"
                      + String.valueOf(annee).substring(2)
                      + " * EQDOM *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_eqdom),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** WAFA SALAF
    // *****************///////////////////////////////
    
    javax.persistence.Query req_salaf =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where es.mois=?1 and es.annee=?2 and gc.intituler=2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_salaf = req_salaf.getSingleResult();
    float somme_salaf = (BigDecimal.ZERO).floatValue();
    if (obj_salaf != null) {
      somme_salaf = ((BigDecimal) obj_salaf).floatValue();
    }
    
    /////////////////////////////// ***************** SGMB
    // *****************///////////////////////////////
    
    javax.persistence.Query req_sgmb =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where es.mois=?1 and es.annee=?2 and gc.intituler=4")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_sgmb = req_sgmb.getSingleResult();
    float somme_sgmb = (BigDecimal.ZERO).floatValue();
    if (obj_sgmb != null) {
      somme_sgmb = ((BigDecimal) obj_sgmb).floatValue();
    }
    
    /////////////////////////////// ***************** BCP
    // *****************///////////////////////////////
    
    javax.persistence.Query req_bcp =
            JPA.em()
                    .createNativeQuery(
                            "select sum(gc.remboursement) as salaf from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id\n"
                                    + "                                left join HR_GESTION_CREDIT gc on he.id = gc.employee where es.mois=?1 and es.annee=?2 and gc.intituler=5")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_bcp = req_bcp.getSingleResult();
    float somme_bcp = (BigDecimal.ZERO).floatValue();
    if (obj_bcp != null) {
      somme_bcp = ((BigDecimal) obj_bcp).floatValue();
    }
    
    /////////////////////////////// ***************** RCAR
    // *****************///////////////////////////////
    String req_rcar =
            "select sum(es.rcar_rg+(es.rcar_rappel)) as rcar, sum(rcarrcomp+(comp_rappel)) as complement from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    
    javax.persistence.Query req_logement =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.indemnite_logem_brut) as ir\n"
                                    + "               from hr_etat_salaire es  where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object req_log = req_logement.getSingleResult();
    float etat_log = (BigDecimal.ZERO).floatValue();
    float etat_log_p = (BigDecimal.ZERO).floatValue();
    if (req_log != null) {
      etat_log = ((BigDecimal) req_log).floatValue();
    }
    
    Object etat_rcar = excuteRequette(mois, annee, req_rcar);
    Object[] obj_rcar = (Object[]) etat_rcar;
    BigDecimal rcar_log = BigDecimal.valueOf(etat_log);
    rcar_log = rcar_log.multiply(new BigDecimal(0.03));
    float somme_rcar = ((BigDecimal) obj_rcar[0]).floatValue();
    float somme_rcar_s =
            ((BigDecimal) obj_rcar[0]).floatValue() + ((BigDecimal) obj_rcar[1]).floatValue();
    etat_log_p = (somme_rcar * 2) + ((BigDecimal) obj_rcar[1]).floatValue();
    
    SaveOrdreVirement(
            "R.C.A.R",
            "Cotisations salariales et Patronales du mois : "
                    + mois
                    + "/"
                    + String.valueOf(annee).substring(2)
                    + " * RCAR *",
            "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
            BigDecimal.valueOf(somme_rcar_s + etat_log_p),
            annee,
            mois);
    
    /////////////////////////////// ***************** CNSS
    // *****************///////////////////////////////
    javax.persistence.Query req_cnss =
            JPA.em()
                    .createNativeQuery(
                            "select sum(he.montant_cnss+(es.cnss_rappel)) as cnss from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_cnss = req_cnss.getSingleResult();
    float somme_cnss = (BigDecimal.ZERO).floatValue();
    if (etat_cnss != null) {
      somme_cnss = ((BigDecimal) etat_cnss).floatValue();
    }
    
    /////////////////////////////// ***************** AMO
    // *****************///////////////////////////////
    
    javax.persistence.Query req_amo =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.amo+(es.amo_rappel)) as amo from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_amo = req_amo.getSingleResult();
    float somme_amo = (BigDecimal.ZERO).floatValue();
    if (etat_amo != null) {
      somme_amo = ((BigDecimal) etat_amo).floatValue();
      SaveOrdreVirement(
              "CNOPS",
              "Cotisations salariales et Patronales du mois : "
                      + mois
                      + "/"
                      + String.valueOf(annee).substring(2)
                      + " * CNOPS *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_amo),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** OMFAM SM
    // *****************///////////////////////////////
    
    javax.persistence.Query req_sm_omfam =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_omfam_sm+(es.mutuelle_omfam_sm_rappel)) as sm from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_sm_omfam = req_sm_omfam.getSingleResult();
    float somme_sm_omfam = (BigDecimal.ZERO).floatValue();
    if (etat_sm_omfam != null) {
      somme_sm_omfam = ((BigDecimal) etat_sm_omfam).floatValue();
      SaveOrdreVirement(
              "OMFAM SM",
              "Secteur mutualiste du mois : "
                      + mois
                      + "/"
                      + String.valueOf(annee).substring(2)
                      + " * Mutuelle OMFAM *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_sm_omfam),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** OMFAM CAAD
    // *****************///////////////////////////////
    
    javax.persistence.Query req_caad_omfam =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_omfam_caad+(es.mutuelle_omfam_caad_rappel)) as caad from hr_etat_salaire es\n"
                                    + "                                left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_caad_omfam = req_caad_omfam.getSingleResult();
    float somme_caad_omfam = (BigDecimal.ZERO).floatValue();
    if (etat_caad_omfam != null) {
      somme_caad_omfam = ((BigDecimal) etat_caad_omfam).floatValue();
      SaveOrdreVirement(
              "OMFAM CAAD",
              "CAAD du mois : "
                      + mois
                      + "/"
                      + String.valueOf(annee).substring(2)
                      + " Mutuelle * OMFAM *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_sm_omfam),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** MGPAP SM KHENIFRA
    // *****************///////////////////////////////
    javax.persistence.Query req_sm_r =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpapsm+(es.mutuelle_mgpapsm_rappel)) as sm_r"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "     left join configuration_annexe_generale ag on he.annexe = ag.id"
                                    + "    where es.mois=?1 and es.annee=?2 and ag.id=311")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_sm_r = req_sm_r.getSingleResult();
    float somme_sm_r = (BigDecimal.ZERO).floatValue();
    if (etat_sm_r != null) {
      somme_sm_r = ((BigDecimal) etat_sm_r).floatValue();
      SaveOrdreVirement(
              "MGPAP SM 311",
              "Secteur mutualiste du mois : "
                      + mois
                      + "/"
                      + String.valueOf(annee).substring(2)
                      + " * MGPAP *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_sm_r),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** MGPAP SM KHERIBGA
    // *****************///////////////////////////////
    javax.persistence.Query req_sm_g =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpapsm+(es.mutuelle_mgpapsm_rappel)) as sm_ra"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "     left join configuration_annexe_generale ag on he.annexe = ag.id"
                                    + "    where es.mois=?1 and es.annee=?2 and ag.id=312")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_sm_g = req_sm_g.getSingleResult();
    float somme_sm_g = (BigDecimal.ZERO).floatValue();
    if (etat_sm_g != null) {
      somme_sm_g = ((BigDecimal) etat_sm_g).floatValue();
      SaveOrdreVirement(
              "MGPAP SM 312",
              "Secteur mutualiste du mois : "
                      + mois
                      + "/"
                      + String.valueOf(annee).substring(2)
                      + " * MGPAP *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_sm_g),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** MGPAP CCD KHENIFRA
    // *****************///////////////////////////////
    javax.persistence.Query req_ccd_r =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpap_ccd+(es.mutuelle_mgpap_ccd_rappel)) as ccd_ra"
                                    + " from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + " left join configuration_annexe_generale ag on he.annexe = ag.id"
                                    + " where ag.id=311 and es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_ccd_r = req_ccd_r.getSingleResult();
    float somme_ccd_r = (BigDecimal.ZERO).floatValue();
    if (etat_ccd_r != null) {
      somme_ccd_r = ((BigDecimal) etat_ccd_r).floatValue();
      SaveOrdreVirement(
              "CCD SM 311",
              "CCD du mois : " + mois + "/" + String.valueOf(annee).substring(2) + " * MGPAP *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_ccd_r),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** MGPAP CCD KHERIBGA
    // *****************///////////////////////////////
    javax.persistence.Query req_ccd_g =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.mutuelle_mgpap_ccd+(es.mutuelle_mgpap_ccd_rappel)) as ccd_ra"
                                    + "        from hr_etat_salaire es left join hr_employee he on es.employee = he.id"
                                    + "     left join configuration_annexe_generale ag on he.annexe = ag.id"
                                    + "    where es.mois=?1 and es.annee=?2 and ag.id=312")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object etat_ccd_g = req_ccd_g.getSingleResult();
    float somme_ccd_g = (BigDecimal.ZERO).floatValue();
    if (etat_ccd_g != null) {
      somme_ccd_g = ((BigDecimal) etat_ccd_g).floatValue();
      SaveOrdreVirement(
              "CCD SM 312",
              "CCD du mois : " + mois + "/" + String.valueOf(annee).substring(2) + " * MGPAP *",
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_ccd_g),
              annee,
              mois);
    }
    
    /////////////////////////////// ***************** IR
    // *****************///////////////////////////////
    
    javax.persistence.Query req_ir =
            JPA.em()
                    .createNativeQuery(
                            "select sum(es.ir+(es.ir_rappel)) as ir from hr_etat_salaire es\n"
                                    + "        left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2")
                    .setParameter(1, mois)
                    .setParameter(2, annee);
    Object obj_ir = req_ir.getSingleResult();
    float somme_ir = (BigDecimal.ZERO).floatValue();
    if (obj_ir != null) {
      somme_ir = ((BigDecimal) obj_ir).floatValue();
      SaveOrdreVirement(
              "IR",
              "IR mois " + mois + "/" + String.valueOf(annee).substring(2),
              "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE",
              BigDecimal.valueOf(somme_eqdom),
              annee,
              mois);
    }
    
    BigDecimal total1 =
            somme_pl
                    .add(somme_net)
                    .add(BigDecimal.valueOf(somme_eqdom))
                    .add(BigDecimal.valueOf(somme_salaf))
                    .add(BigDecimal.valueOf(somme_sgmb))
                    .add(BigDecimal.valueOf(somme_bcp))
                    .add(BigDecimal.valueOf(somme_ir))
                    .add(BigDecimal.valueOf(somme_rcar_s))
                    .add(BigDecimal.valueOf(somme_amo))
                    .add(BigDecimal.valueOf(somme_sm_omfam))
                    .add(BigDecimal.valueOf(somme_caad_omfam))
                    .add(BigDecimal.valueOf(somme_ccd_g))
                    .add(BigDecimal.valueOf(somme_ccd_r))
                    .add(BigDecimal.valueOf(somme_sm_g))
                    .add(BigDecimal.valueOf(somme_sm_r));
    
    BigDecimal total =
            total1.add(BigDecimal.valueOf(etat_log_p)).add(BigDecimal.valueOf(somme_amo));
    double total2 = (Math.round(total.floatValue() * 100)) / 100.0;
    String montant_total = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(total2));
    
    String fileLink =
            ReportFactory.createReport(com.axelor.apps.hr.report.IReport.PageDeGarde, "Page de garde")
                    .addParam("Locale", ReportSettings.getPrintingLocale(null))
                    .addParam("annee", annee)
                    .addParam("mois", mois)
                    .addParam("moisString", tableau_mois_francais[mois - 1])
                    .addParam("total2", total2)
                    .addParam("montantString", montant_total)
                    .generate()
                    .getFileLink();
    response.setView(ActionView.define("Page de garde").add("html", fileLink).map());
  }
}
