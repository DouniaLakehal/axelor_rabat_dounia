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
import com.axelor.apps.base.db.Product;
import com.axelor.apps.configuration.db.RubriqueBudgetaireGenerale;
import com.axelor.apps.configuration.db.repo.RubriqueBudgetaireGeneraleRepository;
import com.axelor.apps.purchase.report.IReport;
import com.axelor.apps.purchase.service.PurchaseProductService;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.exception.AxelorException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Singleton;
import java.time.LocalDate;

@Singleton
public class PurchaseProductController {

  /**
   * Called from product form view, on {@link Product#defShipCoefByPartner} change. Call {@link
   * PurchaseProductService#getLastShippingCoef(Product)}.
   *
   * @param request
   * @param response
   */
  public void fillShippingCoeff(ActionRequest request, ActionResponse response) {
    try {
      Product product = request.getContext().asType(Product.class);
      if (!product.getDefShipCoefByPartner()) {
        return;
      }
      response.setValue(
          "shippingCoef", Beans.get(PurchaseProductService.class).getLastShippingCoef(product));
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void tw_imprimer_situation_depenses(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Integer year = LocalDate.now().getYear();
    try {
      year = Integer.valueOf(request.getContext().get("year").toString());
    } catch (Exception ex) {
      response.setFlash(
          "Attention impossible de convertir "
              + request.getContext().get("year").toString()
              + " en entier");
      return;
    }

    RubriqueBudgetaireGenerale rubriqueBudgetaire =
        (RubriqueBudgetaireGenerale) request.getContext().get("rubriqueBudgetaire");
    Long id = rubriqueBudgetaire.getId();
    RubriqueBudgetaireGenerale rub = Beans.get(RubriqueBudgetaireGeneraleRepository.class).find(id);
    String fileLink =
        ReportFactory.createReport(IReport.GENERER_SITUATION, "Situation")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("id_rubriqueGenerale", rub.getId())
            .generate()
            .getFileLink();
    response.setView(ActionView.define("lettre de consultation").add("html", fileLink).map());
  }
}
