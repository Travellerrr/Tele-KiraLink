package cn.travellerr.onebottelegram.webui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class WebuiController {

    @GetMapping
    public String index() {
        return "webui";
    }

    @GetMapping("/webui")
    public String webui() {
        return "webui.html";
    }






}