package com.ppkwu.zad4;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VCard {

    @RequestMapping("VCard/{hello}")
    public String getVCard(){
        return "hello";
    }

}
