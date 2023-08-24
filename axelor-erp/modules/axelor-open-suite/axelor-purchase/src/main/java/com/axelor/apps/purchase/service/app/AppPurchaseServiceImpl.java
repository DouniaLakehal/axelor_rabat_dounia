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
package com.axelor.apps.purchase.service.app;

import com.axelor.apps.base.db.AppPurchase;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.repo.AppPurchaseRepository;
import com.axelor.apps.base.db.repo.CompanyRepository;
import com.axelor.apps.base.service.app.AppBaseServiceImpl;
import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.repo.*;
import com.axelor.apps.configuration.service.ConvertNomreToLettres;
import com.axelor.apps.configuration.service.RunSqlRequestForMe;
import com.axelor.apps.purchase.db.*;
import com.axelor.apps.purchase.db.repo.*;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.checkerframework.checker.units.qual.s;

@Singleton
public class AppPurchaseServiceImpl extends AppBaseServiceImpl implements AppPurchaseService {

  @Inject private AppPurchaseRepository appPurchaseRepo;

  @Inject private CompanyRepository companyRepo;

  @Inject private PurchaseConfigRepository purchaseConfigRepo;
  @Inject private AnnexeGeneraleRepository annexeGeneraleRepository;
  @Inject private ConsultationPrixRepository consultationPrixRepository;
  @Inject private ArticleRepository articleRepository;
  @Inject private ComparaisonPrixRepository comparaisonPrixRepository;
  @Inject private FournisseurRepository fournisseurRepository;
  @Inject private ArticlesPrixRepository articlesPrixRepository;
  @Inject private ArticleDetailsRepository articleDetailsRepository;
  @Inject private StockAchatRepository stockAchatRepository;
  @Inject private HistoriqueStockRepository historiqueStockRepository;
  @Inject private DemandeAchatRepository demandeAchatRepository;
  @Inject private CommandeAchatRepository commandeAchatRepository;
  @Inject private RubriqueBudgetaireGeneraleRepository rubriqueBudgetaireGeneraleRepository;
  @Inject private BudgetParRubriqueRepository budgetParRubriqueRepository;
  @Inject private CompteRepository compteRepository;
  @Inject private HistoriqueBudgetaireRepository historiqueBudgetaireRepository;
  @Inject private SelectFournisseurRepository selectFournisseurRepository;
  @Inject private HistoriqueCompteRepository historiqueCompteRepository;
  @Inject private VehiculesRepository vehiculesRepository;
  @Inject private DemandeEntretienRepository demandeEntretienRepository;
  @Inject private ReceptionCommandeRepository receptionCommandeRepository;
  @Inject private OrderPaymentCommandeRepository orderPaymentCommandeRepository;
  @Inject private OrdrevirementCommandeRepository ordrevirementCommandeRepository;
  @Inject private OffreAppelRepository offreAppelRepository;
  @Inject private SociecteRepository sociecteRepository;
  @Inject private StatutRepository statutRepository;
  @Inject private ArticlesRecuRepository articlesRecuRepository;
  @Inject private SoumissionnaireRepository soumissionnaireRepository;
  @Inject private PiecesJointe2Repository piecesJointe2Repository;
  @Inject private CritereNotationRepository critereNotationRepository;
  @Inject private CritereNotationDetailRepository critereNotationDetailRepository;
  @Inject private MarcheProvisoireRepository marcheProvisoireRepository;
  @Inject private FactureAchatRepository factureAchatRepository;
  @Inject private LivrablePRepository livrablePRepository;
  @Inject private LivrableDRepository livrableDRepository;

  @Override
  public AppPurchase getAppPurchase() {
    return appPurchaseRepo.all().fetchOne();
  }

  @Override
  @Transactional
  public void generatePurchaseConfigurations() {

    List<Company> companies = companyRepo.all().filter("self.purchaseConfig is null").fetch();

    for (Company company : companies) {
      PurchaseConfig purchaseConfig = new PurchaseConfig();
      purchaseConfig.setCompany(company);
      purchaseConfigRepo.save(purchaseConfig);
    }
  }

  @Transactional
  public void updateComparaisonPrix(Long id) {
    ConsultationPrix c = consultationPrixRepository.find(id);
    List<ComparaisonPrix> com = load_comparaison_by_id(id);
    /*set is deleted*/
    if (com.size() > 0) {
      deletecomparaisonPrix(com);
    }
    generateComparaisonPrix(id, c.getListFournisseur());
  }

  // Private Methode
  @Override
  @Transactional
  public void generateComparaisonPrix(Long id, List<SelectFournisseur> list_f) {
    ConsultationPrix c = consultationPrixRepository.find(id);
    for (SelectFournisseur f : list_f) {
      ComparaisonPrix cp1 = new ComparaisonPrix();
      cp1.setConsultationPrix(c);
      cp1.setFournisseur(fournisseurRepository.find(f.getFournisseurSelect().getId()));
      cp1.setTotal(BigDecimal.valueOf(0));
      cp1.setNbr(c.getArticlesDetailspourconsultation().size());
      cp1 = comparaisonPrixRepository.save(cp1);
    }
  }

  @Transactional
  public void deletecomparaisonPrix(List<ComparaisonPrix> com) {
    for (ComparaisonPrix tmp : com) {
      tmp.setIsDeleted(true);
      comparaisonPrixRepository.save(tmp);
    }
  }

  @Transactional
  public String updateTotal(Long id, Integer tva) {
    ComparaisonPrix cp = Beans.get(ComparaisonPrixRepository.class).find(id);
    BigDecimal sum = new BigDecimal("0");
    for (ArticlesPrix tmp : cp.getArticleprix()) {
      sum = sum.add(tmp.getP_unitaire().multiply(tmp.getArticleprix().getQuantite2()));
    }
    if (tva > 0) sum = sum.add(sum.multiply(BigDecimal.valueOf(tva).divide(new BigDecimal(100))));

    cp.setTotal(sum);
    cp.setTva(tva);

    Beans.get(ComparaisonPrixRepository.class).save(cp);
    return sum + "";
  }

  @Transactional
  public void inject_artilrToComparaison(Long id) {
    ConsultationPrix c = consultationPrixRepository.find(id);
    List<ComparaisonPrix> lc = load_comparaison_by_id(id);
    for (ComparaisonPrix cp1 : lc) {
      Set<ArticlesPrix> lcp = new HashSet<ArticlesPrix>();
      for (ArticleDetails tmp : c.getArticlesDetailspourconsultation()) {
        ArticlesPrix acp = new ArticlesPrix();
        acp.setArticleprix(tmp);
        acp.setP_unitaire(BigDecimal.valueOf(0));
        acp.setPrixtotal(BigDecimal.valueOf(0));
        acp.setComparaisonPrix(cp1);
        lcp.add(articlesPrixRepository.save(acp));
      }
      cp1.setArticleprix(lcp);
      comparaisonPrixRepository.save(cp1);
    }
  }

  private List<ComparaisonPrix> load_comparaison_by_id(Long id) {
    return Beans.get(ComparaisonPrixRepository.class)
        .all()
        .filter("self.consultationPrix=?1 and self.isDeleted=false", id)
        .fetch();
  }

  private List<ComparaisonPrix> load_comparaison_by_id(Long id, Long id_fourn) {
    return Beans.get(ComparaisonPrixRepository.class)
        .all()
        .filter(
            "self.consultationPrix=?1 and self.fournisseur=?2 and self.isDeleted=false",
            id,
            id_fourn)
        .fetch();
  }

  @Transactional
  public int getNombreDemandeAchat(Integer year) {
    return Beans.get(DemandeAchatRepository.class)
        .all()
        .filter("self.year = ?1", year)
        .fetch()
        .size();
  }

  @Transactional
  public ArticleDetails getArticleDetailById(Long id) {
    return Beans.get(ArticleDetailsRepository.class).find(id);
  }

  @Transactional
  public Statut getStatutById(Long id) {
    Statut st = statutRepository.find(id);
    return st;
  }

  @Transactional
  public Sociecte saveSociete(Sociecte c) {
    return sociecteRepository.save(c);
  }

  @Transactional
  public int checkstockByDemandeAchat(ArticleDetails dmd) {
    int res = 0;
    AnnexeGenerale annexe = dmd.getDemandeAchat().getAnnexe();
    Article article = dmd.getArticle();
    if (annexe != null) {
      StockAchat stock =
          stockAchatRepository
              .all()
              .filter(
                  "self.annexe=?1 and self.article=?2 and self.quantity>?3",
                  annexe,
                  article,
                  dmd.getQuantite())
              .fetchOne();
      if (stock != null) {
        res = 1;
      }
    }
    return res;
  }

  @Transactional
  public void retraitStock(Long id) {
    ArticleDetails ad = articleDetailsRepository.find(id);
    StockAchat stock =
        stockAchatRepository
            .all()
            .filter(
                "self.annexe=?1 and self.article=?2",
                ad.getDemandeAchat().getAnnexe(),
                ad.getArticle())
            .fetchOne();
    if (stock != null) {
      Integer newQte = stock.getQuantity() - ad.getQuantite();
      if (newQte >= 0) {
        stock.setQuantity(newQte);
        DemandeAchat demandeAchat =
            Beans.get(DemandeAchatRepository.class).find(ad.getDemandeAchat().getId());
        try {
          stockAchatRepository.save(stock);
          HistoriqueStock history = new HistoriqueStock();
          history.setAnnexe(ad.getDemandeAchat().getAnnexe());
          history.setArticle(ad.getArticle());
          history.setEmployee(ad.getDemandeAchat().getPersonnel());
          history.setDateOperation(LocalDate.now());
          history.setQuantity(ad.getQuantite());
          history.setTypeOperation("Retrait");
          historiqueStockRepository.save(history);
          ad.setEtat("regler");
          demandeAchatRepository.save(demandeAchat);
        } catch (Exception e) {
          e.getMessage();
        }
      }
    }
  }

  public ConsultationPrix getConsultationPrixbyId(Long id) {
    return consultationPrixRepository.find(id);
  }

  public List<ArticlesPrix> getArticlePrixByArticleDetailId(Long id) {
    List<ArticlesPrix> list =
        articlesPrixRepository.all().filter("self.articleprix=?1", id).fetch();
    return list;
  }

  @Transactional
  public void removeArticlePrixfromComparaison(ArticlesPrix articleprix) {
    ComparaisonPrix comparaison =
        comparaisonPrixRepository.find(articleprix.getComparaisonPrix().getId());
    Set<ArticlesPrix> setarticleprix = comparaison.getArticleprix();
    setarticleprix.remove(articleprix);
    comparaison.setArticleprix(setarticleprix);
    comparaison.setNbr(setarticleprix.size());
    comparaisonPrixRepository.save(comparaison);
  }

  @Transactional
  public void removeArticlePrix(ArticlesPrix articleprix) {
    ArticlesPrix articlesPrix = articlesPrixRepository.find(articleprix.getId());
    articlesPrixRepository.remove(articlesPrix);
  }

  @Transactional
  public void removeArticleDetail(ArticleDetails tmp) {
    ArticleDetails a = articleDetailsRepository.find(tmp.getId());
    articleDetailsRepository.remove(a);
  }

  public ComparaisonPrix getComparaisonPrixById(Long id_compPrix) {
    return comparaisonPrixRepository.find(id_compPrix);
  }

  public OffreAppel getOffreById(Long idOffre) {
    return offreAppelRepository.find(idOffre);
  }

  @Override
  public List<ComparaisonPrix> getListActiveComparaisonPrix(Long id_compPrix) {
    ComparaisonPrix c = getComparaisonPrixById(id_compPrix);
    List<ComparaisonPrix> list =
        comparaisonPrixRepository
            .all()
            .filter(
                "self.consultationPrix=?1 and self.isDeleted=?2",
                c.getConsultationPrix().getId(),
                "false")
            .fetch();
    return list;
  }

  @Override
  public int getNombreCommande(int year) {
    List<CommandeAchat> list = commandeAchatRepository.all().filter("self.year=?1", year).fetch();
    return list.size();
  }

  @Transactional
  public CommandeAchat saveCommandeAchat(CommandeAchat c) {
    return commandeAchatRepository.save(c);
  }

  @Transactional
  public BigDecimal getBudgetByrubriqueBudgetaire(RubriqueBudgetaireGenerale rub, int year) {
    RubriqueBudgetaireGenerale r = rubriqueBudgetaireGeneraleRepository.find(rub.getId());
    BudgetParRubrique budgetParRubrique =
        budgetParRubriqueRepository
            .all()
            .filter("self.rubriqueBudgetaire=?1 and self.year=?2", r.getId(), year)
            .fetchOne();
    return budgetParRubrique == null ? BigDecimal.ZERO : budgetParRubrique.getMontant();
  }

  @Override
  @Transactional
  public void retraitMontantCommande(ComparaisonPrix c) {
    ComparaisonPrix comparaisonPrix = comparaisonPrixRepository.find(c.getId());
    ConsultationPrix consultationPrix =
        consultationPrixRepository.find(comparaisonPrix.getConsultationPrix().getId());
    if (consultationPrix != null
        && consultationPrix.getCompte() != null
        && consultationPrix.getRubriqueBudgetaire() != null) {
      Compte compte = compteRepository.find(consultationPrix.getCompte().getId());
      BigDecimal montantDispo = compte.getMontant();
      BigDecimal newMontant = montantDispo.subtract(comparaisonPrix.getTotal());

      BigDecimal budgetDispo =
          getBudgetByrubriqueBudgetaire(
              consultationPrix.getRubriqueBudgetaire(), LocalDate.now().getYear());
      BigDecimal newBudget = budgetDispo.subtract(comparaisonPrix.getTotal());

      HistoriqueBudgetaire historiqueBudgetaire = new HistoriqueBudgetaire();

      historiqueBudgetaire.setMontant(comparaisonPrix.getTotal());
      historiqueBudgetaire.setMontantRubrique(newBudget);
      historiqueBudgetaire.setRubriqueBudgetaire(consultationPrix.getRubriqueBudgetaire());
      historiqueBudgetaire.setAnnee(LocalDate.now().getYear());
      historiqueBudgetaire.setAnnexe(consultationPrix.getAnnexe());
      historiqueBudgetaire.setDateEx(LocalDate.now());

      BudgetParRubrique budgetParRubrique =
          budgetParRubriqueRepository
              .all()
              .filter(
                  "self.rubriqueBudgetaire=?1 and self.year=?2",
                  consultationPrix.getRubriqueBudgetaire().getId(),
                  LocalDate.now().getYear())
              .fetchOne();
      budgetParRubrique.setMontant(newBudget);

      compte.setMontant(newMontant);

      HistoriqueCompte historiqueCompte = new HistoriqueCompte();
      historiqueCompte.setMontant(c.getTotal());
      historiqueCompte.setAnnexe(c.getConsultationPrix().getAnnexe());
      historiqueCompte.setAction(2); // debit = retirer montant

      historiqueBudgetaireRepository.save(historiqueBudgetaire);
      budgetParRubriqueRepository.save(budgetParRubrique);
      compteRepository.save(compte);
    }
  }

  @Transactional
  public void closeComparaisonPrix(
      List<ComparaisonPrix> list, ComparaisonPrix comparaisonPrix, CommandeAchat commandeAchat) {
    ConsultationPrix consultationPrix =
        getConsultationPrixbyId(comparaisonPrix.getConsultationPrix().getId());
    for (ComparaisonPrix tmp : list) {
      ComparaisonPrix cmp = comparaisonPrixRepository.find(tmp.getId());
      cmp.setHasCommande(true);
      if (tmp.getId() == comparaisonPrix.getId()) {
        tmp.setIdCommandeAchat(commandeAchat.getId());
      }
      comparaisonPrixRepository.save(cmp);
    }
    consultationPrix.setHasCommande(true);
    consultationPrixRepository.save(consultationPrix);
  }

  @Transactional
  public void recevoirCommande(Long idcommande) {
    CommandeAchat commande = commandeAchatRepository.find(idcommande);
    ComparaisonPrix comparaisonPrix =
        comparaisonPrixRepository.find(commande.getComparaisonPrix().getId());
    Set<ArticlesPrix> articlesPrixes = comparaisonPrix.getArticleprix();
    ConsultationPrix consultationPrix =
        consultationPrixRepository.find(comparaisonPrix.getConsultationPrix().getId());
    AnnexeGenerale annexeGenerale = consultationPrix.getAnnexe();
    if (articlesPrixes.size() > 0) {
      for (ArticlesPrix tmp : articlesPrixes) {
        Article a = articleRepository.find(tmp.getArticleprix().getArticle().getId());
        StockAchat stock =
            stockAchatRepository
                .all()
                .filter("self.article=?1 and self.annexe=?2", a, annexeGenerale)
                .fetchOne();
        if (stock != null) {
          Integer newQuantite = tmp.getArticleprix().getQuantite() + stock.getQuantity();
          stock.setQuantity(newQuantite);
          stock = stockAchatRepository.save(stock);
          HistoriqueStock historiqueStock = new HistoriqueStock();
          historiqueStock.setTypeOperation("Ajouter");
          historiqueStock.setEmployee(stock.getUpdatedBy().getEmployee());
          historiqueStock.setArticle(
              articleRepository.find(tmp.getArticleprix().getArticle().getId()));
          historiqueStock.setAnnexe(
              annexeGeneraleRepository.find(
                  commande.getComparaisonPrix().getConsultationPrix().getAnnexe().getId()));
          historiqueStock.setDateOperation(LocalDate.now());
          historiqueStock.setQuantity(tmp.getArticleprix().getQuantite());
          historiqueStockRepository.save(historiqueStock);
        }
      }
    }
    commande.setReception(true);
    commandeAchatRepository.save(commande);
  }

  public String getNumeroConsultation() {
    LocalDateTime date = LocalDateTime.now();
    DateFormat df = new SimpleDateFormat("yy"); // Just the year, with 2 digits
    String year = df.format(Calendar.getInstance().getTime());

    String res =
        year
            + ""
            + date.getDayOfYear()
            + ""
            + date.getHour()
            + date.getMinute()
            + ""
            + date.getSecond()
            + randomString();
    return res;
  }

  public String getNumOffre() {
    List<OffreAppel> lls =
        offreAppelRepository.all().filter("self.yearCode=?1", LocalDate.now().getYear()).fetch();
    String res = "";
    if (lls.size() > 0) {
      res =
          (Integer.parseInt(lls.get(lls.size() - 1).getNumero().split("/")[0]) + 1)
              + "/"
              + LocalDate.now().getYear()
              + "/CCISBK";
    } else {
      res = "1/" + LocalDate.now().getYear() + "/CCISBK";
    }
    /*List<OffreAppel> ls = offreAppelRepository.all().fetch();
    if (ls.size() > 0) {
      OffreAppel o = ls.get(ls.size() - 1);
      Long i = o.getId()+1;
      res = "N-" + i;
    } else {
      res = "N-1";
    }*/
    return res;
  }

  public Fournisseur getFournisseurById(Long idFournisseur) {
    return fournisseurRepository.find(idFournisseur);
  }

  @Transactional
  public void upDateSelectFourn(Long idFournissuer) {
    Fournisseur f = fournisseurRepository.find(idFournissuer);
    List<SelectFournisseur> sf =
        selectFournisseurRepository.all().filter("self.fournisseurSelect=?1", f.getId()).fetch();
    for (SelectFournisseur tmp : sf) {
      tmp.setAdresse(f.getAdresse());
      tmp.setEmail(f.getEmail());
      tmp.setName(f.getName());
      tmp.setTele(f.getTele());
      tmp.setVille(f.getVille());
      selectFournisseurRepository.save(tmp);
    }
  }

  private String randomString() {
    String res = "";
    String upperAlphabet = "ABCDEFGHIJKLMNPQRSTUVWXYZ";
    String lowerAlphabet = "abcdefghijklmnpqrstuvwxyz";
    String numbers = "123456789";
    String alphaNumeric = numbers + upperAlphabet + lowerAlphabet + numbers;
    Random random = new Random();
    int length = 4;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      int index = random.nextInt(alphaNumeric.length());
      char randomChar = alphaNumeric.charAt(index);
      sb.append(randomChar);
    }
    res = sb.toString();
    return res;
  }

  @Transactional
  public void deleteSelectFournisseurByConsultationId(Long id_consultation) {
    List<SelectFournisseur> ls =
        selectFournisseurRepository
            .all()
            .filter("self.consultationPrix=?1", id_consultation)
            .fetch();
    for (SelectFournisseur tmp : ls) {
      selectFournisseurRepository.remove(tmp);
    }
  }

  @Transactional
  public void deleteArticleDetailConsultationId(Long id_articleDetail) {
    ArticleDetails articleDetails = articleDetailsRepository.find(id_articleDetail);
    if (articleDetails != null) {
      deleteArticlePrixByArticleDetailId(articleDetails.getId());
      articleDetailsRepository.remove(articleDetails);
    }
  }

  @Transactional
  private void deleteArticlePrixByArticleDetailId(Long idArticleDetail) {
    List<ArticlesPrix> ls =
        articlesPrixRepository.all().filter("self.articleprix = ?1", idArticleDetail).fetch();
    for (ArticlesPrix tmp : ls) {
      deleteArticlePrixFromComparaisonPrix(tmp.getId());
      articlesPrixRepository.remove(tmp);
    }
  }

  @Transactional
  private void deleteArticlePrixFromComparaisonPrix(Long id_articlePrix) {
    ArticlesPrix articlesPrix = articlesPrixRepository.find(id_articlePrix);
    ComparaisonPrix comparaisonPrix =
        comparaisonPrixRepository.find(articlesPrix.getComparaisonPrix().getId());
    Set<ArticlesPrix> set = comparaisonPrix.getArticleprix();
    Set<ArticlesPrix> res = new HashSet<>();
    for (ArticlesPrix art : set) {
      if (art.getId() != id_articlePrix) res.add(art);
    }
    comparaisonPrix.setArticleprix(res);
    comparaisonPrixRepository.save(comparaisonPrix);
  }

  @Transactional
  public void deleteConsultationPrixById(Long id_consultation) {
    ConsultationPrix tmp = consultationPrixRepository.find(id_consultation);
    consultationPrixRepository.remove(tmp);
  }

  public List<ComparaisonPrix> getAllComparaisonPrixByConsultationPrixId(Long id) {
    return comparaisonPrixRepository.all().filter("self.consultationPrix=?1", id).fetch();
  }

  @Transactional
  public void deletecomparaisonPrixByConsultationPrixId(Long id_consultation) {
    List<ComparaisonPrix> ls = getAllComparaisonPrixByConsultationPrixId(id_consultation);
    for (ComparaisonPrix tmp : ls) {
      deleteArticlePrixByComparaisonPrix(tmp.getId());
      comparaisonPrixRepository.remove(tmp);
    }
  }

  @Transactional
  private void deleteArticlePrixByComparaisonPrix(Long id_comparaisonPrix) {
    ComparaisonPrix comparaisonPrix = comparaisonPrixRepository.find(id_comparaisonPrix);
    if (comparaisonPrix != null) {
      Set<ArticlesPrix> ls = comparaisonPrix.getArticleprix();
      for (ArticlesPrix tmp : ls) {
        deleteArticlePrixById(tmp.getId());
      }
      comparaisonPrix.setArticleprix(null);
      comparaisonPrixRepository.save(comparaisonPrix);
    }
  }

  @Transactional
  private void deleteArticlePrixById(Long id_ArticlePrix) {
    ArticlesPrix articlesPrix = articlesPrixRepository.find(id_ArticlePrix);
    if (articlesPrix != null) {
      deleteArticleRecuByArticlePrix(articlesPrix.getId());
      deleteArticlePrixFromComparaisonPrix(articlesPrix.getId());
      articlesPrixRepository.remove(articlesPrix);
    }
  }

  @Transactional
  private void deleteArticleRecuByArticlePrix(Long idArticlePrix) {
    ArticlesPrix a = articlesPrixRepository.find(idArticlePrix);
    List<ArticlesRecu> ls =
        articlesRecuRepository.all().filter("self.articleRecuDetail=?", a).fetch();

    for (ArticlesRecu tmp : ls) {
      articlesRecuRepository.remove(tmp);
    }
  }

  @Override
  public CommandeAchat getCommandeAchatById(Long id) {
    return commandeAchatRepository.find(id);
  }

  @Transactional
  public ArticlesPrix saveArticlePrix(ArticlesPrix art) {
    return articlesPrixRepository.save(art);
  }

  public Vehicules getVehiculesById(Long idVehicule) {
    return vehiculesRepository.find(idVehicule);
  }

  public Sociecte getSociete(Long id) {
    return sociecteRepository.find(id);
  }

  @Transactional
  public CommandeAchat creatCommandeAchat(ActionRequest request, ActionResponse response) {
    CommandeAchat commande = new CommandeAchat();
    String objet = request.getContext().get("objet").toString();
    String desc = request.getContext().get("conserne_desc").toString();
    String motif = request.getContext().get("motif").toString();
    String justif = request.getContext().get("justificatif").toString();
    ComparaisonPrix c =
        getComparaisonPrixById(Long.valueOf(request.getContext().get("_id").toString()));
    List<ComparaisonPrix> list =
        getAllComparaisonPrixByConsultationPrixId(c.getConsultationPrix().getId());
    commande.setObjet(objet);
    commande.setConserne_desc(desc);
    commande.setMotif(motif);
    commande.setJustificatif(justif);
    commande.setDateCommande(LocalDate.now());
    String numeros = getNumeroConsultation();
    commande.setNumero(numeros);
    commande.setReception(false);
    commande.setYear(LocalDate.now().getYear());
    commande.setComparaisonPrix(c);
    commande = saveCommandeAchat(commande);
    List<ArticlesPrix> ls = new ArrayList<>(c.getArticleprix());
    List<ArticlesPrix> res = new ArrayList<>();
    for (ArticlesPrix tmp : ls) {
      tmp.setCommandeAchat(commande);
      res.add(saveArticlePrix(tmp));
    }
    commande.setArticleCommande(res);
    saveCommandeAchat(commande);
    closeComparaisonPrix(list, c, commande);
    return commandeAchatRepository.save(commande);
  }

  @Override
  @Transactional
  public ReceptionCommande addReceptionCommande(Long id_commande, String BLN) {
    ReceptionCommande r = new ReceptionCommande();
    r.setCommandeAchat(commandeAchatRepository.find(id_commande));
    r.setDateReception(LocalDate.now());
    r.setBonLivraison(BLN);
    r = receptionCommandeRepository.save(r);
    return r;
  }

  @Transactional
  public OrderPaymentCommande creatOrdrePayementCommande(
      ActionRequest request, ActionResponse response) {
    Long id_commande = null;
    if (request.getContext().get("id") != null) id_commande = (Long) request.getContext().get("id");
    else if (request.getContext().get("_id") != null)
      id_commande = Long.valueOf((Integer) request.getContext().get("_id"));
    else {
      response.setFlash("no id found");
      return null;
    }
    CommandeAchat commandeAchat = getCommandeAchatById(id_commande);
    commandeAchat.setHasPaymentCommande(true);
    commandeAchatRepository.save(commandeAchat);

    OrderPaymentCommande ordre = new OrderPaymentCommande();
    // ordre.setNumero(getNumeroConsultation());
    ordre.setDateOrdre(LocalDate.now());
    ordre.setId_reference(id_commande);
    ordre.setNature_operation("Commande");
    ordre.setFournisseur(
        Beans.get(FournisseurRepository.class)
            .find(commandeAchat.getComparaisonPrix().getFournisseur().getId()));
    ordre.setObjet_depences(commandeAchat.getObjet());
    ordre.setCommandeAchat(commandeAchat);
    ordre.setYear(LocalDate.now().getYear());
    ordre.setBeneficiaire(commandeAchat.getComparaisonPrix().getFournisseur().getName());
    RubriqueBudgetaireGenerale rub =
        rubriqueBudgetaireGeneraleRepository.find(
            commandeAchat
                .getComparaisonPrix()
                .getConsultationPrix()
                .getRubriqueBudgetaire()
                .getId());
    ordre.setRubriqueBudgetaire(rub);
    ordre.setSommeVerement(commandeAchat.getComparaisonPrix().getTotal());
    ordre.setCompteBenef(commandeAchat.getComparaisonPrix().getFournisseur().getRib());
    ordre = orderPaymentCommandeRepository.save(ordre);
    return ordre;
  }

  private String getNumeroBydata(Long id, String s) {
    String x = String.format("%05d", id);
    s = x + s;
    return s;
  }

  public OrderPaymentCommande getOrdreByCommandeId(Long id_commande) {
    return orderPaymentCommandeRepository
        .all()
        .filter("self.commandeAchat=?1", id_commande)
        .fetchOne();
  }

  @Transactional
  @Override
  public void update_historiquecompte(
      String designation,
      String rib,
      BigDecimal montant,
      String description,
      Integer action,
      AnnexeGenerale annexe,
      Long id_compte) {
    HistoriqueCompte historiqueCompte = new HistoriqueCompte();
    historiqueCompte.setDesignation(designation);
    historiqueCompte.setRib(rib);
    historiqueCompte.setMontant(montant);
    historiqueCompte.setDescription(description);
    historiqueCompte.setDateTransaction(LocalDateTime.now());
    historiqueCompte.setAction(action);
    historiqueCompte.setAnnexe(annexe);
    historiqueCompte.setAnneeHistorique(LocalDate.now().getYear());
    String m =
        (LocalDate.now().getMonthValue() < 10)
            ? ("0" + LocalDate.now().getMonthValue())
            : String.valueOf(LocalDate.now().getMonthValue());
    historiqueCompte.setMoisHistorique(m);
    Compte c = compteRepository.find(id_compte);
    historiqueCompte.setCompte(c);
    Beans.get(HistoriqueCompteRepository.class).save(historiqueCompte);
  }

  @Override
  public Compte getCompteById(Long id) {
    return compteRepository.find(id);
  }

  @Override
  public AnnexeGenerale getAnnexeById(Long id) {
    return annexeGeneraleRepository.find(id);
  }

  @Transactional
  public OrderPaymentCommande getOrdrePayementbyId(Long id) {
    return orderPaymentCommandeRepository.find(id);
  }

  public OrdrevirementCommande getordreVirementById(Long id) {
    return ordrevirementCommandeRepository.find(id);
  }

  @Transactional
  public HistoriqueCompte addHistoriqueCompte(Compte c, OrdrevirementCommande ov) {
    BigDecimal montant = c.getMontant();
    List<SelectOP> ls = ov.getSelectOP();
    BigDecimal newBig = new BigDecimal(0);
    for (SelectOP tmp : ls) {
      BigDecimal x = new BigDecimal(tmp.getSomme());
      newBig = newBig.add(x);

      BudgetParRubrique budg =
          budgetParRubriqueRepository
              .all()
              .filter(
                  "self.year=? and self.rubriqueBudgetaire=?",
                  LocalDate.now().getYear(),
                  tmp.getOrderPaymentCommande().getRubriqueBudgetaire())
              .fetchOne();
      if (budg != null) {
        BigDecimal xx = budg.getMontant().subtract(x);
        budg.setMontant(xx);
        budgetParRubriqueRepository.save(budg);

        HistoriqueBudgetaire hist_budg = new HistoriqueBudgetaire();
        hist_budg.setAnnee(LocalDate.now().getYear());
        hist_budg.setMois(LocalDate.now().getMonthValue());
        hist_budg.setMontant(x);
        hist_budg.setMontantRubrique(xx);
        if (checkifSelectOpHasAnnexe(tmp))
          hist_budg.setAnnexe(
              tmp.getOrderPaymentCommande()
                  .getCommandeAchat()
                  .getComparaisonPrix()
                  .getConsultationPrix()
                  .getAnnexe());
        hist_budg.setDateEx(LocalDate.now());
        hist_budg.setRubriqueBudgetaire(tmp.getOrderPaymentCommande().getRubriqueBudgetaire());
        hist_budg = historiqueBudgetaireRepository.save(hist_budg);
        ov.setId_historique_budget(hist_budg.getId());

        RubriquesBudgetaire rs =
            Beans.get(RubriquesBudgetaireRepository.class)
                .all()
                .filter("self.id_rubrique_generale=:id_generale")
                .bind("id_generale", tmp.getOrderPaymentCommande().getRubriqueBudgetaire().getId())
                .fetchOne();

        rs.setMontant_budget(xx);
        Beans.get(RubriquesBudgetaireRepository.class).save(rs);
      }
    }

    montant = montant.subtract(newBig);
    c.setMontant(montant);
    c = compteRepository.save(c);

    HistoriqueCompte h = new HistoriqueCompte();
    h.setMontant(c.getMontant());
    h.setAnnexe(c.getAnnexe());
    h.setCompte(c);
    h.setAction(2); // DEBIT = RETIRER Montant;
    h.setAnneeHistorique(LocalDate.now().getYear());
    String m =
        (LocalDate.now().getMonthValue() < 10)
            ? ("0" + LocalDate.now().getMonthValue())
            : String.valueOf(LocalDate.now().getMonthValue());
    h.setMoisHistorique(m);
    h.setDateTransaction(LocalDateTime.now());
    h.setDepense(newBig);
    h.setRib(c.getRib());
    h = historiqueCompteRepository.save(h);
    return h;
  }

  @Transactional
  public void saveOrdreVirement(OrdrevirementCommande ov) {
    List<SelectOP> ls_op = ov.getSelectOP();
    BigDecimal sum = BigDecimal.ZERO;
    for (SelectOP tmp : ls_op) {
      sum = sum.add(tmp.getOrderPaymentCommande().getSommeVerement());
      OrderPaymentCommande op =
          orderPaymentCommandeRepository.find(tmp.getOrderPaymentCommande().getId());
      if (op.getHaspayer() == false) {
        op.setHaspayer(true);
      }
      orderPaymentCommandeRepository.save(op);
    }
    ov.setMontant(sum);
    ov.setMontantLettre(ConvertNomreToLettres.getStringMontant(sum));
    if (ls_op.size() > 0) {
      Fournisseur fournisseur = null;
      if (!ls_op.get(0).getOrderPaymentCommande().getIs_rh_module())
        fournisseur =
            Beans.get(FournisseurRepository.class)
                .find(ls_op.get(0).getOrderPaymentCommande().getFournisseur().getId());
      OrderPaymentCommande op =
          Beans.get(OrderPaymentCommandeRepository.class)
              .find(ls_op.get(0).getOrderPaymentCommande().getId());
      if (fournisseur != null) {
        ov.setFournisseurName(fournisseur.getName());
        ov.setBanqueName(fournisseur.getBanqueName());
        ov.setRib(fournisseur.getRib());
      }
      if (op != null) {
        ov.setObjetVirement(op.getObjet_depences());
      }
    }
    ordrevirementCommandeRepository.save(ov);
  }

  public String getsommeTotaleString(Long id_ov) {
    String somme_string = "";
    BigDecimal somme = new BigDecimal(0);
    OrdrevirementCommande ov = ordrevirementCommandeRepository.find(id_ov);
    List<SelectOP> ls = ov.getSelectOP();
    for (SelectOP tmp : ls) {
      BigDecimal x = new BigDecimal(tmp.getSomme() == null ? "0" : tmp.getSomme());
      somme = somme.add(x);
    }
    somme_string = ConvertNomreToLettres.getStringMontant(somme);
    return somme_string;
  }

  @Override
  public List<ArticlesPrix> getArticleRestant(Long id_command) {
    List<ArticlesPrix> ls =
        articlesPrixRepository.all().filter("self.commandeAchat=?", id_command).fetch();

    return ls;
  }

  @Override
  public BigDecimal getQuantite_reliquat(Long id_articlePrix) {
    ArticlesPrix p = articlesPrixRepository.find(id_articlePrix);
    int qte_init = p.getArticleprix().getQuantite();

    List<ReceptionCommande> rec =
        receptionCommandeRepository
            .all()
            .filter(
                "self.commandeAchat = ?1 and self.articleRecuDetail.articleRecuDetail = ?2",
                p.getCommandeAchat(),
                p)
            .fetch();
    BigDecimal somme = BigDecimal.ZERO;
    for (ReceptionCommande tmp : rec) {
      List<ArticlesRecu> ls_art = tmp.getArticleRecuDetail();
      for (ArticlesRecu tt : ls_art) {
        somme = somme.add(BigDecimal.valueOf(tt.getQuantiteRecu()));
      }
    }
    somme = BigDecimal.valueOf(qte_init).subtract(somme);
    return somme;
  }

  @Transactional
  public void updateStock(List<ArticlesRecu> ls, AnnexeGenerale annexe) {

    for (ArticlesRecu recu : ls) {
      StockAchat ss;
      int qte = 0;
      Article a = recu.getArticleRecuDetail().getArticleprix().getArticle();
      StockAchat s =
          stockAchatRepository
              .all()
              .filter("self.article=?1 and self.annexe=?2", a, annexe)
              .fetchOne();
      if (s == null) {
        ss = new StockAchat();
        qte = recu.getQuantiteRecu();
      } else {
        ss = s;
        qte = s.getQuantity() + recu.getQuantiteRecu();
      }
      ss.setAnnexe(annexe);
      ss.setArticle(a);
      ss.setQuantity(qte);
      stockAchatRepository.save(ss);

      HistoriqueStock h = new HistoriqueStock();
      h.setArticle(a);
      h.setAnnexe(annexe);
      h.setQuantity(recu.getQuantiteRecu());
      h.setDateOperation(LocalDate.now());
      h.setTypeOperation("Ajouter");
      historiqueStockRepository.save(h);
    }
  }

  @Override
  public boolean commandeHasReliquat(Long id_commandeAchat) {
    CommandeAchat c = commandeAchatRepository.find(id_commandeAchat);
    boolean commande_Not_Complete = false;
    List<ArticlesPrix> ls_article = c.getArticleCommande();
    for (ArticlesPrix p : ls_article) {
      if (getQuantite_reliquat(p.getId()).intValue() != 0 && commande_Not_Complete == false) {
        commande_Not_Complete = true;
      }
    }
    return commande_Not_Complete;
  }

  @Override
  @Transactional
  public void commandeComplete(Long id_commandeAchat) {
    CommandeAchat c = commandeAchatRepository.find(id_commandeAchat);
    c.setReception(true);
    commandeAchatRepository.save(c);
  }

  @Override
  @Transactional
  public void deleteReceptionCommande(Long id_reception) {
    ReceptionCommande r = receptionCommandeRepository.find(id_reception);
    CommandeAchat c = commandeAchatRepository.find(r.getCommandeAchat().getId());
    c.getReceptionCommande().remove(r);
    List<ArticlesRecu> ls = r.getArticleRecuDetail();
    for (ArticlesRecu tmp : ls) {

      AnnexeGenerale n = c.getComparaisonPrix().getConsultationPrix().getAnnexe();
      Article a = tmp.getArticleRecuDetail().getArticleprix().getArticle();
      StockAchat s =
          stockAchatRepository.all().filter("self.article=?1 and self.annexe=?2", a, n).fetchOne();
      if (s != null) {
        int qte_tmp = s.getQuantity() - tmp.getQuantiteRecu();
        s.setQuantity(qte_tmp);
        stockAchatRepository.save(s);

        HistoriqueStock h = new HistoriqueStock();
        h.setAnnexe(n);
        h.setQuantity(tmp.getQuantiteRecu());
        h.setArticle(a);
        h.setDateOperation(LocalDate.now());
        h.setTypeOperation("Retrait");
        historiqueStockRepository.save(h);
      }
    }
    receptionCommandeRepository.remove(r);
    commandeAchatRepository.save(c);
  }

  @Override
  @Transactional
  public void addReceptionToCommandeAchat(Long id_reception) {
    ReceptionCommande receptionCommande = receptionCommandeRepository.find(id_reception);
    CommandeAchat commande =
        commandeAchatRepository.find(receptionCommande.getCommandeAchat().getId());

    if (!commande.getReceptionCommande().contains(receptionCommande)) {
      commande.getReceptionCommande().add(receptionCommande);
      commandeAchatRepository.save(commande);
    }
  }

  @Override
  public HistoriqueStock getHistoriqueStockByArticleAnnexe(Article article, AnnexeGenerale annexe) {
    return historiqueStockRepository
        .all()
        .filter("self.article=?1 and self.annexe=?2", article, annexe)
        .fetchOne();
  }

  @Override
  @Transactional()
  public void saveHistoriqueStock(HistoriqueStock h) {
    historiqueStockRepository.save(h);
  }

  @Override
  @Transactional
  public void saveOffreAppel(Soumissionnaire s) {
    soumissionnaireRepository.save(s);
  }

  @Override
  public Soumissionnaire getSoumissionnaireById(Long id) {
    return soumissionnaireRepository.find(id);
  }

  @Override
  @Transactional
  public void validerPieceJointe2(Long id) {
    PiecesJointe2 p = piecesJointe2Repository.find(id);
    if (p != null) {
      p.setEtat("valide");
      p.setRemarque("");
    }
  }

  private boolean checkifSelectOpHasAnnexe(SelectOP tmp) {
    boolean res = false;
    try {
      AnnexeGenerale x =
          tmp.getOrderPaymentCommande()
              .getCommandeAchat()
              .getComparaisonPrix()
              .getConsultationPrix()
              .getAnnexe();
      res = true;
    } catch (Exception e) {
    }
    return res;
  }

  @Override
  @Transactional
  public void validerReservePieceJointe2(Long id, String comment) {
    PiecesJointe2 p = piecesJointe2Repository.find(id);
    if (p != null) {
      p.setEtat("ValideReserve");
      p.setRemarque(comment);
      piecesJointe2Repository.save(p);
    }
  }

  @Override
  @Transactional
  public void NotvaliderPieceJointe2(Long id, String comment) {
    PiecesJointe2 p = piecesJointe2Repository.find(id);
    if (p != null) {
      p.setEtat("nonValide");
      p.setRemarque(comment);
      piecesJointe2Repository.save(p);
      Soumissionnaire s = soumissionnaireRepository.find(p.getSoumissionnaire().getId());
      if (s.getDossierAdministrative()) {
        s.setEtat("");
        s.setDossierAdministrative(false);
        soumissionnaireRepository.save(s);
      } else {
        s.setEtat("ecarter");
        soumissionnaireRepository.save(s);
      }
    }
  }

  @Transactional
  public boolean attacherFormulaireTechniqueSoumissionnaire(Long id_pieceJointe) {
    PiecesJointe2 p = piecesJointe2Repository.find(id_pieceJointe);
    Soumissionnaire s = soumissionnaireRepository.find(p.getSoumissionnaire().getId());
    OffreAppel o = offreAppelRepository.find(s.getOffreappels().getId());

    boolean allFilesValide = true;
    if (o.getOffreTechnique()) {
      for (PiecesJointe2 tmp : s.getPiecesJointes()) {
        if ((tmp.getEtat() == null || tmp.getEtat().equals("nonValide"))
            && !Objects.equals(tmp.getId(), id_pieceJointe)) {
          allFilesValide = false;
          break;
        }
      }
    }
    if (!s.getDossierAdministrative() && allFilesValide) s.setDossierAdministrative(true);
    return allFilesValide;
  }

  @Transactional
  public void affecterListCritereToSoumiss(Long id_piece) {
    PiecesJointe2 p = piecesJointe2Repository.find(id_piece);
    Soumissionnaire s = soumissionnaireRepository.find(p.getSoumissionnaire().getId());
    OffreAppel o = offreAppelRepository.find(s.getOffreappels().getId());

    if (o.getCritereNotation() != null
        && o.getCritereNotation().size() > 0
        && (s.getCritereNotation() == null || s.getCritereNotation().size() == 0)) {
      List<CritereNotationDetail> ls = new ArrayList<CritereNotationDetail>();
      for (CritereNotationaSelect tmp : o.getCritereNotation()) {
        CritereNotationDetail c = new CritereNotationDetail();
        c.setNoteMax(tmp.getNotationSelect().getNoteMax());
        c.setSoumissionnaire(s);
        c.setTitre(tmp.getNotationSelect().getName());
        c.setOffreappel(tmp.getOffreappel());
        c = critereNotationDetailRepository.save(c);
        ls.add(c);
      }
      s.setCritereNotation(ls);
      soumissionnaireRepository.save(s);
    }
  }

  @Transactional
  public void supprimerListCritereFromSoumiss(Long id_piece) {
    PiecesJointe2 p = piecesJointe2Repository.find(id_piece);
    Soumissionnaire s = soumissionnaireRepository.find(p.getSoumissionnaire().getId());

    if (s.getCritereNotation() != null || s.getCritereNotation().size() > 0) {
      List<CritereNotationDetail> ls = s.getCritereNotation();
      for (CritereNotationDetail tmp : ls) {
        critereNotationDetailRepository.remove(tmp);
      }
      s.setCritereNotation(null);
      soumissionnaireRepository.save(s);
    }
  }

  @Override
  public BigDecimal getPointObtenuByid_soum(Long id_soum) {
    Soumissionnaire s = soumissionnaireRepository.find(id_soum);
    BigDecimal som = BigDecimal.ZERO;
    List<CritereNotationDetail> ls = s.getCritereNotation();
    for (CritereNotationDetail tmp : ls) {
      if (tmp.getNote() != null) som = som.add(tmp.getNote());
    }
    return som;
  }

  @Override
  @Transactional
  public void affecterMarcheToSoumissionnaire(Long id_soum) {
    Soumissionnaire s = soumissionnaireRepository.find(id_soum);
    OffreAppel o = offreAppelRepository.find(s.getOffreappels().getId());
    o.setSoumissGagnant(s);
    o.setOffreFinanciereValide(true);
    offreAppelRepository.save(o);
    s.setHasMarche(true);
    for (Soumissionnaire tmp : o.getSoumissionnaire()) {
      tmp.setCloseOffre(true);
      soumissionnaireRepository.save(tmp);
    }
    soumissionnaireRepository.save(s);
  }

  @Override
  public List<Soumissionnaire> getSoumissionnaireNotValideByIdOffre(Long id_offre) {
    List<Soumissionnaire> ls =
        soumissionnaireRepository
            .all()
            .filter(
                "self.offreappels =?1 and (self.hasMarche is null or self.hasMarche=false)",
                id_offre)
            .fetch();
    return ls;
  }

  @Override
  @Transactional
  public Soumissionnaire getSoumissionnaireSuivant(Long id) {
    OffreAppel o = offreAppelRepository.find(id);
    String filter1 =
        "(self.recalerMarcheProvisoire=false or self.recalerMarcheProvisoire=null) and self.offreappels=?1 and self.id!=?2 and self.dossierAdministrative='true' ";
    String filter2 =
        "(self.recalerMarcheProvisoire=false or self.recalerMarcheProvisoire=null) and self.offreappels=?1 and self.id!=?2 and self.dossierAdministrative='true' and self.dossierTechnique='true'";
    Soumissionnaire res =
        soumissionnaireRepository
            .all()
            .filter(o.getOffreTechnique() ? filter2 : filter1, id, o.getSoumissGagnant().getId())
            .order("offreFiance")
            .fetchOne();
    if (res == null) return o.getSoumissGagnant();
    return res;
  }

  @Override
  @Transactional
  public void saveOffreAppel(OffreAppel o) {
    offreAppelRepository.save(o);
  }

  @Override
  @Transactional
  public void recalerSoumissionnaireGagnant(Long id) {
    Soumissionnaire s_recaler = soumissionnaireRepository.find(id);
    s_recaler.setRecalerMarcheProvisoire(true);
    s_recaler.setHasMarche(false);
    soumissionnaireRepository.save(s_recaler);
  }

  @Override
  @Transactional
  public void saveMarcheProvisoire(MarcheProvisoire m) {
    marcheProvisoireRepository.save(m);
  }

  @Override
  @Transactional
  public void saveLivrableP(LivrableP p) {
    livrablePRepository.save(p);
  }

  @Override
  public MarcheProvisoire getMarcheProvisoire(Long id_marcheProvisoire) {
    return marcheProvisoireRepository.find(id_marcheProvisoire);
  }

  @Override
  @Transactional
  public void createMarcheProvisoire(Long id_soum) {
    Soumissionnaire s = soumissionnaireRepository.find(id_soum);
    OffreAppel o = offreAppelRepository.find(s.getOffreappels().getId());
    MarcheProvisoire m = new MarcheProvisoire();
    m.setComplementAdministrative(false);
    m.setDocumentPapier(false);
    m.setJustifPrix(false);
    m = marcheProvisoireRepository.save(m);
    o.setMarcheProvisoire(m);
    offreAppelRepository.save(o);
  }

  @Override
  public OffreAppel attribuerNumerosMarche(OffreAppel o) {
    o.setYear(LocalDate.now().getYear());
    o.setHasMarche(true);
    List<OffreAppel> ls =
        offreAppelRepository
            .all()
            .filter("self.numMarche is not null and self.year = " + LocalDate.now().getYear())
            .fetch();
    if (ls != null && ls.size() > 0) {
      o.setNumMarche(
          (Integer.valueOf(ls.get(ls.size() - 1).getNumero().split("/")[1]) + 1)
              + "/"
              + LocalDate.now().getYear()
              + "/CCISBK");
    } else {
      o.setNumMarche("1/" + LocalDate.now().getYear() + "/CCISBK");
    }
    return o;
  }

  @Override
  @Transactional
  public void setSoumissionnaireHas_Marche(Long id, boolean etat) {
    Soumissionnaire s = soumissionnaireRepository.find(id);
    s.setHasMarche(etat);
    soumissionnaireRepository.save(s);
  }

  @Override
  public FactureAchat getFactureById(Long id) {
    return factureAchatRepository.find(id);
  }

  @Override
  @Transactional
  public void saveFactureAchat(FactureAchat f) {
    factureAchatRepository.save(f);
  }

  @Override
  @Transactional
  public OrderPaymentCommande createOrdreDePayement(FactureAchat f) {
    OrderPaymentCommande o = new OrderPaymentCommande();
    o.setDateOrdre(LocalDate.now());
    o.setNature_operation("Facture");
    o.setId_reference(f.getId());
    o.setObjet_depences(f.getNatureOperation());
    o.setYear(LocalDate.now().getYear());
    o.setBeneficiaire(f.getFournisseur().getName());
    o.setFournisseur(Beans.get(FournisseurRepository.class).find(f.getFournisseur().getId()));
    RubriqueBudgetaireGenerale rub =
        rubriqueBudgetaireGeneraleRepository.find(f.getRubriqueBudg().getId());
    o.setRubriqueBudgetaire(rub);
    o.setSommeVerement(f.getMontant());
    o.setCompteBenef(f.getFournisseur().getRib());
    o = orderPaymentCommandeRepository.save(o);
    return o;
  }

  @Override
  @Transactional
  public OrderPaymentCommande updateOrdreDePayement(FactureAchat f) {
    OrderPaymentCommande o = orderPaymentCommandeRepository.find(f.getOrdreDePayment().getId());
    o.setBeneficiaire(f.getFournisseur().getName());
    RubriqueBudgetaireGenerale rub =
        rubriqueBudgetaireGeneraleRepository.find(f.getRubriqueBudg().getId());
    o.setRubriqueBudgetaire(rub);
    o.setSommeVerement(f.getMontant());
    o.setCompteBenef(f.getFournisseur().getRib());
    o.setFournisseur(Beans.get(FournisseurRepository.class).find(f.getFournisseur().getId()));
    o.setNature_operation(f.getNatureOperation());
    o = orderPaymentCommandeRepository.save(o);
    return o;
  }

  @Override
  @Transactional
  public void deleteFactureAchat(Long id) {
    FactureAchat f = factureAchatRepository.find(id);
    factureAchatRepository.remove(f);
  }

  @Override
  @Transactional
  public EngagementAchat save_EngagementAchat(EngagementAchat engagementAchat) {
    return Beans.get(EngagementAchatRepository.class).save(engagementAchat);
  }

  @Override
  @Transactional
  public void validerProvisoire(Long id) {
    LivrableP p = livrablePRepository.find(id);
    if (p != null) {
      p.setEtat("valide");
      p.setRemarque("");
    }
  }

  @Override
  @Transactional
  public void validerReserveProvisoire(Long id, String comment) {
    LivrableP p = livrablePRepository.find(id);
    if (p != null) {
      p.setEtat("ValideReserve");
      p.setRemarque(comment);
      livrablePRepository.save(p);
    }
  }

  @Override
  @Transactional
  public void NotvaliderProvisoire(Long id, String comment) {
    LivrableP p = livrablePRepository.find(id);
    if (p != null) {
      p.setEtat("nonValide");
      p.setRemarque(comment);
      livrablePRepository.save(p);
    }
  }

  @Override
  @Transactional
  public void validerDefinitif(Long id) {
    LivrableD p = livrableDRepository.find(id);
    if (p != null) {
      p.setEtat("valide");
      p.setRemarque("");
    }
  }

  @Override
  @Transactional
  public void validerReserveDefinitif(Long id, String comment) {
    LivrableD p = livrableDRepository.find(id);
    if (p != null) {
      p.setEtat("ValideReserve");
      p.setRemarque(comment);
      livrableDRepository.save(p);
    }
  }

  @Override
  @Transactional
  public void NotvaliderDefinitif(Long id, String comment) {
    LivrableD p = livrableDRepository.find(id);
    if (p != null) {
      p.setEtat("nonValide");
      p.setRemarque(comment);
      livrableDRepository.save(p);
    }
  }

  @Override
  @Transactional
  public void updateDateDocument(String limit) {
    RunSqlRequestForMe.runSqlRequest(limit);
  }
}
