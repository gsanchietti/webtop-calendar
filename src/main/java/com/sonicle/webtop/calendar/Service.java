/*
 * webtop-calendar is a WebTop Service developed by Sonicle S.r.l.
 * Copyright (C) 2014 Sonicle S.r.l.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY SONICLE, SONICLE DISCLAIMS THE
 * WARRANTY OF NON INFRINGEMENT OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 *
 * You can contact Sonicle S.r.l. at email address sonicle@sonicle.com
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * "Powered by Sonicle WebTop" logo. If the display of the logo is not reasonably
 * feasible for technical reasons, the Appropriate Legal Notices must display
 * the words "Powered by Sonicle WebTop".
 */
package com.sonicle.webtop.calendar;

import com.sonicle.commons.db.DbUtils;
import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.commons.web.Crud;
import com.sonicle.commons.web.json.JsListPayload;
import com.sonicle.commons.web.json.Payload;
import com.sonicle.commons.web.ServletUtils;
import com.sonicle.commons.web.json.JsonResult;
import com.sonicle.commons.web.json.MapItem;
import com.sonicle.commons.web.json.extjs.ExtFieldMeta;
import com.sonicle.commons.web.json.extjs.ExtGridColumnMeta;
import com.sonicle.commons.web.json.extjs.ExtGridMetaData;
import com.sonicle.commons.web.json.extjs.ExtTreeNode;
import com.sonicle.webtop.calendar.CalendarUserSettings.CheckedCalendarGroups;
import com.sonicle.webtop.calendar.CalendarUserSettings.CheckedCalendars;
import com.sonicle.webtop.calendar.bol.CalendarGroup;
import com.sonicle.webtop.calendar.bol.model.Event;
import com.sonicle.webtop.calendar.bol.model.SchedulerEvent;
import com.sonicle.webtop.calendar.bol.MyCalendarGroup;
import com.sonicle.webtop.calendar.bol.OCalendar;
import com.sonicle.webtop.calendar.bol.SharedCalendarGroup;
import com.sonicle.webtop.calendar.bol.js.JsAttendee;
import com.sonicle.webtop.calendar.bol.js.JsAttendee.JsAttendeeList;
import com.sonicle.webtop.calendar.bol.js.JsSchedulerEvent;
import com.sonicle.webtop.calendar.bol.js.JsSchedulerEventDate;
import com.sonicle.webtop.calendar.bol.js.JsEvent;
import com.sonicle.webtop.calendar.bol.js.JsEventCalendar;
import com.sonicle.webtop.calendar.bol.js.JsExportStart;
import com.sonicle.webtop.calendar.bol.js.JsTreeCalendarEntry;
import com.sonicle.webtop.calendar.bol.js.JsTreeCalendarEntry.JsTreeCalendarEntries;
import com.sonicle.webtop.calendar.bol.model.EventAttendee;
import com.sonicle.webtop.calendar.bol.model.EventKey;
import com.sonicle.webtop.calendar.dal.CalendarDAO;
import com.sonicle.webtop.core.WT;
import com.sonicle.webtop.core.bol.OUser;
import com.sonicle.webtop.core.bol.js.JsSimple;
import com.sonicle.webtop.core.dal.UserDAO;
import com.sonicle.webtop.core.sdk.BaseService;
import com.sonicle.webtop.core.sdk.BasicEnvironment;
import com.sonicle.webtop.core.sdk.UserProfile;
import com.sonicle.webtop.core.sdk.WTRuntimeException;
import com.sonicle.webtop.core.util.LogEntries;
import com.sonicle.webtop.core.util.LogEntry;
import com.sonicle.webtop.core.util.MessageLogEntry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public class Service extends BaseService {
	public static final Logger logger = WT.getLogger(Service.class);
	
	private BasicEnvironment env = null;
	private CalendarManager manager;
	private CalendarUserSettings cus;
	
	public static final String EVENTS_EXPORT_FILENAME = "events_{0}-{1}-{2}.{3}";
	public final String DEFAULT_PERSONAL_CALENDAR_COLOR = "#FFFFFF";
	
	private final LinkedHashMap<String, CalendarGroup> calendarGroups = new LinkedHashMap<>();
	private CheckedCalendarGroups checkedCalendarGroups = null;
	private CheckedCalendars checkedCalendars = null;
	private ExportWizard wizard = null;

	@Override
	public void initialize() {
		env = getEnv();
		UserProfile profile = env.getProfile();
		manager = new CalendarManager(getManifest(), profile.getStringId());
		cus = new CalendarUserSettings(profile.getDomainId(), profile.getUserId(), getId());
		
		// Loads available groups
		initCalendarGroups();
	}
	
	@Override
	public void cleanup() {
		
	}
	
	@Override
	public HashMap<String, Object> returnClientOptions() {
		UserProfile profile = env.getProfile();
		HashMap<String, Object> hm = new HashMap<>();
		hm.put("view", cus.getCalendarView());
		hm.put("workdayStart", cus.getWorkdayStart());
		hm.put("workdayEnd", cus.getWorkdayEnd());
		return hm;
	}
	
	private void initCalendarGroups() {
		Connection con = null;
		
		try {
			con = getConnection();
			UserProfile.Id pid = env.getProfile().getId();
			synchronized(calendarGroups) {
				calendarGroups.clear();
				calendarGroups.putAll(manager.getCalendarGroups(con, pid));
				
				checkedCalendarGroups = cus.getCheckedCalendarGroups();
				if(checkedCalendarGroups.isEmpty()) {
					// If empty, adds MyGroup checked by default!
					checkedCalendarGroups.add(pid.toString());
					cus.setCheckedCalendarGroups(checkedCalendarGroups);
				}
				
				checkedCalendars = cus.getCheckedCalendars();
			}
			
		} catch(SQLException ex) {
			logger.error("Error initializing calendar groups", ex);
			//TODO: gestire errore
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void processManageCalendarsTree(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		ArrayList<ExtTreeNode> children = new ArrayList<>();
		ExtTreeNode child = null;
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String node = ServletUtils.getStringParameter(request, "node", true);
				if(node.equals("root")) { // Node: root -> list groups
					
					MyCalendarGroup myGroup = null;
					SharedCalendarGroup sharedGroup = null;
					for(CalendarGroup group : calendarGroups.values()) {
						if(group instanceof MyCalendarGroup) { // Adds group as Mine
							myGroup = (MyCalendarGroup)group;
							child = createCalendarGroupNode(myGroup, false);
							children.add(child.setExpanded(true));
							
						} else if(group instanceof SharedCalendarGroup) { // Adds group as Shared
							sharedGroup = (SharedCalendarGroup)group;
							child = createCalendarGroupNode(sharedGroup, false);
							children.add(child);
						}
					}

				} else { // Node: group -> list group's calendars
					UserProfile.Id upId = new UserProfile.Id(node);
					List<OCalendar> cals = manager.listCalendars(upId);
					
					for(OCalendar cal : cals) children.add(createCalendarNode(node, cal));
				}
				new JsonResult("children", children).printTo(out);
				
			} else if(crud.equals(Crud.UPDATE)) {
				JsListPayload<JsTreeCalendarEntries> pl = ServletUtils.getPayloadAsList(request, JsTreeCalendarEntries.class);
				
				for(JsTreeCalendarEntry cal : pl.data) {
					if(cal._nodeType.equals(JsTreeCalendarEntry.TYPE_GROUP)) {
						toggleCheckedCalendarGroup(cal._groupId, cal._visible);
					} else if(cal._nodeType.equals(JsTreeCalendarEntry.TYPE_CALENDAR)) {
						toggleCheckedCalendar(Integer.valueOf(cal.id), cal._visible);
					}
				}
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.DELETE)) {
				JsListPayload<JsTreeCalendarEntries> pl = ServletUtils.getPayloadAsList(request, JsTreeCalendarEntries.class);
				
				CalendarDAO cdao = CalendarDAO.getInstance();
				for(JsTreeCalendarEntry cal : pl.data) {
					if(cal._nodeType.equals("calendar")) {
						manager.deleteCalendar(Integer.valueOf(cal.id));
					}
				}
				new JsonResult().printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error executing action ManageCalendarsTree", ex);
		}
	}
	
	public void processManageCalendars(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		OCalendar item = null;
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				Integer id = ServletUtils.getIntParameter(request, "id", true);
				
				item = manager.getCalendar(id);
				new JsonResult(item).printTo(out);
				
			} else if(crud.equals(Crud.CREATE)) {
				Payload<MapItem, OCalendar> pl = ServletUtils.getPayload(request, OCalendar.class);
				
				item = manager.insertCalendar(pl.data);
				toggleCheckedCalendar(item.getCalendarId(), true);
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.UPDATE)) {
				Payload<MapItem, OCalendar> pl = ServletUtils.getPayload(request, OCalendar.class);
				
				manager.updateCalendar(pl.data);
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.DELETE)) {
				Payload<MapItem, OCalendar> pl = ServletUtils.getPayload(request, OCalendar.class);
				
				manager.deleteCalendar(pl.data.getCalendarId());
				new JsonResult().printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error executing action ManageCalendars", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	public void processGetCalendarGroups(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		Connection con = null;
		List<JsSimple> items = new ArrayList<>();
		
		try {
			Boolean writableOnly = ServletUtils.getBooleanParameter(request, "writableOnly", true);
			UserProfile up = env.getProfile();
			SharedCalendarGroup sharedGroup = null;
			for(CalendarGroup group : calendarGroups.values()) {
				if(group instanceof MyCalendarGroup) { // Adds group as Mine
					items.add(new JsSimple(up.getStringId(), up.getDisplayName()));
				} else if(group instanceof SharedCalendarGroup) { // Adds group as Shared
					//TODO: se writableOnly verificare che il gruppo condiviso sia scrivibile
					//if(writableOnly)
					sharedGroup = (SharedCalendarGroup)group;
					items.add(new JsSimple(sharedGroup.getId(), sharedGroup.getDisplayName()));
				}
			}
			
			new JsonResult("groups", items, items.size()).printTo(out);
			
		} catch(Exception ex) {
			logger.error("Error executing action GetCalendarGroups", ex);
			new JsonResult(false, "Error").printTo(out);
			
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void processGetCalendars(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		Connection con = null;
		List<JsEventCalendar> items = new ArrayList<>();
		
		try {
			JsEventCalendar jsCal = null;
			List<OCalendar> cals = null;
			for(CalendarGroup group : calendarGroups.values()) {
				cals = manager.listCalendars(new UserProfile.Id(group.getId()));
				for(OCalendar cal : cals) {
					jsCal = new JsEventCalendar();
					jsCal.fillFrom(cal);
					items.add(jsCal);
				}
			}
			new JsonResult("calendars", items, items.size()).printTo(out);
			
		} catch(Exception ex) {
			logger.error("Error executing action GetCalendars", ex);
			new JsonResult(false, "Error").printTo(out);
			
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void processGetSchedulerDates(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		Connection con = null;
		ArrayList<JsSchedulerEventDate> items = new ArrayList<>();
		
		try {
			con = getConnection();
			UserProfile up = env.getProfile();
			DateTimeZone utz = up.getTimeZone();
			DateTimeFormatter ymdZoneFmt = DateTimeUtils.createYmdFormatter(utz);
			
			// Defines boundaries
			String start = ServletUtils.getStringParameter(request, "startDate", true);
			String end = ServletUtils.getStringParameter(request, "endDate", true);
			DateTime fromDate = CalendarManager.parseYmdHmsWithZone(start, "00:00:00", up.getTimeZone());
			DateTime toDate = CalendarManager.parseYmdHmsWithZone(end, "23:59:59", up.getTimeZone());
			
			// Get events for each visible group
			Integer[] checked;
			List<DateTime> dates = null;
			for(CalendarGroup group : calendarGroups.values()) {
				if(!checkedCalendarGroups.contains(group.getId())) continue; // Skip if not visible
				
				checked = checkedCalendars.toArray(new Integer[checkedCalendars.size()]);
				dates = manager.getEventsDates(group, checked, fromDate, toDate, utz);
				for(DateTime dt : dates) {
					items.add(new JsSchedulerEventDate(ymdZoneFmt.print(dt)));
					//items.add(new JsSchedulerEventDate(CalendarManager.toYmdWithZone(dt, utz)));
				}
			}
			new JsonResult("dates", items).printTo(out);
			
		} catch(Exception ex) {
			logger.error("Error executing action GetSchedulerDates", ex);
			new JsonResult(false, "Error").printTo(out);
			
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void processManageEventsScheduler(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		Connection con = null;
		ArrayList<JsSchedulerEvent> items = new ArrayList<>();
		
		try {
			con = getConnection();
			UserProfile up = env.getProfile();
			DateTimeZone utz = up.getTimeZone();
			
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String from = ServletUtils.getStringParameter(request, "startDate", true);
				String to = ServletUtils.getStringParameter(request, "endDate", true);
				DateTime fromDate = CalendarManager.parseYmdHmsWithZone(from, "00:00:00", up.getTimeZone());
				DateTime toDate = CalendarManager.parseYmdHmsWithZone(to, "23:59:59", up.getTimeZone());
				
				// Get events for each visible group
				Integer[] checked = getCheckedCalendars();
				JsSchedulerEvent jse = null;
				List<SchedulerEvent> recInstances = null;
				List<CalendarManager.GroupEvents> grpEvts = null;
				for(CalendarGroup group : getCheckedCalendarGroups()) {
					grpEvts = manager.viewEvents(group, checked, fromDate, toDate);
					for(CalendarManager.GroupEvents ge : grpEvts) {
						for(SchedulerEvent evt : ge.events) {
							if(evt.getRecurrenceId() == null) {
								jse = new JsSchedulerEvent(ge.calendar, evt, up.getId(), utz);
								items.add(jse);
							} else {
								recInstances = manager.calculateRecurringInstances(evt, fromDate, toDate, utz);
								for(SchedulerEvent recInstance : recInstances) {
									jse = new JsSchedulerEvent(ge.calendar, recInstance, up.getId(), utz);
									items.add(jse);
								}
							}
						}
					}
				}
				new JsonResult("events", items).printTo(out);
				
			} else if(crud.equals(Crud.CREATE)) {
				Payload<MapItem, JsSchedulerEvent> pl = ServletUtils.getPayload(request, JsSchedulerEvent.class);
				
				DateTimeZone etz = DateTimeZone.forID(pl.data.timezone);
				DateTime newStart = CalendarManager.parseYmdHmsWithZone(pl.data.startDate, etz);
				DateTime newEnd = CalendarManager.parseYmdHmsWithZone(pl.data.endDate, etz);
				manager.copyEvent(EventKey.buildKey(pl.data.eventId, pl.data.originalEventId), newStart, newEnd);
				
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.UPDATE)) {
				Payload<MapItem, JsSchedulerEvent> pl = ServletUtils.getPayload(request, JsSchedulerEvent.class);
				
				DateTimeZone etz = DateTimeZone.forID(pl.data.timezone);
				DateTime newStart = CalendarManager.parseYmdHmsWithZone(pl.data.startDate, etz);
				DateTime newEnd = CalendarManager.parseYmdHmsWithZone(pl.data.endDate, etz);
				manager.updateEvent(pl.data.id, newStart, newEnd, pl.data.title);
				
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.DELETE)) {
				String uid = ServletUtils.getStringParameter(request, "id", true);
				String target = ServletUtils.getStringParameter(request, "target", "this");
				
				manager.deleteEvent(target, uid);
				new JsonResult().printTo(out);
				
			} else if(crud.equals("restore")) {
				String uid = ServletUtils.getStringParameter(request, "id", true);
				
				manager.restoreEvent(uid);
				new JsonResult().printTo(out);
			} else if(crud.equals("search")) {
				String query = ServletUtils.getStringParameter(request, "query", true);
				
				Integer[] checked = getCheckedCalendars();
				List<CalendarManager.GroupEvents> grpEvts = null;
				for(CalendarGroup group : getCheckedCalendarGroups()) {
					grpEvts = manager.searchEvents(group, checked, "%"+query+"%");
					for(CalendarManager.GroupEvents ge : grpEvts) {
						for(SchedulerEvent evt : ge.events) {
							if(evt.getRecurrenceId() == null) {
								items.add(new JsSchedulerEvent(ge.calendar, evt, up.getId(), utz));
							}
						}
					}
				}
				new JsonResult("events", items).printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error executing action ManageEventsScheduler", ex);
			new JsonResult(false, "Error").printTo(out);
			
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void processManageEvents(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		JsEvent item = null;
		
		try {
			UserProfile up = env.getProfile();
			
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String id = ServletUtils.getStringParameter(request, "id", true);
				
				Event evt = manager.readEvent(id);
				item = new JsEvent(evt, manager.getCalendarGroupId(evt.getCalendarId()));
				new JsonResult(item).printTo(out);
				
			} else if(crud.equals(Crud.CREATE)) {
				Payload<MapItem, JsEvent> pl = ServletUtils.getPayload(request, JsEvent.class);
				
				//TODO: verificare che il calendario supporti la scrittura (specialmente per quelli condivisi)
				
				Event evt = JsEvent.buildEvent(pl.data, cus.getWorkdayStart(), cus.getWorkdayEnd());
				// Adds an organizer if event doesn't have it
				if(evt.hasAttendees()) {
					EventAttendee org = evt.getOrganizer();
					if(org == null) {
						org = new EventAttendee();
						org.setRecipient(up.getEmailAddress());
						org.setRecipientType(EventAttendee.RECIPIENT_TYPE_ORGANIZER);
						org.setResponseStatus(EventAttendee.RESPONSE_STATUS_ACCEPTED);
						org.setNotify(false);
						evt.getAttendees().add(org);
					}
				}
				manager.addEvent(evt);
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.UPDATE)) {
				String target = ServletUtils.getStringParameter(request, "target", "this");
				Payload<MapItem, JsEvent> pl = ServletUtils.getPayload(request, JsEvent.class);
				
				Event evt = JsEvent.buildEvent(pl.data, cus.getWorkdayStart(), cus.getWorkdayEnd());
				manager.editEvent(target, evt, up.getTimeZone());
				new JsonResult().printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error executing action ManageEvents", ex);
			new JsonResult(false, "Error").printTo(out);	
		}
	}
	
	public void processExportWizard(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		UserProfile up = env.getProfile();
		
		try {
			String step = ServletUtils.getStringParameter(request, "step", true);
			if(step.equals("start")) {
				Payload<MapItem, JsExportStart> pl = ServletUtils.getPayload(request, JsExportStart.class);
				DateTimeFormatter ymd = DateTimeUtils.createYmdFormatter(up.getTimeZone());
				
				wizard = new ExportWizard();
				wizard.fromDate = ymd.parseDateTime(pl.data.fromDate).withTimeAtStartOfDay();
				wizard.toDate = DateTimeUtils.withTimeAtEndOfDay(ymd.parseDateTime(pl.data.toDate));
				
				new JsonResult().printTo(out);
				
			} else if(step.equals("end")) {
				File file = WT.createTempFile();
				LogEntries log = new LogEntries();
				DateTimeFormatter ymd = DateTimeUtils.createFormatter("yyyyMMdd", up.getTimeZone());
				DateTimeFormatter ymdhms = DateTimeUtils.createFormatter("yyyy-MM-dd HH:mm:ss", up.getTimeZone());
				
				try (FileOutputStream fos = new FileOutputStream(file)) {
					log.addMaster(new MessageLogEntry(LogEntry.LEVEL_INFO, "Started on {0}", ymdhms.print(new DateTime())));
					manager.exportEvents(log, up.getDomainId(), wizard.fromDate, wizard.toDate, fos);
					log.addMaster(new MessageLogEntry(LogEntry.LEVEL_INFO, "Ended on {0}", ymdhms.print(new DateTime())));
					wizard.file = file;
					wizard.filename = MessageFormat.format(EVENTS_EXPORT_FILENAME, up.getDomainId(), ymd.print(wizard.fromDate), ymd.print(wizard.fromDate), "csv");
					log.addMaster(new MessageLogEntry(LogEntry.LEVEL_INFO, "File ready: {0}", wizard.filename));
					log.addMaster(new MessageLogEntry(LogEntry.LEVEL_INFO, "Operation completed succesfully"));
					new JsonResult(log.print()).printTo(out);
					
				} catch(Exception ex1) {
					ex1.printStackTrace();
					new JsonResult(log.print()).setSuccess(false).printTo(out);
				}
			}
			
		} catch(Exception ex) {
			logger.error("Error executing action ExportWizard", ex);
			new JsonResult(false, "Error").printTo(out);	
		}
	}
	
	public void processExportWizard(HttpServletRequest request, HttpServletResponse response) {
		UserProfile up = env.getProfile();
		try {
			try(FileInputStream fis = new FileInputStream(wizard.file)) {
				ServletUtils.writeFileStream(response, wizard.filename, fis, false);
			}
			
		} catch(Exception ex) {
			ex.printStackTrace();
		} finally {
			wizard = null;
		}
	}
	
	public void processGetPlanning(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		Connection con = null;
		ArrayList<MapItem> items = new ArrayList<>();
		
		try {
			String eventStartDate = ServletUtils.getStringParameter(request, "startDate", true);
			String eventEndDate = ServletUtils.getStringParameter(request, "endDate", true);
			String timezone = ServletUtils.getStringParameter(request, "timezone", true);
			JsAttendeeList attendees = ServletUtils.getObjectParameter(request, "attendees", new JsAttendeeList(), JsAttendeeList.class);
			
			// Parses string parameters
			DateTimeZone eventTz = DateTimeZone.forID(timezone);
			DateTime eventStartDt = CalendarManager.parseYmdHmsWithZone(eventStartDate, eventTz);
			DateTime eventEndDt = CalendarManager.parseYmdHmsWithZone(eventEndDate, eventTz);
			
			UserProfile up = env.getProfile();
			DateTimeZone profileTz = up.getTimeZone();
			
			LocalTime localStartTime = eventStartDt.toLocalTime();
			LocalTime localEndTime = eventEndDt.toLocalTime();
			LocalTime fromTime = DateTimeUtils.min(localStartTime, cus.getWorkdayStart());
			LocalTime toTime = DateTimeUtils.max(localEndTime, cus.getWorkdayEnd());
			
			// Defines useful date/time formatters
			DateTimeFormatter ymdhmFmt = DateTimeUtils.createYmdHmFormatter();
			DateTimeFormatter tFmt = DateTimeUtils.createFormatter(env.getCoreUserSettings().getShortTimeFormat());
			DateTimeFormatter dFmt = DateTimeUtils.createFormatter(env.getCoreUserSettings().getShortDateFormat());
			
			// Generates fields and columnsInfo dynamically
			ArrayList<String> hours = manager.generateTimeSpans(60, eventStartDt.toLocalDate(), eventEndDt.toLocalDate(), cus.getWorkdayStart(), cus.getWorkdayEnd(), profileTz);
			ArrayList<ExtFieldMeta> fields = new ArrayList<>();
			ArrayList<ExtGridColumnMeta> colsInfo = new ArrayList<>();
			
			ExtGridColumnMeta col = null;
			fields.add(new ExtFieldMeta("recipient"));
			colsInfo.add(new ExtGridColumnMeta("recipient"));
			for(String hourKey : hours) {
				LocalDateTime ldt = ymdhmFmt.parseLocalDateTime(hourKey);
				fields.add(new ExtFieldMeta(hourKey));
				col = new ExtGridColumnMeta(hourKey, tFmt.print(ldt));
				col.put("date", dFmt.print(ldt));
				col.put("overlaps", DateTimeUtils.between(ldt, eventStartDt.toLocalDateTime(), eventEndDt.toLocalDateTime()));
				colsInfo.add(col);
			}
			
			// Collects attendees availability...
			OUser user = null;
			UserProfile.Id profileId = null;
			LinkedHashSet<String> busyHours = null;
			MapItem item = null;
			for(JsAttendee attendee : attendees) {
				item = new MapItem();
				item.put("recipient", attendee.recipient);
				
				user = guessUserByAttendee(attendee.recipient);
				if(user != null) {
					profileId = new UserProfile.Id(user.getDomainId(), user.getUserId());
					busyHours = manager.calculateAvailabilitySpans(60, profileId, eventStartDt.withTime(fromTime), eventEndDt.withTime(toTime), eventTz, true);
					for(String hourKey : hours) {
						if(busyHours.contains(hourKey)) {
							item.put(hourKey, "busy");
						} else {
							item.put(hourKey, "free");
						}
					}
				} else {
					for(String hourKey : hours) {
						item.put(hourKey, "unknown");
					}
				}
				
				items.add(item);
			}
			
			ExtGridMetaData meta = new ExtGridMetaData(true);
			meta.setFields(fields);
			meta.setColumnsInfo(colsInfo);
			new JsonResult(items, meta, items.size()).printTo(out);
			
		} catch(Exception ex) {
			logger.error("Error executing action ManageEvents", ex);
			new JsonResult(false, "Error").printTo(out);
			
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void processICalImportUploadStream(HttpServletRequest request, InputStream uploadStream) throws Exception {
		UserProfile up = env.getProfile();
		Integer calendarId = ServletUtils.getIntParameter(request, "calendarId", true);
		manager.importICal(calendarId, uploadStream, up.getTimeZone());
	}
	
	private List<CalendarGroup> getCheckedCalendarGroups() {
		ArrayList<CalendarGroup> groups = new ArrayList<>();
		for(CalendarGroup group : calendarGroups.values()) {
			if(!checkedCalendarGroups.contains(group.getId())) continue; // Skip group if not visible
			groups.add(group);
		}
		return groups;
	}
	
	private Integer[] getCheckedCalendars() {
		return checkedCalendars.toArray(new Integer[checkedCalendars.size()]);
	}
	
	private OUser guessUserByAttendee(String recipient) {
		Connection con = null;
		
		try {
			//TODO: gestire definitivamente il campo attendee.recipient... lookup per email???
			UserProfile.Id profileId = new UserProfile.Id(recipient);
			
			con = WT.getCoreConnection();
			UserDAO udao = UserDAO.getInstance();
			return udao.selectByDomainUser(con, profileId.getDomainId(), profileId.getUserId());
		
		} catch(WTRuntimeException ex) {
			return null;
		} catch(Exception ex) {
			logger.error("Error guessing user from attendee", ex);
			return null;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	private void toggleCheckedCalendarGroup(String groupId, boolean checked) {
		synchronized(calendarGroups) {
			if(checked) {
				checkedCalendarGroups.add(groupId);
			} else {
				checkedCalendarGroups.remove(groupId);
			}
			cus.setCheckedCalendarGroups(checkedCalendarGroups);
		}
	}
	
	private void toggleCheckedCalendar(int calendarId, boolean checked) {
		synchronized(calendarGroups) {
			if(checked) {
				checkedCalendars.add(calendarId);
			} else {
				checkedCalendars.remove(calendarId);
			}
			cus.setCheckedCalendars(checkedCalendars);
		}
	}
	
	private ExtTreeNode createCalendarGroupNode(MyCalendarGroup group, boolean leaf) {
		return createCalendarGroupNode(group.getId(), lookupResource(CalendarLocaleKey.MY_CALENDARS), leaf, "wtcal-icon-calendars-my");
	}
	
	private ExtTreeNode createCalendarGroupNode(SharedCalendarGroup group, boolean leaf) {
		return createCalendarGroupNode(group.getId(), group.getDisplayName(), leaf, "wtcal-icon-calendars-shared");
	}
	
	private ExtTreeNode createCalendarGroupNode(String id, String text, boolean leaf, String iconClass) {
		boolean visible = checkedCalendarGroups.contains(id);
		ExtTreeNode node = new ExtTreeNode(id, text, leaf);
		node.put("_nodeType", "group");
		node.put("_groupId", id);
		node.put("_visible", visible);
		node.setIconClass(iconClass);
		node.setChecked(visible);
		return node;
	}
	
	private ExtTreeNode createCalendarNode(String groupId, OCalendar cal) {
		boolean visible = checkedCalendars.contains(cal.getCalendarId());
		ExtTreeNode node = new ExtTreeNode(cal.getCalendarId(), cal.getName(), true);
		node.put("_nodeType", "calendar");
		node.put("_groupId", groupId);
		node.put("_builtIn", cal.getBuiltIn());
		node.put("_default", cal.getIsDefault());
		node.put("_color", cal.getColor());
		node.put("_visible", visible);
		node.put("_isPrivate", cal.getIsPrivate());
		node.put("_busy", cal.getBusy());
		node.put("_reminder", cal.getReminder());
		if(cal.getIsDefault()) node.setCls("wtcal-tree-default");
		node.setIconClass("wt-palette-" + cal.getHexColor());
		node.setChecked(visible);
		return node;
	}
	
	private static class ExportWizard {
		public DateTime fromDate;
		public DateTime toDate;
		public File file;
		public String filename;
	}
}
