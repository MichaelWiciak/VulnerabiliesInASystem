package comp3911.cwk2;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.sql.PreparedStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@SuppressWarnings("serial")
public class AppServlet extends HttpServlet {
  private static final String CONNECTION_URL = "jdbc:sqlite:db.sqlite3";
  private static final String AUTH_QUERY = "select * from user where username = ?";
  private static final String SEARCH_QUERY = "select * from patient where surname = ? collate nocase";
  private static final String ATTEMPT_UPDATE = "update user set login_attempts = ? where username = ?";
  private static final String TIME_UPDATE = "update user set last_fail = ? where username = ?";

  private final Configuration fm = new Configuration(Configuration.VERSION_2_3_28);
  private Connection database;

  @Override
  public void init() throws ServletException {
    configureTemplateEngine();
    connectToDatabase();

    try {
      // convert passwords in case they are not already converted
      Auth.convertPasswords();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private void configureTemplateEngine() throws ServletException {
    try {
      fm.setDirectoryForTemplateLoading(new File("./templates"));
      fm.setDefaultEncoding("UTF-8");
      fm.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
      fm.setLogTemplateExceptions(false);
      fm.setWrapUncheckedExceptions(true);
    } catch (IOException error) {
      throw new ServletException(error.getMessage());
    }
  }

  private void connectToDatabase() throws ServletException {
    try {
      database = DriverManager.getConnection(CONNECTION_URL);
    } catch (SQLException error) {
      throw new ServletException(error.getMessage());
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      Template template = fm.getTemplate("login.html");
      template.process(null, response.getWriter());
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);
    } catch (TemplateException error) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // Get form parameters
    String username = request.getParameter("username");
    String password = request.getParameter("password");
    String surname = request.getParameter("surname");

    try {

      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);

      // early return if user is locked
      if (!unlocked(username)) {
        Template template = fm.getTemplate("locked.html");
        template.process(null, response.getWriter());
        return;
      }

      // early return if user provided invalid credentials
      if (!authenticated(username, password)) {
        Template template = fm.getTemplate("invalid.html");
        template.process(null, response.getWriter());
        return;
      }

      // Get search results and merge with template
      Map<String, Object> model = new HashMap<>();
      model.put("records", searchResults(surname));
      Template template = fm.getTemplate("details.html");
      template.process(model, response.getWriter());

    } catch (Exception error) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  private boolean unlocked(String username) throws SQLException {
    long accountLockTime = 30; // seconds
    try (PreparedStatement stmt = database.prepareStatement(AUTH_QUERY)) {
      stmt.setString(1, username);
      ResultSet results = stmt.executeQuery();

      if (!results.next()) // can't lock an account if it doesn't exist
        return true;

      LocalDateTime lastFail = LocalDateTime.parse(results.getString("last_fail"));

      // if the account is locked and lock time has not passed, return false
      if (results.getInt("login_attempts") <= 0 &&
          lastFail.plusSeconds(accountLockTime).isAfter(LocalDateTime.now()))
        return false;

      return true;
    }
  }

  private boolean authenticated(String username, String password) throws SQLException {
    int loginAttempts = 3;
    try (PreparedStatement stmt = database.prepareStatement(AUTH_QUERY)) {
      stmt.setString(1, username);
      ResultSet results = stmt.executeQuery();

      if (!results.next())
        return false;

      String securePassword = results.getString("password");

      // if the password is incorrect, decrement attempts and update last fail time
      if (!Auth.passwordMatches(password, securePassword)) {
        try (PreparedStatement stmt3 = database.prepareStatement(ATTEMPT_UPDATE)) {
          stmt3.setInt(1, results.getInt("login_attempts") - 1);
          stmt3.setString(2, username);
          stmt3.executeUpdate();
        }
        try (PreparedStatement stmt4 = database.prepareStatement(TIME_UPDATE)) {
          stmt4.setString(1, LocalDateTime.now().toString());
          stmt4.setString(2, username);
          stmt4.executeUpdate();
        }
        return false;
      }

      // otherwise reset attempts and return true
      try (PreparedStatement stmt2 = database.prepareStatement(ATTEMPT_UPDATE)) {
        stmt2.setInt(1, loginAttempts);
        stmt2.setString(2, username);
        stmt2.executeUpdate();
      }
      return true;
    }
  }

  private List<Record> searchResults(String surname) throws SQLException {
    List<Record> records = new ArrayList<>();
    try (PreparedStatement stmt = database.prepareStatement(SEARCH_QUERY)) {
      stmt.setString(1, surname);
      ResultSet results = stmt.executeQuery();
      while (results.next()) {
        Record rec = new Record();
        rec.setSurname(results.getString(2));
        rec.setForename(results.getString(3));
        rec.setAddress(results.getString(4));
        rec.setDateOfBirth(results.getString(5));
        rec.setDoctorId(results.getString(6));
        rec.setDiagnosis(results.getString(7));
        records.add(rec);
      }
    }
    return records;
  }
}
