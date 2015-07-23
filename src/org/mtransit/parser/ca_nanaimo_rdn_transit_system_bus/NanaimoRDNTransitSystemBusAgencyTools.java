package org.mtransit.parser.ca_nanaimo_rdn_transit_system_bus;

import java.util.HashSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

// http://bctransit.com/*/footer/open-data
// http://bctransit.com/servlet/bctransit/data/GTFS.zip
// http://bct2.baremetal.com:8080/GoogleTransit/BCTransit/google_transit.zip
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
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating RDN Transit System bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	private static final String INCLUDE_ONLY_SERVICE_ID_CONTAINS = "NAN";

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (INCLUDE_ONLY_SERVICE_ID_CONTAINS != null && !gCalendar.getServiceId().contains(INCLUDE_ONLY_SERVICE_ID_CONTAINS)) {
			return true;
		}
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (INCLUDE_ONLY_SERVICE_ID_CONTAINS != null && !gCalendarDates.getServiceId().contains(INCLUDE_ONLY_SERVICE_ID_CONTAINS)) {
			return true;
		}
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
		if (INCLUDE_ONLY_SERVICE_ID_CONTAINS != null && !gTrip.getServiceId().contains(INCLUDE_ONLY_SERVICE_ID_CONTAINS)) {
			return true;
		}
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

	private static final String AGENCY_COLOR_GREEN = "34B233";// GREEN (from PDF Corporate Graphic Standards)
	private static final String AGENCY_COLOR_BLUE = "002C77"; // BLUE (from PDF Corporate Graphic Standards)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_F78B1F = "F78B1F";
	private static final String COLOR_8CC63F = "8CC63F";
	private static final String COLOR_FB298C = "FB298C";
	private static final String COLOR_4F6F19 = "4F6F19";
	private static final String COLOR_8D0B3A = "8D0B3A";
	private static final String COLOR_77AD98 = "77AD98";
	private static final String COLOR_0077BF = "0077BF";
	private static final String COLOR_DD92C4 = "DD92C4";
	private static final String COLOR_00ADCD = "00ADCD";
	private static final String COLOR_034476 = "034476";
	private static final String COLOR_A0228D = "A0228D";
	private static final String COLOR_FFBB16 = "FFBB16";
	private static final String COLOR_AB5C3C = "AB5C3C";
	private static final String COLOR_B5BB19 = "B5BB19";

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 1: return COLOR_F78B1F;
			case 2: return COLOR_0077BF;
			case 3: return COLOR_8CC63F;
			case 4: return COLOR_DD92C4;
			case 5: return COLOR_00ADCD;
			case 6: return COLOR_034476;
			case 7: return COLOR_FB298C;
			case 8: return COLOR_A0228D;
			case 9: return COLOR_FFBB16;
			case 10: return COLOR_AB5C3C;
			case 12: return COLOR_B5BB19;
			case 15: return COLOR_8D0B3A;
			case 25: return COLOR_77AD98;
			case 90: return COLOR_4F6F19;
			// @formatter:on
			default:
				return AGENCY_COLOR_BLUE;
			}
		}
		return super.getRouteColor(gRoute);
	}

	private static final String CINNABAR = "Cinnabar";
	private static final String CEDAR = "Cedar";
	private static final String BC_FERRIES = "BC Ferries";
	private static final String FERRY_SHUTTLE = "Ferry Shuttle";
	private static final String HAMMOND_BAY = "Hammond Bay";
	private static final String WOODGROVE_CTR = "Woodgrove Ctr";
	private static final String COUNTRY_CLUB = "Country Club";
	private static final String FAIRVIEW = "Fairview";
	private static final String DOWNTOWN = "Downtown";
	private static final String CINNABAR_CEDAR = CINNABAR + " / " + CEDAR;
	private static final String VI_UNIVERSITY = "VI University";
	private static final String WOODGROVE = "Woodgrove";
	private static final String FERRY_SHUTTLE_BC_FERRIES = FERRY_SHUTTLE + " / " + BC_FERRIES;
	private static final String PM = "PM";
	private static final String AM = "AM";
	private static final String DEEP_BAY = "Deep Bay";
	private static final String WEMBLEY_MALL = "Wembley Mall";
	private static final String NANAIMO = "Nanaimo";
	private static final String QUALICUM_BEACH = "Qualicum Beach";
	private static final String NANAIMO_NORTH = "Nanaimo North";
	private static final String DOVER = "Dover";
	private static final String DOVER_NANAIMO_NORTH = DOVER + " / " + NANAIMO_NORTH;

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (mRoute.id == 1l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(DOWNTOWN, gTrip.getDirectionId());
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WOODGROVE, gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.id == 2l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(DOWNTOWN, gTrip.getDirectionId());
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(HAMMOND_BAY, gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.id == 3l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(DOWNTOWN, gTrip.getDirectionId());
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WOODGROVE_CTR, gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.id == 4l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(DOWNTOWN, gTrip.getDirectionId());
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(COUNTRY_CLUB, gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.id == 5l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(FAIRVIEW, gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.id == 6l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(DOWNTOWN, gTrip.getDirectionId());
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(VI_UNIVERSITY, gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.id == 7l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(CINNABAR_CEDAR, gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.id == 8l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		} else if (mRoute.id == 9l) {
			if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			}
		} else if (mRoute.id == 12l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(DOVER_NANAIMO_NORTH, gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.id == 15l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(VI_UNIVERSITY, gTrip.getDirectionId());
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WOODGROVE, gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.id == 25l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(FERRY_SHUTTLE_BC_FERRIES, gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.id == 88l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(DOWNTOWN, gTrip.getDirectionId());
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(WEMBLEY_MALL, gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.id == 93l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(PM, gTrip.getDirectionId());
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(AM, gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.id == 90l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(NANAIMO, gTrip.getDirectionId());
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(QUALICUM_BEACH, gTrip.getDirectionId());
				return;
			}
		} else if (mRoute.id == 99l) {
			if (gTrip.getDirectionId() == 0) {
				mTrip.setHeadsignString(WOODGROVE, gTrip.getDirectionId());
				return;
			} else if (gTrip.getDirectionId() == 1) {
				mTrip.setHeadsignString(DEEP_BAY, gTrip.getDirectionId());
				return;
			}
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	private static final String EXCH = "Exch";
	private static final Pattern EXCHANGE = Pattern.compile("((^|\\W){1}(exchange)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = "$2" + EXCH + "$4";

	private static final Pattern STARTS_WITH_NUMBER = Pattern.compile("(^[\\d]+[\\S]*)", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_VIA = Pattern.compile("( via .*$)", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_TO = Pattern.compile("(^.* to )", Pattern.CASE_INSENSITIVE);

	private static final Pattern AND = Pattern.compile("( and )", Pattern.CASE_INSENSITIVE);
	private static final String AND_REPLACEMENT = " & ";

	private static final Pattern CLEAN_P1 = Pattern.compile("[\\s]*\\([\\s]*");
	private static final String CLEAN_P1_REPLACEMENT = " (";
	private static final Pattern CLEAN_P2 = Pattern.compile("[\\s]*\\)[\\s]*");
	private static final String CLEAN_P2_REPLACEMENT = ") ";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = EXCHANGE.matcher(tripHeadsign).replaceAll(EXCHANGE_REPLACEMENT);
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

	private static final Pattern AT = Pattern.compile("( at )", Pattern.CASE_INSENSITIVE);
	private static final String AT_REPLACEMENT = " / ";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = STARTS_WITH_BOUND.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = AT.matcher(gStopName).replaceAll(AT_REPLACEMENT);
		gStopName = EXCHANGE.matcher(gStopName).replaceAll(EXCHANGE_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
