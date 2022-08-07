package project;

import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Listing {
    private int lid;
    private String type;
    private double latitude;
    private double longitude;
    private Address address;

    private String aux;

    public Listing(int lid, String type, double latitude, double longitude, Address address, String aux) {
        this.lid = lid;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.aux = aux;
    }

    public Listing(int lid, String type, double latitude, double longitude, Address address) {
        this.lid = lid;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public static int createListing(DAO dao, int hid, String type, double latitude, double longitude, String address,
                                    String city, String country, String postalCode)
            throws IllegalArgumentException, SQLException {

        Listing.validateParameters(type, latitude, longitude, address, city, country, postalCode);

        int aid = dao.createAddress(address.toLowerCase(Locale.ROOT).trim(), city.toLowerCase(Locale.ROOT).trim(),
                country.toLowerCase(Locale.ROOT).trim(), postalCode.toLowerCase(Locale.ROOT).trim());
        int lid = dao.createListing(hid, type, latitude, longitude, aid);
        return lid;
    }

    public static void validateParameters(String type, double latitude, double longitude, String address, String city,
                                          String country, String postalCode) throws IllegalArgumentException {
        if (type.isEmpty() || address.isEmpty() || city.isEmpty() || country.isEmpty() || postalCode.isEmpty() ||
                (!type.equals("house") && !type.equals("apartment") && !type.equals("guesthouse") && !type.equals("hotel"))) {
            throw new IllegalArgumentException();
        }
    }

    /* Displays the report for the most commonly used NPs in reviews for listings */
    public static void displayNPs(DAO dao) {
        try {
            // set up the parser
            InputStream modelFile = new FileInputStream("en-parser-chunking.bin");
            ParserModel model = new ParserModel(modelFile);
            Parser parser = ParserFactory.create(model);

            // get review info
            ResultSet reviews = dao.getAllListingReviews();
            LinkedHashMap<Integer, Integer> listingReviewNum = dao.getListingReviewNum();

            Set<Integer> keys = listingReviewNum.keySet();

            // loop through listingReviewNum, which stores listing to number of reviews
            for (Integer key : keys) {
                // get lid and review number of the current listing
                int lid = key;
                int numReviews = listingReviewNum.get(key);

                // loop through reviews `numReviews` times to get all this listing's reviews
                // combine into one hashmap of NPs to number of occurrences
                HashMap<String, Integer> nounCount = new HashMap<>();
                for (int i = 0; i < numReviews; i++) {
                    // get individual review
                    reviews.next();
                    String review = reviews.getString("Review");

                    // split the review into sentences
                    String[] split = review.split("[.!?]");

                    // loop through sentences and parse them
                    for (int j = 0; j < split.length; j++) {
                        Parse parses[] = ParserTool.parseLine(split[j], parser, 1);

                        // call method to extract the NPs
                        for (Parse p : parses) {
                            getNounPhrases(p, nounCount);
                        }
                    }
                }

                // print out the top NPs for this listing
                System.out.println("LISTING ID: " + lid);

//                Object[] sorted = nounCount.entrySet().stream()
//                                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).toArray();

                List<Map.Entry<String, Integer>> list = new LinkedList<>(nounCount.entrySet());
                Collections.sort(list, Collections.reverseOrder(Map.Entry.comparingByValue()));
                int i = 0;
                for (Map.Entry<String, Integer> cur : list) {
                    System.out.println("\tPhrase: " + cur.getKey() + "; Frequency: " + cur.getValue());
                    if (i == 4) {
                        break;
                    }
                    i++;
                }

            }

        } catch (SQLException sql) {
            sql.printStackTrace();
            System.out.println("Issue accessing database");
        } catch (IOException iox) {
            iox.printStackTrace();
        }
    }

    private static void getNounPhrases(Parse p, HashMap<String, Integer> nounCount) {
        if (p.getType().equals("NP")) {
            String np = p.getCoveredText();
            if (nounCount.containsKey(np)) {
                nounCount.put(np, nounCount.get(np) + 1);
            } else {
                nounCount.put(np, 1);
            }
        }

        for (Parse child : p.getChildren()) {
            getNounPhrases(child, nounCount);
        }
    }

    public int getLid() {
        return lid;
    }

    @Override
    public String toString() {
        return type + " at " + address + (aux == null ? "" : " " + aux);
    }
}
