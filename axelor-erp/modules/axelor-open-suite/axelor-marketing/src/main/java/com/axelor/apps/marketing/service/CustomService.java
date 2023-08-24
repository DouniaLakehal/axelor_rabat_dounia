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
import com.axelor.apps.base.db.Period;
import com.axelor.apps.base.db.Year;
import com.axelor.exception.AxelorException;
import com.google.inject.persist.Transactional;
import java.util.List;
import java.util.Map;

public interface CustomService {

  @Transactional
  public Year saveYear(Year one_year);

  List<GroupeMove> getAllGroupeMoveInPersiode(Period period);

  List<Journal> getAllJournales();

  @Transactional
  Journal saveJournale(Journal j);

  @Transactional
  Move saveMouve(Move m);

  @Transactional
  Journal addNewJourale(Long id_journale_ancetre, int anne);

  GroupeMove getPrecedentGroupe(Period period);

  List<MoveLine> getListEcritureCloturer(GroupeMove group_prec, Move m, Period period);

  @Transactional
  void deleteGroupeMove(GroupeMove gm);

  @Transactional
  void removeMove(List<Move> move_move);

  @Transactional
  void saveGroupeMove(GroupeMove gm);

  @Transactional
  void deleteByQuery(String[] strings);

  GroupeMove getGroupeMoveByMove(Move m);

  GroupeMove getGroupeMoveById(Long id);

  String checkPrecondition(List<Long> moveIds);

  GroupeMove nextGroupMoveHaseAnouv(Period period);

  @Transactional
  void addJournaleRN(GroupeMove gm_next);

  String checkPreconditionExtractFileFromExcel(GroupeMove gm);

  boolean is_all_journal_not_present(
      List<Journal> ls_journale, List<String> ls_code_journale_distinct);

  Map<String, Object> is_all_accountCompte_not_present(List<String> collect);

  void writeMouveLineFromExcelFile(List<UploadExcelMouveLine> ls_data, GroupeMove gm)
      throws AxelorException;

  void removeAccountingRepport(List<AccountingReport> ls_report);
}
