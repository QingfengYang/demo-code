package com.meitu.statistics.common.hive.udf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentLengthException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.DoubleObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.IntObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Description(name = "geoHash",
        value = "geoHash(latitude, longitude, numberOfCharacters) - 输入latitude/longitude；生成 geohash 字符串, base32编码长度为8时，精度在19米左右，而当编码长度为9时，精度在2米左右，编码长度需要根据数据情况进行选择",
        extended = "Example:\n " + "  > SELECT geoHash(latitude, longitude, numberOfCharacters);\n" + "ws101p6rdb08")
public class GeoHashUDF extends GenericUDF {

    DoubleObjectInspector doubleOI;
    IntObjectInspector intOI;

    public ObjectInspector initialize(ObjectInspector[] arguments) throws UDFArgumentException {
        if (arguments.length != 3) {
            throw new UDFArgumentLengthException("arrayContainsExample only takes 3 arguments: T, T, int");
        }
        ObjectInspector a = arguments[0];
        ObjectInspector b = arguments[1];
        ObjectInspector c = arguments[2];

        if ( !(a instanceof DoubleObjectInspector) || !(b instanceof DoubleObjectInspector)
                || !(c instanceof IntObjectInspector)) {
            throw new UDFArgumentException("first/second argument must be a double, third argument must be a int");
        }
        this.doubleOI = (DoubleObjectInspector)a;
        this.intOI = (IntObjectInspector)c;

        return PrimitiveObjectInspectorFactory.javaStringObjectInspector;
    }

    public Object evaluate(DeferredObject[] arguments) throws HiveException {
        if (arguments.length != 3) {
            throw new UDFArgumentLengthException("arrayContainsExample only takes 3 arguments: T, T, int");
        }
        double latitude = this.doubleOI.get(arguments[0].get());
        double longitude = this.doubleOI.get(arguments[1].get());
        int numberOfCharacters = this.intOI.get(arguments[2].get());
        if (numberOfCharacters > 12)
            numberOfCharacters = 12;
        if (numberOfCharacters < 1)
            numberOfCharacters = 1;

        GeoHash geoHash = GeoHash.withCharacterPrecision(latitude, longitude, numberOfCharacters);
        //
        return geoHash.toBase32();
    }

    public String getDisplayString(String[] children) {
        return "GeoHashUDF";
    }
}

final class GeoHash implements Comparable<GeoHash>, Serializable {

    private static final int MAX_BIT_PRECISION = 64;
    private static final int MAX_CHARACTER_PRECISION = 12;

    private static final long serialVersionUID = -8553214249630252175L;
    private static final int[] BITS = { 16, 8, 4, 2, 1 };
    private static final int BASE32_BITS = 5;
    public static final long FIRST_BIT_FLAGGED = 0x8000000000000000l;
    private static final char[] base32 = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

    private final static Map<Character, Integer> decodeMap = new HashMap<>();

    static {
        int sz = base32.length;
        for (int i = 0; i < sz; i++) {
            decodeMap.put(base32[i], i);
        }
    }
    protected long bits = 0;
    private WGS84Point point;

    private BoundingBox boundingBox;
    protected byte significantBits = 0;

    private GeoHash(double latitude, double longitude, int desiredPrecision) {
        point = new WGS84Point(latitude, longitude);
        desiredPrecision = Math.min(desiredPrecision, MAX_BIT_PRECISION);

        boolean isEvenBit = true;
        double[] latitudeRange = { -90, 90 };
        double[] longitudeRange = { -180, 180 };

        while (significantBits < desiredPrecision) {
            if (isEvenBit) {
                divideRangeEncode(longitude, longitudeRange);
            } else {
                divideRangeEncode(latitude, latitudeRange);
            }
            isEvenBit = !isEvenBit;
        }

        setBoundingBox(this, latitudeRange, longitudeRange);
        bits <<= (MAX_BIT_PRECISION - desiredPrecision);
    }

    private void divideRangeEncode(double value, double[] range) {
        double mid = (range[0] + range[1]) / 2;
        if (value >= mid) {
            addOnBitToEnd();
            range[0] = mid;
        } else {
            addOffBitToEnd();
            range[1] = mid;
        }
    }

    private static void setBoundingBox(GeoHash hash, double[] latitudeRange, double[] longitudeRange) {
        hash.boundingBox = new BoundingBox(new WGS84Point(latitudeRange[0], longitudeRange[0]), new WGS84Point(
                latitudeRange[1],
                longitudeRange[1]));
    }

    protected final void addOnBitToEnd() {
        significantBits++;
        bits <<= 1;
        bits = bits | 0x1;
    }

    protected final void addOffBitToEnd() {
        significantBits++;
        bits <<= 1;
    }

    /**
     * get the base32 string for this {@link GeoHash}.<br>
     * this method only makes sense, if this hash has a multiple of 5
     * significant bits.
     *
     * @throws IllegalStateException
     *             when the number of significant bits is not a multiple of 5.
     */
    public String toBase32() {
        if (significantBits % 5 != 0) {
            throw new IllegalStateException("Cannot convert a geohash to base32 if the precision is not a multiple of 5.");
        }
        StringBuilder buf = new StringBuilder();

        long firstFiveBitsMask = 0xf800000000000000l;
        long bitsCopy = bits;
        int partialChunks = (int) Math.ceil(((double) significantBits / 5));

        for (int i = 0; i < partialChunks; i++) {
            int pointer = (int) ((bitsCopy & firstFiveBitsMask) >>> 59);
            buf.append(base32[pointer]);
            bitsCopy <<= 5;
        }
        return buf.toString();
    }

    @Override
    public int compareTo(GeoHash o) {
        int bitsCmp = Long.compare(bits ^ FIRST_BIT_FLAGGED, o.bits ^ FIRST_BIT_FLAGGED);
        if (bitsCmp != 0) {
            return bitsCmp;
        } else {
            return Integer.compare(significantBits, o.significantBits);
        }
    }

    /**
     * This method uses the given number of characters as the desired precision
     * value. The hash can only be 64bits long, thus a maximum precision of 12
     * characters can be achieved.
     */
    public static GeoHash withCharacterPrecision(double latitude, double longitude, int numberOfCharacters) {
        if (numberOfCharacters > MAX_CHARACTER_PRECISION) {
            throw new IllegalArgumentException("A geohash can only be " + MAX_CHARACTER_PRECISION + " character long.");
        }
        int desiredPrecision = (numberOfCharacters * 5 <= 60) ? numberOfCharacters * 5 : 60;
        return new GeoHash(latitude, longitude, desiredPrecision);
    }
}

/**
 * {@link WGS84Point} encapsulates coordinates on the earths surface.<br>
 * Coordinate projections might end up using this class...
 */
final class WGS84Point implements Serializable {
    private static final long serialVersionUID = 7457963026513014856L;
    private final double longitude;
    private final double latitude;

    public WGS84Point(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            throw new IllegalArgumentException("The supplied coordinates " + this + " are out of range.");
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return String.format("(" + latitude + "," + longitude + ")");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WGS84Point) {
            WGS84Point other = (WGS84Point) obj;
            return latitude == other.latitude && longitude == other.longitude;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 42;
        long latBits = Double.doubleToLongBits(latitude);
        long lonBits = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (latBits ^ (latBits >>> 32));
        result = 31 * result + (int) (lonBits ^ (lonBits >>> 32));
        return result;
    }
}

final class BoundingBox implements Serializable {
    private static final long serialVersionUID = -7145192134410261076L;
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;

    /**
     * create a bounding box defined by two coordinates
     */
    public BoundingBox(WGS84Point p1, WGS84Point p2) {
        this(p1.getLatitude(), p2.getLatitude(), p1.getLongitude(), p2.getLongitude());
    }

    public BoundingBox(double y1, double y2, double x1, double x2) {
        minLon = Math.min(x1, x2);
        maxLon = Math.max(x1, x2);
        minLat = Math.min(y1, y2);
        maxLat = Math.max(y1, y2);
    }

    public WGS84Point getUpperLeft() {
        return new WGS84Point(maxLat, minLon);
    }

    public WGS84Point getLowerRight() {
        return new WGS84Point(minLat, maxLon);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BoundingBox) {
            BoundingBox that = (BoundingBox) obj;
            return minLat == that.minLat && minLon == that.minLon && maxLat == that.maxLat && maxLon == that.maxLon;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + hashCode(minLat);
        result = 37 * result + hashCode(maxLat);
        result = 37 * result + hashCode(minLon);
        result = 37 * result + hashCode(maxLon);
        return result;
    }

    private static int hashCode(double x) {
        long f = Double.doubleToLongBits(x);
        return (int) (f ^ (f >>> 32));
    }

    @Override
    public String toString() {
        return getUpperLeft() + " -> " + getLowerRight();
    }
}