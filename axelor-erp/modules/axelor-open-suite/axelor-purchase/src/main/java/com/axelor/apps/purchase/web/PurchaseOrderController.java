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
package com.axelor.apps.purchase.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.account.db.*;
import com.axelor.apps.account.db.repo.DepencesGeneraleRepository;
import com.axelor.apps.account.db.repo.RecetteGeneraleGrandRepository;
import com.axelor.apps.base.db.BankDetails;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.PriceList;
import com.axelor.apps.base.db.PrintingSettings;
import com.axelor.apps.base.db.TradingName;
import com.axelor.apps.base.db.Wizard;
import com.axelor.apps.base.db.repo.BlockingRepository;
import com.axelor.apps.base.db.repo.PartnerRepository;
import com.axelor.apps.base.db.repo.PriceListRepository;
import com.axelor.apps.base.service.BankDetailsService;
import com.axelor.apps.base.service.BlockingService;
import com.axelor.apps.base.service.PartnerPriceListService;
import com.axelor.apps.base.service.TradingNameService;
import com.axelor.apps.configuration.db.*;
import com.axelor.apps.configuration.db.repo.RubriqueBudgetaireGeneraleRepository;
import com.axelor.apps.configuration.db.repo.RubriquesBudgetaireRepository;
import com.axelor.apps.configuration.db.repo.VersionRubriqueBudgetaireRepository;
import com.axelor.apps.configuration.service.ConvertNomreToLettres;
import com.axelor.apps.configuration.service.RunSqlRequestForMe;
import com.axelor.apps.purchase.db.*;
import com.axelor.apps.purchase.db.repo.*;
import com.axelor.apps.purchase.exception.IExceptionMessage;
import com.axelor.apps.purchase.report.IReport;
import com.axelor.apps.purchase.service.PurchaseOrderService;
import com.axelor.apps.purchase.service.PurchaseOrderWorkflowService;
import com.axelor.apps.purchase.service.app.AppPurchaseService;
import com.axelor.apps.purchase.service.print.PurchaseOrderPrintService;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.apps.tool.StringTool;
import com.axelor.common.ObjectUtils;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PurchaseOrderController {

  private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void setSequence(ActionRequest request, ActionResponse response) {
    try {
      PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);

      if (purchaseOrder != null && purchaseOrder.getCompany() != null) {

        response.setValue(
            "purchaseOrderSeq",
            Beans.get(PurchaseOrderService.class).getSequence(purchaseOrder.getCompany()));
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void compute(ActionRequest request, ActionResponse response) {

    PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);

    if (purchaseOrder != null) {
      try {
        purchaseOrder = Beans.get(PurchaseOrderService.class).computePurchaseOrder(purchaseOrder);
        response.setValues(purchaseOrder);
      } catch (Exception e) {
        TraceBackService.trace(response, e);
      }
    }
  }

  public void validateSupplier(ActionRequest request, ActionResponse response) {

    PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);

    response.setValue(
        "supplierPartner", Beans.get(PurchaseOrderService.class).validateSupplier(purchaseOrder));
  }

  /**
   * Called from grid or form purchase order view, print selected purchase order.
   *
   * @param request
   * @param response
   * @return
   */
  @SuppressWarnings("unchecked")
  public void showPurchaseOrder(ActionRequest request, ActionResponse response) {

    Context context = request.getContext();
    String fileLink;
    String title;
    PurchaseOrderPrintService purchaseOrderPrintService =
        Beans.get(PurchaseOrderPrintService.class);

    try {
      if (!ObjectUtils.isEmpty(request.getContext().get("_ids"))) {
        List<Long> ids =
            Lists.transform(
                (List) request.getContext().get("_ids"),
                new Function<Object, Long>() {
                  @Nullable
                  @Override
                  public Long apply(@Nullable Object input) {
                    return Long.parseLong(input.toString());
                  }
                });
        fileLink = purchaseOrderPrintService.printPurchaseOrders(ids);
        title = I18n.get("Purchase orders");
      } else if (context.get("id") != null) {
        PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);
        title = purchaseOrderPrintService.getFileName(purchaseOrder);
        fileLink =
            purchaseOrderPrintService.printPurchaseOrder(purchaseOrder, ReportSettings.FORMAT_PDF);
        logger.debug("Printing " + title);
      } else {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_MISSING_FIELD,
            I18n.get(IExceptionMessage.NO_PURCHASE_ORDER_SELECTED_FOR_PRINTING));
      }
      response.setView(ActionView.define(title).add("html", fileLink).map());
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void requestPurchaseOrder(ActionRequest request, ActionResponse response) {

    PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);

    try {
      Beans.get(PurchaseOrderService.class)
          .requestPurchaseOrder(
              Beans.get(PurchaseOrderRepository.class).find(purchaseOrder.getId()));
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void updateCostPrice(ActionRequest request, ActionResponse response) {
    try {
      PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);
      Beans.get(PurchaseOrderService.class)
          .updateCostPrice(Beans.get(PurchaseOrderRepository.class).find(purchaseOrder.getId()));
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  // Generate single purchase order from several
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void mergePurchaseOrder(ActionRequest request, ActionResponse response) {
    List<PurchaseOrder> purchaseOrderList = new ArrayList<PurchaseOrder>();
    List<Long> purchaseOrderIdList = new ArrayList<Long>();
    boolean fromPopup = false;

    if (request.getContext().get("purchaseOrderToMerge") != null) {

      if (request.getContext().get("purchaseOrderToMerge") instanceof List) {
        // No confirmation popup, purchase orders are content in a parameter list
        List<Map> purchaseOrderMap = (List<Map>) request.getContext().get("purchaseOrderToMerge");
        for (Map map : purchaseOrderMap) {
          purchaseOrderIdList.add(new Long((Integer) map.get("id")));
        }
      } else {
        // After confirmation popup, purchase order's id are in a string separated by ","
        String purchaseOrderIdListStr = (String) request.getContext().get("purchaseOrderToMerge");
        for (String purchaseOrderId : purchaseOrderIdListStr.split(",")) {
          purchaseOrderIdList.add(new Long(purchaseOrderId));
        }
        fromPopup = true;
      }
    }

    // Check if currency, supplierPartner, company and tradingName are the same for all selected
    // purchase orders
    Currency commonCurrency = null;
    Partner commonSupplierPartner = null;
    Company commonCompany = null;
    Partner commonContactPartner = null;
    TradingName commonTradingName = null;
    // Useful to determine if a difference exists between contact partners of all purchase orders
    boolean existContactPartnerDiff = false;
    PriceList commonPriceList = null;
    // Useful to determine if a difference exists between price lists of all purchase orders
    boolean existPriceListDiff = false;
    PurchaseOrder purchaseOrderTemp;
    boolean allTradingNamesAreNull = true;
    int count = 1;
    for (Long purchaseOrderId : purchaseOrderIdList) {
      purchaseOrderTemp = JPA.em().find(PurchaseOrder.class, purchaseOrderId);
      purchaseOrderList.add(purchaseOrderTemp);
      if (count == 1) {
        commonCurrency = purchaseOrderTemp.getCurrency();
        commonSupplierPartner = purchaseOrderTemp.getSupplierPartner();
        commonCompany = purchaseOrderTemp.getCompany();
        commonContactPartner = purchaseOrderTemp.getContactPartner();
        commonPriceList = purchaseOrderTemp.getPriceList();
        commonTradingName = purchaseOrderTemp.getTradingName();
        allTradingNamesAreNull = commonTradingName == null;

      } else {
        if (commonCurrency != null && !commonCurrency.equals(purchaseOrderTemp.getCurrency())) {
          commonCurrency = null;
        }
        if (commonSupplierPartner != null
            && !commonSupplierPartner.equals(purchaseOrderTemp.getSupplierPartner())) {
          commonSupplierPartner = null;
        }
        if (commonCompany != null && !commonCompany.equals(purchaseOrderTemp.getCompany())) {
          commonCompany = null;
        }
        if (commonContactPartner != null
            && !commonContactPartner.equals(purchaseOrderTemp.getContactPartner())) {
          commonContactPartner = null;
          existContactPartnerDiff = true;
        }
        if (commonPriceList != null && !commonPriceList.equals(purchaseOrderTemp.getPriceList())) {
          commonPriceList = null;
          existPriceListDiff = true;
        }
        if (!Objects.equals(commonTradingName, purchaseOrderTemp.getTradingName())) {
          commonTradingName = null;
          allTradingNamesAreNull = false;
        }
      }
      count++;
    }

    StringBuilder fieldErrors = new StringBuilder();
    if (commonCurrency == null) {
      fieldErrors.append(I18n.get(IExceptionMessage.PURCHASE_ORDER_MERGE_ERROR_CURRENCY));
    }
    if (commonSupplierPartner == null) {
      if (fieldErrors.length() > 0) {
        fieldErrors.append("<br/>");
      }
      fieldErrors.append(I18n.get(IExceptionMessage.PURCHASE_ORDER_MERGE_ERROR_SUPPLIER_PARTNER));
    }
    if (commonCompany == null) {
      if (fieldErrors.length() > 0) {
        fieldErrors.append("<br/>");
      }
      fieldErrors.append(I18n.get(IExceptionMessage.PURCHASE_ORDER_MERGE_ERROR_COMPANY));
    }
    if (commonTradingName == null && !allTradingNamesAreNull) {
      fieldErrors.append(I18n.get(IExceptionMessage.PURCHASE_ORDER_MERGE_ERROR_TRADING_NAME));
    }

    if (fieldErrors.length() > 0) {
      response.setFlash(fieldErrors.toString());
      return;
    }

    // Check if priceList or contactPartner are content in parameters
    if (request.getContext().get("priceList") != null) {
      commonPriceList =
          JPA.em()
              .find(
                  PriceList.class,
                  new Long((Integer) ((Map) request.getContext().get("priceList")).get("id")));
    }
    if (request.getContext().get("contactPartner") != null) {
      commonContactPartner =
          JPA.em()
              .find(
                  Partner.class,
                  new Long((Integer) ((Map) request.getContext().get("contactPartner")).get("id")));
    }

    if (!fromPopup && (existContactPartnerDiff || existPriceListDiff)) {
      // Need to display intermediate screen to select some values
      ActionViewBuilder confirmView =
          ActionView.define("Confirm merge purchase order")
              .model(Wizard.class.getName())
              .add("form", "purchase-order-merge-confirm-form")
              .param("popup", "true")
              .param("show-toolbar", "false")
              .param("show-confirm", "false")
              .param("popup-save", "false")
              .param("forceEdit", "true");

      if (existPriceListDiff) {
        confirmView.context("contextPriceListToCheck", "true");
      }
      if (existContactPartnerDiff) {
        confirmView.context("contextContactPartnerToCheck", "true");
        confirmView.context("contextPartnerId", commonSupplierPartner.getId().toString());
      }

      confirmView.context("purchaseOrderToMerge", Joiner.on(",").join(purchaseOrderIdList));

      response.setView(confirmView.map());

      return;
    }

    try {
      PurchaseOrder purchaseOrder =
          Beans.get(PurchaseOrderService.class)
              .mergePurchaseOrders(
                  purchaseOrderList,
                  commonCurrency,
                  commonSupplierPartner,
                  commonCompany,
                  commonContactPartner,
                  commonPriceList,
                  commonTradingName);
      if (purchaseOrder != null) {
        // Open the generated purchase order in a new tab
        response.setView(
            ActionView.define("Purchase order")
                .model(PurchaseOrder.class.getName())
                .add("grid", "purchase-order-grid")
                .add("form", "purchase-order-form")
                .param("search-filters", "purchase-order-filters")
                .param("forceEdit", "true")
                .context("_showRecord", String.valueOf(purchaseOrder.getId()))
                .map());
        response.setCanClose(true);
      }
    } catch (Exception e) {
      response.setFlash(e.getLocalizedMessage());
    }
  }

  /**
   * Called on partner, company or payment change. Fill the bank details with a default value.
   *
   * @param request
   * @param response
   */
  public void fillCompanyBankDetails(ActionRequest request, ActionResponse response) {
    try {
      PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);
      PaymentMode paymentMode = (PaymentMode) request.getContext().get("paymentMode");
      Company company = purchaseOrder.getCompany();
      Partner partner = purchaseOrder.getSupplierPartner();
      if (company == null) {
        return;
      }
      if (partner != null) {
        partner = Beans.get(PartnerRepository.class).find(partner.getId());
      }
      BankDetails defaultBankDetails =
          Beans.get(BankDetailsService.class)
              .getDefaultCompanyBankDetails(company, paymentMode, partner, null);
      response.setValue("companyBankDetails", defaultBankDetails);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void validate(ActionRequest request, ActionResponse response) {
    try {
      PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);
      purchaseOrder = Beans.get(PurchaseOrderRepository.class).find(purchaseOrder.getId());
      Beans.get(PurchaseOrderWorkflowService.class).validatePurchaseOrder(purchaseOrder);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void cancel(ActionRequest request, ActionResponse response) {
    try {
      PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);
      purchaseOrder = Beans.get(PurchaseOrderRepository.class).find(purchaseOrder.getId());
      Beans.get(PurchaseOrderWorkflowService.class).cancelPurchaseOrder(purchaseOrder);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Called on printing settings select. Set the domain for {@link PurchaseOrder#printingSettings}
   *
   * @param request
   * @param response
   */
  public void filterPrintingSettings(ActionRequest request, ActionResponse response) {
    try {
      PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);

      List<PrintingSettings> printingSettingsList =
          Beans.get(TradingNameService.class)
              .getPrintingSettingsList(purchaseOrder.getTradingName(), purchaseOrder.getCompany());
      String domain =
          String.format(
              "self.id IN (%s)",
              !printingSettingsList.isEmpty()
                  ? StringTool.getIdListString(printingSettingsList)
                  : "0");

      response.setAttr("printingSettings", "domain", domain);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Called on trading name change. Set the default value for {@link PurchaseOrder#printingSettings}
   *
   * @param request
   * @param response
   */
  public void fillDefaultPrintingSettings(ActionRequest request, ActionResponse response) {
    try {
      PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);
      response.setValue(
          "printingSettings",
          Beans.get(TradingNameService.class)
              .getDefaultPrintingSettings(
                  purchaseOrder.getTradingName(), purchaseOrder.getCompany()));
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Called from purchase order form view on partner change. Get the default price list for the
   * purchase order. Call {@link PartnerPriceListService#getDefaultPriceList(Partner, int)}.
   *
   * @param request
   * @param response
   */
  public void fillPriceList(ActionRequest request, ActionResponse response) {
    try {
      PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);
      response.setValue(
          "priceList",
          Beans.get(PartnerPriceListService.class)
              .getDefaultPriceList(
                  purchaseOrder.getSupplierPartner(), PriceListRepository.TYPE_PURCHASE));
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void changePriceListDomain(ActionRequest request, ActionResponse response) {
    try {
      PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);
      String domain =
          Beans.get(PartnerPriceListService.class)
              .getPriceListDomain(
                  purchaseOrder.getSupplierPartner(), PriceListRepository.TYPE_PURCHASE);
      response.setAttr("priceList", "domain", domain);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void finishPurchaseOrder(ActionRequest request, ActionResponse response) {
    try {
      PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);
      purchaseOrder = Beans.get(PurchaseOrderRepository.class).find(purchaseOrder.getId());

      Beans.get(PurchaseOrderWorkflowService.class).finishPurchaseOrder(purchaseOrder);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Called on supplier partner select. Set the domain for the field supplierPartner
   *
   * @param request
   * @param response
   */
  public void supplierPartnerDomain(ActionRequest request, ActionResponse response) {
    try {
      PurchaseOrder purchaseOrder = request.getContext().asType(PurchaseOrder.class);
      Company company = purchaseOrder.getCompany();
      long companyId = company.getPartner() == null ? 0L : company.getPartner().getId();
      String domain =
          String.format(
              "self.id != %d AND self.isContact = false AND self.isSupplier = true", companyId);
      String blockedPartnerQuery =
          Beans.get(BlockingService.class)
              .listOfBlockedPartner(company, BlockingRepository.PURCHASE_BLOCKING);

      if (!Strings.isNullOrEmpty(blockedPartnerQuery)) {
        domain += String.format(" AND self.id NOT in (%s)", blockedPartnerQuery);
      }

      domain += " AND :company member of self.companySet";
      response.setAttr("supplierPartner", "domain", domain);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void tw_create_engagement_achat(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("_id").toString());
    ComparaisonPrix comparaisonPrix = Beans.get(ComparaisonPrixRepository.class).find(id);

    EngagementAchat engagementAchat =
        Beans.get(EngagementAchatRepository.class)
            .all()
            .filter("self.natureOperation=:nature and self.id_reference_nature=:id_ref")
            .bind("nature", "Commande")
            .bind("id_ref", id)
            .fetchOne();

    if (engagementAchat != null) {
      engagementAchat.setMontantEngagement(comparaisonPrix.getTotal());
      engagementAchat.setDateEngagment(LocalDate.now());
      engagementAchat.setBudget(
          Beans.get(RubriqueBudgetaireGeneraleRepository.class)
              .find(comparaisonPrix.getConsultationPrix().getRubriqueBudgetaire().getId()));
    } else {
      engagementAchat = new EngagementAchat();
      engagementAchat.setMontantEngagement(comparaisonPrix.getTotal());
      engagementAchat.setEtatEngagement("Non Payé");
      engagementAchat.setNatureOperation("Commande");
      engagementAchat.setId_reference_nature(comparaisonPrix.getCommandeAchat().getId());
      engagementAchat.setDateEngagment(LocalDate.now());
      engagementAchat.setBudget(
          Beans.get(RubriqueBudgetaireGeneraleRepository.class)
              .find(comparaisonPrix.getConsultationPrix().getRubriqueBudgetaire().getId()));
    }
    engagementAchat = Beans.get(AppPurchaseService.class).save_EngagementAchat(engagementAchat);
  }

  public void tw_create_engagement_facture(ActionRequest request, ActionResponse response) {
    Long id = Long.valueOf(request.getContext().get("id").toString());
    FactureAchat facture = Beans.get(FactureAchatRepository.class).find(id);
    EngagementAchat eng =
        Beans.get(EngagementAchatRepository.class)
            .all()
            .filter("self.natureOperation=:nature and self.id_reference_nature=:id_ref")
            .bind("nature", "Facture")
            .bind("id_ref", id)
            .fetchOne();
    if (eng != null) {
      eng.setMontantEngagement(facture.getMontant());
      eng.setDateEngagment(LocalDate.now());
      eng.setBudget(
          Beans.get(RubriqueBudgetaireGeneraleRepository.class)
              .find(facture.getRubriqueBudg().getId()));
    } else {
      eng = new EngagementAchat();
      eng.setMontantEngagement(facture.getMontant());
      eng.setEtatEngagement("Non Payé");
      eng.setNatureOperation("Facture");
      eng.setId_reference_nature(facture.getId());
      eng.setDateEngagment(LocalDate.now());
      eng.setBudget(
          Beans.get(RubriqueBudgetaireGeneraleRepository.class)
              .find(facture.getRubriqueBudg().getId()));
    }

    eng = Beans.get(AppPurchaseService.class).save_EngagementAchat(eng);
  }

  public void tw_load_budget_depenses_bordereau_emission(
      ActionRequest request, ActionResponse response) {
    String[] names = new String[] {"budget"};
    int year = LocalDate.now().getYear();

    if (request.getContext().get("year") != null) {
      try {
        year = Integer.parseInt(request.getContext().get("year").toString());
      } catch (Exception ex) {
        response.setFlash(
            "Erreur : Impossible de convertir <b>"
                + request.getContext().get("year").toString()
                + "</b> en entier");
      }
    } else if (request.getContext().getParent() != null) {
      Context context = request.getContext().getParent();
      if (context.get("annee") != null) {
        try {
          year = Integer.parseInt(context.get("annee").toString());
        } catch (Exception ex) {
          response.setFlash(
              "Erreur : Impossible de convertir <b>"
                  + context.get("annee").toString()
                  + "</b> en entier");
        }
      } else if (context.get("year") != null) {
        try {
          year = Integer.parseInt(context.get("year").toString());
        } catch (Exception ex) {
          response.setFlash(
              "Erreur : Impossible de convertir <b>"
                  + context.get("year").toString()
                  + "</b> en entier");
        }
      }
    }

    for (String name : names) {
      response.setAttr(name, "domain", "self.id in (0)");
    }
    if (year == 0) {
      return;
    }
    try {
      VersionRubriqueBudgetaire versionRubriqueBudgetaire =
          Beans.get(VersionRubriqueBudgetaireRepository.class)
              .all()
              .filter("self.annee = :year")
              .bind("year", year)
              .fetchOne();
      if (versionRubriqueBudgetaire == null) {
        response.setFlash("Aucun budget pour annee " + year);
        return;
      }

      VersionRB version_rb =
          versionRubriqueBudgetaire.getVersionRubriques().stream()
              .filter(VersionRB::getIs_versionFinale)
              .findFirst()
              .orElse(null);

      if (version_rb == null) {
        response.setFlash("Aucun budget n'est validée pour année" + year);
        return;
      }

      List<Long> ids =
          Beans.get(RubriquesBudgetaireRepository.class).all()
              .filter(
                  "self.id_version = :ids_version and (self.code_budget like '2%' or self.code_budget like '6%')")
              .bind("ids_version", version_rb.getId()).fetch().stream()
              .map(RubriquesBudgetaire::getId_rubrique_generale)
              .collect(Collectors.toList());
      for (String name : names) {
        response.setAttr(
            name,
            "domain",
            "self.id in ("
                + ids.stream().map(Object::toString).collect(Collectors.joining(","))
                + ")");
      }

    } catch (Exception ex) {
      response.setFlash(ex.getMessage());
      return;
    }
  }

  public void tw_affecter_nature_budget(ActionRequest request, ActionResponse response) {
    RubriqueBudgetaireGenerale b = (RubriqueBudgetaireGenerale) request.getContext().get("budget");
    if (b.getCodeBudg().startsWith("6")) {
      response.setValue("type", "Charges d'exploitation");
    } else {
      response.setValue("type", "Charges d'investissement");
    }
  }

  public void tw_affecter_information_op_brdEmission(
      ActionRequest request, ActionResponse response) {

    HashSet<OrderPaymentCommande> op =
        (HashSet<OrderPaymentCommande>) request.getContext().get("ordrePayement");
    BigDecimal mt =
        op.stream()
            .map(orderPaymentCommande -> orderPaymentCommande.getSommeVerement())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    response.setValue("montantChiffre", mt);
    response.setValue("montantLettre", ConvertNomreToLettres.getStringMontant(mt));
    response.setValue("nbrOP", op.size());
  }

  public void tw_print_bord_emission(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Long id = (Long) request.getContext().get("id");
    String fileLink =
        ReportFactory.createReport(IReport.BORDERAU_EMISSION_GARDE, "Borderau d'emission")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id_bordereau", id)
            .generate()
            .getFileLink();

    response.setView(ActionView.define("Borderau d'emission").add("html", fileLink).map());
  }

  public void ImprimerCompteArrete(ActionRequest request, ActionResponse response)
      throws Exception {
    int annee = Integer.parseInt(request.getContext().get("anneeArreteCompte").toString());

    RecetteGeneraleGrand r =
        Beans.get(RecetteGeneraleGrandRepository.class)
            .all()
            .filter("self.annee=:annee")
            .bind("annee", annee)
            .fetchOne();
    if (r == null) {
      RecetteGeneraleGrand r_prec =
          Beans.get(RecetteGeneraleGrandRepository.class)
              .all()
              .filter("self.annee=:annee")
              .bind("annee", annee - 1)
              .fetchOne();
      if (r_prec != null) {
        r = Beans.get(PurchaseOrderService.class).dupliquerRecette(r_prec, annee);
      } else {
        response.setFlash(
            "Erreur : pas de Recette saisie dans l'année " + annee + " ou l'année " + (annee - 1));
        return;
      }
    }
    DepencesGenerale dp =
        Beans.get(DepencesGeneraleRepository.class)
            .all()
            .filter("self.year =:year")
            .bind("year", annee)
            .fetchOne();
    if (dp == null) {
      DepencesGenerale dp_prec =
          Beans.get(DepencesGeneraleRepository.class)
              .all()
              .filter("self.year =:year")
              .bind("year", (annee - 1))
              .fetchOne();
      if (dp_prec != null) {
        Beans.get(PurchaseOrderService.class).dupliquerDepenses(dp_prec, annee);
      } else {
        response.setFlash(
            "Erreur : pas de Dépences saisie dans l'année " + annee + " ou l'année " + (annee - 1));
        return;
      }
    }
    String req =
        "select sum(tab1.paiement) from arrete_des_comptes_depenses(" + annee + ") as tab1";
    BigDecimal mt = RunSqlRequestForMe.runSqlRequest_Bigdecimal(req);
    BigDecimal som_recette =
        r.getBudgetrea().stream()
            .map(RecetteGenerale::getRealisations)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal rest_disponible = som_recette.subtract(mt);
    String fileLink =
        ReportFactory.createReport(
                com.axelor.apps.account.report.IReport.ArreteCompte, "ArreteDeCompte")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("year", annee)
            .addParam("montantLettre", ConvertNomreToLettres.getStringMontant(rest_disponible))
            .addParam("montantChiffre", rest_disponible)
            .addParam("id_recette", r.getId())
            .generate()
            .getFileLink();
    response.setView(
        ActionView.define("Le fichier d'arrete des comptes").add("html", fileLink).map());
  }
}
