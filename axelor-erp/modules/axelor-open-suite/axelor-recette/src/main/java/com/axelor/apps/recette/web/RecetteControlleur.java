package com.axelor.apps.recette.web;

import com.axelor.apps.*;
import com.axelor.apps.configuration.db.*;
import com.axelor.apps.recette.db.*;
import com.axelor.apps.recette.report.IReport;
import com.axelor.apps.recette.service.ServiceRecette;
import com.axelor.apps.report.engine.*;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.exception.AxelorException;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import groovy.lang.Singleton;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

@Singleton
public class RecetteControlleur {

  @Inject private ServiceRecette appservice;

  public void showformPersonne(ActionRequest request, ActionResponse response) {
    boolean type = (boolean) request.getContext().get("typePersonne");
    if (type) {
      response.setHidden("p_physique", true);
      response.setHidden("p_morale", false);
      response.setValue("type", 1);
    } else {
      response.setHidden("p_morale", true);
      response.setHidden("p_physique", false);
      response.setValue("type", 1);
    }
  }

  public void getTypecontroller(ActionRequest request, ActionResponse response) {
    response.setValue("type", 2);
  }

  public void showformTaxe(ActionRequest request, ActionResponse response) {
    boolean type = (boolean) request.getContext().get("typeDelivrance");
    if (type) {
      response.setHidden("p_taxe", true);
      response.setHidden("p_model", false);
    } else {
      response.setHidden("p_model", true);
      response.setHidden("p_taxe", false);
    }
  }

  public void imprimmerRessortissant(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    String language = String.valueOf(request.getContext().get("lang_Type").toString());
    String dateF = request.getContext().get("dateFacture").toString();
    String numero = request.getContext().get("numero").toString();
    // String valeur= request.getContext().get("valeur").toString();
    Integer id_ = (Integer) request.getContext().get("_id");
    Ressortissant r = appservice.getRessortissantById(Long.valueOf(id_));
    BigDecimal v = r.getValeur();
    Integer id_etatSomme = (Integer) request.getContext().get("_id");
    Long id = id_etatSomme.longValue();
    SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
    Date d = dt.parse(dateF);
    SimpleDateFormat dt1 = new SimpleDateFormat("dd/MM/yyyy");
    String dateFacture = dt1.format(d);
    switch (language) {
      case "français":
        {
          SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
          Date date = new Date();
          String fileLink =
              ReportFactory.createReport(IReport.CertificatFR, "Certificat d'origin")
                  .addParam("Locale", ReportSettings.getPrintingLocale(null))
                  .addParam("language", language)
                  .addParam("dateFacture", dateFacture)
                  .addParam("numero", numero)
                  .addParam("v", v)
                  .addParam("id", id)
                  .addParam("dateformat", formatter.format(date))
                  .addParam("year", Year.now().getValue())
                  .generate()
                  .getFileLink();
          response.setView(ActionView.define("Certificat d'origin").add("html", fileLink).map());
          break;
        }
      case "es":
        {
          SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
          Date date = new Date();
          String fileLink =
              ReportFactory.createReport(IReport.CertificatES, "Certificado de origen")
                  .addParam("Locale", ReportSettings.getPrintingLocale(null))
                  .addParam("language", language)
                  .addParam("dateFacture", dateFacture)
                  .addParam("numero", numero)
                  .addParam("v", v)
                  .addParam("id", id)
                  .addParam("dateformat", formatter.format(date))
                  .addParam("year", Year.now().getValue())
                  .generate()
                  .getFileLink();
          response.setView(ActionView.define("Certificado de origen").add("html", fileLink).map());
          break;
        }
      case "anglais":
        {
          SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
          Date date = new Date();
          String fileLink =
              ReportFactory.createReport(IReport.CertificatEN, "Certificate of origin")
                  .addParam("Locale", ReportSettings.getPrintingLocale(null))
                  .addParam("language", language)
                  .addParam("dateFacture", dateFacture)
                  .addParam("numero", numero)
                  .addParam("v", v)
                  .addParam("id", id)
                  .addParam("dateformat", formatter.format(date))
                  .addParam("year", Year.now().getValue())
                  .generate()
                  .getFileLink();
          response.setView(ActionView.define("Certificate of origin").add("html", fileLink).map());
          break;
        }
      case "arabe":
        {
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
          Date parse = sdf.parse(dateF);
          Calendar calendar = Calendar.getInstance();
          calendar.setTime(parse);
          int date1 = calendar.get(Calendar.DATE);
          String jrs = "";
          if (date1 < 10) {
            jrs = "0" + date1;
          } else {
            jrs = String.valueOf(date1);
          }
          int month = calendar.get(Calendar.MONTH) + 1;
          String m = "";
          switch (month) {
            case 1:
              m = "يناير";
              break;
            case 2:
              m = "فبراير";
              break;
            case 3:
              m = "مارس";
              break;
            case 5:
              m = "ماي";
              break;
            case 6:
              m = "يونيو";
              break;
            case 7:
              m = "يوليوز";
              break;
            case 8:
              m = "غشت";
              break;
            case 9:
              m = "شتنبر";
              break;
            case 10:
              m = "أكتوبر";
              break;
            case 11:
              m = "نونبر";
              break;
            case 12:
              m = "دجنبر";
              break;
            default:
              break;
          }
          String year = String.valueOf(calendar.get(Calendar.YEAR));
          SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
          Date date = new Date();
          String fileLink =
              ReportFactory.createReport(IReport.CertificatAR, "شهادة الأصل")
                  .addParam("Locale", ReportSettings.getPrintingLocale(null))
                  .addParam("language", language)
                  .addParam("dateFacture", year)
                  .addParam("mois", m)
                  .addParam("jrs", jrs)
                  .addParam("numero", numero)
                  .addParam("v", v)
                  .addParam("id", id)
                  .addParam("dateformat", formatter.format(date))
                  .addParam("year", Year.now().getValue())
                  .generate()
                  .getFileLink();
          response.setView(ActionView.define("certificat arabe").add("html", fileLink).map());
          break;
        }
    }
  }

  public void imprimmerAttestationModel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String language = String.valueOf(request.getContext().get("language").toString());
    String type = String.valueOf(request.getContext().get("type").toString());

    if (Objects.equals(language, "fr") && Objects.equals(type, "morale")) {
      String fileLink =
          ReportFactory.createReport(IReport.Attestation_Morale_fr, "Attestation Morale Français")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .generate()
              .getFileLink();
      response.setView(
          ActionView.define("Attestation Morale Français").add("html", fileLink).map());
    } else if (Objects.equals(language, "en") && Objects.equals(type, "morale")) {
      String fileLink =
          ReportFactory.createReport(IReport.Attestation_Morale_en, "Attestation Morale Anglais")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .generate()
              .getFileLink();
      response.setView(ActionView.define("Attestation Morale Anglais").add("html", fileLink).map());
    } else if (Objects.equals(language, "ar") && Objects.equals(type, "morale")) {
      String fileLink =
          ReportFactory.createReport(IReport.Attestation_Morale_arabic, "Attestation Morale Arabic")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .generate()
              .getFileLink();
      response.setView(ActionView.define("Attestation Morale Arabic").add("html", fileLink).map());
    } else if (Objects.equals(language, "fr") && Objects.equals(type, "physique")) {
      String fileLink =
          ReportFactory.createReport(
                  IReport.Attestation_Physique_fr, "Attestation Physique Français")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .generate()
              .getFileLink();
      response.setView(
          ActionView.define("Attestation Physique Français").add("html", fileLink).map());
    } else if (Objects.equals(language, "en") && Objects.equals(type, "physique")) {
      String fileLink =
          ReportFactory.createReport(
                  IReport.Attestation_Physique_en, "Attestation Physique Anglais")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .generate()
              .getFileLink();
      response.setView(
          ActionView.define("Attestation Physique Anglais").add("html", fileLink).map());
    } else {
      String fileLink =
          ReportFactory.createReport(
                  IReport.Attestation_Physique_arabic, "Attestation Physique Arabic")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .generate()
              .getFileLink();
      response.setView(
          ActionView.define("Attestation Physique Arabic").add("html", fileLink).map());
    }
  }

  public void imprimmerAttestationTaxe(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String language = String.valueOf(request.getContext().get("language").toString());
    if (Objects.equals(language, "fr")) {
      String fileLink =
          ReportFactory.createReport(IReport.CertificatFR, "Certificat d'origin")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .generate()
              .getFileLink();
      response.setView(ActionView.define("Certificat d'origin").add("html", fileLink).map());
    } else if (Objects.equals(language, "en")) {
      String fileLink =
          ReportFactory.createReport(IReport.CertificatFR, "Certificat d'origin")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .generate()
              .getFileLink();
      response.setView(ActionView.define("Certificat d'origin").add("html", fileLink).map());
    } else {
      String fileLink =
          ReportFactory.createReport(IReport.CertificatFR, "Certificat d'origin")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .generate()
              .getFileLink();
      response.setView(ActionView.define("Certificat d'origin").add("html", fileLink).map());
    }
  }

  public void affecterSelectMetier(ActionRequest request, ActionResponse response) {
    MetierSelect m = request.getContext().asType(MetierSelect.class);
    response.setValue("s_name_fr", m.getEmploi().getNom());
    response.setValue("s_name_ar", m.getEmploi().getNom_ar());
    response.setValue("s_desc", m.getEmploi().getDescription());
  }

  @Transactional
  public void updateRessortissant(ActionRequest request, ActionResponse response) {
    InfoAttestation m = request.getContext().asType(InfoAttestation.class);
    Long id = m.getId();
    Ressortissant r = appservice.getRessortissantById(m.getRessortissant().getId());
    appservice.setHasAttestation(true, r.getId());
  }

  @Transactional
  public void updateRessortissantorigin(ActionRequest request, ActionResponse response) {
    InfoOrigin mr = request.getContext().asType(InfoOrigin.class);
    Long id = mr.getId();
    Ressortissant r = appservice.getRessortissantById(mr.getRessortissant().getId());
    appservice.setHasorigin(true, r.getId());
  }

  public void imrpimer_doc(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    InfoAttestation m = request.getContext().asType(InfoAttestation.class);
    Long id = m.getId();
    String repport = appservice.getReportByInfoAttestation(id, m.getLang_de_type());
    InfoAttestation in = appservice.getInfoAttestationById(id);
    if (m.getRessortissant() == null) {
      response.setFlash("L'attestation ne dispose d'aucun ressortissant");
      return;
    }
    Ressortissant r = appservice.getRessortissantById(m.getRessortissant().getId());
    if (repport.equals("")) {
      response.setFlash("Merci de choisir la langue");
      return;
    }

    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    Date date = new Date();

    SimpleDateFormat ar = new SimpleDateFormat("yyyy/MM/dd");
    Date dta = new Date();

    SimpleDateFormat dt = new SimpleDateFormat("dd-MM-yyyy");
    LocalDate tmp = LocalDate.now();
    LocalDate df = in.getDate_delivration_tribunal();
    LocalDate df2 = in.getDate_delivration();
    tmp = df == null ? df2 : df;
    Instant instant = Instant.from(tmp.atStartOfDay(ZoneId.of("GMT")));
    Date d = Date.from(instant);
    String dateDelivration = "";
    String jrs = "";
    String mois = "";
    String year = "";
    if (m.getLang_de_type().equals("anglais")) {
      DateFormat dt1 = new SimpleDateFormat("MMMM 'the' dd'th' yyyy", Locale.ENGLISH);
      dateDelivration = dt1.format(d);
    } else if (m.getLang_de_type().equals("français")) {
      DateFormat dt1 = new SimpleDateFormat("dd/MM/yyyy");
      dateDelivration = dt1.format(d);
    } else {
      DateFormat dt1 = new SimpleDateFormat("yyyy/MM/dd");
      dateDelivration = dt1.format(d);
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(d);
      int date1 = calendar.get(Calendar.DATE);
      if (date1 < 10) {
        jrs = "0" + date1;
      } else {
        jrs = String.valueOf(date1);
      }
      int month = calendar.get(Calendar.MONTH) + 1;
      switch (month) {
        case 1:
          mois = "يناير";
          break;
        case 2:
          mois = "فبراير";
          break;
        case 3:
          mois = "مارس";
          break;
        case 5:
          mois = "ماي";
          break;
        case 6:
          mois = "يونيو";
          break;
        case 7:
          mois = "يوليوز";
          break;
        case 8:
          mois = "غشت";
          break;
        case 9:
          mois = "شتنبر";
          break;
        case 10:
          mois = "أكتوبر";
          break;
        case 11:
          mois = "نونبر";
          break;
        case 12:
          mois = "دجنبر";
          break;
        default:
          break;
      }
      year = String.valueOf(calendar.get(Calendar.YEAR));
    }

    if (m.getLang_de_type().equals("arabe")) {
      String fileLink =
          ReportFactory.createReport(repport, "Attestation")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .addParam("id", Integer.parseInt(String.valueOf(id)))
              .addParam("nom", r.getNom_fr() + " " + r.getPrenom_fr())
              .addParam("nom_ar", r.getNom_ar() + " " + r.getPrenom_ar())
              .addParam("cin", r.getCin())
              .addParam("adresse", r.getAdresseSociete())
              .addParam("adresse_ar", r.getRueEtNumRue_ste_ar() + "-" + r.getVille_ste_ar())
              .addParam("taxpro", r.getNum_tax())
              .addParam("rc", r.getNum_rc())
              .addParam("societe", r.getRaisonSocial_ste())
              .addParam("societe_ar", r.getRaisonSocial_steAr())
              .addParam("taxeAttes", in.getTaxe_attes())
              .addParam("dateAttes", dateDelivration == null ? "-" : dateDelivration)
              .addParam("list", r.getMetiers())
              .addParam("year", Year.now().getValue())
              .addParam(
                  "date_laivaison",
                  in.getDate_delivration() == null
                      ? "-"
                      : in.getDate_delivration().toString()
                          + " "
                          + "بناء على الشهادة التسجيل في الرسم المهني المسلمة بتاريخ ")
              .addParam("dariba", "   من طرف المديرية  الجهوية للضرائب بفاس ")
              .addParam(
                  "date_laivaison_ar",
                  in.getDate_delivration() == null
                      ? "-"
                      : in.getDate_delivration().toString()
                          + " "
                          + "المسلمة بتاريخ "
                          + in.getTaxe_attes()
                          + "بناء على شهادة الرسم المهني رقم ")
              .addParam("dariba_ar", "   من طرف المديرية  الجهوية للضرائب بفاس ")
              // .addParam("date_model",in.getTaxe_tribunal()+"  بناء على النمودج رقم
              // "+in.getNumModel()+" ذو السجل التجاري رقم")
              // .addParam("dariba_model","الصادر بتاريخ"+
              // in.getDate_delivration_tribunal()==null?"-":in.getDate_delivration_tribunal().toString()+"من قبل المحكمة التجارية في فاس .")
              .addParam("if", r.getIdentifiantFiscale())
              .addParam("dateformat", r.getIdentifiantFiscale())
              .addParam("datefax", ar.format(dta))
              .addParam("model", in.getNumModel())
              .addParam("registre", in.getTaxe_tribunal())
              .addParam("dateformat", formatter.format(date))
              .addParam("id_ressort", Integer.parseInt(String.valueOf(r.getId())))
              .addParam("mois", mois)
              .addParam("jrs", jrs)
              .addParam("annee", year)
              .generate()
              .getFileLink();
      response.setView(ActionView.define("Attestation").add("html", fileLink).map());
    } else {

      String fileLink =
          ReportFactory.createReport(repport, "Attestation")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .addParam("id", Integer.parseInt(String.valueOf(id)))
              .addParam("nom", r.getNom_fr() + " " + r.getPrenom_fr())
              .addParam("nom_ar", r.getNom_ar() + " " + r.getPrenom_ar())
              .addParam("cin", r.getCin())
              .addParam("adresse", r.getAdresseSociete())
              .addParam("adresse_ar", r.getRueEtNumRue_ste_ar() + "-" + r.getVille_ste_ar())
              .addParam("taxpro", r.getNum_tax())
              .addParam("rc", r.getNum_rc())
              .addParam("societe", r.getRaisonSocial_ste())
              .addParam("societe_ar", r.getRaisonSocial_steAr())
              .addParam("taxeAttes", in.getTaxe_attes())
              .addParam("dateAttes", dateDelivration == null ? "-" : dateDelivration)
              .addParam("list", r.getMetiers())
              .addParam("year", Year.now().getValue())
              .addParam(
                  "date_laivaison",
                  in.getDate_delivration() == null
                      ? "-"
                      : in.getDate_delivration().toString()
                          + " "
                          + "بناء على الشهادة التسجيل في الرسم المهني المسلمة بتاريخ ")
              .addParam("dariba", "   من طرف المديرية  الجهوية للضرائب بفاس ")
              .addParam(
                  "date_laivaison_ar",
                  in.getDate_delivration() == null
                      ? "-"
                      : in.getDate_delivration().toString()
                          + " "
                          + "المسلمة بتاريخ "
                          + in.getTaxe_attes()
                          + "بناء على شهادة الرسم المهني رقم ")
              .addParam("dariba_ar", "   من طرف المديرية  الجهوية للضرائب بفاس ")
              // .addParam("date_model", in.getTaxe_tribunal() + "  بناء على النمودج رقم " +
              // in.getNumModel() + " ذو السجل التجاري رقم")
              // .addParam("dariba_model", "الصادر بتاريخ" + in.getDate_delivration_tribunal() ==
              // null ? "-" : in.getDate_delivration_tribunal().toString() + "من قبل المحكمة
              // التجارية في فاس .")
              .addParam("if", r.getIdentifiantFiscale())
              .addParam("datefa", formatter.format(date))
              .addParam("model", in.getNumModel())
              .addParam("registre", in.getTaxe_tribunal())
              .addParam("datetrb", dateDelivration != null ? dateDelivration : "-")
              .addParam("dateformat", formatter.format(date))
              .addParam("id_ressort", Integer.parseInt(String.valueOf(r.getId())))
              .generate()
              .getFileLink();
      response.setView(ActionView.define("Attestation").add("html", fileLink).map());
    }
  }

  public void loadRessortissant(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf((Integer) request.getContext().get("_id"));
    Ressortissant r = appservice.getRessortissantById(id);
    if (r.getHas_attestation()) {
      InfoAttestation info = appservice.getInfoAttestationByRessortissant(r.getId());
      response.setReadonly("p_taxe", true);
      response.setReadonly("p_model", true);
      response.setReadonly("typeDelivrance", true);

      response.setValue("id", info.getId());
      response.setValue("ressortissant", info.getRessortissant());
      response.setValue("typeDelivrance", info.getTypeDelivrance());
      response.setValue("lang_de_type", info.getLang_de_type());
      response.setValue("taxe_attes", info.getTaxe_attes());
      response.setValue("date_delivration", info.getDate_delivration());
      response.setValue("adresseImpot", info.getAdresseImpot());
      response.setValue("numModel", info.getNumModel());
      response.setValue("taxe_tribunal", info.getTaxe_tribunal());
      response.setValue("date_delivration_tribunal", info.getDate_delivration_tribunal());
    }
    if (r.getHas_attestation()) {
      response.setHidden("creer_attestation", true);
      response.setHidden("imprimer_attestation", false);
    } else {
      response.setHidden("imprimer_attestation", true);
      response.setHidden("creer_attestation", false);
    }

    response.setValue("ressortissant", r);
  }

  public void loadRessortissantorigin(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf((Integer) request.getContext().get("_id"));
    Ressortissant r = appservice.getRessortissantById(id);
    if (r.getHas_origin()) {
      InfoOrigin infoO = appservice.getInfoOriginByRessortissant(r.getId());
      response.setReadonly("form_ressortissent_downloadayoub", true);
      response.setValue("id", infoO.getId());
      response.setValue("ressortissant", infoO.getRessortissant());
      response.setValue("dateFacture", infoO.getDateFacture());
      response.setValue("numero", infoO.getNumero());
      response.setValue("lang_Type", infoO.getLang_de_type());
      response.setReadonly("dateFacture", true);
      response.setReadonly("numero", true);
    }
    if (r.getHas_origin()) {
      response.setHidden("creer_origin", true);
      response.setHidden("imprimer_origin", false);
    } else {
      response.setHidden("imprimer_origin", true);
      response.setHidden("creer_origin", false);
    }

    response.setValue("ressortissant", r);
  }

  public void show_button_imprimerEtatSalaire(ActionRequest request, ActionResponse response) {
    response.setView(
        ActionView.define("type de document")
            .model("com.axelor.apps.hr.db.EtatSalaireTransaction")
            .add("form", "typefileEtatSaire")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .param("popup", "true")
            .context("annee", (Integer) request.getContext().get("annee"))
            .context("mois", (Integer) request.getContext().get("mois"))
            .context("_signal", request.getContext().get("_signal"))
            .map());
  }

  public void loadCheckButtonClicked(ActionRequest request, ActionResponse response) {
    String name = request.getContext().get("_signal").toString();
    name = name.replace("btn_", "btn_imprimer_");
    response.setHidden(name, false);
    if (!name.equals("btn_imprimer_1")) {
      response.setHidden("saveTransaction", true);
    } else {
      response.setHidden("saveTransaction", false);
    }
  }

  public void verifierorig(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf((Integer) request.getContext().get("_id"));
    Ressortissant r = appservice.getRessortissantById(id);
    if (r.getHas_origin()) {
      // imprimer
    } else {
      // affiche form
    }
  }
}
