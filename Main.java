import java.security.MessageDigest;  // Importing the MessageDigest class for generating MD5 hashes
import java.security.NoSuchAlgorithmException;  // Importing exception for handling no such algorithm scenario
import java.util.HashMap;  // Importing HashMap for mapping URLs
import java.util.HashSet;  // Importing HashSet for storing unique short URLs
import java.util.Set;  // Importing Set interface

// URL Shortener Service

public class Main {
    public static class URLShortenerService {
        private HashMap<String, String> ltos;  // Maps long URLs to their corresponding short URLs
        private HashMap<String, String> stol;  // Maps short URLs back to their corresponding long URLs
        private ShorteningStrategy strategy;    // Strategy used for shortening URLs

        // Constructor for URLShortenerService, which initializes the mappings and sets the shortening strategy
        public URLShortenerService(ShorteningStrategy strategy) {
            this.ltos = new HashMap<>();  // Initialize the long-to-short URL mapping
            this.stol = new HashMap<>();  // Initialize the short-to-long URL mapping
            this.strategy = strategy;      // Set the URL shortening strategy
        }

        // Method to shorten a long URL
        public String longToShort(String longURL) {
            // Check if the URL is already shortened
            if (ltos.containsKey(longURL)) {
                return ltos.get(longURL);  // Return the already shortened URL
            }
            String shortURL = strategy.shorten(longURL);  // Use the strategy to generate a short URL
            ltos.put(longURL, shortURL);  // Store the mapping of long URL to short URL
            stol.put(shortURL, longURL);   // Store the mapping of short URL back to long URL
            return shortURL;  // Return the newly created short URL
        }

        // Method to expand a short URL back to the long URL
        public String shortToLong(String shortURL) {
            return stol.get(shortURL);  // Retrieve and return the long URL corresponding to the short URL
        }
    }

    // Shortening Strategy Interface
    interface ShorteningStrategy {
        String shorten(String longURL);  // Method signature for shortening a long URL
    }

    // Base62 Shortening Strategy
    static class Base62ShorteningStrategy implements ShorteningStrategy {
        private static final String elements = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";  // Base62 characters
        private static int COUNTER = 1000000000;  // Static counter to generate unique short URLs

        @Override
        public String shorten(String longURL) {
            StringBuilder sb = new StringBuilder();  // StringBuilder to construct the short URL
            int n = COUNTER++;  // Get the current counter value and increment it
            while (n != 0) {
                sb.insert(0, elements.charAt(n % 62));  // Convert number to Base62 and prepend to the string
                n /= 62;  // Divide by 62 to prepare for the next digit
            }
            // Ensure the short URL is at least 7 characters long by prepending '0's
            while (sb.length() < 7) {
                sb.insert(0, '0');
            }
            return "http://tiny.url/" + sb.toString();  // Return the final short URL with the base URL
        }
    }

    // MD5 Shortening Strategy
    static class MD5ShorteningStrategy implements ShorteningStrategy {
        private static final int SHORT_URL_CHAR_SIZE = 7;  // Length of the short URL segment

        @Override
        public String shorten(String longURL) {
            String hash = convert(longURL);  // Generate an MD5 hash of the long URL
            for (int i = 0; i < hash.length() - SHORT_URL_CHAR_SIZE; i++) {
                String shortUrl = hash.substring(i, i + SHORT_URL_CHAR_SIZE);  // Create a short URL from the hash
                if (!DB.exists(shortUrl)) {  // Check if the short URL already exists in the database
                    DB.save(shortUrl);  // Save the new short URL to the mock database
                    return "http://tiny.url/" + shortUrl;  // Return the final short URL
                }
            }
            throw new RuntimeException("Unable to generate unique short URL");  // Throw an error if no unique short URL can be created
        }

        private String convert(String longURL) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");  // Create an MD5 digest instance
                digest.update(longURL.getBytes());  // Update the digest with the bytes of the long URL
                byte[] messageDigest = digest.digest();  // Compute the hash
                StringBuilder hexString = new StringBuilder();  // StringBuilder to hold the hex string
                for (byte b : messageDigest) {
                    hexString.append(String.format("%02x", b));  // Format the hash as a hex string
                }
                return hexString.toString();  // Return the hex representation of the hash
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);  // Throw an error if the MD5 algorithm is not available
            }
        }
    }

    // Mock Database for storing URLs
    static class DB {
        private static Set<String> storedUrls = new HashSet<>();  // Set to hold stored short URLs

        public static boolean exists(String shortUrl) {
            return storedUrls.contains(shortUrl);  // Check if the short URL already exists in the mock database
        }

        public static void save(String shortUrl) {
            storedUrls.add(shortUrl);  // Add the new short URL to the mock database
        }
    }

    // Facade to simplify usage of URL shortener service
    static class URLShortenerFacade {
        private URLShortenerService service;  // Instance of the URL shortener service

        public URLShortenerFacade(ShorteningStrategy strategy) {
            this.service = new URLShortenerService(strategy);  // Initialize the service with the provided strategy
        }

        public String shorten(String longURL) {
            return service.longToShort(longURL);  // Delegate the shortening of the long URL to the service
        }

        public String expand(String shortURL) {
            return service.shortToLong(shortURL);  // Delegate the expansion of the short URL to the service
        }
    }

// Main class to test the functionality

    public static void main(String[] args) {
        // Example usage with Base62 strategy
        ShorteningStrategy base62Strategy = new Base62ShorteningStrategy();  // Create a Base62 shortening strategy
        URLShortenerFacade base62Facade = new URLShortenerFacade(base62Strategy);  // Create a facade using the strategy

        String longUrl = "https://www.example.com/some/very/long/url";  // Example long URL
        String shortUrlBase62 = base62Facade.shorten(longUrl);  // Shorten the long URL
        System.out.println("Base62 Short URL: " + shortUrlBase62);  // Print the short URL
        System.out.println("Expanded URL: " + base62Facade.expand(shortUrlBase62));  // Expand the short URL back to long URL

        // Example usage with MD5 strategy
        ShorteningStrategy md5Strategy = new MD5ShorteningStrategy();  // Create an MD5 shortening strategy
        URLShortenerFacade md5Facade = new URLShortenerFacade(md5Strategy);  // Create a facade using the MD5 strategy

        String shortUrlMD5 = md5Facade.shorten(longUrl);  // Shorten the long URL using MD5
        System.out.println("MD5 Short URL: " + shortUrlMD5);  // Print the MD5 short URL
        System.out.println("Expanded URL: " + md5Facade.expand(shortUrlMD5));  // Expand the MD5 short URL back to long URL
    }
}