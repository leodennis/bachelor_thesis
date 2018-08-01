import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class to replace strings and dates with unique numbers (same strings and dates get the same number).
 * Also provides methods to convert them back.
 * Default Date format is yyyy-MM-dd and can be changed by using setDateFormat.
 *
 * @author Leo Knoll
 */
public class StringReplacer {

    //Column types
    public final static int TYPE_NUMBER = 0;
    public final static int TYPE_STRING = 1;
    public final static int TYPE_DATE = 2;
    public final static int TYPE_IGNORE = 3;

    private int[] types;

    //Replacers for each column
    private ItemReplacer[] replacers;

    //Date format parser
    private static final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd");
    public static void setDateFormat(String pattern) {
        new SimpleDateFormat(pattern);
    }


    /**
     * Initializes StringReplacer object.
     * Assertion: columns == types.length
     * @param columns The total number of columns.
     * @param types The types for each column.
     */
    public StringReplacer(int columns, int[] types) {
        this.types = types;

        //Initialize replacer array
        replacers = new ItemReplacer[columns];
        for (int i = 0; i < columns; i++) {
            if (types[i] == TYPE_STRING) { //only string are replace by ItemReplacer
                replacers[i] = new ItemReplacer();
            }
        }
    }

    /**
     * Replaces string and date columns with numbers.
     * @param strings The array of strings to be processed.
     * @return An array with replaced strings and dates.
     */
    public String[] replace(String[] strings) {
        String[] replacementArray = strings.clone();
        for (int i = 0; i < replacementArray.length; i++) {
            if (types[i] == TYPE_STRING) {
                replacementArray[i] = replacers[i].replace(replacementArray[i]);
            } else if (types[i] == TYPE_DATE) {
                try {
                    replacementArray[i] = String.valueOf(dateParser.parse(replacementArray[i]).getTime());
                } catch (ParseException e) {
                    throw new RuntimeException("Date format is not " + dateParser.toPattern());
                }
            }
        }
        return replacementArray;
    }

    /**
     * Replaces all null references and empty values buy a replacement string.
     * @param strings The array of strings to be processed.
     * @param replacement The replacement for null or empty values.
     * @param onlyNumberColumns Set true if only number columns should be replaced
     * @return The corrected array.
     */
    public String[] fillNullOrEmpty(String[] strings, String replacement, boolean onlyNumberColumns) {
        String[] replacementArray = strings.clone();
        for (int i = 0; i < replacementArray.length; i++) {
            if (onlyNumberColumns && types[i] != TYPE_NUMBER) {
                continue;
            }
            if (replacementArray[i] == null || replacementArray[i].trim().isEmpty()) {
                replacementArray[i] = replacement;
            }
        }
        return replacementArray;
    }

    /**
     * Converts back the original strings and dates.
     * @param strings The array of strings to be processed.
     * @return An array with the original strings and dates.
     */
    public String[] convertBack(String[] strings) {
        String[] replacementArray = strings.clone();
        for (int i = 0; i < replacementArray.length; i++) {
            if (types[i] == TYPE_STRING) {
                replacementArray[i] = replacers[i].getString(replacementArray[i]);
            } else if (types[i] == TYPE_DATE) {
                Date date = new Date(Long.valueOf(replacementArray[i]));
                replacementArray[i] = dateParser.format(date);
            }
        }
        return replacementArray;
    }

    /**
     * Class which handles replacements of one column.
     */
    class ItemReplacer {
        private int n = 2; //continuous numerating, starting at 2 because of reserved boolean logic
        private BiMap<Integer, String> replacementMap = HashBiMap.create();

        /**
         * Initializes ItemReplacer object.
         */
        ItemReplacer() {
            //Add default boolean replacement
            replacementMap.put(0, "0"); //Add replacement for 0 is "0"
            replacementMap.put(1, "1"); //Add replacement for 1 is "1"
        }

        /**
         * Assigns to each string a unique number (same strings get the same number).
         * @param string The string to be replaced by a number.
         * @return The associated number.
         */
        private String replace(String string) {
            if (!replacementMap.containsValue(string)) {
                replacementMap.put(n, string);
                return String.valueOf(n++); //increment and return old value
            } else {
                return String.valueOf(replacementMap.inverse().get(string));
            }
        }

        /**
         * Converts a number back to the original string.
         * @param key The number to be replaced by the original string.
         * @return The original String.
         */
        private String getString(String key) {
            return replacementMap.get(Integer.valueOf(key));
        }
    }
}


