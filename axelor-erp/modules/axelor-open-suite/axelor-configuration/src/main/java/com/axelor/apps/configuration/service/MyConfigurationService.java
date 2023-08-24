package com.axelor.apps.configuration.service;

import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.configuration.db.*;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MyConfigurationService extends AppBaseService {

  public void save_new_Responsabilite(ActionRequest request, ActionResponse response);

  public void update_Responsabilite(ActionRequest request, ActionResponse response);

  public HistoriqueBudgetaire saveHistoriqueBudgetaire(HistoriqueBudgetaire hist);

  public void updatehistoriqueBudgetaire2(Long id_hist, BigDecimal montant);

  public BudgetParRubrique getBudgetParRubriqueById(Long id_budgparRub);

  public BudgetParRubrique saveBudgetParRubrique(BudgetParRubrique budgetParRubrique);

  public MUTUELLE getMutuelleById(Long id);

  public String getRibRcar(int year);

  void saveGestionsalire(GestionSalaire t);

  GestionSalaire getGestionSalaireById(Long id);

  List<Long> getListOfAllParents(Long id);

  void modifierMontantParent(Long id, List<Long> ls);

  void updateOldMontant(Long id);

  RubriquesBudgetaire getRubriqueBudgetaire(Long id);

  RubriqueBudgetaireGenerale getRubriqueBudgetaireGeneraleByCodeAndYear(
      String code_budget, Integer anneCurrent);

  Map<String, Long> addBudgetParRubriqueGenerale(Long id);

  boolean checkRubriqueHasChildren(Long id, int annee);

  BudgetParRubrique getBudgetParRubriqueByRubriqueAndYear(Long id, Integer anneCurrent);

  BudgetParRubrique createBudgetParRubriqueByRubrique(
      RubriqueBudgetaireGenerale rg, RubriquesBudgetaire r);

  void saveRubriqueBudgetaire(RubriquesBudgetaire r);

  void updateRubriqueBudgetaireGeneraleAndBudget(Long id);

  void updateAllTotalRubriqueBudgetaire(Integer year);

  List<RubriquesBudgetaire> getAllChildrenBudgetById(Long id_r);

  void reduireMontantBudget(Long id);

  void removeRubriqueBudgetaireById(Long id);

  void reduireRubriqueBudgetaire(Long id);

  BigDecimal getSomme(Long id_typeRubriquePricipal, int annee);

  BigDecimal getSomme(Long id_typeRubriquePricipal, int annee, long id_version);

  BigDecimal getSommeDetail(Long id_typeRubriquePricipal, int annee, Long version);

  BigDecimal getSommeDetail_version(Long id_typeRubriquePricipal, int annee, Long version);

  RubriquesBudgetaire getRubriqueBudgetaireByYearAndCode(String code, Integer annee);

  List<RubriquesBudgetaire> getDetailRubriqueBudgetaireByStartWith(String s, Integer annee);

  List<RubriquesBudgetaire> getDetailRubriqueBudgetaireByStartWith(
      String startWith_code, Integer annee, Long version);

  List<RubriquesBudgetaire> getAllIdTypeDetaillChargesExploitation(int annee, Long version);

  Map<String, BigDecimal> getSommeDetailEquipe(Long id_typeRubriqueDetail, int annee, Long version);

  VersionRubriqueBudgetaire createVersionRubriqueBudgetaire(int annee);

  @Transactional
  VersionRubriqueBudgetaire saveVersionRubriqueBudgetaire(VersionRubriqueBudgetaire v);

  @Transactional
  List<RubriquesBudgetaire> dupliqueAllRubriqueBudgetaire(long id, int annee, boolean fromMaquette)
      throws Exception;

  @Transactional
  VersionRB updateVersionRb(VersionRB v);

  @Transactional
  VersionRubriqueBudgetaire updateVersionRubriqueBudgetaire(VersionRubriqueBudgetaire v);

  @Transactional
  void dupliquerRubriqueGenerale(VersionRB vrb);

  @Transactional
  void update_montant_Fonc(Long version);

  @Transactional
  void update_field_equipe(Long version);

  @Transactional
  void update_Bordereau(Bordereau tmp);

  void setMontantStart(Long id_rb);
}
