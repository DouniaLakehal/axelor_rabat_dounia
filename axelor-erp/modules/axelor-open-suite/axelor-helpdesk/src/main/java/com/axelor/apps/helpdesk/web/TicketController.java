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
package com.axelor.apps.helpdesk.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.base.db.Timer;
import com.axelor.apps.base.db.repo.TimerRepository;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.helpdesk.db.ActionsReclamation;
import com.axelor.apps.helpdesk.db.Imprimer;
import com.axelor.apps.helpdesk.db.Ticket;
import com.axelor.apps.helpdesk.db.repo.TicketRepository;
import com.axelor.apps.helpdesk.exceptions.IExceptionMessage;
import com.axelor.apps.helpdesk.report.IReport;
import com.axelor.apps.helpdesk.service.TicketService;
import com.axelor.apps.helpdesk.service.TicketServiceImpl;
import com.axelor.apps.helpdesk.service.TimerTicketService;
import com.axelor.apps.report.engine.ReportSettings;
import com.axelor.apps.tool.date.DateTool;
import com.axelor.apps.tool.date.DurationTool;
import com.axelor.exception.AxelorException;
import com.axelor.exception.ResponseMessageType;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.CallMethod;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TicketController {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String HIDDEN_ATTR = "hidden";

  @Inject TicketServiceImpl appService;

  /**
   * Ticket assign to the current user.
   *
   * @param request
   * @param response
   */
  public void assignToMeTicket(ActionRequest request, ActionResponse response) {
    try {
      Long id = (Long) request.getContext().get("id");
      List<?> ids = (List<?>) request.getContext().get("_ids");

      if (id == null && ids == null) {
        response.setAlert(I18n.get(IExceptionMessage.SELECT_TICKETS));
      } else {
        Beans.get(TicketService.class).assignToMeTicket(id, ids);
        response.setReload(true);
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Compute duration or endDateTime from startDateTime
   *
   * @param request
   * @param response
   */
  public void computeFromStartDateTime(ActionRequest request, ActionResponse response) {
    try {
      Ticket ticket = request.getContext().asType(Ticket.class);

      if (ticket.getStartDateT() != null) {
        if (ticket.getDuration() != null && ticket.getDuration() != 0) {
          response.setValue(
              "endDateT", DateTool.plusSeconds(ticket.getStartDateT(), ticket.getDuration()));

        } else if (ticket.getEndDateT() != null
            && ticket.getEndDateT().isAfter(ticket.getStartDateT())) {
          Duration duration =
              DurationTool.computeDuration(ticket.getStartDateT(), ticket.getEndDateT());
          response.setValue("duration", DurationTool.getSecondsDuration(duration));
        }
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  /**
   * Compute startDateTime or endDateTime from duration
   *
   * @param request
   * @param response
   */
  public void computeFromDuration(ActionRequest request, ActionResponse response) {
    try {
      Ticket ticket = request.getContext().asType(Ticket.class);

      if (ticket.getDuration() != null) {
        if (ticket.getStartDateT() != null) {
          response.setValue(
              "endDateT", DateTool.plusSeconds(ticket.getStartDateT(), ticket.getDuration()));

        } else if (ticket.getEndDateT() != null) {
          response.setValue(
              "startDateT", DateTool.minusSeconds(ticket.getEndDateT(), ticket.getDuration()));
        }
      }
    } catch (Exception e) {
      TraceBackService.trace(e);
    }
  }

  /**
   * Compute duration or startDateTime from endDateTime
   *
   * @param request
   * @param response
   */
  public void computeFromEndDateTime(ActionRequest request, ActionResponse response) {
    try {
      Ticket ticket = request.getContext().asType(Ticket.class);

      if (ticket.getEndDateT() != null) {

        if (ticket.getStartDateT() != null
            && ticket.getStartDateT().isBefore(ticket.getEndDateT())) {
          Duration duration =
              DurationTool.computeDuration(ticket.getStartDateT(), ticket.getEndDateT());
          response.setValue("duration", DurationTool.getSecondsDuration(duration));

        } else if (ticket.getDuration() != null) {
          response.setValue(
              "startDateT", DateTool.minusSeconds(ticket.getEndDateT(), ticket.getDuration()));
        }
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void manageTimerButtons(ActionRequest request, ActionResponse response) {
    try {
      Ticket ticket = request.getContext().asType(Ticket.class);
      TimerTicketService service = Beans.get(TimerTicketService.class);

      Timer timer = service.find(ticket);

      boolean hideStart = false;
      boolean hideCancel = true;
      if (timer != null) {
        hideStart = timer.getStatusSelect() == TimerRepository.TIMER_STARTED;
        hideCancel =
            timer.getTimerHistoryList().isEmpty()
                || timer.getStatusSelect().equals(TimerRepository.TIMER_STOPPED);
      }

      response.setAttr("startTimerBtn", HIDDEN_ATTR, hideStart);
      response.setAttr("stopTimerBtn", HIDDEN_ATTR, !hideStart);
      response.setAttr("cancelTimerBtn", HIDDEN_ATTR, hideCancel);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void computeTotalTimerDuration(ActionRequest request, ActionResponse response) {
    try {
      Ticket ticket = request.getContext().asType(Ticket.class);
      if (ticket.getId() != null) {
        Duration duration = Beans.get(TimerTicketService.class).compute(ticket);
        response.setValue("$_totalTimerDuration", duration.toMinutes() / 60F);
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void startTimer(ActionRequest request, ActionResponse response) {
    try {
      Ticket ticket = request.getContext().asType(Ticket.class);
      Beans.get(TimerTicketService.class)
          .start(ticket, Beans.get(AppBaseService.class).getTodayDateTime().toLocalDateTime());
    } catch (Exception e) {
      TraceBackService.trace(response, e, ResponseMessageType.ERROR);
    }
  }

  public void stopTimer(ActionRequest request, ActionResponse response) {
    try {
      Ticket ticket = request.getContext().asType(Ticket.class);
      Beans.get(TimerTicketService.class)
          .stop(ticket, Beans.get(AppBaseService.class).getTodayDateTime().toLocalDateTime());
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void cancelTimer(ActionRequest request, ActionResponse response) {
    try {
      Ticket ticket = request.getContext().asType(Ticket.class);
      Beans.get(TimerTicketService.class).cancel(ticket);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  public void computeRealDuration(ActionRequest request, ActionResponse response) {
    try {
      Ticket ticket = request.getContext().asType(Ticket.class);
      if (ticket.getId() != null && ticket.getRealTotalDuration().compareTo(BigDecimal.ZERO) == 0) {
        response.setValue(
            "realTotalDuration",
            Beans.get(TimerTicketService.class).compute(ticket).toMinutes() / 60F);
      }

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  @Transactional
  public void timerStateOn(ActionRequest request, ActionResponse response) {
    try {
      TicketRepository ticketRepo = Beans.get(TicketRepository.class);
      Ticket ticket = request.getContext().asType(Ticket.class);
      ticket = ticketRepo.find(ticket.getId());
      ticket.setTimerState(true);
      ticketRepo.save(ticket);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  @Transactional
  public void timerStateOff(ActionRequest request, ActionResponse response) {
    try {
      TicketRepository ticketRepo = Beans.get(TicketRepository.class);
      Ticket ticket = request.getContext().asType(Ticket.class);
      ticket = ticketRepo.find(ticket.getId());
      ticket.setTimerState(false);
      ticketRepo.save(ticket);
      response.setReload(true);
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  @CallMethod
  public void printReclamation(ActionRequest request, ActionResponse response)
      throws AxelorException {

    Imprimer imp = request.getContext().asType(Imprimer.class);

    Date from_ = Date.valueOf(imp.getFrom_());
    Date to_ = Date.valueOf(imp.getTo_());

    String fileLink =
        ReportFactory.createReport(IReport.RECLAMATION, "reclamation" + "-${date}")
            .addParam("Locale", ReportSettings.getPrintingLocale(null))
            .addParam("from", from_)
            .addParam("to", to_)
            .generate()
            .getFileLink();

    LOG.debug("Printing " + "reclamation");

    response.setView(ActionView.define("reclamation").add("html", fileLink).map());
  }

  /*-- hatim --*/

  public void update_numreclamation(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    String code = appService.chargerNumeros(id);
    if (!code.equals("")) {
      response.setValue("ticketSeqence", code);
    }
  }

  public void load_action_reclamation(ActionRequest request, ActionResponse response) {
    System.out.println(request);
    Long id = (Long) request.getContext().get("id");
    Map<String, Object> u = (Map<String, Object>) request.getContext().get("_user");
    Integer i = (Integer) u.get("id");
    response.setView(
        ActionView.define("Liste des Actions")
            .model(ActionsReclamation.class.getName())
            .add("grid", "actionsReclamation_grid_responsable")
            .add("form", "actionsReclamation_form_responsable_buton")
            .domain("self.reclamation=" + id + " AND self.responsable=" + i)
            .map());
  }

  public void updateactionreclamation(ActionRequest request, ActionResponse response) {
    HashMap<String, Object> _parent = (HashMap<String, Object>) request.getContext().get("_parent");
    Integer id = (Integer) _parent.get("id");
    response.setValue("reclamation", id);
  }

  public void test_for_chow_bouton_resoudre(ActionRequest request, ActionResponse response) {
    Long id = (Long) request.getContext().get("id");
    // true = all tache terminer
    // false = il reste des taches non terminées
    boolean all_tache_finich = appService.checkAllTachesReclamation(id);
    if (all_tache_finich) {
      response.setHidden("bouton_resolue", false);
    } else {
      response.setHidden("bouton_resolue", true);
    }
  }
}
