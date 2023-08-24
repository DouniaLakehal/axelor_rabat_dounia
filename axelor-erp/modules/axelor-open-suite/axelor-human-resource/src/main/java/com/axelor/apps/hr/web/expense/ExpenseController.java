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
/*
< * Axelor Business Solutions
 *
 * Copyright (C) 2019 Axelor (<http://axelor.com>).
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
package com.axelor.apps.hr.web.expense;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Wizard;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.base.service.message.MessageServiceBaseImpl;
import com.axelor.apps.configuration.db.IntitulerCredit;
import com.axelor.apps.configuration.db.repo.IntitulerCreditRepository;
import com.axelor.apps.configuration.db.repo.MUTUELLERepository;
import com.axelor.apps.configuration.service.ConvertNomreToLettres;
import com.axelor.apps.configuration.service.MyConfigurationService;
import com.axelor.apps.configuration.service.RunSqlRequestForMe;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.Expense;
import com.axelor.apps.hr.db.ExpenseLine;
import com.axelor.apps.hr.db.KilometricAllowParam;
import com.axelor.apps.hr.db.repo.EmployeeRepository;
import com.axelor.apps.hr.db.repo.ExpenseRepository;
import com.axelor.apps.hr.exception.IExceptionMessage;
import com.axelor.apps.hr.report.IReport;
import com.axelor.apps.hr.service.EmployeeAdvanceService;
import com.axelor.apps.hr.service.EtatsalaireService.EtatSalaireServiceImpl;
import com.axelor.apps.hr.service.HRMenuTagService;
import com.axelor.apps.hr.service.HRMenuValidateService;
import com.axelor.apps.hr.service.KilometricService;
import com.axelor.apps.hr.service.app.AppHumanResourceService;
import com.axelor.apps.hr.service.expense.ExpenseService;
import com.axelor.apps.hr.service.user.UserHrService;
import com.axelor.apps.message.db.Message;
import com.axelor.apps.message.db.repo.MessageRepository;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.apps.tool.StringTool;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.exception.AxelorException;
import com.axelor.exception.ResponseMessageType;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.CallMethod;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author axelor */
@Singleton
public class ExpenseController {

  @Inject EtatSalaireServiceImpl etatSalaireServiceImpl;
  @Inject MyConfigurationService appConfService;
  @Inject AppHumanResourceService appHumanResourceService;
  @Inject EmployeeAdvanceService employeeAdvanceService;
  @Inject MUTUELLERepository mutuelleRepository;
  @Inject IntitulerCreditRepository intitulerCreditRepository;

  @Inject EmployeeAdvanceService appservice;

  private final String[] tableau_mois_francais = {
    "JANVIER",
    "FEVRIER",
    "MARS",
    "AVRIL",
    "MAI",
    "JUIN",
    "JUILLET",
    "AOUT",
    "SEPTEMBRE",
    "OCTOBRE",
    "NOVEMBRE",
    "DECEMBRE"
  };

  private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void createAnalyticDistributionWithTemplate(ActionRequest request, ActionResponse response)
      throws AxelorException {
    ExpenseLine expenseLine = request.getContext().asType(ExpenseLine.class);

    expenseLine =
        Beans.get(ExpenseService.class).createAnalyticDistributionWithTemplate(expenseLine);
    response.setValue("analyticMoveLineList", expenseLine.getAnalyticMoveLineList());
  }

  public void computeAnalyticDistribution(ActionRequest request, ActionResponse response)
      throws AxelorException {
    ExpenseLine expenseLine = request.getContext().asType(ExpenseLine.class);
    Expense expense = expenseLine.getExpense();
    if (expense == null) {
      setExpense(request, expenseLine);
    }
    if (Beans.get(AppAccountService.class).getAppAccount().getManageAnalyticAccounting()) {
      expenseLine = Beans.get(ExpenseService.class).computeAnalyticDistribution(expenseLine);
      response.setValue("analyticMoveLineList", expenseLine.getAnalyticMoveLineList());
    }
  }

  public void editExpense(ActionRequest request, ActionResponse response) {

    User user = AuthUtils.getUser();
    Company activeCompany = user.getActiveCompany();

    List<Expense> expenseList =
        Beans.get(ExpenseRepository.class)
            .all()
            .filter(
                "self.user = ?1 AND self.company = ?2 AND self.statusSelect = 1 AND (self.multipleUsers is false OR self.multipleUsers is null)",
                user,
                activeCompany)
            .fetch();
    if (expenseList.isEmpty()) {
      response.setView(
          ActionView.define(I18n.get("Expense"))
              .model(Expense.class.getName())
              .add("form", "expense-form")
              .context("_payCompany", Beans.get(UserHrService.class).getPayCompany(user))
              .map());
    } else if (expenseList.size() == 1) {
      response.setView(
          ActionView.define(I18n.get("Expense"))
              .model(Expense.class.getName())
              .add("form", "expense-form")
              .param("forceEdit", "true")
              .context("_showRecord", String.valueOf(expenseList.get(0).getId()))
              .map());
    } else {
      response.setView(
          ActionView.define(I18n.get("Expense"))
              .model(Wizard.class.getName())
              .add("form", "popup-expense-form")
              .param("forceEdit", "true")
              .param("popup", "true")
              .param("show-toolbar", "false")
              .param("show-confirm", "false")
              .param("forceEdit", "true")
              .param("popup-save", "false")
              .map());
    }
  }

  @SuppressWarnings("unchecked")
  public void editExpenseSelected(ActionRequest request, ActionResponse response) {
    Map<String, Object> expenseMap =
        (Map<String, Object>) request.getContext().get("expenseSelect");

    if (expenseMap == null) {
      response.setError(I18n.get(IExceptionMessage.EXPENSE_NOT_SELECTED));
      return;
    }
    Long expenseId = Long.valueOf((Integer) expenseMap.get("id"));
    response.setCanClose(true);
    response.setView(
        ActionView.define(I18n.get("Expense"))
            .model(Expense.class.getName())
            .add("form", "expense-form")
            .param("forceEdit", "true")
            .domain("self.id = " + expenseId)
            .context("_showRecord", expenseId)
            .map());
  }

  public void validateExpense(ActionRequest request, ActionResponse response) {

    User user = AuthUtils.getUser();
    Employee employee = user.getEmployee();

    ActionViewBuilder actionView =
        ActionView.define(I18n.get("Expenses to Validate"))
            .model(Expense.class.getName())
            .add("grid", "expense-validate-grid")
            .add("form", "expense-form")
            .param("search-filters", "expense-filters");

    Beans.get(HRMenuValidateService.class).createValidateDomain(user, employee, actionView);

    response.setView(actionView.map());
  }

  public void historicExpense(ActionRequest request, ActionResponse response) {

    User user = AuthUtils.getUser();
    Employee employee = user.getEmployee();

    ActionViewBuilder actionView =
        ActionView.define(I18n.get("Historic colleague Expenses"))
            .model(Expense.class.getName())
            .add("grid", "expense-grid")
            .add("form", "expense-form")
            .param("search-filters", "expense-filters");

    actionView
        .domain(
            "self.company = :_activeCompany AND (self.statusSelect = 3 OR self.statusSelect = 4)")
        .context("_activeCompany", user.getActiveCompany());

    if (employee == null || !employee.getHrManager()) {
      actionView
          .domain(actionView.get().getDomain() + " AND self.user.employee.managerUser = :_user")
          .context("_user", user);
    }

    response.setView(actionView.map());
  }

  public void showSubordinateExpenses(ActionRequest request, ActionResponse response) {

    User user = AuthUtils.getUser();
    Company activeCompany = user.getActiveCompany();

    ActionViewBuilder actionView =
        ActionView.define(I18n.get("Expenses to be Validated by your subordinates"))
            .model(Expense.class.getName())
            .add("grid", "expense-grid")
            .add("form", "expense-form")
            .param("search-filters", "expense-filters");

    String domain =
        "self.user.employee.managerUser.employee.managerUser = :_user AND self.company = :_activeCompany AND self.statusSelect = 2";

    long nbExpenses =
        Query.of(Expense.class)
            .filter(domain)
            .bind("_user", user)
            .bind("_activeCompany", activeCompany)
            .count();

    if (nbExpenses == 0) {
      response.setNotify(I18n.get("No expense to be validated by your subordinates"));
    } else {
      response.setView(
          actionView
              .domain(domain)
              .context("_user", user)
              .context("_activeCompany", activeCompany)
              .map());
    }
  }

  /**
   * Called from expense form, on expense lines change. Call {@link ExpenseService#compute(Expense)}
   *
   * @param request
   * @param response
   */
  public void compute(ActionRequest request, ActionResponse response) {
    Expense expense = request.getContext().asType(Expense.class);
    expense = Beans.get(ExpenseService.class).compute(expense);
    response.setValues(expense);
  }

  public void updateMoveDateAndPeriod(ActionRequest request, ActionResponse response) {
    Expense expense = request.getContext().asType(Expense.class);
    expense = Beans.get(ExpenseService.class).updateMoveDateAndPeriod(expense);
    response.setValue("moveDate", expense.getMoveDate());
    response.setValue("period", expense.getPeriod());
  }

  public void ventilate(ActionRequest request, ActionResponse response) throws AxelorException {
    try {
      Expense expense = request.getContext().asType(Expense.class);
      expense = Beans.get(ExpenseRepository.class).find(expense.getId());
      Move move = Beans.get(ExpenseService.class).ventilate(expense);
      response.setReload(true);
      if (move != null) {
        response.setView(
            ActionView.define(I18n.get("Move"))
                .model(Move.class.getName())
                .add("grid", "move-grid")
                .add("form", "move-form")
                .param("search-filters", "move-filters")
                .context("_showRecord", String.valueOf(move.getId()))
                .map());
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void printExpense(ActionRequest request, ActionResponse response) throws AxelorException {

    Expense expense = request.getContext().asType(Expense.class);

    String name = I18n.get("Expense") + " " + expense.getFullName().replace("/", "-");

    String fileLink =
        ReportFactory.createReport(IReport.EXPENSE, name)
            .addParam("ExpenseId", expense.getId())
            .addParam(
                "Timezone",
                expense.getCompany() != null ? expense.getCompany().getTimezone() : null)
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .toAttach(expense)
            .generate()
            .getFileLink();

    logger.debug("Printing {}", name);

    response.setView(ActionView.define(name).add("html", fileLink).map());
  }

  /* Count Tags displayed on the menu items */
  @CallMethod
  public String expenseValidateMenuTag() {

    return Beans.get(HRMenuTagService.class)
        .countRecordsTag(Expense.class, ExpenseRepository.STATUS_CONFIRMED);
  }

  public String expenseVentilateMenuTag() {
    Long total =
        JPA.all(Expense.class).filter("self.statusSelect = 3 AND self.ventilated = false").count();

    return String.format("%s", total);
  }

  public void cancel(ActionRequest request, ActionResponse response) throws AxelorException {
    try {
      Expense expense = request.getContext().asType(Expense.class);
      expense = Beans.get(ExpenseRepository.class).find(expense.getId());
      ExpenseService expenseService = Beans.get(ExpenseService.class);

      expenseService.cancel(expense);

      Message message = expenseService.sendCancellationEmail(expense);
      if (message != null && message.getStatusSelect() == MessageRepository.STATUS_SENT) {
        response.setFlash(
            String.format(
                I18n.get("Email sent to %s"),
                Beans.get(MessageServiceBaseImpl.class).getToRecipients(message)));
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    } finally {
      response.setReload(true);
    }
  }

  public void addPayment(ActionRequest request, ActionResponse response) {
    Expense expense = request.getContext().asType(Expense.class);
    expense = Beans.get(ExpenseRepository.class).find(expense.getId());
    try {
      Beans.get(ExpenseService.class).addPayment(expense);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Called on clicking cancelPaymentButton, call {@link ExpenseService#cancelPayment(Expense)}.
   *
   * @param request
   * @param response
   */
  public void cancelPayment(ActionRequest request, ActionResponse response) {
    Expense expense = request.getContext().asType(Expense.class);
    expense = Beans.get(ExpenseRepository.class).find(expense.getId());
    try {
      Beans.get(ExpenseService.class).cancelPayment(expense);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  // sending expense and sending mail to manager
  public void send(ActionRequest request, ActionResponse response) throws AxelorException {

    try {
      Expense expense = request.getContext().asType(Expense.class);
      expense = Beans.get(ExpenseRepository.class).find(expense.getId());
      ExpenseService expenseService = Beans.get(ExpenseService.class);

      expenseService.confirm(expense);

      Message message = expenseService.sendConfirmationEmail(expense);
      if (message != null && message.getStatusSelect() == MessageRepository.STATUS_SENT) {
        response.setFlash(
            String.format(
                I18n.get("Email sent to %s"),
                Beans.get(MessageServiceBaseImpl.class).getToRecipients(message)));
      }

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    } finally {
      response.setReload(true);
    }
  }

  public void newExpense(ActionResponse response) {

    response.setView(
        ActionView.define(I18n.get("Expense"))
            .model(Expense.class.getName())
            .add("form", "expense-form")
            .context(
                "_payCompany", Beans.get(UserHrService.class).getPayCompany(AuthUtils.getUser()))
            .map());
  }

  // validating expense and sending mail to applicant
  public void valid(ActionRequest request, ActionResponse response) throws AxelorException {

    try {
      Expense expense = request.getContext().asType(Expense.class);
      expense = Beans.get(ExpenseRepository.class).find(expense.getId());
      ExpenseService expenseService = Beans.get(ExpenseService.class);

      expenseService.validate(expense);

      Message message = expenseService.sendValidationEmail(expense);
      if (message != null && message.getStatusSelect() == MessageRepository.STATUS_SENT) {
        response.setFlash(
            String.format(
                I18n.get("Email sent to %s"),
                Beans.get(MessageServiceBaseImpl.class).getToRecipients(message)));
      }

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    } finally {
      response.setReload(true);
    }
  }

  // refusing expense and sending mail to applicant
  public void refuse(ActionRequest request, ActionResponse response) throws AxelorException {

    try {
      Expense expense = request.getContext().asType(Expense.class);
      expense = Beans.get(ExpenseRepository.class).find(expense.getId());
      ExpenseService expenseService = Beans.get(ExpenseService.class);

      expenseService.refuse(expense);

      Message message = expenseService.sendRefusalEmail(expense);
      if (message != null && message.getStatusSelect() == MessageRepository.STATUS_SENT) {
        response.setFlash(
            String.format(
                I18n.get("Email sent to %s"),
                Beans.get(MessageServiceBaseImpl.class).getToRecipients(message)));
      }

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    } finally {
      response.setReload(true);
    }
  }

  public void fillKilometricExpenseProduct(ActionRequest request, ActionResponse response)
      throws AxelorException {

    try {
      Expense expense = request.getContext().getParent().asType(Expense.class);
      Product expenseProduct = Beans.get(ExpenseService.class).getKilometricExpenseProduct(expense);
      logger.debug("Get Kilometric expense product : {}", expenseProduct);
      response.setValue("expenseProduct", expenseProduct);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void validateAndCompute(ActionRequest request, ActionResponse response) {

    Expense expense = request.getContext().asType(Expense.class);
    ExpenseService expenseService = Beans.get(ExpenseService.class);

    List<Integer> expenseLineListId = new ArrayList<>();
    int compt = 0;
    for (ExpenseLine expenseLine : expenseService.getExpenseLineList(expense)) {
      compt++;
      if (expenseLine
          .getExpenseDate()
          .isAfter(Beans.get(AppBaseService.class).getTodayDate(expense.getCompany()))) {
        expenseLineListId.add(compt);
      }
    }
    try {
      if (!expenseLineListId.isEmpty()) {

        throw new AxelorException(
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get("Date can't be in the future for line(s) : %s"),
            expenseLineListId.stream().map(id -> id.toString()).collect(Collectors.joining(",")));
      }

    } catch (AxelorException e) {

      TraceBackService.trace(response, e, ResponseMessageType.ERROR);
    }

    response.setValue(
        "personalExpenseAmount", expenseService.computePersonalExpenseAmount(expense));
    response.setValue("advanceAmount", expenseService.computeAdvanceAmount(expense));

    if (expense.getKilometricExpenseLineList() != null
        && !expense.getKilometricExpenseLineList().isEmpty()) {
      response.setValue("kilometricExpenseLineList", expense.getKilometricExpenseLineList());
    }

    compute(request, response);
  }

  public void computeKilometricExpense(ActionRequest request, ActionResponse response)
      throws AxelorException {

    ExpenseLine expenseLine = request.getContext().asType(ExpenseLine.class);
    if (expenseLine.getKilometricAllowParam() == null
        || expenseLine.getDistance().compareTo(BigDecimal.ZERO) == 0
        || expenseLine.getExpenseDate() == null) {
      return;
    }

    String userId;
    String userName;
    if (expenseLine.getExpense() != null) {
      setExpense(request, expenseLine);
    }
    Expense expense = expenseLine.getExpense();

    if (expense != null && expenseLine.getUser() != null) {
      userId = expense.getUser().getId().toString();
      userName = expense.getUser().getFullName();
    } else {
      userId = request.getContext().getParent().asType(Expense.class).getUser().getId().toString();
      userName = request.getContext().getParent().asType(Expense.class).getUser().getFullName();
    }
    Employee employee =
        Beans.get(EmployeeRepository.class).all().filter("self.user.id = ?1", userId).fetchOne();

    if (employee == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
          I18n.get(IExceptionMessage.LEAVE_USER_EMPLOYEE),
          userName);
    }

    BigDecimal amount = BigDecimal.ZERO;
    try {
      amount = Beans.get(KilometricService.class).computeKilometricExpense(expenseLine, employee);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }

    response.setValue("totalAmount", amount);
    response.setValue("untaxedAmount", amount);
  }

  public void updateKAPOfKilometricAllowance(ActionRequest request, ActionResponse response)
      throws AxelorException {
    ExpenseLine expenseLine = request.getContext().asType(ExpenseLine.class);

    if (expenseLine.getExpense() == null) {
      setExpense(request, expenseLine);
    }

    try {
      List<KilometricAllowParam> kilometricAllowParamList =
          Beans.get(ExpenseService.class).getListOfKilometricAllowParamVehicleFilter(expenseLine);
      if (kilometricAllowParamList == null || kilometricAllowParamList.isEmpty()) {
        response.setAttr("kilometricAllowParam", "domain", "self.id IN (0)");
      } else {
        response.setAttr(
            "kilometricAllowParam",
            "domain",
            "self.id IN (" + StringTool.getIdListString(kilometricAllowParamList) + ")");
      }

      KilometricAllowParam currentKilometricAllowParam = expenseLine.getKilometricAllowParam();
      boolean vehicleOk = false;

      if (kilometricAllowParamList != null && kilometricAllowParamList.size() == 1) {
        response.setValue("kilometricAllowParam", kilometricAllowParamList.get(0));
      } else if (kilometricAllowParamList != null) {
        for (KilometricAllowParam kilometricAllowParam : kilometricAllowParamList) {
          if (currentKilometricAllowParam != null
              && currentKilometricAllowParam.equals(kilometricAllowParam)) {
            expenseLine.setKilometricAllowParam(kilometricAllowParam);
            vehicleOk = true;
            break;
          }
        }
        if (!vehicleOk) {
          response.setValue("kilometricAllowParam", null);
        } else {
          response.setValue("kilometricAllowParam", expenseLine.getKilometricAllowParam());
        }
      }

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  private void setExpense(ActionRequest request, ExpenseLine expenseLine) {

    Context parent = request.getContext().getParent();

    if (parent != null && parent.get("_model").equals(Expense.class.getName())) {
      expenseLine.setExpense(parent.asType(Expense.class));
    }
  }

  public void domainOnSelectOnKAP(ActionRequest request, ActionResponse response)
      throws AxelorException {

    ExpenseLine expenseLine = request.getContext().asType(ExpenseLine.class);

    if (expenseLine.getExpense() == null) {
      setExpense(request, expenseLine);
    }

    try {
      List<KilometricAllowParam> kilometricAllowParamList =
          Beans.get(ExpenseService.class).getListOfKilometricAllowParamVehicleFilter(expenseLine);
      response.setAttr(
          "kilometricAllowParam",
          "domain",
          "self.id IN (" + StringTool.getIdListString(kilometricAllowParamList) + ")");
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void computeDistanceAndKilometricExpense(ActionRequest request, ActionResponse response)
      throws AxelorException {

    // Compute distance.
    try {

      if (!Beans.get(AppHumanResourceService.class)
          .getAppExpense()
          .getComputeDistanceWithWebService()) {
        return;
      }

      Context context = request.getContext();
      ExpenseLine expenseLine = context.asType(ExpenseLine.class);

      if (Strings.isNullOrEmpty(expenseLine.getFromCity())
          || Strings.isNullOrEmpty(expenseLine.getToCity())) {
        return;
      }

      KilometricService kilometricService = Beans.get(KilometricService.class);
      BigDecimal distance = kilometricService.computeDistance(expenseLine);
      expenseLine.setDistance(distance);
      response.setValue("distance", distance);

      // Compute kilometric expense.

      if (expenseLine.getKilometricAllowParam() == null
          || expenseLine.getExpenseDate() == null
          || expenseLine.getKilometricTypeSelect() == 0) {
        return;
      }

      Expense expense = expenseLine.getExpense();

      if (expense == null) {
        expense = context.getParent().asType(Expense.class);
      }

      Employee employee = expense.getUser().getEmployee();

      if (employee == null) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(IExceptionMessage.LEAVE_USER_EMPLOYEE),
            expense.getUser().getName());
      }

      BigDecimal amount = kilometricService.computeKilometricExpense(expenseLine, employee);
      response.setValue("totalAmount", amount);
      response.setValue("untaxedAmount", amount);

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void editBeaudereauEmission(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }

    BigDecimal total = appservice.getTotalGenerale(mois, annee);
    total = total.setScale(2, RoundingMode.HALF_UP);

    String montantString = ConvertNomreToLettres.getStringMontant(total);

    String fileLink =
        ReportFactory.createReport(IReport.BordereauDemission, "BordereauDemission")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("montantString", montantString)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Bordereau d'émission").add("html", fileLink).map());
  }

  public void editBeaudereauMensuel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal somme = BigDecimal.ZERO;
    if (etat != null) {
      somme = (BigDecimal) etat;
    }
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.BordereauMensuel, "BordereauMensuel")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("total", somme)
            .addParam("total1", somme)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Bordereau mensuel").add("html", fileLink).map());
  }

  public void editVignette(ActionRequest request, ActionResponse response) throws AxelorException {
    String[] tab_month =
        new String[] {
          "Janvier",
          "Février",
          "Mars",
          "Avril",
          "Mai",
          "Juin",
          "Juiellet",
          "Août",
          "Septembre",
          "Octobre",
          "Novembre",
          "Décembre"
        };
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }

    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal somme = BigDecimal.ZERO;
    if (etat != null) {
      somme = (BigDecimal) etat;
    }
    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.Vignette, "Vignette")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", tab_month[mois - 1])
            .addParam("annee", annee)
            .addParam("total", somme)
            .addParam("total1", somme)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Vignette").add("html", fileLink).map());
  }

  public void editOrdreDeVirement(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    BigDecimal mt1 = appservice.getMontantOrdreVirementAgent(0, mois, annee);
    BigDecimal mt2 = appservice.getMontantOrdreVirementAgent(1, mois, annee);

    String montant_page1 = ConvertNomreToLettres.getStringMontant(mt1);
    String montant_page2 = ConvertNomreToLettres.getStringMontant(mt2);

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdreDeVirement, "OrdreDeVirement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("montant_page1", montant_page1)
            .addParam("montant_page2", montant_page2)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement").add("html", fileLink).map());
  }

  public void RCAR(ActionRequest request, ActionResponse response) throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String code = "617111";
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String req =
        "select sum(es.rcar_rg) as Rcar,sum(es.rcarrcomp) as Rcarrcomp from hr_etat_salaire es  where es.mois=?1 and es.annee=?2 and titulaire=true ";
    Object etat = excuteRequette(mois, annee, code, req);
    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));
    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.OrdredesPaiements, "RCAR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "R.C.A.R")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION SALARIALE AU R.C.A.R DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "R.C.A.R")
            .addParam("Objet2", "CC.RCAR")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("RCAR").add("html", fileLink).map());
  }

  public void RcarInLog(ActionRequest request, ActionResponse response) throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617131";
    String req =
        "select sum(es.rcar_comp617131) as Rcarrcomp from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    String req2 =
        "select sum(es.rcar_comp617131) as Rcar from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);
    Object etat2 = excuteRequette(mois, annee, code, req2);
    float sommeRCAR = ((BigDecimal) etat).floatValue() + ((BigDecimal) etat2).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdredesPaiements, "RCAR IN LOGEMENT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "R.C.A.R")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "INDEMNITES DE LOGEMENT")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION SALARIALE AU R.C.A.R DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "R.C.A.R")
            .addParam("Objet2", "CC.RCAR")
            .addParam("Val1", etat2)
            .addParam("Val2", etat)
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("RCAR IN LOGEMENT").add("html", fileLink).map());
  }

  public void RcarP(ActionRequest request, ActionResponse response) throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617422";
    String req =
        "select sum(es.rcar_rg*2) as Rcar,sum(es.rcarrcomp) as Rcarrcomp from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdredesPaiements, "RCAR PATRONALE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "R.C.A.R")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam(
                "Rubrique",
                "COTISATIONS  PATRONALES  AU  REGIME  COLLECTIF D'ALLOCATION DE  RETRAITES (RCAR)")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION PATRONALE AU R.C.A.R DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "COTISATION PATRONALE AU RCAR")
            .addParam("Objet2", "COTISATION PATRONALE AU CC. RCAR")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("RCAR PATRONALE").add("html", fileLink).map());
  }

  public void MT_Recore(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        " select sum(es.recore) as Recore from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);
    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT RECORE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "CDG  - CNRA")
            .addParam("Code", code)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION SALARIALE RECORE DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT RECORE").add("html", fileLink).map());
  }

  public void MT_Amo(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.amo) as amo from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT AMO")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "C.N.O.P.S")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (A.M.O) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT AMO").add("html", fileLink).map());
  }

  public void MtAmoC(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617112";
    String req =
        "select sum(es.amo) as amo from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT AMO C")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "C.N.O.P.S")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL CONTRACTUEL")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (A.M.O) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT AMO C").add("html", fileLink).map());
  }

  public void MT_SM_CDD(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.mutuelle_mgpapsm) as mutuelle_sm,sum(es.mutuelle_mgpap_ccd) as mutuelle_ccd from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true and is_disposition=false";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.OrdredesPaiements, "MT SM CDD")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "M.G.P.A.P")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (S.M) ET (C.C.D) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "MUTUELLE SECTEUR MUTUALISTE")
            .addParam("Objet2", "MUTUELLE C.C.D")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT SM CDD").add("html", fileLink).map());
  }

  public void OMFAM(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.mutuelle_omfam_sm) as mutuelle_omfam_sm,sum(es.mutuelle_omfam_caad) as mutuelle_omfam_caad from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.OrdredesPaiements, "OMFAM")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "O.M.F.A.M")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "Prélèvement mutualiste (OMFAM) mois ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "MUTUELLE SECTEUR MUTUALISTE")
            .addParam("Objet2", "MUTUELLE C.A.A.D")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("OMFAM").add("html", fileLink).map());
  }

  public void IR(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String code = "617111";
    String req =
        "select sum(es.ir) as ir from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "IR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "LE TRESORIER PREFECTORAL DE FES")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " IMPOT SUR REVENU DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("IR").add("html", fileLink).map());
  }

  public void IrInLog(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617131";
    String req =
        "select sum(es.ir617131) as irLog from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "IR IN LOGEMENT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "  LE TRESORIER PREFECTORAL DE FES")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITE DE LOGEMENT")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " IMPOT SUR INDEMNITE DE LOGEMENT DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("IR IN LOGEMENT").add("html", fileLink).map());
  }

  public void IRC(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617112";
    String req =
        "select sum(es.ir) as ir from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "IR CONTRACTUELLE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "LE TRESORIER PREFECTORAL DE FES")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL CONTRACTUEL")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " IMPOT SUR REVENU DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("IR CONTRACTUEL").add("html", fileLink).map());
  }

  public void Axa(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.pret_axa_credit) as axa from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "AXA CREDIT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "AXA CREDIT")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " PRELEVEMENT SUR SALAIRE DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("AXA CREDIT").add("html", fileLink).map());
  }

  public void OpCss(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.css) as css from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.OrdredesPaiementCSS, "OPCSS")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", " TRESORIER MINISTERIEL AUPRES DU MINISTERE DE L'ECONOMIE")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam(
                "Objet",
                " CONTRIBUTION SOCIALE DE SOLIDARITE DU MOIS "
                    + etatSalaireServiceImpl.ConvertMoisToLettre(mois)
                    + " "
                    + annee)
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "C/B N° 001 810 00 780 002 011 062 02 21")
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("OPCSS").add("html", fileLink).map());
  }

  public void MtMgpapC(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617112";
    String req =
        "select sum(es.mutuelle_mgpapsm) as mutuelle_mgpapsm, sum(es.mutuelle_mgpap_ccd) as mutuelle_mgpapsm_ccd from hr_etat_salaire es where es.mois=?1 and es.annee=?2  and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdredesPaiements, "MTMGPAC CONTRACTUEL")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "M.G.P.A.P")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL CONTRACTUEL")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (S.M) ET (C.C.D) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "MUTUELLE SECTEUR MUTUALISTE")
            .addParam("Objet2", "MUTUELLE C.C.D")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MTMGPAC CONTRACTUEL").add("html", fileLink).map());
  }

  public void MtIndFonction(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617133";
    String req =
        "select sum(es.indemnit_fonction_net) as indemnit_fonction_net from hr_etat_salaire es where es.mois=?1 and es.annee=?2 ";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT IND FONCTION")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITES DE FONCTION")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "INDEMNITE DE FONCTION NETTE / ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND FONCTION").add("html", fileLink).map());
  }

  public void MtLogement(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617131";
    String req =
        "select sum(es.indemnite_logem_brut) as indemnite_logem_brut,sum(es.rcar_comp617131) as rcar_comp617131,sum(es.ir617131) as ir_comp617131 from hr_etat_salaire es where es.mois=?1 and es.annee=?2 ";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float ss = ((BigDecimal) obj[0]).floatValue();
    float sommeRCAR = ((BigDecimal) obj[1]).floatValue() + ((BigDecimal) obj[2]).floatValue();
    float somme = ss - sommeRCAR;
    double number2 = (Math.round(somme * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.Mt_Logement, "MT IND LOGEMENT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("Rubrique", "INDEMNITE DE LOGEMENT")
            .addParam("Total", ss)
            .addParam("Objet", "INDEMNITE DE LOGEMENT DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "RCAR COMPLEMENTAIRE")
            .addParam("Objet2", "IMPOT SUR INDEMNITE DE LOGEMENT")
            .addParam("Val1", obj[2])
            .addParam("Val2", obj[1])
            .addParam("Objet3", "Total Retenues")
            .addParam("Val3", sommeRCAR)
            .addParam("Net", somme)
            .addParam("Montant", montant)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND LOGEMENT").add("html", fileLink).map());
  }

  public void MtIndVoit(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617135";
    String req =
        "select sum(es.indemnit_voiture_net) as indemnit_voiture_net from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT IND VOITURE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITES DE VOITURE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "INDEMNITE DE VOITURE NETTE / ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND VOITURE").add("html", fileLink).map());
  }

  public void MtIndRepresentation(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617136";
    String req =
        "select sum(es.indemnit_represent_net) as indemnit_represent_net from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT IND REPRESENTATION")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITES DE REPRESENTATION")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "INDEMNITE DE REPRESENTATION NETTE/ ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND REPRESENTATION").add("html", fileLink).map());
  }

  public void Cnopsp(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "61743";
    String req =
        "select sum(es.amo) as amo from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "CNOPSP")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "C.N.O.P.S")
            .addParam("Code", code)
            .addParam("Rubrique", "COTISATIONS AUX MUTUELLES")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION PATRONALE AUX MUTUELLES DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("CNOPSP").add("html", fileLink).map());
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

  public void OVRCAR(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(x.rcar_rg)+sum(x.rcarrcomp)+sum(x.rcar_comp617131) from hr_etat_salaire x where mois=?1 and annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat = query.getSingleResult();
    BigDecimal sommeRCAR = etat != null ? (BigDecimal) etat : BigDecimal.ZERO;

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(x.rcar_rg)*2+sum(x.rcarrcomp)+sum(x.rcar_comp617131) from hr_etat_salaire x where mois=?1 and annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat2 = query2.getSingleResult();
    BigDecimal sommeRCAR2 = etat != null ? (BigDecimal) etat2 : BigDecimal.ZERO;

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) as rcar from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat3 = query3.getSingleResult();
    BigDecimal sommeRecore3 = etat != null ? (BigDecimal) etat3 : BigDecimal.ZERO;
    float somme =
        (sommeRCAR.add(sommeRCAR2).add(sommeRecore3).setScale(2, RoundingMode.HALF_UP))
            .floatValue();
    double number2 = (Math.round(somme * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));
    String rib = appConfService.getRibRcar(2021);
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OVRCAR, "Ordre de virement RCAR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("RCAR", "RCAR")
            .addParam("RCAR2", "RCAR")
            .addParam("compteRCAR", rib)
            .addParam("compteRCAR2", rib)
            .addParam("RECORE", "CDG - CNRA (RECORE)")
            .addParam("compteRECORE", "CDG  060810114010349000010174")
            .addParam("Somme1", sommeRCAR)
            .addParam("Somme2", sommeRCAR2)
            .addParam("Somme3", sommeRecore3)
            .addParam("Total", sommeRCAR.add(sommeRCAR2))
            .addParam(
                "TotalGeneral",
                sommeRCAR.add(sommeRCAR2).add(sommeRecore3).setScale(2, RoundingMode.HALF_UP))
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement RCAR").add("html", fileLink).map());
  }

  public void OVAxaCredit(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.pret_axa_credit) as axa from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat = query.getSingleResult();
    BigDecimal sommeAxa = BigDecimal.ZERO;
    IntitulerCredit i = intitulerCreditRepository.all().filter("self.name like '%AXA%'").fetchOne();
    String rib = "";
    if (i != null) {
      rib = i.getNameBank() + " N° " + i.getRib();
    }
    if (etat != null) {
      sommeAxa = (BigDecimal) etat;
    }
    String montant = ConvertNomreToLettres.getStringMontant(sommeAxa);
    String fileLink =
        ReportFactory.createReport(IReport.OVothers, "Ordre de virement AXA Crédit")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("other", "AXA CREDIT")
            .addParam("compteother", rib)
            .addParam("Somme1", sommeAxa)
            .addParam("TotalGeneral", sommeAxa)
            .addParam("montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement AXA Crédit").add("html", fileLink).map());
  }

  public void OVIR(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }

    BigDecimal sommeIR =
        RunSqlRequestForMe.runSqlRequest_Bigdecimal(
            "select sum(es.ir)+ sum(ir617131) as axa from hr_etat_salaire es where es.mois= "
                + mois
                + " and es.annee="
                + annee
                + " ;");
    String montant = ConvertNomreToLettres.getStringMontant(sommeIR);
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OVothers, "Ordre de virement IR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("other", "TRESORIER PREFECTORAL DE FES")
            .addParam("compteother", "TRESORIER PREFECTORAL DE FES")
            .addParam("Somme1", sommeIR.setScale(2, RoundingMode.HALF_UP))
            .addParam("TotalGeneral", sommeIR.setScale(2, RoundingMode.HALF_UP))
            .addParam("montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement IR").add("html", fileLink).map());
  }

  public void OVCSS(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.css) as axa from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat = query.getSingleResult();
    BigDecimal sommeCSS = etat != null ? (BigDecimal) etat : BigDecimal.ZERO;
    String montant = ConvertNomreToLettres.getStringMontant(sommeCSS);
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OVothers, "Ordre de virement CSS")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam(
                "other",
                "TRESORIER MINISTERIEL AUPRES DU MINISTERE DE L'ECONOMIE, DES DFINANCES ET DE LA REFORME DE L'ADMINISTRATION    (Contribution de Solidarité Sociale)")
            .addParam("compteother", "C/B N° 001 810 00 780 002 011 062 02 21")
            .addParam("Somme1", sommeCSS)
            .addParam("TotalGeneral", sommeCSS)
            .addParam("montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement CSS").add("html", fileLink).map());
  }

  public void OVCNOPS(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) as axa from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal sommeAMO = BigDecimal.ZERO;
    String ribAmo = employeeAdvanceService.getRibMutuelle(1, 2021);
    if (etat != null) {
      sommeAMO = (BigDecimal) etat;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpapsm) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal sommeMGPAP = BigDecimal.ZERO;
    String ribMGPAP = employeeAdvanceService.getRibMutuelle(2, 2021);
    if (etat2 != null) {
      sommeMGPAP = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpap_ccd) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal sommeMGPAPCCD = BigDecimal.ZERO;
    String ribMGPAPCCD = employeeAdvanceService.getRibMutuelle(3, 2021);
    if (etat3 != null) {
      sommeMGPAPCCD = (BigDecimal) etat3;
    }

    javax.persistence.Query query4 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_sm) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat4 = query4.getSingleResult();
    BigDecimal sommeOMFAM = BigDecimal.ZERO;
    String ribOMFAM = employeeAdvanceService.getRibMutuelle(4, 2021);
    if (etat4 != null) {
      sommeOMFAM = (BigDecimal) etat4;
    }

    javax.persistence.Query query5 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_caad) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat5 = query5.getSingleResult();
    BigDecimal sommeOMFAMCCD = BigDecimal.ZERO;
    String ribOMFAMCCD = employeeAdvanceService.getRibMutuelle(5, 2021);
    if (etat5 != null) {
      sommeOMFAMCCD = (BigDecimal) etat5;
    }
    float somme =
        (sommeAMO
                .add(sommeMGPAP)
                .add(sommeMGPAPCCD)
                .add(sommeOMFAM)
                .add(sommeOMFAMCCD)
                .add(sommeAMO))
            .floatValue();
    double number2 = (Math.round(somme * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OVCNOPS, "Ordre de virement CNOPS")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("nom", "C.N.O.P.S")
            .addParam("nom2", "M.G.P.A.P")
            .addParam("nom3", "M.G.P.A.P")
            .addParam("nom4", "OMFAM - SM")
            .addParam("nom5", "OMFAM - CAAD")
            .addParam("compte", ribAmo)
            .addParam("compte2", ribMGPAP)
            .addParam("compte3", ribMGPAPCCD)
            .addParam("compte4", ribOMFAM)
            .addParam("compte5", ribOMFAMCCD)
            .addParam("Somme1", sommeAMO)
            .addParam("Somme2", sommeMGPAP)
            .addParam("Somme3", sommeMGPAPCCD)
            .addParam("Somme4", sommeOMFAM)
            .addParam("Somme5", sommeOMFAMCCD)
            .addParam(
                "Total",
                sommeAMO.add(sommeMGPAP).add(sommeMGPAPCCD).add(sommeOMFAM).add(sommeOMFAMCCD))
            .addParam(
                "TotalGeneral",
                sommeAMO
                    .add(sommeMGPAP)
                    .add(sommeMGPAPCCD)
                    .add(sommeOMFAM)
                    .add(sommeOMFAMCCD)
                    .add(sommeAMO))
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement CNOPS").add("html", fileLink).map());
  }

  public void editEtatRCAR(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as salaire from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal somme = BigDecimal.ZERO;
    if (etat != null) {
      somme = (BigDecimal) etat;
    }

    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg) as Rcar from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcarrcomp) as Rcarrcomp from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatRCAR, "EtatRCAR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("mois_name_fr", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("total", somme)
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("total3", somme1.add(somme2))
            .addParam("totalGeneral", (somme1).add(somme1).add(somme2))
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat RCAR").add("html", fileLink).map());
  }

  public void editEtatRCARSal(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as Rcar from hr_etat_salaire es left join hr_employee he\n"
                    + "                                  on es.employee = he.id where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg) as Rcarrcomp from hr_etat_salaire es left join hr_employee he"
                    + " on es.employee = he.id where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcarrcomp) as Rcarrcomp from hr_etat_salaire es  left join hr_employee he "
                    + "on es.employee = he.id where he.is_dispose=false and  es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal somme3 = BigDecimal.ZERO;
    if (etat3 != null) {
      somme3 = (BigDecimal) etat3;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatRcarSal, "EtatRcarSal")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("total3", somme3)
            .addParam("mois_name_fr", tableau_mois_francais[mois - 1])
            .addParam("totalGeneral", somme2.add(somme3))
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat RCAR Salariale").add("html", fileLink).map());
  }

  public void editEtatIR(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as Rcar from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.ir) as IR from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatIR, "EtatIR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("string_mois", tableau_mois_francais[mois - 1])
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("totalGeneral", somme2)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat IR").add("html", fileLink).map());
  }

  public void editEtatOMFAM(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String moiss = mois < 10 ? "0" + mois : String.valueOf(mois);
    LocalDate d1 = LocalDate.parse(annee + "-" + moiss + "-01");
    LocalDate d2 = LocalDate.parse(annee + "-" + moiss + "-" + d1.lengthOfMonth());
    Date date1 = java.sql.Date.valueOf(d1);
    Date date2 = java.sql.Date.valueOf(d2);
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as OMFAM from hr_etat_salaire es left join hr_employee he on es.employee = he.id left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + " where com.name='OMFAM' and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_sm) as OMFAMsm from hr_etat_salaire es left join hr_employee he on es.employee = he.id\n"
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id"
                    + " where es.mois=?1 and es.annee=?2 and com.name='OMFAM'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_caad) as OMFAMcaad from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='OMFAM'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal somme3 = BigDecimal.ZERO;
    if (etat3 != null) {
      somme3 = (BigDecimal) etat3;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatOMFAM, "EtatOMFAM")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("debut1", date1)
            .addParam("fin1", date2)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("total3", somme3)
            .addParam("totalGeneral", somme2.add(somme3))
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat OMFAM SM CAAD").add("html", fileLink).map());
  }

  public void editEtatMGPAP(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String moiss = mois < 10 ? "0" + mois : String.valueOf(mois);
    LocalDate d1 = LocalDate.parse(annee + "-" + moiss + "-01");
    LocalDate d2 = LocalDate.parse(annee + "-" + moiss + "-" + d1.lengthOfMonth());

    Date date1 = java.sql.Date.valueOf(d1);
    Date date2 = java.sql.Date.valueOf(d2);

    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as brut from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) as amo from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpapsm) as sm from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal somme3 = BigDecimal.ZERO;
    if (etat3 != null) {
      somme3 = (BigDecimal) etat3;
    }

    javax.persistence.Query query4 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpap_ccd) as ccd from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat4 = query4.getSingleResult();
    BigDecimal somme4 = BigDecimal.ZERO;
    if (etat4 != null) {
      somme4 = (BigDecimal) etat4;
    }
    BigDecimal tot1_2 = somme4.add(somme3).setScale(2, RoundingMode.HALF_UP);
    String montant1 = ConvertNomreToLettres.getStringMontant(somme3);
    String montant2 = ConvertNomreToLettres.getStringMontant(somme4);
    String montant3 = ConvertNomreToLettres.getStringMontant(tot1_2);

    String fileLink =
        ReportFactory.createReport(IReport.EtatMGPAP, "Etat MGPAP")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("debut1", date1)
            .addParam("fin1", date2)
            .addParam("montant1", montant1)
            .addParam("montant2", montant2)
            .addParam("montant3", montant3)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat MGPAP SM CCD").add("html", fileLink).map());
  }

  public void editEtatAMO(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as TOTAL1 from hr_etat_salaire es left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2 and he.is_dispose=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) as TOTAL2 from hr_etat_salaire es left join hr_employee he on es.employee = he.id\n"
                    + "where es.mois=?1 and es.annee=?2 and he.is_dispose=false ")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }
    float sommeRCAR = ((BigDecimal) etat1).floatValue() + ((BigDecimal) etat2).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    BigDecimal total_amo = appservice.getTotalAmo(mois, annee);
    String mt_string =
        ConvertNomreToLettres.getStringMontant(
            total_amo.multiply(BigDecimal.valueOf(2l)).setScale(2, RoundingMode.HALF_UP));

    String fileLink =
        ReportFactory.createReport(IReport.EtatAMO, "EtatAMO")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("montant", mt_string)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat AMO").add("html", fileLink).map());
  }

  public void editEtatAXA(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(hgc.remboursement) as TOTAL1 from hr_etat_salaire es left join hr_employee he on es.employee = he.id left join hr_gestion_credit hgc on he.id = hgc.employee where es.mois=?1 and es.annee=?2 and hgc.remboursement is not null")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }
    String montant = ConvertNomreToLettres.getStringMontant(somme1);
    String fileLink =
        ReportFactory.createReport(IReport.EtatAXA, "Etat AXA Crédit")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("total1", somme1)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat AXA Crédit").add("html", fileLink).map());
  }

  public void verifier_liste_salaire(ActionRequest request, ActionResponse response) {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");

    int nbr = employeeAdvanceService.getNbrEtatSalaire(mois, annee);
    /*if (nbr > 0) {
      response.setReadonly("mois", true);
      response.setReadonly("annee", true);
      response.setReadonly("salairePlafonne", true);
      response.setHidden("btn_1", false);
    } else {
      response.setReadonly("mois", false);
      response.setReadonly("annee", false);
      response.setReadonly("salairePlafonne", false);
      response.setHidden("btn_1", true);
      response.setFlash("Aucune Etat de salaire dans cette période");
    }*/
    response.setReadonly("mois", true);
    response.setReadonly("annee", true);
    response.setReadonly("salairePlafonne", true);
    response.setHidden("btn_1", false);
  }

  public void imprimerOrdrePayementmt(ActionRequest request, ActionResponse response)
      throws AxelorException {
    int annee = (int) request.getContext().get("annee");
    int mois = (int) request.getContext().get("mois");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String mt_string =
        ConvertNomreToLettres.getStringMontant(appservice.getTotalMtTIT(mois, annee));
    String fileLink =
        ReportFactory.createReport(IReport.OrdrePayment_MTTIT, "Ordre de paiement TIT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("montantString", mt_string)
            .addFormat(type)
            .generate()
            .getFileLink();

    response.setView(ActionView.define("Ordre de paiement TIT").add("html", fileLink).map());
  }

  public void imprimerOrdrePayementmt1(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String[] tab_month =
        new String[] {
          "Janvier",
          "Février",
          "Mars",
          "Avril",
          "Mai",
          "Juin",
          "Juiellet",
          "Août",
          "Septembre",
          "Octobre",
          "Novembre",
          "Décembre"
        };
    int annee = (int) request.getContext().get("annee");
    int mois = (int) request.getContext().get("mois");
    String type = request.getContext().get("type_file").toString();
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal mt = BigDecimal.ZERO;
    if (etat != null) {
      mt = (BigDecimal) etat;
    }

    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcarrcomp) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal mt1 = BigDecimal.ZERO;
    if (etat1 != null) {
      mt1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal mt2 = BigDecimal.ZERO;
    if (etat2 != null) {
      mt2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal mt3 = BigDecimal.ZERO;
    if (etat3 != null) {
      mt3 = (BigDecimal) etat3;
    }

    javax.persistence.Query query4 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpapsm)+sum(es.mutuelle_mgpap_ccd) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat4 = query4.getSingleResult();
    BigDecimal mt4 = BigDecimal.ZERO;
    if (etat4 != null) {
      mt4 = (BigDecimal) etat4;
    }

    javax.persistence.Query query5 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_sm)+sum(es.mutuelle_omfam_caad) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat5 = query5.getSingleResult();
    BigDecimal mt5 = BigDecimal.ZERO;
    if (etat5 != null) {
      mt5 = (BigDecimal) etat5;
    }

    javax.persistence.Query query6 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.ir) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat6 = query6.getSingleResult();
    BigDecimal mt6 = BigDecimal.ZERO;
    if (etat6 != null) {
      mt6 = (BigDecimal) etat6;
    }

    javax.persistence.Query query9 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg)+sum(es.rcarrcomp)+sum(es.recore)+sum(es.amo)+sum(es.mutuelle_mgpapsm)+sum(es.mutuelle_mgpap_ccd)+"
                    + "sum(es.mutuelle_omfam_sm)+sum(es.mutuelle_omfam_caad)+sum(es.ir) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat9 = query9.getSingleResult();
    BigDecimal mt9 = BigDecimal.ZERO;
    if (etat9 != null) {
      mt9 = (BigDecimal) etat9;
    }

    javax.persistence.Query query10 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.allocationfamil)+sum(es.total_mensuel_brut617111) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat10 = query10.getSingleResult();
    BigDecimal mt10 = BigDecimal.ZERO;
    if (etat10 != null) {
      mt10 = (BigDecimal) etat10;
    }

    float NETAORDONNANCER = (mt10.subtract(mt9)).floatValue();
    double number2 = (Math.round(NETAORDONNANCER * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdrePayment_MTTIT1, "Ordre de paiement TIT contractuel")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("data", mt)
            .addParam("data_1", mt1)
            .addParam("data_2", mt2)
            .addParam("data_3", mt3)
            .addParam("data_4", mt4)
            .addParam("data_5", mt5)
            .addParam("data_6", mt6)
            .addParam("data_9", mt9)
            .addParam("data_10", mt10)
            .addParam("data_11", NETAORDONNANCER)
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();

    response.setView(
        ActionView.define("Ordre de paiement TIT contractuel").add("html", fileLink).map());
  }
  // BeaudereauEmission
  public void editBeaudereauEmissionpdf(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }

    BigDecimal total = appservice.getTotalGenerale(mois, annee);
    total = total.setScale(2, RoundingMode.HALF_UP);

    String montantString = ConvertNomreToLettres.getStringMontant(total);

    String fileLink =
        ReportFactory.createReport(IReport.BordereauDemission, "BordereauDemission")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("montantString", montantString)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Bordereau d'émission").add("html", fileLink).map());
  }

  public void editBeaudereauEmissionword(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }

    BigDecimal total = appservice.getTotalGenerale(mois, annee);
    total = total.setScale(2, RoundingMode.HALF_UP);

    String montantString = ConvertNomreToLettres.getStringMontant(total);

    String fileLink =
        ReportFactory.createReport(IReport.BordereauDemission, "BordereauDemission")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("montantString", montantString)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Bordereau d'émission").add("html", fileLink).map());
  }

  public void editBeaudereauEmissionexcel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }

    BigDecimal total = appservice.getTotalGenerale(mois, annee);
    total = total.setScale(2, RoundingMode.HALF_UP);

    String montantString = ConvertNomreToLettres.getStringMontant(total);

    String fileLink =
        ReportFactory.createReport(IReport.BordereauDemission, "BordereauDemission")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("montantString", montantString)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Bordereau d'émission").add("html", fileLink).map());
  }
  //// ********
  // editetatrcarsal
  public void editEtatRCARSalpdf(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as Rcar from hr_etat_salaire es left join hr_employee he\n"
                    + "                                  on es.employee = he.id where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg) as Rcarrcomp from hr_etat_salaire es left join hr_employee he"
                    + " on es.employee = he.id where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcarrcomp) as Rcarrcomp from hr_etat_salaire es  left join hr_employee he "
                    + "on es.employee = he.id where he.is_dispose=false and  es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal somme3 = BigDecimal.ZERO;
    if (etat3 != null) {
      somme3 = (BigDecimal) etat3;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatRcarSal, "EtatRcarSal")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("total3", somme3)
            .addParam("mois_name_fr", tableau_mois_francais[mois - 1])
            .addParam("totalGeneral", somme2.add(somme3))
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat RCAR Salariale").add("html", fileLink).map());
  }

  public void editEtatRCARSalword(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as Rcar from hr_etat_salaire es left join hr_employee he\n"
                    + "                                  on es.employee = he.id where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg) as Rcarrcomp from hr_etat_salaire es left join hr_employee he"
                    + " on es.employee = he.id where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcarrcomp) as Rcarrcomp from hr_etat_salaire es  left join hr_employee he "
                    + "on es.employee = he.id where he.is_dispose=false and  es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal somme3 = BigDecimal.ZERO;
    if (etat3 != null) {
      somme3 = (BigDecimal) etat3;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatRcarSal, "EtatRcarSal")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("total3", somme3)
            .addParam("mois_name_fr", tableau_mois_francais[mois - 1])
            .addParam("totalGeneral", somme2.add(somme3))
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat RCAR Salariale").add("html", fileLink).map());
  }

  public void editEtatRCARSalexcel(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as Rcar from hr_etat_salaire es left join hr_employee he\n"
                    + "                                  on es.employee = he.id where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg) as Rcarrcomp from hr_etat_salaire es left join hr_employee he"
                    + " on es.employee = he.id where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcarrcomp) as Rcarrcomp from hr_etat_salaire es  left join hr_employee he "
                    + "on es.employee = he.id where he.is_dispose=false and  es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal somme3 = BigDecimal.ZERO;
    if (etat3 != null) {
      somme3 = (BigDecimal) etat3;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatRcarSal, "EtatRcarSal")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("total3", somme3)
            .addParam("mois_name_fr", tableau_mois_francais[mois - 1])
            .addParam("totalGeneral", somme2.add(somme3))
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat RCAR Salariale").add("html", fileLink).map());
  }
  // *******
  // editbeaudereaumensuel
  public void editBeaudereauMensuelpdf(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal somme = BigDecimal.ZERO;
    if (etat != null) {
      somme = (BigDecimal) etat;
    }
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.BordereauMensuel, "BordereauMensuel")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("total", somme)
            .addParam("total1", somme)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Bordereau mensuel").add("html", fileLink).map());
  }

  public void editBeaudereauMensuelword(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal somme = BigDecimal.ZERO;
    if (etat != null) {
      somme = (BigDecimal) etat;
    }
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.BordereauMensuel, "BordereauMensuel")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("total", somme)
            .addParam("total1", somme)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Bordereau mensuel").add("html", fileLink).map());
  }

  public void editBeaudereauMensuelexcel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal somme = BigDecimal.ZERO;
    if (etat != null) {
      somme = (BigDecimal) etat;
    }
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.BordereauMensuel, "BordereauMensuel")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("total", somme)
            .addParam("total1", somme)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Bordereau mensuel").add("html", fileLink).map());
  }
  // *******
  // editvignet
  public void editVignettepdf(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String[] tab_month =
        new String[] {
          "Janvier",
          "Février",
          "Mars",
          "Avril",
          "Mai",
          "Juin",
          "Juiellet",
          "Août",
          "Septembre",
          "Octobre",
          "Novembre",
          "Décembre"
        };
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }

    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal somme = BigDecimal.ZERO;
    if (etat != null) {
      somme = (BigDecimal) etat;
    }
    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.Vignette, "Vignette")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", tab_month[mois - 1])
            .addParam("annee", annee)
            .addParam("total", somme)
            .addParam("total1", somme)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Vignette").add("html", fileLink).map());
  }

  public void editVignetteword(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String[] tab_month =
        new String[] {
          "Janvier",
          "Février",
          "Mars",
          "Avril",
          "Mai",
          "Juin",
          "Juiellet",
          "Août",
          "Septembre",
          "Octobre",
          "Novembre",
          "Décembre"
        };
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }

    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal somme = BigDecimal.ZERO;
    if (etat != null) {
      somme = (BigDecimal) etat;
    }
    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.Vignette, "Vignette")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", tab_month[mois - 1])
            .addParam("annee", annee)
            .addParam("total", somme)
            .addParam("total1", somme)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Vignette").add("html", fileLink).map());
  }

  public void editVignetteexcel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String[] tab_month =
        new String[] {
          "Janvier",
          "Février",
          "Mars",
          "Avril",
          "Mai",
          "Juin",
          "Juiellet",
          "Août",
          "Septembre",
          "Octobre",
          "Novembre",
          "Décembre"
        };
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }

    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal somme = BigDecimal.ZERO;
    if (etat != null) {
      somme = (BigDecimal) etat;
    }
    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.Vignette, "Vignette")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", tab_month[mois - 1])
            .addParam("annee", annee)
            .addParam("total", somme)
            .addParam("total1", somme)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Vignette").add("html", fileLink).map());
  }
  // *****
  // editetatamo
  public void editEtatAMOpdf(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as TOTAL1 from hr_etat_salaire es left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2 and he.is_dispose=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) as TOTAL2 from hr_etat_salaire es left join hr_employee he on es.employee = he.id\n"
                    + "where es.mois=?1 and es.annee=?2 and he.is_dispose=false ")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }
    float sommeRCAR = ((BigDecimal) etat1).floatValue() + ((BigDecimal) etat2).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    BigDecimal total_amo = appservice.getTotalAmo(mois, annee);
    String mt_string =
        ConvertNomreToLettres.getStringMontant(
            total_amo.multiply(BigDecimal.valueOf(2l)).setScale(2, RoundingMode.HALF_UP));

    String fileLink =
        ReportFactory.createReport(IReport.EtatAMO, "EtatAMO")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("montant", mt_string)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat AMO").add("html", fileLink).map());
  }

  public void editEtatAMOword(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as TOTAL1 from hr_etat_salaire es left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2 and he.is_dispose=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) as TOTAL2 from hr_etat_salaire es left join hr_employee he on es.employee = he.id\n"
                    + "where es.mois=?1 and es.annee=?2 and he.is_dispose=false ")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }
    float sommeRCAR = ((BigDecimal) etat1).floatValue() + ((BigDecimal) etat2).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    BigDecimal total_amo = appservice.getTotalAmo(mois, annee);
    String mt_string =
        ConvertNomreToLettres.getStringMontant(
            total_amo.multiply(BigDecimal.valueOf(2l)).setScale(2, RoundingMode.HALF_UP));

    String fileLink =
        ReportFactory.createReport(IReport.EtatAMO, "EtatAMO")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("montant", mt_string)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat AMO").add("html", fileLink).map());
  }

  public void editEtatAMOexcel(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as TOTAL1 from hr_etat_salaire es left join hr_employee he on es.employee = he.id where es.mois=?1 and es.annee=?2 and he.is_dispose=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) as TOTAL2 from hr_etat_salaire es left join hr_employee he on es.employee = he.id\n"
                    + "where es.mois=?1 and es.annee=?2 and he.is_dispose=false ")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }
    float sommeRCAR = ((BigDecimal) etat1).floatValue() + ((BigDecimal) etat2).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    BigDecimal total_amo = appservice.getTotalAmo(mois, annee);
    String mt_string =
        ConvertNomreToLettres.getStringMontant(
            total_amo.multiply(BigDecimal.valueOf(2l)).setScale(2, RoundingMode.HALF_UP));

    String fileLink =
        ReportFactory.createReport(IReport.EtatAMO, "EtatAMO")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("montant", mt_string)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat AMO").add("html", fileLink).map());
  }
  // *******
  // editetatmgpap
  public void editEtatMGPAPpdf(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String moiss = mois < 10 ? "0" + mois : String.valueOf(mois);
    LocalDate d1 = LocalDate.parse(annee + "-" + moiss + "-01");
    LocalDate d2 = LocalDate.parse(annee + "-" + moiss + "-" + d1.lengthOfMonth());

    Date date1 = java.sql.Date.valueOf(d1);
    Date date2 = java.sql.Date.valueOf(d2);

    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as brut from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) as amo from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpapsm) as sm from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal somme3 = BigDecimal.ZERO;
    if (etat3 != null) {
      somme3 = (BigDecimal) etat3;
    }

    javax.persistence.Query query4 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpap_ccd) as ccd from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat4 = query4.getSingleResult();
    BigDecimal somme4 = BigDecimal.ZERO;
    if (etat4 != null) {
      somme4 = (BigDecimal) etat4;
    }
    BigDecimal tot1_2 = somme4.add(somme3).setScale(2, RoundingMode.HALF_UP);
    String montant1 = ConvertNomreToLettres.getStringMontant(somme3);
    String montant2 = ConvertNomreToLettres.getStringMontant(somme4);
    String montant3 = ConvertNomreToLettres.getStringMontant(tot1_2);

    String fileLink =
        ReportFactory.createReport(IReport.EtatMGPAP, "Etat MGPAP")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("debut1", date1)
            .addParam("fin1", date2)
            .addParam("montant1", montant1)
            .addParam("montant2", montant2)
            .addParam("montant3", montant3)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat MGPAP SM CCD").add("html", fileLink).map());
  }

  public void editEtatMGPAPword(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String moiss = mois < 10 ? "0" + mois : String.valueOf(mois);
    LocalDate d1 = LocalDate.parse(annee + "-" + moiss + "-01");
    LocalDate d2 = LocalDate.parse(annee + "-" + moiss + "-" + d1.lengthOfMonth());

    Date date1 = java.sql.Date.valueOf(d1);
    Date date2 = java.sql.Date.valueOf(d2);

    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as brut from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) as amo from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpapsm) as sm from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal somme3 = BigDecimal.ZERO;
    if (etat3 != null) {
      somme3 = (BigDecimal) etat3;
    }

    javax.persistence.Query query4 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpap_ccd) as ccd from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat4 = query4.getSingleResult();
    BigDecimal somme4 = BigDecimal.ZERO;
    if (etat4 != null) {
      somme4 = (BigDecimal) etat4;
    }
    BigDecimal tot1_2 = somme4.add(somme3).setScale(2, RoundingMode.HALF_UP);
    String montant1 = ConvertNomreToLettres.getStringMontant(somme3);
    String montant2 = ConvertNomreToLettres.getStringMontant(somme4);
    String montant3 = ConvertNomreToLettres.getStringMontant(tot1_2);

    String fileLink =
        ReportFactory.createReport(IReport.EtatMGPAP, "Etat MGPAP")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("debut1", date1)
            .addParam("fin1", date2)
            .addParam("montant1", montant1)
            .addParam("montant2", montant2)
            .addParam("montant3", montant3)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat MGPAP SM CCD").add("html", fileLink).map());
  }

  public void editEtatMGPAPexcel(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String moiss = mois < 10 ? "0" + mois : String.valueOf(mois);
    LocalDate d1 = LocalDate.parse(annee + "-" + moiss + "-01");
    LocalDate d2 = LocalDate.parse(annee + "-" + moiss + "-" + d1.lengthOfMonth());

    Date date1 = java.sql.Date.valueOf(d1);
    Date date2 = java.sql.Date.valueOf(d2);

    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as brut from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) as amo from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpapsm) as sm from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal somme3 = BigDecimal.ZERO;
    if (etat3 != null) {
      somme3 = (BigDecimal) etat3;
    }

    javax.persistence.Query query4 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpap_ccd) as ccd from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='MGPAP'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat4 = query4.getSingleResult();
    BigDecimal somme4 = BigDecimal.ZERO;
    if (etat4 != null) {
      somme4 = (BigDecimal) etat4;
    }
    BigDecimal tot1_2 = somme4.add(somme3).setScale(2, RoundingMode.HALF_UP);
    String montant1 = ConvertNomreToLettres.getStringMontant(somme3);
    String montant2 = ConvertNomreToLettres.getStringMontant(somme4);
    String montant3 = ConvertNomreToLettres.getStringMontant(tot1_2);

    String fileLink =
        ReportFactory.createReport(IReport.EtatMGPAP, "Etat MGPAP")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("debut1", date1)
            .addParam("fin1", date2)
            .addParam("montant1", montant1)
            .addParam("montant2", montant2)
            .addParam("montant3", montant3)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat MGPAP SM CCD").add("html", fileLink).map());
  }
  // *****
  // editetatomfam
  public void editEtatOMFAMpdf(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String moiss = mois < 10 ? "0" + mois : String.valueOf(mois);
    LocalDate d1 = LocalDate.parse(annee + "-" + moiss + "-01");
    LocalDate d2 = LocalDate.parse(annee + "-" + moiss + "-" + d1.lengthOfMonth());
    Date date1 = java.sql.Date.valueOf(d1);
    Date date2 = java.sql.Date.valueOf(d2);
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as OMFAM from hr_etat_salaire es left join hr_employee he on es.employee = he.id left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + " where com.name='OMFAM' and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_sm) as OMFAMsm from hr_etat_salaire es left join hr_employee he on es.employee = he.id\n"
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id"
                    + " where es.mois=?1 and es.annee=?2 and com.name='OMFAM'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_caad) as OMFAMcaad from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='OMFAM'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal somme3 = BigDecimal.ZERO;
    if (etat3 != null) {
      somme3 = (BigDecimal) etat3;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatOMFAM, "EtatOMFAM")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("debut1", date1)
            .addParam("fin1", date2)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("total3", somme3)
            .addParam("totalGeneral", somme2.add(somme3))
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat OMFAM SM CAAD").add("html", fileLink).map());
  }

  public void editEtatOMFAMword(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String moiss = mois < 10 ? "0" + mois : String.valueOf(mois);
    LocalDate d1 = LocalDate.parse(annee + "-" + moiss + "-01");
    LocalDate d2 = LocalDate.parse(annee + "-" + moiss + "-" + d1.lengthOfMonth());
    Date date1 = java.sql.Date.valueOf(d1);
    Date date2 = java.sql.Date.valueOf(d2);
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as OMFAM from hr_etat_salaire es left join hr_employee he on es.employee = he.id left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + " where com.name='OMFAM' and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_sm) as OMFAMsm from hr_etat_salaire es left join hr_employee he on es.employee = he.id\n"
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id"
                    + " where es.mois=?1 and es.annee=?2 and com.name='OMFAM'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_caad) as OMFAMcaad from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='OMFAM'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal somme3 = BigDecimal.ZERO;
    if (etat3 != null) {
      somme3 = (BigDecimal) etat3;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatOMFAM, "EtatOMFAM")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("debut1", date1)
            .addParam("fin1", date2)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("total3", somme3)
            .addParam("totalGeneral", somme2.add(somme3))
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat OMFAM SM CAAD").add("html", fileLink).map());
  }

  public void editEtatOMFAMexcel(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String moiss = mois < 10 ? "0" + mois : String.valueOf(mois);
    LocalDate d1 = LocalDate.parse(annee + "-" + moiss + "-01");
    LocalDate d2 = LocalDate.parse(annee + "-" + moiss + "-" + d1.lengthOfMonth());
    Date date1 = java.sql.Date.valueOf(d1);
    Date date2 = java.sql.Date.valueOf(d2);
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as OMFAM from hr_etat_salaire es left join hr_employee he on es.employee = he.id left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + " where com.name='OMFAM' and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_sm) as OMFAMsm from hr_etat_salaire es left join hr_employee he on es.employee = he.id\n"
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id"
                    + " where es.mois=?1 and es.annee=?2 and com.name='OMFAM'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_caad) as OMFAMcaad from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "left join configuration_organisme_metuelle com on he.organisme_metuelle2 = com.id "
                    + "where es.mois=?1 and es.annee=?2 and com.name='OMFAM'")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal somme3 = BigDecimal.ZERO;
    if (etat3 != null) {
      somme3 = (BigDecimal) etat3;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatOMFAM, "EtatOMFAM")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("debut1", date1)
            .addParam("fin1", date2)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("total3", somme3)
            .addParam("totalGeneral", somme2.add(somme3))
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat OMFAM SM CAAD").add("html", fileLink).map());
  }
  // *****
  // editetetir
  public void editEtatIRpdf(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as Rcar from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.ir) as IR from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatIR, "EtatIR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("string_mois", tableau_mois_francais[mois - 1])
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("totalGeneral", somme2)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat IR").add("html", fileLink).map());
  }

  public void editEtatIRword(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as Rcar from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.ir) as IR from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatIR, "EtatIR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("string_mois", tableau_mois_francais[mois - 1])
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("totalGeneral", somme2)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat IR").add("html", fileLink).map());
  }

  public void editEtatIRexcel(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as Rcar from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.ir) as IR from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatIR, "EtatIR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("string_mois", tableau_mois_francais[mois - 1])
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("totalGeneral", somme2)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat IR").add("html", fileLink).map());
  }
  // ****
  // editetataxa
  public void editEtatAXApdf(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(hgc.remboursement) as TOTAL1 from hr_etat_salaire es left join hr_employee he on es.employee = he.id left join hr_gestion_credit hgc on he.id = hgc.employee where es.mois=?1 and es.annee=?2 and hgc.remboursement is not null")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }
    String montant = ConvertNomreToLettres.getStringMontant(somme1);
    String fileLink =
        ReportFactory.createReport(IReport.EtatAXA, "Etat AXA Crédit")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("total1", somme1)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat AXA Crédit").add("html", fileLink).map());
  }

  public void editEtatAXAword(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(hgc.remboursement) as TOTAL1 from hr_etat_salaire es left join hr_employee he on es.employee = he.id left join hr_gestion_credit hgc on he.id = hgc.employee where es.mois=?1 and es.annee=?2 and hgc.remboursement is not null")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }
    String montant = ConvertNomreToLettres.getStringMontant(somme1);
    String fileLink =
        ReportFactory.createReport(IReport.EtatAXA, "Etat AXA Crédit")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("total1", somme1)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat AXA Crédit").add("html", fileLink).map());
  }

  public void editEtatAXAexcel(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(hgc.remboursement) as TOTAL1 from hr_etat_salaire es left join hr_employee he on es.employee = he.id left join hr_gestion_credit hgc on he.id = hgc.employee where es.mois=?1 and es.annee=?2 and hgc.remboursement is not null")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }
    String montant = ConvertNomreToLettres.getStringMontant(somme1);
    String fileLink =
        ReportFactory.createReport(IReport.EtatAXA, "Etat AXA Crédit")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("total1", somme1)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat AXA Crédit").add("html", fileLink).map());
  }
  // ****
  // editetatrcar
  public void editEtatRCARpdf(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as salaire from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal somme = BigDecimal.ZERO;
    if (etat != null) {
      somme = (BigDecimal) etat;
    }

    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg) as Rcar from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcarrcomp) as Rcarrcomp from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatRCAR, "EtatRCAR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("mois_name_fr", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("total", somme)
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("total3", somme1.add(somme2))
            .addParam("totalGeneral", (somme1).add(somme1).add(somme2))
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat RCAR").add("html", fileLink).map());
  }

  public void editEtatRCARword(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as salaire from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal somme = BigDecimal.ZERO;
    if (etat != null) {
      somme = (BigDecimal) etat;
    }

    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg) as Rcar from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcarrcomp) as Rcarrcomp from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatRCAR, "EtatRCAR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("mois_name_fr", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("total", somme)
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("total3", somme1.add(somme2))
            .addParam("totalGeneral", (somme1).add(somme1).add(somme2))
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat RCAR").add("html", fileLink).map());
  }

  public void editEtatRCARexcel(ActionRequest request, ActionResponse response)
      throws AxelorException, ParseException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.total_salair_mensuel_brut_imposable) as salaire from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal somme = BigDecimal.ZERO;
    if (etat != null) {
      somme = (BigDecimal) etat;
    }

    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg) as Rcar from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal somme1 = BigDecimal.ZERO;
    if (etat1 != null) {
      somme1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcarrcomp) as Rcarrcomp from hr_etat_salaire es left join hr_employee he on es.employee = he.id "
                    + "where he.is_dispose=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal somme2 = BigDecimal.ZERO;
    if (etat2 != null) {
      somme2 = (BigDecimal) etat2;
    }

    String fileLink =
        ReportFactory.createReport(IReport.EtatRCAR, "EtatRCAR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("mois_name_fr", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("total", somme)
            .addParam("total1", somme1)
            .addParam("total2", somme2)
            .addParam("total3", somme1.add(somme2))
            .addParam("totalGeneral", (somme1).add(somme1).add(somme2))
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Etat RCAR").add("html", fileLink).map());
  }
  // ****************
  // imprimerordrepayement
  public void imprimerOrdrePayementmtpdf(ActionRequest request, ActionResponse response)
      throws AxelorException {
    int annee = (int) request.getContext().get("annee");
    int mois = (int) request.getContext().get("mois");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String mt_string =
        ConvertNomreToLettres.getStringMontant(appservice.getTotalMtTIT(mois, annee));
    String fileLink =
        ReportFactory.createReport(IReport.OrdrePayment_MTTIT, "Ordre de paiement TIT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("montantString", mt_string)
            .addFormat(type)
            .generate()
            .getFileLink();

    response.setView(ActionView.define("Ordre de paiement TIT").add("html", fileLink).map());
  }

  public void imprimerOrdrePayementmtword(ActionRequest request, ActionResponse response)
      throws AxelorException {
    int annee = (int) request.getContext().get("annee");
    int mois = (int) request.getContext().get("mois");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String mt_string =
        ConvertNomreToLettres.getStringMontant(appservice.getTotalMtTIT(mois, annee));
    String fileLink =
        ReportFactory.createReport(IReport.OrdrePayment_MTTIT, "Ordre de paiement TIT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("montantString", mt_string)
            .addFormat(type)
            .generate()
            .getFileLink();

    response.setView(ActionView.define("Ordre de paiement TIT").add("html", fileLink).map());
  }

  public void imprimerOrdrePayementmtexcel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    int annee = (int) request.getContext().get("annee");
    int mois = (int) request.getContext().get("mois");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String mt_string =
        ConvertNomreToLettres.getStringMontant(appservice.getTotalMtTIT(mois, annee));
    String fileLink =
        ReportFactory.createReport(IReport.OrdrePayment_MTTIT, "Ordre de paiement TIT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("montantString", mt_string)
            .addFormat(type)
            .generate()
            .getFileLink();

    response.setView(ActionView.define("Ordre de paiement TIT").add("html", fileLink).map());
  }
  // ****
  // rcar
  public void RCARpdf(ActionRequest request, ActionResponse response) throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String code = "617111";
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String req =
        "select sum(es.rcar_rg) as Rcar,sum(es.rcarrcomp) as Rcarrcomp from hr_etat_salaire es  where es.mois= ?1 and es.annee= ?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);
    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.OrdredesPaiements, "RCAR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "R.C.A.R")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION SALARIALE AU R.C.A.R DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "R.C.A.R")
            .addParam("Objet2", "CC.RCAR")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("RCAR").add("html", fileLink).map());
  }

  public void RCARword(ActionRequest request, ActionResponse response) throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String code = "617111";
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String req =
        "select sum(es.rcar_rg) as Rcar,sum(es.rcarrcomp) as Rcarrcomp from hr_etat_salaire es  where es.mois= ?1 and es.annee= ?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);
    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.OrdredesPaiements, "RCAR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "R.C.A.R")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION SALARIALE AU R.C.A.R DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "R.C.A.R")
            .addParam("Objet2", "CC.RCAR")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("RCAR").add("html", fileLink).map());
  }

  public void RCARexcel(ActionRequest request, ActionResponse response) throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String code = "617111";
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String req =
        "select sum(es.rcar_rg) as Rcar,sum(es.rcarrcomp) as Rcarrcomp from hr_etat_salaire es  where es.mois= ?1 and es.annee= ?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);
    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.OrdredesPaiements, "RCAR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "R.C.A.R")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION SALARIALE AU R.C.A.R DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "R.C.A.R")
            .addParam("Objet2", "CC.RCAR")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("RCAR").add("html", fileLink).map());
  }
  // ******
  // IR
  public void IRpdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String code = "617111";
    String req =
        "select sum(es.ir) as ir from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "IR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "LE TRESORIER PREFECTORAL DE FES")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " IMPOT SUR REVENU DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("IR").add("html", fileLink).map());
  }

  public void IRword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String code = "617111";
    String req =
        "select sum(es.ir) as ir from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "IR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "LE TRESORIER PREFECTORAL DE FES")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " IMPOT SUR REVENU DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("IR").add("html", fileLink).map());
  }

  public void IRexcel(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String code = "617111";
    String req =
        "select sum(es.ir) as ir from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "IR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "LE TRESORIER PREFECTORAL DE FES")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " IMPOT SUR REVENU DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("IR").add("html", fileLink).map());
  }
  // *****
  // mt_record
  public void MT_Recorepdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        " select sum(es.recore) as Recore from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);
    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT RECORE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "CDG  - CNRA")
            .addParam("Code", code)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION SALARIALE RECORE DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT RECORE").add("html", fileLink).map());
  }

  public void MT_Recoreword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        " select sum(es.recore) as Recore from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);
    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT RECORE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "CDG  - CNRA")
            .addParam("Code", code)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION SALARIALE RECORE DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT RECORE").add("html", fileLink).map());
  }

  public void MT_Recoreexcel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        " select sum(es.recore) as Recore from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);
    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT RECORE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "CDG  - CNRA")
            .addParam("Code", code)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION SALARIALE RECORE DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT RECORE").add("html", fileLink).map());
  }
  //
  // OMFAM
  public void OMFAMpdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.mutuelle_omfam_sm) as mutuelle_omfam_sm,sum(es.mutuelle_omfam_caad) as mutuelle_omfam_caad from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.OrdredesPaiements, "OMFAM")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "O.M.F.A.M")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "Prélèvement mutualiste (OMFAM) mois ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "MUTUELLE SECTEUR MUTUALISTE")
            .addParam("Objet2", "MUTUELLE C.A.A.D")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("OMFAM").add("html", fileLink).map());
  }

  public void OMFAMword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.mutuelle_omfam_sm) as mutuelle_omfam_sm,sum(es.mutuelle_omfam_caad) as mutuelle_omfam_caad from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.OrdredesPaiements, "OMFAM")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "O.M.F.A.M")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "Prélèvement mutualiste (OMFAM) mois ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "MUTUELLE SECTEUR MUTUALISTE")
            .addParam("Objet2", "MUTUELLE C.A.A.D")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("OMFAM").add("html", fileLink).map());
  }

  public void OMFAMexcel(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.mutuelle_omfam_sm) as mutuelle_omfam_sm,sum(es.mutuelle_omfam_caad) as mutuelle_omfam_caad from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.OrdredesPaiements, "OMFAM")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "O.M.F.A.M")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "Prélèvement mutualiste (OMFAM) mois ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "MUTUELLE SECTEUR MUTUALISTE")
            .addParam("Objet2", "MUTUELLE C.A.A.D")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("OMFAM").add("html", fileLink).map());
  }
  // *****
  // cnops
  public void Cnopsppdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "61743";
    String req =
        "select sum(es.amo) as amo from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "CNOPSP")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "C.N.O.P.S")
            .addParam("Code", code)
            .addParam("Rubrique", "COTISATIONS AUX MUTUELLES")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION PATRONALE AUX MUTUELLES DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("CNOPSP").add("html", fileLink).map());
  }

  public void Cnopspword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "61743";
    String req =
        "select sum(es.amo) as amo from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "CNOPSP")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "C.N.O.P.S")
            .addParam("Code", code)
            .addParam("Rubrique", "COTISATIONS AUX MUTUELLES")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION PATRONALE AUX MUTUELLES DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("CNOPSP").add("html", fileLink).map());
  }

  public void Cnopspexcel(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "61743";
    String req =
        "select sum(es.amo) as amo from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "CNOPSP")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "C.N.O.P.S")
            .addParam("Code", code)
            .addParam("Rubrique", "COTISATIONS AUX MUTUELLES")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION PATRONALE AUX MUTUELLES DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("CNOPSP").add("html", fileLink).map());
  }
  // ******
  // axa
  public void Axapdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.pret_axa_credit) as axa from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "AXA CREDIT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "AXA CREDIT")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " PRELEVEMENT SUR SALAIRE DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("AXA CREDIT").add("html", fileLink).map());
  }

  public void Axaword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.pret_axa_credit) as axa from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "AXA CREDIT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "AXA CREDIT")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " PRELEVEMENT SUR SALAIRE DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("AXA CREDIT").add("html", fileLink).map());
  }

  public void Axaexcel(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.pret_axa_credit) as axa from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "AXA CREDIT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "AXA CREDIT")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " PRELEVEMENT SUR SALAIRE DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("AXA CREDIT").add("html", fileLink).map());
  }
  ///// *****
  // mt_amo
  public void MT_Amopdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.amo) as amo from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT AMO")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "C.N.O.P.S")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (A.M.O) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT AMO").add("html", fileLink).map());
  }

  public void MT_Amoword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.amo) as amo from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT AMO")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "C.N.O.P.S")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (A.M.O) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT AMO").add("html", fileLink).map());
  }

  public void MT_Amoexcel(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.amo) as amo from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT AMO")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "C.N.O.P.S")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (A.M.O) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT AMO").add("html", fileLink).map());
  }
  // ****
  // mt_sm_cdd
  public void MT_SM_CDDpdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.mutuelle_mgpapsm) as mutuelle_sm,sum(es.mutuelle_mgpap_ccd) as mutuelle_ccd from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true and is_disposition=false";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.OrdredesPaiements, "MT SM CDD")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "M.G.P.A.P")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (S.M) ET (C.C.D) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "MUTUELLE SECTEUR MUTUALISTE")
            .addParam("Objet2", "MUTUELLE C.C.D")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT SM CDD").add("html", fileLink).map());
  }

  public void MT_SM_CDDword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.mutuelle_mgpapsm) as mutuelle_sm,sum(es.mutuelle_mgpap_ccd) as mutuelle_ccd from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true and is_disposition=false";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.OrdredesPaiements, "MT SM CDD")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "M.G.P.A.P")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (S.M) ET (C.C.D) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "MUTUELLE SECTEUR MUTUALISTE")
            .addParam("Objet2", "MUTUELLE C.C.D")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT SM CDD").add("html", fileLink).map());
  }

  public void MT_SM_CDDexcel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617111";
    String req =
        "select sum(es.mutuelle_mgpapsm) as mutuelle_sm,sum(es.mutuelle_mgpap_ccd) as mutuelle_ccd from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true and is_disposition=false";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.OrdredesPaiements, "MT SM CDD")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "M.G.P.A.P")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL TITULAIRE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (S.M) ET (C.C.D) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "MUTUELLE SECTEUR MUTUALISTE")
            .addParam("Objet2", "MUTUELLE C.C.D")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT SM CDD").add("html", fileLink).map());
  }
  // *******
  // rcarp
  public void RcarPpdf(ActionRequest request, ActionResponse response) throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617422";
    String req =
        "select sum(es.rcar_rg*2) as Rcar,sum(es.rcarrcomp)+sum(es.rcar_comp617131) as Rcarrcomp from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdredesPaiements, "RCAR PATRONALE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "R.C.A.R")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam(
                "Rubrique",
                "COTISATIONS  PATRONALES  AU  REGIME  COLLECTIF D'ALLOCATION DE  RETRAITES (RCAR)")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION PATRONALE AU R.C.A.R DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "COTISATION PATRONALE AU RCAR")
            .addParam("Objet2", "COTISATION PATRONALE AU CC. RCAR")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("RCAR PATRONALE").add("html", fileLink).map());
  }

  public void RcarPword(ActionRequest request, ActionResponse response) throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617422";
    String req =
        "select sum(es.rcar_rg*2) as Rcar,sum(es.rcarrcomp)+sum(es.rcar_comp617131) as Rcarrcomp from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdredesPaiements, "RCAR PATRONALE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "R.C.A.R")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam(
                "Rubrique",
                "COTISATIONS  PATRONALES  AU  REGIME  COLLECTIF D'ALLOCATION DE  RETRAITES (RCAR)")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION PATRONALE AU R.C.A.R DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "COTISATION PATRONALE AU RCAR")
            .addParam("Objet2", "COTISATION PATRONALE AU CC. RCAR")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("RCAR PATRONALE").add("html", fileLink).map());
  }

  public void RcarPexcel(ActionRequest request, ActionResponse response) throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617422";
    String req =
        "select sum(es.rcar_rg*2) as Rcar,sum(es.rcarrcomp)+sum(es.rcar_comp617131) as Rcarrcomp from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdredesPaiements, "RCAR PATRONALE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "R.C.A.R")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam(
                "Rubrique",
                "COTISATIONS  PATRONALES  AU  REGIME  COLLECTIF D'ALLOCATION DE  RETRAITES (RCAR)")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION PATRONALE AU R.C.A.R DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "COTISATION PATRONALE AU RCAR")
            .addParam("Objet2", "COTISATION PATRONALE AU CC. RCAR")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("RCAR PATRONALE").add("html", fileLink).map());
  }
  // *****
  // mtamo
  public void MtAmoCpdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617112";
    String req =
        "select sum(es.amo) as amo from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT AMO C")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "C.N.O.P.S")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL CONTRACTUEL")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (A.M.O) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT AMO C").add("html", fileLink).map());
  }

  public void MtAmoCword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617112";
    String req =
        "select sum(es.amo) as amo from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT AMO C")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "C.N.O.P.S")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL CONTRACTUEL")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (A.M.O) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT AMO C").add("html", fileLink).map());
  }

  public void MtAmoCexcel(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617112";
    String req =
        "select sum(es.amo) as amo from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT AMO C")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "C.N.O.P.S")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL CONTRACTUEL")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (A.M.O) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT AMO C").add("html", fileLink).map());
  }
  // *****
  // irc
  public void IRCpdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617112";
    String req =
        "select sum(es.ir) as ir from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "IR CONTRACTUELLE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "LE TRESORIER PREFECTORAL DE FES")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL CONTRACTUEL")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " IMPOT SUR REVENU DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("IR CONTRACTUEL").add("html", fileLink).map());
  }

  public void IRCword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617112";
    String req =
        "select sum(es.ir) as ir from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "IR CONTRACTUELLE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "LE TRESORIER PREFECTORAL DE FES")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL CONTRACTUEL")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " IMPOT SUR REVENU DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("IR CONTRACTUEL").add("html", fileLink).map());
  }

  public void IRCexcel(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617112";
    String req =
        "select sum(es.ir) as ir from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "IR CONTRACTUELLE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "LE TRESORIER PREFECTORAL DE FES")
            .addParam("Code", code)
            .addParam("Rubrique", "PERSONNEL CONTRACTUEL")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " IMPOT SUR REVENU DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("IR CONTRACTUEL").add("html", fileLink).map());
  }
  // *****

  // imprimerorderpayement
  public void imprimerOrdrePayementmt1pdf(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String[] tab_month =
        new String[] {
          "Janvier",
          "Février",
          "Mars",
          "Avril",
          "Mai",
          "Juin",
          "Juiellet",
          "Août",
          "Septembre",
          "Octobre",
          "Novembre",
          "Décembre"
        };
    int annee = (int) request.getContext().get("annee");
    int mois = (int) request.getContext().get("mois");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal mt = BigDecimal.ZERO;
    if (etat != null) {
      mt = (BigDecimal) etat;
    }

    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcarrcomp) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal mt1 = BigDecimal.ZERO;
    if (etat1 != null) {
      mt1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal mt2 = BigDecimal.ZERO;
    if (etat2 != null) {
      mt2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal mt3 = BigDecimal.ZERO;
    if (etat3 != null) {
      mt3 = (BigDecimal) etat3;
    }

    javax.persistence.Query query4 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpapsm)+sum(es.mutuelle_mgpap_ccd) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat4 = query4.getSingleResult();
    BigDecimal mt4 = BigDecimal.ZERO;
    if (etat4 != null) {
      mt4 = (BigDecimal) etat4;
    }

    javax.persistence.Query query5 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_sm)+sum(es.mutuelle_omfam_caad) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat5 = query5.getSingleResult();
    BigDecimal mt5 = BigDecimal.ZERO;
    if (etat5 != null) {
      mt5 = (BigDecimal) etat5;
    }

    javax.persistence.Query query6 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.ir) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat6 = query6.getSingleResult();
    BigDecimal mt6 = BigDecimal.ZERO;
    if (etat6 != null) {
      mt6 = (BigDecimal) etat6;
    }

    javax.persistence.Query query9 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg)+sum(es.rcarrcomp)+sum(es.recore)+sum(es.amo)+sum(es.mutuelle_mgpapsm)+sum(es.mutuelle_mgpap_ccd)+"
                    + "sum(es.mutuelle_omfam_sm)+sum(es.mutuelle_omfam_caad)+sum(es.ir) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat9 = query9.getSingleResult();
    BigDecimal mt9 = BigDecimal.ZERO;
    if (etat9 != null) {
      mt9 = (BigDecimal) etat9;
    }

    javax.persistence.Query query10 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.allocationfamil)+sum(es.total_mensuel_brut617111) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat10 = query10.getSingleResult();
    BigDecimal mt10 = BigDecimal.ZERO;
    if (etat10 != null) {
      mt10 = (BigDecimal) etat10;
    }

    float NETAORDONNANCER = (mt10.subtract(mt9)).floatValue();
    double number2 = (Math.round(NETAORDONNANCER * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdrePayment_MTTIT1, "Ordre de paiement TIT contractuel")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("data", mt)
            .addParam("data_1", mt1)
            .addParam("data_2", mt2)
            .addParam("data_3", mt3)
            .addParam("data_4", mt4)
            .addParam("data_5", mt5)
            .addParam("data_6", mt6)
            .addParam("data_9", mt9)
            .addParam("data_10", mt10)
            .addParam("data_11", NETAORDONNANCER)
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();

    response.setView(
        ActionView.define("Ordre de paiement TIT contractuel").add("html", fileLink).map());
  }

  public void imprimerOrdrePayementmt1word(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String[] tab_month =
        new String[] {
          "Janvier",
          "Février",
          "Mars",
          "Avril",
          "Mai",
          "Juin",
          "Juiellet",
          "Août",
          "Septembre",
          "Octobre",
          "Novembre",
          "Décembre"
        };
    int annee = (int) request.getContext().get("annee");
    int mois = (int) request.getContext().get("mois");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal mt = BigDecimal.ZERO;
    if (etat != null) {
      mt = (BigDecimal) etat;
    }

    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcarrcomp) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal mt1 = BigDecimal.ZERO;
    if (etat1 != null) {
      mt1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal mt2 = BigDecimal.ZERO;
    if (etat2 != null) {
      mt2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal mt3 = BigDecimal.ZERO;
    if (etat3 != null) {
      mt3 = (BigDecimal) etat3;
    }

    javax.persistence.Query query4 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpapsm)+sum(es.mutuelle_mgpap_ccd) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat4 = query4.getSingleResult();
    BigDecimal mt4 = BigDecimal.ZERO;
    if (etat4 != null) {
      mt4 = (BigDecimal) etat4;
    }

    javax.persistence.Query query5 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_sm)+sum(es.mutuelle_omfam_caad) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat5 = query5.getSingleResult();
    BigDecimal mt5 = BigDecimal.ZERO;
    if (etat5 != null) {
      mt5 = (BigDecimal) etat5;
    }

    javax.persistence.Query query6 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.ir) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat6 = query6.getSingleResult();
    BigDecimal mt6 = BigDecimal.ZERO;
    if (etat6 != null) {
      mt6 = (BigDecimal) etat6;
    }

    javax.persistence.Query query9 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg)+sum(es.rcarrcomp)+sum(es.recore)+sum(es.amo)+sum(es.mutuelle_mgpapsm)+sum(es.mutuelle_mgpap_ccd)+"
                    + "sum(es.mutuelle_omfam_sm)+sum(es.mutuelle_omfam_caad)+sum(es.ir) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat9 = query9.getSingleResult();
    BigDecimal mt9 = BigDecimal.ZERO;
    if (etat9 != null) {
      mt9 = (BigDecimal) etat9;
    }

    javax.persistence.Query query10 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.allocationfamil)+sum(es.total_mensuel_brut617111) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat10 = query10.getSingleResult();
    BigDecimal mt10 = BigDecimal.ZERO;
    if (etat10 != null) {
      mt10 = (BigDecimal) etat10;
    }

    float NETAORDONNANCER = (mt10.subtract(mt9)).floatValue();
    double number2 = (Math.round(NETAORDONNANCER * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdrePayment_MTTIT1, "Ordre de paiement TIT contractuel")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("data", mt)
            .addParam("data_1", mt1)
            .addParam("data_2", mt2)
            .addParam("data_3", mt3)
            .addParam("data_4", mt4)
            .addParam("data_5", mt5)
            .addParam("data_6", mt6)
            .addParam("data_9", mt9)
            .addParam("data_10", mt10)
            .addParam("data_11", NETAORDONNANCER)
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();

    response.setView(
        ActionView.define("Ordre de paiement TIT contractuel").add("html", fileLink).map());
  }

  public void imprimerOrdrePayementmt1excel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    String[] tab_month =
        new String[] {
          "Janvier",
          "Février",
          "Mars",
          "Avril",
          "Mai",
          "Juin",
          "Juiellet",
          "Août",
          "Septembre",
          "Octobre",
          "Novembre",
          "Décembre"
        };
    int annee = (int) request.getContext().get("annee");
    int mois = (int) request.getContext().get("mois");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal mt = BigDecimal.ZERO;
    if (etat != null) {
      mt = (BigDecimal) etat;
    }

    javax.persistence.Query query1 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcarrcomp) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat1 = query1.getSingleResult();
    BigDecimal mt1 = BigDecimal.ZERO;
    if (etat1 != null) {
      mt1 = (BigDecimal) etat1;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal mt2 = BigDecimal.ZERO;
    if (etat2 != null) {
      mt2 = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal mt3 = BigDecimal.ZERO;
    if (etat3 != null) {
      mt3 = (BigDecimal) etat3;
    }

    javax.persistence.Query query4 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpapsm)+sum(es.mutuelle_mgpap_ccd) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat4 = query4.getSingleResult();
    BigDecimal mt4 = BigDecimal.ZERO;
    if (etat4 != null) {
      mt4 = (BigDecimal) etat4;
    }

    javax.persistence.Query query5 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_sm)+sum(es.mutuelle_omfam_caad) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat5 = query5.getSingleResult();
    BigDecimal mt5 = BigDecimal.ZERO;
    if (etat5 != null) {
      mt5 = (BigDecimal) etat5;
    }

    javax.persistence.Query query6 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.ir) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat6 = query6.getSingleResult();
    BigDecimal mt6 = BigDecimal.ZERO;
    if (etat6 != null) {
      mt6 = (BigDecimal) etat6;
    }

    javax.persistence.Query query9 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.rcar_rg)+sum(es.rcarrcomp)+sum(es.recore)+sum(es.amo)+sum(es.mutuelle_mgpapsm)+sum(es.mutuelle_mgpap_ccd)+"
                    + "sum(es.mutuelle_omfam_sm)+sum(es.mutuelle_omfam_caad)+sum(es.ir) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat9 = query9.getSingleResult();
    BigDecimal mt9 = BigDecimal.ZERO;
    if (etat9 != null) {
      mt9 = (BigDecimal) etat9;
    }

    javax.persistence.Query query10 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.allocationfamil)+sum(es.total_mensuel_brut617111) from hr_etat_salaire es where es.titulaire=false and es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat10 = query10.getSingleResult();
    BigDecimal mt10 = BigDecimal.ZERO;
    if (etat10 != null) {
      mt10 = (BigDecimal) etat10;
    }

    float NETAORDONNANCER = (mt10.subtract(mt9)).floatValue();
    double number2 = (Math.round(NETAORDONNANCER * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdrePayment_MTTIT1, "Ordre de paiement TIT contractuel")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", tableau_mois_francais[mois - 1])
            .addParam("annee", annee)
            .addParam("data", mt)
            .addParam("data_1", mt1)
            .addParam("data_2", mt2)
            .addParam("data_3", mt3)
            .addParam("data_4", mt4)
            .addParam("data_5", mt5)
            .addParam("data_6", mt6)
            .addParam("data_9", mt9)
            .addParam("data_10", mt10)
            .addParam("data_11", NETAORDONNANCER)
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();

    response.setView(
        ActionView.define("Ordre de paiement TIT contractuel").add("html", fileLink).map());
  }
  //
  // mtmgpapc
  public void MtMgpapCpdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617112";
    String req =
        "select sum(es.mutuelle_mgpapsm) as mutuelle_mgpapsm, sum(es.mutuelle_mgpap_ccd) as mutuelle_mgpapsm_ccd from hr_etat_salaire es where es.mois=?1 and es.annee=?2  and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdredesPaiements, "MTMGPAC CONTRACTUEL")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "M.G.P.A.P")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL CONTRACTUEL")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (S.M) ET (C.C.D) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "MUTUELLE SECTEUR MUTUALISTE")
            .addParam("Objet2", "MUTUELLE C.C.D")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MTMGPAC CONTRACTUEL").add("html", fileLink).map());
  }

  public void MtMgpapCword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617112";
    String req =
        "select sum(es.mutuelle_mgpapsm) as mutuelle_mgpapsm, sum(es.mutuelle_mgpap_ccd) as mutuelle_mgpapsm_ccd from hr_etat_salaire es where es.mois=?1 and es.annee=?2  and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdredesPaiements, "MTMGPAC CONTRACTUEL")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "M.G.P.A.P")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL CONTRACTUEL")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (S.M) ET (C.C.D) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "MUTUELLE SECTEUR MUTUALISTE")
            .addParam("Objet2", "MUTUELLE C.C.D")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MTMGPAC CONTRACTUEL").add("html", fileLink).map());
  }

  public void MtMgpapCexcel(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617112";
    String req =
        "select sum(es.mutuelle_mgpapsm) as mutuelle_mgpapsm, sum(es.mutuelle_mgpap_ccd) as mutuelle_mgpapsm_ccd from hr_etat_salaire es where es.mois=?1 and es.annee=?2  and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float sommeRCAR = ((BigDecimal) obj[0]).floatValue() + ((BigDecimal) obj[1]).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdredesPaiements, "MTMGPAC CONTRACTUEL")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "M.G.P.A.P")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "PERSONNEL CONTRACTUEL")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION AU MUTUELLE (S.M) ET (C.C.D) DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "MUTUELLE SECTEUR MUTUALISTE")
            .addParam("Objet2", "MUTUELLE C.C.D")
            .addParam("Val1", obj[0])
            .addParam("Val2", obj[1])
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MTMGPAC CONTRACTUEL").add("html", fileLink).map());
  }
  // *****
  // mtlogement
  public void MtLogementpdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617131";
    String req =
        "select sum(es.indemnite_logem_brut) as indemnite_logem_brut,sum(es.rcar_comp617131) as rcar_comp617131,sum(es.ir617131) as ir_comp617131 from hr_etat_salaire es where es.mois=?1 and es.annee=?2 ";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float ss = ((BigDecimal) obj[0]).floatValue();
    float sommeRCAR = ((BigDecimal) obj[1]).floatValue() + ((BigDecimal) obj[2]).floatValue();
    float somme = ss - sommeRCAR;
    double number2 = (Math.round(somme * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.Mt_Logement, "MT IND LOGEMENT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("Rubrique", "INDEMNITE DE LOGEMENT")
            .addParam("Total", ss)
            .addParam("Objet", "INDEMNITE DE LOGEMENT DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "RCAR COMPLEMENTAIRE")
            .addParam("Objet2", "IMPOT SUR INDEMNITE DE LOGEMENT")
            .addParam("Val1", obj[2])
            .addParam("Val2", obj[1])
            .addParam("Objet3", "Total Retenues")
            .addParam("Val3", sommeRCAR)
            .addParam("Net", somme)
            .addParam("Montant", montant)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND LOGEMENT").add("html", fileLink).map());
  }

  public void MtLogementword(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617131";
    String req =
        "select sum(es.indemnite_logem_brut) as indemnite_logem_brut,sum(es.rcar_comp617131) as rcar_comp617131,sum(es.ir617131) as ir_comp617131 from hr_etat_salaire es where es.mois=?1 and es.annee=?2 ";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float ss = ((BigDecimal) obj[0]).floatValue();
    float sommeRCAR = ((BigDecimal) obj[1]).floatValue() + ((BigDecimal) obj[2]).floatValue();
    float somme = ss - sommeRCAR;
    double number2 = (Math.round(somme * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.Mt_Logement, "MT IND LOGEMENT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("Rubrique", "INDEMNITE DE LOGEMENT")
            .addParam("Total", ss)
            .addParam("Objet", "INDEMNITE DE LOGEMENT DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "RCAR COMPLEMENTAIRE")
            .addParam("Objet2", "IMPOT SUR INDEMNITE DE LOGEMENT")
            .addParam("Val1", obj[2])
            .addParam("Val2", obj[1])
            .addParam("Objet3", "Total Retenues")
            .addParam("Val3", sommeRCAR)
            .addParam("Net", somme)
            .addParam("Montant", montant)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND LOGEMENT").add("html", fileLink).map());
  }

  public void MtLogementexcel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617131";
    String req =
        "select sum(es.indemnite_logem_brut) as indemnite_logem_brut,sum(es.rcar_comp617131) as rcar_comp617131,sum(es.ir617131) as ir_comp617131 from hr_etat_salaire es where es.mois=?1 and es.annee=?2 ";
    Object etat = excuteRequette(mois, annee, code, req);

    Object[] obj = (Object[]) etat;
    float ss = ((BigDecimal) obj[0]).floatValue();
    float sommeRCAR = ((BigDecimal) obj[1]).floatValue() + ((BigDecimal) obj[2]).floatValue();
    float somme = ss - sommeRCAR;
    double number2 = (Math.round(somme * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(com.axelor.apps.hr.report.IReport.Mt_Logement, "MT IND LOGEMENT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("Rubrique", "INDEMNITE DE LOGEMENT")
            .addParam("Total", ss)
            .addParam("Objet", "INDEMNITE DE LOGEMENT DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "RCAR COMPLEMENTAIRE")
            .addParam("Objet2", "IMPOT SUR INDEMNITE DE LOGEMENT")
            .addParam("Val1", obj[2])
            .addParam("Val2", obj[1])
            .addParam("Objet3", "Total Retenues")
            .addParam("Val3", sommeRCAR)
            .addParam("Net", somme)
            .addParam("Montant", montant)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND LOGEMENT").add("html", fileLink).map());
  }
  // ****
  // rcarinlog
  public void RcarInLogpdf(ActionRequest request, ActionResponse response) throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617131";
    String req =
        "select sum(es.rcar_comp617131) as Rcarrcomp from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    String req2 =
        "select sum(es.rcar_comp617131) as Rcar from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);
    Object etat2 = excuteRequette(mois, annee, code, req2);
    float sommeRCAR = ((BigDecimal) etat).floatValue() + ((BigDecimal) etat2).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdredesPaiements, "RCAR IN LOGEMENT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "R.C.A.R")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "INDEMNITES DE LOGEMENT")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION SALARIALE AU R.C.A.R DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "R.C.A.R")
            .addParam("Objet2", "CC.RCAR")
            .addParam("Val1", etat2)
            .addParam("Val2", etat)
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("RCAR IN LOGEMENT").add("html", fileLink).map());
  }

  public void RcarInLogword(ActionRequest request, ActionResponse response) throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617131";
    String req =
        "select sum(es.rcar_comp617131) as Rcarrcomp from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    String req2 =
        "select sum(es.rcar_comp617131) as Rcar from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);
    Object etat2 = excuteRequette(mois, annee, code, req2);
    float sommeRCAR = ((BigDecimal) etat).floatValue() + ((BigDecimal) etat2).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdredesPaiements, "RCAR IN LOGEMENT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "R.C.A.R")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "INDEMNITES DE LOGEMENT")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION SALARIALE AU R.C.A.R DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "R.C.A.R")
            .addParam("Objet2", "CC.RCAR")
            .addParam("Val1", etat2)
            .addParam("Val2", etat)
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("RCAR IN LOGEMENT").add("html", fileLink).map());
  }

  public void RcarInLogexcel(ActionRequest request, ActionResponse response)
      throws AxelorException {

    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617131";
    String req =
        "select sum(es.rcar_comp617131) as Rcarrcomp from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=true";
    String req2 =
        "select sum(es.rcar_comp617131) as Rcar from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and titulaire=false";
    Object etat = excuteRequette(mois, annee, code, req);
    Object etat2 = excuteRequette(mois, annee, code, req2);
    float sommeRCAR = ((BigDecimal) etat).floatValue() + ((BigDecimal) etat2).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdredesPaiements, "RCAR IN LOGEMENT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "R.C.A.R")
            .addParam("Code", code)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addParam("Rubrique", "INDEMNITES DE LOGEMENT")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "COTISATION SALARIALE AU R.C.A.R DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Objet1", "R.C.A.R")
            .addParam("Objet2", "CC.RCAR")
            .addParam("Val1", etat2)
            .addParam("Val2", etat)
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("RCAR IN LOGEMENT").add("html", fileLink).map());
  }
  //
  // irinlogpdf
  public void IrInLogpdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617131";
    String req =
        "select sum(es.ir617131) as irLog from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "IR IN LOGEMENT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "  LE TRESORIER PREFECTORAL DE FES")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITE DE LOGEMENT")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " IMPOT SUR INDEMNITE DE LOGEMENT DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("IR IN LOGEMENT").add("html", fileLink).map());
  }

  public void IrInLogword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617131";
    String req =
        "select sum(es.ir617131) as irLog from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "IR IN LOGEMENT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "  LE TRESORIER PREFECTORAL DE FES")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITE DE LOGEMENT")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " IMPOT SUR INDEMNITE DE LOGEMENT DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("IR IN LOGEMENT").add("html", fileLink).map());
  }

  public void IrInLogexcel(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617131";
    String req =
        "select sum(es.ir617131) as irLog from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "IR IN LOGEMENT")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "  LE TRESORIER PREFECTORAL DE FES")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITE DE LOGEMENT")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", " IMPOT SUR INDEMNITE DE LOGEMENT DU MOIS ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("IR IN LOGEMENT").add("html", fileLink).map());
  }
  //
  // mtindfonction
  public void MtIndFonctionpdf(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617133";
    String req =
        "select sum(es.indemnit_fonction_net) as indemnit_fonction_net from hr_etat_salaire es where es.mois=?1 and es.annee=?2 ";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT IND FONCTION")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITES DE FONCTION")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "INDEMNITE DE FONCTION NETTE / ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND FONCTION").add("html", fileLink).map());
  }

  public void MtIndFonctionword(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617133";
    String req =
        "select sum(es.indemnit_fonction_net) as indemnit_fonction_net from hr_etat_salaire es where es.mois=?1 and es.annee=?2 ";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT IND FONCTION")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITES DE FONCTION")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "INDEMNITE DE FONCTION NETTE / ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND FONCTION").add("html", fileLink).map());
  }

  public void MtIndFonctionexcel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617133";
    String req =
        "select sum(es.indemnit_fonction_net) as indemnit_fonction_net from hr_etat_salaire es where es.mois=?1 and es.annee=?2 ";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT IND FONCTION")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITES DE FONCTION")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "INDEMNITE DE FONCTION NETTE / ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND FONCTION").add("html", fileLink).map());
  }
  //
  // mtindvoid
  public void MtIndVoitpdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617135";
    String req =
        "select sum(es.indemnit_voiture_net) as indemnit_voiture_net from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT IND VOITURE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITES DE VOITURE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "INDEMNITE DE VOITURE NETTE / ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND VOITURE").add("html", fileLink).map());
  }

  public void MtIndVoitword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617135";
    String req =
        "select sum(es.indemnit_voiture_net) as indemnit_voiture_net from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT IND VOITURE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITES DE VOITURE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "INDEMNITE DE VOITURE NETTE / ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND VOITURE").add("html", fileLink).map());
  }

  public void MtIndVoitexcel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617135";
    String req =
        "select sum(es.indemnit_voiture_net) as indemnit_voiture_net from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT IND VOITURE")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITES DE VOITURE")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "INDEMNITE DE VOITURE NETTE / ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND VOITURE").add("html", fileLink).map());
  }
  //
  // mtindrepresentation
  public void MtIndRepresentationpdf(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617136";
    String req =
        "select sum(es.indemnit_represent_net) as indemnit_represent_net from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT IND REPRESENTATION")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITES DE REPRESENTATION")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "INDEMNITE DE REPRESENTATION NETTE/ ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND REPRESENTATION").add("html", fileLink).map());
  }

  public void MtIndRepresentationword(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617136";
    String req =
        "select sum(es.indemnit_represent_net) as indemnit_represent_net from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT IND REPRESENTATION")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITES DE REPRESENTATION")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "INDEMNITE DE REPRESENTATION NETTE/ ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND REPRESENTATION").add("html", fileLink).map());
  }

  public void MtIndRepresentationexcel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    String code = "617136";
    String req =
        "select sum(es.indemnit_represent_net) as indemnit_represent_net from hr_etat_salaire es where es.mois=?1 and es.annee=?2";
    Object etat = excuteRequette(mois, annee, code, req);

    float sommeRCAR = ((BigDecimal) etat).floatValue();
    double number2 = (Math.round(sommeRCAR * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));

    String fileLink =
        ReportFactory.createReport(IReport.OrdredesPaiementIR, "MT IND REPRESENTATION")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("Benificaire", "DIVERS AGENT SUR L'ACQUIT DU TRESORIER PAYEUR DE LA CCIS")
            .addParam("Code", code)
            .addParam("Rubrique", "INDEMNITES DE REPRESENTATION")
            .addParam("Total", sommeRCAR)
            .addParam("Objet", "INDEMNITE DE REPRESENTATION NETTE/ ")
            .addParam(
                "pieces", "ETAT DES SALAIRES, INDEMNITES ET PRELEVEMENTS DU PERSONNEL TITULAIRE")
            .addParam("Montant", montant)
            .addParam("annee", annee)
            .addParam("moisString", tableau_mois_francais[mois - 1])
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("MT IND REPRESENTATION").add("html", fileLink).map());
  }
  //
  // editordredevirement
  public void editOrdreDeVirementpdf(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    BigDecimal mt1 = appservice.getMontantOrdreVirementAgent(0, mois, annee);
    BigDecimal mt2 = appservice.getMontantOrdreVirementAgent(1, mois, annee);

    String montant_page1 = ConvertNomreToLettres.getStringMontant(mt1);
    String montant_page2 = ConvertNomreToLettres.getStringMontant(mt2);

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdreDeVirement, "OrdreDeVirement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("montant_page1", montant_page1)
            .addParam("montant_page2", montant_page2)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement").add("html", fileLink).map());
  }

  public void editOrdreDeVirementword(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    BigDecimal mt1 = appservice.getMontantOrdreVirementAgent(0, mois, annee);
    BigDecimal mt2 = appservice.getMontantOrdreVirementAgent(1, mois, annee);

    String montant_page1 = ConvertNomreToLettres.getStringMontant(mt1);
    String montant_page2 = ConvertNomreToLettres.getStringMontant(mt2);

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdreDeVirement, "OrdreDeVirement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("montant_page1", montant_page1)
            .addParam("montant_page2", montant_page2)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement").add("html", fileLink).map());
  }

  public void editOrdreDeVirementexcel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    BigDecimal mt1 = appservice.getMontantOrdreVirementAgent(0, mois, annee);
    BigDecimal mt2 = appservice.getMontantOrdreVirementAgent(1, mois, annee);

    String montant_page1 = ConvertNomreToLettres.getStringMontant(mt1);
    String montant_page2 = ConvertNomreToLettres.getStringMontant(mt2);

    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OrdreDeVirement, "OrdreDeVirement")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("mois", mois)
            .addParam("annee", annee)
            .addParam("montant_page1", montant_page1)
            .addParam("montant_page2", montant_page2)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement").add("html", fileLink).map());
  }
  //
  // ovrcar
  public void OVRCARpdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(x.rcar_rg)+sum(x.rcarrcomp)+sum(x.rcar_comp617131) from hr_etat_salaire x where mois=?1 and annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat = query.getSingleResult();
    BigDecimal sommeRCAR = etat != null ? (BigDecimal) etat : BigDecimal.ZERO;

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(x.rcar_rg)*2+sum(x.rcarrcomp)+sum(x.rcar_comp617131) from hr_etat_salaire x where mois=?1 and annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat2 = query2.getSingleResult();
    BigDecimal sommeRCAR2 = etat != null ? (BigDecimal) etat2 : BigDecimal.ZERO;

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) as rcar from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat3 = query3.getSingleResult();
    BigDecimal sommeRecore3 = etat != null ? (BigDecimal) etat3 : BigDecimal.ZERO;
    float somme =
        (sommeRCAR.add(sommeRCAR2).add(sommeRecore3).setScale(2, RoundingMode.HALF_UP))
            .floatValue();
    double number2 = (Math.round(somme * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));
    String rib = appConfService.getRibRcar(2021);
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OVRCAR, "Ordre de virement RCAR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("RCAR", "RCAR")
            .addParam("RCAR2", "RCAR")
            .addParam("compteRCAR", rib)
            .addParam("compteRCAR2", rib)
            .addParam("RECORE", "CDG - CNRA (RECORE)")
            .addParam("compteRECORE", "CDG  060810114010349000010174")
            .addParam("Somme1", sommeRCAR)
            .addParam("Somme2", sommeRCAR2)
            .addParam("Somme3", sommeRecore3)
            .addParam("Total", sommeRCAR.add(sommeRCAR2))
            .addParam(
                "TotalGeneral",
                sommeRCAR.add(sommeRCAR2).add(sommeRecore3).setScale(2, RoundingMode.HALF_UP))
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement RCAR").add("html", fileLink).map());
  }

  public void OVRCARword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(x.rcar_rg)+sum(x.rcarrcomp)+sum(x.rcar_comp617131) from hr_etat_salaire x where mois=?1 and annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat = query.getSingleResult();
    BigDecimal sommeRCAR = etat != null ? (BigDecimal) etat : BigDecimal.ZERO;

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(x.rcar_rg)*2+sum(x.rcarrcomp)+sum(x.rcar_comp617131) from hr_etat_salaire x where mois=?1 and annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat2 = query2.getSingleResult();
    BigDecimal sommeRCAR2 = etat != null ? (BigDecimal) etat2 : BigDecimal.ZERO;

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) as rcar from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat3 = query3.getSingleResult();
    BigDecimal sommeRecore3 = etat != null ? (BigDecimal) etat3 : BigDecimal.ZERO;
    float somme =
        (sommeRCAR.add(sommeRCAR2).add(sommeRecore3).setScale(2, RoundingMode.HALF_UP))
            .floatValue();
    double number2 = (Math.round(somme * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));
    String rib = appConfService.getRibRcar(2021);
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OVRCAR, "Ordre de virement RCAR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("RCAR", "RCAR")
            .addParam("RCAR2", "RCAR")
            .addParam("compteRCAR", rib)
            .addParam("compteRCAR2", rib)
            .addParam("RECORE", "CDG - CNRA (RECORE)")
            .addParam("compteRECORE", "CDG  060810114010349000010174")
            .addParam("Somme1", sommeRCAR)
            .addParam("Somme2", sommeRCAR2)
            .addParam("Somme3", sommeRecore3)
            .addParam("Total", sommeRCAR.add(sommeRCAR2))
            .addParam(
                "TotalGeneral",
                sommeRCAR.add(sommeRCAR2).add(sommeRecore3).setScale(2, RoundingMode.HALF_UP))
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement RCAR").add("html", fileLink).map());
  }

  public void OVRCARexcel(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(x.rcar_rg)+sum(x.rcarrcomp)+sum(x.rcar_comp617131) from hr_etat_salaire x where mois=?1 and annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat = query.getSingleResult();
    BigDecimal sommeRCAR = etat != null ? (BigDecimal) etat : BigDecimal.ZERO;

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(x.rcar_rg)*2+sum(x.rcarrcomp)+sum(x.rcar_comp617131) from hr_etat_salaire x where mois=?1 and annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat2 = query2.getSingleResult();
    BigDecimal sommeRCAR2 = etat != null ? (BigDecimal) etat2 : BigDecimal.ZERO;

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.recore) as rcar from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat3 = query3.getSingleResult();
    BigDecimal sommeRecore3 = etat != null ? (BigDecimal) etat3 : BigDecimal.ZERO;
    float somme =
        (sommeRCAR.add(sommeRCAR2).add(sommeRecore3).setScale(2, RoundingMode.HALF_UP))
            .floatValue();
    double number2 = (Math.round(somme * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));
    String rib = appConfService.getRibRcar(2021);
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OVRCAR, "Ordre de virement RCAR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("RCAR", "RCAR")
            .addParam("RCAR2", "RCAR")
            .addParam("compteRCAR", rib)
            .addParam("compteRCAR2", rib)
            .addParam("RECORE", "CDG - CNRA (RECORE)")
            .addParam("compteRECORE", "CDG  060810114010349000010174")
            .addParam("Somme1", sommeRCAR)
            .addParam("Somme2", sommeRCAR2)
            .addParam("Somme3", sommeRecore3)
            .addParam("Total", sommeRCAR.add(sommeRCAR2))
            .addParam(
                "TotalGeneral",
                sommeRCAR.add(sommeRCAR2).add(sommeRecore3).setScale(2, RoundingMode.HALF_UP))
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement RCAR").add("html", fileLink).map());
  }
  //
  // ovcnops
  public void OVCNOPSpdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) as axa from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal sommeAMO = BigDecimal.ZERO;
    String ribAmo = employeeAdvanceService.getRibMutuelle(1, 2021);
    if (etat != null) {
      sommeAMO = (BigDecimal) etat;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpapsm) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal sommeMGPAP = BigDecimal.ZERO;
    String ribMGPAP = employeeAdvanceService.getRibMutuelle(2, 2021);
    if (etat2 != null) {
      sommeMGPAP = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpap_ccd) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal sommeMGPAPCCD = BigDecimal.ZERO;
    String ribMGPAPCCD = employeeAdvanceService.getRibMutuelle(3, 2021);
    if (etat3 != null) {
      sommeMGPAPCCD = (BigDecimal) etat3;
    }

    javax.persistence.Query query4 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_sm) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat4 = query4.getSingleResult();
    BigDecimal sommeOMFAM = BigDecimal.ZERO;
    String ribOMFAM = employeeAdvanceService.getRibMutuelle(4, 2021);
    if (etat4 != null) {
      sommeOMFAM = (BigDecimal) etat4;
    }

    javax.persistence.Query query5 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_caad) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat5 = query5.getSingleResult();
    BigDecimal sommeOMFAMCCD = BigDecimal.ZERO;
    String ribOMFAMCCD = employeeAdvanceService.getRibMutuelle(5, 2021);
    if (etat5 != null) {
      sommeOMFAMCCD = (BigDecimal) etat5;
    }
    float somme =
        (sommeAMO
                .add(sommeMGPAP)
                .add(sommeMGPAPCCD)
                .add(sommeOMFAM)
                .add(sommeOMFAMCCD)
                .add(sommeAMO))
            .floatValue();
    double number2 = (Math.round(somme * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OVCNOPS, "Ordre de virement CNOPS")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("nom", "C.N.O.P.S")
            .addParam("nom2", "M.G.P.A.P")
            .addParam("nom3", "M.G.P.A.P")
            .addParam("nom4", "OMFAM - SM")
            .addParam("nom5", "OMFAM - CAAD")
            .addParam("compte", ribAmo)
            .addParam("compte2", ribMGPAP)
            .addParam("compte3", ribMGPAPCCD)
            .addParam("compte4", ribOMFAM)
            .addParam("compte5", ribOMFAMCCD)
            .addParam("Somme1", sommeAMO)
            .addParam("Somme2", sommeMGPAP)
            .addParam("Somme3", sommeMGPAPCCD)
            .addParam("Somme4", sommeOMFAM)
            .addParam("Somme5", sommeOMFAMCCD)
            .addParam(
                "Total",
                sommeAMO.add(sommeMGPAP).add(sommeMGPAPCCD).add(sommeOMFAM).add(sommeOMFAMCCD))
            .addParam(
                "TotalGeneral",
                sommeAMO
                    .add(sommeMGPAP)
                    .add(sommeMGPAPCCD)
                    .add(sommeOMFAM)
                    .add(sommeOMFAMCCD)
                    .add(sommeAMO))
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement CNOPS").add("html", fileLink).map());
  }

  public void OVCNOPSword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) as axa from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal sommeAMO = BigDecimal.ZERO;
    String ribAmo = employeeAdvanceService.getRibMutuelle(1, 2021);
    if (etat != null) {
      sommeAMO = (BigDecimal) etat;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpapsm) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal sommeMGPAP = BigDecimal.ZERO;
    String ribMGPAP = employeeAdvanceService.getRibMutuelle(2, 2021);
    if (etat2 != null) {
      sommeMGPAP = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpap_ccd) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal sommeMGPAPCCD = BigDecimal.ZERO;
    String ribMGPAPCCD = employeeAdvanceService.getRibMutuelle(3, 2021);
    if (etat3 != null) {
      sommeMGPAPCCD = (BigDecimal) etat3;
    }

    javax.persistence.Query query4 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_sm) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat4 = query4.getSingleResult();
    BigDecimal sommeOMFAM = BigDecimal.ZERO;
    String ribOMFAM = employeeAdvanceService.getRibMutuelle(4, 2021);
    if (etat4 != null) {
      sommeOMFAM = (BigDecimal) etat4;
    }

    javax.persistence.Query query5 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_caad) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat5 = query5.getSingleResult();
    BigDecimal sommeOMFAMCCD = BigDecimal.ZERO;
    String ribOMFAMCCD = employeeAdvanceService.getRibMutuelle(5, 2021);
    if (etat5 != null) {
      sommeOMFAMCCD = (BigDecimal) etat5;
    }
    float somme =
        (sommeAMO
                .add(sommeMGPAP)
                .add(sommeMGPAPCCD)
                .add(sommeOMFAM)
                .add(sommeOMFAMCCD)
                .add(sommeAMO))
            .floatValue();
    double number2 = (Math.round(somme * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OVCNOPS, "Ordre de virement CNOPS")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("nom", "C.N.O.P.S")
            .addParam("nom2", "M.G.P.A.P")
            .addParam("nom3", "M.G.P.A.P")
            .addParam("nom4", "OMFAM - SM")
            .addParam("nom5", "OMFAM - CAAD")
            .addParam("compte", ribAmo)
            .addParam("compte2", ribMGPAP)
            .addParam("compte3", ribMGPAPCCD)
            .addParam("compte4", ribOMFAM)
            .addParam("compte5", ribOMFAMCCD)
            .addParam("Somme1", sommeAMO)
            .addParam("Somme2", sommeMGPAP)
            .addParam("Somme3", sommeMGPAPCCD)
            .addParam("Somme4", sommeOMFAM)
            .addParam("Somme5", sommeOMFAMCCD)
            .addParam(
                "Total",
                sommeAMO.add(sommeMGPAP).add(sommeMGPAPCCD).add(sommeOMFAM).add(sommeOMFAMCCD))
            .addParam(
                "TotalGeneral",
                sommeAMO
                    .add(sommeMGPAP)
                    .add(sommeMGPAPCCD)
                    .add(sommeOMFAM)
                    .add(sommeOMFAMCCD)
                    .add(sommeAMO))
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement CNOPS").add("html", fileLink).map());
  }

  public void OVCNOPSexcel(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.amo) as axa from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat = query.getSingleResult();
    BigDecimal sommeAMO = BigDecimal.ZERO;
    String ribAmo = employeeAdvanceService.getRibMutuelle(1, 2021);
    if (etat != null) {
      sommeAMO = (BigDecimal) etat;
    }

    javax.persistence.Query query2 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpapsm) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat2 = query2.getSingleResult();
    BigDecimal sommeMGPAP = BigDecimal.ZERO;
    String ribMGPAP = employeeAdvanceService.getRibMutuelle(2, 2021);
    if (etat2 != null) {
      sommeMGPAP = (BigDecimal) etat2;
    }

    javax.persistence.Query query3 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_mgpap_ccd) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat3 = query3.getSingleResult();
    BigDecimal sommeMGPAPCCD = BigDecimal.ZERO;
    String ribMGPAPCCD = employeeAdvanceService.getRibMutuelle(3, 2021);
    if (etat3 != null) {
      sommeMGPAPCCD = (BigDecimal) etat3;
    }

    javax.persistence.Query query4 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_sm) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat4 = query4.getSingleResult();
    BigDecimal sommeOMFAM = BigDecimal.ZERO;
    String ribOMFAM = employeeAdvanceService.getRibMutuelle(4, 2021);
    if (etat4 != null) {
      sommeOMFAM = (BigDecimal) etat4;
    }

    javax.persistence.Query query5 =
        JPA.em()
            .createNativeQuery(
                "select sum(es.mutuelle_omfam_caad) as mgpap from hr_etat_salaire es where es.mois=?1 and es.annee=?2 and es.is_disposition=false")
            .setParameter(1, mois)
            .setParameter(2, annee);

    Object etat5 = query5.getSingleResult();
    BigDecimal sommeOMFAMCCD = BigDecimal.ZERO;
    String ribOMFAMCCD = employeeAdvanceService.getRibMutuelle(5, 2021);
    if (etat5 != null) {
      sommeOMFAMCCD = (BigDecimal) etat5;
    }
    float somme =
        (sommeAMO
                .add(sommeMGPAP)
                .add(sommeMGPAPCCD)
                .add(sommeOMFAM)
                .add(sommeOMFAMCCD)
                .add(sommeAMO))
            .floatValue();
    double number2 = (Math.round(somme * 100)) / 100.0;
    String montant = ConvertNomreToLettres.getStringMontant(BigDecimal.valueOf(number2));
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OVCNOPS, "Ordre de virement CNOPS")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("nom", "C.N.O.P.S")
            .addParam("nom2", "M.G.P.A.P")
            .addParam("nom3", "M.G.P.A.P")
            .addParam("nom4", "OMFAM - SM")
            .addParam("nom5", "OMFAM - CAAD")
            .addParam("compte", ribAmo)
            .addParam("compte2", ribMGPAP)
            .addParam("compte3", ribMGPAPCCD)
            .addParam("compte4", ribOMFAM)
            .addParam("compte5", ribOMFAMCCD)
            .addParam("Somme1", sommeAMO)
            .addParam("Somme2", sommeMGPAP)
            .addParam("Somme3", sommeMGPAPCCD)
            .addParam("Somme4", sommeOMFAM)
            .addParam("Somme5", sommeOMFAMCCD)
            .addParam(
                "Total",
                sommeAMO.add(sommeMGPAP).add(sommeMGPAPCCD).add(sommeOMFAM).add(sommeOMFAMCCD))
            .addParam(
                "TotalGeneral",
                sommeAMO
                    .add(sommeMGPAP)
                    .add(sommeMGPAPCCD)
                    .add(sommeOMFAM)
                    .add(sommeOMFAMCCD)
                    .add(sommeAMO))
            .addParam("Montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement CNOPS").add("html", fileLink).map());
  }
  //
  // ovir
  public void OVIRpdf(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }

    BigDecimal sommeIR =
        RunSqlRequestForMe.runSqlRequest_Bigdecimal(
            "select sum(es.ir)+ sum(ir617131) as axa from hr_etat_salaire es where es.mois= "
                + mois
                + " and es.annee="
                + annee
                + " ;");
    String montant = ConvertNomreToLettres.getStringMontant(sommeIR);
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OVothers, "Ordre de virement IR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("other", "TRESORIER PREFECTORAL DE FES")
            .addParam("compteother", "TRESORIER PREFECTORAL DE FES")
            .addParam("Somme1", sommeIR.setScale(2, RoundingMode.HALF_UP))
            .addParam("TotalGeneral", sommeIR.setScale(2, RoundingMode.HALF_UP))
            .addParam("montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement IR").add("html", fileLink).map());
  }

  public void OVIRword(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }

    BigDecimal sommeIR =
        RunSqlRequestForMe.runSqlRequest_Bigdecimal(
            "select sum(es.ir)+ sum(ir617131) as axa from hr_etat_salaire es where es.mois= "
                + mois
                + " and es.annee="
                + annee
                + " ;");
    String montant = ConvertNomreToLettres.getStringMontant(sommeIR);
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OVothers, "Ordre de virement IR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("other", "TRESORIER PREFECTORAL DE FES")
            .addParam("compteother", "TRESORIER PREFECTORAL DE FES")
            .addParam("Somme1", sommeIR.setScale(2, RoundingMode.HALF_UP))
            .addParam("TotalGeneral", sommeIR.setScale(2, RoundingMode.HALF_UP))
            .addParam("montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement IR").add("html", fileLink).map());
  }

  public void OVIRexcel(ActionRequest request, ActionResponse response) throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }

    BigDecimal sommeIR =
        RunSqlRequestForMe.runSqlRequest_Bigdecimal(
            "select sum(es.ir)+ sum(ir617131) as axa from hr_etat_salaire es where es.mois= "
                + mois
                + " and es.annee="
                + annee
                + " ;");
    String montant = ConvertNomreToLettres.getStringMontant(sommeIR);
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.hr.report.IReport.OVothers, "Ordre de virement IR")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("other", "TRESORIER PREFECTORAL DE FES")
            .addParam("compteother", "TRESORIER PREFECTORAL DE FES")
            .addParam("Somme1", sommeIR.setScale(2, RoundingMode.HALF_UP))
            .addParam("TotalGeneral", sommeIR.setScale(2, RoundingMode.HALF_UP))
            .addParam("montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement IR").add("html", fileLink).map());
  }
  //
  // ovaaxa
  public void OVAxaCreditpdf(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "pdf";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.pret_axa_credit) as axa from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat = query.getSingleResult();
    BigDecimal sommeAxa = BigDecimal.ZERO;
    IntitulerCredit i = intitulerCreditRepository.all().filter("self.name like '%AXA%'").fetchOne();
    String rib = "";
    if (i != null) {
      rib = i.getNameBank() + " N° " + i.getRib();
    }
    if (etat != null) {
      sommeAxa = (BigDecimal) etat;
    }
    String montant = ConvertNomreToLettres.getStringMontant(sommeAxa);
    String fileLink =
        ReportFactory.createReport(IReport.OVothers, "Ordre de virement AXA Crédit")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("other", "AXA CREDIT")
            .addParam("compteother", rib)
            .addParam("Somme1", sommeAxa)
            .addParam("TotalGeneral", sommeAxa)
            .addParam("montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement AXA Crédit").add("html", fileLink).map());
  }

  public void OVAxaCreditword(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "doc";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.pret_axa_credit) as axa from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat = query.getSingleResult();
    BigDecimal sommeAxa = BigDecimal.ZERO;
    IntitulerCredit i = intitulerCreditRepository.all().filter("self.name like '%AXA%'").fetchOne();
    String rib = "";
    if (i != null) {
      rib = i.getNameBank() + " N° " + i.getRib();
    }
    if (etat != null) {
      sommeAxa = (BigDecimal) etat;
    }
    String montant = ConvertNomreToLettres.getStringMontant(sommeAxa);
    String fileLink =
        ReportFactory.createReport(IReport.OVothers, "Ordre de virement AXA Crédit")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("other", "AXA CREDIT")
            .addParam("compteother", rib)
            .addParam("Somme1", sommeAxa)
            .addParam("TotalGeneral", sommeAxa)
            .addParam("montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement AXA Crédit").add("html", fileLink).map());
  }

  public void OVAxaCreditexcel(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer mois = (Integer) request.getContext().get("mois");
    Integer annee = (Integer) request.getContext().get("annee");
    String type = "xls";
    if (type.isEmpty()) {
      response.setFlash("Le type du document est obligatoire");
      return;
    }
    javax.persistence.Query query =
        JPA.em()
            .createNativeQuery(
                "select sum(es.pret_axa_credit) as axa from hr_etat_salaire es where es.mois=?1 and es.annee=?2")
            .setParameter(1, mois)
            .setParameter(2, annee);
    Object etat = query.getSingleResult();
    BigDecimal sommeAxa = BigDecimal.ZERO;
    IntitulerCredit i = intitulerCreditRepository.all().filter("self.name like '%AXA%'").fetchOne();
    String rib = "";
    if (i != null) {
      rib = i.getNameBank() + " N° " + i.getRib();
    }
    if (etat != null) {
      sommeAxa = (BigDecimal) etat;
    }
    String montant = ConvertNomreToLettres.getStringMontant(sommeAxa);
    String fileLink =
        ReportFactory.createReport(IReport.OVothers, "Ordre de virement AXA Crédit")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("other", "AXA CREDIT")
            .addParam("compteother", rib)
            .addParam("Somme1", sommeAxa)
            .addParam("TotalGeneral", sommeAxa)
            .addParam("montant", montant)
            .addFormat(type)
            .generate()
            .getFileLink();
    response.setView(ActionView.define("Ordre de virement AXA Crédit").add("html", fileLink).map());
  }
  //
}
