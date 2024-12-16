package comp3911.cwk2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Auth {
  private static final String CONNECTION_URL = "jdbc:sqlite:db.sqlite3";

  /**
   * Separator between salt and hash in secure password
   */
  private static String saltHashSeparator = "@@@@@";

  /**
   * Create a secure password from a password and salt
   * 
   * @param password
   * @param salt
   * @return secure password
   */
  private static String createSecurePassword(String password, String salt) {
    String hash = hashPassword(password, salt);
    String securePassword = salt + saltHashSeparator + hash;
    return securePassword;
  }

  /**
   * Create a secure password from a password and a randomly generated salt
   * 
   * @param password
   * @return secure password
   */
  public static String createSecurePassword(String password) {
    String salt = createSalt();
    return createSecurePassword(password, salt);
  }

  /**
   * Check if a password matches a secure password
   * 
   * @param password
   * @param securePassword
   * @return true if matches
   */
  public static boolean passwordMatches(String password, String securePassword) {
    String salt = getSaltFromSecurePassword(securePassword);
    String newSecurePassword = createSecurePassword(password, salt);
    return newSecurePassword.equals(securePassword);
  }

  /**
   * Hashes password with salt
   * 
   * @param password
   * @param salt
   * @return hashed password
   */
  private static String hashPassword(String password, String salt) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      md.reset();
      md.update(salt.getBytes(StandardCharsets.UTF_8));
      md.update(password.getBytes(StandardCharsets.UTF_8));
      byte[] hash = md.digest();
      String result = new String(hash, StandardCharsets.UTF_8);
      return result;
    } catch (Exception error) {
      return "";
    }
  }

  /**
   * Create salt
   * 
   * @return
   */
  private static String createSalt() {
    byte[] salt = new byte[16];
    SecureRandom random = new SecureRandom();
    random.nextBytes(salt);
    return new String(salt, StandardCharsets.UTF_8);
  }

  /**
   * Retrieve the salt part of a secure password
   * 
   * @param securePassword
   * @return
   */
  private static String getSaltFromSecurePassword(String securePassword) {
    return securePassword.split(saltHashSeparator)[0];
  }

  public static void convertPasswords() throws SQLException {
    Connection database = DriverManager.getConnection(CONNECTION_URL);

    try (Statement stmt = database.createStatement()) {
      ResultSet results = stmt.executeQuery("Select * from user;");

      while (results.next()) {
        String username = results.getString("username");
        String password = results.getString("password");

        // they likely already have been converted to use secure passwords
        if (password.contains(saltHashSeparator)) {
          System.out.println(username + ": already converted");
          continue;
        }

        String securePassword = createSecurePassword(password);

        String query = "UPDATE user SET password = ? WHERE username = ?;";
        PreparedStatement statement = database.prepareStatement(query);
        statement.setString(1, securePassword);
        statement.setString(2, username);

        statement.executeUpdate();
        System.out.println(username + ": converted");
      }
    }

    database.close();

  }

  /**
   * CLI for testing
   * 
   * @param args password(s)
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Usage: java Auth <password> [password]...");
      System.exit(1);
      return;
    }

    if (args[0].equals("convert")) {
      try {
        Auth.convertPasswords();
        return;
      } catch (Exception e) {
        System.out.println(e.getMessage());
        System.exit(1);
        return;
      }
    }

    for (String arg : args) {
      String securePassword = Auth.createSecurePassword(arg);
      System.out.println(securePassword);
    }
  }
}
