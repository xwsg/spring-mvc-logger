package com.github.logger;

import com.github.logger.annotation.LoggerIgnore;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * This guy is lazy, nothing left.
 *
 * @author xwsg
 */
@RestController
@RequestMapping("/logger")
public class LoggerController {

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public String test1(@LoggerIgnore @RequestParam(required = false) String x,
        @RequestParam(required = false) String y) {
        return "test1";
    }

    @GetMapping("/test1")
    public String test2(HttpServletRequest request, HttpServletResponse response) {

        return "test2";
    }

    @PostMapping("/test")
    public String test3(@RequestParam(value = "file", required = false) MultipartFile file) {

        return "test3";
    }

    @LoggerIgnore
    @DeleteMapping("/test")
    public String test4() {

        return "test4";
    }

    @PutMapping("/test")
    public String test5() {

        return "test5";
    }

    @PatchMapping("/test")
    public String test6() {

        return "test6";
    }
}
