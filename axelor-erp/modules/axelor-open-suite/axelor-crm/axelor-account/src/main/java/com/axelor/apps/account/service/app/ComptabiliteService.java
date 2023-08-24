package com.axelor.apps.account.service.app;

import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.math.BigDecimal;

public interface ComptabiliteService extends AppBaseService {

  public void saveFirstAffranchisement(ActionRequest request, ActionResponse response);

  public BigDecimal calculTotalAffranch(int annee);

  public BigDecimal getSommeComptes(int annee);
}
