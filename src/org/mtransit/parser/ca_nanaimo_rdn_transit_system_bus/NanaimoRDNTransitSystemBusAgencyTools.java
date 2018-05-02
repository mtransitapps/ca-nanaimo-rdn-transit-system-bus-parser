package org.mtransit.parser.ca_nanaimo_rdn_transit_system_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// https://bctransit.com/*/footer/open-data
// https://bctransit.com/servlet/bctransit/data/GTFS - Nanaimo
public class NanaimoRDNTransitSystemBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-nanaimo-rdn-transit-system-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new NanaimoRDNTransitSystemBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating RDN Transit System bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		System.out.printf("\nGenerating RDN Transit System bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	private static final String INCLUDE_AGENCY_ID = "5"; // RDN Transit System only

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!INCLUDE_AGENCY_ID.equals(gRoute.getAgencyId())) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = CleanUtils.cleanSlashes(routeLongName);
		routeLongName = CleanUtils.cleanNumbers(routeLongName);
		routeLongName = CleanUtils.cleanStreetTypes(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_GREEN = "34B233"; // GREEN (from PDF Corporate Graphic Standards)
	private static final String AGENCY_COLOR_BLUE = "002C77"; // BLUE (from PDF Corporate Graphic Standards)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String ROUTE_COLOR_LOCAL = "809699";
	private static final String ROUTE_COLOR_FREQUENT = "009FC2";

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
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
				if (isGoodEnoughAccepted()) {
					return AGENCY_COLOR_BLUE;
				}
				System.out.printf("\nUnexpected route color for %s!\n", gRoute);
				System.exit(-1);
				return null;
			}
		}
		return super.getRouteColor(gRoute);
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private static final String EXCHANGE_SHORT = "Exch";
	private static final String CINNABAR = "Cinnabar";
	private static final String CEDAR = "Cedar";
	private static final String COUNTRY_CLUB = "Country Club";
	private static final String DOWNTOWN = "Downtown";
	private static final String CINNABAR_CEDAR = CINNABAR + " & " + CEDAR;
	private static final String VI_UNIVERSITY_SHORT = "VIU";
	private static final String WOODGROVE = "Woodgrove";
	private static final String QUALICUM_BEACH = "Qualicum Beach";
	private static final String WESTWOOD = "Westwood";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1L, new RouteTripSpec(1L, //
				0, MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				1, MTrip.HEADSIGN_TYPE_STRING, COUNTRY_CLUB) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"110512", // Country Club Exchange Bay E
								"110211", // ++
								"110509", // Prideaux Street Exchange Bay E
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"110509", // Prideaux Street Exchange Bay E
								"109787", // ++
								"110512", // Country Club Exchange Bay E
						})) //
				.compileBothTripSort());
		map2.put(5L, new RouteTripSpec(5L, //
				0, MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				1, MTrip.HEADSIGN_TYPE_STRING, WESTWOOD) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"109756", // Westbound Arbot at Westwood
								"109762", // Eastbound Ashlee at Holland
								"109763", // !=
								"110520", // <> VIU Exchange Bay D
								"110101", // !=
								"110522", // Prideaux Street Exchange Bay B
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"110522", // Prideaux Street Exchange Bay B
								"110100", // !=
								"110520", // <> VIU Exchange Bay D
								"110072", // !=
								"109756", // Westbound Arbot at Westwood
						})) //
				.compileBothTripSort());
		map2.put(25L, new RouteTripSpec(25L, //
				0, MTrip.HEADSIGN_TYPE_STRING, WOODGROVE, // VI_UNIVERSITY_SHORT
				1, MTrip.HEADSIGN_TYPE_STRING, "BC Ferries") //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"109880", // Northbound Trans-Canada at Horseshoe Bay-Departure Bay Ferry
								"110519", // VIU Exchange Bay B VIU
								"109925", // Woodgrove Centre Exchange Bay D
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"109925", // Woodgrove Centre Exchange Bay D
								"110516", // ++
								"109880", // Northbound Trans-Canada at Horseshoe Bay-Departure Bay Ferry

						})) //
				.compileBothTripSort());
		map2.put(88L, new RouteTripSpec(88L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Parksville", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Wembley Mall") //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"110299", // Westbound Jensen Ave E at Craig
								"110441", // ++
								"104058", // Southbound Island highway W at Wembley Mall
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"104058", // Southbound Island highway W at Wembley Mall
								"110280", // ++
								"110299", // Westbound Jensen Ave E at Craig
						})) //
				.compileBothTripSort());
		map2.put(91L, new RouteTripSpec(91L, //
				0, MTrip.HEADSIGN_TYPE_STRING, WOODGROVE, //
				1, MTrip.HEADSIGN_TYPE_STRING, QUALICUM_BEACH) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"110358", // Southbound Jones at Fern Rd W
								"110369", // ==
								"110370", // !=
								"110378", // !=
								"104050", // ==
								"110388", // ++ Eastbound Jensen Ave E at McCarter
								"104160", // ==
								"104169", // !=
								"110399", // !=
								"104173", // !=
								"110415", // ==
								"110300", // !=
								"109925", // <> Woodgrove Centre Exchange Bay D
								"110088", // !=
								"109880", // Northbound Trans-Canada at Horseshoe Bay-Departure Bay Ferry
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"109880", // Northbound Trans-Canada at Horseshoe Bay-Departure Bay Ferry
								"110007", // !=
								"109925", // <> == Woodgrove Centre Exchange Bay D
								"110301", // !=
								"110302", // !=
								"110304", // ==
								"110307", // !=
								"110321", // !=
								"110496", // !=
								"110322", // ==
								"110299", // ++ Westbound Jensen Ave E at Craig
								"104049", // ==
								"104075", // !=
								"104175", // !=
								"110345", // !=
								"110346", // ==
								"110358", // Southbound Jones at Fern Rd W
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 6L) {
			if (Arrays.asList( //
					COUNTRY_CLUB, //
					DOWNTOWN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId()); //
				return true;
			}
		} else if (mTrip.getRouteId() == 7L) {
			if (Arrays.asList( //
					DOWNTOWN, //
					CINNABAR, //
					CINNABAR_CEDAR //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CINNABAR_CEDAR, mTrip.getHeadsignId()); //
				return true;
			}
		} else if (mTrip.getRouteId() == 11L) {
			if (Arrays.asList( //
					"BC Ferries", //
					"Lantzville" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Lantzville", mTrip.getHeadsignId()); //
				return true;
			}
		} else if (mTrip.getRouteId() == 15L) {
			if (Arrays.asList( //
					VI_UNIVERSITY_SHORT, //
					VI_UNIVERSITY_SHORT + " Only", //
					VI_UNIVERSITY_SHORT + "-" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(VI_UNIVERSITY_SHORT, mTrip.getHeadsignId()); //
				return true;
			}
		} else if (mTrip.getRouteId() == 20L) {
			if (Arrays.asList( //
					COUNTRY_CLUB, // same
					DOWNTOWN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId()); //
				return true;
			} else if (Arrays.asList( //
					COUNTRY_CLUB, // same
					WOODGROVE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(WOODGROVE, mTrip.getHeadsignId()); //
				return true;
			}
		}
		System.out.printf("\nUnexpected trips to merge %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern EXCHANGE_ = Pattern.compile("((^|\\W){1}(exchange)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = "$2" + EXCHANGE_SHORT + "$4";

	private static final Pattern VI_UNIVERSITY_ = Pattern.compile("((^|\\W){1}(vi university|viu)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String VI_UNIVERSITY_REPLACEMENT = "$2" + VI_UNIVERSITY_SHORT + "$4";

	private static final Pattern STARTS_WITH_NUMBER = Pattern.compile("(^[\\d]+( \\-)?[\\S]*)", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_VIA = Pattern.compile("( via .*$)", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_TO = Pattern.compile("(^.* to )", Pattern.CASE_INSENSITIVE);

	private static final Pattern AND = Pattern.compile("( and )", Pattern.CASE_INSENSITIVE);
	private static final String AND_REPLACEMENT = " & ";

	private static final Pattern CLEAN_P1 = Pattern.compile("[\\s]*\\([\\s]*");
	private static final String CLEAN_P1_REPLACEMENT = " (";
	private static final Pattern CLEAN_P2 = Pattern.compile("[\\s]*\\)[\\s]*");
	private static final String CLEAN_P2_REPLACEMENT = ") ";

	private static final Pattern CINNABAR_ = Pattern.compile("((^|\\W){1}(cinnibar)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String CINNABAR_REPLACEMENT = "$2" + CINNABAR + "$4";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = CINNABAR_.matcher(tripHeadsign).replaceAll(CINNABAR_REPLACEMENT);
		tripHeadsign = EXCHANGE_.matcher(tripHeadsign).replaceAll(EXCHANGE_REPLACEMENT);
		tripHeadsign = VI_UNIVERSITY_.matcher(tripHeadsign).replaceAll(VI_UNIVERSITY_REPLACEMENT);
		tripHeadsign = ENDS_WITH_VIA.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = STARTS_WITH_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = AND.matcher(tripHeadsign).replaceAll(AND_REPLACEMENT);
		tripHeadsign = CLEAN_P1.matcher(tripHeadsign).replaceAll(CLEAN_P1_REPLACEMENT);
		tripHeadsign = CLEAN_P2.matcher(tripHeadsign).replaceAll(CLEAN_P2_REPLACEMENT);
		tripHeadsign = STARTS_WITH_NUMBER.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern STARTS_WITH_BOUND = Pattern.compile("(^(east|west|north|south)bound)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = STARTS_WITH_BOUND.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = EXCHANGE_.matcher(gStopName).replaceAll(EXCHANGE_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
