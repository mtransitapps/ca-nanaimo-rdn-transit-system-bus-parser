package org.mtransit.parser.ca_nanaimo_rdn_transit_system_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.mtransit.commons.Constants.EMPTY;

// https://www.bctransit.com/open-data
public class NanaimoRDNTransitSystemBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new NanaimoRDNTransitSystemBusAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_EN;
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "Nanaimo RDN TS";
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		final String tripHeadsignLC = gTrip.getTripHeadsignOrDefault().toLowerCase(Locale.ENGLISH);
		if (tripHeadsignLC.contains("not in service")) {
			return true; // exclude
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeStopTime(@NotNull GStopTime gStopTime) {
		final String stopHeadsignLC = gStopTime.getStopHeadsignOrDefault().toLowerCase(Locale.ENGLISH);
		if (stopHeadsignLC.contains("not in service")) {
			return true; // exclude
		}
		return super.excludeStopTime(gStopTime);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return false; // route ID used by GTFS RT
	}

	@Override
	public @Nullable String getRouteIdCleanupRegex() {
		return "\\-[A-Z]+$";
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	private static final Pattern STARTS_WITH_DASH = Pattern.compile("(^- )", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = STARTS_WITH_DASH.matcher(routeLongName).replaceAll(EMPTY);
		routeLongName = CleanUtils.cleanSlashes(routeLongName);
		routeLongName = CleanUtils.cleanNumbers(routeLongName);
		routeLongName = CleanUtils.cleanStreetTypes(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	private static final String AGENCY_COLOR_GREEN = "34B233"; // GREEN (from PDF Corporate Graphic Standards)
	// private static final String AGENCY_COLOR_BLUE = "002C77"; // BLUE (from PDF Corporate Graphic Standards)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String ROUTE_COLOR_LOCAL = "809699";
	private static final String ROUTE_COLOR_FREQUENT = "009FC2";

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@Nullable
	@Override
	public String provideMissingRouteColor(@NotNull GRoute gRoute) {
		final int rsn = Integer.parseInt(gRoute.getRouteShortName());
		switch (rsn) {
		// @formatter:off
		case 1: return ROUTE_COLOR_LOCAL;
		case 5: return ROUTE_COLOR_LOCAL;
		case 6: return ROUTE_COLOR_LOCAL;
		case 7: return ROUTE_COLOR_LOCAL;
		case 11: return ROUTE_COLOR_LOCAL;
		case 15: return ROUTE_COLOR_LOCAL;
		case 20: return ROUTE_COLOR_LOCAL;
		case 25: return ROUTE_COLOR_LOCAL;
		case 30: return ROUTE_COLOR_LOCAL;
		case 40: return ROUTE_COLOR_FREQUENT;
		case 50: return ROUTE_COLOR_LOCAL;
		case 88: return "B3AA7E"; // LIGHT BROWN
		case 90: return "4F6F19"; // DARK GREEN
		case 91: return ROUTE_COLOR_LOCAL;
		case 92: return ROUTE_COLOR_LOCAL; // ?
		case 97: return ROUTE_COLOR_LOCAL;
		case 98: return ROUTE_COLOR_LOCAL;
		case 99: return AGENCY_COLOR_GREEN; // LIGHT GREEN
		// @formatter:on
		default:
			throw new MTLog.Fatal("Unexpected route color for %s!", gRoute);
		}
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern ENDS_WITH_BAY_ABC = Pattern.compile("(( bay | b:)\\w$)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanDirectionHeadsign(int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		if (fromStopName) {
			directionHeadSign = ENDS_WITH_BAY_ABC.matcher(directionHeadSign).replaceAll(EMPTY);
		}
		directionHeadSign = super.cleanDirectionHeadsign(directionId, fromStopName, directionHeadSign);
		return directionHeadSign;
	}

	private static final Pattern VI_UNIVERSITY_ = Pattern.compile("((^|\\W)(vi university|viu)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String VI_UNIVERSITY_REPLACEMENT = "$2" + "VIU" + "$4";

	private static final Pattern STARTS_WITH_NUMBER = Pattern.compile("(^\\d+( -)?\\S*)", Pattern.CASE_INSENSITIVE);

	private static final Pattern FIX_BEACH_ = Pattern.compile("((^|\\W)(Bch)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String FIX_BEACH_REPLACEMENT = "$2" + "Beach" + "$4";

	private static final Pattern FIX_CINNABAR_ = Pattern.compile("((^|\\W)(cinnibar)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String FIX_CINNABAR_REPLACEMENT = "$2" + "Cinnabar" + "$4";

	private static final Pattern SHUTTLE_ = Pattern.compile("((^|\\W)(shutlle)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String SHUTTLE_REPLACEMENT = "$2" + "Shuttle" + "$4";

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign, getIgnoredUpperCaseWords());
		tripHeadsign = FIX_BEACH_.matcher(tripHeadsign).replaceAll(FIX_BEACH_REPLACEMENT);
		tripHeadsign = FIX_CINNABAR_.matcher(tripHeadsign).replaceAll(FIX_CINNABAR_REPLACEMENT);
		tripHeadsign = SHUTTLE_.matcher(tripHeadsign).replaceAll(SHUTTLE_REPLACEMENT);
		tripHeadsign = ENDS_WITH_BAY_ABC.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = VI_UNIVERSITY_.matcher(tripHeadsign).replaceAll(VI_UNIVERSITY_REPLACEMENT);
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = STARTS_WITH_NUMBER.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@NotNull
	private String[] getIgnoredUpperCaseWords() {
		return new String[]{"ACR", "BC", "NS", "FS", "VIU", "YCD"};
	}

	private static final Pattern STARTS_WITH_DCOM = Pattern.compile("(^(\\(-DCOM-\\)))", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_IMPL = Pattern.compile("(^(\\(-IMPL-\\)))", Pattern.CASE_INSENSITIVE);

	private static final Pattern FIX_BOUND = Pattern.compile("(boudn)", Pattern.CASE_INSENSITIVE);
	private static final String FIX_BOUND_REPLACEMENT = "bound";

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName, getIgnoredUpperCaseWords());
		gStopName = STARTS_WITH_DCOM.matcher(gStopName).replaceAll(EMPTY);
		gStopName = STARTS_WITH_IMPL.matcher(gStopName).replaceAll(EMPTY);
		gStopName = FIX_BOUND.matcher(gStopName).replaceAll(FIX_BOUND_REPLACEMENT);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) { // used by GTFS-RT
		return super.getStopId(gStop);
	}
}
