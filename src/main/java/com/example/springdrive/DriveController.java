/**
 * Copyright 2017 Google Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.springdrive;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleBrowserClientRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Controller
public class DriveController {

    private static final String APP_NAME = "Spring Drive";
    private static final String TOKEN_NAME = "access_token";
    private static final String AUTH_CODE = "code";
    private static final String LINK_FILE_CLIENT_SECRET = "/client_secret.json";

    private static final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    private static final InputStreamReader clientSecretStream
            = new InputStreamReader(DriveController.class.getResourceAsStream(LINK_FILE_CLIENT_SECRET));

    @GetMapping("/")
    public String index() {
        return "redirect:/listFile";
    }

    @GetMapping("/oauth2callback")
    public String auth2Callback(HttpServletRequest req)
            throws IOException, GeneralSecurityException {
        //
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, clientSecretStream);

        HttpTransport httpTransport = null;
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException ex) {
            Logger.getLogger(DriveController.class.getName()).log(Level.SEVERE, null, ex);
        }

        // code response
        String code = req.getParameter(AUTH_CODE);
        // request access token
        GoogleAuthorizationCodeTokenRequest gactr = new GoogleAuthorizationCodeTokenRequest(
                httpTransport, jsonFactory,
                clientSecrets.getDetails().getClientId(),
                clientSecrets.getDetails().getClientSecret(),
                code, clientSecrets.getDetails().getRedirectUris().get(0));

        String accessToken = gactr.execute().getAccessToken();
        System.out.println(accessToken);
        if (accessToken != null && !accessToken.isEmpty()) {
            req.getSession().setAttribute(TOKEN_NAME, accessToken);
        }
        //

        return "redirect:/listFile";
    }

    @GetMapping("/listFile")
    public ModelAndView listFile(HttpServletRequest req) throws IOException, GeneralSecurityException {
        ModelAndView modelAndView = new ModelAndView("UpFilePage");

        if (req.getSession().getAttribute(TOKEN_NAME) == null) {
            return modelAndView;
        }

        String accessToken = (String) req.getSession().getAttribute(TOKEN_NAME);

        if (accessToken.length() <= 0) {
            return modelAndView;
        }
        //

        try {
            GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

            //
            Drive drive
                    = new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    jsonFactory,
                    credential)
                    .setApplicationName(APP_NAME)
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
        } catch (Exception ex) {
            req.getSession().removeAttribute(TOKEN_NAME);
        }
        return modelAndView;
    }

    @GetMapping("/auth")
    public String hello() {
        String url = "";
        try {
            //
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, clientSecretStream);
            //
            GoogleBrowserClientRequestUrl client
                    = new GoogleBrowserClientRequestUrl(
                    clientSecrets,
                    clientSecrets.getDetails().getRedirectUris().get(0),
                    Collections.singleton(DriveScopes.DRIVE))
                    .setState("state_parameter_passthrough_value")
                    .set("access_type", "offline")
                    .set("include_granted_scopes", "true")
                    .setResponseTypes(Arrays.asList(AUTH_CODE));
            url = client.build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:" + url;
    }

    @PostMapping("/up")
    public String upFileSubmit(@RequestParam("file") MultipartFile uploadedFile,
                               HttpServletRequest req, Model model) throws IOException {
        String accessToken = (String) req.getSession().getAttribute(TOKEN_NAME);
        if (accessToken == null || accessToken.isEmpty()) {
            return "redirect:/auth";
        }

        //
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        //
        HttpTransport httpTransport = null;
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();


            //
            GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

            //
            Drive drive
                    = new Drive.Builder(
                    httpTransport,
                    jsonFactory, credential)
                    .setApplicationName(APP_NAME)
                    .build();

            String fileName = uploadedFile.getOriginalFilename();
            String mimeType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fileName);

            File fileDrive = new File();
            fileDrive.setOriginalFilename(fileName);
            fileDrive.setTitle(fileName);


            InputStreamContent inputStreamContent = new InputStreamContent(mimeType, uploadedFile.getInputStream());
            File insertedFile = drive.files().insert(fileDrive, inputStreamContent)
                    .execute();


            System.out.println("File ID: " + insertedFile.getId());
        } catch (Exception ex) {
            model.addAttribute("msg", ex.getMessage() + ex.getClass().getName());
            return "ErrorPage";
        }
        return "redirect:/listFile";
    }

}
