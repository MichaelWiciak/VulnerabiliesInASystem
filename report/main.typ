#import "template.typ": template
#show: template.with()

= Analysis of Flaws
// two pages max
- Passwords stored as plain text
- SQL Injection Vulnerability
- Susceptible to brute force attacks (no limit to login attempts)

== Passwords stored as plain text
Nature of the flow
How it was discovered
Screenshot of the flow
== SQL Injection Vulnerability

How it was discovered

SQL Injection is a common flaw when not enough care is taken when treating user
inputs. Describe it more (reference)

#grid(
  columns: 2,
  [
    #figure(
      caption: "SQL Injection login page",
      image("assets/sql-inject-input.png"),
    ) <sql-inject-input>
  ],
  [
    #figure(caption: "SQL Injection result", image("assets/sql-inject-output.png")) <sql-inject-output>
  ],
)

As seen in @sql-inject-input, simply by putting username as `' or 1=1;`,
effectively bypassing authentication.

Also in @sql-inject-input, putting the same magic code into the patient surname
input field also bypasses search, returning all patient's details within the
database as seen in @sql-inject-output.

In the Java code `select * from user where username='%s' and password='%s'`, the
magic code `' or 1=1;` simply closes the first string, and the or effectively
always evaluates to true, bypassing the simple user authentication.

== Susceptible to brute force attacks (no limit to login attempts)

One could easily try commonly used usernames and passwords to put into the form.
The application has no limits on login attemps, which makes it prone to this
method, making it a security flaw.

How is was discovered

To test this theory, we used the `Hydra` @hydra login cracker to see if the
application is susceptible to brute force attacks, `Hydra` attemps to login
using the provided username and a password list. As seen in @hydra-output, we
managed to crack one of the user's password in 3.5 minutes.

#figure(caption: "Command for running Hydra", rect(```sh
hydra -l aps -P /opt/wordlists/rockyou.txt \
  -f localhost -s 8080 \
  http-post-form "/:username=^USER^&password=^PASS^:are not valid"
```), kind: "code", supplement: "Code") <hydra-command>

In @hydra-command, we call `hydra` with the following options:
- `-l aps` specifies user `aps`
- `-P /opt/wordlists/rockyou.txt` specifies which password list to use, in this
  case, the rockyou password list @rockyou
- `-f localhost -s 8080` to specify where the server is
- `http-post-form "/:username=^USER^&password=^PASS^:are not valid"` to specify we
  want to use the `http-post-form` module and post the form to `/` with form data
  `username` and `password`, we also want Hydra to know if the page shows text "are
  not valid", then the login failed.

#figure(caption: "Output from running hydra", image("assets/hydra-result.png")) <hydra-output>

#pagebreak()

= Fixes Implemented
// one page max

== Fixing weak password storage
What was changed
How does it fix the problem

== Fixing SQL injection vulnerability

To fix the SQL injection vulnerability, instead of using `String.format(...)`,
we now use `Connection.prepareStatement(...)`

`String.format` does not escape SQL keywords but `PrepareStatement` does. Using
`PrepareStatement` stops SQL injection since user input is escaped.

#figure(caption: "Fixing SQL injection", rect(```java
// Instead of...
String query = String.format(QUERY, userInput);
try (Statement stmt = database.createStatement()) {
  ResultSet results = stmt.executeQuery(query);
  // work with results...
}

// We now use...
try (PreparedStatement stmt = database.prepareStatement(QUERY)) {
  stmt.setString(1, userInput);
  ResultSet results = stmt.executeQuery();
  // work with results...
}
```), kind: "code", supplement: "Code") <modify-sql-inject>

Applying the generic changes in @modify-sql-inject to all database queries, SQL
injection is no longer possible.

== Fixing login brute force attack
What was changed
How does it fix the problem

#bibliography("references.bib", title: "References")