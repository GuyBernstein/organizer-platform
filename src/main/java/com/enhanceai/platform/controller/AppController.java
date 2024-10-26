package com.enhanceai.platform.controller;

import com.enhanceai.platform.service.Redis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppController {

    @Autowired
    Redis redis;

    @RequestMapping(value = "/getkey", method = RequestMethod.GET)
    public String getKey(@RequestParam String key) {
        return redis.get(key).toString();
    }

    @RequestMapping(value = "/setkey", method = RequestMethod.GET)
    public Boolean setKey(@RequestParam String key, @RequestParam String value) {
        return redis.set(key,value);
    }

}
