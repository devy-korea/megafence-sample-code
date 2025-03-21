package com.devy.demo;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.devy.megafence.WebGate;

/*
* ==============================================================================================
* 메가펜스 유량제어서비스 SAMPLE(JAVA) V.24.1.911
* 이 샘플소스는 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
* 오류조치 및 개선을 목적으로 자유롭게 수정 가능하며 해당 내용은 공급처에 통보 바랍니다.
* 허가된 고객 이외의 무단 복사, 배포, 수정, 동작 등 일체의 이용을 금합니다.
* 작성자 : ysd@devy.co.kr
* ----------------------------------------------------------------------------------------------
*
* BACKEND 방식 : 페이지 로드 시 Backend 코드(java,jsp,php 등..)로 유량제어 서비스 호출
*
* <이용 안내> 
*   ⊙ 아래의 샘플코드를 그대로 테스트용 페이지에 삽입해서 대기UI가 표시되는지 확인
*   ⊙ 서비스 세팅이 완료되면 안내받은 GATE_ID, SERVICE_ID로 수정해서 사용
*   ⊙ 유량제어 코드는 DB접속 등의 부하량이 많은 업무로직 이전에 삽입해야 효과적입니다.
*   ⊙ 쿠키나 세션 등을 이용하는 간단한 처리는 유량제어 코드 이전에 배치되어도 무방합니다.
*   ⊙ log(STEP-0~6)는 동작 확인 후 지우세요~
*
* <주의 사항>
*   ⊙ 공통모듈(WebGate.java)을 사용하므로 함께 배포해 주세요. 
*   ⊙ JAVA Framework을 사용하는 경우 반드시 JAVA OR JSP 중에 하나에만 적용합니다. (중복적용 금지!) 
* ==============================================================================================-->
*/

@Controller
public class BackendSampleController {
	private Logger log = LoggerFactory.getLogger(BackendSampleController.class);
	
	@GetMapping({"/", "/index"}) 
    public String index(HttpServletRequest request, HttpServletResponse response) {
    	return "index"; // response index.jsp
    }

	/**
	 * REPLACE 방식 Controller 
	 */
	@GetMapping({"/sample_replace"}) 
    public String sample_replace(HttpServletRequest request, HttpServletResponse response) {
    	String mappingPage = "sample_replace";    // 이 컨틀롤러가 /sample_replace.jsp를 응답 
    	
    	/* 	========================================================================
    		Light business logic here ....
    		========================================================================
    		예) 로그인 체크 : 쿠키나 세션을 체크해서 Login 페이지로 redirect 등의 간단한 업무로직은 유량제어 코드 이전에 실행해도 됩니다.
    	*/
    	
    	//log.info("[STEP-0] 유량제어 체크 시작");
    	/*▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼  BEGIN OF 유량제어 코드삽입 */ 
    	String serviceId 	= "9000"; 	// 할당된 SERVICE ID 
    	String gateId 		= "1";  	// 사용할 GATE ID (할당된 GATE ID 범위내에서 사용)
    	
    	WebGate webgate = new WebGate();
    	// 대기표 검증하여 유효하지 않으면 대기UI 화면 컨텐츠로 응답 교체
    	/* 아래 2개 중 하나 (일반적으로 WG_IsNeetDoWaiting() 함수 사용
    	 * if(!webgate.WG_IsValidToken(serviceId, gateId, request, response)) 
    	 * OR
    	 * if(webgate.WG_IsNeedToWaiting(serviceId, gateId, request, response))
    	 */
    	if(webgate.WG_IsNeedToWaiting(serviceId, gateId, request, response))
    	{
    		try {
    			
    			/* ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★ 
    			 * 페이지 응답을 대기UI(uiHtml)로 교체하는 코드 샘플입니다.
    			 * 환경에 따라 아래 코드가 잘 동작하지 않을 수 있어 코드 수정(보완)이 필요할 수 있습니다.    
    			 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★*/
    			String uiHtml = webgate.WG_GetWaitingUi(serviceId, gateId);
    			response.setContentType("text/html");
        		PrintWriter out = response.getWriter();
    			out.write(uiHtml);
    			out.close();
    			return "sample_replace"; // 환경(Framework)에 따라 void return을 해야할 수 도 있습니다. 
    			
	    	} catch (Exception e) {
	    		// 필요시 log write..
	    	}
	        finally {}
    	} 
    	/*▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ END OF 유량제어 코드삽입 */
    	
    	
    	/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 
    	 * 여기까지 왔다면 유량제어 체크가 완료된 상태입니다. 
    	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
    	/*	========================================================================
    		Heavy business logic ....
    		========================================================================
    		예) 고객등급 GET : 고객 DB에서 고객등급(Bronze/Silver/Gold) 조회 
    		예) 주문상태 GET : 주문 DB에서 주문상태(주문완료/배송중/배송완료)별 수량 조회
    	*/
    	
    	return "sample_replace"; 
    }

	
	/**
	 * LANDING(REDIRECT) 방식 SAMPLE
	 */
    @GetMapping("/sample_landing") 
    public String sample_landing(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	String mappingPage = "sample_landing";    // 이 컨틀롤러가 /sample_replace.jsp를 응답 
    	
    	/* 	========================================================================
    		Light business logic here ....
    		========================================================================
    		예) 로그인 체크 : 쿠키나 세션을 체크해서 Login 페이지로 redirect 등의 간단한 업무로직은 유량제어 코드 이전에 실행해도 됩니다.
    	*/
    	
    	//log.info("[STEP-0] 유량제어 체크 시작");
    	/*▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼  BEGIN OF 유량제어 코드삽입 */ 
    	String serviceId 	= "9000"; 	// 할당된 SERVICE ID 
    	String gateId 		= "1";  	// 사용할 GATE ID (할당된 GATE ID 범위내에서 사용)
    	
    	WebGate webgate = new WebGate();
    	// 대기표 검증하여 유효하지 않으면 대기UI 화면 컨텐츠로 응답 교체
    	if(!webgate.WG_IsValidToken(serviceId, gateId, request, response))
    	{
    		return "redirect:/landing"; // landing.jsp로 redirect  
    	} 
    	/*▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ END OF 유량제어 코드삽입 */
    	
    	
    	/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 
    	 * 여기까지 왔다면 유량제어 체크가 완료된 상태입니다. 
    	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
    	/*	========================================================================
    		Heavy business logic ....
    		========================================================================
    		예) 고객등급 GET : 고객 DB에서 고객등급(Bronze/Silver/Gold) 조회 
    		예) 주문상태 GET : 주문 DB에서 주문상태(주문완료/배송중/배송완료)별 수량 조회
    	*/
    	
    	return "sample_landing";     
    }
    	
	
    /**
     * API 미사용 SAMPLE Controller
     */
    @GetMapping({"/sample_noapi"}) 
    public String sample_noapi(HttpServletRequest request, HttpServletResponse response) {
    	String mappingPage = "sample_noapi";    // 이 컨틀롤러가 /sample_without_api.jsp를 응답하는 경우 
    	
    	/* 	========================================================================
    		Light business logic here ....
    		========================================================================
    		예) 로그인 체크 : 쿠키나 세션을 체크해서 Login 페이지로 redirect 등의 간단한 업무로직은 유량제어 코드 이전에 실행해도 됩니다.
    	*/
    	
    	//log.info("[STEP-0] 유량제어 체크 시작");
    	/*▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼  BEGIN OF 유량제어 코드삽입 */ 
    	String serviceId 		= "9000"; 	// [Admin Page 참고] 할당된 SERVICE ID  
    	String gateId 			= "1";  	// [Admin Page 참고] 사용할 GATE ID (할당된 GATE ID 범위내에서 사용)
    	String secretApiKey 	= "YOUR_SECRET_API_KEY"; //[Admin Page 참고] Secret Api Key (외부 노출 주의 : 내부망 SITE 또는 Backend에서만 사용!) 
    	Integer freepassMinutes = 60; 		// fixed(수정금지) :  default 토큰 만료시간
    	Integer freepassCount   = 99;		// fixed(수정금지) :  default 토큰 사용회수 = Page load 수
    	Boolean useIpCheck		= false;	// fixed(수정금지) :  default IP 체크
    	
    	WebGate webgate = new WebGate();
    	
    	/**
    	 * 대기표 검증 : 신규 유입자면 유량제어 체크를 위한 응답으로 교체
    	 * 결과 코드는 WG_TOKEN_CHECKRESULT 쿠키로 저장됩니다.
    	 */
    	if(0 != webgate.WG_CheckTokenData(
    			request, 
    			response, 
    			serviceId, 
    			gateId, 
    			freepassMinutes, 
    			freepassCount, 
    			useIpCheck, 
    			secretApiKey))
    	{
    		try {
    			
    			/* ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★ 
    			 * 대기UI Html을 응답으로 교체
    			 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★*/
    			String uiHtml = webgate.WG_GetWaitingUi(serviceId, gateId);
    			response.setContentType("text/html");
        		PrintWriter out = response.getWriter();
    			out.write(uiHtml);
    			out.close();
    			return "sample_noapi"; // 환경(Framework)에 따라 void return을 해야할 수 도 있습니다. 
    			
	    	} catch (Exception e) {
	    		// 필요시 log write..
	    	}
	        finally {}
    	} 
    	/*▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ END OF 유량제어 코드삽입 */
    	
    	
    	/* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 
    	 * 여기까지 왔다면 유량제어 체크가 완료된 상태입니다. 
    	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
    	/*	========================================================================
    		Heavy business logic ....
    		========================================================================
    		예) 고객등급 GET : 고객 DB에서 고객등급(Bronze/Silver/Gold) 조회 
    		예) 주문상태 GET : 주문 DB에서 주문상태(주문완료/배송중/배송완료)별 수량 조회
    	*/
    	
    	return "sample_noapi"; // response sample_without_api.jsp
    }
    

    /**
     * 대기 전용 페이지
     * sample_landing 페이지에서 redirect target 페이지
     */
    @GetMapping({"/landing"}) 
    public String landing(HttpServletRequest request, HttpServletResponse response) {
    	return "landing";
    }
    	
}

