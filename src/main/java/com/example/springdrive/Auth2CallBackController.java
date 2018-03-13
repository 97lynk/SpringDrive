package com.example.springdrive;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author 97lynk
 */
@Controller
public class Auth2CallBackController {

    private static final Logger logger
            = Logger.getLogger(Auth2CallBackController.class.getName());

    @GetMapping("/oauth2callback")
    public String auth2Callback(HttpServletRequest req)
            throws IOException, GeneralSecurityException {
        //
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        //
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                jsonFactory,
                new InputStreamReader(Controller.class.getResourceAsStream("/client_secret.json")));

        HttpTransport httpTransport = null;
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException ex) {
            Logger.getLogger(Auth2CallBackController.class.getName()).log(Level.SEVERE, null, ex);
        }

        // code response
        String code = req.getParameter("code");
        // request access token
        GoogleAuthorizationCodeTokenRequest gactr = new GoogleAuthorizationCodeTokenRequest(
                httpTransport, jsonFactory,
                clientSecrets.getDetails().getClientId(),
                clientSecrets.getDetails().getClientSecret(),
                code, "http://project2cloud-197215.appspot.com/oauth2callback");

        String accessToken = gactr.execute().getAccessToken();
        System.out.println(accessToken);
        if (accessToken != null && !accessToken.isEmpty()) {
            req.getSession().setAttribute("access_token", accessToken);
        }
        // 

        return "redirect:/listFile";
    }

    @GetMapping("/listFile")
    public ModelAndView listFile(HttpServletRequest req) throws IOException, GeneralSecurityException {
        ModelAndView modelAndView = new ModelAndView("UpFilePage");

        if (req.getSession().getAttribute("access_token") == null) {
            return modelAndView;
        }

        String accessToken = (String) req.getSession().getAttribute("access_token");

        if (accessToken.length() <= 0) {
            return modelAndView;
        }
        //
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

        //
        Drive drive
                = new Drive.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        JacksonFactory.getDefaultInstance(),
                        credential)
                        .setApplicationName(HelloworldController.APP_NAME)
                        .build();

        //
        List<String> fileNames = new ArrayList<>();

        if (drive != null) {
            List<File> files = drive.files().list().execute().getItems();
            User user = drive.about().get().execute().getUser();
            for (File file : files) {

                if (file.getOriginalFilename() == null
                        || !file.getOriginalFilename().isEmpty()) {
                    fileNames.add(file.getTitle());
                }
            }
            user.getPicture().getUrl();
            modelAndView.addObject("files", files);
            modelAndView.addObject("user", user);
        }

        return modelAndView;
    }
}
