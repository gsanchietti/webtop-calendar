/**
 * This class is generated by jOOQ
 */
package com.sonicle.webtop.calendar.jooq.tables;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.5.3"
	},
	comments = "This class is generated by jOOQ"
)
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Calendars extends org.jooq.impl.TableImpl<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord> {

	private static final long serialVersionUID = 2019949657;

	/**
	 * The reference instance of <code>calendar.calendars</code>
	 */
	public static final com.sonicle.webtop.calendar.jooq.tables.Calendars CALENDARS = new com.sonicle.webtop.calendar.jooq.tables.Calendars();

	/**
	 * The class holding records for this type
	 */
	@Override
	public java.lang.Class<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord> getRecordType() {
		return com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord.class;
	}

	/**
	 * The column <code>calendar.calendars.calendar_id</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.Integer> CALENDAR_ID = createField("calendar_id", org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaulted(true), this, "");

	/**
	 * The column <code>calendar.calendars.domain_id</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.String> DOMAIN_ID = createField("domain_id", org.jooq.impl.SQLDataType.VARCHAR.length(20).nullable(false), this, "");

	/**
	 * The column <code>calendar.calendars.user_id</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.String> USER_ID = createField("user_id", org.jooq.impl.SQLDataType.VARCHAR.length(100).nullable(false), this, "");

	/**
	 * The column <code>calendar.calendars.built_in</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.Boolean> BUILT_IN = createField("built_in", org.jooq.impl.SQLDataType.BOOLEAN.defaulted(true), this, "");

	/**
	 * The column <code>calendar.calendars.name</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR.length(50).nullable(false), this, "");

	/**
	 * The column <code>calendar.calendars.description</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.String> DESCRIPTION = createField("description", org.jooq.impl.SQLDataType.VARCHAR.length(100), this, "");

	/**
	 * The column <code>calendar.calendars.color</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.String> COLOR = createField("color", org.jooq.impl.SQLDataType.VARCHAR.length(20), this, "");

	/**
	 * The column <code>calendar.calendars.sync</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.String> SYNC = createField("sync", org.jooq.impl.SQLDataType.VARCHAR.length(1).nullable(false), this, "");

	/**
	 * The column <code>calendar.calendars.busy</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.Boolean> BUSY = createField("busy", org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaulted(true), this, "");

	/**
	 * The column <code>calendar.calendars.reminder</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.Integer> REMINDER = createField("reminder", org.jooq.impl.SQLDataType.INTEGER, this, "");

	/**
	 * The column <code>calendar.calendars.invitation</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.Boolean> INVITATION = createField("invitation", org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaulted(true), this, "");

	/**
	 * The column <code>calendar.calendars.is_private</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.Boolean> IS_PRIVATE = createField("is_private", org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaulted(true), this, "");

	/**
	 * The column <code>calendar.calendars.is_default</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.Boolean> IS_DEFAULT = createField("is_default", org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaulted(true), this, "");

	/**
	 * Create a <code>calendar.calendars</code> table reference
	 */
	public Calendars() {
		this("calendars", null);
	}

	/**
	 * Create an aliased <code>calendar.calendars</code> table reference
	 */
	public Calendars(java.lang.String alias) {
		this(alias, com.sonicle.webtop.calendar.jooq.tables.Calendars.CALENDARS);
	}

	private Calendars(java.lang.String alias, org.jooq.Table<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord> aliased) {
		this(alias, aliased, null);
	}

	private Calendars(java.lang.String alias, org.jooq.Table<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord> aliased, org.jooq.Field<?>[] parameters) {
		super(alias, com.sonicle.webtop.calendar.jooq.Calendar.CALENDAR, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.Identity<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord, java.lang.Integer> getIdentity() {
		return com.sonicle.webtop.calendar.jooq.Keys.IDENTITY_CALENDARS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.UniqueKey<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord> getPrimaryKey() {
		return com.sonicle.webtop.calendar.jooq.Keys.CALENDARS_PKEY1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.List<org.jooq.UniqueKey<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord>> getKeys() {
		return java.util.Arrays.<org.jooq.UniqueKey<com.sonicle.webtop.calendar.jooq.tables.records.CalendarsRecord>>asList(com.sonicle.webtop.calendar.jooq.Keys.CALENDARS_PKEY1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public com.sonicle.webtop.calendar.jooq.tables.Calendars as(java.lang.String alias) {
		return new com.sonicle.webtop.calendar.jooq.tables.Calendars(alias, this);
	}

	/**
	 * Rename this table
	 */
	public com.sonicle.webtop.calendar.jooq.tables.Calendars rename(java.lang.String name) {
		return new com.sonicle.webtop.calendar.jooq.tables.Calendars(name, null);
	}
}
