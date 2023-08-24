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
package com.axelor.apps.marketing.service;

import com.axelor.apps.account.db.*;
import com.axelor.apps.account.db.repo.*;
import com.axelor.apps.account.service.move.MoveToolService;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.Period;
import com.axelor.apps.base.db.Year;
import com.axelor.apps.base.db.repo.PartnerRepository;
import com.axelor.apps.base.db.repo.PeriodRepository;
import com.axelor.apps.base.db.repo.YearRepository;
import com.axelor.apps.configuration.service.RunSqlRequestForMe;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CustomServiceImp implements CustomService {
  @Inject YearRepository yearRepository;
  @Inject GroupeMoveRepository groupeMoveRepository;
  @Inject JournalRepository journalRepository;
  @Inject PeriodRepository periodRepository;
  @Inject MoveRepository moveRepository;
  @Inject AccountRepository accountRepository;
  @Inject AccountTypeRepository accountTypeRepository;
  @Inject MoveLineRepository moveLineRepository;
  protected int counter = 0;

  @Override
  @Transactional
  public Year saveYear(Year one_year) {
    return yearRepository.save(one_year);
  }

  @Override
  public List<GroupeMove> getAllGroupeMoveInPersiode(Period period) {
    return groupeMoveRepository.all().filter("self.period=:period").bind("period", period).fetch();
  }

  @Override
  public List<Journal> getAllJournales() {
    return journalRepository.all().filter("self.year is null or self.year=0").fetch();
  }

  @Transactional
  @Override
  public Journal saveJournale(Journal j) {
    return journalRepository.save(j);
  }

  @Transactional
  @Override
  public Move saveMouve(Move m) {
    return moveRepository.save(m);
  }

  @Transactional
  @Override
  public Journal addNewJourale(Long id_journale_ancetre, int annee) {
    Journal parent = journalRepository.find(id_journale_ancetre);
    Journal j_tmp = new Journal();

    j_tmp.setYear(annee);
    j_tmp.setId_journal_parent(id_journale_ancetre);
    j_tmp.setCode(parent.getCode() + "_" + annee);
    j_tmp.setCompany(parent.getCompany());
    j_tmp.setCompatiblePartnerTypeSelect(parent.getCompatiblePartnerTypeSelect());
    j_tmp.setValidAccountTypeSet(parent.getValidAccountTypeSet());
    j_tmp.setName(parent.getName());
    j_tmp.setStatusSelect(1);
    j_tmp.setJournalType(parent.getJournalType());
    j_tmp.setSequence(parent.getSequence());
    j_tmp.setIsInvoiceMoveConsolidated(parent.getIsInvoiceMoveConsolidated());
    j_tmp.setDescriptionIdentificationOk(parent.getDescriptionIdentificationOk());
    Set<AccountType> tmp_ls_type = parent.getValidAccountTypeSet();
    Set<AccountType> accountTypes = new LinkedHashSet<>();
    for (AccountType tmp : tmp_ls_type) {
      accountTypes.add(accountTypeRepository.find(tmp.getId()));
    }
    j_tmp.setValidAccountTypeSet(accountTypes);
    Set<Account> setAcount = parent.getValidAccountSet();
    Set<Account> tmp_ls = new LinkedHashSet<>();
    for (Account tmp : setAcount) {
      tmp_ls.add(accountRepository.find(tmp.getId()));
    }
    j_tmp.setValidAccountSet(tmp_ls);
    j_tmp = journalRepository.save(j_tmp);
    return j_tmp;
  }

  @Override
  public GroupeMove getPrecedentGroupe(Period period) {
    LocalDate tmp = period.getFromDate().minusYears(1);
    Period prec = periodRepository.all().filter("self.fromDate=:from").bind("from", tmp).fetchOne();
    if (prec == null) return null;
    return groupeMoveRepository.all().filter("self.period=:period").bind("period", prec).fetchOne();
  }

  @Override
  @Transactional
  public void deleteGroupeMove(GroupeMove gm) {
    GroupeMove m = groupeMoveRepository.find(gm.getId());
    groupeMoveRepository.remove(m);
  }

  @Override
  @Transactional
  public void removeMove(List<Move> move_move) {
    try {
      for (Move tmp : move_move) {
        Move m = moveRepository.find(tmp.getId());
        if (tmp.getMoveLineList() != null && tmp.getMoveLineList().size() > 0) {
          removeMoveLine(m.getMoveLineList());
          m.setMoveLineList(new ArrayList<>());
        }
        moveRepository.remove(m);
        journalRepository.remove(journalRepository.find(m.getJournal().getId()));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Override
  @Transactional
  public void saveGroupeMove(GroupeMove gm) {
    groupeMoveRepository.save(gm);
  }

  @Transactional
  private void removeMoveLine(List<MoveLine> moveLineList) {
    for (MoveLine tmp : moveLineList) {
      MoveLine l = moveLineRepository.find(tmp.getId());
      moveLineRepository.remove(l);
    }
  }

  @Override
  @Transactional
  public List<MoveLine> getListEcritureCloturer(GroupeMove group_prec, Move m, Period period) {
    List<MoveLine> ls_all_moveline = new ArrayList<>();

    for (Move m_tmp : group_prec.getMove_move()) {
      ls_all_moveline.addAll(m_tmp.getMoveLineList());
    }
    List<String> ls_code =
        ls_all_moveline.stream()
            .map(moveLine -> moveLine.getAccount().getCode())
            .distinct()
            .collect(Collectors.toList());

    List<MoveLine> list_result = new ArrayList<>();
    BigDecimal sum_total = BigDecimal.ZERO;
    for (String ml_tmp : ls_code) {
      boolean test = ml_tmp.matches("^[1-5].*");
      if (test) {
        List<MoveLine> ls_by_code =
            ls_all_moveline.stream()
                .filter(moveLine -> moveLine.getAccount().getCode().equals(ml_tmp))
                .collect(Collectors.toList());
        BigDecimal sum_debit =
            ls_by_code.stream().map(MoveLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sum_credit =
            ls_by_code.stream().map(MoveLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean debitEqCredit = sum_credit.compareTo(sum_debit) == 0;
        if (!debitEqCredit) {
          MoveLine mm = moveLineRepository.copy(ls_by_code.get(0), true);
          BigDecimal solde = sum_debit.subtract(sum_credit);
          sum_total = sum_total.add(solde);
          mm.setDebit(solde.compareTo(BigDecimal.ZERO) >= 0 ? solde.abs() : BigDecimal.ZERO);
          mm.setCredit(solde.compareTo(BigDecimal.ZERO) < 0 ? solde.abs() : BigDecimal.ZERO);
          mm.setPartner(null);
          mm.setDate(period.getFromDate());
          list_result.add(moveLineRepository.save(mm));
        }
      }
    }
    if (sum_total.compareTo(BigDecimal.ZERO) != 0) {
      List<Account> ls_debiteur = new ArrayList<>();
      ls_debiteur.add(accountRepository.findByCode("11990000"));
      ls_debiteur.add(accountRepository.findByCode("11990000"));
      ls_debiteur.add(accountRepository.findByCode("11890000"));

      List<Account> ls_crediteur = new ArrayList<>();
      ls_crediteur.add(accountRepository.findByCode("11910000"));
      ls_crediteur.add(accountRepository.findByCode("11910000"));
      ls_crediteur.add(accountRepository.findByCode("11810000"));
      Boolean debit_writed = false;
      MoveLine ml =
          list_result.stream()
              .filter(
                  moveLine ->
                      journalRepository
                          .find(moveLine.getMove().getJournal().getId_journal_parent())
                          .getIs_journalAnouv())
              .findFirst()
              .orElse(null);

      for (Account acc : sum_total.compareTo(BigDecimal.ZERO) > 0 ? ls_crediteur : ls_debiteur) {
        MoveLine m_tmp = moveLineRepository.copy(ml, true);
        m_tmp.setMove(m);
        if (acc.getCode().startsWith("119")) {
          m_tmp.setAccount(acc);
          m_tmp.setAccountCode(acc.getCode());
          m_tmp.setAccountName(acc.getName());
          m_tmp.setDescription(m_tmp.getLibelle());
          m_tmp.setLibelle(
              String.format(
                  "A.N. au %s à ventiler",
                  m_tmp.getDate().format(DateTimeFormatter.ofPattern("dd-MM-YY"))));
          if (debit_writed) {
            m_tmp.setDebit(BigDecimal.ZERO);
            m_tmp.setCredit(sum_total.abs());
          } else {
            m_tmp.setDebit(sum_total.abs());
            m_tmp.setCredit(BigDecimal.ZERO);
            debit_writed = true;
          }
          list_result.add(moveLineRepository.save(m_tmp));
        } else {
          m_tmp.setLibelle("Résultat en instance d'aff");
          m_tmp.setAccount(acc);
          m_tmp.setAccountCode(acc.getCode());
          m_tmp.setAccountName(acc.getName());
          m_tmp.setDescription(m_tmp.getLibelle());
          if (sum_total.compareTo(BigDecimal.ZERO) > 0) {
            m_tmp.setCredit(sum_total);
            m_tmp.setDebit(BigDecimal.ZERO);
          } else {
            m_tmp.setDebit(sum_total.abs());
            m_tmp.setCredit(BigDecimal.ZERO);
          }
          list_result.add(moveLineRepository.save(m_tmp));
        }
      }
    }

    return list_result;
  }

  @Override
  @Transactional
  public void deleteByQuery(String[] strings) {
    for (String req : strings) {
      if (!req.isEmpty()) RunSqlRequestForMe.runSqlRequest(req);
    }
  }

  @Override
  public GroupeMove getGroupeMoveByMove(Move m) {
    GroupeMove gm =
        groupeMoveRepository
            .all()
            .filter(":move MEMBER OF self.move_move")
            .bind("move", m)
            .fetchOne();
    return groupeMoveRepository.find(gm.getId());
  }

  @Override
  public GroupeMove getGroupeMoveById(Long id) {
    return groupeMoveRepository.find(id);
  }

  @Override
  public String checkPrecondition(List<Long> moveIds) {
    List<Move> ls = moveRepository.all().filter("self.id in ?1", moveIds).fetch();
    if (ls.stream().anyMatch(it -> it.getMoveLineList().size() == 0)) {
      return "un ou plusieur journaux ne disposent pas des lignes d'écritures.";
    }
    for (Move tmp : ls) {
      BigDecimal ttDebit =
          tmp.getMoveLineList().stream()
              .map(MoveLine::getDebit)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal ttCredit =
          tmp.getMoveLineList().stream()
              .map(MoveLine::getCredit)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      if (ttDebit.compareTo(ttCredit) != 0) {
        return "un ou plusieur journaux ne sont pas équilibrées.";
      }
    }

    List<? extends Move> moveList =
        moveRepository
            .all()
            .filter(
                "self.id in ?1 AND self.statusSelect NOT IN (?2, ?3)",
                moveIds,
                MoveRepository.STATUS_VALIDATED,
                MoveRepository.STATUS_CANCELED)
            .order("date")
            .fetch();
    if (!moveList.isEmpty()) {
      return "un ou plusieur journaux ne sont pas cloturées.";
    }

    return null;
  }

  @Override
  public GroupeMove nextGroupMoveHaseAnouv(Period period) {
    Period nextPeriod =
        periodRepository
            .all()
            .filter("self.fromDate>=:dateplus and self.toDate<=:dateplus")
            .bind("dateplus", period.getToDate().plusDays(1))
            .order("fromDate")
            .fetchOne();
    if (nextPeriod != null) {
      GroupeMove gm =
          groupeMoveRepository
              .all()
              .filter("self.period =:period")
              .bind("period", nextPeriod)
              .fetchOne();
      if (gm != null
          && gm.getMove_move().stream().noneMatch(it -> it.getJournal().getIs_journalAnouv())) {
        return gm;
      }
    }
    return null;
  }

  @Override
  @Transactional
  public void addJournaleRN(GroupeMove gm_next) {

    Journal jr_an =
        journalRepository
            .all()
            .filter("self.is_journalAnouv is true and (self.year is null or self.year=0)")
            .fetchOne();
    Journal j = new Journal(jr_an.getCode(), jr_an.getName());
    j.setJournalType(jr_an.getJournalType());
    j.setStatusSelect(jr_an.getStatusSelect());
    j.setCompany(jr_an.getCompany());
    j.setSequence(jr_an.getSequence());
    j.setValidAccountTypeSet(jr_an.getValidAccountTypeSet());
    j.setValidAccountSet(jr_an.getValidAccountSet());
    j.setDescriptionIdentificationOk(jr_an.getDescriptionIdentificationOk());
    j.setIsInvoiceMoveConsolidated(jr_an.getIsInvoiceMoveConsolidated());
    j.setCompatiblePartnerTypeSelect(jr_an.getCompatiblePartnerTypeSelect());
    j.setId_journal_parent(jr_an.getId());
    j.setYear(gm_next.getPeriod().getYear().getFromDate().getYear());
    j.setComptComptableBanque(jr_an.getComptComptableBanque());
    j.setComptComptableCaisse(jr_an.getComptComptableCaisse());
    j.setIs_journalAnouv(false);
    j = journalRepository.save(j);
    JPA.clear();
    Move m = new Move();
    m.setJournal(j);
    m = moveRepository.save(m);
    JPA.clear();
    List<Move> move_move = gm_next.getMove_move();
    move_move.add(m);
    gm_next.setMove_move(move_move);
    groupeMoveRepository.save(gm_next);
  }

  @Override
  public String checkPreconditionExtractFileFromExcel(GroupeMove gm) {
    String msg = null;
    if (gm.getStatusSelect() != 1)
      msg = "L'exercice " + gm.getPeriod().getYear() + " n'est pas ouvert";
    if (gm.getPeriod().getStatusSelect() != PeriodRepository.STATUS_OPENED)
      msg = "La periode " + gm.getPeriod().getYear() + " n'est pas ouverte";
    if (gm.getMove_move().stream()
        .anyMatch(
            move ->
                move.getMoveLineList().size() > 0
                    && !Beans.get(JournalRepository.class)
                        .find(move.getJournal().getId_journal_parent())
                        .getIs_journalAnouv())) msg = "Les journaux ne sont pas vides.";
    return msg;
  }

  public boolean is_all_journal_not_present(
      List<Journal> ls_journale, List<String> ls_code_journale_distinct) {
    boolean Found = false;
    int year = ls_journale.get(0).getYear();
    for (String code : ls_code_journale_distinct) {
      String codeJourale_year = code + "_" + year;
      if (ls_journale.stream().noneMatch(journal -> (journal.getCode()).equals(codeJourale_year)))
        return true;
    }
    return Found;
  }

  @Override
  public Map<String, Object> is_all_accountCompte_not_present(List<String> collect) {
    boolean found = false;
    String _code = "";
    Map<String, Object> map = new HashMap<String, Object>();
    for (String code : collect) {
      Account account =
          Beans.get(AccountRepository.class)
              .all()
              .filter("self.code=:code")
              .bind("code", code)
              .fetchOne();
      if (!found && account == null) {
        found = true;
        _code = code;
      }
    }
    map.put("found", found);
    map.put("code", _code);
    return map;
  }

  @Override
  @Transactional
  public void writeMouveLineFromExcelFile(List<UploadExcelMouveLine> ls_data, GroupeMove gm)
      throws AxelorException {
    List<Move> ls_move = gm.getMove_move();
    for (Move m : ls_move) {
      Journal j = journalRepository.find(m.getJournal().getId());
      List<UploadExcelMouveLine> ls_data_filter =
          ls_data.stream()
              .filter(u -> (u.getCode_journale() + "_" + j.getYear()).equals(j.getCode()))
              .collect(Collectors.toList());
      for (UploadExcelMouveLine upl : ls_data_filter) {
        Partner partner = null;
        if (upl.getName_tiers() != null && !upl.getName_tiers().equals("")) {
          partner =
              Beans.get(PartnerRepository.class)
                  .all()
                  .filter("partnerCode=:code")
                  .bind("code", upl.getName_tiers())
                  .fetchOne();
        }
        BigDecimal currencyBalance =
            Beans.get(MoveToolService.class).getBalanceCurrencyAmount(m.getMoveLineList());
        BigDecimal balance = Beans.get(MoveToolService.class).getBalanceAmount(m.getMoveLineList());
        Account account =
            Beans.get(AccountRepository.class)
                .all()
                .filter("self.code=:code")
                .bind("code", upl.code_compte)
                .fetchOne();
        MoveLine moveLine = new MoveLine();
        moveLine.setDate(upl.getDate());
        moveLine.setLibelle(upl.getLibelle());
        moveLine.setDescription(upl.getLibelle());
        moveLine.setDebit(upl.getDebit());
        moveLine.setCredit(upl.getCredit());
        moveLine.setMove(m);
        moveLine.setAccount(account);
        moveLine.setAccountId(account.getId());
        moveLine.setAccountCode(account.getCode());
        moveLine.setCounter(++counter);
        moveLine.setDueDate(upl.getDate());
        moveLine.setAmountPaid(BigDecimal.ZERO);
        moveLine.setAmountRemaining(BigDecimal.ZERO);
        moveLine.setPartner(partner);
        moveLine.setPieceJustif(upl.getNumFacture());
        moveLine.setCurrencyAmount(upl.getDebit().add(upl.getCredit()));
        moveLineRepository.save(moveLine);
        m.addMoveLineListItem(moveLine);
      }
      moveRepository.save(m);
    }
  }

  @Override
  @Transactional
  public void removeAccountingRepport(List<AccountingReport> ls_report) {
    for (AccountingReport report : ls_report) {
      Beans.get(AccountingReportRepository.class).remove(report);
    }
  }
}
