package com.devy.demo;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@Controller
public class backendController {
 
	/*
	 * 대기상태체크 및 UI표시를 모두 JSP에서 처리하는 샘플 
	 * 컨트롤러에 구현된 server-side 작업이 많지 않은 경우 적합
	 */
    @GetMapping("/")
    public String index(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	return "index";
    }

}
