package com.axelor.apps.configuration.web;

import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.repo.BordereauRepository;
import com.axelor.apps.configuration.db.repo.GestionSalaireRepository;
import com.axelor.apps.configuration.db.repo.VersionRBRepository;
import com.axelor.apps.configuration.db.repo.VersionRubriqueBudgetaireRepository;
import com.axelor.apps.configuration.service.MyConfigurationServiceImpl;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class AppConfigurationController {

  @Inject MyConfigurationServiceImpl configurationService;

  public void afficheMessage(ActionRequest request, ActionResponse response) {
    boolean msg = false;
    String message = "<p>Les champs suivants sont obligatoire:</p><ul>";
    Map<String, Object> _maps = request.getData();
    Map<String, Object> maps = (Map<String, Object>) _maps.get("context");
    String designation = (String) maps.get("designation"); // NULL
    if (designation == null) {
      message += "<li>Désignation</li>";
      if (!msg) msg = true;
    }
    String rib = (String) maps.get("rib"); // null
    if (rib == null) {
      message += "<li>Rib</li>";
      if (!msg) msg = true;
    }
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
  }

  public void EnregistrerResponsabilite(ActionRequest request, ActionResponse response) {
    configurationService.save_new_Responsabilite(request, response);
    response.setReload(true);
    response.setCanClose(true);
  }

  public void update_Responsabilite(ActionRequest request, ActionResponse response) {
    configurationService.update_Responsabilite(request, response);
    response.setReload(true);
    response.setCanClose(true);
  }

  public void updateHistoriqueBudgetaire(ActionRequest request, ActionResponse response) {
    HistoriqueBudgetaire hist = null;
    Long id_budgparRub = (Long) request.getContext().get("id");
    BudgetParRubrique budgetParRubrique =
        configurationService.getBudgetParRubriqueById(id_budgparRub);
    if (budgetParRubrique.getUpdatedOn() == null) {
      // creation
      hist = new HistoriqueBudgetaire();
      hist.setRubriqueBudgetaire(budgetParRubrique.getRubriqueBudgetaire());
      hist.setMontant(new BigDecimal(0));
      hist.setTypeOperation("credit");
      hist.setMontantRubrique(budgetParRubrique.getMontant());
      hist.setAnnee(budgetParRubrique.getYear());
      hist.setDateEx(LocalDate.now());
      hist.setMois(LocalDate.now().getMonthValue());
      hist = configurationService.saveHistoriqueBudgetaire(hist);
      budgetParRubrique.setHistoriqueBudgetaire(hist);
      configurationService.saveBudgetParRubrique(budgetParRubrique);
    } else {
      // modification
      Long id = budgetParRubrique.getHistoriqueBudgetaire().getId();
      BigDecimal m = budgetParRubrique.getMontant();
      configurationService.updatehistoriqueBudgetaire2(id, m);
    }
  }

  public void show_other_field(ActionRequest request, ActionResponse response) {
    boolean exonerer = (boolean) request.getContext().get("exonerer");
    if (exonerer) {
      response.setHidden("montant_red", true);
      response.setHidden("pors", true);
    } else {
      response.setHidden("montant_red", false);
      response.setHidden("pors", false);
    }
  }

  public void savedataGestionSalaire(ActionRequest request, ActionResponse response) {
    GestionSalaire t = request.getContext().asType(GestionSalaire.class);
    GestionSalaire persist = new GestionSalaire();
    if (t.getId() != null) {
      persist = configurationService.getGestionSalaireById(t.getId());
    }
    persist.setCorps(t.getCorps());
    persist.setEchelle(t.getEchelle());
    persist.setEchelon(t.getEchelon());
    persist.setGrade(t.getGrade());
    persist.setIndice(t.getIndice());
    persist.setTraitementDeBase(t.getTraitementDeBase());
    persist.setIndemniteDeResidenceZC10(t.getIndemniteDeResidenceZC10());
    persist.setIndemniteDeResidenceZB15(t.getIndemniteDeResidenceZB15());
    persist.setIndemniteDeResidenceZA25(t.getIndemniteDeResidenceZA25());
    persist.setIndemniteDeHierarchieAdministrative(t.getIndemniteDeHierarchieAdministrative());
    persist.setIndemniteDeSujetion(t.getIndemniteDeSujetion());
    persist.setIndemniteEncadrement(t.getIndemniteEncadrement());
    persist.setIndemniteDeTechnicite(t.getIndemniteDeTechnicite());
    configurationService.saveGestionsalire(persist);
  }

  public void checkDoublon(ActionRequest request, ActionResponse response) {
    if (request.getContext().get("echelon_start") == null
        || request.getContext().get("echelon_end") == null
        || request.getContext().get("nbr_mois_rapide") == null
        || request.getContext().get("nbr_mois_moyen") == null
        || request.getContext().get("nbr_mois_long") == null) {
      response.setValue("etatSave", false);
      response.setFlash("Un ou plusieurs champs sont vides");
      return;
    }
    String ech_start = request.getContext().get("echelon_start").toString();
    String ech_end = request.getContext().get("echelon_end").toString();
    int nbr = configurationService.getAvancementByEchelonStartEnd(ech_start, ech_end);
    if (nbr > 0) {
      response.setValue("etatSave", false);
      response.setFlash(
          "il existe un avancement avec les même chelon-début et le même echelon-fin ");
      return;
    }
    if (nbr == 0) {
      response.setValue("etatSave", true);
    }
  }

  public void calucle_some_total_rubrique(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    RubriquesBudgetaire r = configurationService.getRubriqueBudgetaire(id);
    List<Long> ls = configurationService.getListOfAllParents(id);
    if (ls.size() > 0) {
      configurationService.modifierMontantParent(id, ls);
      configurationService.updateOldMontant(id);
    }
    if (r.getId_rubrique_generale() == null || r.getId_rubrique_generale() == 0l) {
      Map<String, Long> created_ids = configurationService.addBudgetParRubriqueGenerale(id);
      r.setId_rubrique_generale(created_ids.get("rubrique"));
      r.setId_budget_par_rubrique_generale(created_ids.get("budget"));
      configurationService.saveRubriqueBudgetaire(r);
    } else {
      configurationService.updateRubriqueBudgetaireGeneraleAndBudget(r.getId());
    }
  }

  public void tw_updateAllTotal(ActionRequest request, ActionResponse response) {
    Integer year = (Integer) request.getContext().get("_$date_anne");
    if (year == null || year == 0) {
      year = LocalDate.now().getYear();
    }
    configurationService.updateAllTotalRubriqueBudgetaire(year);
    response.setView(
        ActionView.define("Afficher les Budget")
            .model(RubriquesBudgetaire.class.getName())
            .add("form", "formRubriquesBudgetaireAffichage")
            .map());
  }

  public void tw_delete_rubriqueBudgitaire(ActionRequest request, ActionResponse response) {
    Long id_r = (Long) request.getContext().get("id");
    RubriquesBudgetaire r = configurationService.getRubriqueBudgetaire(id_r);
    List<RubriquesBudgetaire> ls = configurationService.getAllChildrenBudgetById(id_r);
    if (ls.isEmpty()) {
      List<Long> ls_rub = configurationService.getListOfAllParents(id_r);
      if (!ls_rub.isEmpty()) {
        configurationService.reduireRubriqueBudgetaire(r.getId());
        configurationService.reduireMontantBudget(r.getId());
      }
      configurationService.removeRubriqueBudgetaireById(r.getId());
      response.setReload(true);

    } else {
      String msg =
          "<p>Attentions ce budget possède des sous budgets avec les codes suivant : </p><ul>";
      for (RubriquesBudgetaire tmp : ls) {
        msg += "<li>" + tmp.getCode_budget() + "</li>";
      }
      msg += "</ul>";
      response.setFlash(msg);
      return;
    }
  }

  public void sum_total_generale(ActionRequest request, ActionResponse response) {
    int anne = (int) request.getContext().get("date_anne");
    // tresorerie
    BigDecimal some1 = configurationService.getSomme(6l, anne);
    // produit exploitation
    BigDecimal some2 = configurationService.getSomme(7l, anne);
    // ressource d'invesstissement
    BigDecimal some3 = configurationService.getSomme(8l, anne);
    // Charge exploitation
    BigDecimal some4 = configurationService.getSomme(9l, anne);
    // Emploi investissement
    BigDecimal some5 = configurationService.getSomme(23l, anne);
    // ressource d'invesstissement
    BigDecimal some6 = configurationService.getSomme(11l, anne);
    response.setValue("$recette_som1", some1);
    response.setValue("$recette_som2", some2);
    response.setValue("$recette_som3", some3);
    response.setValue("$recette_som4", some4);
    response.setValue("$recette_som5", some5);
    response.setValue("$recette_som6", some6);
    response.setValue("$total_total_recette", some1.add(some2).add(some3));
    response.setValue("$total_total_depense", some4.add(some5).add(some6));
  }

  public void redirect_to_detail(ActionRequest request, ActionResponse response) {
    Integer anne = (Integer) request.getContext().get("date_anne");
    if (anne == null || anne == 0) {
      response.setFlash("L'anné n'est pas saisie");
      return;
    }
    response.setCanClose(true);
    String domaine = "self.code_budget like '7%' and self.anneCurrent = " + anne;
    response.setView(
        ActionView.define("Détail Rubrique")
            .model(RubriquesBudgetaire.class.getName())
            .add("form", "tw_detail_ProduitExploitation")
            .param("popup", "false")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .context("fieldanne", anne)
            .domain(domaine)
            .map());
  }

  public void redirect_to_detail_single(ActionRequest request, ActionResponse response) {
    Integer anne = (Integer) request.getContext().get("date_anne");
    Long version_id = (Long) request.getContext().get("id_version");
    if (anne == null || anne == 0) {
      response.setFlash("L'anné n'est pas saisie");
      return;
    }
    response.setCanClose(true);
    response.setView(
        ActionView.define("Détail Rubrique")
            .model(RubriquesBudgetaire.class.getName())
            .add("form", "tw_detail_ProduitExploitationFromVersion")
            .param("popup", "false")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .context("fieldanne", anne)
            .context("id_version", version_id)
            .map());
  }

  public void calcule_tot_rubrique_detail_1(ActionRequest request, ActionResponse response) {
    Integer anne = (Integer) request.getContext().get("fieldanne");
    Long version = (Long) request.getContext().get("id_version");
    BigDecimal mt1 = configurationService.getSommeDetail(12l, anne, version);
    BigDecimal mt2 = configurationService.getSommeDetail(13l, anne, version);
    response.setValue("$recette_som1", mt1);
    response.setValue("$recette_som2", mt2);
    response.setValue("$total_total_recette", mt1.add(mt2));
  }

  public void calcule_tot_rubrique_detail_1_version(
      ActionRequest request, ActionResponse response) {
    Integer anne = (Integer) request.getContext().get("fieldanne");
    Long version = (Long) request.getContext().get("id_version");
    BigDecimal mt1 = configurationService.getSommeDetail_version(11l, anne, version);
    BigDecimal mt2 = configurationService.getSommeDetail_version(12l, anne, version);
    response.setValue("$recette_som1_version", mt1);
    response.setValue("$recette_som2_version", mt2);
    response.setValue("$total_total_recette_version", mt1.add(mt2));
  }

  public void redirect_to_detail_depense(ActionRequest request, ActionResponse response) {
    Integer annee = (Integer) request.getContext().get("date_anne");
    if (annee == null || annee == 0) {
      response.setFlash("L'anné n'est pas saisie");
      return;
    }
    String name = (String) request.getContext().get("_signal");
    response.setCanClose(true);
    List<RubriquesBudgetaire> ls =
        configurationService.getDetailRubriqueBudgetaireByStartWith("6%", annee);
    List<Long> ls_ids =
        ls.stream()
            .sorted(Comparator.comparing(RubriquesBudgetaire::getCode_budget))
            .filter(it -> it.getTypeRubriqueDetaille() != null)
            .map(
                rubriquesBudgetaire -> {
                  if (rubriquesBudgetaire.getTypeRubriqueDetaille() != null)
                    return rubriquesBudgetaire.getTypeRubriqueDetaille().getId();
                  return null;
                })
            .distinct()
            .collect(Collectors.toList());
    String domaine = "self.code_budget like '6%' and self.anneCurrent = " + annee;
    ActionView.ActionViewBuilder actionView =
        ActionView.define("Détail Rubrique")
            .model(RubriquesBudgetaire.class.getName())
            .add("form", "tw_ChargeExploitation")
            .param("popup", "false")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .context("fieldanne", annee);
    List<RubriquesBudgetaire> parent_budget =
        ls.stream()
            .filter(it -> it.getBudgetParent() == null)
            .sorted(Comparator.comparing(RubriquesBudgetaire::getCode_budget))
            .collect(Collectors.toList());
    for (int i = 0; i < ls_ids.size(); i++) {
      if (ls_ids.get(i) != null) {
        RubriquesBudgetaire budget_tmp = parent_budget.get(i);
        if (parent_budget.get(i).getCode_budget().equals("61")) {
          budget_tmp = configurationService.getRubriqueBudgetaireByYearAndCode("612", annee);
        }
        Long tmp = ls_ids.get(i);
        actionView.context("type_budget_detaille_" + (i + 1), tmp);
        actionView.context("parent_budget_" + (i + 1), budget_tmp.getId());
        actionView.context("som_" + (i + 1), budget_tmp.getMontant_budget());
      }
    }
    actionView.domain(domaine);
    response.setView(actionView.map());
  }

  public void redirect_to_detail_depense_single(ActionRequest request, ActionResponse response) {
    Integer annee = (Integer) request.getContext().get("date_anne");
    Long version = (Long) request.getContext().get("id_version");

    if (annee == null || annee == 0) {
      response.setFlash("L'anné n'est pas saisie");
      return;
    }
    String name = (String) request.getContext().get("_signal");
    response.setCanClose(true);
    List<RubriquesBudgetaire> ls =
        configurationService.getDetailRubriqueBudgetaireByStartWith("6%", annee, version);

    configurationService.update_montant_Fonc(version);
    ActionView.ActionViewBuilder actionView =
        ActionView.define("Détail Rubrique")
            .model(RubriquesBudgetaire.class.getName())
            .add("form", "tw_ChargeExploitation_version")
            .param("popup", "false")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .context("fieldanne", annee)
            .context("vue", "fonc")
            .context("id_version", version);

    Map<String, Long> map = configurationService.getIdFonc();

    for (Map.Entry<String, Long> tmp : map.entrySet()) {
      actionView.context(tmp.getKey(), tmp.getValue());
    }

    response.setCanClose(true);
    response.setView(actionView.map());
  }

  public void calcule_montant_sub_detail(ActionRequest request, ActionResponse response) {
    int annee = (int) request.getContext().get("fieldanne");
    List<RubriquesBudgetaire> ls =
        configurationService.getDetailRubriqueBudgetaireByStartWith("6%", annee);
    List<RubriquesBudgetaire> ls_rub =
        ls.stream()
            .sorted(Comparator.comparing(RubriquesBudgetaire::getCode_budget))
            .filter(it -> it.getBudgetParent() == null)
            .distinct()
            .collect(Collectors.toList());
    BigDecimal sum = BigDecimal.ZERO;
    for (int i = 0; i < ls_rub.size(); i++) {
      sum = sum.add(ls_rub.get(i).getMontant_total_children());
      response.setValue("$recette_som" + (i + 1), ls_rub.get(i).getMontant_total_children());
    }
    response.setValue("$total_sub_detail", sum);
  }

  public void calcule_montant_sub_detail_version(ActionRequest request, ActionResponse response) {
    int annee = (int) request.getContext().get("fieldanne");
    Long version = (Long) request.getContext().get("id_version");
    String vue = (String) request.getContext().get("vue");
    if (vue != null) {
      if (vue.equals("fonc")) {
        Map<String, Long> map = configurationService.getIdFonc();
        for (Map.Entry<String, Long> tmp : map.entrySet()) {
          response.setValue(
              "$mt_" + tmp.getKey(),
              configurationService.calcule_fonc(annee, version, tmp.getValue()));
        }
      }
    }

    List<RubriquesBudgetaire> ls =
        configurationService.getDetailRubriqueBudgetaireByStartWith("6%", annee, version);

    List<RubriquesBudgetaire> ls_rub =
        ls.stream()
            .sorted(Comparator.comparing(RubriquesBudgetaire::getCode_budget))
            .filter(it -> it.getBudgetParent() == null)
            .distinct()
            .collect(Collectors.toList());
    BigDecimal sum = BigDecimal.ZERO;
    for (int i = 0; i < ls_rub.size(); i++) {
      sum = sum.add(ls_rub.get(i).getMontant_total_children());
      response.setValue(
          "$recette_som" + (i + 1) + "_version", ls_rub.get(i).getMontant_total_children());
    }
    response.setValue("$total_sub_detail_version", sum);
  }

  public void show_montant_detailFromSubdetail(ActionRequest request, ActionResponse response) {
    int annee = (int) request.getContext().get("fieldanne");
    Long version = (Long) request.getContext().get("id_version");
    ActionView.ActionViewBuilder viewBuilder =
        ActionView.define("Détail Rubrique")
            .model(RubriquesBudgetaire.class.getName())
            .add("form", "tw_Sub_detail_chargExploi")
            .param("popup", "false")
            .param("show-toolbar", "true")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .context("fieldanne", annee)
            .context("id_version", version);
    List<RubriquesBudgetaire> ls_r =
        configurationService.getAllIdTypeDetaillChargesExploitation(annee, version);
    Map<String, Long> map = configurationService.getIdFonc();
    for (Map.Entry<String, Long> tmp : map.entrySet()) {
      viewBuilder.context(tmp.getKey(), tmp.getValue());
    }
    response.setCanClose(true);
    response.setView(viewBuilder.map());
  }

  public void calcule_montant_sub_detail_depencesCharges(
      ActionRequest request, ActionResponse response) {
    int annee = (int) request.getContext().get("fieldanne");
    Long version = (Long) request.getContext().get("id_version");
    List<RubriquesBudgetaire> ls_r =
        configurationService.getAllIdTypeDetaillChargesExploitation(annee, version);
    BigDecimal sum = BigDecimal.ZERO;
    Map<String, Long> map = configurationService.getIdFonc();
    for (Map.Entry<String, Long> tmp : map.entrySet()) {
      BigDecimal mt = configurationService.calcule_fonc_d(annee, version, tmp.getValue());
      response.setValue("$mt_" + tmp.getKey(), mt);
      sum = sum.add(mt);
    }
    response.setValue("$total_depenceCharges_detail", sum);
  }

  public void redirect_to_detail_equipe(ActionRequest request, ActionResponse response) {
    Integer annee = (Integer) request.getContext().get("date_anne");
    Long version = (Long) request.getContext().get("id_version");

    if (annee == null || annee == 0) {
      response.setFlash("L'anné n'est pas saisie");
      return;
    }

    Map<String, Long> map = configurationService.getIdEquip();

    ActionView.ActionViewBuilder actionViewBuilder =
        ActionView.define("Détail Rubrique")
            .model(RubriquesBudgetaire.class.getName())
            .add("form", "tw_EmploiInvestissement")
            .param("popup", "false")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .context("fieldanne", annee)
            .context("id_version", version);

    for (Map.Entry<String, Long> tmp : map.entrySet()) {
      actionViewBuilder.context(tmp.getKey(), tmp.getValue());
    }

    configurationService.update_field_equipe(version);
    response.setCanClose(true);
    response.setView(actionViewBuilder.map());
  }

  public void tw_getTotalEquipeByType(ActionRequest request, ActionResponse response) {
    int annee = (int) request.getContext().get("fieldanne");
    Long version = (Long) request.getContext().get("id_version");

    BigDecimal som1 = BigDecimal.ZERO;
    BigDecimal som2 = BigDecimal.ZERO;
    BigDecimal som3 = BigDecimal.ZERO;
    BigDecimal som4 = BigDecimal.ZERO;

    Map<String, Long> map = configurationService.getIdEquip();

    for (Map.Entry<String, Long> tmp : map.entrySet()) {
      Map<String, BigDecimal> data =
          configurationService.getSommeDetailEquipe(tmp.getValue(), annee, version);
      som1 = som1.add(data.get("ReportCredit"));
      som2 = som2.add(data.get("NouvCredit"));
      som3 = som3.add(data.get("Montant_budget"));

      response.setValue("$mt_report_" + tmp.getKey(), format_currency(data.get("ReportCredit")));
      response.setValue("$mt_nouv_" + tmp.getKey(), format_currency(data.get("NouvCredit")));
      response.setValue("$mt_budget_" + tmp.getKey(), format_currency(data.get("Montant_budget")));
    }

    response.setValue("$anne_current", annee);

    response.setValue("$equip_som_total_x_1", format_currency(som1));
    response.setValue("$equip_som_total_x_2", format_currency(som2));
    response.setValue("$equip_som_total_x_3", format_currency(som3));
    response.setValue("$equip_som_total_x_4", format_currency(som4));
  }

  private String format_currency(BigDecimal montant) {
    DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
    symbols.setGroupingSeparator(' ');
    symbols.setDecimalSeparator('.');

    DecimalFormat formatter = new DecimalFormat("###,#00.00", symbols);
    return formatter.format(montant);
  }

  public void tw_getListEquipeDetail(ActionRequest request, ActionResponse response) {
    int annee = (int) request.getContext().get("fieldanne");

    response.setCanClose(true);
    ActionView.ActionViewBuilder viewBuilder =
        ActionView.define("Détail Rubrique")
            .model(RubriquesBudgetaire.class.getName())
            .add("form", "tw_equipeDetail")
            .param("popup", "false")
            .param("show-toolbar", "true")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .context("id_version", request.getContext().get("id_version"))
            .context("fieldanne", annee);
    List<Long> ls_r =
        configurationService.getDetailRubriqueBudgetaireByStartWith("2%", annee).stream()
            .filter(rubriquesBudgetaire -> rubriquesBudgetaire.getTypeRubriqueDetaille() != null)
            .sorted(
                Comparator.comparing(
                    rubriquesBudgetaire ->
                        rubriquesBudgetaire.getTypeRubriqueDetaille().getCustom_order()))
            .map(rubriquesBudgetaire -> rubriquesBudgetaire.getTypeRubriqueDetaille().getId())
            .distinct()
            .collect(Collectors.toList());
    for (int i = 0; i < ls_r.size(); i++) {
      if (ls_r.get(i) != null) {
        viewBuilder.context("type_detail_" + (i + 1), ls_r.get(i));
      }
    }
    response.setView(viewBuilder.map());
  }

  public void tw_saveVersion(ActionRequest request, ActionResponse response) {
    if (request.getContext().get("annee") == null) {
      response.setFlash("le champs Année est vide");
      return;
    }
    Long id = (Long) request.getContext().get("id");

    int annee = (int) request.getContext().get("annee");

    VersionRubriqueBudgetaire versionRubriqueBudgetaire =
        Beans.get(VersionRubriqueBudgetaireRepository.class)
            .all()
            .filter("self.annee=:annee")
            .bind("annee", annee)
            .fetchOne();

    List<VersionRB> ls = new ArrayList<VersionRB>();
    if (request.getContext().get("versionRubriques") != null) {
      ls = (List<VersionRB>) request.getContext().get("versionRubriques");
    }

    if (versionRubriqueBudgetaire != null && id == null) {
      response.setFlash("Année duppliquée");
      return;
    } else {
      VersionRubriqueBudgetaire x = new VersionRubriqueBudgetaire();
      if (id != null && id > 0) x.setId(id);
      x.setVersionRubriques(ls);
      x.setAnnee(annee);
      versionRubriqueBudgetaire = configurationService.saveVersionRubriqueBudgetaire(x);
      response.setCanClose(true);

      ActionView.ActionViewBuilder viewBuilder =
          ActionView.define("List des Versions")
              .model(VersionRubriqueBudgetaire.class.getName())
              .add("form", "rubriqueBudgetaireVersionList_form")
              .add("grid", "rubriqueBudgetaireVersionList_grid")
              .param("popup", "false")
              .param("forceEdit", "true")
              .param("show-toolbar", "true")
              .param("show-confirm", "false")
              .context("_showRecord", versionRubriqueBudgetaire.getId());

      response.setView(viewBuilder.map());
    }
  }

  public void tw_start_generate_All_rubrique(ActionRequest request, ActionResponse response) {

    try {
      Long id = (Long) request.getContext().get("id");
      Context ctx = request.getContext().getParent();
      int annee = (int) ctx.get("annee");
      Long id_parent = (Long) ctx.get("id");
      VersionRubriqueBudgetaire vv =
          Beans.get(VersionRubriqueBudgetaireRepository.class).find(id_parent);

      List<RubriquesBudgetaire> ls =
          configurationService.dupliqueAllRubriqueBudgetaire(
              id, annee, vv.getVersionRubriques().size() == 1);
      VersionRB v = Beans.get(VersionRBRepository.class).find(id);
      v.setIs_generate(true);
      configurationService.updateVersionRb(v);
      configurationService.dupliquerRubriqueGenerale(v);
      response.setReload(true);
    } catch (Exception ex) {
      response.setFlash(ex.getMessage());
    }
  }

  public void tw_versionFinale_rb(ActionRequest request, ActionResponse response) {
    Long id_rb = (Long) request.getContext().get("id");
    Context ctx = request.getContext().getParent();
    Long id = (Long) ctx.get("id");
    VersionRubriqueBudgetaire v = Beans.get(VersionRubriqueBudgetaireRepository.class).find(id);
    v.setHas_version_final(true);
    v = configurationService.updateVersionRubriqueBudgetaire(v);
    for (VersionRB tmp : v.getVersionRubriques()) {
      if (tmp.getId() == id_rb) tmp.setIs_versionFinale(true);
      tmp.setHas_version_final(true);
      configurationService.updateVersionRb(tmp);
    }
    configurationService.setMontantStart(id_rb);
    response.setReload(true);
  }

  public void tw_get_all_rubrique_budgetaire(ActionRequest request, ActionResponse response) {
    Context ctx = request.getContext().getParent();

    long id_version = (long) request.getContext().get("id");
    int annee = (int) ctx.get("annee");

    configurationService.updateAllTotalRubriqueBudgetaire(annee);
    response.setView(
        ActionView.define("Afficher les Budget")
            .model(RubriquesBudgetaire.class.getName())
            .add("form", "formRubriquesBudgetaireAffichageFromVersion")
            .context("anne", annee)
            .context("id_version", id_version)
            .map());
    response.setReload(true);
  }

  public void tw_lowd_data_now(ActionRequest request, ActionResponse response) {
    int annee = (int) request.getContext().get("anne");
    Long version_id = (Long) request.getContext().get("id_version");

    response.setValue("$date_anne", annee);
    response.setValue("$id_version", version_id);
    // tresorerie
    BigDecimal some1 = configurationService.getSomme(13L, annee, version_id);
    // produit exploitation
    BigDecimal some2 = configurationService.getSomme(7L, annee, version_id);
    // ressource d'invesstissement
    BigDecimal some3 = configurationService.getSomme(8L, annee, version_id);
    // Charge exploitation
    BigDecimal some4 = configurationService.getSomme(9L, annee, version_id);
    // Emploi investissement
    BigDecimal some5 = configurationService.getSomme(10L, annee, version_id);
    // ressource d'invesstissement
    BigDecimal some6 = configurationService.getSomme(29L, annee, version_id);

    response.setValue("$recette_som1_single", some1);
    response.setValue("$recette_som2_single", some2);
    response.setValue("$recette_som3_single", some3);
    response.setValue("$recette_som4_single", some4);
    response.setValue("$recette_som5_single", some5);
    response.setValue("$recette_som6_single", some6);

    response.setValue("$total_total_recette_single", some1.add(some2).add(some3));
    response.setValue("$total_total_depense_single", some4.add(some5).add(some6));
  }

  public void tw_get_edit_rubrique_budgetaire(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    response.setView(
        ActionView.define("Saisir les Budget")
            .model(RubriquesBudgetaire.class.getName())
            .add("grid", "gridRubriquesBudgetaireSaisieNew")
            .add("form", "custom_budget_info")
            .param("forceEdit", "true")
            .param("popup", "false")
            .context("id_version", id)
            .domain("self.id_version=" + id)
            .map());
  }

  public void tw_afficher_Row_Equipe_version(ActionRequest request, ActionResponse response) {
    Long version = (Long) request.getContext().get("id_version");
    ActionView.ActionViewBuilder actionView =
        ActionView.define("Cacher les lignes des Rubrique")
            .model(RubriquesBudgetaire.class.getName())
            .add("grid", "tw_grid_cacher_Row_Equipe")
            .param("popup", "true")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .domain(
                " self.is_detaille is true and self.code_budget like '2%' and self.id_version="
                    + version);
    response.setView(actionView.map());
  }

  public void get_tw_cacher_Rubrique_principale_version(
      ActionRequest request, ActionResponse response) {
    Long version = (Long) request.getContext().get("id_version");
    ActionView.ActionViewBuilder actionView =
        ActionView.define("Cacher les montants des rubriques principales")
            .model(RubriquesBudgetaire.class.getName())
            .add("grid", "tw_grid_cacher_rubrique_principale")
            .param("popup", "true")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .domain("self.is_principal is true AND self.id_version=" + version);
    response.setView(actionView.map());
  }

  public void get_tw_affiche_Row_Fonc_version(ActionRequest request, ActionResponse response) {
    Long version = (Long) request.getContext().get("id_version");
    ActionView.ActionViewBuilder actionView =
        ActionView.define("Choisir les lignes a afficher dans FONC")
            .model(RubriquesBudgetaire.class.getName())
            .add("grid", "tw_grid_affiche_Row_Fonc")
            .param("popup", "true")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .domain(
                "self.is_detaille is true and self.code_budget like '6%' and self.id_version="
                    + version);
    response.setView(actionView.map());
  }

  public void get_tw_cacher_montant_Fonc_version(ActionRequest request, ActionResponse response) {
    Long version = (Long) request.getContext().get("id_version");
    ActionView.ActionViewBuilder actionView =
        ActionView.define("Cacher les montant dans FONC")
            .model(RubriquesBudgetaire.class.getName())
            .add("grid", "tw_grid_hide_montant_fonc")
            .param("popup", "true")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .domain(
                "self.show_row_fonc is true and self.is_detaille is true and self.code_budget like '6%' and self.id_version="
                    + version);
    response.setView(actionView.map());
  }

  public void get_tw_cacher_montant_Equipe_version(ActionRequest request, ActionResponse response) {
    Long version = (Long) request.getContext().get("id_version");
    ActionView.ActionViewBuilder actionView =
        ActionView.define("Cacher les montant dans FONC")
            .model(RubriquesBudgetaire.class.getName())
            .add("grid", "tw_grid_hide_montant_equip")
            .param("popup", "true")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .domain(
                "self.is_detaille is true and self.code_budget like '2%' and self.id_version="
                    + version);
    response.setView(actionView.map());
  }

  public void tw_edit_comment_versionRb(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    response.setView(
        ActionView.define("Saisir le Commentaire")
            .model(RubriquesBudgetaire.class.getName())
            .add("form", "versionRb_saisie_commentaire")
            .param("forceEdit", "true")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup", "reload")
            .context("_showRecord", id)
            .map());
  }

  public void get_traitement_ayoub_save(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    List<Bordereau> b =
        Beans.get(BordereauRepository.class).all().filter("self.active is true").fetch();
    for (Bordereau tmp : b) {
      if (tmp.getId() != id) {
        tmp.setActive(false);
        configurationService.update_Bordereau(tmp);
      }
    }
  }

  public void genererAnnee(ActionRequest request, ActionResponse response) {
    Integer courant = (Integer) request.getContext().get("courant");
    Integer generer = (Integer) request.getContext().get("generer");
    List<GestionSalaire> gs =
        Beans.get(GestionSalaireRepository.class)
            .all()
            .filter("self.annee=:annee")
            .bind("annee", courant)
            .fetch();
    if (gs.size() != 0) {
      for (GestionSalaire s : gs) {
        GestionSalaire b = Beans.get(GestionSalaireRepository.class).copy(s, true);
        b.setAnnee(generer);
        configurationService.saveGestionsalire(b);
      }
    }
  }
}
