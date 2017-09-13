package org.dcache.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.io.BaseEncoding;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.padStart;
import static org.dcache.util.ChecksumType.ADLER32;

public class Checksum  implements Serializable
{
    private static final long serialVersionUID = 7338775749513974986L;

    private static final CharMatcher HEXADECIMAL = CharMatcher.anyOf("0123456789abcdef");

    private static final char DELIMITER = ':';

    private final ChecksumType type;
    private final String value;

    /**
     * Creates a new instance of Checksum.
     * @param type The checksum algorithm.
     * @param value The checksum value.
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if the value has the wrong length for
     * the checksum algorithm.
     */
    public Checksum(ChecksumType type, byte[] value)
    {
        this(type, BaseEncoding.base16().lowerCase().encode(value));
    }

    public Checksum(MessageDigest digest)
    {
        this(ChecksumType.getChecksumType(digest.getAlgorithm()), digest.digest());
    }

    /**
     * Creates a new instance of Checksum based on supplied type and a
     * string of the checksum value in hexadecimal.  If the type is ADLER32
     * then the value may omit any leading zeros.
     * @param type The checksum algorithm.
     * @param value The hexadecimal representation of the checksum value.
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if the value contains non-hexadecimal
     * characters or has the wrong length for the checksum type.
     */
    public Checksum(ChecksumType type, String value)
    {
        checkNotNull(type, "type may not be null");
        checkNotNull(value, "value may not be null");

        this.type = type;
        this.value = normalise(value);
    }

    /**
     * Check whether the supplied value is consistent with the given
     * ChecksumType.
     * @param type The checksum algorithm.
     * @param value The checksum value to verify.
     * @return true if value contains only hexadecimal characters and has the
     * correct length for the supplied algorithm.
     */
    public static boolean isValid(ChecksumType type, String value)
    {
        String normalised = normalise(type, value);
        return HEXADECIMAL.matchesAllOf(normalised) &&
            normalised.length() == type.getNibbles();
    }

    private static String normalise(ChecksumType type, String value)
    {
        String normalised = value.trim().toLowerCase();
        /**
         * Due to bug in checksum calculation module, some ADLER32
         * sums are stored without leading zeros.
         */

        if (type == ADLER32) {
            normalised = padStart(normalised, type.getNibbles(), '0');
        }

        return normalised;
    }

    private String normalise(String original) throws IllegalArgumentException
    {
        String normalised = normalise(type, original);

        if (!HEXADECIMAL.matchesAllOf(normalised)) {
            throw new IllegalArgumentException("checksum value \"" +
                    original + "\" contains non-hexadecimal digits");
        }

        if (normalised.length() != type.getNibbles()) {
            throw new IllegalArgumentException(type.getName() + " requires " +
                    type.getNibbles() + " hexadecimal digits but \"" +
                    original + "\" has " + normalised.length());
        }

        return normalised;
    }

    public ChecksumType getType()
    {
        return type;
    }

    public String getValue()
    {
        return value;
    }

    public byte[] getBytes()
    {
        return stringToBytes(value);
    }

    @Override
    public boolean equals(Object other)
    {
        if(other == null){
            return false;
        }

        if (other == this) {
            return true;
        }

        if (!(other.getClass().equals(Checksum.class))) {
            return false;
        }

        Checksum that = (Checksum) other;
        return ((this.type == that.type) && this.value.equals(that.value));
    }

    @Override
    public int hashCode()
    {
        return value.hashCode() ^ type.hashCode();
    }

    @Override
    public String toString()
    {
        return toString(false);
    }

    public String toString(boolean useStringKey)
    {
        return (useStringKey ? type.getName() : String.valueOf(type.getType())) + ':' + value;
    }

    private static byte[] stringToBytes(String str)
    {
        if ((str.length() % 2) != 0) {
            str = '0' + str;
        }

        byte[] r = new byte[str.length() / 2];

        for (int i = 0, l = str.length(); i < l; i += 2) {
            r[i / 2] = (byte) Integer.parseInt(str.substring(i, i + 2), 16);
        }
        return r;
    }

    /**
     * Create a new checksum instance for an already computed digest
     * of a particular type.
     *
     * @param digest the input must have the following format:
     *            <type>:<hexadecimal digest>
     * @throws IllegalArgumentException if argument has wrong form
     * @throws NullPointerException if argument is null
     */
    public static Checksum parseChecksum(String digest)
    {
        checkNotNull(digest, "value may not be null");

        int del = digest.indexOf(DELIMITER);
        if (del < 1) {
            throw new IllegalArgumentException("Not a dCache checksum: " + digest);
        }

        String type = digest.substring(0, del);
        String checksum = digest.substring(del + 1);

        return new Checksum(ChecksumType.getChecksumType(type), checksum);
    }


    /**
     * Returns an {@link Optional} containing checksum of a given type. If
     * no matching checksum type is found, an empty {@link Optional} will be returned.
     * @param checksums to evaluate
     * @param type of checksum
     * @return Optional containing checksum
     */
    public static Optional<Checksum> forType(final Set<Checksum> checksums, final ChecksumType type) {
        return Iterables.tryFind(checksums, t -> t.getType() == type);
    }
}
