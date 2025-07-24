package com.devy.demo;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Post방식 Landing용 페이지입니다.
 * 외부 CDN의 Intro 페이지에서 유량제어 체크 후 post 되는 페이지입니다.
 * 서브도메인 쿠키 연동이 안되는 경우 부득이 사용하는 방식입니다.
 * @author devyadmin
 *
 */

@RestController
@RequestMapping("/postLanding")
public class PostLandingController {

	/**
	 * POST 요청 처리부
	 */
    @PostMapping
    public void postLanding(
        @RequestParam("WG_GATE_ID")   String gateId,
        @RequestParam("WG_TOKEN_NO")   String tokenNo,
        @RequestParam("WG_CLIENT_ID")  String clientId,
        @RequestParam("WG_WAS_IP")     String wasIp,
        @RequestParam("NextUrl")   String nextUrl,
        HttpServletResponse response
    ) throws IOException 
    {
    	/*  CDN Intro 페이지에서 post한 Cookie용 값들을 local cookie로 저장합니다. */
        // write cookie
    	addCookie(response, "WG_GATE_ID", gateId);
    	addCookie(response, "WG_TOKEN_NO", tokenNo);
    	addCookie(response, "WG_CLIENT_ID", clientId);
    	addCookie(response, "WG_WAS_IP", wasIp);

    	/* Backend 호출코드가 적용된 업무페이지로 이동합니다. */
        // redirect
        response.sendRedirect(nextUrl);
    }
    
    
    /**
     * cookie 저장 함수
     */
    public void addCookie(HttpServletResponse resp, String name, String value) throws IOException {
        if (value != null) {
            String encoded = URLEncoder.encode(value, "UTF-8"); 
            Cookie cookie = new Cookie(name, encoded);
            cookie.setHttpOnly(false);  // 반드시 false. frontend 접근 필요
            cookie.setSecure(true);		// use https
            cookie.setPath("/");        // 사이트 전체에서 접근 가능
            cookie.setMaxAge(60*60*24); // 유효기간 : 1Day
            resp.addCookie(cookie);
        }
    }
    	    
}