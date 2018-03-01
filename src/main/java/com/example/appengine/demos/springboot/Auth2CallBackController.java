/*
 * Copyright 2018 97lynk.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.appengine.demos.springboot;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import java.security.GeneralSecurityException;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.*;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.*;
import java.io.InputStreamReader;
import java.util.ArrayList;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author 97lynk
 */
@Controller("/auth2callback")
public class Auth2CallBackController {

    private static final Logger logger
            = Logger.getLogger(Auth2CallBackController.class.getName());

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView listFile(HttpServletRequest req, HttpServletResponse res) throws IOException {
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
                code, "http://localhost:8082/oauth2callback");

        String accessToken = gactr.execute().getAccessToken();
        System.out.println(accessToken);
        if (accessToken != null && !accessToken.isEmpty()) {
            req.getSession().setAttribute("access_token", accessToken);
        }

        //
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

        //
        Drive drive
                = new Drive.Builder(
                        httpTransport,
                        jsonFactory, credential)
                        .build();
        List<String> fileNames = new ArrayList<>();
        if (drive != null) {
            List<File> files = drive.files().list().execute().getItems();
            for (File file : files) {
                if (file.getOriginalFilename() == null
                        || !file.getOriginalFilename().isEmpty()) {
                    fileNames.add(file.getOriginalFilename());
                }
            }
        }
        ModelAndView modelAndView = new ModelAndView("FileListPage");
        modelAndView.addObject("files", fileNames);
        return modelAndView;
    }
}
