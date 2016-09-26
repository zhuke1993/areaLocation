package com.zhuke.controller;

import com.alibaba.fastjson.JSONArray;
import com.zhuke.util.AreaUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@EnableAutoConfiguration
public class AreaController {
    
    @ResponseBody
    @RequestMapping("/country.do")
    public Object country() {
        List country = AreaUtils.getCountry();
        return country == null ? null : new JSONArray(country);
    }

    @RequestMapping("/province.do")
    @ResponseBody
    public Object province(@RequestParam("id") String id) {
        List state = AreaUtils.getState(id);
        return state == null ? null : new JSONArray(state);
    }

    @RequestMapping("/city.do")
    @ResponseBody
    public Object city(@RequestParam("id") String id) {
        List city = AreaUtils.getCity(id);
        return city == null ? null : new JSONArray(city);
    }

    @RequestMapping("/district.do")
    @ResponseBody
    public Object district(@RequestParam("id") String id) {
        List region = AreaUtils.getRegion(id);
        return region == null ? null : new JSONArray(region);
    }

    public static void main(String[] args) {
        SpringApplication.run(AreaController.class, args);
    }
}


