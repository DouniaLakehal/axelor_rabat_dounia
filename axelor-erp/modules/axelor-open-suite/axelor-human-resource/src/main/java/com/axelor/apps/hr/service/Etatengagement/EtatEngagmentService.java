package com.axelor.apps.hr.service.Etatengagement;

import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.EtatEngagement;
import java.time.LocalDate;
import java.util.Map;

public interface EtatEngagmentService {

  public Map<String, Object> getDataInfo1(Employee emp, LocalDate d1, LocalDate d2);

  public EtatEngagement check_changement(
      Employee emp, LocalDate d1, LocalDate d2, Map<String, Object> data);

  public void deleteEtatEngagement(Long id);
}
