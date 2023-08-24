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

package com.axelor.apps.marketing.web;

import com.axelor.apps.account.db.*;
import com.axelor.apps.account.db.repo.AccountingReportRepository;
import com.axelor.apps.account.db.repo.GroupeMoveRepository;
import com.axelor.apps.account.service.move.MoveService;
import com.axelor.apps.base.db.Period;
import com.axelor.apps.base.db.Year;
import com.axelor.apps.base.db.repo.CompanyRepository;
import com.axelor.apps.base.db.repo.CurrencyRepository;
import com.axelor.apps.base.db.repo.PeriodRepository;
import com.axelor.apps.base.db.repo.YearRepository;
import com.axelor.apps.base.service.PeriodService;
import com.axelor.apps.base.service.administration.SequenceService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.configuration.service.RunSqlRequestForMe;
import com.axelor.apps.marketing.db.MoveUploadFile;
import com.axelor.apps.marketing.db.repo.MoveUploadFileRepository;
import com.axelor.apps.marketing.service.CustomService;
import com.axelor.apps.marketing.service.UploadExcelMouveLine;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.*;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

@Singleton
public class CustomCtr {

  @Inject PeriodRepository periodRepository;
  @Inject YearRepository yearRepository;
  @Inject CompanyRepository companyRepository;
  @Inject CustomService appservice;
  @Inject CurrencyRepository currencyRepository;

  public void generateCodePeriode(ActionRequest request, ActionResponse response) {
    if (request.getContext().get("name") == null
        || request.getContext().get("mois_debut") == null
        || request.getContext().get("mois_fin") == null) {
      return;
    }
    Long id = 0l;
    if (request.getContext().get("id") != null) id = (Long) request.getContext().get("id");
    int anne = Integer.parseInt(request.getContext().get("name").toString());
    int mois_db = Integer.parseInt(request.getContext().get("mois_debut").toString());
    int mois_fin = Integer.parseInt(request.getContext().get("mois_fin").toString());
    if (mois_db > mois_fin) {
      response.setFlash("Le Mois de Début est suppérieur au Mois de Fin");
      return;
    }
    String code = String.format("CCISBK/%04d%02d%02d", anne, mois_db, mois_fin);
    LocalDate db = LocalDate.parse(anne + "-" + String.format("%02d", mois_db) + "-" + "01");
    LocalDate df =
        LocalDate.parse(anne + "-" + String.format("%02d", mois_fin) + "-" + "01")
            .plusMonths(1)
            .minusDays(1);
    Year oneYear =
        yearRepository
            .all()
            .filter("self.name=:anne and self.typeSelect=:type")
            .bind("anne", anne)
            .bind("type", 1) // 1 == annee fiscale
            .fetchOne();
    if (oneYear != null) {
      List<Period> ls = oneYear.getPeriodList();
      String mois_ok = "<p>Le mois saisie non Valide; il existe une periode avec </p><p><ul>";
      boolean periode_is_ok = true;
      for (Period tmp : ls) {
        if (!tmp.getId().equals(id) && Integer.parseInt(tmp.getMois_debut()) == mois_db
            || Integer.parseInt(tmp.getMois_fin()) == mois_db && periode_is_ok) {
          periode_is_ok = false;
          mois_ok +=
              "<li> mois de début : "
                  + PeriodRepository.mois[Integer.parseInt(tmp.getMois_debut()) - 1]
                  + "</li>";
          mois_ok +=
              "<li> mois de FIN : "
                  + PeriodRepository.mois[Integer.parseInt(tmp.getMois_fin()) - 1]
                  + "</li>";
        }
      }
      mois_ok += "</ul></p>";
      if (periode_is_ok) {
        response.setValue("year", oneYear);
        response.setReadonly("saveCustomBtn", false);
      } else {
        response.setReadonly("saveCustomBtn", true);
        response.setFlash(mois_ok);
        return;
      }

    } else {
      oneYear = new Year();
      oneYear.setCode("CCISBK/AF/" + anne);
      oneYear.setCompany(companyRepository.find(1l));
      oneYear.setFromDate(db);
      oneYear.setToDate(df);
      oneYear.setName(String.valueOf(anne));
      oneYear.setPeriodDurationSelect(12);
      oneYear.setStatusSelect(1);
      oneYear.setTypeSelect(1);
      oneYear = appservice.saveYear(oneYear);
      response.setValue("year", oneYear);
    }
    response.setValue("code", code);
    response.setValue("fromDate", db);
    response.setValue("toDate", df);
  }

  public void disabledCheckbox_banque(ActionRequest request, ActionResponse response) {
    boolean isBanque = (boolean) request.getContext().get("is_banque");
    if (isBanque) {
      response.setValue("is_banque", false);
    }
  }

  public void disabledCheckbox_caisse(ActionRequest request, ActionResponse response) {
    boolean isCaisse = (boolean) request.getContext().get("is_caisse");
    if (isCaisse) {
      response.setValue("is_caisse", false);
    }
  }

  public void addJournaleAndMove(ActionRequest request, ActionResponse response)
      throws AxelorException {
    List<Journal> ls_journal = appservice.getAllJournales();
    Period period = (Period) request.getContext().get("period");
    GroupeMove group_prec = appservice.getPrecedentGroupe(period);
    String is_first_groupe_nouv = request.getContext().get("isFristYears").toString();
    GroupeMove this_groupe =
        Beans.get(GroupeMoveRepository.class).find((Long) request.getContext().get("id"));
    if (this_groupe != null && this_groupe.getMove_move().size() > 0) {
      for (Move m : this_groupe.getMove_move()) {
        m.setDate(this_groupe.getDate());
        m.setPeriod(this_groupe.getPeriod());
        appservice.saveMouve(m);
        Journal j = m.getJournal();
        j.setYear(Integer.valueOf(this_groupe.getPeriod().getYear().getName()));
        appservice.saveJournale(j);
      }
      return;
    }
    List<Move> moves = new ArrayList<Move>();
    for (Journal tmp : ls_journal) {
      Journal j = null;
      Move m = new Move();
      // kouidi une seule fois generation
      // j = appservice.addNewJourale(tmp.getId(), Integer.parseInt(period.getYear().getName()));
      // cas normale

      j = appservice.addNewJourale(tmp.getId(), Integer.parseInt(period.getYear().getName()));

      if (j != null) {
        m.setCompany(companyRepository.find(1l));
        m.setStatusSelect(1);
        m.setCompanyCurrency(currencyRepository.find(88l)); // maroc
        m.setDate((LocalDate) request.getContext().get("date"));
        m.setAutoYearClosureMove(false);
        m.setCompanyCurrencyCode("MAD");
        m.setJournal(j);
        m.setPeriod((Period) request.getContext().get("period"));
        m.setCurrency(currencyRepository.find(88l));
        m = appservice.saveMouve(m);
        m.setReference(Beans.get(SequenceService.class).getDraftSequenceNumber(m));
      }

      if (tmp.getIs_journalAnouv()) {
        if ((group_prec != null && group_prec.getStatusSelect() == 3)) {
          if (is_first_groupe_nouv.equals("non"))
            m.setMoveLineList(appservice.getListEcritureCloturer(group_prec, m, period));
        }
      }
      m = appservice.saveMouve(m);
      moves.add(m);
    }
    response.setValue("move_move", moves);
  }

  public void getlastPeriod(ActionRequest request, ActionResponse response) {
    Period p = (Period) request.getContext().get("period");
    GroupeMove g = appservice.getPrecedentGroupe(p);
    if (g != null) {
      if (g.getStatusSelect() == 3) {
        response.setValue("$isClosed", "close");
      } else {
        response.setValue("$isClosed", "open");
      }
      response.setValue("$lastPeriod", g.getPeriod());
      response.setValue("$isFristYears", "non");

    } else {
      response.setFlash("Aucun Exercice Précédent détécter, Le Repport a Nouv. sera vide");
      response.setValue("$lastPeriod", null);
      response.setValue("$isClosed", "close");
      response.setValue("$isFristYears", "oui");
    }
    response.setValue("date", LocalDate.parse(p.getName() + "-01-01"));
  }

  public void deleteMoveGroupe(ActionRequest request, ActionResponse response) {
    GroupeMove gm = request.getContext().asType(GroupeMove.class);
    List<AccountingReport> ls_report =
        Beans.get(AccountingReportRepository.class)
            .all()
            .filter("self.period = :period")
            .bind("period", gm.getPeriod())
            .fetch();
    appservice.removeAccountingRepport(ls_report);
    String ids_move =
        (String)
            gm.getMove_move().stream()
                .map(
                    move -> {
                      return String.valueOf(move.getId());
                    })
                .collect(Collectors.joining(","));
    try {
      String r2 = "";
      String rr1 = "";
      String r0 =
          "delete from account_groupe_move_move_move where account_groupe_move ="
              + gm.getId()
              + ";";
      String r1 = "delete from account_groupe_move where id =" + gm.getId() + ";";
      if (!ids_move.isEmpty()) {
        rr1 =
            "delete from account_reconcile x where x.credit_move_line in (select id from account_move_line where move in ("
                + ids_move
                + ") ) or debit_move_line in (select id from account_move_line account_move_line where move in ("
                + ids_move
                + "));";
        r2 = "delete from account_move_line where move in (" + ids_move + ");";
      }
      String r3 = "delete from account_move where period =" + gm.getPeriod().getId() + ";";
      String r4 =
          "delete from account_journal_valid_account_type_set where account_journal in (select id from account_journal where year = "
              + gm.getPeriod().getYear().getFromDate().getYear()
              + ");";
      String r5 =
          "delete from account_journal_valid_account_set where account_journal in (select id from account_journal where year = "
              + gm.getPeriod().getYear().getFromDate().getYear()
              + ");";
      String r6 =
          "delete from account_journal where year = "
              + gm.getPeriod().getYear().getFromDate().getYear()
              + ";";
      appservice.deleteByQuery(new String[] {r0, r1, rr1, r2, r3, r4, r5, r6});
      response.setReload(true);
    } catch (Exception e) {
      response.setFlash(e.getMessage());
    }
  }

  public void cloturerGroupeMove(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    GroupeMove gm = appservice.getGroupeMoveById(id);
    List<Long> moveIds = gm.getMove_move().stream().map(Move::getId).collect(Collectors.toList());
    String msg = appservice.checkPrecondition(moveIds);
    if (msg != null) {
      return;
    }
    gm.setStatusSelect(3);
    appservice.saveGroupeMove(gm);
    response.setReload(true);
  }

  public void clotureMoves(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    GroupeMove gm = appservice.getGroupeMoveById(id);
    if (gm.getStatusSelect() == 3) {
      return;
    }
    List<Move> moveList = gm.getMove_move();
    /* String msg = appservice.checkPrecondition(moveIds);
    if (msg != null) {
      response.setFlash(msg);
      return;
    }*/

    if (!moveList.isEmpty()) {

      boolean error =
          Beans.get(MoveService.class).getMoveValidateService().validateMultiple(moveList);
      if (error)
        response.setFlash(
            I18n.get(com.axelor.apps.account.exception.IExceptionMessage.MOVE_VALIDATION_NOT_OK));
      else {
        response.setFlash(
            I18n.get(com.axelor.apps.account.exception.IExceptionMessage.MOVE_VALIDATION_OK));
        // Beans.get(CustomService.class).addJournaleRN(gm);
        response.setReload(true);
      }
    } else
      response.setFlash(
          I18n.get(com.axelor.apps.account.exception.IExceptionMessage.NO_MOVES_SELECTED));
  }

  public void cloture_anouv(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    GroupeMove gm = appservice.getGroupeMoveById(id);
    List<Long> moveIds = gm.getMove_move().stream().map(Move::getId).collect(Collectors.toList());
    String msg = appservice.checkPrecondition(moveIds);
    if (msg != null) {
      return;
    }
    GroupeMove gm_next = appservice.nextGroupMoveHaseAnouv(gm.getPeriod());
    if (gm_next != null) {
      appservice.addJournaleRN(gm_next);
    }
  }

  public void cloturePeriode(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    GroupeMove gm = appservice.getGroupeMoveById(id);
    List<Long> moveIds = gm.getMove_move().stream().map(Move::getId).collect(Collectors.toList());
    String msg = appservice.checkPrecondition(moveIds);
    if (msg != null) {
      return;
    }
    Period period = Beans.get(PeriodRepository.class).find(gm.getPeriod().getId());
    try {
      Beans.get(PeriodService.class).close(period);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
    response.setReload(true);
  }

  public void updateGroupeMove_partiel(ActionRequest request, ActionResponse response) {
    Move m = request.getContext().asType(Move.class);
    GroupeMove gm = appservice.getGroupeMoveByMove(m);
    BigInteger res =
        (BigInteger)
            RunSqlRequestForMe.runSqlRequest_Object(
                "SELECT count(*) FROM account_groupe_move a JOIN account_groupe_move_move_move a2 ON a.id = a2.account_groupe_move JOIN account_move a3 ON a2.move_move = a3.id WHERE a3.status_select!=3 and a.id="
                    + gm.getId()
                    + ";");
    gm.setStatusSelect(res.compareTo(BigInteger.ZERO) == 0 ? 3 : 2);
    appservice.saveGroupeMove(gm);
  }

  public void tw_extract_data_from_excel_move(ActionRequest request, ActionResponse response)
      throws IOException, AxelorException, InvalidFormatException {
    MoveUploadFile _file =
        Beans.get(MoveUploadFileRepository.class)
            .find(request.getContext().asType(MoveUploadFile.class).getId());
    Long id = Long.valueOf(request.getContext().get("id_groupMouve").toString());
    GroupeMove gm = Beans.get(GroupeMoveRepository.class).find(id);
    if (appservice.checkPreconditionExtractFileFromExcel(gm) != null) {
      response.setFlash(appservice.checkPreconditionExtractFileFromExcel(gm));
      return;
    }

    String dataExportDir = Beans.get(AppBaseService.class).getDataExportDir();
    String chemain = dataExportDir + _file.getDocumentUpload().getFilePath();

    Workbook wb = WorkbookFactory.create(new File(chemain));
    Sheet sheet = wb.getSheetAt(0);
    FormulaEvaluator formulaEvaluator = wb.getCreationHelper().createFormulaEvaluator();

    List<UploadExcelMouveLine> ls_data = new ArrayList<>();
    for (Row row : sheet) {
      if (row.getRowNum() == sheet.getFirstRowNum()) {
        continue;
      }
      UploadExcelMouveLine up_ml = new UploadExcelMouveLine();
      for (Cell cell : row) {
        switch (cell.getColumnIndex()) {
          case 0:
            switch (formulaEvaluator.evaluateInCell(cell).getCellType()) {
              case Cell.CELL_TYPE_NUMERIC:
                up_ml.setCode_journale(String.valueOf(Math.round(cell.getNumericCellValue())));
                break;
              case Cell.CELL_TYPE_STRING:
                up_ml.setCode_journale(cell.getStringCellValue());
                break;
            }
            break;
          case 1:
            switch (formulaEvaluator.evaluateInCell(cell).getCellType()) {
              case Cell.CELL_TYPE_NUMERIC:
                up_ml.setNumFacture(String.valueOf(Math.round(cell.getNumericCellValue())));
                break;
              case Cell.CELL_TYPE_STRING:
                up_ml.setNumFacture(cell.getStringCellValue());
                break;
            }
            break;
          case 2:
            boolean cellDateFormatted = HSSFDateUtil.isCellDateFormatted(cell);
            if (cellDateFormatted) {
              up_ml.setDate(new Date(row.getCell(2).getDateCellValue().getTime()).toLocalDate());
            } else {
              up_ml.setDate(LocalDate.parse(cell.getStringCellValue()));
            }
            break;
          case 3:
            switch (formulaEvaluator.evaluateInCell(cell).getCellType()) {
              case Cell.CELL_TYPE_NUMERIC:
                up_ml.setCode_compte(String.valueOf(Math.round(cell.getNumericCellValue())));
                break;
              case Cell.CELL_TYPE_STRING:
                up_ml.setCode_compte(cell.getStringCellValue());
                break;
            }
            break;
          case 4:
            switch (formulaEvaluator.evaluateInCell(cell).getCellType()) {
              case Cell.CELL_TYPE_NUMERIC:
                up_ml.setName_tiers(String.valueOf(Math.round(cell.getNumericCellValue())));
                break;
              case Cell.CELL_TYPE_STRING:
                up_ml.setName_tiers(cell.getStringCellValue());
                break;
            }
            break;
          case 5:
            switch (formulaEvaluator.evaluateInCell(cell).getCellType()) {
              case Cell.CELL_TYPE_NUMERIC:
                up_ml.setLibelle(String.valueOf(Math.round(cell.getNumericCellValue())));
                break;
              case Cell.CELL_TYPE_STRING:
                up_ml.setLibelle(cell.getStringCellValue());
                break;
            }
            break;
          case 6:
            switch (formulaEvaluator.evaluateInCell(cell).getCellType()) {
              case Cell.CELL_TYPE_NUMERIC:
                up_ml.setDebit(BigDecimal.valueOf(cell.getNumericCellValue()));
                break;
              case Cell.CELL_TYPE_STRING:
                up_ml.setDebit(new BigDecimal(cell.getStringCellValue()));
                break;
            }
            break;
          case 7:
            switch (formulaEvaluator.evaluateInCell(cell).getCellType()) {
              case Cell.CELL_TYPE_NUMERIC:
                up_ml.setCredit(BigDecimal.valueOf(cell.getNumericCellValue()));
                break;
              case Cell.CELL_TYPE_STRING:
                up_ml.setCredit(new BigDecimal(cell.getStringCellValue()));
                break;
            }
            break;
        }
      }
      ls_data.add(up_ml);
    }

    if (!_file.getI_want_upload_a_nouv_ecriture()) {
      for (UploadExcelMouveLine moveline_data : ls_data) {
        if ((moveline_data.getName_tiers() == null || moveline_data.getName_tiers().equals(""))
            && !moveline_data.getCode_journale().equals("AN")) {
          UploadExcelMouveLine data_whith_name =
              ls_data.stream()
                  .filter(
                      o ->
                          o.getName_tiers() != null
                              && !o.getName_tiers().equals("")
                              && o.getLibelle().equals(moveline_data.getLibelle()))
                  .findFirst()
                  .orElse(null);
          if (data_whith_name != null) moveline_data.setName_tiers(data_whith_name.getName_tiers());
        }
      }
    } else {
      for (UploadExcelMouveLine moveline_data : ls_data) {
        if (moveline_data.getName_tiers() == null || moveline_data.getName_tiers().equals("")) {
          UploadExcelMouveLine data_whith_name =
              ls_data.stream()
                  .filter(
                      o ->
                          o.getName_tiers() != null
                              && !o.getName_tiers().equals("")
                              && o.getLibelle().equals(moveline_data.getLibelle()))
                  .findFirst()
                  .orElse(null);
          if (data_whith_name != null) moveline_data.setName_tiers(data_whith_name.getName_tiers());
        }
      }
    }

    List<Journal> ls_journale =
        gm.getMove_move().stream().map(Move::getJournal).collect(Collectors.toList());
    boolean notFound_journale =
        appservice.is_all_journal_not_present(
            ls_journale,
            ls_data.stream()
                .map(UploadExcelMouveLine::getCode_journale)
                .collect(Collectors.toList()));
    if (notFound_journale) {
      response.setFlash(
          "un code du journal dans le fichier n'existe pas dans les journaux de l'exercice");
      return;
    }
    Map<String, Object> map_res =
        appservice.is_all_accountCompte_not_present(
            ls_data.stream()
                .map(UploadExcelMouveLine::getCode_compte)
                .collect(Collectors.toList()));
    boolean notFound_account = (boolean) map_res.get("found");
    if (notFound_account) {
      response.setFlash(
          "un numéros "
              + String.valueOf(map_res.get("code"))
              + " de compte dans le fichier n'existe pas dans le système");
      return;
    }
    appservice.writeMouveLineFromExcelFile(ls_data, gm);
  }

  public void yesyes(ActionRequest request, ActionResponse response) {
    response.setValue("dateUploadFile", LocalDate.now());
    response.setValue(
        "period",
        Beans.get(GroupeMoveRepository.class)
            .find(Long.valueOf(request.getContext().get("id_groupMouve").toString()))
            .getPeriod());
  }
}
