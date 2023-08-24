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
package com.axelor.apps.hr.service.Etatengagement;

import com.axelor.apps.configuration.db.Echelon;
import com.axelor.apps.hr.db.*;
import com.google.inject.persist.Transactional;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppServiceEtatEngagement extends ServiceFunction implements EtatEngagmentService {

  private static final Logger PC = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public Map<String, Object> getDataInfo1(Employee employer, LocalDate d1, LocalDate d2) {
    LocalDate tmp = d1.minusDays(1);
    Map<String, Object> map = new HashMap<>();

    Echelon echelon_old = super.getEchelon_at_Date(employer.getId(), tmp);
    Echelon echelon_new = super.getEchelon_at_Date(employer.getId(), d2);

    map.put("echelon_old", echelon_old.getId());
    map.put("echelon_new", echelon_new.getId());

    map.put("enfant_old", super.enfant_21_max(employer, tmp));
    map.put("enfant_new", super.enfant_21_max(employer, d2));

    map.put("sitFamille_old", super.mariage_situation(employer, tmp));
    map.put("sitFamille_new", super.mariage_situation(employer, d2));

    map.put("duration", super.getDuration_betweenTo_date(d1, d2));

    map.put("oldEqulasNew", super.checkEquals(map));
    return map;
  }

  @Override
  @Transactional
  public EtatEngagement check_changement(
      Employee emp, LocalDate d1, LocalDate d2, Map<String, Object> data) {
    EtatEngagement etat = super.getEtatengagementByemployer(emp.getId(), d1, d2);
    EtatEngagement id_etat = null;
    boolean equlz = (boolean) data.get("oldEqulasNew");
    if (etat == null) {
      id_etat = super.saveEtatEngagment(data, emp, d1, d2);
    }
    return id_etat;
  }

  @Override
  @Transactional
  public void deleteEtatEngagement(Long id) {
    EtatEngagement etat = etatEngagementRepository.find(id);
    Employee emp = employeeRepository.find(etat.getEmployee().getId());
    List<EtatEngagement> ls = emp.getEtatEngagement();
    ls.remove(etat);
    emp.setEtatEngagement(ls);
    employeeRepository.save(emp);
    etatEngagementRepository.remove(etat);
  }
}
