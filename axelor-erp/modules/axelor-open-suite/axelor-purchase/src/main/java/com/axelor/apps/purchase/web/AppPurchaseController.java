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

package com.axelor.apps.purchase.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.repo.RubriqueBudgetaireGeneraleRepository;
import com.axelor.apps.configuration.db.repo.RubriquesBudgetaireRepository;
import com.axelor.apps.configuration.db.repo.VersionRubriqueBudgetaireRepository;
import com.axelor.apps.configuration.service.ConvertNomreToLettres;
import com.axelor.apps.purchase.db.*;
import com.axelor.apps.purchase.db.repo.*;
import com.axelor.apps.purchase.report.IReport;
import com.axelor.apps.purchase.service.app.AppPurchaseService;
import com.axelor.apps.purchase.service.app.AppPurchaseServiceImpl;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class AppPurchaseController {
  @Inject VersionRubriqueBudgetaireRepository versionRubriqueBudgetaireRepository;
  @Inject RubriquesBudgetaireRepository rubriquesBudgetaireRepository;
  @Inject AppPurchaseService appPurchaseService;

  public void generatePurchaseConfigurations(ActionRequest request, ActionResponse response) {
    Beans.get(AppPurchaseService.class).generatePurchaseConfigurations();
    response.setReload(true);
  }

  public void remplirConsultation(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("_id").toString());
    String signale = request.getContext().get("_signal").toString();
    Long id_fourn =
        Long.valueOf(request.getContext().get("id_fourn" + signale.replace("btn", "")).toString());
    response.setView(
        ActionView.define("Saisir le prix")
            .model(ComparaisonPrix.class.getName())
            .add("grid", "remplirGrid")
            .add("form", "remplirForm")
            .domain(
                "self.consultationPrix.id = " + id + " and self.fournisseur.id = " + id_fourn + "")
            .map());
    response.setValue("id_fournisseur", id_fourn);
  }

  public void remplir_grid_societe_statut1(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("id").toString());
    OffreAppel offre = appPurchaseService.getOffreById(id);
    List<Long> ls = new ArrayList<Long>();
    String x = "";
    /*for (int i = 0; i < offre.getSociete().size(); i++) {
        Sociecte s = offre.getSociete().get(i);
        x += s.getId();
        if (offre.getSociete().size() - 1 >= i) x += ",";
    }*/
    x += 0;
    response.setView(
        ActionView.define("Liste des societes non accepter")
            .model(Sociecte.class.getName())
            .add("grid", "societeNonaccepter_grid2")
            .add("form", "societeNonaccepter_form2")
            .param("forceEdit", "true")
            .param("popup", "true")
            .param("popup-save", "false")
            .param("show-confirm", "false")
            .param("show-toolbar", "false")
            .param("height", "400")
            .domain("(self.statut1=2 OR self.statut2=2) AND self.id IN(" + x + ")")
            .map());
  }

  public void remplir_grid_societe_statut2(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("id").toString());
    OffreAppel offre = appPurchaseService.getOffreById(id);
    List<Long> ls = new ArrayList<Long>();
    String x = "";
    /*for (int i = 0; i < offre.getSociete().size(); i++) {
        Sociecte s = offre.getSociete().get(i);
        x += s.getId();
        if (offre.getSociete().size() - 1 >= i) x += ",";
    }*/
    x += 0;
    response.setView(
        ActionView.define("Liste des societes non accepter")
            .model(Sociecte.class.getName())
            .add("grid", "societeNonaccepter_grid")
            .add("form", "societeNonaccepter_form")
            .param("forceEdit", "true")
            .param("popup", "true")
            .param("popup-save", "false")
            .param("show-confirm", "false")
            .param("show-toolbar", "false")
            .param("height", "400")
            .domain(
                "(self.statut2=2 OR self.statut1=2 OR self.statut3=2) AND self.id IN(" + x + ")")
            .map());
  }

  public void remplir_grid_societe_statut3(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("id").toString());
    OffreAppel offre = appPurchaseService.getOffreById(id);
    List<Long> ls = new ArrayList<Long>();
    String x = "";
    /*for (int i = 0; i < offre.getSociete().size(); i++) {
        Sociecte s = offre.getSociete().get(i);
        x += s.getId();
        if (offre.getSociete().size() - 1 >= i) x += ",";
    }*/
    x += 0;
    response.setView(
        ActionView.define("Liste des societes non accepter")
            .model(Sociecte.class.getName())
            .add("grid", "societeNonaccepter_grid3")
            .add("form", "societeNonaccepter_form3")
            .param("forceEdit", "true")
            .param("popup", "true")
            .param("popup-save", "false")
            .param("show-confirm", "false")
            .param("show-toolbar", "false")
            .param("height", "400")
            .domain(
                "(self.statut2=2 OR self.statut1=2 OR self.statut3=2) AND self.id IN(" + x + ")")
            .map());
  }

  /* public void test_consultationPrix(ActionRequest request, ActionResponse response){
  ConsultationPrix consultationPrix = request.getContext().asType(ConsultationPrix.class);
  Long id = consultationPrix.getId();
  */
  /*Long id = Long.valueOf(request.getContext().get("id").toString());
  ConsultationPrix consultationPrix = appPurchaseService.getConsultationPrixbyId(id);*/
  /*
    if(!consultationPrix.getHasCommande()){
      //edit comparaisonPrix
      appPurchaseService.saveConsultationPrix(request);
      addNew_comparaisonPrix(request,response);
      response.setReload(true);
    }else{
      //stop edit comparaisonPrix
      response.setFlash("Enregistrement impossible la consultation Prix dispose d'une commande d'achat");
    }
  }*/
  public void addNew_comparaisonPrix(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("id").toString());
    appPurchaseService.updateComparaisonPrix(id);
    appPurchaseService.inject_artilrToComparaison(id);
  }

  public void checkData_article_fournisseur(ActionRequest request, ActionResponse response) {
    // response.setPending("add_comparaisonPrix");
    response.setError("les champs sont obligatoire");
  }

  public void updateTotole(ActionRequest request, ActionResponse response) {
    Long id_compPrix = Long.valueOf(request.getContext().get("id").toString());
    Integer tva = Integer.valueOf(request.getContext().get("tva").toString());
    String sum = appPurchaseService.updateTotal(id_compPrix, tva);
    response.setValue("total", sum);
    response.setReload(true);
  }

  public void definir_commande(ActionRequest request, ActionResponse response) {
    Long id_compPrix = Long.valueOf(request.getContext().get("id").toString());
    ComparaisonPrix c = appPurchaseService.getComparaisonPrixById(id_compPrix);
    Compte compte = c.getConsultationPrix().getCompte();
    RubriqueBudgetaireGenerale rubrique = c.getConsultationPrix().getRubriqueBudgetaire();
    BigDecimal mt_budget =
        appPurchaseService.getBudgetByrubriqueBudgetaire(rubrique, LocalDate.now().getYear());
    if (c.getTotal().compareTo(new BigDecimal(0)) == 0) {
      response.setFlash("Le montant des produits n'est pas saisie");
      return;
    }
    List<ComparaisonPrix> list = appPurchaseService.getListActiveComparaisonPrix(id_compPrix);
    Long id_commandeAchat = 0l;
    if (c.getCommandeAchat() != null) {
      id_commandeAchat = c.getCommandeAchat().getId();
    } else {
      String mesg = "La commande à été attribuer au fournisseur : ";
      for (ComparaisonPrix tmp_msg : list) {
        if (tmp_msg.getCommandeAchat() != null) {
          mesg = mesg + tmp_msg.getFournisseur().getName();
        }
      }
      response.setFlash(mesg);
      return;
    }
    response.setView(
        ActionView.define("Commande d'achat")
            .add("form", "commandeAchat_form")
            .add("grid", "commandeAchat_grid")
            .model("com.axelor.apps.purchase.db.CommandeAchat")
            .context("_showRecord", id_commandeAchat)
            .param("forceEdit", "true")
            .map());
    response.setReload(true);
  }

  public void getOffreAppel(ActionRequest request, ActionResponse response) {
    Long idOffre = Long.valueOf(request.getContext().get("offre").toString());
    OffreAppel c = appPurchaseService.getOffreById(idOffre);
    response.setValue("numero", c.getNumero());
    response.setValue("nomOffre", c.getNom());
    response.setValue("dateLancement", c.getDateLancement());
    response.setValue("dateOverture", c.getDateOverture());
    response.setValue("comite", c.getComite());
    response.setValue("piecesJointes", c.getPiecesJointes());
  }

  public void imprimer_coonsultaion(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id_comparaison = Long.valueOf(request.getContext().get("valId").toString());
    String format = "pdf";
    if (request.getContext().get("typefichier") != null) {
      format = ((boolean) request.getContext().get("typefichier")) ? format : "doc";
    }
    ComparaisonPrix comparaisonPrix = appPurchaseService.getComparaisonPrixById(id_comparaison);
    Long idComparaisonPrix = comparaisonPrix.getId();
    String objetLettre = comparaisonPrix.getConsultationPrix().getRubriqueBudgetaire().getName();
    LocalDate date1 = comparaisonPrix.getConsultationPrix().getDateFermeture();
    ZoneId defaultZoneId = ZoneId.systemDefault();
    Date dateFermeture = Date.from(date1.atStartOfDay(defaultZoneId).toInstant());
    String fileLink =
        ReportFactory.createReport(IReport.LettreDeConsultation, "lettreConsultation")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("dateFermeture", dateFermeture)
            .addParam("idComparaisonPrix", idComparaisonPrix)
            .addParam("objetLettre", objetLettre)
            .addFormat(format)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("lettre de consultation").add("html", fileLink).map());
  }

  public void imprimerBonCommande(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id_commande = Long.valueOf(request.getContext().get("valId").toString());
    String format = "pdf";
    if (request.getContext().get("typefichier") != null) {
      format = ((boolean) request.getContext().get("typefichier")) ? format : "doc";
    }
    CommandeAchat commandeAchat = appPurchaseService.getCommandeAchatById(id_commande);
    ComparaisonPrix comparaisonPrix = commandeAchat.getComparaisonPrix();
    String montant_string = ConvertNomreToLettres.getStringMontant(comparaisonPrix.getTotal());
    String fileLink =
        ReportFactory.createReport(IReport.BonDeCommande2, "BonDeCommande")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam(
                "name_rub",
                commandeAchat
                    .getComparaisonPrix()
                    .getConsultationPrix()
                    .getRubriqueBudgetaire()
                    .getName())
            .addParam("name_fourn", commandeAchat.getComparaisonPrix().getFournisseur().getName())
            .addParam(
                "code_budg",
                commandeAchat
                    .getComparaisonPrix()
                    .getConsultationPrix()
                    .getRubriqueBudgetaire()
                    .getCodeBudg())
            .addParam("adresse", commandeAchat.getComparaisonPrix().getFournisseur().getAdresse())
            .addParam("numCom", commandeAchat.getNumero())
            .addParam("idCommande", id_commande)
            .addParam("montant_string", montant_string)
            .addFormat(format)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Bon de commande").add("html", fileLink).map());
  }

  public void imprimer_printNotePresentation(ActionRequest request, ActionResponse response)
      throws AxelorException {
    if (request.getContext().get("valId") == null) {
      response.setFlash("la commande n'est pas valide");
      return;
    }
    Long id_commande = Long.valueOf(request.getContext().get("valId").toString());
    String format = "pdf";
    if (request.getContext().get("typefichier") != null) {
      format = ((boolean) request.getContext().get("typefichier")) ? format : "doc";
    }
    CommandeAchat commandeAchat = appPurchaseService.getCommandeAchatById(id_commande);
    Long id_consultation = commandeAchat.getComparaisonPrix().getId();
    int numbreFourn =
        commandeAchat.getComparaisonPrix().getConsultationPrix().getListFournisseur().size();
    String mode_attribution = "";
    if (numbreFourn > 1) {
      mode_attribution =
          "LETTRE CIRCULAIRE ADRESSEE A "
              + ConvertNomreToLettres.convert(numbreFourn).toUpperCase()
              + " CONCURRENTS A SAVOIR.";
    } else if (numbreFourn == 1) {
      mode_attribution =
          "LETTRE CIRCULAIRE ADRESSEE A "
              + ConvertNomreToLettres.convert(numbreFourn).toUpperCase()
              + " CONCURRENT A SAVOIR.";
    } else {
      response.setFlash("Aucun Fournisseur");
      return;
    }
    String montant_string =
        "Ste : "
            + commandeAchat.getComparaisonPrix().getFournisseur().getName()
            + " POUR UN MONTANT DE "
            + commandeAchat.getComparaisonPrix().getTotal()
            + " DH (";
    montant_string =
        montant_string
            + ConvertNomreToLettres.getStringMontant(commandeAchat.getComparaisonPrix().getTotal())
            + " )";
    String fileLink =
        ReportFactory.createReport(IReport.NoteDePresentation, "NoteDePresentation")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id_consultation", id_consultation)
            .addParam("id_commande", commandeAchat.getId())
            .addParam("mode_attribution", mode_attribution)
            .addParam("montant_string", montant_string)
            .addFormat(format)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Notice de présentation").add("html", fileLink).map());
  }

  public void genereNumeroDemandeAchat(ActionRequest request, ActionResponse response) {
    Calendar c = Calendar.getInstance();
    int year = c.get(Calendar.YEAR);
    int nbr = appPurchaseService.getNombreDemandeAchat(year);
    nbr = nbr + 1;
    String numeros = appPurchaseService.getNumeroConsultation();
    response.setValue("numero", numeros);
    response.setValue("nbr", nbr);
    response.setValue("year", year);
  }

  public void afficheDetailDemandeAchat(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("id").toString());
    response.setView(
        ActionView.define("Detail")
            .model(ArticleDetails.class.getName())
            .add("grid", "detail-demande-achat")
            .add("form", "form-demande-achat")
            .domain("self.demandeAchat.etat='Ouvert' and self.demandeAchat.id=" + id)
            .map());
  }

  public void affiche_form(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("id").toString());
    response.setView(
        ActionView.define("Detail")
            .model(ArticleDetails.class.getName())
            .add("form", "stock_non_ok")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup", "reload")
            .param("popup-save", "false")
            .context("id", request.getContext().get("id"))
            .map());
    response.setValue("valueText", id);
    response.setReload(true);
  }

  public void verifier_stock(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("id").toString());
    ArticleDetails dmd = appPurchaseService.getArticleDetailById(id);
    int reponse = appPurchaseService.checkstockByDemandeAchat(dmd);
    response.setValue("valueText", reponse);
    response.setValue("idval", id);
    response.setSignal("refresh-tab", true);
  }

  public void retrait_stock(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("idval").toString());
    Long type = Long.valueOf(request.getContext().get("valueText").toString());
    appPurchaseService.retraitStock(id);
    response.setFlash("Retait du stock est effectuer avec succès");
    response.setPending("close");
  }

  public void receptionCommande(ActionRequest request, ActionResponse response) {
    Long idcommande = Long.valueOf(request.getContext().get("id").toString());
    appPurchaseService.recevoirCommande(idcommande);
    response.setReload(true);
  }

  public void geneateNumeroConsultation(ActionRequest request, ActionResponse response) {
    String numeros = appPurchaseService.getNumeroConsultation();
    response.setValue("numero", numeros);
    response.setValue("year", LocalDate.now().getYear());
  }

  public void geneateNumeroOffre(ActionRequest request, ActionResponse response) {
    String numero = appPurchaseService.getNumOffre();
    response.setValue("numero", numero);
  }

  public void getSocieteAccept(ActionRequest request, ActionResponse response) {
    List<Sociecte> ls = (List<Sociecte>) request.getContext().get("societe");
    List<Sociecte> lt = new ArrayList<>();
    for (Sociecte s : ls) {
      if (s.getStatut1() != null) {
        if (s.getStatut1().getId() == 1) {
          lt.add(s);
        }
      }
    }
    Context cntx = request.getContext();
    cntx.put("societe", lt);
    response.setValues(cntx);
    response.setValue("societe", lt);
  }

  public void getSocieteAccept2(ActionRequest request, ActionResponse response) {
    List<Sociecte> ls = (List<Sociecte>) request.getContext().get("societe");
    List<Sociecte> lt = new ArrayList<>();
    for (Sociecte s : ls) {
      if (s.getStatut2() != null) {
        if (s.getStatut2().getId() == 1) {
          lt.add(s);
        }
      }
    }
    Context cntx = request.getContext();
    cntx.put("societe", lt);
    response.setValues(cntx);
    response.setValue("societe", lt);
  }

  public void getSocieteAccept3(ActionRequest request, ActionResponse response) {
    List<Sociecte> ls = (List<Sociecte>) request.getContext().get("societe");
    Boolean moinsdisant = (Boolean) request.getContext().get("moinsdisant");
    List<Sociecte> lt = new ArrayList<>();
    Sociecte st = ls.size() > 0 ? ls.get(0) : null;
    float min = ls.size() > 0 ? ls.get(0).getOffreFinanciere_prix().floatValue() : 0;
    if (moinsdisant) {
      for (Sociecte s : ls) {
        if (s.getStatut3() != null) {
          if (s.getStatut3().getId() == 1 && s.getOffreFinanciere_prix().floatValue() <= min) {
            min = s.getOffreFinanciere_prix().floatValue();
            st = s;
          } else {
            s.setStatut3(appPurchaseService.getStatutById(Long.valueOf(2)));
            appPurchaseService.saveSociete(s);
          }
        }
      }
      lt.add(st);
    } else {
      for (Sociecte s : ls) {
        if (s.getStatut3() != null) {
          if (s.getStatut3().getId() == 1) {
            lt.add(s);
          }
        }
      }
    }
    Context cntx = request.getContext();
    cntx.put("societe", lt);
    response.setValues(cntx);
    response.setValue("societe", lt);
  }

  public void test_test(ActionRequest request, ActionResponse response) {
    Fournisseur fournis = (Fournisseur) request.getContext().get("fournisseurSelect");
    Long id = fournis.getId();
    Fournisseur f = appPurchaseService.getFournisseurById(id);
    response.setValue("tele", f.getTele());
    response.setValue("email", f.getEmail());
    response.setValue("ville", f.getVille());
    response.setValue("adresse", f.getAdresse());
  }

  public void updateSelectFournisseur(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("id").toString());
    appPurchaseService.upDateSelectFourn(id);
  }

  public void removeConsultation(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("id").toString());
    ConsultationPrix consultationPrix = appPurchaseService.getConsultationPrixbyId(id);
    appPurchaseService.deleteSelectFournisseurByConsultationId(consultationPrix.getId());
    appPurchaseService.deleteArticleDetailConsultationId(consultationPrix.getId());
    appPurchaseService.deletecomparaisonPrixByConsultationPrixId(consultationPrix.getId());
    appPurchaseService.deleteConsultationPrixById(consultationPrix.getId());
    response.setReload(true);
    /* response.setSignal("tab-refresh",true);*/
  }

  public void removeArticleDetails(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("id").toString());
    Context _parent = request.getContext().getParent();
    boolean hasCommande = (boolean) _parent.get("hasCommande");
    if (hasCommande == false) {
      ArticleDetails articleDetails = appPurchaseService.getArticleDetailById(id);
      if (articleDetails == null) {
        response.setFlash("Enregistrement Non disponible");
      } else {
        appPurchaseService.deleteArticleDetailConsultationId(articleDetails.getId());
      }
      response.setReload(true);
    } else {
      response.setFlash("La consultation Prix dispose d'une commande");
      response.setReload(true);
    }
  }

  public void refresh_vehicule(ActionRequest request, ActionResponse response) {
    Vehicules vehicule = (Vehicules) request.getContext().get("vehicules");
    Long id = vehicule.getId();
    Vehicules v = appPurchaseService.getVehiculesById(id);
    response.setValue("matricule", v.getMatricule());
    response.setValue("modele", v.getModele());
  }

  public void imprimer_vehicule_entretien(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String annee = (String) request.getContext().get("annee");
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.purchase.report.IReport.VehiculeEntretiens, "GestionRecette")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("Plannign annuel de maintenance").add("html", fileLink).map());
  }

  public void saveCommandeAchat(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(0);
    if (request.getContext().get("id") != null) {
      id = Long.valueOf(request.getContext().get("id").toString());
    } else if (request.getContext().get("_id") != null) {
      id = Long.valueOf(request.getContext().get("_id").toString());
    } else {
      response.setFlash("ComparaisonPrix not valide");
      return;
    }
    ComparaisonPrix com = appPurchaseService.getComparaisonPrixById(id);
    BigDecimal zero = new BigDecimal(0);
    if (com.getTotal().compareTo(zero) < 0) {
      response.setFlash("Les articles ne dispose pas de prix");
      return;
    }
    CommandeAchat commandeAchat = appPurchaseService.creatCommandeAchat(request, response);
    response.setView(
        ActionView.define("Commande d'achat")
            .add("form", "commandeAchat_form")
            .add("grid", "commandeAchat_grid")
            .model("com.axelor.apps.purchase.db.CommandeAchat")
            .context("_showRecord", commandeAchat.getId())
            .param("forceEdit", "true")
            .map());
  }

  public void validationFields(ActionRequest request, ActionResponse response) {
    Context context = request.getContext();
    String li = "";
    if (context.get("objet") == null) li += "<li>Object</li>";
    if (context.get("conserne_desc") == null) li += "<li>Description</li>";
    if (context.get("motif") == null) li += "<li>Motif</li>";
    if (context.get("justificatif") == null) li += "<li>Justification</li>";
    String message = "<p>Les champs suivant sont obligatoire</p><ul>" + li + "</ul>";
    response.setFlash(message);
  }

  public void imprimerDemandeEntretien(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id = (Long) request.getContext().get("id");
    String fileLink =
        ReportFactory.createReport(IReport.DemandeEntretiens, "DemandeEntretiens")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id", id)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Demande d'entretien").add("html", fileLink).map());
  }

  public void ImprimerBonreception(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id_commande = Long.valueOf((Integer) request.getContext().get("_id"));
    String format = "pdf";
    if (request.getContext().get("typefichier") != null) {
      format = (request.getContext().get("typefichier").toString().equals("pdf")) ? format : "doc";
    }

    String BLN = "0";
    if (request.getContext().get("num_BLN") != null) {
      BLN = request.getContext().get("num_BLN").toString();
    }
    CommandeAchat commandeAchat = appPurchaseService.getCommandeAchatById(id_commande);
    if (commandeAchat.getNumLivraison() == null || commandeAchat.getNumLivraison().equals("")) {
      ReceptionCommande r = appPurchaseService.addReceptionCommande(id_commande, BLN);
      commandeAchat.setNumLivraison(r.getBonLivraison());
      appPurchaseService.saveCommandeAchat(commandeAchat);
    }
    ComparaisonPrix comparaisonPrix = commandeAchat.getComparaisonPrix();
    String montant_string = ConvertNomreToLettres.getStringMontant(comparaisonPrix.getTotal());

    String fileLink =
        ReportFactory.createReport(IReport.BonReception, "BonDeRecepetion")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id_commande", id_commande)
            .addParam("BLN", BLN)
            .addParam("montant_string", montant_string)
            .addFormat(format)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Bon de reception").add("html", fileLink).map());
  }

  public void imprimerPVreception(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id_commande = Long.valueOf((Integer) request.getContext().get("valId"));
    CommandeAchat commandeAchat = appPurchaseService.getCommandeAchatById(id_commande);
    String format = "pdf";
    if (request.getContext().get("typefichier") != null) {
      format = ((boolean) request.getContext().get("typefichier")) ? format : "doc";
    }
    ComparaisonPrix comparaisonPrix = commandeAchat.getComparaisonPrix();
    String montant_string = ConvertNomreToLettres.getStringMontant(comparaisonPrix.getTotal());

    String NumeroComande = commandeAchat.getNumero();
    String objetCommande = commandeAchat.getObjet();
    String nomFournisseur = commandeAchat.getComparaisonPrix().getFournisseur().getName();
    String fileLink =
        ReportFactory.createReport(IReport.PvReception, "PvReception")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id_commande", id_commande)
            .addParam("montant_string", montant_string)
            .addFormat(format)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("PV de reception").add("html", fileLink).map());
  }

  public void imprimerPVreceptionMarche(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id_ordreService = (Long) request.getContext().get("id");
    String fileLink =
        ReportFactory.createReport(IReport.PvReception_old, "PV de réception")
            .addParam("id", id_ordreService)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("PV de réception").add("html", fileLink).map());
  }

  public void imprimerOrdrePayement(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id_commande = Long.valueOf((Integer) request.getContext().get("valId"));
    CommandeAchat commandeAchat = appPurchaseService.getCommandeAchatById(id_commande);
    String format = "pdf";
    if (request.getContext().get("typefichier") != null) {
      format = ((boolean) request.getContext().get("typefichier")) ? format : "doc";
    }
    if (!commandeAchat.getHasPaymentCommande()) {
      OrderPaymentCommande ordre = appPurchaseService.creatOrdrePayementCommande(request, response);
    }
    OrderPaymentCommande ordrePayementt = appPurchaseService.getOrdreByCommandeId(id_commande);
    String nomFournisseur = commandeAchat.getComparaisonPrix().getFournisseur().getName();
    String adresseFournisseur = commandeAchat.getComparaisonPrix().getFournisseur().getAdresse();
    String ribFournisseur = commandeAchat.getComparaisonPrix().getFournisseur().getRib();
    String rubrique =
        commandeAchat.getComparaisonPrix().getConsultationPrix().getRubriqueBudgetaire().getName();
    String numeroOrdre = ordrePayementt.getNumero() == null ? "-1" : ordrePayementt.getNumero();
    int anneeOrdree = LocalDate.now().getYear();
    String anneeOrdre = String.valueOf(anneeOrdree);
    String codeBudg =
        commandeAchat
            .getComparaisonPrix()
            .getConsultationPrix()
            .getRubriqueBudgetaire()
            .getCodeBudg();
    String objetCommande = commandeAchat.getObjet();
    BigDecimal montantOrder = commandeAchat.getComparaisonPrix().getTotal();
    String montantOrder_string = ConvertNomreToLettres.getStringMontant(montantOrder);
    String fileLink =
        ReportFactory.createReport(IReport.OrdrePayement, "OrdrePaiement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("nomFournisseur", nomFournisseur)
            .addParam("adresseFournisseur", adresseFournisseur)
            .addParam("ribFournisseur", ribFournisseur)
            .addParam("rubrique", rubrique)
            .addParam("codeBudg", codeBudg)
            .addParam("objetCommande", objetCommande)
            .addParam("montantOrder", montantOrder)
            .addParam("montantOrderString", montantOrder_string)
            .addParam("numeroOrdre", numeroOrdre)
            .addParam("anneeOrdre", anneeOrdre)
            .addFormat(format)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de paiement").add("html", fileLink).map());
  }

  public void load_selectOP(ActionRequest request, ActionResponse response) {
    if (request.getContext().get("orderPaymentCommande") == null) return;
    OrderPaymentCommande _ordre =
        (OrderPaymentCommande) request.getContext().get("orderPaymentCommande");
    OrderPaymentCommande ordre = appPurchaseService.getOrdrePayementbyId(_ordre.getId());
    response.setValue("numero", ordre.getNumero());
    response.setValue("benificiaire", ordre.getBeneficiaire());
    response.setValue("rubriqueBudgetaire", ordre.getRubriqueBudgetaire().getName());

    response.setValue("somme", ordre.getSommeVerement());

    if (!ordre.getIs_rh_module()) response.setValue("compte", ordre.getCompteBenef());
    else response.setValue("rib", ordre.getRib());
  }

  public void updateCompteOv(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    OrdrevirementCommande ov = appPurchaseService.getordreVirementById(id);
    Compte c = ov.getCompte();
    HistoriqueCompte h = appPurchaseService.addHistoriqueCompte(c, ov);
    ov.setHistorique(h);

    appPurchaseService.saveOrdreVirement(ov);
  }

  public void imprimerOV(ActionRequest request, ActionResponse response) throws AxelorException {
    Long id_ov = (Long) request.getContext().get("id");
    // String somme_string = appPurchaseService.getsommeTotaleString(id_ov);
    String fileLink =
        ReportFactory.createReport(IReport.OrdreVersement_v2, "OrdreVersement")
            .addParam("id_virement", id_ov)
            // .addParam("montant_string", somme_string)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de versement").add("html", fileLink).map());
  }

  public void getNumerosOV(ActionRequest request, ActionResponse response) {
    if (request.getContext().get("numero") == null)
      response.setValue(
          "numero",
          String.format(
                  "%02d",
                  Beans.get(OrdrevirementCommandeRepository.class)
                          .all()
                          .filter("DATE_PART('year', self.datevirement ) = :year")
                          .bind("year", LocalDate.now().getYear())
                          .count()
                      + 1)
              + "/"
              + LocalDate.now().format(DateTimeFormatter.ofPattern("yy")));
  }

  public void update_historiquecompte(ActionRequest request, ActionResponse response) {
    Map<String, Object> _maps = request.getData();
    Map<String, Object> maps = (Map<String, Object>) _maps.get("context");
    String designation = (String) maps.get("designation"); // NULL
    String message = "<p>Les champs suivants sont obligatoire:</p><ul>";
    boolean msg = false;
    if (designation == null) {
      message += "<li>Désignation</li>";
      if (!msg) msg = true;
    }
    String rib = (String) maps.get("rib"); // null
    if (rib == null) {
      message += "<li>Rib</li>";
      if (!msg) msg = true;
    }
    String t = String.valueOf(maps.get("montant"));
    BigDecimal montant = new BigDecimal(t);
    String description = (String) maps.get("description"); // null
    if (description == null) {
      message += "<li>Déscription</li>";
      if (!msg) msg = true;
    }
    Integer action_id = (Integer) request.getContext().get("action"); // 0
    if (action_id == 0) {
      message += "<li>Action</li>";
      if (!msg) msg = true;
    }
    AnnexeGenerale annexeGenerale = (AnnexeGenerale) request.getContext().get("annexe"); // null
    if (annexeGenerale == null) {
      message += "<li>Annexe</li>";
      if (!msg) msg = true;
    }
    if (msg) {
      message += "</ul>";
      response.setFlash(message);
      return;
    }
    Long id = annexeGenerale.getId();
    AnnexeGenerale annexe = appPurchaseService.getAnnexeById(id);
    Long id_compte = (Long) request.getContext().get("id");
    Beans.get(AppPurchaseServiceImpl.class)
        .update_historiquecompte(
            designation, rib, montant, description, action_id, annexe, id_compte);
  }

  public void get_id_offre(ActionRequest request, ActionResponse response) {
    HashMap<String, Object> _parent = (HashMap<String, Object>) request.getContext().get("_parent");
    response.setValue("offre", _parent.get("id"));
  }

  public void checkMontantByListSelectOP(ActionRequest request, ActionResponse response) {
    HashMap<String, Object> _parent = (HashMap<String, Object>) request.getContext().get("_parent");
    List<Object> test = (List<Object>) request.getContext().get("selectOP");
    if (request.getContext().get("compte") == null || test == null || test.size() == 0) {
      response.setHidden("save1", true);
      response.setHidden("save2", true);
      return;
    }
    List<SelectOP> ls = (List<SelectOP>) request.getContext().get("selectOP");
    BigDecimal sum = new BigDecimal(0);
    boolean tout_Nok = false;
    for (SelectOP tmp : ls) {
      BigDecimal x = new BigDecimal(tmp.getSomme());
      BigDecimal montant_rub =
          appPurchaseService.getBudgetByrubriqueBudgetaire(
              tmp.getOrderPaymentCommande().getRubriqueBudgetaire(),
              tmp.getOrderPaymentCommande().getRubriqueBudgetaire().getAnnee());
      if ((montant_rub.compareTo(BigDecimal.valueOf(0)) == 0 || x.compareTo(montant_rub) > 0)
          && tout_Nok == false) {
        tout_Nok = true;
      }
      sum = sum.add(x);
    }
    Compte c = (Compte) request.getContext().get("compte");
    c = appPurchaseService.getCompteById(c.getId());
    if (sum.compareTo(c.getMontant()) > 0 || tout_Nok) {
      response.setFlash(
          tout_Nok
              ? "Le Montant dans la Rubrique n'est pas suffisant"
              : "Le Montant dans le compte n'est pas suffisant");
      response.setHidden("save1", true);
      response.setHidden("save2", true);
      return;
    } else {
      if (request.getContext().get("id") != null) {
        response.setHidden("save2", false);
        response.setHidden("save1", true);
      } else {
        response.setHidden("save1", false);
        response.setHidden("save2", true);
      }
    }
  }

  public void update_list_article(ActionRequest request, ActionResponse response) {
    Map<String, Object> parent = request.getContext().getParent();
    CommandeAchat commande = (CommandeAchat) parent.get("commandeAchat");
    List<ArticlesPrix> ls = appPurchaseService.getArticleRestant(commande.getId());
    List<Long> ls_exclu = new ArrayList<>();
    if (parent.get("articleRecuDetail") != null) {
      List<ArticlesRecu> obj = (List<ArticlesRecu>) parent.get("articleRecuDetail");
      for (ArticlesRecu tmp : obj) {
        ls_exclu.add(tmp.getArticleRecuDetail().getArticleprix().getId());
      }
    }
    List<Long> ids = new ArrayList<>();
    for (ArticlesPrix tmp : ls) {
      if (!ls_exclu.contains(tmp.getArticleprix().getId())) {
        ids.add(tmp.getId());
      }
    }
    Context ctx = request.getContext();
    ctx.put("Listid", ids);
    ctx.put("id_commande", commande.getId());
    response.setValues(ctx);
  }

  public void load_quantite_reliquat(ActionRequest request, ActionResponse response) {
    ArticlesRecu p = request.getContext().asType(ArticlesRecu.class);
    Long id_commande = Long.valueOf(request.getContext().get("id_commande").toString());
    BigDecimal reliquat = appPurchaseService.getQuantite_reliquat(p.getArticleRecuDetail().getId());
    response.setValue("quantiteRecu", 0);
    response.setValue("reliquat", reliquat);
    response.setReadonly("quantiteRecu", false);
    response.setValue(
        "nameProduct", p.getArticleRecuDetail().getArticleprix().getArticle().getNom_fr());
  }

  public void verifiermax(ActionRequest request, ActionResponse response) {
    Integer nbr = (Integer) request.getContext().get("quantiteRecu");
    if (request.getContext().get("articleRecuDetail") == null) {
      response.setFlash("Merci de choisir un Article");
      return;
    }
    ArticlesPrix p = (ArticlesPrix) request.getContext().get("articleRecuDetail");
    BigDecimal reliquat = appPurchaseService.getQuantite_reliquat(p.getId());
    if (nbr > reliquat.intValue()) {
      response.setValue("quantiteRecu", reliquat);
      response.setFlash("La quantitée maximal est dépasée");
      response.setValue("reliquat", 0);
    } else {
      response.setValue("reliquat", reliquat.intValue() - nbr);
    }
  }

  public void updateStockAchat(ActionRequest request, ActionResponse response) {
    ReceptionCommande r = request.getContext().asType(ReceptionCommande.class);
    appPurchaseService.updateStock(
        r.getArticleRecuDetail(),
        r.getCommandeAchat().getComparaisonPrix().getConsultationPrix().getAnnexe());
    // verifier si tous les article sont recu pour cloturer la commande
    boolean commande_Not_Complete =
        appPurchaseService.commandeHasReliquat(r.getCommandeAchat().getId());
    if (!commande_Not_Complete) {
      appPurchaseService.commandeComplete(r.getCommandeAchat().getId());
    }
    response.setView(
        ActionView.define("Liste des societes non accepter")
            .model(ReceptionCommande.class.getName())
            .add("grid", "reception-commande-achat-grid")
            .add("form", "reception-commande-achat-form")
            .param("forceEdit", "true")
            .map());
  }

  public void deleteReceptionCommande(ActionRequest request, ActionResponse response) {
    Long id_reception = (Long) request.getContext().get("id");
    appPurchaseService.deleteReceptionCommande(id_reception);
    response.setReload(true);
  }

  public void addreceptionToCommandeAchat(ActionRequest request, ActionResponse response) {
    Long id_reception = (Long) request.getContext().get("id");
    appPurchaseService.addReceptionToCommandeAchat(id_reception);
  }

  public void addHistoriqueStockByAddStock(ActionRequest request, ActionResponse response) {
    StockAchat s = request.getContext().asType(StockAchat.class);
    HistoriqueStock h = new HistoriqueStock();
    h.setTypeOperation("Ajouter");
    h.setArticle(s.getArticle());
    h.setAnnexe(s.getAnnexe());
    h.setQuantity(s.getQuantity());
    h.setDateOperation(LocalDate.now());
    appPurchaseService.saveHistoriqueStock(h);
  }

  public void loadIdfile(ActionRequest request, ActionResponse response) {
    Integer id = (Integer) request.getContext().get("_id");
    response.setValue("valId", id);
    response.setValue("typefichier", true);
  }

  public void verifierStockAndLoadForm(ActionRequest request, ActionResponse response) {
    Long idComparaison = (Long) request.getContext().get("id");
    ComparaisonPrix comparaisonPrix = appPurchaseService.getComparaisonPrixById(idComparaison);
    Compte compte = comparaisonPrix.getConsultationPrix().getCompte();
    RubriqueBudgetaireGenerale rubrique =
        comparaisonPrix.getConsultationPrix().getRubriqueBudgetaire();
    BigDecimal mt_budget =
        appPurchaseService.getBudgetByrubriqueBudgetaire(rubrique, rubrique.getAnnee());
    if (comparaisonPrix.getTotal().compareTo(compte.getMontant()) > 0) {
      String msg =
          "Le montant dans le compte bancaire " + compte.getDesignation() + " est insuffisant";
      response.setFlash(msg);
      return;
    }
    if (comparaisonPrix.getTotal().compareTo(mt_budget) > 0) {
      String msg =
          "Le montant de la rubrique budgétaire " + rubrique.getName() + " est insuffisant";
      response.setFlash(msg);
      return;
    }
    response.setView(
        ActionView.define("Liste des societes non accepter")
            .model(CommandeAchat.class.getName())
            .add("form", "commande_form")
            .param("forceEdit", "true")
            .param("popup", "reload")
            .param("popup-save", "false")
            .param("show-confirm", "false")
            .param("show-toolbar", "false")
            .context("_id", idComparaison)
            .map());
  }

  public void imprimerPlaningParAnnee(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer year = Integer.valueOf(request.getContext().get("year").toString());
    String Langue = request.getContext().get("lang_de_article_type").toString();
    String format = "pdf";
    if (request.getContext().get("typefichier") != null)
      format = request.getContext().get("typefichier").toString().equals("pdf") ? format : "doc";
    switch (Langue) {
      case "français":
        {
          String fileLink =
              ReportFactory.createReport(IReport.PlanningAO, "Programme prévisionnel")
                  .addParam("Locale", ReportSettings.getPrintingLocale(null))
                  .addParam("year", year)
                  .addFormat(format)
                  .generate()
                  .getFileLink();
          response.setView(ActionView.define("Programme prévisionnel").add("html", fileLink).map());
          break;
        }
      case "arabe":
        {
          String fileLink =
              ReportFactory.createReport(IReport.articlesarabe, "البرنامج التوقعي للصفقات العمومية")
                  .addParam("Locale", ReportSettings.getPrintingLocale(null))
                  .addParam("year", year)
                  .addFormat(format)
                  .generate()
                  .getFileLink();
          response.setView(
              ActionView.define("البرنامج التوقعي للصفقات العمومية").add("html", fileLink).map());
          break;
        }
    }
  }

  public void verifierSoumissionnaire(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    OffreAppel o = appPurchaseService.getOffreById(id);
    /*if (LocalDate.now().compareTo(o.getDateOverture()) > 0) {
        String msg = "La date d'ouverture du marché " + o.getDateOverture() + " est dépassée";
        response.setFlash(msg);
        return;
    }*/
    response.setView(
        ActionView.define("Gestion des Soumissionnaires")
            .model(Soumissionnaire.class.getName())
            .add("grid", "soumissionnaire-grid")
            .add("form", "soumissionnaire-form")
            .domain("self.offreappels=" + id)
            .param("forceEdit", "true")
            .context("idOffres", id)
            .map());
  }

  public void check_type_soumission(ActionRequest request, ActionResponse response) {
    Long id_offres = Long.valueOf((Integer) request.getContext().get("idOffres"));
    OffreAppel o = appPurchaseService.getOffreById(id_offres);
    if (o != null && o.getTypeDossier() != null && !o.getTypeDossier().isEmpty()) {
      String[] t = o.getTypeDossier().split(",");
      for (String tmp : t) {
        if (tmp.equals("éléctronique")) response.setHidden("soumissionElectro", false);
        if (tmp.equals("papier")) response.setHidden("soumissionPapier", false);
      }
    }
    if (o.getEchantillionnage()) {
      response.setHidden("dateEchantillon", false);
      response.setHidden("heurEchantillon", false);
    } else {
      response.setHidden("dateEchantillon", true);
      response.setHidden("heurEchantillon", true);
    }
    if (o.getDossierAddetive()) {
      response.setHidden("dossierAdetive", false);
    } else {
      response.setHidden("dossierAdetive", true);
    }
  }

  public void valide_doc_administratite(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    OffreAppel o = appPurchaseService.getOffreById(id);
    response.setView(
        ActionView.define("Gestion des Soumissionnaires")
            .model(Soumissionnaire.class.getName())
            .add("grid", "soumissionnaire_valide-grid")
            .add("form", "soumissionnaire_valide-form")
            .domain("self.offreappels=" + id /*+ " and self.openEnveloppe='true'"*/)
            .param("forceEdit", "true")
            .context("idOffres", id)
            .map());
  }

  public void attacherOffreToSoumissionnaire(ActionRequest request, ActionResponse response) {
    Long id_soum = (Long) request.getContext().get("id");
    Soumissionnaire s = appPurchaseService.getSoumissionnaireById(id_soum);
    Long id = Long.valueOf((Integer) request.getContext().get("idOffres"));
    s.setOffreappels(appPurchaseService.getOffreById(id));
    appPurchaseService.saveOffreAppel(s);
  }

  public void get_Soumissionaire_Doc(ActionRequest request, ActionResponse response) {
    Long id_s = Long.valueOf(request.getContext().get("id_soumissionnaire").toString());
    Soumissionnaire s = Beans.get(SoumissionnaireRepository.class).find(id_s);
    response.setValue("soumissionnaire", s);
  }

  public void showAll_all_file(ActionRequest request, ActionResponse response) {
    Long id_ste = (Long) request.getContext().get("id");
    Soumissionnaire s = appPurchaseService.getSoumissionnaireById(id_ste);
    if (s != null) {
      response.setView(
          ActionView.define("Validation des documents")
              .model(PiecesJointe2.class.getName())
              .add("grid", "validationDocument-grid")
              .add("form", "validationDocument-form")
              .domain("self.soumissionnaire=" + id_ste)
              .context("id_soumissionnaire", id_ste)
              .param("forceEdit", "true")
              .map());
    } else {
      response.setFlash("Soumissionnaure Non valide");
    }
  }

  public void validerdocumentcourrant(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    appPurchaseService.validerPieceJointe2(id);
    if (appPurchaseService.attacherFormulaireTechniqueSoumissionnaire(id)) {
      appPurchaseService.affecterListCritereToSoumiss(id);
    }
    response.setReload(true);
  }

  public void Notvalide_document_courrant(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf((Integer) request.getContext().get("_id"));
    String comment = "";
    if (request.getContext().get("remarque") != null) {
      comment = request.getContext().get("remarque").toString();
      if (comment == null || comment.equals("")) {
        comment = "Aucun Commentaire";
      }
    }
    if (!appPurchaseService.attacherFormulaireTechniqueSoumissionnaire(id)) {
      appPurchaseService.supprimerListCritereFromSoumiss(id);
    }
    appPurchaseService.NotvaliderPieceJointe2(id, comment);
  }

  public void showPanelCriterNotation(ActionRequest request, ActionResponse response) {
    boolean test = (boolean) request.getContext().get("offreTechnique");
    response.setHidden("critereNotationPanel", !test);
  }

  public void ListSoumissionnaireDossierAdministrativeValide(
      ActionRequest request, ActionResponse response) {
    Long id_offre = (Long) request.getContext().get("id");
    if (id_offre > 0) {
      response.setView(
          ActionView.define("Liste des soumissionnaires")
              .model(Soumissionnaire.class.getName())
              .add("grid", "soumissionnaireTechnique-grid")
              .add("form", "soumissionnaireTechnique-form")
              .domain("self.dossierAdministrative=true and self.offreappels=" + id_offre)
              .param("forceEdit", "true")
              .map());
    } else {
      response.setFlash("Soumissionnaure Non valide");
    }
  }

  public void getNote_PourValidation(ActionRequest request, ActionResponse response) {
    Long id_soum = (Long) request.getContext().get("id");
    Soumissionnaire s = appPurchaseService.getSoumissionnaireById(id_soum);
    response.setValue("notePourValidation", s.getOffreappels().getPourcentageValideNote());
    response.setValue("pointPourValidation", s.getOffreappels().getPointValideNote());
    // response.setHidden("offreappels",true);
  }

  public void verifier_liste_pv(ActionRequest request, ActionResponse response) {
    response.setReadonly("numero", true);
    response.setHidden("btn_1", false);
  }

  public void verifier_noteMax(ActionRequest request, ActionResponse response) {
    BigDecimal d = (BigDecimal) request.getContext().get("note");
    Integer x = (Integer) request.getContext().get("noteMax");
    if (d.compareTo(new BigDecimal(x)) > 0) {
      response.setFlash("La Note saisie est supperieur au Maximum");
      response.setValue("note", new BigDecimal(x));
      response.setCanClose(false);
      return;
    }
  }

  public void load_point_obtenu(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    BigDecimal seuil = BigDecimal.valueOf(0);
    BigDecimal s1 = new BigDecimal(request.getContext().get("notePourValidation").toString());
    BigDecimal s2 = new BigDecimal(request.getContext().get("pointPourValidation").toString());
    if (s1.compareTo(BigDecimal.ZERO) > 0) {
      seuil = s1;
    } else {
      seuil = s2;
    }
    List<CritereNotationDetail> ls =
        (List<CritereNotationDetail>) request.getContext().get("critereNotation");
    BigDecimal point = BigDecimal.ZERO;
    BigDecimal pr_cent = BigDecimal.ZERO;
    for (CritereNotationDetail tmp : ls) {
      point = point.add(tmp.getNote());
      pr_cent = pr_cent.add(BigDecimal.valueOf(tmp.getNoteMax()));
    }
    pr_cent =
        point
            .divide(pr_cent, 2, RoundingMode.HALF_UP)
            .multiply(new BigDecimal(100l))
            .setScale(2, RoundingMode.HALF_UP);
    response.setValue("note", pr_cent);
    Soumissionnaire soum = appPurchaseService.getSoumissionnaireById(id);
    if (soum.getOffreappels().getPourcentage()) {
      pr_cent =
          point
              .divide(pr_cent, 2, RoundingMode.HALF_UP)
              .multiply(new BigDecimal(100l))
              .setScale(2, RoundingMode.HALF_UP);
      response.setValue("note", pr_cent);
    } else {
      pr_cent = point.setScale(2, RoundingMode.HALF_UP);
      response.setValue("note_point", pr_cent);
    }

    if (seuil.compareTo(pr_cent) > 0) {
      response.setValue("dossierTechnique", false);
      soum.setEtat("ecarter");
      response.setValue("etat", "ecarter");
    } else {
      response.setValue("dossierTechnique", true);
      response.setValue("etat", "");
    }
  }

  public void ecarterOffre(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    BigDecimal estimation = BigDecimal.ZERO;
    BigDecimal estimation_max = BigDecimal.ZERO;
    BigDecimal estimation_min = BigDecimal.ZERO;
    BigDecimal pr_cent = BigDecimal.ZERO;
    Soumissionnaire soum = appPurchaseService.getSoumissionnaireById(id);

    estimation = estimation.add(soum.getOffreappels().getEstimation_coup());
    if (soum.getOffreappels().getPrestation().getId() == 1) {
      estimation_max = estimation.add(estimation.multiply(BigDecimal.valueOf(0.2)));
      estimation_min = estimation.multiply(BigDecimal.valueOf(0.2));
    } else {
      estimation_max = estimation.add(estimation.multiply(BigDecimal.valueOf(0.25)));
      estimation_min = estimation.multiply(BigDecimal.valueOf(0.25));
    }
    pr_cent = pr_cent.add(soum.getOffreFiance());

    if (estimation_max.compareTo(pr_cent) <= 0 || pr_cent.compareTo(estimation_min) <= 0) {
      response.setFlash("Le soumissionnaire est ecarté");
      response.setValue("etat", "ecarter");
    } else {
      response.setValue("etat", null);
    }
  }

  public void Afficher_list_soum_offreFiance(ActionRequest request, ActionResponse response) {
    Long id_offre = (Long) request.getContext().get("id");
    if (id_offre > 0) {
      response.setView(
          ActionView.define("Liste des soumissionnaires")
              .model(Soumissionnaire.class.getName())
              .add("grid", "soumissionnaireFinance-grid")
              .add("form", "soumissionnaireFinance-form")
              .param("forceEdit", "true")
              .domain(
                  " self.offreappels="
                      + id_offre
                      + " and ( (self.dossierTechnique='true' and self.offreappels.offreTechnique='true' ) or "
                      + "(self.offreappels.offreTechnique='false' and self.dossierAdministrative='true' ) )")
              .context("ttrtt", id_offre)
              .map());
    } else {
      response.setFlash("Soumissionnaure Non valide");
    }
  }

  public void Afficher_list_soum_offreFiance2(ActionRequest request, ActionResponse response) {
    Long id_offre = (Long) request.getContext().get("id");
    OffreAppel o = Beans.get(OffreAppelRepository.class).find(id_offre);
    List<Soumissionnaire> ls = o.getSoumissionnaire();
    if (id_offre > 0) {
      response.setView(
          ActionView.define("Liste des soumissionnaires")
              .model(OffreAppel.class.getName())
              /*.add("grid", "soumissionnaireFinance-grid")*/
              .add("form", "soumissionnaireFinance2-form")
              .param("forceEdit", "true")
              .context("_showRecord", id_offre)
              .context("id_offre", id_offre)
              .map());
    } else {
      response.setFlash("Soumissionnaure Non valide");
    }
  }

  public void affecter_offre_to_Soumissionnaire(ActionRequest request, ActionResponse response) {
    Long id_soum = (Long) request.getContext().get("id");
    appPurchaseService.affecterMarcheToSoumissionnaire(id_soum);
    appPurchaseService.createMarcheProvisoire(id_soum);
    response.setReload(true);
  }

  public void load_soumiss_not_valide(ActionRequest request, ActionResponse response) {
    Long id_offre = (Long) request.getContext().get("id");
    List<Soumissionnaire> ls = appPurchaseService.getSoumissionnaireNotValideByIdOffre(id_offre);
    response.setValue("soumisField", ls);
    response.setSignal("refresh-tab", true);
  }

  public void load_default_coordonnee(ActionRequest request, ActionResponse response) {
    String tel = "+212 05 35 62 31 83";
    String email = "infos@ccisbk.ma";
    String adress = "CCIS-Beni Mellal Khénifra";
    response.setValue("telephone", tel);
    response.setValue("email", email);
    response.setValue("adress", adress);
  }

  public void getArticleData(ActionRequest request, ActionResponse response) {
    Article fournis = (Article) request.getContext().get("fournisseurSelect");
    Long id = fournis.getId();
    Fournisseur f = appPurchaseService.getFournisseurById(id);
    response.setValue("tele", f.getTele());
    response.setValue("email", f.getEmail());
    response.setValue("ville", f.getVille());
    response.setValue("adresse", f.getAdresse());
  }

  public void activerModification_cordonnee(ActionRequest request, ActionResponse response) {
    response.setReadonly("telephone", false);
    response.setReadonly("email", false);
    response.setReadonly("adress", false);
    response.setHidden("btn_active_field_coordonnee", true);
  }

  public void loadDataNotationSelect(ActionRequest request, ActionResponse response) {
    CritereNotationaSelect n = request.getContext().asType(CritereNotationaSelect.class);
    if (n != null
        && n.getNotationSelect() != null
        && n.getNotationSelect().getDescription() != null)
      response.setValue("description", n.getNotationSelect().getDescription());
    if (n != null && n.getNotationSelect() != null && n.getNotationSelect().getNoteMax() != null)
      response.setValue("notationSelect.noteMax", n.getNotationSelect().getNoteMax());
  }

  public void tw_getPrestationCritere(ActionRequest request, ActionResponse response) {
    Context context = request.getContext().getParent();
    TypePrestation p = (TypePrestation) context.get("prestation");
    response.setAttr("notationSelect", "domain", "self.prestation.id=" + p.getId());
  }

  public void ouvrirEnvlope(ActionRequest request, ActionResponse response) {
    response.setValue("openEnveloppe", true);
    response.setHidden("docField", false);
    response.setHidden("btn_open_envloppe", true);
  }

  public void imprimer_pv_1(ActionRequest request, ActionResponse response) throws AxelorException {
    Long id_offres = (Long) request.getContext().get("id");
    OffreAppel o = appPurchaseService.getOffreById(id_offres);
    LocalDate dateL = (LocalDate) request.getContext().get("dateLancement");
    String fileLink =
        ReportFactory.createReport(
                IReport.PVGARDIENNAGE1ereseance, "PV d'appel d'offre ouvert 1ére séance")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id_offre", id_offres.intValue())
            .addParam("numOffres", o.getNumero())
            .addParam("objetOffres", o.getNom())
            .addParam("dLancementOffres", java.sql.Date.valueOf(dateL))
            .addParam("montantOffres", o.getEstimation_coup())
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("PV d'appel d'offre ouvert 1ére séance").add("html", fileLink).map());
  }

  public void imprimerListSoumissionnaire(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id_offres = (Long) request.getContext().get("idOffres");
    OffreAppel o = appPurchaseService.getOffreById(id_offres);
    String fileLink =
        ReportFactory.createReport(
                IReport.Listsoumissionnaire, "Liste des enveloppes des soumissionnaires")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id_offre", o.getId())
            .addParam("numOffres", o.getNumero())
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("Liste des enveloppes des soumissionnaires").add("html", fileLink).map());
  }

  public void valide_document_courrant_reserve(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id = Long.valueOf((Integer) request.getContext().get("_id"));
    String comment = "";
    if (request.getContext().get("remarque") != null) {
      comment = request.getContext().get("remarque").toString();
      if (comment == null || comment.equals("")) {
        comment = "Aucun Commentaire";
      }
    }
    if (appPurchaseService.attacherFormulaireTechniqueSoumissionnaire(id)) {
      appPurchaseService.affecterListCritereToSoumiss(id);
    }
    appPurchaseService.validerReservePieceJointe2(id, comment);
  }

  public void imprimerLetteInformation_methode(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id_soum = Long.valueOf((Integer) request.getContext().get("_id"));
    String br = request.getContext().get("BrAnormale").toString();
    LocalDate date = (LocalDate) request.getContext().get("dateDocument");
    if (br == null || br.trim().equals("")) {
      response.setFlash("La liste des bordereaux estimatifs anormales est obligatoire");
      return;
    }
    Soumissionnaire s = appPurchaseService.getSoumissionnaireById(id_soum);
    OffreAppel o = s.getOffreappels();
    o.setDateDocument(date);
    appPurchaseService.saveOffreAppel(o);
    String soumissionElectro = s.getSoumissionElectro() ? "oui" : "non";
    String fileLink =
        ReportFactory.createReport(IReport.LettreInformationMarche, "Lettre d'information")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("steName", s.getSociete())
            .addParam("numOffres", o.getNumero())
            .addParam("ObjectOffre", o.getNom())
            .addParam("montantLettre", ConvertNomreToLettres.getStringMontant(s.getOffreFiance()))
            .addParam("montant", s.getOffreFiance())
            .addParam("BEstimatif", br)
            .addParam("soumissionElectro", soumissionElectro)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Lettre d'infromation").add("html", fileLink).map());
  }

  public void disableSoumissionPapier(ActionRequest request, ActionResponse response) {
    boolean select1 = (boolean) request.getContext().get("soumissionElectro");
    if (select1 && request.getContext().get("soumissionPapier") != null)
      response.setValue("soumissionPapier", false);
  }

  public void disableSoumissionElectro(ActionRequest request, ActionResponse response) {
    boolean select1 = (boolean) request.getContext().get("soumissionPapier");
    if (select1 && request.getContext().get("soumissionElectro") != null)
      response.setValue("soumissionElectro", false);
  }

  public void show_typeSoumission(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    if (id > 0) {
      Long idOffres = Long.valueOf((Integer) request.getContext().get("idOffres"));
      OffreAppel o = appPurchaseService.getOffreById(idOffres);
      String[] t = o.getTypeDossier().split(",");
      for (String tmp : t) {
        if (tmp.equals("papier")) response.setHidden("soumissionPapier", false);
        if (tmp.equals("éléctronique")) response.setHidden("soumissionElectro", false);
      }

      if (o.getEchantillionnage()) {
        response.setHidden("dateEchantillon", false);
        response.setHidden("heurEchantillon", false);
      } else {
        response.setHidden("dateEchantillon", true);
        response.setHidden("heurEchantillon", true);
      }
    }
  }

  public void show_validation_doc_soumission(ActionRequest request, ActionResponse response) {
    Long id_offre = (Long) request.getContext().get("id");
    OffreAppel o = appPurchaseService.getOffreById(id_offre);
    response.setView(
        ActionView.define("Liste des documents de validation")
            .model(MarcheProvisoire.class.getName())
            .add("form", "validation-MarcheProvisoire")
            .param("forceEdit", "true")
            .param("popup", "Reload")
            .param("popup-save", "false")
            .param("show-confirm", "false")
            .param("show-toolbar", "false")
            .context("_id", o.getId())
            .context("_showRecord", o.getMarcheProvisoire().getId())
            .map());
  }

  public void rejeter_soumissionnaire(ActionRequest request, ActionResponse response) {
    Long id_offre = (Long) request.getContext().get("id");
    OffreAppel o = appPurchaseService.getOffreById(id_offre);
    o.setDateDocument(null);
    o.setNbrJourLimit(null);
    appPurchaseService.saveOffreAppel(o);
    Soumissionnaire soumissionnaire_suivant =
        appPurchaseService.getSoumissionnaireSuivant(o.getId());
    if (o.getSoumissGagnant().getId() != soumissionnaire_suivant.getId()) {
      appPurchaseService.recalerSoumissionnaireGagnant(o.getSoumissGagnant().getId());
      appPurchaseService.setSoumissionnaireHas_Marche(o.getSoumissGagnant().getId(), false);
      appPurchaseService.setSoumissionnaireHas_Marche(soumissionnaire_suivant.getId(), true);
    }
    o.setSoumissGagnant(soumissionnaire_suivant);
    appPurchaseService.saveOffreAppel(o);
    appPurchaseService.createMarcheProvisoire(soumissionnaire_suivant.getId());
    response.setReload(true);
  }

  public void validationProvisoire(ActionRequest request, ActionResponse response) {
    MarcheProvisoire m = request.getContext().asType(MarcheProvisoire.class);
    Long id_offre = Long.valueOf((Integer) request.getContext().get("_id"));
    boolean testDocElectro = false;
    OffreAppel o = appPurchaseService.getOffreById(id_offre);
    if (o.getSoumissGagnant() != null && o.getSoumissGagnant().getSoumissionElectro())
      testDocElectro = m.getComplementAdministrative() && m.getJustifPrix();
    else
      testDocElectro =
          m.getComplementAdministrative() && m.getJustifPrix() && m.getDocumentPapier();
    if (testDocElectro) {
      o = appPurchaseService.attribuerNumerosMarche(o);
      o.setValidationMarheProvisoire(true);
    } else {
      o.setValidationMarheProvisoire(false);
    }
    appPurchaseService.saveOffreAppel(o);
    response.setReload(true);
  }

  public void imprimerLettreDeNotification(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id_offres = (Long) request.getContext().get("id");
    OffreAppel o = appPurchaseService.getOffreById(id_offres);
    String fileLink =
        ReportFactory.createReport(IReport.LettreDeNotification, "Lettre d'information")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id_offres", id_offres)
            .addParam("numMarche", o.getNumMarche())
            .addParam("ObjectOffre", o.getNom())
            .addParam("nameRepresentant", o.getSoumissGagnant().getRepresentant())
            .addParam("adresseSte", o.getSoumissGagnant().getAdresse())
            .addParam("steName", o.getSoumissGagnant().getSociete())
            .addParam(
                "montantLettre",
                ConvertNomreToLettres.getStringMontant(o.getSoumissGagnant().getOffreFiance()))
            .addParam("montant", o.getSoumissGagnant().getOffreFiance())
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Lettre d'infromation").add("html", fileLink).map());
  }

  public void verfierVersionPapier(ActionRequest request, ActionResponse response) {
    Long id_offre = Long.valueOf((Integer) request.getContext().get("_id"));
    OffreAppel o = appPurchaseService.getOffreById(id_offre);
    Soumissionnaire s = o.getSoumissGagnant();
    if (s.getSoumissionElectro()) {
      response.setHidden("panelVersionPapier", true);
    } else {
      response.setHidden("panelVersionPapier", false);
    }
  }

  public void print_prorogation(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String[] tabDays =
        new String[] {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};
    String[] tabMonth =
        new String[] {
          "Janvier",
          "Février",
          "Mars",
          "Avril",
          "Mai",
          "Juin",
          "Juillet",
          "Août",
          "Septembre",
          "Octobre",
          "Novembre ",
          "Décembre"
        };
    Long id_offre = Long.valueOf((Integer) request.getContext().get("_id"));
    Integer nbr_jour = (Integer) request.getContext().get("nbrJourProrogation");
    LocalDate dateAttente = (LocalDate) request.getContext().get("dateProrogation");

    OffreAppel o = appPurchaseService.getOffreById(id_offre);
    Soumissionnaire s = o.getSoumissGagnant();
    o.setDateProrogation(dateAttente);
    o.setNbrJourProrogation(nbr_jour);
    appPurchaseService.saveOffreAppel(o);
    String date_21_jour =
        tabDays[dateAttente.getDayOfWeek().getValue() - 1]
            + " "
            + dateAttente.getDayOfMonth()
            + " "
            + tabMonth[dateAttente.getMonthValue() - 1]
            + " "
            + dateAttente.getYear();
    String fileLink =
        ReportFactory.createReport(IReport.DemandeProrogation, "Demande_de_prorogation")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("name_societe", s.getSociete())
            .addParam("num_offre", o.getNumero())
            .addParam("adresse_societe", o.getSoumissGagnant().getAdresse())
            .addParam("nbrJourProrogation", o.getNbrJourProrogation().toString())
            .addParam("date21Jours", date_21_jour)
            .addParam("object_offre", o.getNom())
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Demande de prorogation").add("html", fileLink).map());
  }

  public void calculeAutoMontantGenerale(ActionRequest request, ActionResponse response) {
    BigDecimal qte = BigDecimal.ZERO;
    BigDecimal prix = BigDecimal.ZERO;
    if (request.getContext().get("p_unitaire") != null) {
      prix = (BigDecimal) request.getContext().get("p_unitaire");
    }

    if (request.getContext().get("articleprix") instanceof ArticleDetails) {
      ArticleDetails a = (ArticleDetails) request.getContext().get("articleprix");
      qte = a.getQuantite2();
    } else {
      Map<String, Object> art = (Map<String, Object>) request.getContext().get("articleprix");
      if (art != null && art.get("quantite2") != null) {
        qte = (BigDecimal) art.get("quantite2");
      }
    }

    response.setValue("prixtotal", prix.multiply(qte));
  }

  public void genererOrdreDePayeent(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id = (Long) request.getContext().get("id");
    FactureAchat f = appPurchaseService.getFactureById(id);
    if (f != null) {
      OrderPaymentCommande o = null;
      if (f.getOrdreDePayment() == null) {
        o = appPurchaseService.createOrdreDePayement(f);
        f.setOrdreDePayment(o);
        appPurchaseService.saveFactureAchat(f);
      } else {
        o = f.getOrdreDePayment();
      }

      String fileLink =
          ReportFactory.createReport(IReport.OrdrePayementFacture, "OrdrePayement")
              .addParam("Locale", ReportSettings.getPrintingLocale(null))
              .addParam("nomFournisseur", o.getBeneficiaire())
              .addParam("adresseFournisseur", f.getFournisseur().getAdresse())
              .addParam("ribFournisseur", o.getCompteBenef())
              .addParam("rubrique", o.getRubriqueBudgetaire().getName())
              .addParam("codeBudg", o.getRubriqueBudgetaire().getCodeBudg())
              .addParam("objetCommande", f.getNatureOperation())
              .addParam("montantOrder", f.getMontant())
              .addParam(
                  "montantOrderString", ConvertNomreToLettres.getStringMontant(f.getMontant()))
              .addParam("numeroOrdre", o.getNumero() == null ? "-1" : o.getNumero())
              .addParam("anneeOrdre", o.getYear().toString())
              .generate()
              .getFileLink();
      response.setView(ActionView.define("Ordre de payement").add("html", fileLink).map());

    } else {
      response.setFlash("Aucun Facture trouvée");
      return;
    }
  }

  public void updateOPInfo(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    FactureAchat f = appPurchaseService.getFactureById(id);
    OrderPaymentCommande o = null;
    if (f.getOrdreDePayment() != null) {
      o = appPurchaseService.updateOrdreDePayement(f);
    } else {
      o = appPurchaseService.createOrdreDePayement(f);
      f.setOrdreDePayment(o);
      appPurchaseService.saveFactureAchat(f);
    }

    response.setReload(true);
  }

  public void deleteFacture(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    appPurchaseService.deleteFactureAchat(id);
    response.setReload(true);
  }

  public void tw_action_annulation_oa(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    OffreAppel of = Beans.get(OffreAppelRepository.class).find(id);
    of.setAnnulation(true);
    appPurchaseService.saveOffreAppel(of);
    response.setReload(true);
  }

  public void tw_action_print_annulation_oa(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id = (Long) request.getContext().get("id");
    OffreAppel appeloffre = Beans.get(OffreAppelRepository.class).find(id);
    List<Media> ls =
        appeloffre.getMediaPublication().stream()
            .filter(media -> media.getType().equals("Journale"))
            .collect(Collectors.toList());
    String journale_info = "";
    for (Media m : ls) {
      if (journale_info.isEmpty() && ls.size() == 1) journale_info = " au journal ";
      else if (journale_info.isEmpty()) journale_info = " aux journaux ";
      else journale_info += " et ";

      journale_info +=
          m.getNom_media()
              + " n° : "
              + m.getNumero_publication()
              + " du "
              + m.getDate_publication();
    }
    String web_info = "";
    Media m =
        appeloffre.getMediaPublication().stream()
            .filter(media -> media.getType().equals("Portail des marchés publics"))
            .findFirst()
            .orElse(null);
    if (m != null) {
      web_info = m.getNom_media() + " le : " + m.getDate_publication();
    }
    String fileLink =
        ReportFactory.createReport(IReport.AnnulationAppeloffre, "Annulation Appel Offre")
            .addParam("id", id)
            .addParam("journal_info", journale_info)
            .addParam("web_info", web_info)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Annulation Appel Offre").add("html", fileLink).map());
  }

  public void tw_show_hide_num_publication(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String val = request.getContext().get("type").toString();
    if (val != null && val.equals("Portail des marchés publics")) {
      response.setValue("numero_publication", '0');
      response.setHidden("numero_publication", true);
      response.setAttr("nom_media", "title", "Nom du domaine");
    } else {
      response.setHidden("numero_publication", false);
      response.setAttr("nom_media", "title", "Nom");
    }
  }

  public void tw_load_new_cuation_lettre(ActionRequest request, ActionResponse response)
      throws AxelorException {
    BigDecimal caution = (BigDecimal) request.getContext().get("montantCaution");
    String mt = ConvertNomreToLettres.getStringMontant(caution);
    response.setValue("caution_lettre", mt);
  }

  public void tw_load_new_estimation_coup_lettre(ActionRequest request, ActionResponse response)
      throws AxelorException {
    BigDecimal caution = (BigDecimal) request.getContext().get("estimation_coup");
    String mt = ConvertNomreToLettres.getStringMontant(caution);
    response.setValue("estimation_coup_lettre", mt);
  }

  public void tw_print_avis_ao_document(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id = Long.valueOf(request.getContext().get("_id").toString());
    Boolean langue_ar =
        request.getContext().get("langue") != null
            ? ((Boolean) request.getContext().get("langue"))
            : false;
    String report = "";
    if (langue_ar) {
      report = IReport.AvisAppeloffre_AR;
    } else {
      report = IReport.AvisAppeloffre_FR;
    }
    String fileLink =
        ReportFactory.createReport(report, "Avis Offre Appel")
            .addParam("id", id)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Avis Offre Appel").add("html", fileLink).map());
  }

  public void tw_modifier_introduction_convocation(ActionRequest request, ActionResponse response) {
    String type = request.getContext().get("post_direction").toString();
    response.setHidden("zone_departement_service_comite", true);
    response.setHidden("zone_annexe_comite", true);

    switch (type) {
      case "controleur_etat":
        response.setValue("introduction_convocation", "Monsieur le Contrôleur d’Etat");
        break;
      case "directeur_ic_BM":
      case "directeur_annexe":
        response.setValue("introduction_convocation", "Monsieur le Directeur");
        if (type.equals("directeur_annexe")) response.setHidden("zone_annexe_comite", false);
        break;
      default:
        response.setValue("introduction_convocation", "Monsieur");
        if (type.equals("chef_departement")) {
          response.setAttr("nom_service_departement", "title", "Nom du département");
          response.setHidden("zone_departement_service_comite", false);
        } else if (type.equals("chef_service")) {
          response.setAttr("nom_service_departement", "title", "Nom du service");
          response.setHidden("zone_departement_service_comite", false);
        }
        break;
    }
  }

  public void tw_form_convocation_appel_offre(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id = (Long) request.getContext().get("id");
    String fileLink =
        ReportFactory.createReport(IReport.Convocationcomission, "Convocation Comission")
            .addParam("id", id)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Convocation Comission").add("html", fileLink).map());
  }

  public void tw_print_orderService_offre_appel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id_ordreService = (Long) request.getContext().get("id");
    String fileLink =
        ReportFactory.createReport(IReport.OrdreService, "Ordre de service")
            .addParam("id", id_ordreService)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de service").add("html", fileLink).map());
  }

  public void tw_print_LettreNotification(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id_ordreService = (Long) request.getContext().get("id");
    String fileLink =
        ReportFactory.createReport(IReport.LettreNotificationBK, "Lettre deNotification")
            .addParam("id", id_ordreService)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Lettre de notification").add("html", fileLink).map());
  }

  public void tw_print_TableauAdmiEtTechnique(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Context context = request.getContext();
    Map<String, Object> leadContext = (Map<String, Object>) context.get("numero_offre");
    OffreAppel numero =
        Beans.get(OffreAppelRepository.class).find(((Integer) leadContext.get("id")).longValue());
    String fileLink =
        ReportFactory.createReport(
                IReport.TableauAdmiEtTechnique, "Tableau administratif et technique")
            .addParam("numero_offre", numero.getNumero())
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("Tableau administratif et technique").add("html", fileLink).map());
  }

  public void tw_print_feuille_presence(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Context context = request.getContext();
    Map<String, Object> leadContext = (Map<String, Object>) context.get("numero_offre");
    OffreAppel numero =
        Beans.get(OffreAppelRepository.class).find(((Integer) leadContext.get("id")).longValue());

    String fileLink =
        ReportFactory.createReport(IReport.feuilledepresence, "Feuille de présence")
            .addParam("numero_offre", numero.getNumero())
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Feuille de présence").add("html", fileLink).map());
  }

  public void validerMeilleurOffre(ActionRequest request, ActionResponse response) {
    Long id_offre = Long.valueOf((Integer) request.getContext().get("_id"));
    OffreAppel o = appPurchaseService.getOffreById(id_offre);
    List<Soumissionnaire> ld = new ArrayList<>();
    for (Soumissionnaire s : o.getSoumissionnaire()) {
      if ((!Objects.equals(s.getEtat(), "ecarter") || s.getEtat() == null)
          && s.getDossierAdministrative()) {
        ld.add(s);
      }
    }
    BigDecimal moy =
        (ld.stream().map(Soumissionnaire::getOffreFiance).reduce(BigDecimal.ZERO, BigDecimal::add))
            .divide(BigDecimal.valueOf(ld.size()), 2, RoundingMode.HALF_UP);
    response.setValue("moyenne", moy);
    ld =
        ld.stream()
            .filter(soumissionnaire -> soumissionnaire.getOffreFiance().compareTo(moy) < 0)
            .collect(Collectors.toList());
    Soumissionnaire s_choisi =
        ld.stream()
            .max(
                Comparator.comparingDouble(
                    value -> Double.parseDouble(value.getOffreFiance().toString())))
            .orElse(null);
    response.setValue("soumChoisi", s_choisi);
    response.setValue("montant", s_choisi.getOffreFiance());
  }

  public void saveMeilleurOffre(ActionRequest request, ActionResponse response) {
    Long id_offre = Long.valueOf((Integer) request.getContext().get("_id"));
    OffreAppel o = appPurchaseService.getOffreById(id_offre);
    Soumissionnaire s4 = (Soumissionnaire) request.getContext().get("soumChoisi");
    o.setSoumChoisi(s4);
    appPurchaseService.saveOffreAppel(o);
  }

  public void add_validation(ActionRequest request, ActionResponse response) {
    Long id_offre = (Long) request.getContext().get("id");
    OffreAppel o = Beans.get(OffreAppelRepository.class).find(id_offre);
    List<Soumissionnaire> ls = o.getSoumissionnaire();
    if (id_offre > 0) {
      response.setView(
          ActionView.define("La validation des livrables")
              .model(OffreAppel.class.getName())
              .add("form", "add_validation-form")
              .param("forceEdit", "true")
              .context("_showRecord", id_offre)
              .context("id_offre", id_offre)
              .map());
    } else {
      response.setFlash("La validation des livrables Non valide");
    }
  }

  public void add_ordreService(ActionRequest request, ActionResponse response) {
    Long id_offre = (Long) request.getContext().get("id");
    OffreAppel o = Beans.get(OffreAppelRepository.class).find(id_offre);
    List<Soumissionnaire> ls = o.getSoumissionnaire();
    if (id_offre > 0) {
      response.setView(
          ActionView.define("Les ordres de service")
              .model(OffreAppel.class.getName())
              .add("form", "add_ordre-form")
              .param("forceEdit", "true")
              .context("_showRecord", id_offre)
              .context("id_offre", id_offre)
              .map());
    } else {
      response.setFlash("Les ordres de service Non valide");
    }
  }

  public void valider_provisoire_courrant_ss(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    appPurchaseService.validerProvisoire(id);
    response.setReload(true);
  }

  public void valide_provisoire_courrant_reserve_ss(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id = Long.valueOf((Integer) request.getContext().get("_id"));
    String comment = "";
    if (request.getContext().get("remarque") != null) {
      comment = request.getContext().get("remarque").toString();
      if (comment == null || comment.equals("")) {
        comment = "Aucun Commentaire";
      }
    }
    appPurchaseService.validerReserveProvisoire(id, comment);
  }

  public void Notvalide_provisoire_courrant_ss(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf((Integer) request.getContext().get("_id"));
    String comment = "";
    if (request.getContext().get("remarque") != null) {
      comment = request.getContext().get("remarque").toString();
      if (comment == null || comment.equals("")) {
        comment = "Aucun Commentaire";
      }
    }
    appPurchaseService.NotvaliderProvisoire(id, comment);
  }

  public void valider_definitif_courrant(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    appPurchaseService.validerDefinitif(id);
    response.setReload(true);
  }

  public void valide_definitif_courrant_reserve(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id = Long.valueOf((Integer) request.getContext().get("_id"));
    String comment = "";
    if (request.getContext().get("remarque") != null) {
      comment = request.getContext().get("remarque").toString();
      if (comment == null || comment.equals("")) {
        comment = "Aucun Commentaire";
      }
    }
    appPurchaseService.validerReserveDefinitif(id, comment);
  }

  public void Notvalide_definitif_courrant(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf((Integer) request.getContext().get("_id"));
    String comment = "";
    if (request.getContext().get("remarque") != null) {
      comment = request.getContext().get("remarque").toString();
      if (comment == null || comment.equals("")) {
        comment = "Aucun Commentaire";
      }
    }
    appPurchaseService.NotvaliderDefinitif(id, comment);
  }

  public void tw_print_pv_provisoire(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Context context = request.getContext();
    Map<String, Object> leadContext = (Map<String, Object>) context.get("numero_offre");
    OffreAppel numero =
        Beans.get(OffreAppelRepository.class).find(((Integer) leadContext.get("id")).longValue());
    String fileLink =
        ReportFactory.createReport(IReport.pv_provisoire, "PV de la réception provisoire")
            .addParam("numero_offre", numero.getNumero())
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("PV de la réception provisoire").add("html", fileLink).map());
  }

  public void tw_print_pv_sous_comission(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Context context = request.getContext();
    Map<String, Object> leadContext = (Map<String, Object>) context.get("numero_offre");
    OffreAppel numero =
        Beans.get(OffreAppelRepository.class).find(((Integer) leadContext.get("id")).longValue());
    String fileLink =
        ReportFactory.createReport(IReport.PvSouscomission, "PV de sous commission")
            .addParam("numero_offre", numero.getNumero())
            .generate()
            .getFileLink();
    response.setView(ActionView.define("PV de sous commission").add("html", fileLink).map());
  }

  public void tw_print_tous_article(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String language = String.valueOf(request.getContext().get("lang_de_article_type").toString());
    switch (language) {
      case "français":
        {
          String fileLink =
              ReportFactory.createReport(IReport.articlesfracais, "Articles en français")
                  .addParam("id", 1)
                  .generate()
                  .getFileLink();
          response.setView(ActionView.define("Articles en français").add("html", fileLink).map());
          break;
        }
      case "arabe":
        {
          String fileLink =
              ReportFactory.createReport(IReport.articlesarabe, "Article en Arabe")
                  .addParam("id", 1)
                  .generate()
                  .getFileLink();
          response.setView(ActionView.define("Article en Arabe").add("html", fileLink).map());
          break;
        }
    }
  }

  public void Imprimer_inventaire(ActionRequest request, ActionResponse response)
      throws AxelorException {

    RubriqueBudgetaireGenerale rubrique =
        (RubriqueBudgetaireGenerale) request.getContext().get("budgetrub");

    if (request.getContext().get("dateDebut") == null) {
      response.setError("Le champ <b>Date Début</b> est obligatoire");
      return;
    }
    if (request.getContext().get("dateFin") == null) {
      response.setError("Le champ <b>Date Fin</b> est obligatoire");
      return;
    }
    String date_debut = request.getContext().get("dateDebut").toString();
    String date_fin = request.getContext().get("dateFin").toString();

    LocalDate dateDebut = LocalDate.parse(date_debut);
    LocalDate dateFin = LocalDate.parse(date_fin);

    String report = com.axelor.apps.purchase.report.IReport.imprimerinv;

    String fileLink =
        ReportFactory.createReport(report, "imprimerinv")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("datedebut", java.sql.Date.valueOf(dateDebut))
            .addParam("datefin", java.sql.Date.valueOf(dateFin))
            .addParam("rubrique", rubrique.getId())
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Inventaire").add("html", fileLink).map());
  }

  public void get_all_rubriqueBudgetaireAC(ActionRequest request, ActionResponse response) {
    int year = LocalDate.now().getYear();
    Context ls = request.getContext().getParent();
    if (ls != null && !ls.get("annee").toString().equals("0")) {
      try {
        year = Integer.parseInt(ls.get("annee").toString());
      } catch (Exception ex) {
        response.setFlash(
            "Erreur : impossible de convertir " + ls.get("annee").toString() + " en entier");
        return;
      }
    }
    response.setAttr("budgetrub", "domain", "self.id in (0)");
    try {
      VersionRubriqueBudgetaire versionRubriqueBudgetaire =
          Beans.get(VersionRubriqueBudgetaireRepository.class)
              .all()
              .filter("self.annee = :year")
              .bind("year", year)
              .fetchOne();
      if (versionRubriqueBudgetaire == null) {
        response.setFlash("Aucun budget pour annee " + year);
        return;
      }

      VersionRB version_rb =
          versionRubriqueBudgetaire.getVersionRubriques().stream()
              .filter(VersionRB::getIs_versionFinale)
              .findFirst()
              .orElse(null);

      if (version_rb == null) {
        response.setFlash("Aucun budget n'est validée pour année" + year);
        return;
      }

      List<Long> ids =
          Beans.get(RubriquesBudgetaireRepository.class).all()
              .filter("self.id_version = :ids_version").bind("ids_version", version_rb.getId())
              .fetch().stream()
              .map(RubriquesBudgetaire::getId_rubrique_generale)
              .collect(Collectors.toList());

      response.setAttr(
          "budgetrub",
          "domain",
          "self.codeBudg like '2%' and self.id in ("
              + ids.stream().map(Object::toString).collect(Collectors.joining(","))
              + ")");

    } catch (Exception ex) {
      response.setFlash(ex.getMessage());
      return;
    }
  }

  public void tw_et_all_rubriqueBudgetaire(ActionRequest request, ActionResponse response) {
    int year = LocalDate.now().getYear();
    response.setAttr("rubriqueBudg", "domain", "self.id in (0)");
    try {
      VersionRubriqueBudgetaire versionRubriqueBudgetaire =
          Beans.get(VersionRubriqueBudgetaireRepository.class)
              .all()
              .filter("self.annee = :year")
              .bind("year", year)
              .fetchOne();
      if (versionRubriqueBudgetaire == null) {
        response.setFlash("Aucun budget pour annee " + year);
        return;
      }

      VersionRB version_rb =
          versionRubriqueBudgetaire.getVersionRubriques().stream()
              .filter(VersionRB::getIs_versionFinale)
              .findFirst()
              .orElse(null);

      if (version_rb == null) {
        response.setFlash("Aucun budget n'est validée pour année" + year);
        return;
      }

      List<Long> ids =
          Beans.get(RubriquesBudgetaireRepository.class).all()
              .filter("self.id_version = :ids_version").bind("ids_version", version_rb.getId())
              .fetch().stream()
              .map(RubriquesBudgetaire::getId_rubrique_generale)
              .collect(Collectors.toList());

      response.setAttr(
          "rubriqueBudg",
          "domain",
          "self.codeBudg like '2%' and self.id in ("
              + ids.stream().map(Object::toString).collect(Collectors.joining(","))
              + ")");

    } catch (Exception ex) {
      response.setFlash(ex.getMessage());
      return;
    }
  }

  public void tw_get_rubrique_by_year(ActionRequest request, ActionResponse response) {
    String[] names =
        new String[] {
          "rubriqueBudgetaire",
          "rubriquesBudgetaire",
          "budgetrub",
          "rubriqueBudg",
          "$rubriqueBudgetaire"
        };
    int year = LocalDate.now().getYear();

    if (request.getContext().get("year") != null) {
      try {
        year = Integer.parseInt(request.getContext().get("year").toString());
      } catch (Exception ex) {
        response.setFlash(
            "Erreur : Impossible de convertir <b>"
                + request.getContext().get("year").toString()
                + "</b> en entier");
      }
    } else if (request.getContext().getParent() != null) {
      Context context = request.getContext().getParent();
      if (context.get("annee") != null) {
        try {
          year = Integer.parseInt(context.get("annee").toString());
        } catch (Exception ex) {
          response.setFlash(
              "Erreur : Impossible de convertir <b>"
                  + context.get("annee").toString()
                  + "</b> en entier");
        }
      } else if (context.get("year") != null) {
        try {
          year = Integer.parseInt(context.get("year").toString());
        } catch (Exception ex) {
          response.setFlash(
              "Erreur : Impossible de convertir <b>"
                  + context.get("year").toString()
                  + "</b> en entier");
        }
      }
    }

    for (String name : names) {
      response.setAttr(name, "domain", "self.id in (0)");
    }
    if (year == 0) {
      return;
    }
    try {
      VersionRubriqueBudgetaire versionRubriqueBudgetaire =
          Beans.get(VersionRubriqueBudgetaireRepository.class)
              .all()
              .filter("self.annee = :year")
              .bind("year", year)
              .fetchOne();
      if (versionRubriqueBudgetaire == null) {
        response.setFlash("Aucun budget pour annee " + year);
        return;
      }

      VersionRB version_rb =
          versionRubriqueBudgetaire.getVersionRubriques().stream()
              .filter(VersionRB::getIs_versionFinale)
              .findFirst()
              .orElse(null);

      if (version_rb == null) {
        response.setFlash("Aucun budget n'est validée pour année" + year);
        return;
      }

      List<Long> ids =
          Beans.get(RubriquesBudgetaireRepository.class).all()
              .filter("self.id_version = :ids_version").bind("ids_version", version_rb.getId())
              .fetch().stream()
              .map(RubriquesBudgetaire::getId_rubrique_generale)
              .collect(Collectors.toList());
      for (String name : names) {
        response.setAttr(
            name,
            "domain",
            "self.id in ("
                + ids.stream().map(Object::toString).collect(Collectors.joining(","))
                + ")");
      }

    } catch (Exception ex) {
      response.setFlash(ex.getMessage());
      return;
    }
  }

  public void tw_modifierEngagementAchat(ActionRequest request, ActionResponse response) {
    OrdrevirementCommande ordrevirementCommande =
        request.getContext().asType(OrdrevirementCommande.class);
    ordrevirementCommande =
        Beans.get(OrdrevirementCommandeRepository.class).find(ordrevirementCommande.getId());
    List<SelectOP> ls = ordrevirementCommande.getSelectOP();
    for (SelectOP select : ls) {
      EngagementAchat engagementAchat =
          Beans.get(EngagementAchatRepository.class)
              .all()
              .filter("self.natureOperation=:nature and self.id_reference_nature=:id_ref")
              .bind("nature", select.getOrderPaymentCommande().getNature_operation())
              .bind("id_ref", select.getOrderPaymentCommande().getId_reference())
              .fetchOne();
      if (engagementAchat != null) {
        engagementAchat.setEtatEngagement("Payé");
        appPurchaseService.save_EngagementAchat(engagementAchat);
      }
    }
  }

  public void tw_check_data_kouidi(ActionRequest request, ActionResponse response) {
    request.getContext().put("type_aug", request.getContext().getParent().get("type_aug"));
  }

  public void tw_load_etat(ActionRequest request, ActionResponse response) {
    Long id_offre = (Long) request.getContext().get("id");
    OffreAppel o = Beans.get(OffreAppelRepository.class).find(id_offre);
    List<Soumissionnaire> ls =
        o.getSoumissionnaire().stream()
            .filter(
                soumissionnaire -> {
                  if ((soumissionnaire.getEtat() == null
                          || !soumissionnaire.getEtat().equals("ecarter"))
                      && soumissionnaire.getDossierAdministrative()) return true;
                  return false;
                })
            .collect(Collectors.toList());
    response.setValue("soumissionnaire", ls);
  }

  public void action_rafraichir_date(ActionRequest request, ActionResponse response) {
    LocalDate date = LocalDate.now();
    String limit =
        "update purchase_offre_appel o set nbr_jour_limit = DATE_PART('day', AGE(cast('"
            + date
            + "' as date), o.date_document)) where o.date_document is not null";
    appPurchaseService.updateDateDocument(limit);
  }

  public void update_livrable(ActionRequest request, ActionResponse response) {
    Long id_p = (Long) request.getContext().get("id");
    LivrableP o = Beans.get(LivrablePRepository.class).find(id_p);
    appPurchaseService.saveLivrableP(o);
  }

  public void tw_create_engagement_rh(ActionRequest request, ActionResponse response) {
    Integer mois = Integer.valueOf(request.getContext().get("mois").toString());
    Integer year = Integer.valueOf(request.getContext().get("year").toString());
    EtatSalaire_ops etatSalaire_ops =
        Beans.get(EtatSalaire_opsRepository.class)
            .all()
            .filter("self.year=:year and self.mois =:mois")
            .bind("year", year)
            .bind("mois", mois)
            .fetchOne();
    List<OrderPaymentCommande> ls =
        Beans.get(OrderPaymentCommandeRepository.class)
            .all()
            .filter(
                "self.is_rh_module is true and self.nature_operation=:nature and self.id_reference =:id ")
            .bind("nature", "EtatSalaireOps")
            .bind("id", etatSalaire_ops.getId())
            .fetch();

    for (OrderPaymentCommande tmp : ls) {
      EngagementAchat eng =
          Beans.get(EngagementAchatRepository.class)
              .all()
              .filter("self.natureOperation=:nature and self.id_reference_nature=:id_ref")
              .bind("nature", "EtatSalaireOps")
              .bind("id_ref", tmp.getId())
              .fetchOne();

      if (eng != null) {
        eng.setMontantEngagement(tmp.getSommeVerement());
        eng.setDateEngagment(LocalDate.now());
        eng.setBudget(
            Beans.get(RubriqueBudgetaireGeneraleRepository.class)
                .find(tmp.getRubriqueBudgetaire().getId()));
      } else {
        eng = new EngagementAchat();
        eng.setMontantEngagement(tmp.getSommeVerement());
        eng.setEtatEngagement("Non Payé");
        eng.setNatureOperation("EtatSalaireOps");
        eng.setId_reference_nature(tmp.getId());
        eng.setDateEngagment(LocalDate.now());
        eng.setBudget(
            Beans.get(RubriqueBudgetaireGeneraleRepository.class)
                .find(tmp.getRubriqueBudgetaire().getId()));
      }
      eng = Beans.get(AppPurchaseService.class).save_EngagementAchat(eng);
    }
  }
}
