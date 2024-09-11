package com.devy.demo;

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
    	String mappingPage = "index";    // 이 컨틀롤러가 /index.jsp를 응답하는 경우 
    	String landingPage = "landing";  // landing.html 샘플파일을 이용하여 만든 대기 전용 경량화된 static 페이지
    	
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
    	// 대기표 검증하여 컨텐츠 교체 OR 별도의 대기페이지로 REDIRECT
    	if(!webgate.WG_IsValidToken(serviceId, gateId, request, response))
    	{
    		try {
    			
    			/* ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★ 
    			 * 아래 코드가 잘 동작하지 않는다면 적절히 수정해서 사용 바랍니다. 
    			 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★*/
    			
    			// 페이지 응답교체 또는 별도의 대기페이지로 REDIRECT할지 선택
    			boolean isReplaceMode = true; 
    			
    			// URL 이동없이 응답 컨텐츠 교체
    			if(isReplaceMode)  
    			{
	    			String uiHtml = webgate.WG_GetWaitingUi(serviceId, gateId);
	    			response.setContentType("text/html");
	        		PrintWriter out = response.getWriter();
	    			out.write(uiHtml);
	    			out.close();
	    			return "index";
    			}
    			// 별도의 대기 페이지로 REDIRECT
    			else { 
        			return "redirect:/landing";  // 대기용 페이지(/landing)를 별도로 만든 경우  
    			}
    			
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
    	
    	return "index"; // response index.jsp
    }

    
    @GetMapping("/landing") 
    public String landing(HttpServletRequest request, HttpServletResponse response) {
    	return "landing"; //response landing.jsp
    }    
}

