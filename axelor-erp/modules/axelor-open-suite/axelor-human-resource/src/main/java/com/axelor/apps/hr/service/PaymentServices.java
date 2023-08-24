package com.axelor.apps.hr.service;

import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.repo.*;
import com.axelor.apps.configuration.service.ServiceUtil;
import com.axelor.apps.hr.db.EtatSalaire;
import com.axelor.apps.hr.db.GestionCredit;
import com.axelor.apps.hr.db.repo.GestionCreditRepository;
import com.axelor.apps.purchase.db.EtatSalaire_ops;
import com.axelor.apps.purchase.db.OrderPaymentCommande;
import com.axelor.apps.purchase.db.repo.EtatSalaire_opsRepository;
import com.axelor.apps.purchase.db.repo.OrderPaymentCommandeRepository;
import com.axelor.inject.Beans;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PaymentServices {
  private Map<String, String[]> map_beneficaire = new HashMap<>();
  private Integer mois;
  private Integer year;
  private static final String is_fournisseur_intern = "fournisseur_interne";
  private static final String is_fournisseur_extern = "fournisseur_externe";

  public PaymentServices() {
    Map_beneficaireInitialiser();
  }

  public void Map_beneficaireInitialiser() {
    List<IntitulerCredit> credit = Beans.get(IntitulerCreditRepository.class).all().fetch();
    this.map_beneficaire.put(
        "001_salaire",
        new String[] {
          "Personnel de la CCIS Béni - Mellal Khénifra",
          "Salaires du mois",
          "617111",
          is_fournisseur_intern
        });
    this.map_beneficaire.put(
        "002_indem_log",
        new String[] {
          "Personnel de la CCIS Béni - Mellal Khénifra",
          "Ind. de log du mois",
          "617131",
          is_fournisseur_intern
        });
    this.map_beneficaire.put(
        "003_indem_fonc",
        new String[] {
          "Personnel de la CCIS Béni - Mellal Khénifra",
          "Ind. de fon du mois",
          "617133",
          is_fournisseur_intern
        });
    this.map_beneficaire.put(
        "004_indem_voit",
        new String[] {
          "Personnel de la CCIS Béni - Mellal Khénifra",
          "Ind. de voit du mois",
          "617135",
          is_fournisseur_intern
        });
    this.map_beneficaire.put(
        "005_indem_rep",
        new String[] {
          "Personnel de la CCIS Béni - Mellal Khénifra",
          "Ind. de rep du mois",
          "617136",
          is_fournisseur_intern
        });
    this.map_beneficaire.put(
        "006_taxIr",
        new String[] {
          "Direction générale des impôts.", "IR/ Salaires du mois", "617111", is_fournisseur_extern
        });
    this.map_beneficaire.put(
        "007_taxLog",
        new String[] {
          "Direction générale des impôts.",
          "IR/Indemnité de logement du mois",
          "617111",
          is_fournisseur_extern
        });
    this.map_beneficaire.put(
        "008_RCARCS",
        new String[] {
          "CDG pour compte RCAR", "Cotisations Salariales du mois", "617111", is_fournisseur_extern
        });
    this.map_beneficaire.put(
        "009_RCARCSRC",
        new String[] {
          "CDG pour compte RCAR",
          "Cotisations Salariales RC sur ind de log du mois",
          "617111",
          is_fournisseur_extern
        });
    this.map_beneficaire.put(
        "010_AMOCNOPS",
        new String[] {
          "AMO-CNOPS", "Cotisations Salariales du mois", "617111", is_fournisseur_extern
        });
    this.map_beneficaire.put(
        "011_MGPAPSM311",
        new String[] {"MGPAP", "SM khénifra du mois", "617111", is_fournisseur_extern});
    this.map_beneficaire.put(
        "012_MGPAPSM312",
        new String[] {"MGPAP", "SM kheribga du mois", "617111", is_fournisseur_extern});
    this.map_beneficaire.put(
        "013_MGPAPCCD311",
        new String[] {"MGPAP CB", "CCD khénifra du mois", "617111", is_fournisseur_extern});
    this.map_beneficaire.put(
        "014_MGPAPCCD312",
        new String[] {"MGPAP CB", "CCD kheribga du mois", "617111", is_fournisseur_extern});
    this.map_beneficaire.put(
        "015_OMFAMSM",
        new String[] {"Mutuelle OMFAM", "SM du mois", "617111", is_fournisseur_extern});
    this.map_beneficaire.put(
        "016_OMFAMCAAD",
        new String[] {"Mutuelle OMFAM", "CAAD du mois", "617111", is_fournisseur_extern});
    for (IntitulerCredit tmp : credit) {
      this.map_beneficaire.put(
          tmp.getId().toString(),
          new String[] {
            tmp.getName(), "Précomptes sur salaires du mois", "617111", is_fournisseur_extern
          });
    }
    this.map_beneficaire.put(
        "999_CNSS",
        new String[] {"CNSS", "Précomptes sur salaires du mois", "617111", is_fournisseur_extern});
  }

  @Transactional
  public EtatSalaire_ops saveEtatSalaireOP(EtatSalaire_ops etatSalaire_ops) {
    return Beans.get(EtatSalaire_opsRepository.class).save(etatSalaire_ops);
  }

  @Transactional
  public void createOrderPaymentRH(EtatSalaire_ops etatSalaire_ops) throws Exception {
    this.mois = etatSalaire_ops.getMois();
    this.year = etatSalaire_ops.getYear();
    if (etatSalaire_ops.getEtatSalaireList().size() == 0)
      throw new Exception("Aucune Etat salaire trouvée");

    ArrayList<String> keys = new ArrayList<String>(map_beneficaire.keySet());
    Collections.sort(keys);
    for (int i = 0; i < keys.size(); i++) {
      String[] data = (String[]) map_beneficaire.get(keys.get(i));
      Map<String, Object> map_donnee =
          getMontantViementByKeys(keys.get(i), etatSalaire_ops.getEtatSalaireList());
      OrderPaymentCommande ordre = new OrderPaymentCommande();
      ordre.setDateOrdre(LocalDate.now());
      ordre.setYear(LocalDate.now().getYear());
      ordre.setId_reference(etatSalaire_ops.getId());
      ordre.setNature_operation("EtatSalaireOps");
      ordre.setIs_rh_module(true);
      ordre.setBeneficiaire(data[0]);
      ordre.setObjet_depences(data[1] + " : " + mois + "/" + year);
      ordre.setRubriqueBudgetaire(this.getGubriqueBudgetaireValide(data[2], year));
      ordre.setSommeVerement((BigDecimal) map_donnee.get("montant"));
      ordre.setCompteBenef(null);
      ordre.setIs_fournisseur_interne(data[3].equals(is_fournisseur_intern));
      if (!ordre.getIs_fournisseur_interne()) ordre.setRib(map_donnee.get("rib").toString());
      ordre.setNumero(
          String.format(
                  "%02d",
                  (Beans.get(OrderPaymentCommandeRepository.class)
                          .all()
                          .filter("self.year=:year")
                          .bind("year", LocalDate.now().getYear())
                          .count()
                      + 1))
              + "/"
              + LocalDate.now().format(DateTimeFormatter.ofPattern("yy")));
      ordre = Beans.get(OrderPaymentCommandeRepository.class).save(ordre);
    }
  }

  private Map<String, Object> getMontantViementByKeys(
      String key, Set<EtatSalaire> etatSalaireList) {
    BigDecimal res = BigDecimal.ZERO;
    String ribBeneficiaire = "";
    Map<String, Object> map = new HashMap<>();
    switch (key) {
      case "001_salaire":
        // salaire sans indemnite
        res =
            etatSalaireList.stream()
                .map(EtatSalaire::getNetAPayer)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(
                    (BigDecimal)
                        getMontantViementByKeys("005_indem_rep", etatSalaireList).get("montant"))
                .subtract(
                    (BigDecimal)
                        getMontantViementByKeys("003_indem_fonc", etatSalaireList).get("montant"))
                .subtract(
                    (BigDecimal)
                        getMontantViementByKeys("004_indem_voit", etatSalaireList).get("montant"))
                .subtract(
                    (BigDecimal)
                        getMontantViementByKeys("002_indem_log", etatSalaireList).get("montant"));

        break;
      case "002_indem_log":
        // indemnite de logement
        res =
            etatSalaireList.stream()
                .map(EtatSalaire::getIndemniteLogemBrut)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        res =
            res.subtract(
                ((res.subtract((res.multiply(new BigDecimal("0.03")))))
                        .multiply(new BigDecimal("0.38")))
                    .add(res.multiply(new BigDecimal("0.03"))));

        break;
      case "003_indem_fonc":
        // indemnite de fonction
        res =
            etatSalaireList.stream()
                .map(EtatSalaire::getIndemnitFonctionNet)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        break;
      case "004_indem_voit":
        // indemnite de voiture
        res =
            etatSalaireList.stream()
                .map(EtatSalaire::getIndemnitVoitureNet)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        break;
      case "005_indem_rep":
        // indemnite de representation
        res =
            etatSalaireList.stream()
                .map(EtatSalaire::getIndemnitRepresentNet)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        break;
      case "006_taxIr":
        // tax IR
        res =
            etatSalaireList.stream()
                .map(EtatSalaire::getiR)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        res =
            res.subtract(
                (BigDecimal) getMontantViementByKeys("007_taxLog", etatSalaireList).get("montant"));
        ribBeneficiaire = getRibByKeyAndYear(key, year);
        break;
      case "007_taxLog":
        // tax Logement
        res =
            etatSalaireList.stream()
                .map(EtatSalaire::getIndemniteLogemBrut)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        res =
            ((res.subtract((res.multiply(BigDecimal.valueOf(0.03)))))
                .multiply(BigDecimal.valueOf(0.38)));
        ribBeneficiaire = getRibByKeyAndYear(key, year);
        break;
      case "008_RCARCS":
        // RCAR
        res =
            etatSalaireList.stream()
                .map(
                    etatSalaire ->
                        etatSalaire
                            .getRcarRg()
                            .add(etatSalaire.getRcar_rappel())
                            .add(etatSalaire.getRcarRComp())
                            .add(etatSalaire.getComp_rappel())
                            .subtract(
                                etatSalaire
                                    .getIndemniteLogemBrut()
                                    .multiply(BigDecimal.valueOf(0.03))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ribBeneficiaire = getRibByKeyAndYear(key, year);

        break;
      case "009_RCARCSRC":
        // RCAR LOGEMENT
        res =
            etatSalaireList.stream()
                .map(EtatSalaire::getIndemniteLogemBrut)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        res = res.multiply(BigDecimal.valueOf(0.03));
        ribBeneficiaire = getRibByKeyAndYear(key, year);
        break;

      case "010_AMOCNOPS":
        // AMO
        res =
            etatSalaireList.stream()
                .map(etatSalaire -> etatSalaire.getAmo().add(etatSalaire.getAmo_rappel()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ribBeneficiaire = getRibByKeyAndYear(key, year);
        break;

      case "013_MGPAPCCD311":
        // mgpap ccd
        res = getMontantMGPAPCCDByAnnexe(etatSalaireList, 311);
        break;

      case "014_MGPAPCCD312":
        // mgpap ccd
        res = getMontantMGPAPCCDByAnnexe(etatSalaireList, 312);
        break;

      case "011_MGPAPSM311":
        // mgpap ccd
        res = getMontantMGPAPSMByANNEXE(etatSalaireList, 311);
        break;

      case "012_MGPAPSM312":
        // mgpap ccd
        res = getMontantMGPAPSMByANNEXE(etatSalaireList, 312);
        break;

      case "015_OMFAMSM":
        // OMFAM SM
        res =
            etatSalaireList.stream()
                .map(
                    etatSalaire ->
                        etatSalaire
                            .getMutuelleOmfamSm()
                            .add(etatSalaire.getMutuelleOmfamSm_rappel()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        break;

      case "016_OMFAMCAAD":
        // MGPAP CB
        res =
            etatSalaireList.stream()
                .map(
                    etatSalaire ->
                        etatSalaire
                            .getMutuelleOmfamCaad()
                            .add(etatSalaire.getMutuelleOmfamCaad_rappel()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        break;

      case "999_CNSS":
        // CNSS
        res =
            etatSalaireList.stream()
                .map(
                    etatSalaire -> {
                      return etatSalaire
                          .getEmployee()
                          .getMontant_cnss()
                          .add(etatSalaire.getCnss_rappel());
                    })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        break;

      default:
        LocalDate date =
            LocalDate.parse(
                "01/" + String.format("%02d", mois) + "/" + year,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        date = date.plusMonths(1).minusDays(1);

        res =
            Beans.get(GestionCreditRepository.class)
                .all()
                .filter(
                    ":date between self.dateDebut and self.dateFin and self.intituler.id =:id_intituler")
                .bind("date", date.plusMonths(1L).minusDays(1L))
                .bind("id_intituler", Long.valueOf(key))
                .fetchStream()
                .map(GestionCredit::getRemboursement)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    map.put("montant", res);
    map.put("rib", ribBeneficiaire);
    return map;
  }

  private String getRibByKeyAndYear(String key, Integer year) {
    String res = "";
    switch (key) {
      case "006_taxIr":
      case "007_taxLog":
        res = getIrBanqueInfoByYear(year).getRib();
        break;

      case "008_RCARCS":
      case "009_RCARCSRC":
        res = getRibRcar(year);
        break;

        /*case "010_AMOCNOPS":
        MUTUELLE mutuelle = MUTUELLE
        res = Beans.get(MUTUELLERepository.class).all()
        		.filter("slef.mutuelle.id=:id")
        		.bind("id",)*/

    }
    return res;
  }

  private String getRibRcar(int year) {
    String ribBeneficiaire = "";
    RCAR rcar =
        Beans.get(RCARRepository.class)
            .all()
            .filter("self.annee=:year")
            .bind("year", year)
            .fetchOne();
    if (rcar != null) ribBeneficiaire = rcar.getRib();
    return ribBeneficiaire;
  }

  @Transactional
  private IrBanqueInfo getIrBanqueInfoByYear(int year) {
    IrBanqueInfo info =
        Beans.get(IrBanqueInfoRepository.class)
            .all()
            .filter("self.year=:year")
            .bind("year", year)
            .fetchOne();
    if (info == null) {
      info = new IrBanqueInfo();
      IrBanqueInfo info_old =
          Beans.get(IrBanqueInfoRepository.class)
              .all()
              .fetchStream()
              .max(Comparator.comparing(IrBanqueInfo::getYear))
              .orElse(this.getdefaultIrBanqueInfo(year - 1));
      info.setYear(year);
      info.setNamBank(info_old.getNamBank());
      info.setRib(info_old.getRib());
      info = Beans.get(IrBanqueInfoRepository.class).save(info);
    }
    return info;
  }

  @Transactional
  private IrBanqueInfo getdefaultIrBanqueInfo(int year) {
    IrBanqueInfo def = new IrBanqueInfo();
    def.setNamBank("Default_name");
    def.setRib("000000000000000000000000");
    def.setYear(year);
    return Beans.get(IrBanqueInfoRepository.class).save(def);
  }

  private BigDecimal getMontantMGPAPCCDByAnnexe(Set<EtatSalaire> etatSalaireList, int id_annexe) {
    return etatSalaireList.stream()
        .map(
            etatSalaire -> {
              if (etatSalaire.getEmployee().getAnnexe().getId() == id_annexe)
                return etatSalaire
                    .getMutuelleMgpapCcd()
                    .add(etatSalaire.getMutuelleMgpapCcd_rappel());
              return BigDecimal.ZERO;
            })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal getMontantMGPAPSMByANNEXE(Set<EtatSalaire> etatSalaireList, int id_annexe) {
    return etatSalaireList.stream()
        .map(
            etatSalaire -> {
              if (etatSalaire.getEmployee().getAnnexe().getId() == id_annexe) {
                return etatSalaire
                    .getMutuelleMgpapSM()
                    .add(etatSalaire.getMutuelleMgpapSM_rappel());
              }
              return BigDecimal.ZERO;
            })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private RubriqueBudgetaireGenerale getGubriqueBudgetaireValide(String code_budget, Integer year) {
    RubriqueBudgetaireGenerale resultat = null;
    try {
      RubriquesBudgetaire rub =
          Beans.get(ServiceUtil.class)
              .getRubriqueBudgetaireGeneraleByAnneeAndCode(year, code_budget);
      resultat =
          Beans.get(RubriqueBudgetaireGeneraleRepository.class).find(rub.getId_rubrique_generale());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return resultat;
  }
}
