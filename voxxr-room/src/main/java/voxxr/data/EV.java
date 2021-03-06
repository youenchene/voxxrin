package voxxr.data;

import voxxr.web.RoomResource;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * User: xavierhanin
 * Date: 2/26/12
 * Time: 9:01 AM
 */
public class EV {

    public static enum Type {
        RATE("R", 0, RoomResource.BroadcastMode.DASHBOARD, RoomResource.BroadcastMode.USER_DELAYED) {
            private final Pattern REGEX = Pattern.compile("^R\\d+$");
            @Override
            public boolean accept(String valueWithType) {
                return REGEX.matcher(valueWithType).matches();
            }

            @Override
            public int getHotFactorPoints(String value) {
                return Integer.parseInt(value);
            }
        },
        FEELING("F", 0, RoomResource.BroadcastMode.DASHBOARD, RoomResource.BroadcastMode.USER_DELAYED) {
            @Override
            public int getHotFactorPoints(String value) {
                return "A".equals(value) ? 8 :
                        ("W".equals(value) ? 3 :
                                ("Y".equals(value) ? -5 : 0));
            }
        },
        POKE("PK", 1,  RoomResource.BroadcastMode.DASHBOARD, RoomResource.BroadcastMode.USER),
        IN("IN", 1,  RoomResource.BroadcastMode.DASHBOARD, RoomResource.BroadcastMode.USER_DELAYED),
        OUT("OUT", -1,  RoomResource.BroadcastMode.DASHBOARD, RoomResource.BroadcastMode.USER_DELAYED),
        CONNECTION("C", 0, RoomResource.BroadcastMode.DASHBOARD, RoomResource.BroadcastMode.USER_DELAYED),
        TITLE("T", 0, RoomResource.BroadcastMode.DASHBOARD, RoomResource.BroadcastMode.USER),
        HOT_FACTOR("H", 0, RoomResource.BroadcastMode.DASHBOARD),
        POLL_START("PS", 10, RoomResource.BroadcastMode.DASHBOARD, RoomResource.BroadcastMode.USER),
        POLL_END("PE", 10, RoomResource.BroadcastMode.DASHBOARD, RoomResource.BroadcastMode.USER),
        POLL_VOTE("PV", 4),
        ROOM_START("RS"), ROOM_END("RE"),
        PREZ_START("PZS"), PREZ_END("PZE"),
        UNKNOWN("");

        public static Type getByCode(String code) {
            for (Type type : values()) {
                if (type.getCode().equals(code)) {
                    return type;
                }
            }
            return null;
        }

        private String code;
        private int hotFactorPoints = 0;
        private RoomResource.BroadcastMode[] bcModes = new RoomResource.BroadcastMode[] {
                RoomResource.BroadcastMode.DASHBOARD};

        Type(String code) {
            this.code = code;
        }
        Type(String code, int hotFactorPoints) {
            this.code = code;
            this.hotFactorPoints = hotFactorPoints;
        }
        Type(String code, int hotFactorPoints, RoomResource.BroadcastMode... modes) {
            this.code = code;
            this.hotFactorPoints = hotFactorPoints;
            this.bcModes = modes;
        }

        public String getCode() {
            return code;
        }

        public RoomResource.BroadcastMode[] getBcModes() {
            return bcModes;
        }

        public boolean accept(String valueWithType) {
            return valueWithType.startsWith(code);
        }

        public String getValueIn(String valueWithType) {
            return valueWithType.substring(code.length());
        }

        public int getHotFactorPoints(String value) {
            return hotFactorPoints;
        }
    }

    public static EV parse(String pres, String evBC) {
        String[] parts = evBC.split("\\|");
        for (EV.Type type : EV.Type.values()) {
            if (type.accept(parts[1])) {
                return new EV(pres, parts[0], type, type.getValueIn(parts[1]));
            }
        }
        throw new IllegalArgumentException("invalid EV: " + evBC + ": no matched EV.Type");
    }

    private final UUID key;
    private final String pres;
    private final String user;
    private final Type type;
    private final String value;
    private final long timestamp;

    public EV(String pres, String user, Type type, String value) {
        this(UUID.randomUUID(), pres, user, type, value, System.currentTimeMillis());
    }

    public EV(UUID key, String pres, String user, Type type, String value, long timestamp) {
        this.key = key;
        this.pres = pres;
        this.user = user;
        this.type = type;
        this.value = value;
        this.timestamp = timestamp;
    }

    public int getHotFactorPoints() {
        return type.getHotFactorPoints(this.value);
    }

    public UUID getKey() {
        return key;
    }

    public String getPres() {
        return pres;
    }

    public String getUser() {
        return user;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String toBC() {
        return user + '|' + type.getCode() + value;
    }

    public RoomResource.BroadcastMode[] getBCModes() {
        return getType().getBcModes();
    }

    @Override
    public String toString() {
        return "EV{" +
                "room='" + pres + '\'' +
                ", user='" + user + '\'' +
                ", type=" + type +
                ", value='" + value + '\'' +
                '}';
    }
}
