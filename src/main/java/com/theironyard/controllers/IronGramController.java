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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Timestamp;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by DrScott on 11/17/15.
 */
@RestController
public class IronGramController {
    @Autowired
    UserRepository users;

    @Autowired
    PhotoRepository photos;

    @RequestMapping("/login")
    public User login(HttpServletResponse response, HttpSession session, String username, String password) throws Exception {
        User user = users.findOneByUsername(username);
        if (user == null) {
            user = new User();
            user.username = username;
            user.password = PasswordHash.createHash(password);
            users.save(user);
        } else if (!PasswordHash.validatePassword(password, user.password)) {
            throw new Exception("Wrong password");
        }
        session.setAttribute("username", username);
        response.sendRedirect("/");
        return user;
    }

    @RequestMapping("/logout")
    public void logout(HttpServletResponse response, HttpSession session) throws IOException {
        session.invalidate();
        response.sendRedirect("/");
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
    public Photo upload(HttpSession session,
                        HttpServletResponse response,
                        String receiver,
                      @RequestParam(defaultValue = "0") long deleteTime,
                        boolean isPublic,
                        MultipartFile photo
    ) throws Exception {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("not logged in");
        }
        User senderUser = users.findOneByUsername(username);
        User receiverUser = users.findOneByUsername(receiver);
        if (receiverUser == null) {
            throw new Exception("receiver name doesn't exist");
        }
        File photoFile = File.createTempFile("photo", ".jpg", new File("public"));
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());

        Photo p = new Photo();
        p.receiver = receiverUser;
        p.sender = senderUser;
        p.filename = photoFile.getName();
        if (deleteTime==0){
           p.deleteTime = 10;
        } else{
        p.deleteTime = deleteTime;}
        p.isPublic = isPublic;

        photos.save(p);

        response.sendRedirect("/");
        return p;
    }

    @RequestMapping("/photos")
    public List<Photo> showPhotos(HttpSession session) throws Exception {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new Exception("Not logged int");
        }
        User user = users.findOneByUsername(username);

        List<Photo> photoList = photos.findByReceiver(user);
        for (Photo p : photoList) {
            if (p.accessTime == null) {
                p.accessTime = LocalDateTime.now();

                photos.save(p);
            } else if (p.accessTime.isBefore(LocalDateTime.now().minusSeconds(p.deleteTime))) {
                photos.delete(p);
                //File newFile = File.createTempFile("photo", "jpg",  File );
                File f = new File("public", p.filename);
                f.delete();
            }
        }
        return photos.findByReceiver(user);
    }

    @RequestMapping("/public-photos")
    public List<Photo> publicPhotos(String name) throws Exception {

        User sender = users.findOneByUsername(name);

       ArrayList<Photo> photoList = new ArrayList();
        for (Photo p : photos.findBySender(sender)){
            if (p.isPublic){
                photoList.add(p);
            }
        }
        return photoList;
    }
}

