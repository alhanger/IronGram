package com.theironyard.controllers;

import com.theironyard.entities.Photo;
import com.theironyard.entities.User;
import com.theironyard.services.PhotoRepository;
import com.theironyard.services.UserRepository;
import com.theironyard.utils.PasswordHash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alhanger on 11/17/15.
 */
@RestController
public class IronGramController {
    @Autowired
    UserRepository users;

    @Autowired
    PhotoRepository photos;

    @RequestMapping("/login")
    public User login(HttpSession session, HttpServletResponse response, String username, String password) throws Exception {
        User user = users.findOneByUsername(username);

        if (user == null) {
            user = new User();
            user.username = username;
            user.password = PasswordHash.createHash(password);
            users.save(user);
        }
        else if (!PasswordHash.validatePassword(password, user.password)) {
            throw new Exception("Incorrect Password");
        }

        session.setAttribute("username", username);

        response.sendRedirect("/");

        return user;
    }

    @RequestMapping("/logout")
    public void logout(HttpSession session, HttpServletResponse response) throws IOException {
        session.invalidate();
        response.sendRedirect("/");
        System.out.println("Goodbye");
    }

    @RequestMapping("/user")
    public User user(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return null;
        }
        return users.findOneByUsername(username);
    }

    @RequestMapping("/upload")
    public Photo upload(
            HttpSession session,
            HttpServletResponse response,
            String receiver,
            MultipartFile photo,
            boolean isPublic,
            @RequestParam(defaultValue = "0") long deleteTime
    ) throws Exception {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("Not logged in.");
        }

        User senderUser = users.findOneByUsername(username);
        User receiverUser = users.findOneByUsername(receiver);

        if (receiverUser == null) {
            throw new Exception("Receiver name does not exists.");
        }

        if (!photo.getContentType().startsWith("image")) {
            throw new Exception("Only images are allowed.");
        }

        File photoFile = File.createTempFile("photo", photo.getOriginalFilename(), new File("public"));
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());

        Photo p = new Photo();
        p.sender = senderUser;
        p.receiver = receiverUser;
        p.filename = photoFile.getName();
        p.isPublic = isPublic;
        if (p.deleteTime == 0) {
            p.deleteTime = 10;
        }
        else {
            p.deleteTime = deleteTime;
        }
        photos.save(p);

        response.sendRedirect("/");

        return p;
    }

    @RequestMapping("/photos")
    public List<Photo> showPhotos(HttpSession session) throws Exception {
        String username = (String) session.getAttribute("username");

        User user = users.findOneByUsername(username);

        List<Photo> photoList = photos.findByReceiver(user);
        for (Photo p : photoList) {
            if (p.accessDate == null) {
                p.accessDate = LocalDateTime.now();
                photos.save(p);
            }
            else if (p.accessDate.isBefore(LocalDateTime.now().minusSeconds(p.deleteTime))) {
                File fileToDelete = new File(String.format("public/%s", p.filename));
                fileToDelete.delete();
                photos.delete(p);
            }
        }

        return photos.findByReceiver(user);
    }

    @RequestMapping("/public-photos")
    public List<Photo> publicPhotos(String username) {
        User user = users.findOneByUsername(username);
        List<Photo> pubPhotos = photos.findBySender(user);
        ArrayList<Photo> photos2 = new ArrayList<>();

        for (Photo p : pubPhotos) {
            if (p.isPublic == true) {
                photos2.add(p);
            }
        }

        return photos2;
    }
}
