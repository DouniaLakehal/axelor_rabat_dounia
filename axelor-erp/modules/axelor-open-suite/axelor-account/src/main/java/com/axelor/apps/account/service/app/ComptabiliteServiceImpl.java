package com.axelor.apps.account.service.app;

import com.axelor.apps.account.db.Afranchissement;
import com.axelor.apps.account.db.repo.AfranchissementRepository;
import com.axelor.apps.account.db.repo.GestionRecetteRepository;
import com.axelor.apps.account.db.repo.VersementRepository;
import com.axelor.apps.base.service.app.AppBaseServiceImpl;
import com.axelor.apps.configuration.db.Compte;
import com.axelor.apps.configuration.db.HistoriqueCompte;
import com.axelor.apps.configuration.db.repo.CompteRepository;
import com.axelor.apps.configuration.db.repo.HistoriqueCompteRepository;
import com.axelor.db.JPA;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Singleton
public class ComptabiliteServiceImpl extends AppBaseServiceImpl implements ComptabiliteService {
  @Inject VersementRepository versementRepository;
  @Inject GestionRecetteRepository gestionRecetteRepository;

  @Transactional
  public void saveFirstAffranchisement(ActionRequest request, ActionResponse response) {
    int code = (int) request.getContext().get("code");
    String beneficiaire = request.getContext().get("beneficiaire").toString();
    String designation = request.getContext().get("designation").toString();
    BigDecimal montant =
        BigDecimal.valueOf(Double.valueOf(request.getContext().get("montant").toString()));
    if (request.getContext().get("id") == null) {
      Afranchissement afranchissement = new Afranchissement();

      afranchissement.setCode(code);
      afranchissement.setBeneficiaire(beneficiaire);
      afranchissement.setDesignation(designation);
      afranchissement.setMontant(montant);
      afranchissement.setDateAjout(LocalDate.now());
      afranchissement.setAnnee(LocalDate.now().getYear());
      Beans.get(AfranchissementRepository.class).save(afranchissement);
    } else {
      Long id = (Long) request.getContext().get("id");
      Afranchissement afranchissement = Beans.get(AfranchissementRepository.class).find(id);

      afranchissement.setCode(code);
      afranchissement.setBeneficiaire(beneficiaire);
      afranchissement.setDesignation(designation);
      afranchissement.setMontant(montant);
      Beans.get(AfranchissementRepository.class).save(afranchissement);
    }
    response.setReload(true);
  }

  public BigDecimal calculTotalAffranch(int annee) {
    List<Afranchissement> listAffranchs = findAllAfranchissementByAnnee(annee);
    BigDecimal somme = new BigDecimal(0);
    if (listAffranchs != null) {

      for (Afranchissement l : listAffranchs) {
        somme = somme.add(l.getMontant());
      }
    }

    return somme;
  }

  public List<Afranchissement> findAllAfranchissementByAnnee(int annee) {
    List<Afranchissement> listAffranchs =
        Beans.get(AfranchissementRepository.class).all().filter("self.annee=?1", annee).fetch();
    return listAffranchs;
  }

  public String ConvertMoisToLettre(int mois) {
    String moisString = "";
    if (mois == 1) {
      moisString = "Janvier";
    } else if (mois == 2) {
      moisString = "Février";
    } else if (mois == 3) {
      moisString = "Mars";
    } else if (mois == 4) {
      moisString = "Avril";
    } else if (mois == 5) {
      moisString = "Mai";
    } else if (mois == 6) {
      moisString = "Juin";
    } else if (mois == 7) {
      moisString = "Juillet";
    } else if (mois == 8) {
      moisString = "Aout";
    } else if (mois == 9) {
      moisString = "Septembre";
    } else if (mois == 10) {
      moisString = "Octobre";
    } else if (mois == 11) {
      moisString = "Novembre";
    } else if (mois == 12) {
      moisString = "Décembre";
    }
    return moisString;
  }

  public BigDecimal getSommeComptes(int annee) {

    BigDecimal somme = new BigDecimal(0);
    List<Compte> listComptes = Beans.get(CompteRepository.class).all().fetch();
    for (Compte l : listComptes) {
      HistoriqueCompte historiqueCompte =
          Beans.get(HistoriqueCompteRepository.class)
              .all()
              .filter(
                  "self.anneeHistorique=? and self.compte=? order by self.id desc",
                  annee,
                  l.getId())
              .fetchOne();
      if (historiqueCompte != null) {
        somme = somme.add(historiqueCompte.getMontant());
      }
    }

    return somme;
  }

  public Object excuteRequetteSommeDepense(int mois, int annee) {
    String req =
        "select case when sum(ordPay.somme_verement) is null then 0 else sum(ordPay.somme_verement) end as sommeOrdPay from purchase_order_payment_commande ordPay where (SELECT date_part('month',ordPay.date_ordre))=?1 and (SELECT date_part('year',ordPay.date_ordre))=?2  ";
    Object etat = null;

    javax.persistence.Query query =
        JPA.em().createNativeQuery(req).setParameter(1, mois).setParameter(2, annee);
    etat = query.getSingleResult();
    if (etat == null) {
      etat = "";
    }

    return etat;
  }

  public BigDecimal CalculSommeRecetteDecimal(int mois, int annee, String datePrecedenteString) {
    BigDecimal totalHistCompte = CalculTotalHistCompte(mois, annee, datePrecedenteString);
    BigDecimal totalVersementMois = CalculTotalVersementMois(mois, annee, datePrecedenteString);
    BigDecimal totalRecette = CalaculTotalRecette(mois, annee);
    BigDecimal totaldepenses = totalHistCompte.add(totalVersementMois).add(totalRecette);
    return totaldepenses;
  }

  public BigDecimal CalculSommeDepenseDecimal(
      int mois, int annee, BigDecimal sommeDepenseDecimal, String datePrecedenteString) {
    BigDecimal totalHistCompte = CalculTotalHistCompte(mois, annee, datePrecedenteString);
    BigDecimal totalVersementMois = CalculTotalVersementMois(mois, annee, datePrecedenteString);
    BigDecimal totalRecette = CalaculTotalRecette(mois, annee);
    BigDecimal totaldepenses =
        totalHistCompte.add(totalVersementMois).add(totalRecette).subtract(sommeDepenseDecimal);
    return totaldepenses;
  }

  public BigDecimal TotalCreditsJournalF2(int mois, int annee, String datePrecedenteString) {

    BigDecimal totalVersementMois = CalculTotalVersementMois(mois, annee, datePrecedenteString);
    BigDecimal totalRecette = CalaculTotalRecette(mois, annee);
    BigDecimal totaldepenses = totalVersementMois.add(totalRecette);
    return totaldepenses;
  }

  public BigDecimal TotalDebitJournalF2(int mois, int annee, String datePrecedenteString) {

    BigDecimal TotalDepense = CalaculTotalDepense(mois, annee);
    BigDecimal TotalOrdrePayementRH = CalaculTotalOrdrePayementRH(mois, annee);
    BigDecimal totaldepenses = TotalDepense.add(TotalOrdrePayementRH);
    return totaldepenses;
  }

  private BigDecimal CalaculTotalRecette(int mois, int annee) {

    //	versementRepository.all().filter("self.", params)
    String req =
        "SELECT case when sum(montant) is null then 0 else sum(montant) end FROM account_gestion_recette where (SELECT date_part('month', date_recette))=?1 and (SELECT date_part('year', date_recette))=?2";
    Object etat = null;

    javax.persistence.Query query =
        JPA.em().createNativeQuery(req).setParameter(1, mois).setParameter(2, annee);
    etat = query.getSingleResult();
    if (etat == null) {
      etat = "";
    }
    float somme = ((BigDecimal) etat).floatValue();

    BigDecimal etatDecimal = new BigDecimal(Float.toString(somme));

    return etatDecimal;
  }

  private BigDecimal CalculTotalVersementMois(int mois, int annee, String datePrecedenteString) {
    //	versementRepository.all().filter("self.", params)
    String req =
        "select case when sum(montant) is null then 0 else sum(montant) end  FROM account_versement where (SELECT date_part('month', date_versement))=?1 and (SELECT date_part('year', date_versement))=?2";
    Object etat = null;

    javax.persistence.Query query =
        JPA.em().createNativeQuery(req).setParameter(1, mois).setParameter(2, annee);
    etat = query.getSingleResult();
    if (etat == null) {
      etat = "";
    }
    float somme = ((BigDecimal) etat).floatValue();

    BigDecimal etatDecimal = new BigDecimal(Float.toString(somme));

    return etatDecimal;
  }

  private BigDecimal CalaculTotalDepense(int mois, int annee) {

    //	versementRepository.all().filter("self.", params)
    String req =
        "select case when sum(ordPay.somme_verement) is null then 0 else sum(ordPay.somme_verement) end as sommeOrdPay from purchase_order_payment_commande ordPay where (SELECT date_part('month',ordPay.date_ordre))=?1 and (SELECT date_part('year',ordPay.date_ordre))=?2  ";
    Object etat = null;

    javax.persistence.Query query =
        JPA.em().createNativeQuery(req).setParameter(1, mois).setParameter(2, annee);
    etat = query.getSingleResult();
    if (etat == null) {
      etat = "";
    }
    float somme = ((BigDecimal) etat).floatValue();

    BigDecimal etatDecimal = new BigDecimal(Float.toString(somme));

    return etatDecimal;
  }

  private BigDecimal CalaculTotalOrdrePayementRH(int mois, int annee) {

    //	versementRepository.all().filter("self.", params)
    String req =
        "select case when sum(montant) is null then 0 else sum(montant)  end from hr_ordre_payement_jf ordjf1 where (select DATE_PART('month', datre_ordrejf))=?1 and (select DATE_PART('year', datre_ordrejf ))=?2 ";
    Object etat = null;

    javax.persistence.Query query =
        JPA.em().createNativeQuery(req).setParameter(1, mois).setParameter(2, annee);
    etat = query.getSingleResult();
    if (etat == null) {
      etat = "";
    }
    float somme = ((BigDecimal) etat).floatValue();

    BigDecimal etatDecimal = new BigDecimal(Float.toString(somme));

    return etatDecimal;
  }

  public BigDecimal CalculTotalHistCompte(int mois, int annee, String datePrecedenteString) {
    String req =
        "select case when sum(hist1.montant) is null then 0 else sum(hist1.montant) end as SommeHist from configuration_historique_compte hist1 where  date_transaction<?1 and  hist1.id=(select max(hist2.id) from configuration_historique_compte hist2 where  date_transaction<?1 and hist2.compte= hist1.compte)";
    Object etat = null;

    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(req)
            .setParameter(1, java.sql.Date.valueOf(datePrecedenteString));
    etat = query.getSingleResult();
    if (etat == null) {
      etat = "";
    }
    float somme = ((BigDecimal) etat).floatValue();

    BigDecimal etatDecimal = new BigDecimal(Float.toString(somme));

    return etatDecimal;
  }

  public Object excuteRequette(int mois, int annee, String code, String req) {
    Object etat = null;

    javax.persistence.Query query =
        JPA.em().createNativeQuery(req).setParameter(1, mois).setParameter(2, annee);
    etat = query.getSingleResult();
    if (etat == null) {
      etat = new String[] {".", "0", "0"};
    }

    return etat;
  }

  public int getTheLastDayofTheMonth(String date) {
    LocalDate convertedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    convertedDate =
        convertedDate.withDayOfMonth(convertedDate.getMonth().length(convertedDate.isLeapYear()));

    return convertedDate.getDayOfMonth();
  }
}
