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
package com.sonicle.webtop.calendar.bol.js;

import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.webtop.calendar.bol.model.SchedulerEventInstance;
import com.sonicle.webtop.calendar.model.Calendar;
import com.sonicle.webtop.core.sdk.UserProfileId;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author malbinola
 */
public class JsSchedulerEvent {
	public String id;
	public Integer eventId;
	public Integer originalEventId;
	public Integer calendarId;
	public String startDate;
	public String endDate;
	public String timezone;
	public Boolean isAllDay;
	public String title;
	public String color;
	public String location;
	public Boolean isPrivate;
	public Integer reminder;
	public Boolean isReadOnly;
	public Boolean hasTz;
	public Boolean hasAtts;
	public Boolean isRecurring;
	public Boolean isBroken;
	public Boolean hasCmts;
	public String folderName;
	public String _rights;
	public String _profileId;
	
	public JsSchedulerEvent() {}
	
	public JsSchedulerEvent(Calendar calendar, SchedulerEventInstance event, UserProfileId profileId, DateTimeZone profileTz) {
		DateTimeFormatter ymdhmsZoneFmt = DateTimeUtils.createYmdHmsFormatter(profileTz);
		
		// Determine if keep event data private
		boolean keepDataPrivate = false;
		if(event.getIsPrivate()) {
			UserProfileId ownerProfileId = new UserProfileId(calendar.getDomainId(), calendar.getUserId());
			if(!ownerProfileId.equals(profileId)) {
				keepDataPrivate = true;
			}
		}
		
		id = event.getKey();
		eventId = event.getEventId();
		originalEventId = event.getEventId();
		calendarId = event.getCalendarId();
		
		// Source field is already in UTC, we need only to display it
		// in the timezone choosen by user in his settings.
		// Formatter will be instantiated specifying desired timezone.
		startDate = ymdhmsZoneFmt.print(event.getStartDate());
		endDate = ymdhmsZoneFmt.print(event.getEndDate());
		timezone = event.getTimezone();
		isAllDay = event.getAllDay();
		
		title = (keepDataPrivate) ? "***" : event.getTitle();
		color = calendar.getColor();
		location = (keepDataPrivate) ? "" : event.getLocation();
		isPrivate = event.getIsPrivate();
		reminder = (event.getReminder() == null) ? -1 : event.getReminder();
		//TODO: gestire eventi readonly...(utenti admin devono poter editare)
		isReadOnly = event.getReadOnly() || keepDataPrivate;
		hasTz = !event.getDateTimeZone().getID().equals(profileTz.getID()) && !DateTimeUtils.isTimeZoneCompatible(event.getDateTimeZone(), profileTz, event.getStartDate());
		hasAtts = event.getHasAttendees();
		isRecurring = event.getIsRecurring();
		isBroken = event.getIsBroken();
		hasCmts = !StringUtils.isBlank(event.getDescription());
		
		folderName = calendar.getName();
		_profileId = new UserProfileId(calendar.getDomainId(), calendar.getUserId()).toString();
	}
	
	public static class Update {
		public String id;
		public String startDate;
		public String endDate;
		public String timezone;
		public String title;
	}
}
