package com.axelor.apps.configuration.service;

import com.axelor.apps.base.service.app.AppBaseServiceImpl;
import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.repo.*;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class MyConfigurationServiceImpl extends AppBaseServiceImpl
		implements MyConfigurationService {
	
	@Inject
	ResponsabiliteRepository responsabiliteRepository;
	@Inject
	HistoriqueBudgetaireRepository historiqueBudgetaireRepository;
	@Inject
	BudgetParRubriqueRepository budgetParRubriqueRepository;
	@Inject
	MUTUELLERepository mutuelleRepository;
	@Inject
	GestionSalaireRepository gestionSalaireRepository;
	@Inject
	RCARRepository rcarRepository;
	@Inject
	IRRepository irRepository;
	@Inject
	AvancementPeriodeRepository avancementPeriodeRepository;
	@Inject
	RubriquesBudgetaireRepository rubriquesBudgetaireRepository;
	@Inject
	RubriqueBudgetaireGeneraleRepository rubriqueBudgetaireGeneraleRepository;
	@Inject
	VersionRubriqueBudgetaireRepository versionRubriqueBudgetaireRepository;
	@Inject
	VersionRBRepository versionRBRepository;
	@Inject
	BordereauRepository bordereauRepository;
	
	@Override
	@Transactional
	public void save_new_Responsabilite(ActionRequest request, ActionResponse response) {
		String name = request.getContext().get("name").toString();
		String description = request.getContext().get("description").toString();
		//       String idtest= request.getContext().get("id").toString();
		//        Long id_responsabilite= Long.valueOf( request.getContext().get("id").toString());
		String logementIsNet = request.getContext().get("logementIsNet").toString();
		String fonctionIsNet = request.getContext().get("fonctionIsNet").toString();
		String voitureIsNet = request.getContext().get("voitureIsNet").toString();
		String representationIsNet = request.getContext().get("representationIsNet").toString();
		Double indemnitLogement =
				Double.parseDouble(request.getContext().get("indemnitLogement").toString());
		Double indemnitFonction =
				Double.parseDouble(request.getContext().get("indemnitFonction").toString());
		Double indemnitVoiture =
				Double.parseDouble(request.getContext().get("indemnitVoiture").toString());
		Double indemnitRepresentation =
				Double.parseDouble(request.getContext().get("indemnitRepresentation").toString());
		Double indemnitLogement_Net =
				Double.parseDouble(request.getContext().get("indemnitLogement_Net").toString());
		Double indemnitFonction_Net =
				Double.parseDouble(request.getContext().get("indemnitFonction_Net").toString());
		Double indemnitVoiture_Net =
				Double.parseDouble(request.getContext().get("indemnitVoiture_Net").toString());
		Double indemnitRepresentation_Net =
				Double.parseDouble(request.getContext().get("indemnitRepresentation_Net").toString());
		BigDecimal indemnitLogementDecimal = new BigDecimal(indemnitLogement);
		BigDecimal indemnitFonctionDecimal = new BigDecimal(indemnitFonction);
		BigDecimal indemnitVoitureDecimal = new BigDecimal(indemnitVoiture);
		BigDecimal indemnitRepresentationDecimal = new BigDecimal(indemnitRepresentation);
		BigDecimal indemnitLogementDecimal_Net = new BigDecimal(indemnitLogement_Net);
		BigDecimal indemnitFonctionDecimal_Net = new BigDecimal(indemnitFonction_Net);
		BigDecimal indemnitVoitureDecimal_Net = new BigDecimal(indemnitVoiture_Net);
		BigDecimal indemnitRepresentationDecimal_Net = new BigDecimal(indemnitRepresentation_Net);
		Responsabilite responsabilite = new Responsabilite();
		responsabilite.setName(name);
		responsabilite.setDescription(description);
		if (logementIsNet == "false") {
			responsabilite.setIndemnitLogement(indemnitLogementDecimal);
			responsabilite.setIndemnitLogement_Net(BigDecimal.ZERO);
			responsabilite.setLogementIsNet(false);
		} else {
			responsabilite.setIndemnitLogement(BigDecimal.ZERO);
			responsabilite.setIndemnitLogement_Net(indemnitLogementDecimal_Net);
			responsabilite.setLogementIsNet(true);
		}
		if (fonctionIsNet == "false") {
			responsabilite.setIndemnitFonction(indemnitFonctionDecimal);
			responsabilite.setIndemnitFonction_Net(BigDecimal.ZERO);
			responsabilite.setFonctionIsNet(false);
		} else {
			responsabilite.setIndemnitFonction(BigDecimal.ZERO);
			responsabilite.setIndemnitFonction_Net(indemnitFonctionDecimal_Net);
			responsabilite.setFonctionIsNet(true);
		}
		if (voitureIsNet == "false") {
			responsabilite.setIndemnitVoiture(indemnitVoitureDecimal);
			responsabilite.setIndemnitVoiture_Net(BigDecimal.ZERO);
			responsabilite.setVoitureIsNet(false);
		} else {
			responsabilite.setIndemnitVoiture(BigDecimal.ZERO);
			responsabilite.setIndemnitVoiture_Net(indemnitVoitureDecimal_Net);
			responsabilite.setVoitureIsNet(true);
		}
		if (representationIsNet == "false") {
			responsabilite.setIndemnitRepresentation(indemnitRepresentationDecimal);
			responsabilite.setIndemnitRepresentation_Net(BigDecimal.ZERO);
			responsabilite.setRepresentationIsNet(false);
		} else {
			responsabilite.setIndemnitRepresentation(BigDecimal.ZERO);
			responsabilite.setIndemnitRepresentation_Net(indemnitRepresentationDecimal_Net);
			responsabilite.setRepresentationIsNet(true);
		}
		responsabiliteRepository.save(responsabilite);
	}
	
	@Override
	@Transactional
	public void update_Responsabilite(ActionRequest request, ActionResponse response) {
		String name = request.getContext().get("name").toString();
		String description = request.getContext().get("description").toString();
		String idtest = request.getContext().get("id").toString();
		Long id_responsabilite = Long.valueOf(request.getContext().get("id").toString());
		String logementIsNet = request.getContext().get("logementIsNet").toString();
		String fonctionIsNet = request.getContext().get("fonctionIsNet").toString();
		String voitureIsNet = request.getContext().get("voitureIsNet").toString();
		String representationIsNet = request.getContext().get("representationIsNet").toString();
		Double indemnitLogement =
				Double.parseDouble(request.getContext().get("indemnitLogement").toString());
		Double indemnitFonction =
				Double.parseDouble(request.getContext().get("indemnitFonction").toString());
		Double indemnitVoiture =
				Double.parseDouble(request.getContext().get("indemnitVoiture").toString());
		Double indemnitRepresentation =
				Double.parseDouble(request.getContext().get("indemnitRepresentation").toString());
		Double indemnitLogement_Net =
				Double.parseDouble(request.getContext().get("indemnitLogement_Net").toString());
		Double indemnitFonction_Net =
				Double.parseDouble(request.getContext().get("indemnitFonction_Net").toString());
		Double indemnitVoiture_Net =
				Double.parseDouble(request.getContext().get("indemnitVoiture_Net").toString());
		Double indemnitRepresentation_Net =
				Double.parseDouble(request.getContext().get("indemnitRepresentation_Net").toString());
		BigDecimal indemnitLogementDecimal = new BigDecimal(indemnitLogement);
		BigDecimal indemnitFonctionDecimal = new BigDecimal(indemnitFonction);
		BigDecimal indemnitVoitureDecimal = new BigDecimal(indemnitVoiture);
		BigDecimal indemnitRepresentationDecimal = new BigDecimal(indemnitRepresentation);
		BigDecimal indemnitLogementDecimal_Net = new BigDecimal(indemnitLogement_Net);
		BigDecimal indemnitFonctionDecimal_Net = new BigDecimal(indemnitFonction_Net);
		BigDecimal indemnitVoitureDecimal_Net = new BigDecimal(indemnitVoiture_Net);
		BigDecimal indemnitRepresentationDecimal_Net = new BigDecimal(indemnitRepresentation_Net);
		Responsabilite responsabilite = loadOne_responsabilite_byId(id_responsabilite);
		responsabilite.setName(name);
		responsabilite.setDescription(description);
		if (logementIsNet == "false") {
			responsabilite.setIndemnitLogement(indemnitLogementDecimal);
			responsabilite.setIndemnitLogement_Net(BigDecimal.ZERO);
			responsabilite.setLogementIsNet(false);
		} else {
			responsabilite.setIndemnitLogement(BigDecimal.ZERO);
			responsabilite.setIndemnitLogement_Net(indemnitLogementDecimal_Net);
			responsabilite.setLogementIsNet(true);
		}
		if (fonctionIsNet == "false") {
			responsabilite.setIndemnitFonction(indemnitFonctionDecimal);
			responsabilite.setIndemnitFonction_Net(BigDecimal.ZERO);
			responsabilite.setFonctionIsNet(false);
		} else {
			responsabilite.setIndemnitFonction(BigDecimal.ZERO);
			responsabilite.setIndemnitFonction_Net(indemnitFonctionDecimal_Net);
			responsabilite.setFonctionIsNet(true);
		}
		if (voitureIsNet == "false") {
			responsabilite.setIndemnitVoiture(indemnitVoitureDecimal);
			responsabilite.setIndemnitVoiture_Net(BigDecimal.ZERO);
			responsabilite.setVoitureIsNet(false);
		} else {
			responsabilite.setIndemnitVoiture(BigDecimal.ZERO);
			responsabilite.setIndemnitVoiture_Net(indemnitVoitureDecimal_Net);
			responsabilite.setVoitureIsNet(true);
		}
		if (representationIsNet == "false") {
			responsabilite.setIndemnitRepresentation(indemnitRepresentationDecimal);
			responsabilite.setIndemnitRepresentation_Net(BigDecimal.ZERO);
			responsabilite.setRepresentationIsNet(false);
		} else {
			responsabilite.setIndemnitRepresentation(BigDecimal.ZERO);
			responsabilite.setIndemnitRepresentation_Net(indemnitRepresentationDecimal_Net);
			responsabilite.setRepresentationIsNet(true);
		}
		responsabiliteRepository.save(responsabilite);
	}
	
	private Responsabilite loadOne_responsabilite_byId(Long id) {
		return responsabiliteRepository.find(id);
	}
	
	@Transactional
	public HistoriqueBudgetaire saveHistoriqueBudgetaire(HistoriqueBudgetaire hist) {
		return historiqueBudgetaireRepository.save(hist);
	}
	
	@Transactional
	public BudgetParRubrique saveBudgetParRubrique(BudgetParRubrique budgetParRubrique) {
		return budgetParRubriqueRepository.save(budgetParRubrique);
	}
	
	@Transactional
	public void updatehistoriqueBudgetaire2(Long id_hist, BigDecimal montant) {
		HistoriqueBudgetaire hist = historiqueBudgetaireRepository.find(id_hist);
		BigDecimal difference = montant.subtract(hist.getMontantRubrique());
		BudgetParRubrique bpr =
				budgetParRubriqueRepository
						.all()
						.filter("self.historiqueBudgetaire.id=:id")
						.bind("id", id_hist)
						.fetchOne();
		List<HistoriqueBudgetaire> ls =
				historiqueBudgetaireRepository
						.all()
						.filter(
								"self.createdOn>?1 and self.budgetParRubrique.id=?2",
								hist.getCreatedOn(),
								bpr.getId())
						.fetch();
		for (HistoriqueBudgetaire tmp : ls) {
			if (difference.compareTo(new BigDecimal(0)) >= 0)
				tmp.setMontantRubrique(tmp.getMontantRubrique().add(difference));
			else
				tmp.setMontantRubrique(
						tmp.getMontantRubrique().subtract((difference).multiply(new BigDecimal("-1"))));
			historiqueBudgetaireRepository.save(tmp);
		}
		hist.setMontantRubrique(difference);
		historiqueBudgetaireRepository.save(hist);
	}
	
	public BudgetParRubrique getBudgetParRubriqueById(Long id_budgparRub) {
		return budgetParRubriqueRepository.find(id_budgparRub);
	}
	
	@Override
	public MUTUELLE getMutuelleById(Long id) {
		return mutuelleRepository.find(id);
	}
	
	public String getRibRcar(int year) {
		RCAR c = rcarRepository.all().filter("self.annee=?", year).fetchOne();
		return c.getNamBank() + " " + c.getRib();
	}
	
	@Override
	@Transactional
	public void saveGestionsalire(GestionSalaire t) {
		gestionSalaireRepository.save(t);
	}
	
	@Override
	public GestionSalaire getGestionSalaireById(Long id) {
		return gestionSalaireRepository.find(id);
	}
	
	public int getAvancementByEchelonStartEnd(String ech_start, String ech_end) {
		return avancementPeriodeRepository
				.all()
				.filter("self.echelon_start = :ech_start and self.echelon_end = :ech_end")
				.bind("ech_start", ech_start)
				.bind("ech_end", ech_end)
				.fetch()
				.size();
	}
	
	private BigDecimal getMontantAllChild(BudgetParRubrique tmp) {
		BigDecimal som = BigDecimal.ZERO;
		RubriqueBudgetaireGenerale parent = tmp.getRubriqueBudgetaire().getParent();
		if (parent == null) return som;
		List<BudgetParRubrique> ls =
				budgetParRubriqueRepository
						.all()
						.filter("self.rubriqueBudgetaire=:parent and self.id!=:id")
						.bind("parent", parent)
						.bind("id", tmp.getId())
						.fetch();
		if (ls != null && ls.size() > 0) {
			for (BudgetParRubrique tt : ls) {
				if (tt.getMontant().compareTo(BigDecimal.ZERO) <= 0
						&& tt.getRubriqueBudgetaire().getParent() != null)
					som = som.add(getMontantAllChild(tt));
				else {
					som = som.add(tt.getMontant());
				}
			}
		}
		return som;
	}
	
	@Override
	public RubriquesBudgetaire getRubriqueBudgetaire(Long id) {
		return rubriquesBudgetaireRepository.find(id);
	}
	
	@Override
	public List<Long> getListOfAllParents(Long id) {
		RubriquesBudgetaire r = getRubriqueBudgetaire(id);
		List<Long> ls = new ArrayList<>();
		if (r.getBudgetParent() != null) {
			RubriquesBudgetaire rr = getRubriqueBudgetaire(r.getBudgetParent().getId());
			ls.add(rr.getId());
			if (rr.getBudgetParent() != null) ls.addAll(getListOfAllParents(rr.getId()));
		}
		return ls;
	}
	
	@Override
	@Transactional
	public void modifierMontantParent(Long id, List<Long> ls) {
		RubriquesBudgetaire r = getRubriqueBudgetaire(id);
		for (Long id_one : ls) {
			BigDecimal mt = BigDecimal.ZERO;
			RubriquesBudgetaire rr = getRubriqueBudgetaire(id_one);
			mt =
					rr.getMontant_total_children()
							.subtract(r.getOld_montant_budget())
							.add(r.getMontant_budget());
			rr.setMontant_total_children(mt);
			if (r.getCode_budget().startsWith("2")) {
				if (r.getReportCredit().compareTo(r.getReportCredit_old()) != 0) {
					BigDecimal mt2 = BigDecimal.ZERO;
					mt2 =
							rr.getSomme_mtChildren_mtreportCredit()
									.subtract(r.getReportCredit_old())
									.add(r.getReportCredit());
					rr.setSomme_mtChildren_mtreportCredit(mt2);
				}
				if (r.getNouvCredit().compareTo(r.getNouvCredit_old()) != 0) {
					BigDecimal mt3 = BigDecimal.ZERO;
					mt3 =
							rr.getSomme_mtChildren_mtnouvCredit()
									.subtract(r.getNouvCredit_old())
									.add(r.getNouvCredit());
					rr.setSomme_mtChildren_mtnouvCredit(mt3);
				}
				if (r.getCreditEngagement().compareTo(r.getCreditEngagement_old()) != 0) {
					BigDecimal mt4 = BigDecimal.ZERO;
					mt4 =
							rr.getSomme_mtChildren_creditEngagement()
									.subtract(r.getCreditEngagement_old())
									.add(r.getCreditEngagement());
					rr.setSomme_mtChildren_creditEngagement(mt4);
				}
			}
			rubriquesBudgetaireRepository.save(rr);
		}
	}
	
	@Transactional
	public void modifierMontantParentForDelete(Long id, List<Long> ls) {
		RubriquesBudgetaire r = getRubriqueBudgetaire(id);
		for (Long id_one : ls) {
			BigDecimal mt = BigDecimal.ZERO;
			RubriquesBudgetaire rr = getRubriqueBudgetaire(id_one);
			mt = rr.getMontant_total_children().subtract(r.getMontant_budget());
			rr.setMontant_total_children(mt);
			rubriquesBudgetaireRepository.save(rr);
		}
	}
	
	@Override
	@Transactional
	public void updateOldMontant(Long id) {
		RubriquesBudgetaire r = rubriquesBudgetaireRepository.find(id);
		r.setOld_montant_budget(r.getMontant_budget());
		if (r.getCode_budget().startsWith("2")
				&& r.getReportCredit().compareTo(r.getReportCredit_old()) != 0) {
			r.setReportCredit_old(r.getReportCredit());
		}
		if (r.getCode_budget().startsWith("2")
				&& r.getNouvCredit().compareTo(r.getNouvCredit_old()) != 0) {
			r.setNouvCredit_old(r.getNouvCredit());
		}
		if (r.getCode_budget().startsWith("2")
				&& r.getCreditEngagement().compareTo(r.getCreditEngagement_old()) != 0) {
			r.setCreditEngagement_old(r.getCreditEngagement());
		}
		rubriquesBudgetaireRepository.save(r);
	}
	
	@Override
	public RubriqueBudgetaireGenerale getRubriqueBudgetaireGeneraleByCodeAndYear(
			String code_budget, Integer anneCurrent) {
		return rubriqueBudgetaireGeneraleRepository
				.all()
				.filter("self.codeBudg=:code and self.annee=:annee")
				.bind("code", code_budget)
				.bind("annee", anneCurrent)
				.fetchOne();
	}
	
	@Override
	@Transactional
	public Map<String, Long> addBudgetParRubriqueGenerale(Long id) {
		RubriquesBudgetaire r = getRubriqueBudgetaire(id);
		RubriqueBudgetaireGenerale rg = createRubriquebudgetaireGenerale(r);
		BudgetParRubrique bpr = createBudgetParRubrique(rg, r);
		Map<String, Long> map = new HashMap<>();
		map.put("rubrique", rg.getId());
		map.put("budget", bpr.getId());
		// update budget par budgetaire des parents
		// update historique budgetaire des parents
		List<Long> ls_ids = getListOfAllParents(id);
		for (Long tmp : ls_ids) {
			RubriquesBudgetaire rr = getRubriqueBudgetaire(tmp);
			updateBudgetParRubrique(
					rr.getId_budget_par_rubrique_generale(),
					rr.getMontant_total_children().add(rr.getMontant_budget()));
		}
		return map;
	}
	
	@Transactional
	private void updateBudgetParRubrique(Long id_budget_par_rubrique_generale, BigDecimal add) {
		BudgetParRubrique bpr = budgetParRubriqueRepository.find(id_budget_par_rubrique_generale);
		bpr.setMontant(add);
		budgetParRubriqueRepository.save(bpr);
		updatehistoriqueBudgetaire2(bpr.getHistoriqueBudgetaire().getId(), add);
	}
	
	@Transactional
	private BudgetParRubrique createBudgetParRubrique(
			RubriqueBudgetaireGenerale rg, RubriquesBudgetaire r) {
		BudgetParRubrique b = new BudgetParRubrique();
		b.setYear(r.getAnneCurrent());
		b.setName(r.getTitre_budget());
		b.setMontant(
				r.getMontant_budget().compareTo(BigDecimal.ZERO) == 0
						? r.getMontant_total_children()
						: r.getMontant_budget());
		b.setRubriqueBudgetaire(rubriqueBudgetaireGeneraleRepository.find(rg.getId()));
		b = saveBudgetParRubrique(b);
		HistoriqueBudgetaire hist = new HistoriqueBudgetaire();
		hist.setRubriqueBudgetaire(b.getRubriqueBudgetaire());
		hist.setMontant(new BigDecimal(0));
		hist.setMontantRubrique(b.getMontant());
		hist.setAnnee(b.getYear());
		hist.setDateEx(LocalDate.now());
		hist.setMois(LocalDate.now().getMonthValue());
		hist.setTypeOperation("credit");
		hist = saveHistoriqueBudgetaire(hist);
		b.setHistoriqueBudgetaire(hist);
		return saveBudgetParRubrique(b);
	}
	
	@Transactional
	private RubriqueBudgetaireGenerale createRubriquebudgetaireGenerale(RubriquesBudgetaire r) {
		RubriqueBudgetaireGenerale rg = new RubriqueBudgetaireGenerale();
		if (r.getBudgetParent() != null && r.getBudgetParent().getId_rubrique_generale() != null) {
			RubriqueBudgetaireGenerale r_parent =
					rubriqueBudgetaireGeneraleRepository.find(r.getBudgetParent().getId_rubrique_generale());
			rg.setParent(r_parent);
		}
		rg.setAnnee(r.getAnneCurrent());
		rg.setName(r.getTitre_budget());
		rg.setCodeBudg(r.getCode_budget());
		rg.setType(r.getDepenceAndRecette() ? "Recettes" : "Depenses");
		return rubriqueBudgetaireGeneraleRepository.save(rg);
	}
	
	@Override
	public boolean checkRubriqueHasChildren(Long id, int annee) {
		List<RubriquesBudgetaire> ls =
				rubriquesBudgetaireRepository
						.all()
						.filter("self.anneCurrent=:annee and self.budgetParent.id=:id")
						.bind("annee", annee)
						.bind("id", id)
						.fetch();
		return ls.size() > 0;
	}
	
	@Override
	public BudgetParRubrique getBudgetParRubriqueByRubriqueAndYear(Long id, Integer anneCurrent) {
		return budgetParRubriqueRepository
				.all()
				.filter("self.year=:anne and self.rubriqueBudgetaire.id=:id")
				.bind("anne", anneCurrent)
				.bind("id", id)
				.fetchOne();
	}
	
	@Override
	@Transactional
	public BudgetParRubrique createBudgetParRubriqueByRubrique(
			RubriqueBudgetaireGenerale rg, RubriquesBudgetaire r) {
		return createBudgetParRubrique(rg, r);
	}
	
	@Override
	@Transactional
	public void saveRubriqueBudgetaire(RubriquesBudgetaire r) {
		rubriquesBudgetaireRepository.save(r);
	}
	
	@Override
	@Transactional
	public void updateRubriqueBudgetaireGeneraleAndBudget(Long id) {
		RubriquesBudgetaire r = getRubriqueBudgetaire(id);
		RubriqueBudgetaireGenerale rg =
				rubriqueBudgetaireGeneraleRepository.find(r.getId_rubrique_generale());
		BudgetParRubrique bpr =
				budgetParRubriqueRepository.find(r.getId_budget_par_rubrique_generale());
		updateRubriqueBudgetaireGenerale(rg, r);
		updateBudgetParRubrique(bpr, r);
	}
	
	@Transactional
	private void updateBudgetParRubrique(BudgetParRubrique bpr, RubriquesBudgetaire r) {
		bpr.setMontant(r.getMontant_budget());
		bpr.setName(r.getTitre_budget());
		bpr.setYear(r.getAnneCurrent());
		updatehistoriqueBudgetaire2(bpr.getHistoriqueBudgetaire().getId(), r.getMontant_budget());
		budgetParRubriqueRepository.save(bpr);
	}
	
	@Transactional
	private void updateRubriqueBudgetaireGenerale(
			RubriqueBudgetaireGenerale rg, RubriquesBudgetaire r) {
		rg = rubriqueBudgetaireGeneraleRepository.find(rg.getId());
		rg.setName(r.getTitre_budget());
		rg.setAnnee(r.getAnneCurrent());
		rg.setCodeBudg(r.getCode_budget());
		rg.setType(r.getDepenceAndRecette() ? "Recettes" : "Depenses");
		if (r.getBudgetParent() != null && r.getBudgetParent().getId_rubrique_generale() != null) {
			rg.setParent(
					rubriqueBudgetaireGeneraleRepository.find(r.getBudgetParent().getId_rubrique_generale()));
		} else {
			rg.setParent(null);
		}
		rubriqueBudgetaireGeneraleRepository.save(rg);
	}
	
	@Override
	@Transactional
	public void updateAllTotalRubriqueBudgetaire(Integer year) {
		String req =
				"update configuration_rubriques_budgetaire x set somme_mt_children_mt_budget = ( select case when x.hide_in_principale is true then 0 else (montant_budget + montant_total_children) end ) where anne_current = "
						+ year
						+ " ;";
		RunSqlRequestForMe.runSqlRequest(req);
	}
	
	@Override
	public List<RubriquesBudgetaire> getAllChildrenBudgetById(Long id_r) {
		return rubriquesBudgetaireRepository
				.all()
				.filter("self.budgetParent.id = :id")
				.bind("id", id_r)
				.fetch();
	}
	
	@Override
	@Transactional
	public void reduireMontantBudget(Long id) {
		RubriquesBudgetaire r0 = getRubriqueBudgetaire(id);
		List<Long> ls = getListOfAllParents(id);
		for (Long ids : ls) {
			RubriquesBudgetaire r = getRubriqueBudgetaire(ids);
			RubriqueBudgetaireGenerale rg =
					rubriqueBudgetaireGeneraleRepository.find(r.getId_rubrique_generale());
			BudgetParRubrique bpr =
					budgetParRubriqueRepository.find(r.getId_budget_par_rubrique_generale());
			updatehistoriqueBudgetaire2(bpr.getHistoriqueBudgetaire().getId(), bpr.getMontant());
			bpr.setMontant(bpr.getMontant().subtract(r0.getMontant_budget()));
			budgetParRubriqueRepository.save(bpr);
		}
	}
	
	@Override
	@Transactional
	public void removeRubriqueBudgetaireById(Long id) {
		RubriquesBudgetaire r = getRubriqueBudgetaire(id);
		RubriqueBudgetaireGenerale rg =
				rubriqueBudgetaireGeneraleRepository.find(r.getId_rubrique_generale());
		BudgetParRubrique bpr =
				budgetParRubriqueRepository.find(r.getId_budget_par_rubrique_generale());
		removeHistoriqueBudgetaure(rg.getId());
		budgetParRubriqueRepository.remove(bpr);
		rubriqueBudgetaireGeneraleRepository.remove(rg);
		rubriquesBudgetaireRepository.remove(r);
	}
	
	@Transactional
	private void removeHistoriqueBudgetaure(Long id) {
		List<HistoriqueBudgetaire> ls =
				historiqueBudgetaireRepository.all().filter("self.rubriqueBudgetaire.id=?1", id).fetch();
		for (HistoriqueBudgetaire tmp : ls) {
			historiqueBudgetaireRepository.remove(tmp);
		}
	}
	
	@Override
	@Transactional
	public void reduireRubriqueBudgetaire(Long id) {
		RubriquesBudgetaire r = getRubriqueBudgetaire(id);
		List<Long> ls_ids = getListOfAllParents(r.getId());
		modifierMontantParentForDelete(r.getId(), ls_ids);
	}
	
	@Override
	public BigDecimal getSomme(Long id_typeRubriquePricipal, int annee) {
		BigDecimal res = BigDecimal.ZERO;
		List<RubriquesBudgetaire> r =
				rubriquesBudgetaireRepository
						.all()
						.filter("self.typeRubriquePrincipal= :id and self.anneCurrent = :annee")
						.bind("id", id_typeRubriquePricipal)
						.bind("annee", annee)
						.fetch();
		res =
				r.stream()
						.map(RubriquesBudgetaire::getSomme_mtChildren_mtBudget)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		return res;
	}
	
	@Override
	public BigDecimal getSomme(Long id_typeRubriquePricipal, int annee, long id_version) {
		BigDecimal res = BigDecimal.ZERO;
		List<RubriquesBudgetaire> r =
				rubriquesBudgetaireRepository
						.all()
						.filter(
								"self.id_version=:id_version and self.typeRubriquePrincipal= :id and self.anneCurrent = :annee")
						.bind("id", id_typeRubriquePricipal)
						.bind("annee", annee)
						.bind("id_version", id_version)
						.fetch();
		res =
				r.stream()
						.map(RubriquesBudgetaire::getSomme_mtChildren_mtBudget)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		return res;
	}
	
	@Override
	public BigDecimal getSommeDetail(Long id_typeRubriquePricipal, int annee, Long version) {
		BigDecimal res = BigDecimal.ZERO;
		List<RubriquesBudgetaire> r =
				rubriquesBudgetaireRepository
						.all()
						.filter(
								"self.id_version=:version and self.typeRubriqueDetaille= :id and self.anneCurrent = :annee")
						.bind("id", id_typeRubriquePricipal)
						.bind("annee", annee)
						.bind("version", version)
						.fetch();
		res =
				r.stream()
						.map(RubriquesBudgetaire::getMontant_budget)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		return res;
	}
	
	@Override
	public BigDecimal getSommeDetail_version(Long id_typeRubriquePricipal, int annee, Long version) {
		BigDecimal res = BigDecimal.ZERO;
		List<RubriquesBudgetaire> r =
				rubriquesBudgetaireRepository
						.all()
						.filter(
								"self.id_version=:version and self.typeRubriqueDetaille= :id and self.anneCurrent = :annee")
						.bind("id", id_typeRubriquePricipal)
						.bind("annee", annee)
						.bind("version", version)
						.fetch();
		res =
				r.stream()
						.map(RubriquesBudgetaire::getMontant_budget)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		return res;
	}
	
	@Override
	public RubriquesBudgetaire getRubriqueBudgetaireByYearAndCode(String code, Long id_version) {
		return rubriquesBudgetaireRepository
				.all()
				.filter("self.code_budget like :code and self.id_version=:version")
				.bind("version", id_version)
				.bind("code", code)
				.fetchOne();
	}
	
	@Override
	public RubriqueConfiguration getCategorieBudgetaireByCode(String code, Long id_version) {
		String req = code.isEmpty() ? "self.code_budget is null and self.id_version=:version and self.typeRubriquePrincipal is not null"
				: "self.code_budget like :code and self.id_version=:version and self.typeRubriquePrincipal is not null";
		RubriquesBudgetaire r = rubriquesBudgetaireRepository
				.all()
				.filter(req)
				.bind("version", id_version)
				.bind("code", code)
				.fetchOne();
		return r != null ? r.getTypeRubriquePrincipal() : null;
	}
	
	@Override
	public List<RubriquesBudgetaire> getDetailRubriqueBudgetaireByStartWith(
			String startWith_code, Integer annee) {
		return rubriquesBudgetaireRepository
				.all()
				.filter("self.code_budget like :code and self.anneCurrent=:annee")
				.bind("annee", annee)
				.bind("code", startWith_code)
				.fetch();
	}
	
	@Override
	public List<RubriquesBudgetaire> getDetailRubriqueBudgetaireByStartWith(
			String startWith_code, Integer annee, Long version) {
		return rubriquesBudgetaireRepository
				.all()
				.filter(
						"self.id_version=:version and self.code_budget like :code and self.anneCurrent=:annee")
				.bind("annee", annee)
				.bind("code", startWith_code)
				.bind("version", version)
				.fetch();
	}
	
	@Override
	public List<RubriquesBudgetaire> getAllIdTypeDetaillChargesExploitation(int annee, Long version) {
		return rubriquesBudgetaireRepository
				.all()
				.filter(
						"self.id_version=:version and self.code_budget like '6%' and self.anneCurrent=:annee and budgetParent is null")
				.bind("annee", annee)
				.bind("version", version)
				.order("code_budget")
				.fetch();
	}
	
	@Override
	public Map<String, BigDecimal> getSommeDetailEquipe(
			Long id_typeRubriqueDetail, int annee, Long version) {
		BigDecimal res = BigDecimal.ZERO;
		List<RubriquesBudgetaire> r =
				rubriquesBudgetaireRepository
						.all()
						.filter(
								"self.id_version=:version and self.typeRubriqueDetaille= :id and self.anneCurrent = :annee")
						.bind("id", id_typeRubriqueDetail)
						.bind("annee", annee)
						.bind("version", version)
						.fetch();
		Map<String, BigDecimal> map = new HashMap<>();
		map.put(
				"ReportCredit",
				r.stream()
						.map(RubriquesBudgetaire::getReportCredit)
						.reduce(BigDecimal.ZERO, BigDecimal::add));
		map.put(
				"NouvCredit",
				r.stream()
						.map(RubriquesBudgetaire::getNouvCredit)
						.reduce(BigDecimal.ZERO, BigDecimal::add));
		map.put(
				"Montant_budget",
				r.stream()
						.map(RubriquesBudgetaire::getMontant_budget)
						.reduce(BigDecimal.ZERO, BigDecimal::add));
		map.put(
				"creditEngagement",
				r.stream()
						.map(RubriquesBudgetaire::getCreditEngagement)
						.reduce(BigDecimal.ZERO, BigDecimal::add));
		return map;
	}
	
	@Override
	@Transactional
	public VersionRubriqueBudgetaire createVersionRubriqueBudgetaire(int annee) {
		VersionRubriqueBudgetaire v = new VersionRubriqueBudgetaire();
		v.setAnnee(annee);
		return versionRubriqueBudgetaireRepository.save(v);
	}
	
	@Override
	@Transactional
	public VersionRubriqueBudgetaire saveVersionRubriqueBudgetaire(VersionRubriqueBudgetaire v) {
		List<VersionRB> ls = new ArrayList<>();
		VersionRubriqueBudgetaire vv = null;
		if (v.getId() != null) {
			vv = versionRubriqueBudgetaireRepository.find(v.getId());
		} else {
			vv = new VersionRubriqueBudgetaire();
		}
		if (v.getId() != null) {
			vv.setId(v.getId());
		}
		for (VersionRB tmp : v.getVersionRubriques()) {
			VersionRB vrb = null;
			if (tmp.getId() != null) {
				vrb = versionRBRepository.find(tmp.getId());
			} else {
				vrb = new VersionRB();
			}
			if (vv.getHas_version_final()) vrb.setHas_version_final(true);
			vrb.setNomVersion(tmp.getNomVersion());
			vrb.setIs_versionFinale(tmp.getIs_versionFinale());
			vrb.setIs_saved(true);
			ls.add(versionRBRepository.save(vrb));
		}
		vv.setAnnee(v.getAnnee());
		vv.setVersionRubriques(ls);
		v = versionRubriqueBudgetaireRepository.save(vv);
		return v;
	}
	
	@Override
	@Transactional
	public List<RubriquesBudgetaire> dupliqueAllRubriqueBudgetaire(
			long id, int annee, boolean fromMaquette) throws Exception {
		List<RubriquesBudgetaire> ls = new ArrayList<>();
		VersionRubriqueBudgetaire vr =
				versionRubriqueBudgetaireRepository
						.all()
						.filter("self.annee=:annee")
						.bind("annee", annee - 1)
						.fetchOne();
		if (vr == null) {
			throw new Exception("Erreur : Aucun Budget dans annee " + (annee - 1) + " n'est valide !");
		}
		VersionRB vrb =
				vr.getVersionRubriques().stream()
						.filter(VersionRB::getIs_versionFinale)
						.findFirst()
						.orElse(null);
		
		if (fromMaquette)
			ls =
					rubriquesBudgetaireRepository
							.all()
							.filter("self.id_version = :ids and self.anneCurrent=:annee")
							.bind("annee", annee - 1)
							.bind("ids", vrb.getId())
							.fetch();
			// rubriquesBudgetaireRepository.all().filter("self.id_version is null").fetch();
		else {
			VersionRubriqueBudgetaire v =
					versionRubriqueBudgetaireRepository
							.all()
							.filter("self.versionRubriques.id=:id")
							.bind("id", id)
							.fetchOne();
			VersionRB vr2 =
					v.getVersionRubriques().stream()
							.filter(versionRB -> versionRB.getId() != id)
							.max(Comparator.comparing(VersionRB::getId))
							.orElse(null);
			ls =
					rubriquesBudgetaireRepository
							.all()
							.filter("self.id_version=:version")
							.bind("version", vr2.getId())
							.fetch();
		}
		List<RubriquesBudgetaire> ls_ = new ArrayList<>();
		List<RubriquesBudgetaire> ls_1 = new ArrayList<>();
		for (RubriquesBudgetaire tmp : ls) {
			RubriquesBudgetaire p = copierRubrique(tmp, fromMaquette);
			p.setId_version(id);
			p.setAnneCurrent(annee);
			ls_.add(rubriquesBudgetaireRepository.save(p));
		}
		ls_ =
				ls_.stream()
						.filter(
								rubriquesBudgetaire -> rubriquesBudgetaire.getId_parent_origine_version() != null)
						.collect(Collectors.toList());
		for (RubriquesBudgetaire tmp : ls_) {
			if (tmp.getId_parent_origine_version() != null) {
				RubriquesBudgetaire t =
						rubriquesBudgetaireRepository
								.all()
								.filter("self.id_origine_version=:id_parent_origine and id_version=:id_version")
								.bind("id_parent_origine", tmp.getId_parent_origine_version())
								.bind("id_version", tmp.getId_version())
								.fetchOne();
				tmp.setBudgetParent(t);
				ls_1.add(rubriquesBudgetaireRepository.save(tmp));
			}
		}
		// set parent rubrique budgetaire
		return ls_1;
	}
	
	private RubriquesBudgetaire copierRubrique(RubriquesBudgetaire tmp, boolean fromMaquette) {
		RubriquesBudgetaire r = new RubriquesBudgetaire();
		r.setId_origine_version(tmp.getId());
		r.setDepenceAndRecette(tmp.getDepenceAndRecette());
		r.setIs_principal(tmp.getIs_principal());
		r.setIs_detaille(tmp.getIs_detaille());
		r.setCode_budget(tmp.getCode_budget());
		r.setTitre_budget(tmp.getTitre_budget());
		r.setTypeRubriquePrincipal(tmp.getTypeRubriquePrincipal());
		r.setTypeRubriqueDetaille(tmp.getTypeRubriqueDetaille());
		r.setShow_row_equipe(tmp.getShow_row_equipe());
		r.setHide_in_principale(tmp.getHide_in_principale());
		r.setHide_montant_fonc(tmp.getHide_montant_fonc());
		r.setShow_row_fonc(tmp.getShow_row_fonc());
		if (fromMaquette) {
			r.setMontant_budget(BigDecimal.ZERO);
			r.setCreditEngagement(BigDecimal.ZERO);
			r.setNouvCredit(BigDecimal.ZERO);
			r.setReportCredit(BigDecimal.ZERO);
			r.setMontant_total_children(BigDecimal.ZERO);
			r.setOld_montant_budget(BigDecimal.ZERO);
			r.setSomme_mtChildren_mtBudget(BigDecimal.ZERO);
			r.setTot_mt_fonc(BigDecimal.ZERO);
		} else {
			r.setMontant_budget(tmp.getMontant_budget());
			r.setCreditEngagement(tmp.getCreditEngagement());
			r.setNouvCredit(tmp.getNouvCredit());
			r.setReportCredit(tmp.getReportCredit());
			r.setMontant_total_children(tmp.getMontant_total_children());
			r.setOld_montant_budget(tmp.getOld_montant_budget());
			r.setSomme_mtChildren_mtBudget(tmp.getSomme_mtChildren_mtBudget());
			r.setTot_mt_fonc(tmp.getTot_mt_fonc());
			
			r.setMontant_old_version(tmp.getMontant_budget());
		}
		if (tmp.getBudgetParent() != null) {
			r.setId_parent_origine_version(tmp.getBudgetParent().getId());
		}
		return r;
	}
	
	@Override
	@Transactional
	public VersionRB updateVersionRb(VersionRB v) {
		return versionRBRepository.save(v);
	}
	
	@Override
	@Transactional
	public VersionRubriqueBudgetaire updateVersionRubriqueBudgetaire(VersionRubriqueBudgetaire v) {
		return versionRubriqueBudgetaireRepository.save(v);
	}
	
	@Override
	@Transactional
	public void dupliquerRubriqueGenerale(VersionRB vrb) {
		List<RubriquesBudgetaire> ls =
				rubriquesBudgetaireRepository
						.all()
						.filter("self.id_version=:id")
						.bind("id", vrb.getId())
						.fetch();
		for (RubriquesBudgetaire tmp : ls) {
			RubriqueBudgetaireGenerale rg = createRubriquebudgetaireGeneraleWithoutParent(tmp);
			tmp.setId_rubrique_generale(rg.getId());
			BudgetParRubrique bpr = createBudgetParRubrique(rg, tmp);
			tmp.setId_budget_par_rubrique_generale(bpr.getId());
			rubriquesBudgetaireRepository.save(tmp);
		}
		generateLiaisonParentRubriqueGenerale(ls);
	}
	
	@Transactional
	private void generateLiaisonParentRubriqueGenerale(List<RubriquesBudgetaire> ls) {
		for (RubriquesBudgetaire r : ls) {
			if (r.getBudgetParent() != null && r.getBudgetParent().getId_rubrique_generale() != null) {
				RubriqueBudgetaireGenerale rg =
						rubriqueBudgetaireGeneraleRepository.find(r.getId_rubrique_generale());
				RubriqueBudgetaireGenerale r_parent =
						rubriqueBudgetaireGeneraleRepository.find(
								r.getBudgetParent().getId_rubrique_generale());
				rg.setParent(r_parent);
				rubriqueBudgetaireGeneraleRepository.save(rg);
			}
		}
	}
	
	@Transactional
	private RubriqueBudgetaireGenerale createRubriquebudgetaireGeneraleWithoutParent(
			RubriquesBudgetaire tmp) {
		RubriqueBudgetaireGenerale rg = new RubriqueBudgetaireGenerale();
		rg.setAnnee(tmp.getAnneCurrent());
		rg.setName(tmp.getTitre_budget());
		rg.setCodeBudg(tmp.getCode_budget());
		rg.setType(tmp.getDepenceAndRecette() ? "Recettes" : "Depenses");
		return rubriqueBudgetaireGeneraleRepository.save(rg);
	}
	
	@Override
	@Transactional
	public void update_montant_Fonc(Long version) {
		String req =
				"update configuration_rubriques_budgetaire x set tot_mt_fonc = ( select case when x.hide_montant_fonc is true then 0 else (montant_budget + montant_total_children) end ) where show_row_fonc is true and id_version = "
						+ version
						+ " ;";
		RunSqlRequestForMe.runSqlRequest(req);
	}
	
	@Override
	@Transactional
	public void update_field_equipe(Long version) {
		String req =
				"update configuration_rubriques_budgetaire x set equip_mt_report_credit = ( select case when x.hide_mt_equipe is true then 0 when report_credit is null then somme_mt_children_mtreport_credit when  somme_mt_children_mtreport_credit is null then report_credit else (report_credit + somme_mt_children_mtreport_credit) end ), equip_mt_nouv_credit = ( select case when x.hide_mt_equipe is true then 0 when nouv_credit is null then somme_mt_children_mtnouv_credit when somme_mt_children_mtnouv_credit is null then nouv_credit else (nouv_credit + somme_mt_children_mtnouv_credit) end ), equip_mt_credit_engagement =( select case when x.hide_mt_equipe is true then 0 when credit_engagement is null then somme_mt_children_credit_engagement when somme_mt_children_credit_engagement is null then credit_engagement else (credit_engagement + somme_mt_children_credit_engagement) end ), equip_mt_montant_Budget = ( select case when x.hide_mt_equipe is true then 0 when montant_budget is null then montant_total_children when montant_total_children is null then montant_budget else (montant_budget + montant_total_children) end ) where show_row_equipe is true and id_version = "
						+ version
						+ " ;";
		RunSqlRequestForMe.runSqlRequest(req);
	}
	
	@Override
	@Transactional
	public void setMontantStart(Long id_rb) {
		String req =
				"update configuration_rubriques_budgetaire x set budget_start = ( select case when montant_budget is null then montant_total_children when montant_total_children is null then montant_budget else (montant_budget + montant_total_children) end ) where id_version ="
						+ id_rb
						+ " ;";
		RunSqlRequestForMe.runSqlRequest(req);
	}
	
	@Override
	@Transactional
	public void update_Bordereau(Bordereau tmp) {
		bordereauRepository.save(tmp);
	}
	
	public Map<String, Long> getIdFonc() {
		Map<String, Long> map = new HashMap<>();
		
		map.put("achat_consome_mat", 15L);
		map.put("autres_charges_externes", 16L);
		map.put("impots_et_taxes", 17L);
		map.put("charges_personnel", 18L);
		map.put("autres_charges_exploitation", 19L);
		map.put("charges_interets", 20L);
		map.put("subvention_accordees", 21L);
		
		return map;
	}
	
	public Map<String, Long> getIdEquip() {
		Map<String, Long> map = new HashMap<>();
		
		map.put("immobilisation_en_non_valeurs", 22L);
		map.put("terrains", 23L);
		map.put("construction", 24L);
		map.put("materiel_transport", 25L);
		map.put("mobilier_bureau_amenagement_divers", 26L);
		map.put("titres_participation", 27L);
		map.put("plan_developpement_CCISBK", 28L);
		
		return map;
	}
	
	public BigDecimal calcule_fonc(int annee, Long version, Long id_type) {
		BigDecimal res = BigDecimal.ZERO;
		List<RubriquesBudgetaire> ls =
				rubriquesBudgetaireRepository
						.all()
						.filter(
								"self.hide_montant_fonc is false and self.show_row_fonc is true and "
										+ "self.id_version = :version and self.anneCurrent=:annee and self.typeRubriqueDetaille.id=:type_id")
						.bind("version", version)
						.bind("annee", annee)
						.bind("type_id", id_type)
						.fetch();
		res =
				ls.stream()
						.map(RubriquesBudgetaire::getTot_mt_fonc)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		return res;
	}
	
	public BigDecimal calcule_fonc_d(int annee, Long version, Long id_type) {
		BigDecimal res = BigDecimal.ZERO;
		List<RubriquesBudgetaire> ls =
				rubriquesBudgetaireRepository
						.all()
						.filter(
								"self.id_version = :version and self.anneCurrent=:annee and self.typeRubriqueDetaille.id=:type_id")
						.bind("version", version)
						.bind("annee", annee)
						.bind("type_id", id_type)
						.fetch();
		res =
				ls.stream()
						.map(RubriquesBudgetaire::getMontant_budget)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		return res;
	}
}
