/**
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.appengine.demos.springboot;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleBrowserClientRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import org.springframework.boot.context.embedded.MimeMappings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HelloworldController {

    private static final Logger logger
            = Logger.getLogger(HelloworldController.class.getName());

    @GetMapping("/")
    public String index() {
        return "UpFilePage";
    }

    @GetMapping("/auth")
    public String hello() {
        String url = "";
        try {
            //
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            //
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                    jsonFactory,
                    new InputStreamReader(Controller.class.getResourceAsStream("/client_secret.json")));
            //
            GoogleBrowserClientRequestUrl client
                    = new GoogleBrowserClientRequestUrl(
                            clientSecrets,
                            "http://localhost:8082/oauth2callback",
                            Collections.singleton(DriveScopes.DRIVE))
                            .setState("state_parameter_passthrough_value")
                            .set("access_type", "offline")
                            .set("include_granted_scopes", "true")
                            .setResponseTypes(Arrays.asList("code"));
            url = client.build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:" + url;
    }

    @GetMapping("/up")
    public String upFilePage() {
        return "UpFilePage";
    }

    @PostMapping("/up")
    public String upFileSubmit(@RequestParam("file") MultipartFile file,
            HttpServletRequest req, Model model) throws IOException {
        String accessToken = (String) req.getSession().getAttribute("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            return "redirect:/auth";
        }
        Enumeration<String> headers = req.getHeaderNames();
        while (headers.hasMoreElements()) {
            String h = headers.nextElement();
            logger.info(h + " : " + req.getHeader(h));
        }

        //
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        //
        HttpTransport httpTransport = null;
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException ex) {
            Logger.getLogger(Auth2CallBackController.class.getName()).log(Level.SEVERE, null, ex);
        }

        //
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

        //
        Drive drive
                = new Drive.Builder(
                        httpTransport,
                        jsonFactory, credential)
                        .build();

        File fileMetadata = new File();
        fileMetadata.setOriginalFilename(file.getOriginalFilename());
        fileMetadata.setTitle(file.getOriginalFilename());
        logger.info(file.getOriginalFilename());
        String mimeType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(file.getOriginalFilename());
        System.out.println(mimeType);
        FileContent mediaContent = new FileContent(mimeType,
                convert(file));
        File insertedFile = drive.files().insert(fileMetadata, mediaContent)
                .execute();
        System.out.println("File ID: " + insertedFile.getId());
        model.addAttribute("files", drive.files().list().execute().getItems()
                .stream().map(File::getOriginalFilename).collect(Collectors.toList()));
        return "FileListPage";
    }

    public static java.io.File convert(MultipartFile file) throws IOException {
        java.io.File convFile = new java.io.File(file.getOriginalFilename());
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        if (convFile == null) {
            logger.info("File is null");
        }
        return convFile;
    }

}
