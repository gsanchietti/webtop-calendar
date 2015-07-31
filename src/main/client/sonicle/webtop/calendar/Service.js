/*
 * webtop-mail is a WebTop Service developed by Sonicle S.r.l.
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
Ext.define('Sonicle.webtop.calendar.Service', {
	extend: 'WT.sdk.Service',
	requires: [
		//'Sonicle.webtop.calendar.Tool',
		'Sonicle.calendar.Panel',
		'Sonicle.MultiCalendar',
		'Sonicle.webtop.calendar.model.FolderNode',
		'Sonicle.webtop.calendar.model.MultiCalDate',
		'Sonicle.webtop.calendar.model.Event',
		'Sonicle.webtop.calendar.model.GridEvent',
		'Sonicle.calendar.data.Events',
		'Sonicle.calendar.data.MemoryCalendarStore', //TODO: rimuovere dopo aver elminato la dipendenza inutile nel componente calendar
		'Sonicle.calendar.data.Calendars', //TODO: rimuovere dopo aver elminato la dipendenza inutile nel componente calendar
		'Sonicle.calendar.data.MemoryEventStore',
		'Sonicle.webtop.calendar.view.Calendar'
	],
	
	init: function() {
		var me = this;
		//TODO: trovare una collocazione a questa chiamata
		Sonicle.upload.Uploader.registerMimeType('text/calendar', ['ical','ics','icalendar']);
		
		me.initActions();
		me.initCxm();
		
		me.on('activate', me.onActivate, me);
		me.onMessage('notifyReminder', function() {
			WT.info('reminder arrived');
		});
		
		me.setToolbar(Ext.create({
			xtype: 'toolbar',
			items: [
				'-',
				me.getAction('today'),
				me.getAction('previousday'),
				me.getAction('nextday'),
				'-',
				me.getAction('dayview'),
				me.getAction('week5view'),
				me.getAction('weekview'),
				me.getAction('dweekview'),
				me.getAction('monthview'),
				'->',
				Ext.create({
					xtype: 'textfield',
					width: 200,
					triggers: {
						search: {
							cls: Ext.baseCSSPrefix + 'form-search-trigger',
							handler: function(s) {
								e.searchEvents(s.getValue());
							}
						}
					},
					listeners: {
						specialkey: function(s, e) {
							if(e.getKey() === e.ENTER) {
								me.searchEvents(s.getValue());
							}
						}
					}
				}),
				{
					xtype: 'button',
					iconCls: 'wt-menu-tools',
					tooltip: WT.res('menu.tools.lbl'),
					menu: [
						me.getAction('erpExport')
					]
				}
			]
		}));
		
		me.setToolComponent(Ext.create({
			xtype: 'panel',
			layout: 'border',
			title: me.getName(),
			items: [{
					region: 'north',
					xtype: 'panel',
					layout: 'center',
					header: false,
					border: false,
					height: 305,
					items: [
						me.addRef('multical', Ext.create({
							xtype: 'somulticalendar',
							border: true,
							startDay: WT.getStartDay(),
							highlightMode: me.getOption('view'),
							width: 184, //TODO: valutare un sistema di misura affidabile
							height: 298,
							store: {
								model: 'Sonicle.webtop.calendar.model.MultiCalDate',
								proxy: WTF.proxy(me.ID, 'GetSchedulerDates', 'dates')
							},
							listeners: {
								change: function(s, nv) {
									me.getRef('scheduler').setStartDate(nv);
								}
							}
						}))
					]
				},
				me.addRef('folderstree', Ext.create({
					region: 'center',
					xtype: 'treepanel',
					border: false,
					useArrows: true,
					rootVisible: false,
					store: {
						autoLoad: true,
						autoSync: true,
						model: 'Sonicle.webtop.calendar.model.FolderNode',
						proxy: WTF.apiProxy(me.ID, 'ManageFoldersTree', 'children', {
							writer: {
								allowSingle: false // Make update/delete using array payload
							}
						}),
						root: {
							id: 'root',
							expanded: true
						},
						listeners: {
							write: function(s,op) {
								me.refreshEvents();
							}
						}
					},
					hideHeaders: true,
					listeners: {
						checkchange: function(n, ck) {
							me._showHideCal(n, ck);
						},
						itemcontextmenu: function(s, rec, itm, i, e) {
							if(rec.get('_type') === 'root') {
								WT.showContextMenu(e, me.getRef('cxmRootFolder'), {folder: rec});
							} else {
								WT.showContextMenu(e, me.getRef('cxmFolder'), {folder: rec});
							}
						}
					}
				}))
			]
		}));
		
		me.setMainComponent(Ext.create({
			xtype: 'container',
			layout: 'card',
			activeItem: 0,
			items: [
				me.addRef('scheduler', Ext.create({
					xtype: 'calendarpanel',
					itemId: 'scheduler',
					activeView: me.getOption('view'),
					startDay: WT.getStartDay(),
					use24HourTime: WT.getUse24HourTime(),
					timezone: WT.getTimezone(),
					viewCfg: {
						timezoneIconCls: 'fa fa-globe',
						privateIconCls: 'fa fa-lock',
						reminderIconCls: 'fa fa-bell-o',
						recurrenceIconCls: 'fa fa-refresh',
						recurrenceBrokenIconCls: 'fa fa-chain-broken',
						todayText: me.res('socal.today'),
						moreText: me.res('socal.more'),
						ddCreateEventText: me.res('socal.ddcreateevent'),
						ddCopyEventText: me.res('socal.ddcopyevent'),
						ddMoveEventText: me.res('socal.ddmoveevent'),
						ddResizeEventText: me.res('socal.ddresizeevent'),
						ddDateFormat: 'j/m',
						scrollStartHour: 7
					},
					monthViewCfg: {
						showHeader: true,
						showWeekLinks: true,
						showWeekNumbers: true
					},
					calendarStore: Ext.create('Sonicle.calendar.data.MemoryCalendarStore', {
						data: Sonicle.calendar.data.Calendars.getData()
					}),
					store: {
						autoSync: true,
						model: 'Sonicle.calendar.data.EventModel',
						proxy: WTF.apiProxy(me.ID, 'ManageEventsScheduler', 'events')
					},
					listeners: {
						rangeselect: function(s,dates,onComplete) {
							onComplete();
							var cal = me.getSelectedFolder();
							if(cal) me.addEvent(cal.get('_rootId'), cal.getId(), cal.get('_isPrivate'), cal.get('_busy'), cal.get('_reminder'), dates.startDate, dates.endDate, false);
						},
						eventdblclick: function(s, rec) {
							if(me.isEventEditable(rec)) me.editEvent(rec);
						},
						daydblclick: function(s, dt, ad) {
							var cal = me.getSelectedFolder(),
									soDate = Sonicle.Date,
									start, end;
							if(cal) {
								if(ad) {
									start = soDate.copyTime(me.getOption('workdayStart'), dt);
									end = soDate.copyTime(me.getOption('workdayEnd'), dt);
								} else {
									start = dt;
									end = soDate.add(dt, {minutes: 30});
								}
								me.addEvent(cal.get('_rootId'), cal.getId(), cal.get('_isPrivate'), cal.get('_busy'), cal.get('_reminder'), start, end, ad);
							}
						},
						eventcontextmenu: function(s, rec, el, evt) {
							WT.showContextMenu(evt, me.getRef('cxmEvent'), {event: rec});
						}
					}
				})),
				me.addRef('results', Ext.create({
					xtype: 'grid',
					itemId: 'results',
					store: {
						model: 'Sonicle.webtop.calendar.model.GridEvent',
						proxy: WTF.apiProxy(me.ID, 'ManageGridEvents', 'events', {
							extraParams: {
								query: null
							}
						})
					},
					columns: [{
						dataIndex: 'id',
						renderer: WTF.iconColRenderer({
							iconField: function(rec) {
								if(rec.get('isBroken')) return 'broken-event';
								if(rec.get('isRecurring')) return 'recurring-event';
								return 'single-event';
							},
							xid: me.XID,
							size: 'xs'
						}),
						header: '',
						width: 40
					}, {
						dataIndex: 'startDate',
						xtype: 'datecolumn',
						format: WT.getShortDateFmt() + ' ' + WT.getShortTimeFmt(),
						header: me.res('event.fld-startDate.lbl'),
						width: 150
					}, {
						dataIndex: 'endDate',
						xtype: 'datecolumn',
						format: WT.getShortDateFmt() + ' ' + WT.getShortTimeFmt(),
						header: me.res('event.fld-endDate.lbl'),
						width: 150
					}, {
						dataIndex: 'title',
						header: me.res('event.fld-title.lbl'),
						flex: 1
					}, {
						dataIndex: 'location',
						header: me.res('event.fld-location.lbl'),
						flex: 1
					}],
					tools: [{
						type: 'close',
						callback: function() {
							me._activateMain('scheduler');
						}
					}],
					listeners: {
						rowdblclick: function(s, rec) {
							if(me.isEventEditable(rec)) me.editEvent(rec);
						}
					}
				}))
			]
		}));
	},
	
	initActions: function() {
		var me = this,
				view = me.getOption('view');
		
		me.addAction('new', 'newEvent', {
			handler: function() {
				me.getAction('addEvent').execute();
			}
		});
		me.addAction('today', {
			handler: function() {
				me.moveDate(0);
			}
		});
		me.addAction('previousday', {
			text: null,
			handler: function() {
				me.moveDate(-1);
			}
		});
		me.addAction('nextday', {
			text: null,
			handler: function() {
				me.moveDate(1);
			}
		});
		me.addAction('dayview', {
			itemId: 'd',
			pressed: view === 'd',
			toggleGroup: 'view',
			handler: function() {
				me.changeView('d');
			}
		});
		me.addAction('week5view', {
			itemId: 'w5',
			pressed: view === 'w5',
			toggleGroup: 'view',
			handler: function() {
				me.changeView('w5');
			}
		});
		me.addAction('weekview', {
			itemId: 'w',
			pressed: view === 'w',
			toggleGroup: 'view',
			handler: function() {
				me.changeView('w');
			}
		});
		me.addAction('dweekview', {
			itemId: 'dw',
			pressed: view === 'dw',
			toggleGroup: 'view',
			handler: function() {
				me.changeView('dw');
			}
		});
		me.addAction('monthview', {
			itemId: 'm',
			pressed: view === 'm',
			toggleGroup: 'view',
			handler: function() {
				me.changeView('m');
			}
		});
		me.addAction('addCalendar', {
			handler: function() {
				var node = me.getSelectedFolder();
				if(node) me.addCalendar(node.get('_domainId'), node.get('_userId'));
			}
		});
		me.addAction('editCalendar', {
			handler: function() {
				var node = me.getSelectedFolder();
				if(node) me.editCalendar(node.getId());
			}
		});
		me.addAction('deleteCalendar', {
			handler: function() {
				var node = me.getSelectedFolder();
				if(node) me.deleteCalendar(node);
			}
		});
		me.addRef('uploaders', 'importEvents', Ext.create('Sonicle.upload.Item', {
			text: WT.res(me.ID, 'act-importEvents.lbl'),
			iconCls: WTF.cssIconCls(me.XID, 'importEvents', 'xs'),
			uploaderConfig: WTF.uploader(me.ID, 'ICalImport', {
				extraParams: {
					calendarId: null // Depends on selected node...
				},
				mimeTypes: [
					{title: 'iCalendar files', extensions: 'ical,ics,icalendar'}
					//{title: 'CSV files', extensions: 'csv'}
				]
			}),
			handler: function(s) {
				var node = me.getSelectedFolder();
				if(node) {
					s.uploader.mergeExtraParams({
						calendarId: node.getId()
					});
				}
			}
		}));
		me.addAction('viewAllCalendars', {
			iconCls: 'wt-icon-select-all-xs',
			handler: function() {
				me._showHideAllCals(me.getSelectedRootFolder(), true);
			}
		});
		me.addAction('viewNoneCalendars', {
			iconCls: 'wt-icon-select-none-xs',
			handler: function() {
				me._showHideAllCals(me.getSelectedRootFolder(), false);
			}
		});
		me.addAction('addEvent', {
			handler: function() {
				var cal = me.getSelectedFolder(),
						day = me.getRef('multical').getValue();
				if(cal) me.addEventAtNow(cal.get('_rootId'), cal.getId(), cal.get('_isPrivate'), cal.get('_busy'), cal.get('_reminder'), day);
			}
		});
		me.addAction('openEvent', {
			text: WT.res('act-open.lbl'),
			handler: function() {
				var rec = WT.getContextMenuData().event;
				if(me.isEventEditable(rec)) me.editEvent(rec);
			}
		});
		me.addAction('deleteEvent', {
			text: WT.res('act-delete.lbl'),
			iconCls: 'wt-icon-delete-xs',
			handler: function() {
				var rec = WT.getContextMenuData().event;
				me.deleteEvent(rec);
			}
		});
		me.addAction('restoreEvent', {
			text: WT.res('act-restore.lbl'),
			iconCls: 'wt-icon-restore-xs',
			handler: function() {
				var rec = WT.getContextMenuData().event;
				me.restoreEvent(rec);
			}
		});
		me.addAction('printEvents', {
			text: WT.res('act-print.lbl'),
			iconCls: 'wt-icon-print-xs',
			handler: function() {
				//TODO: implementare stampa
				WT.warn('TODO');
			}
		});
		me.addAction('erpExport', {
			iconCls: 'wtcal-icon-exportEvents-xs',
			handler: function() {
				me.erpExport();
			}
		});
	},
	
	initCxm: function() {
		var me = this;
		
		me.addRef('cxmRootFolder', Ext.create({
			xtype: 'menu',
			items: [
				me.getAction('addCalendar'),
				'-',
				me.getAction('addEvent')
				//TODO: azioni altri servizi?
			],
			listeners: {
				beforeshow: function() {
					var rec = WT.getContextMenuData().folder,
							my = (rec.get('_rootId') === WT.getOption('principal'));
					me.getAction('addCalendar').setDisabled(!my);
					me.getAction('addEvent').setDisabled(!my);
				}
			}
		}));
		
		me.addRef('cxmFolder', Ext.create({
			xtype: 'menu',
			items: [
				me.getAction('editCalendar'),
				me.getAction('deleteCalendar'),
				'-',
				me.getAction('addCalendar'),
				me.getRef('uploaders', 'importEvents'),
				'-',
				me.getAction('viewAllCalendars'),
				me.getAction('viewNoneCalendars'),
				'-',
				me.getAction('addEvent')
				//TODO: azioni altri servizi?
			],
			listeners: {
				beforeshow: function() {
					var rec = WT.getContextMenuData().folder,
							my = (rec.get('_rootId') === WT.getOption('principal'));
					me.getAction('editCalendar').setDisabled(!my);
					me.getAction('deleteCalendar').setDisabled(!my);
					me.getAction('addCalendar').setDisabled(!my);
					me.getAction('deleteCalendar').setDisabled(!my || rec.get('_builtIn'));
					me.getAction('addEvent').enable();
					//TODO: disabilitare azioni se readonly
				}
			}
		}));
		
		me.addRef('cxmEvent', Ext.create({
			xtype: 'menu',
			items: [
				me.getAction('openEvent'),
				'-',
				me.getAction('deleteEvent'),
				me.getAction('restoreEvent'),
				'-',
				me.getAction('addEvent'),
				me.getAction('printEvents')
				//TODO: azioni altri servizi?
			],
			listeners: {
				beforeshow: function() {
					var rec = WT.getContextMenuData().event,
							readOnly = (rec.get('isReadOnly') === true),
							broken = (rec.get('isBroken') === true);
					
					me.getAction('openEvent').setDisabled(readOnly);
					me.getAction('deleteEvent').setDisabled(readOnly);
					me.getAction('restoreEvent').setDisabled(readOnly || !broken);
					//me.getAction('restoreEvent').setDisabled(rec.get('isBroken') === false);
				}
			}
		}));
	},
	
	onActivate: function() {
		var me = this,
				scheduler = me.getRef('scheduler');
		
		if(scheduler.getStore().loadCount === 0) {
			// skip multical, it's already updated with now date
			scheduler.setStartDate(me.getRef('multical').getValue());
		}
	},
	
	onCalendarViewSave: function(s, success, model) {
		if(!success) return;
		var me = this,
				store = me.getRef('folderstree').getStore(),
				node;
		
		// Look for root folder and reload it!
		node = store.getNodeById(model.get('_profileId'));
		if(node) store.load({node: node});
	},
	
	onEventViewSave: function(s, success, model) {
		if(!success) return;
		this.refreshEvents();
	},
	
	refreshEvents: function() {
		var me = this;
		me.getRef('multical').getStore().load();
		me.getRef('scheduler').getStore().load();
	},
	
	changeView: function(view) {
		var me = this;
		me._activateMain('scheduler');
		me.getRef('multical').setHighlightMode(view);
		me.getRef('scheduler').setActiveView(view);
	},
	
	moveDate: function(direction) {
		this._activateMain('scheduler');
		var mc = this.getRef('multical');
		if(direction === 0) {
			mc.setToday();
		} else if(direction === -1) {
			mc.setPreviousDay();
		} else if(direction === 1) {
			mc.setNextDay();
		}
	},
	
	addCalendar: function(domainId, userId) {
		var me = this,
				vwc = WT.createView(me.ID, 'view.Calendar');
		
		vwc.getComponent(0).on('viewsave', me.onCalendarViewSave, me);
		vwc.show(false, function() {
			vwc.getComponent(0).beginNew({
				data: {
					domainId: domainId,
					userId: userId
				}
			});
		});
	},
	
	editCalendar: function(calendarId) {
		var me = this,
				vwc = WT.createView(me.ID, 'view.Calendar');
		
		vwc.getComponent(0).on('viewsave', me.onCalendarViewSave, me);
		vwc.show(false, function() {
			vwc.getComponent(0).beginEdit({
				data: {
					calendarId: calendarId
				}
			});
		});
	},
	
	deleteCalendar: function(rec) {
		WT.confirm(this.res('calendar.confirm.delete', rec.get('text')), function(bid) {
			if(bid === 'yes') rec.drop();
		}, this);
	},
	
	addEventAtNow: function(profileId, calendarId, isPrivate, busy, reminder, day) {
		if(day === undefined) day = new Date();
		var date = Sonicle.Date.copyTime(new Date(), day);
		this.addEvent(profileId, calendarId, isPrivate, busy, reminder, date, date, false);
	},
	
	addEvent: function(profileId, calendarId, isPrivate, busy, reminder, start, end, allDay) {
		var me = this,
				EM = Sonicle.webtop.calendar.model.Event,
				vwc = WT.createView(me.ID, 'view.Event', {
					viewCfg: {
						profileId: profileId
					}
				});
		
		vwc.getComponent(0).on('viewsave', me.onEventViewSave, me);
		vwc.show(false, function() {
			vwc.getComponent(0).beginNew({
				data: {
					calendarId: calendarId,
					isPrivate: isPrivate,
					busy: busy,
					reminder: reminder,
					startDate: start,
					endDate: end,
					timezone: WT.getOption('timezone'),
					allDay: allDay
				}
			});
		});
	},
	
	editEvent: function(rec) {
		var me = this,
				vwc = WT.createView(me.ID, 'view.Event', {
					viewCfg: {
						profileId: rec.get('_profileId')
					}
				});
		
		vwc.getView().on('viewsave', me.onEventViewSave, me);
		vwc.show(false, function() {
			vwc.getView().beginEdit({
				data: {
					id: rec.get('id')
				}
			});
		});
	},
	
	deleteEvent: function(rec, opts) {
		opts = opts || {};
		var me = this, ajaxFn;
		
		ajaxFn = function(target, id) {
			WT.ajaxReq(me.ID, 'ManageEventsScheduler', {
				params: {
					crud: 'delete',
					target: target,
					id: id
				},
				callback: function(success, o) {
					Ext.callback(opts.callback, opts.scope, [success, o]);
					if(success) me.refreshEvents();
					
				}
			});
		};
		
		if(rec.get('isRecurring')) {
			me.confirmForRecurrence(me.res('event.recurring.confirm.delete'), function(bid) {
				if(bid === 'ok') {
					var target = WT.Util.getCheckedRadioUsingDOM(['this', 'since', 'all']);
					ajaxFn(target, rec.get('id'));
				}
			}, me);
		} else {
			WT.confirm(me.res('event.confirm.delete', rec.get('title')), function(bid) {
				if(bid === 'yes') {
					ajaxFn('this', rec.get('id'));
				}
			}, me);
		}
	},
	
	restoreEvent: function(rec, opts) {
		opts = opts || {};
		var me = this;
		WT.confirm(me.res('event.recurring.confirm.restore'), function(bid) {
			if(bid === 'yes') {
				WT.ajaxReq(me.ID, 'ManageEventsScheduler', {
					params: {
						crud: 'restore',
						id: rec.get('id')
					},
					callback: function(success, o) {
						Ext.callback(opts.callback, opts.scope, [success, o]);
						if(success) me.refreshEvents();
					}
				});
			}
		}, me);
	},
	
	erpExport: function() {
		var me = this,
				vwc = WT.createView(me.ID, 'view.ErpExport');
		vwc.show(false);
	},
	
	searchEvents: function(query) {
		var me = this,
				gp = me.getRef('results');
		
		gp.setTitle(Ext.String.format('{0}: {1}', WT.res('word.search'), query));
		me._activateMain('results');
		WTU.loadExtraParams(gp.getStore(), {
			query: query
		});
	},
	
	isEventEditable: function(rec) {
		return !(rec.get('isReadOnly') === true);
	},
	
	/**
	 * @private
	 * Returns selected root folder. If force param is 'true', this method 
	 * returns a default value if no selection is available.
	 * @param {Boolean} [force=false] 'true' to always return a value.
	 * @returns {Ext.data.NodeInterface}
	 */
	getSelectedRootFolder: function(force) {
		var tree = this.getRef('folderstree'),
				sel = tree.getSelection();
		
		if(sel.length === 0) {
			if(!force) return null;
			// As default returns myFolder, which have id equals to principal option
			return tree.getStore().getNodeById(WT.getOption('principal'));
		}
		return (sel[0].get('_type') === 'root') ? sel[0] : sel[0].parentNode;
	},
	
	/*
	 * @private
	 * Returns selected folder (calendar). If no selection is available, 
	 * this method tries to return the default folder and then the built-in one.
	 * @returns {Ext.data.NodeInterface}
	 */
	getSelectedFolder: function() {
		var me = this,
				tree = me.getRef('folderstree'),
				sel = tree.getSelection(),
				node;
		
		if(sel.length > 0) {
			if(sel[0].get('_type') === 'root') {
				return me.getFolderByRoot(sel[0]);
			} else {
				return sel[0];
			}
		} else {
			node = tree.getStore().getNodeById(WT.getOption('principal'));
			if(node) return me.getFolderByRoot(node);
		}
		return null;
	},
	
	/*
	 * @private
	 */
	getFolderByRoot: function(rootNode) {
		var cal = this.getDefaultFolder(rootNode);
		return (cal) ? cal : this.getBuiltInFolder(rootNode);
	},
	
	/*
	 * @private
	 */
	getDefaultFolder: function(rootNode) {
		return rootNode.findChildBy(function(n) {
			return (n.get('_default') === true);
		});
	},
	
	/*
	 * @private
	 */
	getBuiltInFolder: function(rootNode) {
		return rootNode.findChildBy(function(n) {
			return (n.get('_builtIn') === true);
		});
	},
	
	_activateMain: function(id) {
		this.getMainComponent().getLayout().setActiveItem(id);
	},
	
	_isMainActive: function(id) {
		return (this.getMainComponent().getLayout().getActiveItem() === id);
	},
	
	_showHideCal: function(node, show) {
		node.beginEdit();
		node.set('_visible', show);
		node.endEdit();
	},
	
	_showHideAllCals: function(parent, show) {
		var me = this,
				store = parent.getTreeStore();
		
		store.suspendAutoSync();
		parent.cascadeBy(function(n) {
			if(n !== parent) {
				n.set('checked', show);
				me._showHideCal(n, show);
			}
		});
		store.resumeAutoSync();
		store.sync();
	},
	
	print: function() {
		
		Ext.create('Ext.XTemplate',
			'<table border="0" cellspacing="0" width="100%">',
			
			
			
			
			'</table>'
		);
		
	},
	
	
	
	printEvents: function (owner, start, end, records) {
		var me = this,
				EM = Sonicle.calendar.data.EventMappings,
				days = Sonicle.Date.diffDays(start, end);
		
		var month = this.monthNames[this.value.getMonth()];
		var year = this.value.getFullYear();
		var max = this.value.getDaysInMonth();
		if (view === 'd') {
			max = 1;
		} else if (view === 'w5') {
			max = 5;
		} else if (view === 'w' || view === 'w7') {
			max = 7;
		}

		var htmlSrc = "";
		htmlSrc += "<TABLE BORDER=0 CELLSPACING=0 width='100%'>";

		// Title
		htmlSrc += "<TR BGCOLOR=GRAY>";
		htmlSrc += "<TD ALIGN=CENTER COLSPAN=2>";
		htmlSrc += "<H1>";
		htmlSrc += owner + " - " + Ext.Date.monthNames[start.getMonth()] + " " + start.getFullYear();
		htmlSrc += "</H1>";
		htmlSrc += "</TD>";
		htmlSrc += "</TR>";
		htmlSrc += "<TR BGCOLOR=GRAY>";
		htmlSrc += "<TD ALIGN=CENTER COLSPAN=2>";
		htmlSrc += "<H2>";

		htmlSrc += Ext.Date.dayNames[start.getDay()] + " " + start.getDate();
		if (days > 0) {
			htmlSrc += " - " + Ext.Date.dayNames[end.getDay()] + " " + end.getDate();
		}
		htmlSrc += "</H2>";
		htmlSrc += "</TD>";
		htmlSrc += "</TR>";
		
		
		Ext.iterate(records, function(rec) {
			
		});
		
		// Events
		var d = null;
		var cspan = "";
		if (days > 0) cspan = "COLSPAN=2";
		for (var i = 1; i <= max; i++) {
			d = this.sd[i].getDate();
			var len = this.sd[i].evts.length;
			if (len > 0) {
				for (var j = 0; j < len; j++) {
					var sevent = this.sd[i].evts[j];
					WT.debug("event=" + sevent.getCustomer() + " " + sevent.getReport() + " " + sevent.getActivity());
					htmlSrc += "<TR>";
					if (j == 0 && max > 1) {
						htmlSrc += "<TD width=100>" + this.dayNames[d.getDay()] + " " + d.getDate() + "</TD>";
					} else if (max > 1) {
						htmlSrc += "<TD width=100>&nbsp;</TD>";
					}
					htmlSrc += "<TD " + cspan + ">" + sevent.getStartHour() + ":" + sevent.getStartMinute() + " " + sevent.getEndHour() + ":" + sevent.getEndMinute() + " " + sevent.event + " " + WT.res("calendar", "textLocation") + sevent.getReport() + " - " + WT.res("calendar", "activity") + sevent.getActivity() + " - " + WT.res("calendar", "customer") + sevent.getCustomer() + "</TD>";
					htmlSrc += "</TR>";
				}
			} else {
				htmlSrc += "<TR>";
				if (max > 1)
					htmlSrc += "<TD width=100>" + this.dayNames[d.getDay()] + " " + d.getDate() + "</TD>";
				htmlSrc += "<TD " + cspan + ">&nbsp;</TD>";
				htmlSrc += "</TR>";
			}
		}

		htmlSrc += "</TABLE>";

		//WT.app.print(htmlSrc);
	},
	
	confirmForRecurrence: function(msg, cb, scope, opts) {
		var me = this, html;
		html = "</br></br>"
				+ "<table width='70%' style='font-size: 12px'>"
				+ "<tr><td><input type='radio' name='recurrence' id='this' checked='true' /></td><td width='95%'>"+me.res("confirm.recurrence.this")+"</td></tr>"
				+ "<tr><td><input type='radio' name='recurrence' id='since' /></td><td width='95%'>"+me.res("confirm.recurrence.since")+"</td></tr>"
				+ "<tr><td><input type='radio' name='recurrence' id='all' /></td><td width='95%'>"+me.res("confirm.recurrence.all")+"</td></tr>"
				+ "</table>";
		WT.confirm(msg + html, cb, scope, Ext.apply({
			buttons: Ext.Msg.OKCANCEL
		}, opts));
	}
});
