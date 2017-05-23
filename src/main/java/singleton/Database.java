package singleton;

import logger.Level;
import logger.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class Database
{

    //The database relative URL
    private static final String DB_URL = "src/main/resources/db.sqlite";

    //The timeout for each query
    private static final int QUERY_TIMEOUT = 10;


    //The database singleton instance
    private static Database instance;

    //The connection object towards the server
    private Connection connection;

    /**
     * The constructor of the instance.
     * Initializes a connection towards the sqlite database
     */
    private Database() {

        //Load up the sqlite JDBC Driver (Must be set either manually or with MAVEN)
        try {

            Class.forName("org.sqlite.JDBC");

            //Connect!
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + DB_URL);

        } catch (ClassNotFoundException e) {

            Logger.log(Level.SEVERE, "Database", "JDBC Driver not found", e);

        } catch (SQLException e) {

            Logger.log(Level.SEVERE, "Database::constructor", "SQL Exception", e);

        }

    }

    /**
     * Singleton method to retrieve the object instance
     * @return The database instance
     */
    public static Database getInstance() {

        if (instance == null) {

            instance = new Database();

        }

        return instance;

    }

    /**
     * This method attempts to perform a login with the provided data
     * @param username the username
     * @param password tha password, in plain, that will be hashed with MD5
     * @return true upon login success, false otherwise
     */
    public boolean login(String username, String password) {

        boolean authenticated = false;

        try {

            //Create a statement
            Statement stmt = this.connection.createStatement();

            //Setup the timeout
            stmt.setQueryTimeout(QUERY_TIMEOUT);

            //Create the query;
            String query = "SELECT * FROM users WHERE username = '" + username + "' AND password = '" + this.getMD5Hex(password) + "'";

            //Execute the query
            ResultSet result = stmt.executeQuery(query);

            //Check results
            while (result.next()) {

                //If there is a match, login succeeded
                authenticated = true;

            }

        } catch (SQLException e) {

            Logger.log(Level.SEVERE, "Database::login", "SQL Exception", e);

        } catch (NoSuchAlgorithmException e) {

            Logger.log(Level.SEVERE, "Database::login", "No such digest algorithm", e);

        }

        return authenticated;

    }


    /**
     * Converts a string to an MD5 hash
     * @param inputString the string to be converted
     * @return the hashed string to md5
     * @throws NoSuchAlgorithmException will never occur.
     */
    public String getMD5Hex(final String inputString) throws NoSuchAlgorithmException {

        //Creates a message digest
        MessageDigest md = MessageDigest.getInstance("MD5");

        //Update the message digest
        md.update(inputString.getBytes());

        //Get the final md5 digest
        byte[] digest = md.digest();

        return this.convertByteToHex(digest);
    }

    /**
     * Converts an array of bytes to a hexadecimal string
     * @param byteData the array to be converted
     * @return the converted string
     */
    private String convertByteToHex(byte[] byteData) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

}
