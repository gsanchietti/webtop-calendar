/* 
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
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 *
 * You can contact Sonicle S.r.l. at email address sonicle[at]sonicle[dot]com
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * Sonicle logo and Sonicle copyright notice. If the display of the logo is not
 * reasonably feasible for technical reasons, the Appropriate Legal Notices must
 * display the words "Copyright (C) 2014 Sonicle S.r.l.".
 */
package com.sonicle.webtop.calendar;

import com.sonicle.commons.MailUtils;
import com.sonicle.commons.db.DbUtils;
import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.commons.web.Crud;
import com.sonicle.commons.web.DispositionType;
import com.sonicle.commons.web.json.PayloadAsList;
import com.sonicle.commons.web.json.Payload;
import com.sonicle.commons.web.ServletUtils;
import com.sonicle.commons.web.ServletUtils.StringArray;
import com.sonicle.commons.web.json.CompositeId;
import com.sonicle.commons.web.json.JsonResult;
import com.sonicle.commons.web.json.MapItem;
import com.sonicle.commons.web.json.extjs.FieldMeta;
import com.sonicle.commons.web.json.extjs.GridColumnMeta;
import com.sonicle.commons.web.json.extjs.GridMetadata;
import com.sonicle.commons.web.json.extjs.ExtTreeNode;
import com.sonicle.webtop.calendar.CalendarUserSettings.CheckedFolders;
import com.sonicle.webtop.calendar.CalendarUserSettings.CheckedRoots;
import com.sonicle.webtop.calendar.bol.model.SchedulerEventInstance;
import com.sonicle.webtop.calendar.bol.js.JsAttendee;
import com.sonicle.webtop.calendar.bol.js.JsAttendee.JsAttendeeList;
import com.sonicle.webtop.calendar.bol.js.JsCalendar;
import com.sonicle.webtop.calendar.bol.js.JsSchedulerEvent;
import com.sonicle.webtop.calendar.bol.js.JsSchedulerEventDate;
import com.sonicle.webtop.calendar.bol.js.JsEvent;
import com.sonicle.webtop.calendar.bol.js.JsCalendarLkp;
import com.sonicle.webtop.calendar.bol.js.JsErpExportStart;
import com.sonicle.webtop.calendar.bol.js.JsFolderNode;
import com.sonicle.webtop.calendar.bol.js.JsFolderNode.JsFolderNodeList;
import com.sonicle.webtop.calendar.bol.js.JsSharing;
import com.sonicle.webtop.calendar.model.CalendarFolder;
import com.sonicle.webtop.calendar.model.CalendarRoot;
import com.sonicle.webtop.calendar.model.EventInstance;
import com.sonicle.webtop.calendar.bol.model.RBEventDetail;
import com.sonicle.webtop.calendar.model.EventKey;
import com.sonicle.webtop.calendar.bol.model.MyCalendarFolder;
import com.sonicle.webtop.calendar.bol.model.MyCalendarRoot;
import com.sonicle.webtop.calendar.io.EventICalFileReader;
import com.sonicle.webtop.calendar.model.Calendar;
import com.sonicle.webtop.calendar.rpt.AbstractAgenda;
import com.sonicle.webtop.calendar.rpt.RptAgendaSummary;
import com.sonicle.webtop.calendar.rpt.RptEventsDetail;
import com.sonicle.webtop.calendar.rpt.RptAgendaWeek5;
import com.sonicle.webtop.calendar.rpt.RptAgendaWeek7;
import com.sonicle.webtop.core.CoreManager;
import com.sonicle.webtop.core.CoreUserSettings;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.app.WebTopSession.UploadedFile;
import com.sonicle.webtop.core.bol.OUser;
import com.sonicle.webtop.core.bol.js.JsSimple;
import com.sonicle.webtop.core.bol.js.JsWizardData;
import com.sonicle.webtop.core.bol.model.Sharing;
import com.sonicle.webtop.core.model.SharePermsRoot;
import com.sonicle.webtop.core.dal.UserDAO;
import com.sonicle.webtop.core.io.output.AbstractReport;
import com.sonicle.webtop.core.io.output.ReportConfig;
import com.sonicle.webtop.core.sdk.BaseService;
import com.sonicle.webtop.core.sdk.UserProfile;
import com.sonicle.webtop.core.sdk.UserProfileId;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.core.sdk.WTRuntimeException;
import com.sonicle.webtop.core.util.LogEntries;
import com.sonicle.webtop.core.util.LogEntry;
import com.sonicle.webtop.core.util.MessageLogEntry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
	
	private CalendarManager manager;
	private CalendarServiceSettings ss;
	private CalendarUserSettings us;
	
	public static final String ERP_EXPORT_FILENAME = "events_{0}-{1}-{2}.{3}";
	
	private final LinkedHashMap<String, CalendarRoot> roots = new LinkedHashMap<>();
	private final HashMap<String, ArrayList<CalendarFolder>> foldersByRoot = new HashMap<>();
	private final LinkedHashMap<Integer, CalendarFolder> folders = new LinkedHashMap<>();
	private CheckedRoots checkedRoots = null;
	private CheckedFolders checkedFolders = null;
	private ErpExportWizard erpWizard = null;

	@Override
	public void initialize() throws Exception {
		UserProfile up = getEnv().getProfile();
		manager = (CalendarManager)WT.getServiceManager(SERVICE_ID, up.getId());
		ss = new CalendarServiceSettings(SERVICE_ID, up.getDomainId());
		us = new CalendarUserSettings(SERVICE_ID, up.getId());
		initFolders();
	}
	
	@Override
	public void cleanup() throws Exception {
		checkedFolders.clear();
		checkedFolders = null;
		checkedRoots.clear();
		checkedRoots = null;
		folders.clear();
		foldersByRoot.clear();
		roots.clear();
		us = null;
		ss = null;
		manager = null;
	}
	
	@Override
	public ServiceVars returnServiceVars() {
		DateTimeFormatter hmf = DateTimeUtils.createHmFormatter();
		ServiceVars co = new ServiceVars();
		co.put("defaultCalendarSync", ss.getDefaultCalendarSync().toString());
		co.put("view", us.getView());
		co.put("workdayStart", hmf.print(us.getWorkdayStart()));
		co.put("workdayEnd", hmf.print(us.getWorkdayEnd()));
		return co;
	}
	
	private void initFolders() throws Exception {
		UserProfileId pid = getEnv().getProfile().getId();
		synchronized(roots) {
			updateRootFoldersCache();
			updateFoldersCache();

			checkedRoots = us.getCheckedCalendarRoots();
			// If empty, adds MyNode checked by default!
			if(checkedRoots.isEmpty()) {
				checkedRoots.add(MyCalendarRoot.SHARE_ID);
				us.setCheckedCalendarRoots(checkedRoots);
			}
			checkedFolders = us.getCheckedCalendarFolders();
		}
	}
	
	private void updateRootFoldersCache() throws WTException {
		UserProfileId pid = getEnv().getProfile().getId();
		synchronized(roots) {
			roots.clear();
			roots.put(MyCalendarRoot.SHARE_ID, new MyCalendarRoot(pid));
			for(CalendarRoot root : manager.listIncomingCalendarRoots()) {
				roots.put(root.getShareId(), root);
			}
		}
	}
	
	private void updateFoldersCache() throws WTException {
		synchronized(roots) {
			foldersByRoot.clear();
			folders.clear();
			for(CalendarRoot root : roots.values()) {
				foldersByRoot.put(root.getShareId(), new ArrayList<CalendarFolder>());
				if(root instanceof MyCalendarRoot) {
					for(Calendar cal : manager.listCalendars()) {
						MyCalendarFolder fold = new MyCalendarFolder(root.getShareId(), cal);
						foldersByRoot.get(root.getShareId()).add(fold);
						folders.put(cal.getCalendarId(), fold);
					}
				} else {
					for(CalendarFolder fold : manager.listIncomingCalendarFolders(root.getShareId()).values()) {
						foldersByRoot.get(root.getShareId()).add(fold);
						folders.put(fold.getCalendar().getCalendarId(), fold);
					}
				}
			}
		}
	}
	
	public void processManageFoldersTree(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		ArrayList<ExtTreeNode> children = new ArrayList<>();
				
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String node = ServletUtils.getStringParameter(request, "node", true);
				
				if(node.equals("root")) { // Node: root -> list roots
					for(CalendarRoot root : roots.values()) {
						children.add(createRootNode(root));
					}
				} else { // Node: folder -> list folders (calendars)
					CalendarRoot root = roots.get(node);
					
					if(root instanceof MyCalendarRoot) {
						for(Calendar cal : manager.listCalendars()) {
							MyCalendarFolder folder = new MyCalendarFolder(node, cal);
							children.add(createFolderNode(folder, root.getPerms()));
						}
					} else {
						/*
						HashMap<Integer, CalendarFolder> folds = manager.listIncomingCalendarFolders(root.getShareId());
						for(CalendarFolder fold : folds.values()) {
							children.add(createFolderNode(fold, root.getPerms()));
						}
						*/
						if(foldersByRoot.containsKey(root.getShareId())) {
							for(CalendarFolder fold : foldersByRoot.get(root.getShareId())) {
								children.add(createFolderNode(fold, root.getPerms()));
							}
						}
					}
				}
				new JsonResult("children", children).printTo(out);
				
			} else if(crud.equals(Crud.UPDATE)) {
				PayloadAsList<JsFolderNodeList> pl = ServletUtils.getPayloadAsList(request, JsFolderNodeList.class);
				
				for(JsFolderNode node : pl.data) {
					if(node._type.equals(JsFolderNode.TYPE_ROOT)) {
						toggleCheckedRoot(node.id, node._visible);
						
					} else if(node._type.equals(JsFolderNode.TYPE_FOLDER)) {
						CompositeId cid = new CompositeId().parse(node.id);
						toggleCheckedFolder(Integer.valueOf(cid.getToken(1)), node._visible);
					}
				}
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.DELETE)) {
				PayloadAsList<JsFolderNodeList> pl = ServletUtils.getPayloadAsList(request, JsFolderNodeList.class);
				
				for(JsFolderNode node : pl.data) {
					if(node._type.equals(JsFolderNode.TYPE_FOLDER)) {
						CompositeId cid = new CompositeId().parse(node.id);
						manager.deleteCalendar(Integer.valueOf(cid.getToken(1)));
					}
				}
				new JsonResult().printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error in action ManageCalendarsTree", ex);
		}
	}
	
	public void processLookupCalendarRoots(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		List<JsSimple> items = new ArrayList<>();
		
		try {
			Boolean writableOnly = ServletUtils.getBooleanParameter(request, "writableOnly", true);
			
			for(CalendarRoot root : roots.values()) {
				if(root instanceof MyCalendarRoot) {
					UserProfile up = getEnv().getProfile();
					items.add(new JsSimple(up.getStringId(), up.getDisplayName()));
				} else {
					//TODO: se writableOnly verificare che il gruppo condiviso sia scrivibile
					items.add(new JsSimple(root.getOwnerProfileId().toString(), root.getDescription()));
				}
			}
			
			new JsonResult("roots", items, items.size()).printTo(out);
			
		} catch(Exception ex) {
			logger.error("Error in action LookupCalendarRoots", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	public void processLookupCalendarFolders(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		List<JsCalendarLkp> items = new ArrayList<>();
		
		try {
			for(CalendarRoot root : roots.values()) {
				if(root instanceof MyCalendarRoot) {
					for(Calendar cal : manager.listCalendars()) {
						items.add(new JsCalendarLkp(cal));
					}
				} else {
					/*
					HashMap<Integer, CalendarFolder> folds = manager.listIncomingCalendarFolders(root.getShareId());
					for(CalendarFolder fold : folds.values()) {
						if(!fold.getElementsPerms().implies("CREATE")) continue;
						items.add(new JsCalendarLkp(fold.getCalendar()));
					}
					*/
					if(foldersByRoot.containsKey(root.getShareId())) {
						for(CalendarFolder fold : foldersByRoot.get(root.getShareId())) {
							if(!fold.getElementsPerms().implies("CREATE")) continue;
							items.add(new JsCalendarLkp(fold.getCalendar()));
						}
					}
				}
			}
			new JsonResult("folders", items, items.size()).printTo(out);
			
		} catch(Exception ex) {
			logger.error("Error in action LookupCalendarFolders", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	public void processManageSharing(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String id = ServletUtils.getStringParameter(request, "id", true);
				
				Sharing sharing = manager.getSharing(id);
				String description = buildSharingPath(sharing);
				new JsonResult(new JsSharing(sharing, description)).printTo(out);
				
			} else if(crud.equals(Crud.UPDATE)) {
				Payload<MapItem, Sharing> pl = ServletUtils.getPayload(request, Sharing.class);
				
				manager.updateSharing(pl.data);
				new JsonResult().printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error in action ManageSharing", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	public void processManageCalendars(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		Calendar item = null;
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				Integer id = ServletUtils.getIntParameter(request, "id", true);
				
				item = manager.getCalendar(id);
				new JsonResult(new JsCalendar(item)).printTo(out);
				
			} else if(crud.equals(Crud.CREATE)) {
				Payload<MapItem, JsCalendar> pl = ServletUtils.getPayload(request, JsCalendar.class);
				
				item = manager.addCalendar(JsCalendar.buildFolder(pl.data));
				updateFoldersCache();
				toggleCheckedFolder(item.getCalendarId(), true);
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.UPDATE)) {
				Payload<MapItem, JsCalendar> pl = ServletUtils.getPayload(request, JsCalendar.class);
				
				manager.updateCalendar(JsCalendar.buildFolder(pl.data));
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.DELETE)) {
				Payload<MapItem, JsCalendar> pl = ServletUtils.getPayload(request, JsCalendar.class);
				
				manager.deleteCalendar(pl.data.calendarId);
				updateFoldersCache();
				new JsonResult().printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error in action ManageCalendars", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	public void processGetSchedulerDates(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		ArrayList<JsSchedulerEventDate> items = new ArrayList<>();
		
		try {
			UserProfile up = getEnv().getProfile();
			DateTimeZone utz = up.getTimeZone();
			DateTimeFormatter ymdZoneFmt = DateTimeUtils.createYmdFormatter(utz);
			
			// Defines boundaries
			String start = ServletUtils.getStringParameter(request, "startDate", true);
			String end = ServletUtils.getStringParameter(request, "endDate", true);
			DateTime fromDate = DateTimeUtils.parseYmdHmsWithZone(start, "00:00:00", up.getTimeZone());
			DateTime toDate = DateTimeUtils.parseYmdHmsWithZone(end, "23:59:59", up.getTimeZone());
			
			// Get events for each visible group
			Integer[] checked = getCheckedFolders();
			List<DateTime> dates = null;
			for(CalendarRoot root : getCheckedRoots()) {
				dates = manager.listEventsDates(root, checked, fromDate, toDate, utz);
				for(DateTime dt : dates) {
					items.add(new JsSchedulerEventDate(ymdZoneFmt.print(dt)));
				}
			}
			new JsonResult("dates", items).printTo(out);
			
		} catch(Exception ex) {
			logger.error("Error in action GetSchedulerDates", ex);
			new JsonResult(false, "Error").printTo(out);
			
		}
	}
	
	public void processManageEventsScheduler(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		ArrayList<JsSchedulerEvent> items = new ArrayList<>();
		
		try {
			UserProfile up = getEnv().getProfile();
			DateTimeZone utz = up.getTimeZone();
			
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String from = ServletUtils.getStringParameter(request, "startDate", true);
				String to = ServletUtils.getStringParameter(request, "endDate", true);
				
				// Defines view boundary 
				DateTime fromDate = DateTimeUtils.parseYmdHmsWithZone(from, "00:00:00", up.getTimeZone());
				DateTime toDate = DateTimeUtils.parseYmdHmsWithZone(to, "23:59:59", up.getTimeZone());
				
				// Get events for each visible folder
				JsSchedulerEvent jse = null;
				List<SchedulerEventInstance> recInstances = null;
				List<CalendarManager.CalendarEvents> foldEvents = null;
				Integer[] checked = getCheckedFolders();
				for(CalendarRoot root : getCheckedRoots()) {
					foldEvents = manager.listSchedulerEvents(root, checked, fromDate, toDate);
					
					for(CalendarManager.CalendarEvents ce : foldEvents) {
						CalendarFolder fold = folders.get(ce.calendar.getCalendarId());
						if(fold == null) continue;
						
						for(SchedulerEventInstance evt : ce.events) {
							if(evt.getRecurrenceId() == null) {
								jse = new JsSchedulerEvent(ce.calendar, evt, up.getId(), utz);
								jse._rights = fold.getElementsPerms().toString();
								items.add(jse);
							} else {
								recInstances = manager.calculateRecurringInstances(evt, fromDate, toDate, utz);
								for(SchedulerEventInstance recInstance : recInstances) {
									jse = new JsSchedulerEvent(ce.calendar, recInstance, up.getId(), utz);
									jse._rights = fold.getElementsPerms().toString();
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
				DateTime newStart = DateTimeUtils.parseYmdHmsWithZone(pl.data.startDate, etz);
				DateTime newEnd = DateTimeUtils.parseYmdHmsWithZone(pl.data.endDate, etz);
				manager.cloneEventInstance(EventKey.buildKey(pl.data.eventId, pl.data.originalEventId), newStart, newEnd);
				
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.UPDATE)) {
				Payload<MapItem, JsSchedulerEvent.Update> pl = ServletUtils.getPayload(request, JsSchedulerEvent.Update.class);
				
				DateTimeZone etz = DateTimeZone.forID(pl.data.timezone);
				DateTime newStart = DateTimeUtils.parseYmdHmsWithZone(pl.data.startDate, etz);
				DateTime newEnd = DateTimeUtils.parseYmdHmsWithZone(pl.data.endDate, etz);
				manager.updateEventInstance(pl.data.id, newStart, newEnd, pl.data.title);
				
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.DELETE)) {
				String uid = ServletUtils.getStringParameter(request, "id", true);
				String target = ServletUtils.getStringParameter(request, "target", "this");
				
				manager.deleteEventInstance(target, uid);
				new JsonResult().printTo(out);
				
			} else if(crud.equals("restore")) {
				String uid = ServletUtils.getStringParameter(request, "id", true);
				
				manager.restoreEventInstance(uid);
				new JsonResult().printTo(out);
			} else if(crud.equals("search")) {
				String query = ServletUtils.getStringParameter(request, "query", true);
				
				List<CalendarManager.CalendarEvents> calEvts = null;
				Integer[] checked = getCheckedFolders();
				for(CalendarRoot root : getCheckedRoots()) {
					calEvts = manager.searchSchedulerEvents(root.getOwnerProfileId(), checked, "%"+query+"%");
					// Iterates over calendar->events
					for(CalendarManager.CalendarEvents ce : calEvts) {
						for(SchedulerEventInstance evt : ce.events) {
							if(evt.getRecurrenceId() == null) {
								items.add(new JsSchedulerEvent(ce.calendar, evt, up.getId(), utz));
							}
						}
					}
				}
				new JsonResult("events", items).printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error in action ManageEventsScheduler", ex);
			new JsonResult(false, "Error").printTo(out);	
		}
	}
	
	public void processManageEvents(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		JsEvent item = null;
		
		try {
			UserProfile up = getEnv().getProfile();
			
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String eventKey = ServletUtils.getStringParameter(request, "id", true);
				
				EventInstance evt = manager.getEventInstance(eventKey);
				UserProfileId ownerId = manager.getCalendarOwner(evt.getCalendarId());
				item = new JsEvent(evt, ownerId.toString());
				new JsonResult(item).printTo(out);
				
			} else if(crud.equals(Crud.CREATE)) {
				Payload<MapItem, JsEvent> pl = ServletUtils.getPayload(request, JsEvent.class);
				
				//TODO: verificare che il calendario supporti la scrittura (specialmente per quelli condivisi)
				
				EventInstance evt = JsEvent.buildEventInstance(pl.data);
				CoreManager core = WT.getCoreManager();
				evt.setOrganizer(core.getUserData().getFullEmailAddress());
				
				manager.addEvent(evt);
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.UPDATE)) {
				String target = ServletUtils.getStringParameter(request, "target", "this");
				Payload<MapItem, JsEvent> pl = ServletUtils.getPayload(request, JsEvent.class);
				
				EventInstance evt = JsEvent.buildEventInstance(pl.data);
				manager.updateEventInstance(target, evt);
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.MOVE)) {
				String id = ServletUtils.getStringParameter(request, "id", true);
				Integer calendarId = ServletUtils.getIntParameter(request, "targetCalendarId", true);
				boolean copy = ServletUtils.getBooleanParameter(request, "copy", false);
				
				manager.moveEventInstance(copy, id, calendarId);
				new JsonResult().printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error in action ManageEvents", ex);
			new JsonResult(false, "Error").printTo(out);	
		}
	}
	
	public void processManageGridEvents(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		ArrayList<JsSchedulerEvent> items = new ArrayList<>();
		
		try {
			UserProfile up = getEnv().getProfile();
			DateTimeZone utz = up.getTimeZone();
			
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String query = ServletUtils.getStringParameter(request, "query", true);
				
				List<CalendarManager.CalendarEvents> calEvts = null;
				Integer[] checked = getCheckedFolders();
				for(CalendarRoot root : getCheckedRoots()) {
					calEvts = manager.searchSchedulerEvents(root.getOwnerProfileId(), checked, "%"+query+"%");
					// Iterates over calendar->events
					for(CalendarManager.CalendarEvents ce : calEvts) {
						for(SchedulerEventInstance evt : ce.events) {
							if(evt.getRecurrenceId() == null) {
								items.add(new JsSchedulerEvent(ce.calendar, evt, up.getId(), utz));
							}
						}
					}
				}
				new JsonResult("events", items).printTo(out);
			}
		
		} catch(Exception ex) {
			logger.error("Error in action ManageGridEvents", ex);
			new JsonResult(false, "Error").printTo(out);
			
		}
	}
	
	public void processExportForErp(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		UserProfile up = getEnv().getProfile();
		
		try {
			String op = ServletUtils.getStringParameter(request, "op", true);
			if (op.equals("do")) {
				String fromDate = ServletUtils.getStringParameter(request, "fromDate", true);
				String toDate = ServletUtils.getStringParameter(request, "toDate", true);
				
				DateTimeFormatter ymd = DateTimeUtils.createYmdFormatter(up.getTimeZone());
				erpWizard = new ErpExportWizard();
				erpWizard.fromDate = ymd.parseDateTime(fromDate).withTimeAtStartOfDay();
				erpWizard.toDate = DateTimeUtils.withTimeAtEndOfDay(ymd.parseDateTime(toDate));
				
				LogEntries log = new LogEntries();
				File file = WT.createTempFile();
				
				try {
					DateTimeFormatter ymd2 = DateTimeUtils.createFormatter("yyyyMMdd", up.getTimeZone());
					DateTimeFormatter ymdhms = DateTimeUtils.createFormatter("yyyy-MM-dd HH:mm:ss", up.getTimeZone());
					
					try (FileOutputStream fos = new FileOutputStream(file)) {
						log.addMaster(new MessageLogEntry(LogEntry.LEVEL_INFO, "Started on {0}", ymdhms.print(new DateTime())));
						manager.exportEvents(log, up.getDomainId(), erpWizard.fromDate, erpWizard.toDate, fos);
						log.addMaster(new MessageLogEntry(LogEntry.LEVEL_INFO, "Ended on {0}", ymdhms.print(new DateTime())));
						erpWizard.file = file;
						erpWizard.filename = MessageFormat.format(ERP_EXPORT_FILENAME, up.getDomainId(), ymd2.print(erpWizard.fromDate), ymd2.print(erpWizard.fromDate), "csv");
						log.addMaster(new MessageLogEntry(LogEntry.LEVEL_INFO, "File ready: {0}", erpWizard.filename));
						log.addMaster(new MessageLogEntry(LogEntry.LEVEL_INFO, "Operation completed succesfully"));
						new JsonResult(new JsWizardData(log.print())).printTo(out);
					}
				} catch(Throwable t) {
					logger.error("Error generating export", t);
					file.delete();
					new JsonResult(new JsWizardData(log.print())).setSuccess(false).printTo(out);
				}	
			}
			
		} catch(Exception ex) {
			logger.error("Error in ExportForErp", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
	
	public void processExportForErp(HttpServletRequest request, HttpServletResponse response) {
		try {
			try(FileInputStream fis = new FileInputStream(erpWizard.file)) {
				OutputStream os = response.getOutputStream();
				ServletUtils.setFileStreamHeaders(response, "application/octet-stream", DispositionType.ATTACHMENT, erpWizard.filename);
				ServletUtils.setContentLengthHeader(response, erpWizard.file.length());
				IOUtils.copy(fis, os);
			}
			
		} catch(Exception ex) {
			logger.error("Error in ExportForErp", ex);
		} finally {
			erpWizard = null;
		}
	}
	
	public void processGetPlanning(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		CoreUserSettings cus = getEnv().getCoreUserSettings();
		CoreManager core = WT.getCoreManager();
		ArrayList<MapItem> items = new ArrayList<>();
		Connection con = null;
		
		try {
			String eventStartDate = ServletUtils.getStringParameter(request, "startDate", true);
			String eventEndDate = ServletUtils.getStringParameter(request, "endDate", true);
			String timezone = ServletUtils.getStringParameter(request, "timezone", true);
			JsAttendeeList attendees = ServletUtils.getObjectParameter(request, "attendees", new JsAttendeeList(), JsAttendeeList.class);
			
			// Parses string parameters
			DateTimeZone eventTz = DateTimeZone.forID(timezone);
			DateTime eventStartDt = DateTimeUtils.parseYmdHmsWithZone(eventStartDate, eventTz);
			DateTime eventEndDt = DateTimeUtils.parseYmdHmsWithZone(eventEndDate, eventTz);
			
			UserProfile up = getEnv().getProfile();
			DateTimeZone profileTz = up.getTimeZone();
			
			LocalTime localStartTime = eventStartDt.toLocalTime();
			LocalTime localEndTime = eventEndDt.toLocalTime();
			LocalTime fromTime = DateTimeUtils.min(localStartTime, us.getWorkdayStart());
			LocalTime toTime = DateTimeUtils.max(localEndTime, us.getWorkdayEnd());
			
			// Defines useful date/time formatters
			DateTimeFormatter ymdhmFmt = DateTimeUtils.createYmdHmFormatter();
			DateTimeFormatter tFmt = DateTimeUtils.createFormatter(cus.getShortTimeFormat());
			DateTimeFormatter dFmt = DateTimeUtils.createFormatter(cus.getShortDateFormat());
			
			ArrayList<String> hours = manager.generateTimeSpans(60, eventStartDt.toLocalDate(), eventEndDt.toLocalDate(), us.getWorkdayStart(), us.getWorkdayEnd(), profileTz);
			
			// Generates fields and columnsInfo dynamically
			ArrayList<FieldMeta> fields = new ArrayList<>();
			ArrayList<GridColumnMeta> colsInfo = new ArrayList<>();
			
			GridColumnMeta col = null;
			fields.add(new FieldMeta("recipient"));
			colsInfo.add(new GridColumnMeta("recipient"));
			for(String hourKey : hours) {
				LocalDateTime ldt = ymdhmFmt.parseLocalDateTime(hourKey);
				fields.add(new FieldMeta(hourKey));
				col = new GridColumnMeta(hourKey, tFmt.print(ldt));
				col.put("date", dFmt.print(ldt));
				col.put("overlaps", DateTimeUtils.between(ldt, eventStartDt.toLocalDateTime(), eventEndDt.toLocalDateTime()));
				colsInfo.add(col);
			}
			
			// Collects attendees availability...
			OUser user = null;
			UserProfileId profileId = null;
			LinkedHashSet<String> busyHours = null;
			MapItem item = null;
			for(JsAttendee attendee : attendees) {
				item = new MapItem();
				item.put("recipient", attendee.recipient);
				
				user = guessUserByAttendee(core, attendee.recipient);
				if (user != null) {
					profileId = new UserProfileId(user.getDomainId(), user.getUserId());
					busyHours = manager.calculateAvailabilitySpans(60, profileId, eventStartDt.withTime(fromTime), eventEndDt.withTime(toTime), eventTz, true);
					for(String hourKey : hours) {
						item.put(hourKey, busyHours.contains(hourKey) ? "busy" : "free");
					}
				} else {
					for(String hourKey : hours) {
						item.put(hourKey, "unknown");
					}
				}
				items.add(item);
			}
			
			GridMetadata meta = new GridMetadata(true);
			meta.setFields(fields);
			meta.setColumnsInfo(colsInfo);
			new JsonResult(items, meta, items.size()).printTo(out);
			
		} catch(Exception ex) {
			logger.error("Error in action ManageEvents", ex);
			new JsonResult(false, "Error").printTo(out);
			
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void processImportEventsFromICal(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		UserProfile up = getEnv().getProfile();
		
		try {
			String uploadId = ServletUtils.getStringParameter(request, "uploadId", true);
			String op = ServletUtils.getStringParameter(request, "op", true);
			
			UploadedFile upl = getUploadedFile(uploadId);
			if(upl == null) throw new WTException("Uploaded file not found [{0}]", uploadId);
			File file = new File(WT.getTempFolder(), upl.getUploadId());
			
			EventICalFileReader rea = new EventICalFileReader(up.getTimeZone());
			
			if(op.equals("do")) {
				Integer calendarId = ServletUtils.getIntParameter(request, "calendarId", true);
				String mode = ServletUtils.getStringParameter(request, "importMode", true);
				
				LogEntries log = manager.importEvents(calendarId, rea, file, mode);
				removeUploadedFile(uploadId);
				new JsonResult(new JsWizardData(log.print())).printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error in action ImportContactsFromICal", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
	
	public void processPrintScheduler(HttpServletRequest request, HttpServletResponse response) {
		ByteArrayOutputStream baos = null;
		UserProfile up = getEnv().getProfile();
		
		try {
			String filename = ServletUtils.getStringParameter(request, "filename", "print");
			String view = ServletUtils.getStringParameter(request, "view", "w5");
			String from = ServletUtils.getStringParameter(request, "startDate", true);
			
			DateTime startDate = DateTimeUtils.parseYmdHmsWithZone(from, "00:00:00", up.getTimeZone());
			
			ReportConfig.Builder builder = reportConfigBuilder();
			DateTime fromDate = null, toDate = null;
			AbstractAgenda rpt = null;
			if(view.equals("d")) {
				fromDate = startDate.withTimeAtStartOfDay();
				toDate = startDate.plusDays(1).withTimeAtStartOfDay();
				rpt = new RptAgendaSummary(builder.build(), 1);
				
			} else if(view.equals("w5")) {
				fromDate = startDate.withTimeAtStartOfDay();
				toDate = startDate.plusDays(5).withTimeAtStartOfDay();
				rpt = new RptAgendaWeek5(builder.build());
				
			} else if(view.equals("w")) {
				fromDate = startDate.withTimeAtStartOfDay();
				toDate = startDate.plusDays(7).withTimeAtStartOfDay();
				rpt = new RptAgendaWeek7(builder.build());
				
			} else if(view.equals("dw")) {
				fromDate = startDate.withTimeAtStartOfDay();
				toDate = startDate.plusDays(14).withTimeAtStartOfDay();
				rpt = new RptAgendaSummary(builder.build(), 14);
				
			} else if(view.equals("m")) {
				if(startDate.getDayOfMonth() == 1) {
					fromDate = startDate.withTimeAtStartOfDay();
				} else {
					fromDate = startDate.plusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay();
				}
				int days = fromDate.dayOfMonth().getMaximumValue();
				toDate = fromDate.plusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay();
				rpt = new RptAgendaSummary(builder.build(), days);
				
			} else {
				throw new WTException("View not supported [{0}]", view);
			}
			
			// Get events for each visible folder
			List<CalendarManager.CalendarEvents> foldEvents = new ArrayList<>();
			Integer[] checked = getCheckedFolders();
			for(CalendarRoot root : getCheckedRoots()) {
				foldEvents.addAll(manager.listSchedulerEvents(root, checked, fromDate, toDate));
			}
			rpt.setDataSource(manager, fromDate, toDate, up.getTimeZone(), foldEvents);
			
			baos = new ByteArrayOutputStream();
			WT.generateReportToStream(rpt, AbstractReport.OutputType.PDF, baos);
			ServletUtils.setContentDispositionHeader(response, "inline", filename + ".pdf");
			ServletUtils.writeContent(response, baos, "application/pdf");
			
		} catch(Exception ex) {
			logger.error("Error in action PrintScheduler", ex);
			ServletUtils.writeErrorHandlingJs(response, ex.getMessage());
		} finally {
			IOUtils.closeQuietly(baos);
		}
	}
	
	public void processPrintEventsDetail(HttpServletRequest request, HttpServletResponse response) {
		ArrayList<RBEventDetail> items = new ArrayList<>();
		ByteArrayOutputStream baos = null;
		CoreManager core = WT.getCoreManager();
		UserProfile up = getEnv().getProfile();
		
		try {
			String filename = ServletUtils.getStringParameter(request, "filename", "print");
			StringArray keys = ServletUtils.getObjectParameter(request, "keys", StringArray.class, true);
			RRuleStringify rrs = new RRuleStringify();
			rrs.setTimeZone(up.getTimeZone());
			//rrs.setDateFormat();
			//rrs.setTimeFormat();
			
			EventInstance event = null;
			Calendar calendar = null;
			for(String key : keys) {
				event = manager.getEventInstance(key);
				calendar = manager.getCalendar(event.getCalendarId());
				items.add(new RBEventDetail(core, rrs, calendar, event));
			}
			
			ReportConfig.Builder builder = reportConfigBuilder();
			RptEventsDetail rpt = new RptEventsDetail(builder.build());
			rpt.setDataSource(items);
			
			baos = new ByteArrayOutputStream();
			WT.generateReportToStream(rpt, AbstractReport.OutputType.PDF, baos);
			ServletUtils.setContentDispositionHeader(response, "inline", filename + ".pdf");
			ServletUtils.writeContent(response, baos, "application/pdf");
			
		} catch(Exception ex) {
			logger.error("Error in action PrintContacts", ex);
			ServletUtils.writeErrorHandlingJs(response, ex.getMessage());
		} finally {
			IOUtils.closeQuietly(baos);
		}
	}
	
	private ReportConfig.Builder reportConfigBuilder() {
		UserProfile.Data ud = getEnv().getProfile().getData();
		CoreUserSettings cus = getEnv().getCoreUserSettings();
		return new ReportConfig.Builder()
				.useLocale(ud.getLocale())
				.useTimeZone(ud.getTimeZone().toTimeZone())
				.dateFormatShort(cus.getShortDateFormat())
				.dateFormatLong(cus.getLongDateFormat())
				.timeFormatShort(cus.getShortTimeFormat())
				.timeFormatLong(cus.getLongTimeFormat())
				.generatedBy(WT.getPlatformName() + " " + lookupResource(CalendarLocale.SERVICE_NAME))
				.printedBy(ud.getDisplayName());
	}
	
	private OUser guessUserByAttendee(CoreManager core, String recipient) {
		Connection con = null;
		
		try {
			InternetAddress ia = MailUtils.buildInternetAddress(recipient);
			if (ia == null) return null;
			List<UserProfileId> pids = core.listUserIdsByEmail(recipient);
			if (pids.isEmpty()) return null;
			UserProfileId pid = pids.get(0);
			
			con = WT.getCoreConnection();
			UserDAO udao = UserDAO.getInstance();
			return udao.selectByDomainUser(con, pid.getDomainId(), pid.getUserId());
		
		} catch(WTRuntimeException ex) {
			return null;
		} catch(Exception ex) {
			logger.error("Error guessing user from attendee", ex);
			return null;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	private String buildSharingPath(Sharing sharing) throws WTException {
		StringBuilder sb = new StringBuilder();
		
		// Root description part
		CompositeId cid = new CompositeId().parse(sharing.getId());
		if(roots.containsKey(cid.getToken(0))) {
			CalendarRoot root = roots.get(cid.getToken(0));
			if(root instanceof MyCalendarRoot) {
				sb.append(lookupResource(CalendarLocale.CALENDARS_MY));
			} else {
				sb.append(root.getDescription());
			}
		}
		
		// Folder description part
		if(sharing.getLevel() == 1) {
			int calId = Integer.valueOf(cid.getToken(1));
			Calendar calendar = manager.getCalendar(calId);
			sb.append("/");
			sb.append((calendar != null) ? calendar.getName() : cid.getToken(1));
		}
		
		return sb.toString();
	}
	
	private List<CalendarRoot> getCheckedRoots() {
		ArrayList<CalendarRoot> checked = new ArrayList<>();
		for(CalendarRoot root : roots.values()) {
			if(!checkedRoots.contains(root.getShareId())) continue; // Skip folder if not visible
			checked.add(root);
		}
		return checked;
	}
	
	private Integer[] getCheckedFolders() {
		return checkedFolders.toArray(new Integer[checkedFolders.size()]);
	}
	
	private void toggleCheckedRoot(String shareId, boolean checked) {
		synchronized(roots) {
			if(checked) {
				checkedRoots.add(shareId);
			} else {
				checkedRoots.remove(shareId);
			}
			us.setCheckedCalendarRoots(checkedRoots);
		}
	}
	
	private void toggleCheckedFolder(int folderId, boolean checked) {
		synchronized(roots) {
			if(checked) {
				checkedFolders.add(folderId);
			} else {
				checkedFolders.remove(folderId);
			}
			us.setCheckedCalendarFolders(checkedFolders);
		}
	}
	
	private ExtTreeNode createRootNode(CalendarRoot root) {
		if(root instanceof MyCalendarRoot) {
			return createRootNode(root.getShareId(), root.getOwnerProfileId().toString(), root.getPerms().toString(), lookupResource(CalendarLocale.CALENDARS_MY), false, "wtcal-icon-root-my-xs").setExpanded(true);
		} else {
			return createRootNode(root.getShareId(), root.getOwnerProfileId().toString(), root.getPerms().toString(), root.getDescription(), false, "wtcal-icon-root-incoming-xs");
		}
	}
	
	private ExtTreeNode createRootNode(String id, String pid, String rights, String text, boolean leaf, String iconClass) {
		boolean visible = checkedRoots.contains(id);
		ExtTreeNode node = new ExtTreeNode(id, text, leaf);
		node.put("_type", JsFolderNode.TYPE_ROOT);
		node.put("_pid", pid);
		node.put("_rrights", rights);
		node.put("_visible", visible);
		node.setIconClass(iconClass);
		node.setChecked(visible);
		return node;
	}
	
	private ExtTreeNode createFolderNode(CalendarFolder folder, SharePermsRoot rootPerms) {
		Calendar cal = folder.getCalendar();
		String id = new CompositeId().setTokens(folder.getShareId(), cal.getCalendarId()).toString();
		boolean visible = checkedFolders.contains(cal.getCalendarId());
		ExtTreeNode node = new ExtTreeNode(id, cal.getName(), true);
		node.put("_type", JsFolderNode.TYPE_FOLDER);
		node.put("_pid", cal.getProfileId().toString());
		node.put("_rrights", rootPerms.toString());
		node.put("_frights", folder.getPerms().toString());
		node.put("_erights", folder.getElementsPerms().toString());
		node.put("_calId", cal.getCalendarId());
		node.put("_builtIn", cal.getBuiltIn());
		node.put("_default", cal.getIsDefault());
		node.put("_color", cal.getColor());
		node.put("_visible", visible);
		node.put("_isPrivate", cal.getIsPrivate());
		node.put("_busy", cal.getDefaultBusy());
		node.put("_reminder", cal.getDefaultReminder());
		
		List<String> classes = new ArrayList<>();
		if(cal.getIsDefault()) classes.add("wtcal-tree-default");
		if(!folder.getElementsPerms().implies("CREATE") 
				&& !folder.getElementsPerms().implies("UPDATE")
				&& !folder.getElementsPerms().implies("DELETE")) classes.add("wtcal-tree-readonly");
		node.setCls(StringUtils.join(classes, " "));
		
		node.setIconClass("wt-palette-" + cal.getHexColor());
		node.setChecked(visible);
		return node;
	}
	
	private static class ErpExportWizard {
		public DateTime fromDate;
		public DateTime toDate;
		public File file;
		public String filename;
	}
}
