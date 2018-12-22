package org.mtransit.parser.ca_nanaimo_rdn_transit_system_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.commons.StrategicMappingCommons;
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
// https://nanaimo.mapstrat.com/current/google_transit.zip
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
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
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

	private static final String INCLUDE_AGENCY_ID;
	static {
		INCLUDE_AGENCY_ID = "1"; // RDN Transit System only
	}

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

	private static final String AND = " & ";
	private static final String EXCHANGE_SHORT = "Exch";
	private static final String CINNABAR = "Cinnabar";
	private static final String CEDAR = "Cedar";
	private static final String COUNTRY_CLUB = "Country Club";
	private static final String DOWNTOWN = "Downtown";
	private static final String CINNABAR_CEDAR = CINNABAR + AND + CEDAR;
	private static final String VI_UNIVERSITY_SHORT = "VIU";
	private static final String WOODGROVE = "Woodgrove";
	private static final String BEACH = "Beach";
	private static final String WESTWOOD = "Westwood";


	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(5L, new RouteTripSpec(5L, //
				StrategicMappingCommons.CLOCKWISE_0, MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				StrategicMappingCommons.CLOCKWISE_1, MTrip.HEADSIGN_TYPE_STRING, WESTWOOD) //
				.addTripSort(StrategicMappingCommons.CLOCKWISE_0, //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("109756"), Stops2.ALL_STOPS2.get("109756"), // Westbound Arbot at Westwood
								Stops.ALL_STOPS.get("109762"), Stops2.ALL_STOPS2.get("109762"), // Eastbound Ashlee at Holland
								Stops.ALL_STOPS.get("109763"), Stops2.ALL_STOPS2.get("109763"), // !=
								Stops.ALL_STOPS.get("110520"), Stops2.ALL_STOPS2.get("110520"), // <> VIU Exchange Bay D
								Stops.ALL_STOPS.get("110101"), Stops2.ALL_STOPS2.get("110101"), // !=
								Stops.ALL_STOPS.get("110522"), Stops2.ALL_STOPS2.get("110522"), // Prideaux Street Exchange Bay B
						})) //
				.addTripSort(StrategicMappingCommons.CLOCKWISE_1, //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("110522"), Stops2.ALL_STOPS2.get("110522"), // Prideaux Street Exchange Bay B
								Stops.ALL_STOPS.get("110100"), Stops2.ALL_STOPS2.get("110100"), // !=
								Stops.ALL_STOPS.get("110520"), Stops2.ALL_STOPS2.get("110520"), // <> VIU Exchange Bay D
								Stops.ALL_STOPS.get("110072"), Stops2.ALL_STOPS2.get("110072"), // !=
								Stops.ALL_STOPS.get("109756"), Stops2.ALL_STOPS2.get("109756"), // Westbound Arbot at Westwood
						})) //
				.compileBothTripSort());
		map2.put(11L, new RouteTripSpec(11L, //
				StrategicMappingCommons.CLOCKWISE_0, MTrip.HEADSIGN_TYPE_STRING, "West", //
				StrategicMappingCommons.CLOCKWISE_1, MTrip.HEADSIGN_TYPE_STRING, WOODGROVE) //
				.addTripSort(StrategicMappingCommons.CLOCKWISE_0, //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("109925"), Stops2.ALL_STOPS2.get("109925"), // Woodgrove Centre Exchange Bay D
								Stops.ALL_STOPS.get("110220"), Stops2.ALL_STOPS2.get("110220"), // ++
								Stops.ALL_STOPS.get("110226"), Stops2.ALL_STOPS2.get("110226"), // Eastwind at Northwind
						})) //
				.addTripSort(StrategicMappingCommons.CLOCKWISE_1, //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("110226"), Stops2.ALL_STOPS2.get("110226"), // Eastwind at Northwind
								Stops.ALL_STOPS.get("109829"), Stops2.ALL_STOPS2.get("109829"), // == Dover at Applecross
								Stops.ALL_STOPS.get("109830"), Stops2.ALL_STOPS2.get("109830"), // != Uplands at McRobb
								Stops.ALL_STOPS.get("109831"), Stops2.ALL_STOPS2.get("109831"), // != Nanaimo Seniors Village
								Stops.ALL_STOPS.get("109929"), Stops2.ALL_STOPS2.get("109929"), // != Dover at Uplands
								Stops.ALL_STOPS.get("109921"), Stops2.ALL_STOPS2.get("109921"), // != Dover Bay High School
								Stops.ALL_STOPS.get("109922"), Stops2.ALL_STOPS2.get("109922"), // == Hammond Bay at Uplands
								Stops.ALL_STOPS.get("109925"), Stops2.ALL_STOPS2.get("109925"), // Woodgrove Centre Exchange Bay D
						})) //
				.compileBothTripSort());
		map2.put(25L, new RouteTripSpec(25L, //
				StrategicMappingCommons.CLOCKWISE_0, MTrip.HEADSIGN_TYPE_STRING, WOODGROVE, // VI_UNIVERSITY_SHORT
				StrategicMappingCommons.CLOCKWISE_1, MTrip.HEADSIGN_TYPE_STRING, "BC Ferries") //
				.addTripSort(StrategicMappingCommons.CLOCKWISE_0, //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("109880"), Stops2.ALL_STOPS2.get("109880"), // == Departure Bay Ferry
								Stops.ALL_STOPS.get("109964"), Stops2.ALL_STOPS2.get("109964"), // != Stewart at Maple
								Stops.ALL_STOPS.get("104170"), Stops2.ALL_STOPS2.get("104170"), // Prideaux Street Exchange Bay H
								Stops.ALL_STOPS.get("110519"), Stops2.ALL_STOPS2.get("110519"), // ++ VIU Exchange Bay B VIU
								Stops.ALL_STOPS.get("110005"), Stops2.ALL_STOPS2.get("110005"), // != Metral 6300 block
								Stops.ALL_STOPS.get("109881"), Stops2.ALL_STOPS2.get("109881"), // != Brechin at Beach
								Stops.ALL_STOPS.get("110215"), Stops2.ALL_STOPS2.get("110215"), // != Norwell at Victoria
								Stops.ALL_STOPS.get("110006"), Stops2.ALL_STOPS2.get("110006"), // == Metral at Enterprise
								Stops.ALL_STOPS.get("109925"), Stops2.ALL_STOPS2.get("109925"), // Woodgrove Centre Exchange Bay D
						})) //
				.addTripSort(StrategicMappingCommons.CLOCKWISE_1, //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("109925"), Stops2.ALL_STOPS2.get("109925"), // Woodgrove Centre Exchange Bay D
								Stops.ALL_STOPS.get("110516"), Stops2.ALL_STOPS2.get("110516"), // ++ Country Club Exchange Bay A
								Stops.ALL_STOPS.get("109880"), Stops2.ALL_STOPS2.get("109880"), // Departure Bay Ferry
						})) //
				.compileBothTripSort());
		map2.put(88L, new RouteTripSpec(88L, //
				StrategicMappingCommons.OUTBOUND_0, MTrip.HEADSIGN_TYPE_STRING, "Parksville", //
				StrategicMappingCommons.OUTBOUND_1, MTrip.HEADSIGN_TYPE_STRING, "Wembley Mall") //
				.addTripSort(StrategicMappingCommons.OUTBOUND_0, //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("110299"), Stops2.ALL_STOPS2.get("110299"), // Jensen Ave E at Craig
								Stops.ALL_STOPS.get("110441"), Stops2.ALL_STOPS2.get("110441"), // Pym St N at Jenkins
								Stops.ALL_STOPS.get("104168"), Stops2.ALL_STOPS2.get("104168"), // Wembley Mall AT Wembley Rd
						})) //
				.addTripSort(StrategicMappingCommons.OUTBOUND_1, //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("104168"), Stops2.ALL_STOPS2.get("104168"), // Wembley Mall AT Wembley Rd
								Stops.ALL_STOPS.get("110280"), Stops2.ALL_STOPS2.get("110280"), // Finholm St S at Morison
								Stops.ALL_STOPS.get("110299"), Stops2.ALL_STOPS2.get("110299"), // Jensen Ave E at Craig
						})) //
				.compileBothTripSort());
		map2.put(97L, new RouteTripSpec(97L, //
				StrategicMappingCommons.COUNTERCLOCKWISE_0, MTrip.HEADSIGN_TYPE_STRING, "Ravensong", // "West", //
				StrategicMappingCommons.COUNTERCLOCKWISE_1, MTrip.HEADSIGN_TYPE_STRING, "East") // Eaglecrest") //
				.addTripSort(StrategicMappingCommons.COUNTERCLOCKWISE_0, //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("110376"), Stops2.ALL_STOPS2.get("110376"), // Eastbound Sunrise at Drew
								Stops.ALL_STOPS.get("104080"), Stops2.ALL_STOPS2.get("104080"), // Westbound Pintail at Eaglecrest Dr
								Stops.ALL_STOPS.get("104113"), Stops2.ALL_STOPS2.get("104113"), // Southbound Eaglecrest farside Mallard
								Stops.ALL_STOPS.get("104122"), Stops2.ALL_STOPS2.get("104122"), // ++
								Stops.ALL_STOPS.get("110358"), Stops2.ALL_STOPS2.get("110358"), // Southbound Jones at Fern Rd W
						})) //
				.addTripSort(StrategicMappingCommons.COUNTERCLOCKWISE_1, //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("110358"), Stops2.ALL_STOPS2.get("110358"), // Southbound Jones at Fern Rd W
								Stops.ALL_STOPS.get("104060"), Stops2.ALL_STOPS2.get("104060"), // ++
								Stops.ALL_STOPS.get("104061"), Stops2.ALL_STOPS2.get("104061"), // ++
								Stops.ALL_STOPS.get("110376"), Stops2.ALL_STOPS2.get("110376"), // Eastbound Sunrise at Drew
						})) //
				.compileBothTripSort());
		map2.put(98L, new RouteTripSpec(98L, //
				StrategicMappingCommons.CLOCKWISE_0, MTrip.HEADSIGN_TYPE_STRING, "Ravensong", //
				StrategicMappingCommons.CLOCKWISE_1, MTrip.HEADSIGN_TYPE_STRING, "Island Hwy W") // "Qualicum Beach") //
				.addTripSort(StrategicMappingCommons.CLOCKWISE_0, //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("104141"), Stops2.ALL_STOPS2.get("104141"), // Westbound Island Hwy W at 2711 Blk
								Stops.ALL_STOPS.get("104146"), Stops2.ALL_STOPS2.get("104146"), // Westbound Island Hwy W ACR Beach Dr
								Stops.ALL_STOPS.get("104147"), Stops2.ALL_STOPS2.get("104147"), // == Southbound Garrett at Parkridge
								Stops.ALL_STOPS.get("104138"), Stops2.ALL_STOPS2.get("104138"), // != Southbound Garrett at Garrett Turn-About
								Stops.ALL_STOPS.get("104149"), Stops2.ALL_STOPS2.get("104149"), // == Eastbound Canyon at 727
								Stops.ALL_STOPS.get("110358"), Stops2.ALL_STOPS2.get("110358"), // Southbound Jones at Fern Rd W
						})) //
				.addTripSort(StrategicMappingCommons.CLOCKWISE_1, //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("110358"), Stops2.ALL_STOPS2.get("110358"), // Southbound Jones at Fern Rd W
								Stops.ALL_STOPS.get("104134"), Stops2.ALL_STOPS2.get("104134"), // ++
								Stops.ALL_STOPS.get("104140"), Stops2.ALL_STOPS2.get("104140"), // Eastbound Crescent Rd W at Memorial
								Stops.ALL_STOPS.get("104141"), Stops2.ALL_STOPS2.get("104141"), // Westbound Island Hwy W at 2711 Blk
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
		if (MTrip.mergeEmpty(mTrip, mTripToMerge)) {
			return true;
		}
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
					"A " + VI_UNIVERSITY_SHORT, //
					VI_UNIVERSITY_SHORT + " Only", //
					VI_UNIVERSITY_SHORT + "-", //
					VI_UNIVERSITY_SHORT //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(VI_UNIVERSITY_SHORT, mTrip.getHeadsignId()); //
				return true;
			}
			if (Arrays.asList( //
					"A " + WOODGROVE, //
					WOODGROVE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(WOODGROVE, mTrip.getHeadsignId()); //
				return true;
			}
		} else if (mTrip.getRouteId() == 20L) {
			if (Arrays.asList( //
					COUNTRY_CLUB, // <>
					DOWNTOWN //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId()); //
				return true;
			} else if (Arrays.asList( //
					COUNTRY_CLUB, // <>
					WOODGROVE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(WOODGROVE, mTrip.getHeadsignId()); //
				return true;
			}
		} else if (mTrip.getRouteId() == 40L) {
			if (Arrays.asList( //
					"School Special", //
					WOODGROVE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(WOODGROVE, mTrip.getHeadsignId()); //
				return true;
			}
		} else if (mTrip.getRouteId() == 91L) {
			if (Arrays.asList( //
					"BC Ferries", //
					WOODGROVE //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(WOODGROVE, mTrip.getHeadsignId()); //
				return true;
			}
		} else if (mTrip.getRouteId() == 99L) {
			if (Arrays.asList( //
					"Duke Pt", //
					"Qualicum Beach" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Qualicum Beach", mTrip.getHeadsignId()); //
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

	private static final Pattern CLEAN_P1 = Pattern.compile("[\\s]*\\([\\s]*");
	private static final String CLEAN_P1_REPLACEMENT = " (";
	private static final Pattern CLEAN_P2 = Pattern.compile("[\\s]*\\)[\\s]*");
	private static final String CLEAN_P2_REPLACEMENT = ") ";

	private static final Pattern BEACH_ = Pattern.compile("((^|\\W){1}(Bch)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String BEACH_REPLACEMENT = "$2" + BEACH + "$4";

	private static final Pattern CINNABAR_ = Pattern.compile("((^|\\W){1}(cinnibar)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String CINNABAR_REPLACEMENT = "$2" + CINNABAR + "$4";

	private static final Pattern SHUTTLE_ = Pattern.compile("((^|\\W){1}(shutlle)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String SHUTTLE_REPLACEMENT = "$2" + "Shuttle" + "$4";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = BEACH_.matcher(tripHeadsign).replaceAll(BEACH_REPLACEMENT);
		tripHeadsign = CINNABAR_.matcher(tripHeadsign).replaceAll(CINNABAR_REPLACEMENT);
		tripHeadsign = SHUTTLE_.matcher(tripHeadsign).replaceAll(SHUTTLE_REPLACEMENT);
		tripHeadsign = EXCHANGE_.matcher(tripHeadsign).replaceAll(EXCHANGE_REPLACEMENT);
		tripHeadsign = VI_UNIVERSITY_.matcher(tripHeadsign).replaceAll(VI_UNIVERSITY_REPLACEMENT);
		tripHeadsign = ENDS_WITH_VIA.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = STARTS_WITH_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CLEAN_P1.matcher(tripHeadsign).replaceAll(CLEAN_P1_REPLACEMENT);
		tripHeadsign = CLEAN_P2.matcher(tripHeadsign).replaceAll(CLEAN_P2_REPLACEMENT);
		tripHeadsign = STARTS_WITH_NUMBER.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern STARTS_WITH_IMPL = Pattern.compile("(^(\\(\\-IMPL\\-\\)))", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_BOUND = Pattern.compile("(^(east|west|north|south)bound)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = STARTS_WITH_IMPL.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = STARTS_WITH_BOUND.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = EXCHANGE_.matcher(gStopName).replaceAll(EXCHANGE_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		return Integer.parseInt(gStop.getStopCode()); // use stop code as stop ID
	}
}
