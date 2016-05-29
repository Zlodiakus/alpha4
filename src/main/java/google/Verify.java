package google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;


import java.util.Arrays;

public class Verify {
    private static final String SERVER_ID = "818299087088-ooq951dsv5btv7361u4obhlse0apt3al.apps.googleusercontent.com";
    private static final String CLIENT_ID = "818299087088-v573i62e7flakbu5uupkp06gs76ct8le.apps.googleusercontent.com";

    //sAj6skHU0Y14gMQcWNZsyLb5

    public static String checkID(String token) {
        try {
            Checker checker = new Checker(new String[]{CLIENT_ID}, SERVER_ID);
            GoogleIdToken.Payload jwt = checker.check(token);
            if (jwt==null) return "~"+checker.problem();
            else return jwt.getEmail();
        } catch (Exception e) {
            return e.toString() + "\n" + Arrays.toString(e.getStackTrace());
        }
    }
}

