package com.axelor.apps.hr.service.EtatsalaireService;

import com.axelor.apps.hr.db.EtatSalaireTransaction;

public interface EtatSalaireTransationService {
  boolean checkMontantAlltransaction(int annee, int mois, Long idCompte);

  EtatSalaireTransaction doRetraittransaction(int annee, int mois, Long id);

  void reduceFromRubriqueAndCompte(EtatSalaireTransaction t);

  void removeTransaction(int mois, int annee);
}
