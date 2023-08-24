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
package com.axelor.apps.account.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.account.db.Encaissement;
import com.axelor.apps.account.db.GestionRecette;
import com.axelor.apps.account.report.IReport;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.debtrecovery.PayerQualityService;
import com.axelor.apps.configuration.db.AnnexeGenerale;
import com.axelor.apps.configuration.db.Compte;
import com.axelor.apps.configuration.service.ConvertNomreToLettres;
import com.axelor.apps.configuration.service.RunSqlRequestForMe;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

@Singleton
public class AppAccountController {

  @Inject AppAccountService appService;

  public void payerQualityProcess(ActionRequest request, ActionResponse response) {

    try {
      Beans.get(PayerQualityService.class).payerQualityProcess();
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void generateAccountConfigurations(ActionRequest request, ActionResponse response) {

    Beans.get(AppAccountService.class).generateAccountConfigurations();

    response.setReload(true);
  }

  public void imprimerGestionRegie(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id_OR = (Long) request.getContext().get("id");
    GestionRecette c = appService.getRecetteById(id_OR);
    if (id_OR <= 0) {
      response.setFlash("l'id d'ordre de recette n'est pas valide");
      return;
    }
    String fileLink =
        ReportFactory.createReport(IReport.OrdreRecette, "Ordre de Recette")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id_OR", id_OR)
            .addParam("compteString", c.getRib().getRib())
            .addParam(
                "partieVersante",
                c.getPartierversement().getLibelle() != null
                    ? c.getPartierversement().getLibelle()
                    : "-")
            .addParam(
                "nature",
                c.getNatureoperation().getNom() != null ? c.getNatureoperation().getNom() : "-")
            .addParam("montant", c.getMontant() != null ? c.getMontant() : "0.00")
            .addParam(
                "montantLettre", c.getMontantenlettre() != null ? c.getMontantenlettre() : "-")
            .addParam(
                "rubrique",
                c.getRubrique().getCodeBudg() != null ? c.getRubrique().getCodeBudg() : "-")
            .addParam("pieceJointe", c.getPiecesJointes() != null ? c.getPiecesJointes() : "-")
            .addParam(
                "nomRubrique", c.getRubrique().getName() != null ? c.getRubrique().getName() : "-")
            .addParam("annee", c.getAnneeRecette() != null ? c.getAnneeRecette() : "-")
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de Recette").add("html", fileLink).map());
  }

  public void imprimerTresorerie(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer annee = (Integer) request.getContext().get("anneeHistorique");
    Compte compte = (Compte) request.getContext().get("compte");

    Integer anneeP = (Integer) request.getContext().get("anneeHistorique");
    int a = anneeP - 1;
    String anneePrecedente = String.valueOf(a);

    String lastMonth = "31/12/" + anneePrecedente.substring(anneePrecedente.length() - 2);
    Long idCompte = compte.getId();
    String ribCompte = compte.getRib();

    String fileLink =
        ReportFactory.createReport(IReport.TRESORERIE, "Trésorerie")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("compte", idCompte)
            .addParam("anneePrecedente", anneePrecedente)
            .addParam("compteString", ribCompte)
            .addParam("lastMonth", lastMonth)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Trésorerie").add("html", fileLink).map());
  }

  public void imprimerEncaissementParAnnee(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String etat = String.valueOf(request.getContext().get("etat").toString());
    Integer annee = (Integer) request.getContext().get("annee");
    AnnexeGenerale a = appService.getDefaultAnnexeCentrale();
    User s = AuthUtils.getUser();
    int paramDate = -1;
    Date date1 = Date.valueOf(LocalDate.now());
    Date date2 = Date.valueOf(LocalDate.now());
    if (request.getContext().get("dateDebut") != null
        && request.getContext().get("dateFin") != null) {
      paramDate = 1;
      date1 = java.sql.Date.valueOf(request.getContext().get("dateDebut").toString());
      date2 = java.sql.Date.valueOf(request.getContext().get("dateFin").toString());
    }
    String choix = "all";
    int paramAnnexe = -1;
    if (request.getContext().get("choixAnnexe") != null) {
      choix = request.getContext().get("choixAnnexe").toString();
      paramAnnexe = choix.equals("all") ? -1 : 1;
    }
    Long id_annexe_choix = 0l;
    if (!choix.equals("all") && request.getContext().get("annexe") != null) {
      AnnexeGenerale annexeGenerale = (AnnexeGenerale) request.getContext().get("annexe");
      id_annexe_choix = annexeGenerale.getId();
    }

    if (request.getContext().get("annexeg") != null) {
      ObjectMapper mapper = new ObjectMapper();
      AnnexeGenerale annexe =
          mapper.convertValue(
              request.getContext().get("annexeg"), new TypeReference<AnnexeGenerale>() {});
      a = appService.getAnnexeById(annexe.getId());
    }

    if (Objects.equals(etat, "3")) {
      Integer mois = Integer.valueOf(request.getContext().get("mois").toString());

      BigDecimal mt1 =
          RunSqlRequestForMe.runSqlRequest_Bigdecimal(
              "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                  + annee
                  + " and DATE_PART('month', es.date_recette)="
                  + mois
                  + " and es.annexe="
                  + a.getId()
                  + ";");
      BigDecimal somme1 = mt1 != null ? (BigDecimal) mt1 : BigDecimal.ZERO;

      BigDecimal sommecorrect = BigDecimal.ZERO;
      BigDecimal sommeTmp = BigDecimal.ZERO;
      if (s.getAllencaissement()) {
        if (paramAnnexe == 1 && paramDate == -1) {
          // annexe seulement
          BigDecimal mtAnnexe =
              RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                  "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                      + annee
                      + " and DATE_PART('month', es.date_recette)="
                      + mois
                      + " and es.annexe="
                      + id_annexe_choix
                      + ";");
          sommeTmp = mtAnnexe != null ? (BigDecimal) mtAnnexe : BigDecimal.ZERO;
        } else if (paramAnnexe == -1 && paramDate == 1) {
          // par date seuelent
          BigDecimal mtDate =
              RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                  "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                      + annee
                      + " and DATE_PART('month', es.date_recette)="
                      + mois
                      + " and es.date_recette between '"
                      + date1
                      + "' and '"
                      + date2
                      + "';");
          sommeTmp = mtDate != null ? (BigDecimal) mtDate : BigDecimal.ZERO;
        } else if (paramAnnexe == 1 && paramDate == 1) {
          // par date et par annexe
          BigDecimal mtTout =
              RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                  "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                      + annee
                      + " and DATE_PART('month', es.date_recette)="
                      + mois
                      + " and es.date_recette between '"
                      + date1
                      + "' and '"
                      + date2
                      + "' and es.annexe="
                      + id_annexe_choix
                      + ";");
          sommeTmp = mtTout != null ? (BigDecimal) mtTout : BigDecimal.ZERO;
        } else if (paramAnnexe == -1 && paramDate == -1) {
          BigDecimal mt2 =
              RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                  "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                      + annee
                      + " and DATE_PART('month', es.date_recette)="
                      + mois);
          sommeTmp = mt2 != null ? (BigDecimal) mt2 : BigDecimal.ZERO;
        }
        sommecorrect = sommeTmp;
      } else {
        sommecorrect = somme1;
      }
      String montant2 = ConvertNomreToLettres.getStringMontant(sommecorrect);
      imprimerEtatMensuel(
          id_annexe_choix,
          paramAnnexe,
          paramDate,
          date1,
          date2,
          a.getId(),
          a.getName(),
          montant2,
          sommecorrect,
          annee,
          mois,
          s.getAllencaissement(),
          response);
    } else if (Objects.equals(etat, "4")) {
      imprimerEtatAnnuel(
          id_annexe_choix,
          paramAnnexe,
          paramDate,
          date1,
          date2,
          a.getId(),
          a.getName(),
          annee,
          s.getAllencaissement(),
          response);
    } else if (Objects.equals(etat, "5")) {
      imprimerEtatMoyenne(
          id_annexe_choix,
          paramAnnexe,
          paramDate,
          date1,
          date2,
          a.getId(),
          a.getName(),
          annee,
          s.getAllencaissement(),
          response);
    } else if (Objects.equals(etat, "6")) {
      imprimerEtatStatistique(
          id_annexe_choix,
          paramAnnexe,
          paramDate,
          date1,
          date2,
          a.getId(),
          annee,
          s.getAllencaissement(),
          response);
    }
  }

  public void imprimerEtatMensuel(
      Long id_annexe_choix,
      int paramAnnexe,
      int paramDate,
      Date date1,
      Date date2,
      Long id_annexe,
      String name,
      String montant,
      BigDecimal somme,
      Integer annee,
      Integer mois,
      boolean allencaissement,
      ActionResponse response)
      throws AxelorException {
    String fileLink =
        ReportFactory.createReport(IReport.EtatEncaissementMensuel, "Etat d'encaissement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("montantLettre", montant)
            .addParam("somme", somme)
            .addParam("annee", annee)
            .addParam("mois", mois)
            .addParam("annexe", id_annexe)
            .addParam("allencaissement", allencaissement ? 1 : 0)
            .addParam("name", name)
            .addParam("paramDate", paramDate)
            .addParam("paramAnnexe", paramAnnexe)
            .addParam("date1", date1)
            .addParam("date2", date2)
            .addParam("id_annexe_choix", id_annexe_choix)
            .addParam("moisString", Month.of(mois).getDisplayName(TextStyle.FULL, Locale.FRANCE))
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat mensuel").add("html", fileLink).map());
  }

  public void imprimerEtatStatistique(
      Long id_annexe_choix,
      int paramAnnexe,
      int paramDate,
      Date date1,
      Date date2,
      Long id_annexe,
      Integer annee,
      boolean allencaissement,
      ActionResponse response)
      throws AxelorException {
    String fileLink =
        ReportFactory.createReport(IReport.StPrestation, "Statistique")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("annexe", id_annexe)
            .addParam("allencaissement", allencaissement ? 1 : 0)
            .addParam("paramDate", paramDate)
            .addParam("paramAnnexe", paramAnnexe)
            .addParam("date1", date1)
            .addParam("date2", date2)
            .addParam("id_annexe_choix", id_annexe_choix)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Statistique").add("html", fileLink).map());
  }

  public void imprimerEtatMoyenne(
      Long id_annexe_choix,
      int paramAnnexe,
      int paramDate,
      Date date1,
      Date date2,
      Long id_annexe,
      String name,
      Integer annee,
      boolean allencaissement,
      ActionResponse response)
      throws AxelorException {
    String fileLink =
        ReportFactory.createReport(IReport.EtatEncaissementAnnuelMoyenne, "Etat d'encaissement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("annexe", id_annexe)
            .addParam("name", name)
            .addParam("allencaissement", allencaissement ? 1 : 0)
            .addParam("paramDate", paramDate)
            .addParam("paramAnnexe", paramAnnexe)
            .addParam("date1", date1)
            .addParam("date2", date2)
            .addParam("id_annexe_choix", id_annexe_choix)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Moyenne annuel").add("html", fileLink).map());
  }

  public void imprimerEtatAnnuel(
      Long id_annexe_choix,
      int paramAnnexe,
      int paramDate,
      Date date1,
      Date date2,
      Long id_annexe,
      String name,
      Integer annee,
      boolean allencaissement,
      ActionResponse response)
      throws AxelorException {
    String fileLink =
        ReportFactory.createReport(IReport.EtatEncaissementAnnuel, "Etat d'encaissement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("annee", annee)
            .addParam("annexe", id_annexe)
            .addParam("name", name)
            .addParam("allencaissement", allencaissement ? 1 : 0)
            .addParam("paramDate", paramDate)
            .addParam("paramAnnexe", paramAnnexe)
            .addParam("date1", date1)
            .addParam("date2", date2)
            .addParam("id_annexe_choix", id_annexe_choix)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat annuel").add("html", fileLink).map());
  }

  public void checkMontant(ActionRequest request, ActionResponse response) {
    String dateDebut = request.getContext().get("dateDebut").toString();
    String dateFin = request.getContext().get("dateFin").toString();
    LocalDate d1 = LocalDate.parse(dateDebut);
    LocalDate d2 = LocalDate.parse(dateFin);
    BigDecimal d = appService.checkMontant(d1, d2);
    if (d != null) {
      response.setValue("somme", "Le total des montants encaisser est: " + d);
    } else {
      response.setValue("somme", "Aucun montant encaisser");
    }
    response.setHidden("somme", false);
  }

  public void annulerEncaissement(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    String commentaire = request.getContext().get("commentaire").toString();
    appService.AnnulerEncaissement(id, commentaire);
    response.setReload(true);
  }

  public void afficherAnnulerEncaissement(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    Encaissement ec = appService.getEncaissementById(id);
    response.setView(
        ActionView.define("Annuler encaissement")
            .model(Encaissement.class.getName())
            .add("form", "encaissement-annuler-form")
            .param("search-filters", "partner-filters")
            .context("_showRecord", ec.getId())
            .map());
  }

  public void afficherEncaissement(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    Encaissement ec = appService.getEncaissementById(id);
    response.setView(
        ActionView.define("Modifier encaissement")
            .model(Encaissement.class.getName())
            .add("form", "encaissement-form")
            .param("search-filters", "partner-filters")
            .context("_showRecord", ec.getId())
            .map());
  }

  public void imprimerVersement(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String etat = String.valueOf(request.getContext().get("etat").toString());
    Integer annee = (Integer) request.getContext().get("annee");
    AnnexeGenerale a = appService.getDefaultAnnexeCentrale();
    User s = AuthUtils.getUser();
    int paramDate = -1;
    Date date1 = Date.valueOf(LocalDate.now());
    Date date2 = Date.valueOf(LocalDate.now());
    if (request.getContext().get("dateDebut") != null
        && request.getContext().get("dateFin") != null) {
      paramDate = 1;
      date1 = java.sql.Date.valueOf(request.getContext().get("dateDebut").toString());
      date2 = java.sql.Date.valueOf(request.getContext().get("dateFin").toString());
    }
    String choix = "all";
    int paramAnnexe = -1;
    if (request.getContext().get("choixAnnexe") != null) {
      choix = request.getContext().get("choixAnnexe").toString();
      paramAnnexe = choix.equals("all") ? -1 : 1;
    }
    Long id_annexe_choix = 0l;
    if (!choix.equals("all") && request.getContext().get("annexe") != null) {
      AnnexeGenerale annexeGenerale = (AnnexeGenerale) request.getContext().get("annexe");
      id_annexe_choix = annexeGenerale.getId();
    }

    if (request.getContext().get("annexeg") != null) {
      ObjectMapper mapper = new ObjectMapper();
      AnnexeGenerale annexe =
          mapper.convertValue(
              request.getContext().get("annexeg"), new TypeReference<AnnexeGenerale>() {});
      a = appService.getAnnexeById(annexe.getId());
    }

    if (Objects.equals(etat, "1")) {
      Integer mois = Integer.valueOf(request.getContext().get("mois").toString());

      BigDecimal mt1 =
          RunSqlRequestForMe.runSqlRequest_Bigdecimal(
              "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                  + annee
                  + " and DATE_PART('month', es.date_recette)="
                  + mois
                  + " and es.annexe="
                  + a.getId()
                  + ";");
      BigDecimal somme1 = mt1 != null ? (BigDecimal) mt1 : BigDecimal.ZERO;

      BigDecimal sommecorrect = BigDecimal.ZERO;
      BigDecimal sommeTmp = BigDecimal.ZERO;
      if (s.getAllencaissement()) {
        if (paramAnnexe == 1 && paramDate == -1) {
          // annexe seulement
          BigDecimal mtAnnexe =
              RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                  "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                      + annee
                      + " and DATE_PART('month', es.date_recette)="
                      + mois
                      + " and es.annexe="
                      + id_annexe_choix
                      + ";");
          sommeTmp = mtAnnexe != null ? (BigDecimal) mtAnnexe : BigDecimal.ZERO;
        } else if (paramAnnexe == -1 && paramDate == 1) {
          // par date seuelent
          BigDecimal mtDate =
              RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                  "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                      + annee
                      + " and DATE_PART('month', es.date_recette)="
                      + mois
                      + " and es.date_recette between '"
                      + date1
                      + "' and '"
                      + date2
                      + "';");
          sommeTmp = mtDate != null ? (BigDecimal) mtDate : BigDecimal.ZERO;
        } else if (paramAnnexe == 1 && paramDate == 1) {
          // par date et par annexe
          BigDecimal mtTout =
              RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                  "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                      + annee
                      + " and DATE_PART('month', es.date_recette)="
                      + mois
                      + " and es.date_recette between '"
                      + date1
                      + "' and '"
                      + date2
                      + "' and es.annexe="
                      + id_annexe_choix
                      + ";");
          sommeTmp = mtTout != null ? (BigDecimal) mtTout : BigDecimal.ZERO;
        } else if (paramAnnexe == -1 && paramDate == -1) {
          BigDecimal mt2 =
              RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                  "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                      + annee
                      + " and DATE_PART('month', es.date_recette)="
                      + mois);
          sommeTmp = mt2 != null ? (BigDecimal) mt2 : BigDecimal.ZERO;
        }
        sommecorrect = sommeTmp;
      } else {
        sommecorrect = somme1;
      }
      String montant2 = ConvertNomreToLettres.getStringMontant(sommecorrect);
      String formattedDate = String.valueOf(annee).substring(2);
      imprimerOrdreVersement(
          id_annexe_choix,
          paramAnnexe,
          paramDate,
          date1,
          date2,
          a.getId(),
          a.getName(),
          montant2,
          sommecorrect,
          annee,
          mois,
          formattedDate,
          s.getAllencaissement(),
          response);
    } else {
      Integer mois = Integer.valueOf(request.getContext().get("mois").toString());
      BigDecimal mt1 =
          RunSqlRequestForMe.runSqlRequest_Bigdecimal(
              "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                  + annee
                  + " and DATE_PART('month', es.date_recette)="
                  + mois
                  + " and es.annexe="
                  + a.getId()
                  + ";");
      BigDecimal somme1 = mt1 != null ? (BigDecimal) mt1 : BigDecimal.ZERO;

      BigDecimal sommecorrect = BigDecimal.ZERO;
      BigDecimal sommeTmp = BigDecimal.ZERO;
      if (s.getAllencaissement()) {
        if (paramAnnexe == 1 && paramDate == -1) {
          // annexe seulement
          BigDecimal mtAnnexe =
              RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                  "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                      + annee
                      + " and DATE_PART('month', es.date_recette)="
                      + mois
                      + " and es.annexe="
                      + id_annexe_choix
                      + ";");
          sommeTmp = mtAnnexe != null ? (BigDecimal) mtAnnexe : BigDecimal.ZERO;
        } else if (paramAnnexe == -1 && paramDate == 1) {
          // par date seuelent
          BigDecimal mtDate =
              RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                  "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                      + annee
                      + " and DATE_PART('month', es.date_recette)="
                      + mois
                      + " and es.date_recette between '"
                      + date1
                      + "' and '"
                      + date2
                      + "';");
          sommeTmp = mtDate != null ? (BigDecimal) mtDate : BigDecimal.ZERO;
        } else if (paramAnnexe == 1 && paramDate == 1) {
          // par date et par annexe
          BigDecimal mtTout =
              RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                  "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                      + annee
                      + " and DATE_PART('month', es.date_recette)="
                      + mois
                      + " and es.date_recette between '"
                      + date1
                      + "' and '"
                      + date2
                      + "' and es.annexe="
                      + id_annexe_choix
                      + ";");
          sommeTmp = mtTout != null ? (BigDecimal) mtTout : BigDecimal.ZERO;
        } else if (paramAnnexe == -1 && paramDate == -1) {
          BigDecimal mt2 =
              RunSqlRequestForMe.runSqlRequest_Bigdecimal(
                  "select sum(es.montant) as total from account_gestion_recette es where DATE_PART('year', es.date_recette)="
                      + annee
                      + " and DATE_PART('month', es.date_recette)="
                      + mois);
          sommeTmp = mt2 != null ? (BigDecimal) mt2 : BigDecimal.ZERO;
        }
        sommecorrect = sommeTmp;
      } else {
        sommecorrect = somme1;
      }
      String montant2 = ConvertNomreToLettres.getStringMontant(sommecorrect);
      String formattedDate = String.valueOf(annee).substring(2);
      imprimerReleveDesProduits(
          id_annexe_choix,
          paramAnnexe,
          paramDate,
          date1,
          date2,
          a.getId(),
          a.getName(),
          montant2,
          sommecorrect,
          annee,
          mois,
          formattedDate,
          s.getAllencaissement(),
          response);
    }
  }

  public void imprimerOrdreVersement(
      Long id_annexe_choix,
      int paramAnnexe,
      int paramDate,
      Date date1,
      Date date2,
      Long id_annexe,
      String name,
      String montant,
      BigDecimal somme,
      Integer annee,
      Integer mois,
      String formattedDate,
      boolean allencaissement,
      ActionResponse response)
      throws AxelorException {
    String fileLink =
        ReportFactory.createReport(IReport.OrdreDeVersement, "Ordre de versement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("montantLettre", montant)
            .addParam("somme", somme)
            .addParam("annee", annee)
            .addParam("mois", mois)
            .addParam("ans", formattedDate)
            .addParam("annexe", id_annexe)
            .addParam("allencaissement", allencaissement ? 1 : 0)
            .addParam("name", name)
            .addParam("paramDate", paramDate)
            .addParam("paramAnnexe", paramAnnexe)
            .addParam("date1", date1)
            .addParam("date2", date2)
            .addParam("id_annexe_choix", id_annexe_choix)
            .addParam("moisString", Month.of(mois).getDisplayName(TextStyle.FULL, Locale.FRANCE))
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de versement").add("html", fileLink).map());
  }

  public void imprimerReleveDesProduits(
      Long id_annexe_choix,
      int paramAnnexe,
      int paramDate,
      Date date1,
      Date date2,
      Long id_annexe,
      String name,
      String montant,
      BigDecimal somme,
      Integer annee,
      Integer mois,
      String formattedDate,
      boolean allencaissement,
      ActionResponse response)
      throws AxelorException {
    String fileLink =
        ReportFactory.createReport(IReport.ReleveDesProduits, "Relevé des produits non courants")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("montantLettre", montant)
            .addParam("somme", somme)
            .addParam("annee", annee)
            .addParam("mois", mois)
            .addParam("ans", formattedDate)
            .addParam("annexe", id_annexe)
            .addParam("allencaissement", allencaissement ? 1 : 0)
            .addParam("name", name)
            .addParam("paramDate", paramDate)
            .addParam("paramAnnexe", paramAnnexe)
            .addParam("date1", date1)
            .addParam("date2", date2)
            .addParam("id_annexe_choix", id_annexe_choix)
            .addParam("moisString", Month.of(mois).getDisplayName(TextStyle.FULL, Locale.FRANCE))
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("Relevé des produits non courants").add("html", fileLink).map());
  }
}
